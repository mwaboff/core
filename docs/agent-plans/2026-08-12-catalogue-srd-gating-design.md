# Catalogue SRD Gating (Workstream D) — Implementation Plan

## Context
Branch `feat/srd-content-gating`. Foundation (schema, `ContentAccessService`, `ContentRedaction`,
`Restrictable`) is already merged on the branch. Domain, Question, and CardCostTag are already
fully gated by a prior agent. This plan covers the remaining four Workstream-D types, assigned in
full detail by `team-lead`. The task brief already specifies exact method names, predicate text,
and file-level decisions — this doc records the concrete plan derived from reading the actual
code (not a re-derivation of the brief) so implementation can proceed without re-litigating
settled decisions.

## 1. Fix compile break: Class
- `UpdateClassRequest` / `CreateClassRequest`: add optional `Boolean srd` (no `@NotNull`).
- `ClassResponse`: add `implements Restrictable`, `private Boolean restricted;`, and `private
  Boolean srd;` (matches `DomainResponse`, which already has both). Already has
  `@JsonInclude(NON_NULL)`.
- `ClassService`: add a private `currentUser(Authentication)` helper identical to
  `DomainService`'s (extract `((CustomUserDetails) authentication.getPrincipal()).getUser()`).
  Fixes the three `currentUser(...)` call sites already written at lines 132/151/191.
  `request.getSrd()` calls at 190/191/321 resolve once the DTO field exists.
  `ContentRedaction.stub` at 379 resolves once `ClassResponse implements Restrictable`.
  `ClassResponse.builder().srd(...)` at 388 resolves once the field exists.

## 2. TransformationCard — full standard treatment
- Repository: add `@Param("includeNonSrd") boolean includeNonSrd` predicate to
  `findByDeletedAtIsNullAndExpansion` (its `TransformationCard` has both `isOfficial` and `srd`,
  so use the full standard predicate: `AND (:includeNonSrd = true OR t.isOfficial = false OR
  t.srd = true)`). `findAllWithExpansion` (the `includeDeleted=true` sibling) gets no predicate.
- Service: inject `ContentAccessService`. In `getAllTransformationCards`, replace the raw
  `includeDeleted` branch with `contentAccessService.resolveIncludeDeleted(includeDeleted)` and
  thread `contentAccessService.includeNonSrd()` into the list query. In
  `createTransformationCard`/bulk/update, call `contentAccessService.resolveSrd(user, ...)`
  (need `Authentication` threaded into `buildFromRequest`, which currently doesn't take a user).
  `toResponse` gets the standard `mayView(isOfficial, srd)` redaction guard at the top.
- DTOs: `CreateTransformationCardRequest`/`UpdateTransformationCardRequest` get optional
  `Boolean srd`. `TransformationCardResponse` gets `implements Restrictable` +
  `Boolean restricted` + `Boolean isOfficial`/`Boolean srd` fields (mirroring `ClassResponse`).

## 3. SubclassPath — full standard treatment + cascade
- `SubclassPath` has no `isOfficial` field (confirmed by reading the entity) — same shape as
  `Question`/`CardCostTag`. Repository predicate follows their pattern:
  `AND (:includeNonSrd = true OR sp.srd = true)` on `findByDeletedAtIsNullAndFilters`.
  `findAllWithFilters` (includeDeleted=true) gets none.
- Service: inject `ContentAccessService`. `toResponse` redaction guard:
  `contentAccessService.mayView(true, path.getSrd())` (force the check, matching
  `QuestionService`/`CardCostTagService` — `SubclassPath` has no official/custom distinction of
  its own).
- Cascade (the core of this item): `updateSubclassPath`, when `request.getSrd() != null`, resolves
  `contentAccessService.resolveSrd(user, request.getSrd())`, sets it on the path, and then — in
  the same `@Transactional` method — loads every non-deleted `SubclassCard` whose
  `subclassPath.id` matches and sets `card.setSrd(path.getSrd())` on each, saving via
  `subclassCardRepository.saveAll(...)`. Needs a new `SubclassCardRepository` finder:
  `findBySubclassPathIdAndDeletedAtIsNull`. Same cascade fires in `createSubclassPath`/bulk
  after path save, though a freshly created path has no cards yet — included for symmetry and
  because `resolvePath`'s find-or-create branch can attach cards to an *existing* path.
- Verified (read, not edited) `SubclassCardService`: it already ignores request-supplied `srd`
  and derives `card.setSrd(path.getSrd())` on every create/update — contract confirmed, no
  changes needed there.
- DTOs: `CreateSubclassPathRequest`/`UpdateSubclassPathRequest` get optional `Boolean srd`.
  `SubclassPathResponse` gets `implements Restrictable` + `Boolean restricted` + `Boolean srd`.
- Test: assert no active `SubclassCard` disagrees with its `SubclassPath`'s `srd`, exercised via
  the update-cascade path.

## 4. Feature — new `is_official` column
- Migration via `./scripts/create-migration.sh add_is_official_to_features`:
  `ALTER TABLE features ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT FALSE;` then backfill
  `is_official = true` where any parent join table links to an official (`is_official = true`)
  row. Parent join tables (grepped, not guessed): `card_features`→`cards`,
  `class_hope_features`+`class_class_features`→`classes`, `adversary_features`→`adversaries`,
  `beastform_features`→`beastforms`, `weapon_features`→`weapons`, `armor_features`→`armors`,
  `loot_features`→`loot`, `transformation_card_features`→`transformation_cards`,
  `environment_features`→`environments`, `martial_stance_features`→`martial_stances`. Cross-check
  against `FeatureService`'s existing signal (a feature with `expansion_id IS NOT NULL AND
  created_by_user_id IS NULL` is official) — report counts if the two derivations disagree
  instead of picking one silently. Drop `idx_features_srd_visibility`, recreate as
  `idx_features_srd_visibility ON features(is_official, srd) WHERE deleted_at IS NULL`.
- Entity: add `Boolean isOfficial` to `Feature.java`, `@Column(name = "is_official", nullable =
  false)`, matching `Card.java`'s declaration (no `@Builder.Default`, since it's always set
  explicitly in code, never left to the DB default — same reasoning as the migration's
  `@DynamicInsert` caveat).
- Service: `FeatureService` never reads `isOfficial` from a request DTO.
  - Standalone `createFeature`/`createFeaturesBulk` (ADMIN/OWNER-only, `expansionId` is
    `@NotNull`): `isOfficial = (expansion != null)` — effectively always true since expansion
    is required, but written generically rather than hardcoded `true`.
  - Parent-driven `findOrCreate`/`createFeatureFromInput`: derive from `FeatureOrigin`. Extend
    the record with a new `boolean srd` component (default `false` via existing `imported()`/
    `forItem()` factories, so `WeaponService`/`ArmorService`/`LootService`'s existing call sites
    are unaffected). `isOfficial` is *not* a new field — it's `origin.mayClaimSourcebook()`,
    which by construction already equals "this came from official content" (`imported()` →
    `true`; `forItem(user, false)` → `false`). Add `FeatureOrigin.forParent(User user, boolean
    parentIsOfficial, boolean parentSrd)` as a new factory, used only by `ClassService` and
    `TransformationCardService` (the two parent types this workstream owns), so their inline
    hope/class features and transformation-card features inherit the parent's resolved
    `isOfficial`/`srd` instead of unconditionally defaulting to official via `imported()`.
  - `srd` on standalone create/update: add optional `Boolean srd` to `CreateFeatureRequest`/
    `UpdateFeatureRequest`, resolved via `contentAccessService.resolveSrd(user, request.getSrd())`
    (ADMIN+ only, matches every other type). Do **not** add `isOfficial` to either DTO.
- Predicate: `FeatureRepository#findByDeletedAtIsNullAndFilters` gets the full standard predicate
  `AND (:includeNonSrd = true OR f.isOfficial = false OR f.srd = true)`. `findAllWithFilters`
  (includeDeleted=true) gets none.
- Reflection test: assert no `Feature*Request` DTO in `model.dto.dh.request` declares an
  `isOfficial` field, with a comment explaining why (mirrors the intent of
  `SrdPredicateCoverageTest`'s allowlist comments).

## Shared allowlist cleanup
`SrdPredicateCoverageTest.ALLOWED_UNGATED_QUERIES` still carries stale "Pending Workstream D"
entries for `DomainRepository`/`QuestionRepository`/`CardCostTagRepository` even though those
predicates already exist (confirmed by reading the files) — harmless (the test short-circuits on
`bindsIncludeNonSrd` before consulting the allowlist) but misleading. Remove all Workstream-D
entries (Domain, Question, CardCostTag, Class, SubclassPath, TransformationCard, Feature) for
queries that get a predicate; leave `findAllWithFilters` entries with the existing
`includeDeleted=true administrative listing` reasoning already established by sibling types.

## Verification
`./mvnw -q compile` first (unblocks other workstreams), then targeted service tests, then
`./scripts/start-db.sh` + `./mvnw spring-boot:run` to prove the migration applies, per the brief.
