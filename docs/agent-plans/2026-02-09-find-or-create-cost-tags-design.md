# Plan: Find-or-Create Cost Tags by Label

## Context

Currently, when creating or updating cards, clients must provide `costTagIds` — a list of pre-existing tag IDs. This requires the client to first query tags, then create any missing ones, then finally create the card. The goal is to allow clients to pass tag labels directly as strings. The system will look up existing tags by label (case-insensitive) and auto-create any that don't exist, simplifying the client workflow. The existing `costTagIds` field remains supported for backwards compatibility. Both fields can be provided in the same request (results are merged).

This pattern is designed to be reusable for other child entities like Features in the future, though only CardCostTag is in scope for this effort.

---

## Changes Overview

| # | Component | Type | File |
|---|-----------|------|------|
| 1 | Flyway migration | New | `src/main/resources/db/migration/V{timestamp}__add_ci_unique_label_cost_tags.sql` |
| 2 | `CostTagInput` DTO | New | `src/main/java/com/aboff/core/model/dto/dh/request/CostTagInput.java` |
| 3 | `CardCostTagRepository` | Modify | `src/main/java/com/aboff/core/repository/dh/CardCostTagRepository.java` |
| 4 | `CardCostTagService` | Modify | `src/main/java/com/aboff/core/service/dh/CardCostTagService.java` |
| 5 | 8 Card Request DTOs | Modify | `src/main/java/com/aboff/core/model/dto/dh/request/Create*CardRequest.java` and `Update*CardRequest.java` |
| 6 | 4 Card Services | Modify | `src/main/java/com/aboff/core/service/dh/*CardService.java` |
| 7 | Tests | New + Modify | Unit and integration tests for all changed components |

---

## Step 1: Database Migration — Case-Insensitive Unique Index

**File:** `src/main/resources/db/migration/V{timestamp}__add_ci_unique_label_cost_tags.sql` (via `./scripts/create-migration.sh`)

**Changes:**
- Drop the existing case-sensitive unique constraint `uq_card_cost_tags_label`
- Add a new unique index on `LOWER(label)` where `deleted_at IS NULL` (partial unique index — only enforces uniqueness among active tags)
- Drop the now-redundant `idx_card_cost_tags_label` plain index

```sql
-- Replace case-sensitive unique constraint with case-insensitive unique index
ALTER TABLE card_cost_tags DROP CONSTRAINT uq_card_cost_tags_label;
DROP INDEX idx_card_cost_tags_label;

CREATE UNIQUE INDEX uq_card_cost_tags_label_ci ON card_cost_tags (LOWER(label)) WHERE deleted_at IS NULL;
```

**Rationale:** The partial unique index prevents duplicate active tags regardless of casing, while allowing soft-deleted tags to coexist (a deleted "3 Hope" won't block creating a new "3 Hope").

---

## Step 2: New DTO — `CostTagInput`

**File:** `src/main/java/com/aboff/core/model/dto/dh/request/CostTagInput.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostTagInput {
    @NotBlank(message = "Cost tag label is required")
    @Size(max = 200, message = "Cost tag label must not exceed 200 characters")
    private String label;

    @NotNull(message = "Cost tag category is required")
    private CostTagCategory category;
}
```

---

## Step 3: Repository Change — Add Case-Insensitive Lookup

**File:** `src/main/java/com/aboff/core/repository/dh/CardCostTagRepository.java`

**Add method:**
```java
@Query("SELECT t FROM CardCostTag t WHERE LOWER(t.label) = LOWER(:label) AND t.deletedAt IS NULL")
Optional<CardCostTag> findByLabelIgnoreCaseAndDeletedAtIsNull(@Param("label") String label);
```

---

## Step 4: Service Changes — Add `findOrCreate` and `resolveCostTags`

**File:** `src/main/java/com/aboff/core/service/dh/CardCostTagService.java`

**Add two new methods:**

### `findOrCreate(String label, CostTagCategory category)`
1. Query `findByLabelIgnoreCaseAndDeletedAtIsNull(label)`
2. If found → return it
3. If not found → create new `CardCostTag` with the given label and category, save, and return it
4. Log the outcome (found existing vs. created new)

### `resolveCostTags(List<Long> costTagIds, List<CostTagInput> costTags)`
Central resolution method called by all card services:
1. If both inputs are null → return null (signals "no change" for update operations)
2. Initialize an empty `Set<CardCostTag>`
3. If `costTagIds` is non-null and non-empty → fetch by IDs using existing `findAllByIdInAndDeletedAtIsNull`, add to set
4. If `costTags` is non-null and non-empty → for each input, call `findOrCreate`, add to set
5. Return the merged set (may be empty if both lists were provided but empty — signals "clear tags")

**Update semantics (for card update methods):**
- Both `costTagIds` and `costTags` are null → return `null` (don't touch existing tags)
- At least one is non-null → resolve both, merge, and return the set (may be empty to clear)

---

## Step 5: Card Request DTO Changes

**Files (all 8):**
- `CreateAncestryCardRequest.java`, `UpdateAncestryCardRequest.java`
- `CreateCommunityCardRequest.java`, `UpdateCommunityCardRequest.java`
- `CreateSubclassCardRequest.java`, `UpdateSubclassCardRequest.java`
- `CreateDomainCardRequest.java`, `UpdateDomainCardRequest.java`

**Add to each:**
```java
/**
 * Cost tags to find or create by label. Merged with costTagIds if both provided.
 */
@Valid
private List<CostTagInput> costTags;
```

The existing `costTagIds` field remains unchanged.

---

## Step 6: Card Service Changes

**Files (all 4):**
- `AncestryCardService.java`
- `CommunityCardService.java`
- `SubclassCardService.java`
- `DomainCardService.java`

**Each service needs:**
1. Inject `CardCostTagService` (in addition to existing `CardCostTagRepository`)
2. Replace the inline tag resolution logic in `create` and `update` methods with a call to `cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags())`

**Create method — before:**
```java
if (request.getCostTagIds() != null && !request.getCostTagIds().isEmpty()) {
    Set<CardCostTag> costTags = new HashSet<>(cardCostTagRepository.findAllByIdInAndDeletedAtIsNull(request.getCostTagIds()));
    card.setCostTags(costTags);
}
```

**Create method — after:**
```java
Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
if (resolvedTags != null) {
    card.setCostTags(resolvedTags);
}
```

**Update method — before:**
```java
if (request.getCostTagIds() != null) {
    if (request.getCostTagIds().isEmpty()) {
        card.setCostTags(new HashSet<>());
    } else {
        Set<CardCostTag> costTags = new HashSet<>(cardCostTagRepository.findAllByIdInAndDeletedAtIsNull(request.getCostTagIds()));
        card.setCostTags(costTags);
    }
}
```

**Update method — after:**
```java
Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
if (resolvedTags != null) {
    card.setCostTags(resolvedTags);
}
```

---

## Step 7: Tests

### Unit Tests

**`CardCostTagServiceTest.java`** (modify existing) — add tests for:
- `findOrCreate` — existing tag found (case-insensitive match)
- `findOrCreate` — no match, creates new tag
- `resolveCostTags` — only `costTagIds` provided
- `resolveCostTags` — only `costTags` provided
- `resolveCostTags` — both provided (merge behavior)
- `resolveCostTags` — both null (returns null)
- `resolveCostTags` — both empty (returns empty set)
- `resolveCostTags` — duplicate between IDs and labels (deduplicated in set)

**Card service tests** (`AncestryCardServiceTest.java`, etc.) — update existing create/update tests to:
- Verify `resolveCostTags` is called with correct arguments
- Add tests for creating cards with `costTags` (string-based) input
- Add tests for creating cards with both `costTagIds` and `costTags`

### Integration Tests

**`CardCostTagServiceIntegrationTest.java`** (new or modify existing) — test:
- `findOrCreate` against real DB with case-insensitive uniqueness
- Concurrent `findOrCreate` calls for same label (verify no duplicates created)

**Card service integration tests** — test end-to-end card creation with string-based tags.

---

## Verification

1. Run `./mvnw test` — all tests pass
2. Start the app and test via API:
   - Create a card with `costTags: [{"label": "3 Hope", "category": "COST"}]` — verify tag created and attached
   - Create another card with `costTags: [{"label": "3 hope", "category": "COST"}]` — verify same tag reused (case-insensitive)
   - Create a card with both `costTagIds: [1]` and `costTags: [{"label": "New Tag", "category": "TIMING"}]` — verify both resolved and merged
   - Create a card with only `costTagIds` — verify backwards compatibility
3. Run `./mvnw test` again after manual testing to confirm no regressions