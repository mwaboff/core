# Feature Cost Tags Design

## Context

Cost tags (`CardCostTag`) are currently associated with `Card` entities via a `@ManyToMany` join table (`card_card_cost_tags`). Features are standalone entities (not Card subclasses) and need the same cost tag support. Additionally, Features need a bulk create endpoint.

## Approach

Mirror the exact pattern used for cards: entity ManyToMany relationship, dual-input DTOs (ID-based + label-based via `CostTagInput`), expandable response fields, and `CardCostTagService.resolveCostTags()` for resolution.

## File Changes

### 1. Entity - `Feature.java`
- Add `@ManyToMany` `costTags` field with join table `feature_card_cost_tags`

### 2. Database Migration
- New migration: `feature_card_cost_tags` join table
  - `feature_id` (FK to features.id)
  - `card_cost_tag_id` (FK to card_cost_tags.id)
  - Primary key on both columns

### 3. Request DTOs
- `CreateFeatureRequest.java`: Add `costTagIds` (List<Long>) and `@Valid costTags` (List<CostTagInput>)
- `UpdateFeatureRequest.java`: Add `costTagIds` (List<Long>) and `@Valid costTags` (List<CostTagInput>)

### 4. Response DTO
- `FeatureResponse.java`: Add `costTagIds` (List<Long>, always) and `costTags` (List<CardCostTagResponse>, expandable)

### 5. Service - `FeatureService.java`
- Inject `CardCostTagService`
- Create: resolve and set cost tags via `resolveCostTags()`
- Update: resolve and set cost tags (null = no change, empty = clear)
- `toResponse()`: always include costTagIds, expand costTags when requested
- Add `createFeaturesBulk()` method

### 6. Controller - `FeatureController.java`
- Add `POST /api/dh/features/bulk` endpoint (ADMIN/OWNER only)

### 7. Tests - `FeatureServiceTest.java`
- Create with costTagIds
- Create with costTags (label-based)
- Create with both costTagIds and costTags
- Update costTags
- Update clearing costTags (empty lists)
- Update with null costTags (no change)
- Expand costTags in toResponse
- Bulk create with costTags
- Bulk create without costTags

## Testing Strategy

Unit tests only (matching existing FeatureServiceTest pattern). Mock `CardCostTagService` and verify `resolveCostTags()` calls. Verify toResponse output for costTagIds and expanded costTags.
