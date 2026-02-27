# Subclass Path Redesign

**Date:** 2026-02-15
**Status:** Approved

## Context

Subclass cards in Daggerheart are organized by **paths** — named groupings of 3 cards (Foundation, Specialization, Mastery) within a class. For example, the Druid class offers "Warden of Renewal" and "Warden of the Elements" paths. Each path shares associated domains and a spellcasting trait across its 3 cards.

Currently, SubclassCard has `associatedClass`, `associatedDomains`, and `spellcastingTrait` directly on the card. This redesign introduces a formal `SubclassPath` entity to group cards and hold shared attributes.

**Future consideration:** Multiclassing allows a player to select a second class and subclass path. This design supports that by making paths first-class entities.

## Approach

### Data Model

#### New: `SubclassPath` Entity

| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| id | Long | id | PK (from BaseEntity) |
| name | String | name | VARCHAR(200), NOT NULL |
| associatedClass | Class | associated_class_id | FK, NOT NULL, ManyToOne LAZY |
| spellcastingTrait | Trait | spellcasting_trait | VARCHAR(20), nullable |
| associatedDomains | Set\<Domain\> | (join table) | ManyToMany LAZY |
| expansion | Expansion | expansion_id | FK, NOT NULL, ManyToOne LAZY |
| deletedAt | LocalDateTime | deleted_at | nullable (soft delete) |

- **Unique constraint:** `UNIQUE(LOWER(name), associated_class_id)` — case-insensitive name uniqueness per class
- **Soft delete support:** `isDeleted()`, `softDelete()`, `restore()`

#### Modified: `SubclassCard` Entity

| Removed | Added |
|---------|-------|
| `associatedClass` (ManyToOne Class) | `subclassPath` (ManyToOne SubclassPath, NOT NULL, LAZY) |
| `spellcastingTrait` (Trait enum) | |
| `associatedDomains` (ManyToMany Domain) | |

Retained: `level` (SubclassLevel), `name`, `description`, `features`, `costTags`, `expansion`, `isOfficial`, `backgroundImageUrl`, `deletedAt`

#### Updated: `FeatureType` Enum

Add `SUBCLASS` to existing values: HOPE, ANCESTRY, CLASS, COMMUNITY, DOMAIN, OTHER, **SUBCLASS**

### Database Migration

New migration: `V{timestamp}__add_subclass_paths.sql`

Steps:
1. Create `subclass_paths` table (id, name, associated_class_id FK, spellcasting_trait, expansion_id FK, created_at, last_modified_at, deleted_at)
2. Create `subclass_path_domains` join table (subclass_path_id FK, domain_id FK)
3. Add unique index on `(LOWER(name), associated_class_id)` for case-insensitive uniqueness
4. Migrate existing data: create SubclassPath records from existing SubclassCard data (group by associated_class_id + spellcasting_trait combination)
5. Add `subclass_path_id` FK column to `subclass_cards`
6. Populate `subclass_path_id` from migrated paths
7. Add NOT NULL constraint on `subclass_cards.subclass_path_id`
8. Migrate `subclass_domains` data to `subclass_path_domains`
9. Drop `associated_class_id`, `spellcasting_trait` columns from `subclass_cards`
10. Drop `subclass_domains` join table
11. Add indexes on `subclass_paths` and `subclass_cards.subclass_path_id`

## File Changes

### New Files

#### Entity
- `src/main/java/com/aboff/core/model/entity/dh/SubclassPath.java`

#### DTOs
- `src/main/java/com/aboff/core/model/dto/dh/request/SubclassPathInput.java` — inline find-or-create input (name, associatedDomainIds, spellcastingTrait)
- `src/main/java/com/aboff/core/model/dto/dh/request/CreateSubclassPathRequest.java` — CRUD create (name, associatedClassId, expansionId, spellcastingTrait, associatedDomainIds)
- `src/main/java/com/aboff/core/model/dto/dh/request/UpdateSubclassPathRequest.java` — CRUD update
- `src/main/java/com/aboff/core/model/dto/dh/response/SubclassPathResponse.java` — response with expand support (associatedClass, associatedDomains, expansion)

#### Repository
- `src/main/java/com/aboff/core/repository/dh/SubclassPathRepository.java`
  - `findByIdAndDeletedAtIsNull(Long id)`
  - `findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull(String name, Long classId)` — for find-or-create
  - `findByDeletedAtIsNullAndFilters(Long classId, Pageable)` — paginated with optional class filter
  - `findAllWithFilters(Long classId, Pageable)` — including deleted
  - `findAllByIdInAndDeletedAtIsNull(List<Long> ids)`

#### Service
- `src/main/java/com/aboff/core/service/dh/SubclassPathService.java`
  - CRUD: `getAllSubclassPaths()`, `getSubclassPathById()`, `createSubclassPath()`, `updateSubclassPath()`, `deleteSubclassPath()`, `restoreSubclassPath()`
  - `findOrCreate(String name, Long classId, Long expansionId, List<Long> domainIds, Trait spellcastingTrait)` — case-insensitive lookup by name + class; creates new path if not found
  - `resolvePath(Long subclassPathId, SubclassPathInput pathInput, Long associatedClassId, Long expansionId)` — resolves path from either ID or inline input
  - `toResponse(SubclassPath, Set<String> expand)` — entity to DTO with expansion support

#### Controller
- `src/main/java/com/aboff/core/controller/dh/SubclassPathController.java`
  - `GET /api/dh/subclass-paths` — list (paginated, filter by classId, expand)
  - `GET /api/dh/subclass-paths/{id}` — get single
  - `POST /api/dh/subclass-paths` — create (ADMIN/OWNER)
  - `POST /api/dh/subclass-paths/bulk` — bulk create (ADMIN/OWNER)
  - `PUT /api/dh/subclass-paths/{id}` — update (ADMIN/OWNER)
  - `DELETE /api/dh/subclass-paths/{id}` — soft delete (ADMIN/OWNER)
  - `POST /api/dh/subclass-paths/{id}/restore` — restore (ADMIN/OWNER)

#### Migration
- `src/main/resources/db/migration/V{timestamp}__add_subclass_paths.sql`

#### Tests
- `src/test/java/com/aboff/core/service/dh/SubclassPathServiceTest.java`
- `src/test/java/com/aboff/core/controller/dh/SubclassPathControllerIntegrationTest.java`

### Modified Files

#### Entity
- `src/main/java/com/aboff/core/model/entity/dh/SubclassCard.java` — remove `associatedClass`, `spellcastingTrait`, `associatedDomains`; add `subclassPath` ManyToOne

#### Enum
- `src/main/java/com/aboff/core/model/enums/FeatureType.java` — add `SUBCLASS`

#### DTOs
- `src/main/java/com/aboff/core/model/dto/dh/request/CreateSubclassCardRequest.java` — remove `associatedClassId`, `spellcastingTrait`, `associatedDomainIds`; add `subclassPathId`, `subclassPath` (SubclassPathInput), `associatedClassId` (for auto-create scope)
- `src/main/java/com/aboff/core/model/dto/dh/request/UpdateSubclassCardRequest.java` — same changes
- `src/main/java/com/aboff/core/model/dto/dh/response/SubclassCardResponse.java` — remove `associatedClassId`, `spellcastingTrait`, `associatedDomainIds`, expand fields for class/domains; add `subclassPathId`, expand `subclassPath`

#### Service
- `src/main/java/com/aboff/core/service/dh/SubclassCardService.java` — inject SubclassPathService; use `resolvePath()` in create/update; update `toResponse()` for path; update filters

#### Repository
- `src/main/java/com/aboff/core/repository/dh/SubclassCardRepository.java` — update filter queries to support both `subclassPathId` and `associatedClassId` (via join through path)

#### Controller
- `src/main/java/com/aboff/core/controller/dh/SubclassCardController.java` — add `subclassPathId` filter param, keep `associatedClassId` filter

#### Tests
- `src/test/java/com/aboff/core/service/dh/SubclassCardServiceTest.java` — update for path-based creation/response
- `src/test/java/com/aboff/core/controller/dh/SubclassCardControllerIntegrationTest.java` — update request bodies and assertions

## Testing Strategy

### New Unit Tests: `SubclassPathServiceTest`
- CRUD operations (get all, get by ID, create, update, delete, restore)
- `findOrCreate`: existing path found (case-insensitive), new path created, with/without domains/trait
- `resolvePath`: by ID, by input, validation when neither provided, validation when both provided
- `toResponse`: with and without expand (associatedClass, associatedDomains, expansion)
- Edge cases: not found, already deleted, duplicate name+class

### New Integration Tests: `SubclassPathControllerIntegrationTest`
- All REST endpoints with proper auth
- Pagination and filtering by classId
- Expand parameter support
- 401/403 for unauthorized access
- 404 for missing paths
- Bulk creation

### Modified Unit Tests: `SubclassCardServiceTest`
- Update create/update flows to use `subclassPathId` / `subclassPath` input
- Test path auto-creation during card creation
- Update response assertions (subclassPathId instead of associatedClassId/domains/trait)
- Test filtering by subclassPathId and associatedClassId

### Modified Integration Tests: `SubclassCardControllerIntegrationTest`
- Update request JSON bodies
- Update response assertions
- Test new filter parameters
- Test expand=subclassPath

## Implementation Order

1. FeatureType enum update (SUBCLASS)
2. Database migration
3. SubclassPath entity
4. SubclassPath DTOs (Input, Create, Update, Response)
5. SubclassPath repository
6. SubclassPath service
7. SubclassPath controller
8. SubclassPath tests (unit + integration)
9. SubclassCard entity modification
10. SubclassCard DTO modifications
11. SubclassCard repository modification
12. SubclassCard service modification
13. SubclassCard controller modification
14. SubclassCard test updates
15. Full test suite validation