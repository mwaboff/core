# Workstream D (Catalogue) — SRD vs. Paid-Expansion Content Gating

## Context

Part of the multi-workstream "SRD vs. Paid-Expansion Content Gating" feature on branch
`feat/srd-content-gating`. This document covers only Workstream D's seven types: `Domain`,
`Class`, `SubclassPath`, `TransformationCard`, `Question`, `CardCostTag`, `Feature`. Other
workstreams (A foundation, B cards, C items, E GM content) are editing the same tree
concurrently; this plan touches only files for these seven types plus this workstream's own
entries in the shared `SrdPredicateCoverageTest` allowlist.

The team lead's brief (reproduced in full in the task history) is itself the approved design —
it was produced by a prior planning pass at the team level, specifies exact predicate text,
exact file-level obligations, and two special-cased types (SubclassPath cascade, Feature
`is_official` backfill). This document records the concrete decisions made while translating
that brief into this codebase's actual files, verified by reading the real entities,
repositories, services, DTOs, and migrations rather than assuming their shape.

## Current state (verified by reading the code)

- All seven entities already carry `Boolean srd` (Workstream A). `TransformationCard` also
  already has `isOfficial`. No entity/migration work needed for `srd` itself.
- `Feature` has **no** `is_official` column; `V20260811222234279__add_srd_and_expansion_access.sql`
  shipped `idx_features_srd_visibility` as a stopgap on `srd` alone, with a comment flagging this
  workstream to fix it.
- All seven `*Response` DTOs already have `@JsonInclude(NON_NULL)`.
- `DomainRepository` has two active-list queries (`findByDeletedAtIsNullAndFilters`,
  `findByIdAndDeletedAtIsNull` is single-get, `findAllByIdInAndDeletedAtIsNull` is batch) — per
  the coverage-test allowlist, all four non-`findAllWithFilters` methods on each of the seven
  repositories are currently allowlisted "pending Workstream D" and must be resolved (predicate
  added or a real reason substituted).
- `TransformationCardRepository`'s active-list query is named `findByDeletedAtIsNullAndExpansion`
  (not `findByDeletedAtIsNullAndFilters` like the others).
- None of the seven repository methods have a separate `countQuery` — all use `Page<T>` return
  with a single JPQL string, so Spring Data derives the count query from the same JPQL. No
  double predicate needed for these seven (unlike types elsewhere that do have one).
- `SubclassCardRepository` (Workstream B, already edited) exposes
  `findByDeletedAtIsNullAndFilters(expansionId, isOfficial, associatedClassId, subclassPathId,
  level, includeNonSrd, pageable)`, which already accepts a `subclassPathId` filter. The
  SubclassPath cascade reuses this existing method with `includeNonSrd=true` and
  `Pageable.unpaged()` to fetch every active card under a path, rather than adding a new
  repository method to a file Workstream B owns.
- Feature-parent join tables found by grepping every migration for `feature_id BIGINT` (not
  guessed): `card_features` (cards), `class_hope_features` (classes), `class_class_features`
  (classes), `adversary_features` (adversaries), `beastform_features` (beastforms),
  `weapon_features` (weapons), `armor_features` (armors), `loot_features` (loot),
  `transformation_card_features` (transformation_cards), `martial_stance_features`
  (martial_stances), `environment_features` (environments). `feature_card_cost_tags` and
  `feature_feature_modifiers` are Feature's own children, not parents, and are excluded from the
  backfill join. All eleven parent tables have an `is_official` column on the parent.

## Approach

### Standard treatment (Domain, Class, SubclassPath, TransformationCard, Question, CardCostTag)

1. Repository: add `AND (:includeNonSrd = true OR x.isOfficial = false OR x.srd = true)` (the
   `x.isOfficial = false` disjunct dropped for the three entities with no `isOfficial` column —
   `SubclassPath`, `Question`, `CardCostTag`) to each **active-list `Page<T>` query only**, with a
   new `@Param("includeNonSrd") boolean includeNonSrd` parameter appended to the method
   signature. `DomainRepository` has two such queries (`findByDeletedAtIsNull(Pageable)`, dead
   code with zero callers today but converted to `@Query` and gated anyway per the brief, plus
   `findByDeletedAtIsNullAndFilters`); every other repository has exactly one.

   **Correction verified against real call sites, overriding a literal reading of the coverage
   test's placeholder allowlist text:** single-get (`findByIdAndDeletedAtIsNull`), batch-by-ids
   (`findAllByIdInAndDeletedAtIsNull`), and find-or-create dedupe lookups
   (`findByLabelIgnoreCaseAndDeletedAtIsNull`, `findByQuestionTextIgnoreCase...`,
   `findByNameIgnoreCaseAndExpansionId...`, `findByNameIgnoreCaseAndAssociatedClassId...`) are
   **not** gated at the repository level, matching the precedent already shipped by Workstream E
   (`AdversaryRepository#findByIdAndDeletedAtIsNull` etc. — "Single-entity fetch; feeds
   XService#toResponse, which redacts non-SRD content directly. Not a list/browse query.") This
   is not merely stylistic: three of these methods have a **real external caller in another
   workstream's file** — `DomainRepository#findByIdAndDeletedAtIsNull` (`DomainCardService`),
   `TransformationCardRepository#findByIdAndDeletedAtIsNull` (`CharacterSheetService`), and
   `FeatureRepository#findAllByIdInAndDeletedAtIsNull` (`AncestryCardService`) — all verified by
   grepping every caller of each of the seven repositories across `src/main`. Adding a required
   `includeNonSrd` parameter to any of these would force an edit to a file outside this
   workstream's scope, mid-flight under a concurrently-running agent. Leaving them ungated and
   routing redaction through `toResponse` (next point) avoids that entirely, and is the
   correct design per the brief's own point 3 ("toResponse is the verified universal funnel").
2. Service: inject `ContentAccessService`, thread `includeNonSrd()` through the gated `Page<T>`
   query call(s), wrap `includeDeleted` in `resolveIncludeDeleted(...)`, call
   `resolveSrd(user, ...)` in create/bulk-create/update.
3. `toResponse`: redact via `ContentRedaction.stub` at the top when `!mayView(...)`. **Extends to
   embedded expansions of sibling gated types within this workstream** — `ClassService.toResponse`
   currently builds `DomainResponse`/`QuestionResponse` inline for `?expand=associatedDomains` /
   `backgroundQuestions` / `connectionQuestions`, bypassing redaction entirely; same for
   `SubclassPathService.toResponse` (`associatedClass`, `associatedDomains`) and
   `FeatureService.toResponse` (`costTags`). These are rewritten to call
   `domainService.toResponse(...)`, `questionService.toResponse(...)`,
   `classService.toResponse(...)`, `cardCostTagService.toResponse(...)` respectively (each made
   `public`, matching `FeatureService.toResponse`'s existing visibility), so a gated embedded
   domain/question/class/cost-tag comes back as a proper redacted stub instead of leaking full
   detail through a sibling type's expand. `TransformationCardService.toResponse` already routes
   `features` through `featureService.toResponse`; only its inline `questions` block needed the
   same fix.
4. DTOs: `*Response implements Restrictable` + `restricted` field; `Create*/Update*Request` gain
   optional `Boolean srd`.

**Correction to the brief for entities without `isOfficial`:** `SubclassPath`, `Question`, and
`CardCostTag` have no `isOfficial` field at all (only `Domain`, `Class`, `TransformationCard`,
and `Feature` do, among these seven). For those three, `mayView(Boolean, Boolean)` is called with
`isOfficial=null` — `ContentAccessService.mayView` treats a null `isOfficial` as "not official",
which makes every row unconditionally visible. That's correct for `CardCostTag`, `Question`, and
arguably not for `SubclassPath` — but a subclass path's real gate is implicit through its own
`srd` flag mirroring its cards' `isOfficial`/`srd`, and the repository predicate for these three
uses a literal `true` in place of `x.isOfficial = false` (there is no such column), i.e.:
`AND (:includeNonSrd = true OR x.srd = true)`. This is deliberately less permissive than
`mayView`'s null-safe reading (custom content on these three is never distinguished from official
by an `isOfficial` flag, so nothing to exempt), and matches how `mayView(null, srd)` would already
evaluate at the redaction layer — visible whenever the caller may see non-SRD content OR the row
is SRD-flagged.

### SubclassPath cascade

`SubclassPathService.updateSubclassPath`, inside the existing `@Transactional` method: after
resolving `srd` via `contentAccessService.resolveSrd(user, request.getSrd())` and setting it on
the path, if the resolved value differs from the *effective* incoming request (i.e. `srd` was
present in the request), reload every active `SubclassCard` under the path via
`subclassCardRepository.findByDeletedAtIsNullAndFilters(null, null, null, path.getId(), null,
true, Pageable.unpaged())`, set `card.setSrd(...)` to the same value, and `saveAll(...)` — all in
the same transaction as the path save. `createSubclassPath`/`createSubclassPathsBulk` also resolve
and set `srd` on the new path; no card cascade runs there because a path has zero cards at the
moment of its own creation.

Test: `SubclassPathServiceTest` — updating a path's `srd` updates every active card under it;
a new integration-style unit test in `SrdPredicateCoverageTest`'s spirit (but scoped as a plain
repository/service test, not touching the shared file beyond the allowlist) asserting no active
`SubclassCard`'s `srd` disagrees with its path's `srd` after the cascade.

### Feature `is_official`

1. New migration (`./scripts/create-migration.sh add_is_official_to_features`): add
   `is_official BOOLEAN NOT NULL DEFAULT FALSE`, backfill `TRUE` for any feature id reachable
   from the eleven parent join tables above where the parent is official, replace
   `idx_features_srd_visibility` with `(is_official, srd) WHERE deleted_at IS NULL`.
2. Before writing the backfill, cross-check row counts against `FeatureService`'s existing
   signal (`expansion_id IS NOT NULL AND created_by_user_id IS NULL` ⇒ official) and report both
   counts. If they disagree on any row, stop and report rather than picking one silently.
3. Entity: add `isOfficial` to `Feature.java`, `@Column(name = "is_official", nullable = false)`,
   matching `Card.java`'s declaration (no `@Builder.Default`, since the create-time default is
   set in the service, not the entity, matching `Domain`/`Class`).
4. Service: `FeatureService` never reads `isOfficial` from a request DTO. `createFeatureFromInput`
   (parent-driven path) inherits `origin`'s official-ness; standalone `createFeature`/
   `createFeaturesBulk` derive `isOfficial` from whether `resolveExpansionId` actually resolved an
   expansion (mirrors the existing `mayClaimSourcebook` signal: an expansion was named and
   accepted ⇒ official). Add a reflection test asserting no `Create*FeatureRequest`/
   `Update*FeatureRequest` declares `isOfficial`.
5. Repository: `f.isOfficial = false OR f.srd = true`.
6. `FeatureResponse`: add `isOfficial` (siblings `DomainResponse`/`ClassResponse` expose it, so
   consistency argues for exposing it here too) — not on request DTOs.

## File changes (by type)

For each of the seven types: one repository file, one service file, one `*Response` DTO, one
`Create*Request` DTO, one `Update*Request` DTO. Plus for Feature: one new migration, `Feature.java`
entity edit, `FeatureResponse.java`. Plus for SubclassPath: cascade logic inside
`SubclassPathService.java` (no new file). Plus shared: surgical edits to
`SrdPredicateCoverageTest.java`'s `ALLOWED_UNGATED_QUERIES` (Workstream D block only) and the
relevant `.api-blueprint/references/*.md` files for these seven types.

## Testing strategy

- Unit tests per service: gating behavior (redacted stub when `mayView` is false), `resolveSrd`
  wiring on create/update, `resolveIncludeDeleted` wiring.
- `FeatureServiceTest`: reflection test for no `isOfficial` on request DTOs; derivation tests for
  parent-driven vs. standalone creation.
- `SubclassPathServiceTest`: cascade test (update propagates to active cards, in one transaction,
  soft-deleted cards untouched).
- `SrdPredicateCoverageTest`: remove Workstream D's allowlist entries as each predicate is added;
  run the whole test to confirm no regressions from concurrent workstreams' edits.
- Migration: `./scripts/start-db.sh` + `./mvnw spring-boot:run` until "Started CoreApplication",
  confirming the new `is_official` column and backfill apply cleanly; report boot success
  explicitly.
- Scoped test run only (per the team lead's instruction): the seven services' test classes plus
  `SrdPredicateCoverageTest`, not the full suite.
