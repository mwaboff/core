# SRD Content Gating — Workstream C (Items) Design

## Context

Oh Sheet currently serves all official Daggerheart content to every authenticated user. This
feature gates paid-expansion ("non-SRD") content behind ADMIN/OWNER role or a per-user "Access
All Expansions" grant, while SRD-licensed content stays visible to everyone. Workstream A
(foundation) is complete and already on branch `feat/srd-content-gating`: `ContentAccessService`,
`Restrictable`, `ContentRedaction`, and the `BaseItem.srd` column all exist.

This is Workstream C: applying that contract to the four item types — Weapon, Armor, Loot,
MartialStance. Scope is strictly these four types plus this agent's entries in the shared
`SrdPredicateCoverageTest` allowlist. No entity fields, no migrations, no other workstream's
files.

## Approach

Mechanical, per-type application of an already-established pattern (confirmed by reading the
foundation code and one full reference implementation per layer):

1. **Repository**: add `AND (:includeNonSrd = true OR x.isOfficial = false OR x.srd = true)` to
   every `findAccessibleWithFilters` query — both `value` and `countQuery` where the type has a
   separate count query (Weapon, Armor, Loot all do; MartialStance's list queries have no
   campaign join so no separate count query exists there). Add the `@Param("includeNonSrd")`
   parameter. `findAllWithFilters` (includeDeleted=true path) gets no predicate — reachability is
   closed by `resolveIncludeDeleted`.

2. **Service**: inject `ContentAccessService`. Pass `contentAccessService.includeNonSrd()` into
   the gated query call. Wrap `includeDeleted` in `resolveIncludeDeleted(...)` in every list
   method (this also closes a currently-unenforced hole in `MartialStanceService`, which today
   branches on the raw `includeDeleted` flag with zero permission check). Call
   `resolveSrd(user, request.getSrd())` in single-create, bulk-create, and update — for
   MartialStance, `currentUser(authentication)` does not currently exist on that service (it has
   no `ItemAccessService`), so pull the user via `((CustomUserDetails) authentication.getPrincipal()).getUser()` — checked against existing services, this is exactly what
   `ItemAccessService.currentUser` does, so this stays a one-line inline resolution rather than
   introducing a new service dependency for one call.

   Correction after re-reading `ItemAccessService`: simplest and most consistent is to inject
   `ContentAccessService` only (not `ItemAccessService`, which MartialStanceService does not use
   today and the brief does not ask this agent to add) and resolve the user directly via
   `CustomUserDetails` cast, matching the one place `resolveSrd` needs a `User`.

3. **Redaction**: two-line guard at the top of each `toResponse`, returning
   `ContentRedaction.stub(...)` when `!contentAccessService.mayView(item)`. This is the universal
   funnel — list endpoints, single-get, and `CharacterSheetService`'s
   `toWeaponResponse`/`toArmorResponse`/`toLootResponse` delegates (confirmed at
   `CharacterSheetService.java:1456-1524`) all resolve through it. MartialStance is not currently
   embedded in `CharacterSheetService`, but gating `toResponse` still covers its own list/get
   endpoints and any future embed.

4. **DTOs**: each `*Response` implements `Restrictable` + `private Boolean restricted;` (all four
   already carry `@JsonInclude(NON_NULL)` — verified, none need adding it). `CreateWeaponRequest`,
   `CreateArmorRequest`, `CreateLootRequest`, `UpdateWeaponRequest`, `UpdateArmorRequest`,
   `UpdateLootRequest`, `CreateMartialStanceRequest`, `UpdateMartialStanceRequest` each gain an
   optional `Boolean srd` (no `@NotNull`). `CreateCustomWeaponRequest`, `CreateCustomArmorRequest`,
   `CreateCustomLootRequest` get **no** `srd` field — confirmed there is no
   `CreateCustomMartialStanceRequest` (MartialStance has no user-authoring path; only ADMIN/OWNER
   create via `CreateMartialStanceRequest`, confirmed in `MartialStanceController`), so nothing
   else to touch for that type. A reflection test asserts the three custom-request classes
   declare no `srd` field.

5. **Coverage test**: remove this workstream's 18 entries from `ALLOWED_UNGATED_QUERIES` in
   `SrdPredicateCoverageTest` as each predicate is added (5 Weapon, 5 Armor, 5 Loot, 4
   MartialStance — `MartialStanceRepository` has no `findAccessibleWithFilters`/no campaign-scoped
   query, so its allowlist block only has `findByDeletedAtIsNullAndFilters`,
   `findAllWithFilters`, `findByIdAndDeletedAtIsNull`, `findAllByIdInAndDeletedAtIsNull` — 4, not
   5). `findByIdAndDeletedAtIsNull` and `findAllByIdInAndDeletedAtIsNull` don't filter by
   official/srd at all (single/batch lookup by ID, used for embed resolution and `originalXId`
   references) — these stay ungated at the SQL layer by design; `toResponse`'s redaction guard is
   what protects them, since the coverage test only checks that a predicate exists or a reason is
   given, not that every ID lookup itself is filtered. Reason recorded for each: "ID lookup; not a
   browse surface — protected by toResponse redaction, not by query filtering."

6. **Docs**: update `.api-blueprint/references/weapons-api.md`, `armors-api.md`, `loot-api.md`,
   and the MartialStance reference doc (find its actual filename) for the new `srd` field and
   `restricted`/`expansionName` response fields.

## File changes

- `repository/dh/WeaponRepository.java`, `ArmorRepository.java`, `LootRepository.java`,
  `MartialStanceRepository.java`
- `service/dh/WeaponService.java`, `ArmorService.java`, `LootService.java`,
  `MartialStanceService.java`
- `model/dto/dh/response/WeaponResponse.java`, `ArmorResponse.java`, `LootResponse.java`,
  `MartialStanceResponse.java`
- `model/dto/dh/request/CreateWeaponRequest.java`, `UpdateWeaponRequest.java`,
  `CreateArmorRequest.java`, `UpdateArmorRequest.java`, `CreateLootRequest.java`,
  `UpdateLootRequest.java`, `CreateMartialStanceRequest.java`, `UpdateMartialStanceRequest.java`
- `test/java/com/aboff/core/repository/dh/SrdPredicateCoverageTest.java` (remove 18 C entries)
- New reflection test for the three custom-request classes
- Existing `WeaponServiceTest`, `ArmorServiceTest`, `LootServiceTest`, `MartialStanceServiceTest`
  updated for new mock/signature (`ContentAccessService` mock, `resolveIncludeDeleted` stubbing)
- `.api-blueprint/references/*.md` for the four types

## Testing strategy

- Update existing service unit tests for the new `ContentAccessService` dependency and
  `includeNonSrd`/`resolveIncludeDeleted` call sites.
- Add redaction-path assertions to each service test (`toResponse` returns a stub when
  `mayView` is false).
- New reflection test: `CreateCustomWeaponRequest`, `CreateCustomArmorRequest`,
  `CreateCustomLootRequest` declare no field named `srd`.
- Run only: `WeaponServiceTest, ArmorServiceTest, LootServiceTest, MartialStanceServiceTest,
  SrdPredicateCoverageTest` (+ new reflection test), per the brief — the full suite is run by
  another agent at the end.
