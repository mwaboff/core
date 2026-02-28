# Spec: Inline Feature Creation on Endpoints

## Overview

Currently, endpoints that accept features require pre-existing feature IDs (`featureIds` or `featureId`). This means creating a card, weapon, armor, or adversary with new features requires two separate API calls: one to create the feature, then another to create the entity referencing that feature ID.

This spec adds the ability to pass inline feature definitions (similar to how `costTags`/`CostTagInput` works today) so that features can be created automatically as part of the parent entity's create/update request.

## Reference Pattern: CostTagInput

The existing inline cost tag creation pattern serves as the exact template for this work.

**CostTagInput DTO** (`src/main/java/com/aboff/core/model/dto/dh/request/CostTagInput.java`):
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CostTagInput {
    @NotBlank private String label;
    @NotNull private CostTagCategory category;
}
```

**CardCostTagService.resolveCostTags()** (`src/main/java/com/aboff/core/service/dh/CardCostTagService.java`):
- Takes both `List<Long> costTagIds` and `List<CostTagInput> costTags`
- Returns `null` if both are null (signals "don't modify" for updates)
- Returns empty set if both are provided but empty (signals "clear tags")
- Merges ID-based lookups with find-or-create label-based lookups

**CardCostTagService.findOrCreate()**:
- Looks up by label (case-insensitive) first
- Creates new entity only if no match found
- Returns the existing or newly created entity

---

## Changes Required

### 1. New DTO: `FeatureInput`

**File:** `src/main/java/com/aboff/core/model/dto/dh/request/FeatureInput.java` (NEW)

```java
package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.FeatureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Input DTO for finding or creating a feature by name.
 * Used in card, item, and adversary create/update requests to allow clients to specify
 * features inline instead of (or in addition to) existing feature IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureInput {

    /** Name of the feature. Matched case-insensitively against existing features within the same expansion and type. */
    @NotBlank(message = "Feature name is required")
    @Size(max = 200, message = "Feature name must not exceed 200 characters")
    private String name;

    /** Detailed description of what the feature does. */
    private String description;

    /** Type/category of the feature. */
    @NotNull(message = "Feature type is required")
    private FeatureType featureType;

    /** ID of the expansion this feature belongs to. */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /** IDs of cost tags associated with this feature. */
    private List<Long> costTagIds;

    /** Cost tags to find or create by label. Merged with costTagIds if both provided. */
    @Valid
    private List<CostTagInput> costTags;
}
```

**Design decision — matching strategy:** Features are matched by `name` (case-insensitive) + `expansionId` + `featureType`. Unlike cost tags which only have a label, features have multiple identifying fields. Matching on all three prevents false matches (e.g., two different expansions could each have a feature named "Quick Strike" with different descriptions).

---

### 2. New Repository Method: `FeatureRepository`

**File:** `src/main/java/com/aboff/core/repository/dh/FeatureRepository.java`

Add:
```java
/**
 * Finds a non-deleted feature by name (case-insensitive), expansion, and feature type.
 */
@Query("SELECT f FROM Feature f WHERE LOWER(f.name) = LOWER(:name) " +
       "AND f.expansion.id = :expansionId " +
       "AND f.featureType = :featureType " +
       "AND f.deletedAt IS NULL")
Optional<Feature> findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDeletedAtIsNull(
        @Param("name") String name,
        @Param("expansionId") Long expansionId,
        @Param("featureType") FeatureType featureType);
```

---

### 3. New Service Methods: `FeatureService`

**File:** `src/main/java/com/aboff/core/service/dh/FeatureService.java`

Add two new methods following the `CardCostTagService` pattern:

```java
/**
 * Finds an existing feature by name+expansion+type (case-insensitive) or creates a new one.
 */
@Transactional
public Feature findOrCreate(FeatureInput input) {
    return featureRepository
            .findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDeletedAtIsNull(
                    input.getName(), input.getExpansionId(), input.getFeatureType())
            .map(existing -> {
                log.debug("Found existing feature with name '{}' (id: {})", input.getName(), existing.getId());
                return existing;
            })
            .orElseGet(() -> {
                log.info("Creating new feature with name '{}', type '{}', expansion '{}'",
                        input.getName(), input.getFeatureType(), input.getExpansionId());
                Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(input.getExpansionId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Expansion not found with id: " + input.getExpansionId()));
                Feature feature = Feature.builder()
                        .name(input.getName())
                        .description(input.getDescription())
                        .featureType(input.getFeatureType())
                        .expansion(expansion)
                        .build();
                Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(
                        input.getCostTagIds(), input.getCostTags());
                if (resolvedTags != null) {
                    feature.setCostTags(resolvedTags);
                }
                return featureRepository.save(feature);
            });
}

/**
 * Resolves features from both ID-based and input-based sources, merging the results.
 * Returns null when both inputs are null (signals "don't modify" for updates).
 * Returns empty set when at least one input is non-null but both are empty (signals "clear features").
 */
@Transactional
public Set<Feature> resolveFeatures(List<Long> featureIds, List<FeatureInput> features) {
    if (featureIds == null && features == null) {
        return null;
    }

    Set<Feature> resolved = new HashSet<>();

    if (featureIds != null && !featureIds.isEmpty()) {
        resolved.addAll(featureRepository.findAllByIdInAndDeletedAtIsNull(featureIds));
    }

    if (features != null && !features.isEmpty()) {
        for (FeatureInput input : features) {
            resolved.add(findOrCreate(input));
        }
    }

    return resolved;
}
```

Also add an overload for single-feature resolution (used by Weapon/Armor):

```java
/**
 * Resolves a single feature from either an ID or an inline input.
 * Returns null when both inputs are null (signals "don't modify" for updates).
 */
@Transactional
public Feature resolveFeature(Long featureId, FeatureInput feature) {
    if (featureId == null && feature == null) {
        return null;
    }

    if (featureId != null) {
        return featureRepository.findByIdAndDeletedAtIsNull(featureId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Feature not found with id: " + featureId));
    }

    return findOrCreate(feature);
}
```

---

### 4. Request DTO Updates

Each request DTO that currently has `featureIds` (or `featureId`) gets a new companion field `features` (or `feature`).

#### Multi-feature DTOs (Cards + Adversary)

**Files to update (add `features` field):**

| File | Current field | New field to add |
|------|--------------|-----------------|
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateAncestryCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateAncestryCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateCommunityCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateCommunityCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateDomainCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateDomainCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateSubclassCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateSubclassCardRequest.java` | `List<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateAdversaryRequest.java` | `Set<Long> featureIds` | `List<FeatureInput> features` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateAdversaryRequest.java` | `Set<Long> featureIds` | `List<FeatureInput> features` |

**Pattern for each multi-feature DTO** (example using CreateAncestryCardRequest):
```java
// Existing field — keep as-is
private List<Long> featureIds;

// New field to add
/** Features to find or create inline. Merged with featureIds if both provided. */
@Valid
private List<FeatureInput> features;
```

#### Single-feature DTOs (Weapon + Armor)

**Files to update (add `feature` field — singular):**

| File | Current field | New field to add |
|------|--------------|-----------------|
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateWeaponRequest.java` | `Long featureId` | `FeatureInput feature` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateWeaponRequest.java` | `Long featureId` | `FeatureInput feature` |
| `src/main/java/com/aboff/core/model/dto/dh/request/CreateArmorRequest.java` | `Long featureId` | `FeatureInput feature` |
| `src/main/java/com/aboff/core/model/dto/dh/request/UpdateArmorRequest.java` | `Long featureId` | `FeatureInput feature` |

**Pattern for each single-feature DTO** (example using CreateWeaponRequest):
```java
// Existing field — keep as-is
private Long featureId;

// New field to add
/** Feature to find or create inline. Used if featureId is not provided. featureId takes precedence. */
@Valid
private FeatureInput feature;
```

---

### 5. Service Updates

Each service that resolves features needs to be updated to call `FeatureService.resolveFeatures()` (or `resolveFeature()`) instead of directly querying `FeatureRepository`.

#### Multi-feature services

**Files to update:**

| Service file | Methods to update |
|---|---|
| `src/main/java/com/aboff/core/service/dh/AncestryCardService.java` | `createAncestryCard()`, `updateAncestryCard()`, `createAncestryCardsBulk()` |
| `src/main/java/com/aboff/core/service/dh/CommunityCardService.java` | `createCommunityCard()`, `updateCommunityCard()`, `createCommunityCardsBulk()` |
| `src/main/java/com/aboff/core/service/dh/DomainCardService.java` | `createDomainCard()`, `updateDomainCard()`, `createDomainCardsBulk()` |
| `src/main/java/com/aboff/core/service/dh/SubclassCardService.java` | `createSubclassCard()`, `updateSubclassCard()`, `createSubclassCardsBulk()` |
| `src/main/java/com/aboff/core/service/dh/AdversaryService.java` | `createAdversary()`, `updateAdversary()`, `batchCreateAdversaries()` |

**Each service needs:**
1. Inject `FeatureService` (add to constructor / `@RequiredArgsConstructor` fields)
2. Replace direct `featureRepository` feature resolution with `featureService.resolveFeatures()`

**Before (current pattern in card services):**
```java
if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
    Set<Feature> features = new HashSet<>(
        featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
    card.setFeatures(features);
}
```

**After (new pattern):**
```java
Set<Feature> resolvedFeatures = featureService.resolveFeatures(
    request.getFeatureIds(), request.getFeatures());
if (resolvedFeatures != null) {
    card.setFeatures(resolvedFeatures);
}
```

#### Single-feature services

**Files to update:**

| Service file | Methods to update |
|---|---|
| `src/main/java/com/aboff/core/service/dh/WeaponService.java` | `createWeapon()`, `updateWeapon()`, `createWeaponsBulk()` |
| `src/main/java/com/aboff/core/service/dh/ArmorService.java` | `createArmor()`, `updateArmor()`, `createArmorsBulk()` |

**Each service needs:**
1. Inject `FeatureService`
2. Replace direct `featureRepository` feature resolution with `featureService.resolveFeature()`

**Before (current pattern in weapon/armor services):**
```java
if (request.getFeatureId() != null) {
    Feature feature = featureRepository.findByIdAndDeletedAtIsNull(request.getFeatureId())
            .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + request.getFeatureId()));
    weapon.setFeature(feature);
}
```

**After (new pattern):**
```java
Feature resolvedFeature = featureService.resolveFeature(
    request.getFeatureId(), request.getFeature());
if (resolvedFeature != null) {
    weapon.setFeature(resolvedFeature);
}
```

**Note on clearing:** For update endpoints, if `featureId` is `null` AND `feature` is `null`, the current behavior (don't modify) is preserved. To explicitly clear a feature on an item, the client would need to send a request that signals clearing — this matches the existing behavior where not sending `featureId` means "don't change it."

---

### 6. Dependency Injection Updates

Services that currently inject `FeatureRepository` directly for feature resolution should switch to injecting `FeatureService`. The following services need their constructor dependencies updated:

| Service | Remove dependency | Add dependency |
|---|---|---|
| `AncestryCardService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `CommunityCardService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `DomainCardService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `SubclassCardService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `WeaponService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `ArmorService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |
| `AdversaryService` | `FeatureRepository` (if only used for feature resolution) | `FeatureService` |

**Note:** Only remove `FeatureRepository` if no other methods in the service use it directly. If the service uses `FeatureRepository` for other queries, keep both.

---

## Affected Endpoints Summary

| Endpoint | Method | Feature cardinality | DTO to update |
|---|---|---|---|
| `/api/dh/cards/ancestry` | POST | Multiple | `CreateAncestryCardRequest` |
| `/api/dh/cards/ancestry/{id}` | PUT | Multiple | `UpdateAncestryCardRequest` |
| `/api/dh/cards/ancestry/bulk` | POST | Multiple | `CreateAncestryCardRequest` (list) |
| `/api/dh/cards/community` | POST | Multiple | `CreateCommunityCardRequest` |
| `/api/dh/cards/community/{id}` | PUT | Multiple | `UpdateCommunityCardRequest` |
| `/api/dh/cards/community/bulk` | POST | Multiple | `CreateCommunityCardRequest` (list) |
| `/api/dh/cards/domain` | POST | Multiple | `CreateDomainCardRequest` |
| `/api/dh/cards/domain/{id}` | PUT | Multiple | `UpdateDomainCardRequest` |
| `/api/dh/cards/domain/bulk` | POST | Multiple | `CreateDomainCardRequest` (list) |
| `/api/dh/cards/subclass` | POST | Multiple | `CreateSubclassCardRequest` |
| `/api/dh/cards/subclass/{id}` | PUT | Multiple | `UpdateSubclassCardRequest` |
| `/api/dh/cards/subclass/bulk` | POST | Multiple | `CreateSubclassCardRequest` (list) |
| `/api/dh/weapons` | POST | Single | `CreateWeaponRequest` |
| `/api/dh/weapons/{id}` | PUT | Single | `UpdateWeaponRequest` |
| `/api/dh/weapons/bulk` | POST | Single | `CreateWeaponRequest` (list) |
| `/api/dh/armors` | POST | Single | `CreateArmorRequest` |
| `/api/dh/armors/{id}` | PUT | Single | `UpdateArmorRequest` |
| `/api/dh/armors/bulk` | POST | Single | `CreateArmorRequest` (list) |
| `/api/dh/adversaries` | POST | Multiple | `CreateAdversaryRequest` |
| `/api/dh/adversaries/{id}` | PUT | Multiple | `UpdateAdversaryRequest` |
| `/api/dh/adversaries/batch` | POST | Multiple | `CreateAdversaryRequest` (list) |

---

## Example curl Bodies

### Creating an Ancestry Card with inline features (POST /api/dh/cards/ancestry)

**Option 1: Existing feature IDs only (current behavior, unchanged)**
```json
{
  "name": "Ribbet Ancestry",
  "description": "Frog-like humanoids",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [10, 11]
}
```

**Option 2: Inline features only (new)**
```json
{
  "name": "Ribbet Ancestry",
  "description": "Frog-like humanoids",
  "expansionId": 1,
  "isOfficial": true,
  "features": [
    {
      "name": "Mighty Leap",
      "description": "You can jump great distances, clearing up to 30 feet in a single bound.",
      "featureType": "ANCESTRY",
      "expansionId": 1
    },
    {
      "name": "Amphibious",
      "description": "You can breathe underwater and have a swim speed equal to your movement speed.",
      "featureType": "ANCESTRY",
      "expansionId": 1,
      "costTags": [
        { "label": "1/session", "category": "LIMITATION" }
      ]
    }
  ]
}
```

**Option 3: Mixed — some existing IDs, some new inline (new)**
```json
{
  "name": "Ribbet Ancestry",
  "description": "Frog-like humanoids",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [10],
  "features": [
    {
      "name": "Mighty Leap",
      "description": "You can jump great distances.",
      "featureType": "ANCESTRY",
      "expansionId": 1
    }
  ]
}
```

### Creating a Weapon with inline feature (POST /api/dh/weapons)

**Option 1: Existing feature ID (current behavior, unchanged)**
```json
{
  "name": "Flaming Sword",
  "description": "A sword wreathed in magical flame",
  "expansionId": 1,
  "isOfficial": true,
  "trait": "STRENGTH",
  "damageAmount": 2,
  "damageType": "PHYSICAL",
  "featureId": 15
}
```

**Option 2: Inline feature (new)**
```json
{
  "name": "Flaming Sword",
  "description": "A sword wreathed in magical flame",
  "expansionId": 1,
  "isOfficial": true,
  "trait": "STRENGTH",
  "damageAmount": 2,
  "damageType": "PHYSICAL",
  "feature": {
    "name": "Flame Burst",
    "description": "Once per rest, deal an additional d8 fire damage on a successful hit.",
    "featureType": "OTHER",
    "expansionId": 1,
    "costTags": [
      { "label": "1/rest", "category": "LIMITATION" }
    ]
  }
}
```

### Updating an Adversary with inline features (PUT /api/dh/adversaries/5)

```json
{
  "name": "Shadow Drake",
  "description": "A mid-tier drake corrupted by shadow magic",
  "expansionId": 1,
  "isOfficial": false,
  "featureIds": [20],
  "features": [
    {
      "name": "Shadow Breath",
      "description": "Exhales a cone of shadow energy dealing 2d10 damage.",
      "featureType": "OTHER",
      "expansionId": 1,
      "costTags": [
        { "label": "Recharge 5-6", "category": "LIMITATION" }
      ]
    }
  ]
}
```

### Bulk creating Domain Cards with inline features (POST /api/dh/cards/domain/bulk)

```json
[
  {
    "name": "Arcane Ward",
    "description": "A protective magical barrier",
    "expansionId": 1,
    "isOfficial": true,
    "features": [
      {
        "name": "Shield of Force",
        "description": "Gain +2 to armor score until end of next turn.",
        "featureType": "DOMAIN",
        "expansionId": 1
      }
    ]
  },
  {
    "name": "Eldritch Blast",
    "description": "A bolt of arcane energy",
    "expansionId": 1,
    "isOfficial": true,
    "featureIds": [30]
  }
]
```

---

## Testing Strategy

### Overview

All new and modified code must have near 100% logic coverage. Tests follow the project conventions:
- **Unit tests:** `{ClassName}Test` using `@ExtendWith(MockitoExtension.class)`, Arrange-Act-Assert, AssertJ assertions
- **Integration tests:** `{ClassName}IntegrationTest` using `@SpringBootTest` + `@AutoConfigureMockMvc` + `@TestPropertySource(locations = "classpath:application-test.properties")` + `@Transactional`
- No `@MockBean`, `@DirtiesContext`, or `deleteAll()` cleanup — rely on `@Transactional` rollback
- BCrypt strength = 4 in tests (never override)

### Test Files to Create or Modify

| Test file | Action | Type |
|---|---|---|
| `src/test/java/com/aboff/core/service/dh/FeatureServiceTest.java` | **Modify** — add tests for `findOrCreate()`, `resolveFeatures()`, `resolveFeature()` | Unit |
| `src/test/java/com/aboff/core/service/dh/AncestryCardServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/CommunityCardServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/DomainCardServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/SubclassCardServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/WeaponServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/ArmorServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/service/dh/AdversaryServiceTest.java` | **Modify** — update feature resolution tests | Unit |
| `src/test/java/com/aboff/core/controller/dh/AncestryCardControllerIntegrationTest.java` | **Modify** — add inline feature integration tests | Integration |
| `src/test/java/com/aboff/core/controller/dh/WeaponControllerIntegrationTest.java` | **Modify** — add inline feature integration tests | Integration |
| `src/test/java/com/aboff/core/controller/dh/AdversaryControllerIntegrationTest.java` | **Modify** — add inline feature integration tests | Integration |

### Unit Tests: FeatureService (new methods)

Add these tests to `FeatureServiceTest.java`. Follow the existing test structure: `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service, Arrange-Act-Assert pattern.

**The mock for `FeatureRepository` needs the new method:**
```java
when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDeletedAtIsNull(
        eq("Mighty Leap"), eq(1L), eq(FeatureType.ANCESTRY)))
    .thenReturn(Optional.of(existingFeature));
```

#### `findOrCreate()` tests

| Test name | Scenario | Key assertions |
|---|---|---|
| `findOrCreate_ExistingFeature_ReturnsExisting` | Feature with matching name+expansion+type exists | Returns existing feature, `save()` never called |
| `findOrCreate_ExistingFeatureCaseInsensitive_ReturnsExisting` | Name differs only in case ("mighty leap" vs "Mighty Leap") | Returns existing feature, case-insensitive match works |
| `findOrCreate_NoMatch_CreatesNewFeature` | No feature matches | Calls `expansionRepository.findByIdAndDeletedAtIsNull()`, calls `featureRepository.save()`, returns saved feature |
| `findOrCreate_NoMatch_WithCostTags_CreatesWithTags` | No match, input includes costTagIds and costTags | Calls `cardCostTagService.resolveCostTags()`, saved feature has costTags set |
| `findOrCreate_NoMatch_WithoutCostTags_CreatesWithoutTags` | No match, input has no cost tag fields | `cardCostTagService.resolveCostTags()` returns null, costTags not set |
| `findOrCreate_ExpansionNotFound_ThrowsEntityNotFoundException` | Expansion ID doesn't exist | Throws `EntityNotFoundException` with expansion message |

**Example test (template for the pattern):**
```java
@Test
void findOrCreate_ExistingFeature_ReturnsExisting() {
    // Arrange
    Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
    Feature existingFeature = Feature.builder()
            .id(5L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
            .expansion(expansion).build();
    FeatureInput input = FeatureInput.builder()
            .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(1L)
            .description("Jump far").build();

    when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDeletedAtIsNull(
            "Mighty Leap", 1L, FeatureType.ANCESTRY))
        .thenReturn(Optional.of(existingFeature));

    // Act
    Feature result = featureService.findOrCreate(input);

    // Assert
    assertThat(result).isEqualTo(existingFeature);
    assertThat(result.getId()).isEqualTo(5L);
    verify(featureRepository, never()).save(any());
    verify(expansionRepository, never()).findByIdAndDeletedAtIsNull(any());
}
```

#### `resolveFeatures()` tests (multi-feature)

| Test name | Scenario | Key assertions |
|---|---|---|
| `resolveFeatures_BothNull_ReturnsNull` | `featureIds=null`, `features=null` | Returns `null` (signals "don't modify" for updates) |
| `resolveFeatures_BothEmpty_ReturnsEmptySet` | `featureIds=[]`, `features=[]` | Returns empty `Set<Feature>` (signals "clear features") |
| `resolveFeatures_IdsOnly_ResolvesById` | `featureIds=[1,2]`, `features=null` | Calls `findAllByIdInAndDeletedAtIsNull()`, returns matched features |
| `resolveFeatures_InputsOnly_FindsOrCreatesEach` | `featureIds=null`, `features=[input1,input2]` | Calls `findOrCreate()` for each input |
| `resolveFeatures_BothProvided_MergesResults` | `featureIds=[1]`, `features=[input1]` | Returns union of ID-resolved and input-resolved features |
| `resolveFeatures_DuplicatesBetweenIdsAndInputs_Deduplicates` | Same feature resolved by ID and by input | Set deduplicates, size is 1 not 2 |
| `resolveFeatures_IdsNotFound_ReturnsOnlyFound` | `featureIds=[1,999]`, feature 999 doesn't exist | Returns only feature 1 (silent skip for missing IDs, matching existing behavior) |

#### `resolveFeature()` tests (single feature — Weapon/Armor)

| Test name | Scenario | Key assertions |
|---|---|---|
| `resolveFeature_BothNull_ReturnsNull` | `featureId=null`, `feature=null` | Returns `null` |
| `resolveFeature_IdProvided_ResolvesById` | `featureId=5`, `feature=null` | Calls `findByIdAndDeletedAtIsNull(5)`, returns feature |
| `resolveFeature_IdNotFound_ThrowsEntityNotFoundException` | `featureId=999`, doesn't exist | Throws `EntityNotFoundException` |
| `resolveFeature_InputProvided_FindsOrCreates` | `featureId=null`, `feature=input` | Calls `findOrCreate(input)`, returns result |
| `resolveFeature_BothProvided_IdTakesPrecedence` | `featureId=5`, `feature=input` | Resolves by ID, never calls `findOrCreate()` |

### Unit Tests: Consuming Services (updated mocks)

Each consuming service test needs to be updated because the dependency changes from `FeatureRepository` to `FeatureService`. The mock setup changes:

**Before (current mock pattern):**
```java
@Mock
private FeatureRepository featureRepository;

// In test:
when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
    .thenReturn(List.of(feature));
```

**After (new mock pattern):**
```java
@Mock
private FeatureService featureService;

// In test:
when(featureService.resolveFeatures(eq(List.of(1L)), isNull()))
    .thenReturn(Set.of(feature));
```

#### Tests to add per multi-feature service (AncestryCard, CommunityCard, DomainCard, SubclassCard, Adversary)

For each service, add to both `create` and `update` test groups:

| Test name pattern | Scenario |
|---|---|
| `create{Entity}_WithInlineFeatures_CreatesWithResolvedFeatures` | `features` field populated, `featureIds` null |
| `create{Entity}_WithMixedFeatures_MergesIdsAndInline` | Both `featureIds` and `features` populated |
| `update{Entity}_WithInlineFeatures_UpdatesFeatures` | Update request with `features` field |
| `update{Entity}_WithNullFeatures_DoesNotModifyFeatures` | Both `featureIds` and `features` null → features unchanged |
| `update{Entity}_WithEmptyFeatures_ClearsFeatures` | Empty `featureIds=[]` → features cleared |
| `create{Entity}Bulk_WithInlineFeatures_CreatesAll` | Bulk create with inline features in some items |

**Example test (AncestryCardService):**
```java
@Test
void createAncestryCard_WithInlineFeatures_CreatesWithResolvedFeatures() {
    // Arrange
    Feature resolvedFeature = Feature.builder()
            .id(1L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
            .expansion(testExpansion).build();

    CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
            .name("Ribbet")
            .expansionId(1L)
            .isOfficial(true)
            .features(List.of(
                FeatureInput.builder()
                    .name("Mighty Leap")
                    .featureType(FeatureType.ANCESTRY)
                    .expansionId(1L)
                    .build()
            ))
            .build();

    when(featureService.resolveFeatures(isNull(), eq(request.getFeatures())))
        .thenReturn(Set.of(resolvedFeature));
    // ... other mocks ...

    // Act
    AncestryCardResponse result = ancestryCardService.createAncestryCard(request);

    // Assert
    verify(featureService).resolveFeatures(isNull(), eq(request.getFeatures()));
    // ... verify card has features set ...
}
```

#### Tests to add per single-feature service (Weapon, Armor)

| Test name pattern | Scenario |
|---|---|
| `create{Entity}_WithInlineFeature_CreatesWithResolvedFeature` | `feature` field populated, `featureId` null |
| `create{Entity}_WithBothIdAndInline_IdTakesPrecedence` | Both `featureId` and `feature` populated |
| `update{Entity}_WithInlineFeature_UpdatesFeature` | Update with `feature` field |
| `update{Entity}_WithNullFeature_DoesNotModifyFeature` | Both null → feature unchanged |
| `create{Entity}Bulk_WithInlineFeature_CreatesAll` | Bulk create with inline feature in some items |

### Integration Tests

Add integration tests to validate the full request-to-database flow. These go in the existing `*ControllerIntegrationTest` files. Use the existing test setup pattern (admin user, token, expansion created in `@BeforeEach`).

#### AncestryCardControllerIntegrationTest (representative for all multi-feature card endpoints)

| Test name | What it validates |
|---|---|
| `createAncestryCard_WithInlineFeatures_Returns201AndCreatesFeatures` | POST with `features` array → 201, features exist in DB |
| `createAncestryCard_WithMixedFeatures_Returns201AndMerges` | POST with both `featureIds` and `features` → both resolved |
| `createAncestryCard_WithInlineFeaturesHavingCostTags_Returns201` | Inline features include nested `costTags` → features and cost tags all created |
| `createAncestryCard_WithInvalidFeatureInput_Returns400` | `features` with missing required fields → 400 validation error |
| `createAncestryCard_WithInlineFeatureInvalidExpansion_Returns404` | Feature references non-existent expansion → 404 |
| `updateAncestryCard_WithInlineFeatures_Returns200` | PUT with `features` → existing card gets new features |
| `createAncestryCardBulk_WithInlineFeatures_Returns201` | Bulk POST with inline features in some items |

**Example integration test:**
```java
@Test
void createAncestryCard_WithInlineFeatures_Returns201AndCreatesFeatures() throws Exception {
    // Arrange
    String requestJson = """
        {
            "name": "Ribbet Ancestry",
            "description": "Frog-like humanoids",
            "expansionId": %d,
            "isOfficial": true,
            "features": [
                {
                    "name": "Mighty Leap",
                    "description": "Jump great distances",
                    "featureType": "ANCESTRY",
                    "expansionId": %d
                },
                {
                    "name": "Amphibious",
                    "description": "Breathe underwater",
                    "featureType": "ANCESTRY",
                    "expansionId": %d
                }
            ]
        }
        """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

    // Act & Assert
    mockMvc.perform(post("/api/dh/cards/ancestry")
                    .cookie(new Cookie("AUTH_TOKEN", adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.featureIds").isArray())
            .andExpect(jsonPath("$.featureIds.length()").value(2));

    // Verify features were actually created in the database
    List<Feature> features = featureRepository.findAll();
    assertThat(features).hasSize(2);
    assertThat(features).extracting(Feature::getName)
            .containsExactlyInAnyOrder("Mighty Leap", "Amphibious");
}
```

#### WeaponControllerIntegrationTest (representative for single-feature endpoints)

| Test name | What it validates |
|---|---|
| `createWeapon_WithInlineFeature_Returns201AndCreatesFeature` | POST with `feature` object → 201, feature created in DB |
| `createWeapon_WithBothIdAndInline_IdTakesPrecedence` | Both `featureId` and `feature` → resolves by ID |
| `createWeapon_WithInlineFeatureHavingCostTags_Returns201` | Nested cost tags resolved correctly |
| `updateWeapon_WithInlineFeature_Returns200` | PUT with `feature` → weapon gets new feature |

**Example integration test:**
```java
@Test
void createWeapon_WithInlineFeature_Returns201AndCreatesFeature() throws Exception {
    String requestJson = """
        {
            "name": "Flaming Sword",
            "description": "A sword wreathed in flame",
            "expansionId": %d,
            "isOfficial": true,
            "trait": "STRENGTH",
            "damageAmount": 2,
            "damageType": "PHYSICAL",
            "feature": {
                "name": "Flame Burst",
                "description": "Deal extra fire damage",
                "featureType": "OTHER",
                "expansionId": %d,
                "costTags": [
                    { "label": "1/rest", "category": "LIMITATION" }
                ]
            }
        }
        """.formatted(testExpansion.getId(), testExpansion.getId());

    mockMvc.perform(post("/api/dh/weapons")
                    .cookie(new Cookie("AUTH_TOKEN", adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.featureId").isNumber());

    // Verify feature and cost tag were created
    List<Feature> features = featureRepository.findAll();
    assertThat(features).hasSize(1);
    assertThat(features.get(0).getName()).isEqualTo("Flame Burst");
    assertThat(features.get(0).getCostTags()).hasSize(1);
}
```

#### Find-or-Create Idempotency Integration Test

This is a critical behavioral test — add to `AncestryCardControllerIntegrationTest`:

| Test name | What it validates |
|---|---|
| `createAncestryCard_SameInlineFeatureTwice_ReusesSameFeature` | Two cards with identical inline feature → same feature ID on both |

```java
@Test
void createAncestryCard_SameInlineFeatureTwice_ReusesSameFeature() throws Exception {
    // Arrange — same inline feature in two separate requests
    String featureJson = """
        { "name": "Mighty Leap", "featureType": "ANCESTRY", "expansionId": %d }
        """.formatted(testExpansion.getId());
    String request1 = """
        { "name": "Card A", "expansionId": %d, "isOfficial": true, "features": [%s] }
        """.formatted(testExpansion.getId(), featureJson);
    String request2 = """
        { "name": "Card B", "expansionId": %d, "isOfficial": true, "features": [%s] }
        """.formatted(testExpansion.getId(), featureJson);

    // Act — create two cards
    MvcResult result1 = mockMvc.perform(post("/api/dh/cards/ancestry")
                    .cookie(new Cookie("AUTH_TOKEN", adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request1))
            .andExpect(status().isCreated())
            .andReturn();

    MvcResult result2 = mockMvc.perform(post("/api/dh/cards/ancestry")
                    .cookie(new Cookie("AUTH_TOKEN", adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request2))
            .andExpect(status().isCreated())
            .andReturn();

    // Assert — only one feature created, shared by both cards
    assertThat(featureRepository.findAll()).hasSize(1);

    JsonNode card1 = objectMapper.readTree(result1.getResponse().getContentAsString());
    JsonNode card2 = objectMapper.readTree(result2.getResponse().getContentAsString());
    assertThat(card1.get("featureIds").get(0).asLong())
            .isEqualTo(card2.get("featureIds").get(0).asLong());
}
```

### Edge Cases That Must Be Tested

Per project testing rules, the following edge cases are required:

| Category | Test scenario | Where to test |
|---|---|---|
| **Null** | `features` field is null → features not modified (update) | Unit: all services |
| **Null** | `FeatureInput.description` is null (optional field) | Unit: `findOrCreate()` |
| **Empty** | `features` is empty list `[]` → features cleared (update) | Unit: `resolveFeatures()` |
| **Empty** | `featureIds` is empty + `features` is empty → clear all | Unit: `resolveFeatures()` |
| **Invalid types** | `featureType` is invalid enum value | Integration: 400 response |
| **Boundaries** | Feature name at 200 chars (max) | Unit: validation passes |
| **Boundaries** | Feature name at 201 chars (over max) | Integration: 400 response |
| **Special characters** | Feature name with unicode/emoji/SQL chars | Integration: creates successfully, no injection |
| **Error: not found** | `FeatureInput.expansionId` references non-existent expansion | Unit: throws `EntityNotFoundException` |
| **Error: not found** | `featureIds` contains non-existent ID | Unit: silent skip (matching existing pattern) |
| **Validation** | `FeatureInput` missing required `name` | Integration: 400 with validation message |
| **Validation** | `FeatureInput` missing required `featureType` | Integration: 400 with validation message |
| **Validation** | `FeatureInput` missing required `expansionId` | Integration: 400 with validation message |
| **Deduplication** | Same feature referenced by ID and inline input | Unit: `resolveFeatures()` returns single entry |
| **Case sensitivity** | "mighty leap" matches existing "Mighty Leap" | Unit: `findOrCreate()` returns existing |
| **Nested creation** | Inline feature with inline costTags (nested find-or-create) | Integration: both feature and cost tags created |

### Existing Tests That Need Updating

When switching from `FeatureRepository` to `FeatureService` injection in consuming services, all existing tests that mock `FeatureRepository` for feature resolution must be updated to mock `FeatureService` instead. This is a mock-swap change:

**Per service, update these existing test methods:**

| Service test file | Existing tests to update (mock swap) |
|---|---|
| `AncestryCardServiceTest` | `createAncestryCard_WithFeatures_*`, `updateAncestryCard_WithFeatures_*`, `createAncestryCardsBulk_*` |
| `CommunityCardServiceTest` | Same pattern as above |
| `DomainCardServiceTest` | Same pattern as above |
| `SubclassCardServiceTest` | Same pattern as above |
| `WeaponServiceTest` | `createWeapon_WithFeature_*`, `updateWeapon_WithFeature_*`, `createWeaponsBulk_*` |
| `ArmorServiceTest` | Same pattern as Weapon |
| `AdversaryServiceTest` | `createAdversary_WithFeatures_*`, `updateAdversary_WithFeatures_*`, `batchCreateAdversaries_*` |

### Test Execution & Validation

After all changes:
1. Run full test suite: `./mvnw test`
2. All tests must pass (green)
3. No skipped or disabled tests
4. Verify new tests appear in output (check count increased)
5. Run unit tests only to confirm speed: `./mvnw test -Dtest="*Test"` (should be < 50s)

---

## Implementation Order

1. Create `FeatureInput` DTO
2. Add `findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDeletedAtIsNull` to `FeatureRepository`
3. Add `findOrCreate()`, `resolveFeatures()`, and `resolveFeature()` to `FeatureService`
4. Write unit tests for new `FeatureService` methods (step 3 code)
5. Add `features`/`feature` field to all request DTOs (14 DTOs)
6. Update card services (Ancestry, Community, Domain, Subclass) — create, update, bulk methods
7. Update item services (Weapon, Armor) — create, update, bulk methods
8. Update adversary service — create, update, batch methods
9. Update unit tests for all consuming services (mock swap + new test cases)
10. Write/update integration tests
11. Run full test suite: `./mvnw test` — all green
12. Validate build: `./mvnw compile`