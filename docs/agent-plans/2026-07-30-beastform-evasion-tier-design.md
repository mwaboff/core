# Beastform: Add `evasion` and `tier` — Design

**Date:** 2026-07-30
**Status:** Approved (proceeding per team-lead task spec; auto-mode, no interactive human user for this subagent)
**Scope:** Migration, entity, DTOs, service/mapper, search indexing, tests. Backend only, `core/` directory.

## Context

The Beastform entity (added 2026-01-31, see `2026-01-31-beastform-design.md`) models Daggerheart
druid beastform stat-block cards but is missing two printed values: `evasion` (every card prints an
Evasion bonus) and `tier` (cards are grouped Tier 1-4, gating which forms a Druid can access). 24
parsed records are waiting on these fields to exist. Table is currently empty in prod.

## Field Decisions

| Field | Type | Nullable | Default | Rationale |
|-------|------|----------|---------|-----------|
| `evasion` | Integer | NOT NULL | `0` (Java `@Builder.Default` + entity/DTO default; no DB `DEFAULT` needed since table is empty) | Architecturally it's another numeric modifier alongside the six trait modifiers (`agilityModifier`, etc.), which already follow "NOT NULL, defaults to 0" — 0 reads as "no evasion bonus," a sensible neutral value for future user-created customs. |
| `tier` | Integer | NOT NULL | none (required on create, no default) | Matches the existing tier convention used by `Weapon`, `Armor`, `Loot`, `Adversary`, `Environment`, `MartialStance`: `@Column(nullable = false)` with **no** Java default, `@NotNull @Min(1) @Max(4)` on the create DTO, and a `CHECK (tier BETWEEN 1 AND 4)` constraint (`chk_beastforms_tier`) matching `chk_weapons_tier` / `chk_armors_tier` / `chk_loot_tier`. Tier is a required classification with no meaningful "unset" value, unlike a modifier. |

## Migration

Generated via `./scripts/create-migration.sh add_evasion_and_tier_to_beastforms`. Since the table has
zero rows in prod, `ADD COLUMN ... NOT NULL` needs no `DEFAULT` clause (Postgres only requires a
default for `NOT NULL` adds on non-empty tables).

```sql
ALTER TABLE beastforms ADD COLUMN evasion INTEGER NOT NULL;
ALTER TABLE beastforms ADD COLUMN tier INTEGER NOT NULL;

ALTER TABLE beastforms ADD CONSTRAINT chk_beastforms_tier CHECK (tier BETWEEN 1 AND 4);
```

## Entity (`Beastform.java`)

Add two fields near the trait modifiers / after `advantages`:

```java
@Column(name = "evasion", nullable = false)
@Builder.Default
private Integer evasion = 0;

@Column(name = "tier", nullable = false)
private Integer tier;
```

## DTOs

- `CreateBeastformRequest`: `evasion` gets `@Builder.Default private Integer evasion = 0;` (matches
  trait modifier style). `tier` gets `@NotNull @Min(1) @Max(4) private Integer tier;` (matches
  `CreateWeaponRequest.tier`).
- `UpdateBeastformRequest`: both plain nullable `Integer` fields, no validation annotations (matches
  existing optional-patch style — non-null means "apply this change").
- `BeastformResponse`: both plain `Integer` fields alongside the other stat fields.

## Service (`BeastformService.java`)

- `createBeastform`: set `.evasion(request.getEvasion() != null ? request.getEvasion() : 0)` (same
  null-guarded pattern as the six trait modifiers) and `.tier(request.getTier())`.
- `updateBeastform`: `if (request.getEvasion() != null) beastform.setEvasion(...)` and same for
  `tier`.
- `toResponse`: add `.evasion(beastform.getEvasion())` and `.tier(beastform.getTier())` to the
  builder chain.
- Bulk create path reuses `createBeastform`, so no separate change needed.

## Search Indexing

`tier` is added to `buildForBeastform` in `SearchFieldMapping` — it's an existing filter column
(`SearchIndexData.tier`) already used by `Weapon`/`Armor`/`Loot`/`Adversary`/`Environment`/
`MartialStance` as a facet filter, and Beastform should support the same tier-based filtering.

`evasion` is **not** added to the search index. None of the other numeric stat/modifier fields
(trait modifiers, weapon damage numbers, etc.) are indexed as filter columns or search text — only
categorical/facet-like values are. Evasion is a raw stat, not a facet, so it stays out, consistent
with the existing pattern.

## Tests

- `SearchFieldMappingTest`: add a `buildForBeastform`-focused test that constructs a `Beastform` via
  its entity builder (not via HTTP) with `tier` set, and asserts `data.getTier()` is populated —
  following the existing regression-test pattern in that file (e.g. `MapsCardTypeFilterColumn`
  tests) that catches fields silently staying null.
- `BeastformControllerIntegrationTest`: add a new bulk-create integration test that POSTs **raw JSON**
  (not a builder-constructed request) to `/api/dh/beastforms/bulk` using the real "Agile Scout"
  card values from the core rulebook (p351), then asserts the persisted/returned beastform via the
  real HTTP response, including `evasion`, `tier`, and nested `features`. This is the pattern
  HANDOFF.md §4.3 calls out: builder-constructed test objects can't catch a field arriving null
  because `@Builder.Default` doesn't apply to Jackson deserialization — only a real JSON string run
  through the HTTP path exercises that.
- `BeastformTest` (entity unit test) and `BeastformServiceTest`: extend existing coverage for the
  two new fields where the existing test structure has natural extension points (getters/setters,
  create/update mapping).

## Verification (separate from `./mvnw verify`)

Per HANDOFF.md, `./mvnw verify` runs against H2 with Flyway disabled and cannot catch a migration
failure. Verification plan:
1. `./mvnw clean` (avoid stale `target/classes/db/migration/` duplicate-migration failures).
2. Boot a **disposable** Postgres container on a **non-default port** (not `core-postgres-1`, which
   holds real prod-copy data and is currently serving the running app on :8080).
3. Boot the Spring Boot app against that disposable Postgres via
   `--spring.datasource.url=jdbc:postgresql://localhost:<port>/...` and confirm no `FlywayException`.
4. Do not touch `compose.yaml`, do not stop/restart the app already running on :8080.

## Files Touched

| File | Action |
|------|--------|
| `src/main/resources/db/migration/V{timestamp}__add_evasion_and_tier_to_beastforms.sql` | Create |
| `src/main/java/com/aboff/core/model/entity/dh/Beastform.java` | Modify |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateBeastformRequest.java` | Modify |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateBeastformRequest.java` | Modify |
| `src/main/java/com/aboff/core/model/dto/dh/response/BeastformResponse.java` | Modify |
| `src/main/java/com/aboff/core/service/dh/BeastformService.java` | Modify |
| `src/main/java/com/aboff/core/config/SearchFieldMapping.java` | Modify (`buildForBeastform` adds `tier`) |
| `src/test/java/com/aboff/core/config/SearchFieldMappingTest.java` | Modify |
| `src/test/java/com/aboff/core/controller/dh/BeastformControllerIntegrationTest.java` | Modify |
| `src/test/java/com/aboff/core/model/entity/dh/BeastformTest.java` | Modify (if extension points exist) |
| `src/test/java/com/aboff/core/service/dh/BeastformServiceTest.java` | Modify (if extension points exist) |
| `.api-blueprint/references/beastforms-api.md` | Modify (per CLAUDE.md — endpoint/DTO doc must stay current) |

## Out of Scope

- Frontend / `dawn` changes (other agents own that directory).
- `core-import` payload generation (other agent).
- Any change to `CharacterSheet.activeBeastform` or beastform selection logic.
