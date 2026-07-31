# Add `is_official` to domains and classes — design

**Date:** 2026-07-31
**Status:** approved, pending implementation

## Context

The `/reference` page's "Official content only" filter defaults to `isOfficial: true`
(`dawn/src/app/features/reference/reference.ts:62`). That flows into
`SearchIndexRepository`'s predicate:

```sql
AND (CAST(:isOfficial AS boolean) IS NULL OR si.is_official = :isOfficial)
```

`search_index.is_official` is **NULL for every DOMAIN and CLASS row** (10 + 13 = 23 rows),
because `SearchFieldMapping.buildForDomain` (`:123-132`) and `buildForClass` (`:141-150`)
never call `.isOfficial(...)`. A NULL fails `= true`, so **typing any search query makes all
domains and all classes vanish, for every expansion.**

The root cause is structural, not a dropped assignment: the `domains` and `classes` tables
have no `is_official` column at all. Verified against every migration — only 11 mention
`is_official` (cards, weapons, armors, loot, beastforms, adversaries, encounters,
environments, martial_stances, conditions, search_index).

Secondary symptom: `DomainController` and `ClassController` accept no `isOfficial` query
param, but `dawn`'s `domain.service.ts` / `class.service.ts:53-55` send one. Spring ignores
unknown params silently, so the checkbox is inert rather than erroring on those browse tabs.

**Live-import context.** A Hope & Fear content import is in flight against production right
now. Domain id 10 (Dread) and class ids 10–13 (Assassin, Brawler, Warlock, Witch) already
exist under expansion 2 and must end up official. All 9 core domains and 9 core classes
must likewise stay official.

### Decisions taken

| Question | Decision |
|---|---|
| Approach | Add a real `is_official` column (rejected: loosening the search predicate, dropping the frontend default, hardcoding `true` in the index mapping) |
| Column default | `NOT NULL DEFAULT true` |
| `search_index` backfill | SQL `UPDATE` inside the same migration, not a post-deploy reindex |

Rationale for the column over the cheaper options: loosening the predicate to
`IS NOT FALSE` would un-hide genuinely unofficial rows of *other* types; dropping the
frontend default changes the page's content policy. The column makes the index honest and
leaves room for a genuinely unofficial domain or class later.

Rationale for the migration backfill: a reindex only rewrites rows whose entities get
re-saved, so it would leave the 23 existing rows NULL. A migration applies identically to
qa and prod on deploy with no manual step. Precedent:
`V20260730151632944__backfill_search_index_card_type.sql`.

### Out of scope

- `SUBCLASS_PATH`, `FEATURE`, `EXPANSION` search rows also carry NULL `is_official`. Same
  class of problem, not this change (YAGNI — no reported symptom).
- The separate in-flight fix adding `isOfficial` to adversaries, fixing
  `buildForCommunityCard`, and publishing an index event from
  `SubclassPathService.findOrCreate`. **Coordination hazard:** that work also edits
  `SearchFieldMapping.java`. It must not touch `buildForDomain`/`buildForClass`; this work
  must not touch `buildForCommunityCard`.

## Approach

A complete vertical slice per entity, following the conventions already established by
`BaseItem` / `Weapon`:

- Entity field: `@Column(name = "is_official", nullable = false) private Boolean isOfficial;`
  — matching `BaseItem.java:75-76`. **No Java field initializer.** Hibernate has no
  `@DynamicInsert` here, so the column is always in the INSERT and the DB `DEFAULT` never
  applies to new rows. The default must therefore be applied in the service layer.
- Service defaulting: `request.getIsOfficial() != null ? request.getIsOfficial() : true` on
  create; null-means-unchanged on update, matching the existing partial-update convention.
- Repository filter: `AND (:isOfficial IS NULL OR d.isOfficial = :isOfficial)`, matching
  `WeaponRepository.java:40`.
- Controller param: `@RequestParam(required = false) Boolean isOfficial`, matching
  `WeaponController.java:64`.

## File changes

### 1. Migration

`src/main/resources/db/migration/V20260731HHMMSSmmm__add_is_official_to_domains_and_classes.sql`

```sql
ALTER TABLE domains ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE classes ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT true;

-- 1. DOMAIN / CLASS rows have never carried is_official (no column existed).
UPDATE search_index SET is_official = true
 WHERE entity_type IN ('DOMAIN', 'CLASS')
   AND is_official IS NULL;

-- 2. COMMUNITY_CARD rows: buildForCommunityCard never set the field. The code fix
--    only affects future writes, so existing rows need this backfill.
UPDATE search_index si SET is_official = c.is_official
  FROM cards c
 WHERE si.entity_type = 'COMMUNITY_CARD'
   AND si.entity_id = c.id
   AND si.is_official IS NULL;
```

Plus a **subclass-path reindex backfill** — paths created implicitly by
`SubclassPathService.findOrCreate` were never indexed at all, so 8 rows are missing entirely
rather than merely NULL. This is an INSERT, not an UPDATE, and it must reproduce what
`SearchFieldMapping.buildForSubclassPath` produces (including `search_vector`). Confirm the
exact column set against that method before writing it; if reproducing `search_vector`
faithfully in SQL proves fragile, fall back to the admin reindex endpoint for this one type
and say so explicitly rather than emitting a subtly wrong row.

`DEFAULT true` backfills all existing rows in the `ALTER`, covering the 9 core + 1 H&F
domains and 9 core + 4 H&F classes in one step. Include a header comment explaining why the
default is `true` and why these backfills are here rather than left to a post-deploy reindex
(prior art: the subclass-path index gap went unnoticed precisely because it depended on a
manual step).

### Verification after the migration

| Check | Expected |
|---|---|
| `SELECT count(*) FROM search_index WHERE entity_type IN ('DOMAIN','CLASS','COMMUNITY_CARD') AND is_official IS NULL` | 0 |
| `SELECT count(*) FROM search_index WHERE entity_type = 'SUBCLASS_PATH'` | 26 (was 18) |
| `SELECT count(*) FROM domains WHERE is_official IS NOT true` | 0 |
| `SELECT count(*) FROM classes WHERE is_official IS NOT true` | 0 |

### 2. Entities

| File | Change |
|---|---|
| `model/entity/dh/Domain.java` | add `isOfficial` field after `description` (~line 49) |
| `model/entity/dh/Class.java` | add `isOfficial` field, same position relative to `description` |

### 3. DTOs

| File | Change |
|---|---|
| `dto/dh/request/CreateDomainRequest.java` | add nullable `Boolean isOfficial` |
| `dto/dh/request/UpdateDomainRequest.java` | add nullable `Boolean isOfficial` |
| `dto/dh/request/CreateClassRequest.java` | add nullable `Boolean isOfficial` |
| `dto/dh/request/UpdateClassRequest.java` | add nullable `Boolean isOfficial` |
| `dto/dh/response/DomainResponse.java` | expose `isOfficial` |
| `dto/dh/response/ClassResponse.java` | expose `isOfficial` |

Nullable on create (not `@NotNull`) so existing clients keep working and get `true`.

### 4. Repositories

| File | Change |
|---|---|
| `repository/dh/DomainRepository.java` | add `isOfficial` param to `findByDeletedAtIsNullAndExpansion` and `findAllWithExpansion`; rename to `...AndFilters` to match `WeaponRepository` |
| `repository/dh/ClassRepository.java` | same |

### 5. Services

| File | Change |
|---|---|
| `service/dh/DomainService.java` | set `isOfficial` on create (default `true`) and update (null-means-unchanged); thread the new filter param through `getAllDomains` (~`:60-79`) |
| `service/dh/ClassService.java` | same (~`:69-84`) |

### 6. Search mapping

`config/SearchFieldMapping.java` — add `.isOfficial(domain.getIsOfficial())` to
`buildForDomain` (~`:123-132`) and `.isOfficial(clazz.getIsOfficial())` to `buildForClass`
(~`:141-150`). Update each method's `Filter:` javadoc line to include `isOfficial`, matching
the sibling builders. **Touch nothing else in this file.**

### 7. Controllers

| File | Change |
|---|---|
| `controller/dh/DomainController.java` | add `@RequestParam(required = false) Boolean isOfficial` to `getAllDomains` (~`:47-59`), pass to service, document in javadoc |
| `controller/dh/ClassController.java` | same (~`:52`) |

## Testing strategy

Two known blind spots in this repo's existing tests let bugs of exactly this shape ship
green. Do not repeat either:

1. `SearchFieldMappingTest.java:479,497` builds a card with `.isOfficial(true)` but asserts
   only `entityType` and `cardType` — never the resulting `getIsOfficial()`. **New
   assertions must assert the field itself.**
2. Builder-based fixtures mask DTO deserialization gaps. This repo has a raw-JSON test
   convention for that; use it so a missing DTO field fails.

Coverage required:

- `SearchFieldMappingTest` — `buildForDomain` and `buildForClass` propagate `isOfficial`,
  asserting the value, for both `true` and `false`.
- `DomainServiceTest` / `ClassServiceTest` — create with `isOfficial` omitted defaults to
  `true`; create with explicit `false` persists `false`; update with `null` leaves it
  unchanged; update with a value changes it.
- Repository/controller integration — the `isOfficial` filter actually narrows results, and
  omitting it returns both official and unofficial rows.
- Raw-JSON deserialization test for `CreateDomainRequest` and `CreateClassRequest`.
- Migration: verify against a scratch DB that existing rows land `true` and that the 23
  `search_index` rows are no longer NULL.

Run `./mvnw test` to green. Report any pre-existing unrelated failure rather than fixing it.

## Deployment note

Merged is not deployed. This project has already been bitten once by prod running flyway 64
against a repo at 76. The migration must actually reach production before the `/reference`
search filter is fixed there — verify `flyway_schema_history` on prod after deploy, and
confirm `search_index` has no NULL `is_official` for DOMAIN or CLASS.
