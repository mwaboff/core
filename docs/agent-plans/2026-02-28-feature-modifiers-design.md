# Feature Modifiers Design

## Context

Features are associated with many game elements (Armor, Weapons, Cards, Classes) and currently describe abilities via text descriptions and cost tags. The frontend needs a way to **programmatically determine** what stat modifications a Feature applies when active (e.g., Chainmail Armor's Feature giving -1 Evasion).

Currently, cost tags serve as informational labels ("3 Hope", "-1 Evasion") but aren't machine-readable. This design adds structured, queryable modifier data to Features that the frontend can use to automatically apply stat changes.

**Scope:** Passive/continuous modifiers only (stat changes while something is equipped/active). One-time resource costs ("spend 3 hope") remain as CostTags and will be addressed by a future FeatureAction system.

**Frontend responsibility:** The backend stores modifier definitions on Features. The frontend determines which modifiers are active based on equipped items/active beastform and applies them to the character sheet display.

**Beastforms:** Existing hardcoded trait modifier fields on Beastform are kept as-is. This system applies to Features only.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Where modifiers live | On Features (many-to-many) | Features are already the universal ability mechanism across items, cards, beastforms, and classes |
| Target stats | All numeric CharacterSheet fields | Full flexibility for any stat modification |
| Activation tracking | Frontend-only | Backend stores definitions; frontend reads equipped items and applies modifiers |
| Cardinality | Reusable entity, many-to-many | Same find-or-create pattern as CostTags. A "-1 EVASION" modifier is shared across Features |
| Operations | ADD, SET, MULTIPLY | ADD covers current cases; SET and MULTIPLY provide future flexibility without schema changes |
| Resource costs | Separate future system | Passive modifiers (continuous) vs triggered costs (one-time) are fundamentally different concepts |

## New Enums

### ModifierTarget

Location: `src/main/java/com/aboff/core/model/enums/ModifierTarget.java`

```java
public enum ModifierTarget {
    AGILITY("Agility trait modifier"),
    STRENGTH("Strength trait modifier"),
    FINESSE("Finesse trait modifier"),
    INSTINCT("Instinct trait modifier"),
    PRESENCE("Presence trait modifier"),
    KNOWLEDGE("Knowledge trait modifier"),
    EVASION("Evasion score"),
    MAJOR_DAMAGE_THRESHOLD("Major damage threshold"),
    SEVERE_DAMAGE_THRESHOLD("Severe damage threshold"),
    HIT_POINT_MAX("Maximum hit points"),
    STRESS_MAX("Maximum stress"),
    HOPE_MAX("Maximum hope"),
    ARMOR_MAX("Maximum armor slots"),
    GOLD("Gold amount");

    private final String description;
    // constructor, getter
}
```

### ModifierOperation

Location: `src/main/java/com/aboff/core/model/enums/ModifierOperation.java`

```java
public enum ModifierOperation {
    ADD("Add value to the stat"),
    SET("Override stat to this value"),
    MULTIPLY("Multiply stat by this value");

    private final String description;
    // constructor, getter
}
```

## New Entity: FeatureModifier

Location: `src/main/java/com/aboff/core/model/entity/dh/FeatureModifier.java`

Table: `feature_modifiers`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, auto-generated | From BaseEntity |
| target | VARCHAR(30) | NOT NULL | ModifierTarget enum |
| operation | VARCHAR(10) | NOT NULL | ModifierOperation enum |
| value | INTEGER | NOT NULL | Modifier value (e.g., -1, 2, 15) |
| deleted_at | TIMESTAMP | nullable | Soft deletion |
| created_at | TIMESTAMP | NOT NULL | From BaseEntity |
| last_modified_at | TIMESTAMP | NOT NULL | From BaseEntity |

**Unique constraint:** `(target, operation, value)` WHERE `deleted_at IS NULL` — prevents duplicate modifier definitions.

**Relationship with Feature:** Many-to-many via `feature_feature_modifiers` junction table.

Junction table `feature_feature_modifiers`:

| Column | Type | Constraints |
|--------|------|-------------|
| feature_id | BIGINT | FK to features.id, part of composite PK |
| feature_modifier_id | BIGINT | FK to feature_modifiers.id, part of composite PK |

## New DTOs

### FeatureModifierInput (find-or-create)

Location: `src/main/java/com/aboff/core/model/dto/dh/request/FeatureModifierInput.java`

```java
public class FeatureModifierInput {
    @NotNull private ModifierTarget target;
    @NotNull private ModifierOperation operation;
    @NotNull private Integer value;
}
```

Used inline when creating/updating Features. The service looks up an existing modifier by `(target, operation, value)` or creates a new one.

### CreateFeatureModifierRequest (direct API creation)

Location: `src/main/java/com/aboff/core/model/dto/dh/request/CreateFeatureModifierRequest.java`

```java
public class CreateFeatureModifierRequest {
    @NotNull private ModifierTarget target;
    @NotNull private ModifierOperation operation;
    @NotNull private Integer value;
}
```

### FeatureModifierResponse

Location: `src/main/java/com/aboff/core/model/dto/dh/response/FeatureModifierResponse.java`

```java
public class FeatureModifierResponse {
    private Long id;
    private ModifierTarget target;
    private ModifierOperation operation;
    private Integer value;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime deletedAt;
}
```

## Changes to Existing DTOs

### CreateFeatureRequest & UpdateFeatureRequest

Add dual-input fields (same pattern as costTagIds/costTags):

```java
private List<Long> modifierIds;         // Existing modifier IDs
@Valid
private List<FeatureModifierInput> modifiers;  // Find-or-create by (target, operation, value)
```

### FeatureInput

Add the same dual-input fields:

```java
private List<Long> modifierIds;
@Valid
private List<FeatureModifierInput> modifiers;
```

### FeatureResponse

Add:

```java
private List<Long> modifierIds;                    // Always included
private List<FeatureModifierResponse> modifiers;   // Only with ?expand=modifiers
```

## New Repository: FeatureModifierRepository

Location: `src/main/java/com/aboff/core/repository/dh/FeatureModifierRepository.java`

Key methods (matching CardCostTagRepository pattern):

```java
// Find-or-create lookup
Optional<FeatureModifier> findByTargetAndOperationAndValueAndDeletedAtIsNull(target, operation, value);

// Bulk ID lookup
List<FeatureModifier> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

// Single ID lookup
Optional<FeatureModifier> findByIdAndDeletedAtIsNull(Long id);

// List all active (paginated)
Page<FeatureModifier> findAllByDeletedAtIsNull(Pageable pageable);
```

## New Service: FeatureModifierService

Location: `src/main/java/com/aboff/core/service/dh/FeatureModifierService.java`

Follows the exact CostTag service pattern:

| Method | Description |
|--------|-------------|
| `findOrCreate(FeatureModifierInput)` | Look up by `(target, operation, value)`, create if not found |
| `resolveModifiers(List<Long> ids, List<FeatureModifierInput> inputs)` | Merge ID-based + find-or-create inputs. Returns `null` if both inputs null (don't modify), empty set if clearing |
| `createModifier(CreateFeatureModifierRequest)` | Direct creation via API |
| `getModifier(Long id)` | Get single modifier |
| `getAllModifiers(Pageable)` | List all active modifiers (paginated) |
| `deleteModifier(Long id)` | Soft delete |
| `restoreModifier(Long id)` | Restore soft-deleted |
| `toResponse(FeatureModifier)` | Convert to response DTO |

## Changes to Existing Services

### FeatureService

- Inject `FeatureModifierService`
- **createFeature():** After building Feature, call `featureModifierService.resolveModifiers(request.getModifierIds(), request.getModifiers())` and set on entity (same pattern as costTags)
- **updateFeature():** Same resolution pattern, null = don't modify
- **findOrCreate():** Include modifier resolution when creating inline
- **toResponse():** Always include `modifierIds`, expand `modifiers` when requested
- **createFeaturesBulk():** Include modifier resolution per feature

## New Controller: FeatureModifierController

Location: `src/main/java/com/aboff/core/controller/dh/FeatureModifierController.java`

Base path: `/api/dh/feature-modifiers`

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/dh/feature-modifiers` | GET | Authenticated | List all modifiers (paginated) |
| `/api/dh/feature-modifiers/{id}` | GET | Authenticated | Get single modifier |
| `/api/dh/feature-modifiers` | POST | ADMIN/OWNER | Create modifier |
| `/api/dh/feature-modifiers/{id}` | DELETE | ADMIN/OWNER | Soft delete modifier |
| `/api/dh/feature-modifiers/{id}/restore` | PATCH | ADMIN/OWNER | Restore modifier |

## Changes to Existing Controllers

### FeatureController

- Add `modifiers` to the set of valid expand fields

## Database Migration

Filename: Generated via `./scripts/create-migration.sh add_feature_modifiers`

```sql
-- Feature modifiers table
CREATE TABLE feature_modifiers (
    id BIGSERIAL PRIMARY KEY,
    target VARCHAR(30) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    value INTEGER NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Unique constraint: no duplicate active modifiers
CREATE UNIQUE INDEX uq_feature_modifier_active
    ON feature_modifiers (target, operation, value)
    WHERE deleted_at IS NULL;

-- Junction table
CREATE TABLE feature_feature_modifiers (
    feature_id BIGINT NOT NULL REFERENCES features(id),
    feature_modifier_id BIGINT NOT NULL REFERENCES feature_modifiers(id),
    PRIMARY KEY (feature_id, feature_modifier_id)
);

-- Index for reverse lookups
CREATE INDEX idx_feature_feature_modifiers_modifier
    ON feature_feature_modifiers (feature_modifier_id);
```

---

## Implementation Tasks

Tasks are organized for parallel team execution. Each task includes all files to create/modify and enough detail for an agent to work independently.

### Task 1: Foundation — Enums, Migration, Entity, Repository

**Dependencies:** None
**Files to create:**
- `src/main/java/com/aboff/core/model/enums/ModifierTarget.java`
- `src/main/java/com/aboff/core/model/enums/ModifierOperation.java`
- `src/main/resources/db/migration/V{timestamp}__add_feature_modifiers.sql` (use `./scripts/create-migration.sh add_feature_modifiers`)
- `src/main/java/com/aboff/core/model/entity/dh/FeatureModifier.java`
- `src/main/java/com/aboff/core/repository/dh/FeatureModifierRepository.java`

**Files to modify:**
- `src/main/java/com/aboff/core/model/entity/dh/Feature.java` — add `@ManyToMany` `modifiers` field with join table `feature_feature_modifiers`

**Details:**
- Both enums follow the pattern of existing enums (e.g., `Trait.java`, `CostTagCategory.java`) with description field and Lombok
- FeatureModifier entity extends `BaseEntity`, includes soft deletion, uses `@Builder`
- Feature entity gets `Set<FeatureModifier> modifiers` initialized as `new HashSet<>()`, lazy fetch, join table with `feature_id` and `feature_modifier_id` columns
- Repository follows `CardCostTagRepository` pattern exactly — `@Query` methods with `deletedAt IS NULL` filters

### Task 2: DTOs

**Dependencies:** Task 1 (needs enum class names to compile, but can be written in parallel if enum names are known)
**Files to create:**
- `src/main/java/com/aboff/core/model/dto/dh/request/FeatureModifierInput.java`
- `src/main/java/com/aboff/core/model/dto/dh/request/CreateFeatureModifierRequest.java`
- `src/main/java/com/aboff/core/model/dto/dh/response/FeatureModifierResponse.java`

**Files to modify:**
- `src/main/java/com/aboff/core/model/dto/dh/request/CreateFeatureRequest.java` — add `modifierIds` and `modifiers` fields
- `src/main/java/com/aboff/core/model/dto/dh/request/UpdateFeatureRequest.java` — add `modifierIds` and `modifiers` fields
- `src/main/java/com/aboff/core/model/dto/dh/request/FeatureInput.java` — add `modifierIds` and `modifiers` fields
- `src/main/java/com/aboff/core/model/dto/dh/response/FeatureResponse.java` — add `modifierIds` and `modifiers` fields

**Details:**
- All DTOs use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` and `@JsonInclude(JsonInclude.Include.NON_NULL)` for responses
- Validation annotations: `@NotNull` on all FeatureModifierInput/CreateFeatureModifierRequest fields
- Dual-input pattern on Feature DTOs: `List<Long> modifierIds` + `@Valid List<FeatureModifierInput> modifiers`
- FeatureResponse: `modifierIds` always included, `modifiers` only with expand

### Task 3: Service Layer

**Dependencies:** Tasks 1 and 2
**Files to create:**
- `src/main/java/com/aboff/core/service/dh/FeatureModifierService.java`

**Files to modify:**
- `src/main/java/com/aboff/core/service/dh/FeatureService.java` — inject FeatureModifierService, integrate into create/update/findOrCreate/toResponse

**Details:**
- FeatureModifierService follows `CardCostTagService` pattern exactly
- `findOrCreate()` looks up by `(target, operation, value)` with `deletedAt IS NULL`, creates if not found
- `resolveModifiers()` merges ID-based and input-based, returns null when both inputs null
- FeatureService integration: call `resolveModifiers()` in `createFeature()`, `updateFeature()`, `findOrCreate()`, `createFeaturesBulk()`
- `toResponse()`: always collect `modifierIds` from the feature's modifiers set, include full `FeatureModifierResponse` list when `expand` contains `"modifiers"`

### Task 4: Controller Layer

**Dependencies:** Task 3
**Files to create:**
- `src/main/java/com/aboff/core/controller/dh/FeatureModifierController.java`

**Files to modify:**
- `src/main/java/com/aboff/core/controller/dh/FeatureController.java` — add `modifiers` to valid expand fields

**Details:**
- FeatureModifierController mirrors `CardCostTagController` structure
- Base path: `/api/dh/feature-modifiers`
- Endpoints: GET list (paginated), GET by id, POST create, DELETE soft-delete, PATCH restore
- POST/DELETE/PATCH require ADMIN or OWNER role (use `@PreAuthorize` or `RoleHierarchyService`)
- FeatureController: add `"modifiers"` to the set of recognized expand values in `parseExpand()` or wherever expand validation occurs

### Task 5: Unit Tests

**Dependencies:** Tasks 1-4
**Files to create:**
- `src/test/java/com/aboff/core/service/dh/FeatureModifierServiceTest.java`

**Files to modify:**
- `src/test/java/com/aboff/core/service/dh/FeatureServiceTest.java` — add tests for modifier integration

**FeatureModifierServiceTest cases:**
- `findOrCreate` — returns existing modifier when match found
- `findOrCreate` — creates new modifier when no match
- `resolveModifiers` — merges IDs and inputs
- `resolveModifiers` — returns null when both inputs null
- `resolveModifiers` — returns empty set when inputs non-null but empty
- `createModifier` — success
- `createModifier` — duplicate active modifier (unique constraint behavior)
- `getModifier` — found
- `getModifier` — not found (throws EntityNotFoundException)
- `deleteModifier` — soft deletes
- `restoreModifier` — restores
- `toResponse` — maps all fields correctly

**FeatureServiceTest additions:**
- Create feature with modifierIds
- Create feature with modifiers (input-based find-or-create)
- Create feature with both modifierIds and modifiers
- Update feature modifiers
- Update clearing modifiers (empty lists)
- Update with null modifiers (no change)
- toResponse includes modifierIds always
- toResponse expands modifiers when requested
- findOrCreate includes modifier resolution
- Bulk create with modifiers

### Task 6: Integration Tests

**Dependencies:** Tasks 1-4
**Files to create:**
- `src/test/java/com/aboff/core/controller/dh/FeatureModifierControllerIntegrationTest.java`

**Files to modify:**
- `src/test/java/com/aboff/core/controller/dh/FeatureControllerIntegrationTest.java` — add modifier expand tests

**FeatureModifierControllerIntegrationTest cases:**
- GET `/api/dh/feature-modifiers` — returns paginated list
- GET `/api/dh/feature-modifiers/{id}` — returns single modifier
- GET `/api/dh/feature-modifiers/{id}` — 404 for non-existent
- POST `/api/dh/feature-modifiers` — creates modifier (ADMIN)
- POST `/api/dh/feature-modifiers` — 403 for USER role
- POST `/api/dh/feature-modifiers` — 400 for invalid input (missing fields)
- DELETE `/api/dh/feature-modifiers/{id}` — soft deletes (ADMIN)
- DELETE `/api/dh/feature-modifiers/{id}` — 403 for USER role
- PATCH `/api/dh/feature-modifiers/{id}/restore` — restores (ADMIN)

**FeatureControllerIntegrationTest additions:**
- Create feature with inline modifiers
- Get feature with `?expand=modifiers`
- Update feature modifiers

---

## Testing Strategy

- **Unit tests:** Mock repository and dependent services. Follow existing patterns in `FeatureServiceTest` and `CardCostTagServiceTest`.
- **Integration tests:** Use `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Transactional`. Follow existing patterns. BCrypt strength 4 via test properties.
- **Coverage target:** Near 100% for new service and controller code.
- Tasks 5 and 6 can run in parallel since unit tests and integration tests are independent.

## Future Considerations

### FeatureAction System (Resource Costs)
A future system for machine-readable triggered actions ("spend 3 hope", "add 1 stress", "mark an armor slot"). Could reuse `ModifierTarget` enum for targeting consistency. Would have a trigger type (ON_USE, REACTION, etc.) and action type (SPEND, GAIN, MARK). This is the path toward the multi-step automation described in the original request (e.g., "Mark an Armor Slot to roll a d4 and add to Evasion").

### Multi-Step Automated Actions
The "Timeslowing" example (mark armor slot -> roll d4 -> add to evasion) represents a chain of actions. A future `FeatureActionChain` or `FeatureScript` system could model sequences of steps. The current modifier system provides the foundation — it defines **what** gets modified, while the action system would define **when** and **how**.

### Modifier Conditions
Some modifiers may eventually need conditions (e.g., "only while in beastform", "only against magic damage"). The current design keeps conditions implicit via Feature association — the frontend knows a modifier applies because the Feature's parent item is equipped. If explicit conditions are needed, a `ModifierCondition` entity could be added later.