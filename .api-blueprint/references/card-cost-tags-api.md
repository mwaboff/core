# Card Cost Tags API

Base path: `/api/dh/cost-tags`

Authentication: JWT token via `AUTH_TOKEN` HttpOnly cookie. All endpoints require authentication; write endpoints (create, update, delete, restore) require ADMIN or OWNER role.

---

## Data Model

### CostTagCategory Enum

| Value        | Description                                                        |
|--------------|--------------------------------------------------------------------|
| `COST`       | Resource expenditure tags (e.g., "3 Hope", "1 Stress")             |
| `LIMITATION` | Restriction or requirement tags (e.g., "Close range", "Requires Level 5") |
| `TIMING`     | Frequency or action type tags (e.g., "1/session", "Action", "Reaction")   |

### Database Table: `card_cost_tags`

| Column             | Type          | Constraints                                              |
|--------------------|---------------|----------------------------------------------------------|
| `id`               | BIGSERIAL     | PRIMARY KEY                                              |
| `label`            | VARCHAR(200)  | NOT NULL, case-insensitive unique (among active records)  |
| `category`         | VARCHAR(20)   | NOT NULL, CHECK IN ('COST', 'LIMITATION', 'TIMING')     |
| `created_at`       | TIMESTAMP     | NOT NULL, DEFAULT CURRENT_TIMESTAMP                      |
| `last_modified_at` | TIMESTAMP     | NOT NULL, DEFAULT CURRENT_TIMESTAMP                      |
| `deleted_at`       | TIMESTAMP     | Nullable (NULL = active, non-NULL = soft-deleted)        |

**Indexes:**
- `idx_card_cost_tags_category` on `category`
- `idx_card_cost_tags_deleted_at` on `deleted_at`
- `idx_card_cost_tags_active` on `category` WHERE `deleted_at IS NULL`
- `uq_card_cost_tags_label_ci` unique on `LOWER(label)` WHERE `deleted_at IS NULL`

**Join Tables:**
- `card_card_cost_tags` (card_id, card_cost_tag_id) -- links cards to cost tags
- `feature_card_cost_tags` (feature_id, card_cost_tag_id) -- links features to cost tags

### Migrations

1. `V20260209153142969__add_card_cost_tags.sql` -- Creates `card_cost_tags` table and `card_card_cost_tags` join table
2. `V20260209190818275__add_ci_unique_label_cost_tags.sql` -- Replaces case-sensitive unique constraint with case-insensitive partial unique index (active records only)
3. `V20260209203727891__add_feature_cost_tags_join_table.sql` -- Creates `feature_card_cost_tags` join table

---

## DTOs

### CreateCardCostTagRequest

| Field      | Type              | Required | Validation                       |
|------------|-------------------|----------|----------------------------------|
| `label`    | String            | Yes      | `@NotBlank`, max 200 characters  |
| `category` | CostTagCategory   | Yes      | `@NotNull`                       |

```json
{
  "label": "3 Hope",
  "category": "COST"
}
```

### UpdateCardCostTagRequest

| Field      | Type              | Required | Validation                       |
|------------|-------------------|----------|----------------------------------|
| `label`    | String            | Yes      | `@NotBlank`, max 200 characters  |
| `category` | CostTagCategory   | Yes      | `@NotNull`                       |

```json
{
  "label": "Updated Label",
  "category": "TIMING"
}
```

### CostTagInput

Used in card/feature create/update requests to specify cost tags by label (find-or-create semantics).

| Field      | Type              | Required | Validation                       |
|------------|-------------------|----------|----------------------------------|
| `label`    | String            | Yes      | `@NotBlank`, max 200 characters  |
| `category` | CostTagCategory   | Yes      | `@NotNull`                       |

```json
{
  "label": "1/session",
  "category": "TIMING"
}
```

### CardCostTagResponse

Uses `@JsonInclude(NON_NULL)` -- null fields are omitted from the response.

| Field            | Type            | Nullable | Description                                    |
|------------------|-----------------|----------|------------------------------------------------|
| `id`             | Long            | No       | Unique identifier                              |
| `label`          | String          | No       | Display label (e.g., "3 Hope", "1/session")    |
| `category`       | CostTagCategory | No       | Tag category: COST, LIMITATION, or TIMING      |
| `createdAt`      | LocalDateTime   | No       | When the tag was created                       |
| `lastModifiedAt` | LocalDateTime   | No       | When the tag was last modified                 |
| `deletedAt`      | LocalDateTime   | Yes      | When soft-deleted (omitted from JSON if null)  |

```json
{
  "id": 1,
  "label": "3 Hope",
  "category": "COST",
  "createdAt": "2026-02-09T15:31:42",
  "lastModifiedAt": "2026-02-09T15:31:42"
}
```

### PagedResponse\<CardCostTagResponse\>

| Field           | Type                       | Description                          |
|-----------------|----------------------------|--------------------------------------|
| `content`       | CardCostTagResponse[]      | Items for the current page           |
| `totalElements` | long                       | Total number of elements across all pages |
| `totalPages`    | int                        | Total number of pages                |
| `currentPage`   | int                        | Current page number (zero-based)     |
| `pageSize`      | int                        | Number of items per page             |

```json
{
  "content": [
    {
      "id": 1,
      "label": "3 Hope",
      "category": "COST",
      "createdAt": "2026-02-09T15:31:42",
      "lastModifiedAt": "2026-02-09T15:31:42"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## Endpoints

### GET `/api/dh/cost-tags`

Retrieve a paginated list of cost tags with optional category filtering.

**Required Role:** Any authenticated user

**Query Parameters:**

| Parameter        | Type              | Required | Default | Description                                           |
|------------------|-------------------|----------|---------|-------------------------------------------------------|
| `page`           | int               | No       | 0       | Zero-based page number                                |
| `size`           | int               | No       | 20      | Items per page (max: 100, values above 100 are clamped) |
| `includeDeleted` | boolean           | No       | false   | Include soft-deleted tags (ADMIN+ only)               |
| `category`       | CostTagCategory   | No       | --      | Filter by category: `COST`, `LIMITATION`, or `TIMING` |

**Response:** `200 OK` -- `PagedResponse<CardCostTagResponse>`

```json
{
  "content": [
    {
      "id": 1,
      "label": "3 Hope",
      "category": "COST",
      "createdAt": "2026-02-09T15:31:42",
      "lastModifiedAt": "2026-02-09T15:31:42"
    },
    {
      "id": 2,
      "label": "1/session",
      "category": "TIMING",
      "createdAt": "2026-02-09T15:31:42",
      "lastModifiedAt": "2026-02-09T15:31:42"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Errors:**

| Status | Condition           |
|--------|---------------------|
| 401    | No AUTH_TOKEN cookie |

**curl:**

```bash
# List all active cost tags
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags"

# With pagination
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags?page=1&size=2"

# Filter by category
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags?category=COST"

# Include soft-deleted (admin only)
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags?includeDeleted=true"
```

---

### GET `/api/dh/cost-tags/{id}`

Retrieve a single cost tag by ID. Only returns active (non-deleted) tags.

**Required Role:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| `id`      | Long | Yes      | The cost tag ID |

**Response:** `200 OK` -- `CardCostTagResponse`

```json
{
  "id": 1,
  "label": "3 Hope",
  "category": "COST",
  "createdAt": "2026-02-09T15:31:42",
  "lastModifiedAt": "2026-02-09T15:31:42"
}
```

**Errors:**

| Status | Condition                            |
|--------|--------------------------------------|
| 401    | No AUTH_TOKEN cookie                 |
| 404    | Cost tag not found or is soft-deleted |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags/1"
```

---

### POST `/api/dh/cost-tags`

Create a new cost tag.

**Required Role:** ADMIN or OWNER

**Request Body:** `CreateCardCostTagRequest`

```json
{
  "label": "3 Hope",
  "category": "COST"
}
```

**Response:** `201 Created` -- `CardCostTagResponse`

```json
{
  "id": 1,
  "label": "3 Hope",
  "category": "COST",
  "createdAt": "2026-02-09T15:31:42",
  "lastModifiedAt": "2026-02-09T15:31:42"
}
```

**Errors:**

| Status | Condition                        |
|--------|----------------------------------|
| 400    | Blank label or null category     |
| 401    | No AUTH_TOKEN cookie             |
| 403    | Role is USER or MODERATOR        |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"label":"3 Hope","category":"COST"}' \
  "http://localhost:8080/api/dh/cost-tags"
```

---

### PUT `/api/dh/cost-tags/{id}`

Update an existing cost tag. Only active (non-deleted) tags can be updated.

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | Long | Yes      | The cost tag ID to update |

**Request Body:** `UpdateCardCostTagRequest`

```json
{
  "label": "Updated Label",
  "category": "TIMING"
}
```

**Response:** `200 OK` -- `CardCostTagResponse`

```json
{
  "id": 1,
  "label": "Updated Label",
  "category": "TIMING",
  "createdAt": "2026-02-09T15:31:42",
  "lastModifiedAt": "2026-02-09T16:00:00"
}
```

**Errors:**

| Status | Condition                            |
|--------|--------------------------------------|
| 400    | Blank label or null category         |
| 401    | No AUTH_TOKEN cookie                 |
| 403    | Role is USER or MODERATOR            |
| 404    | Cost tag not found or is soft-deleted |

**curl:**

```bash
curl -s -X PUT -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"label":"Updated Label","category":"TIMING"}' \
  "http://localhost:8080/api/dh/cost-tags/1"
```

---

### DELETE `/api/dh/cost-tags/{id}`

Soft-delete a cost tag (sets `deleted_at` timestamp). Only active tags can be deleted.

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | Long | Yes      | The cost tag ID to delete |

**Response:** `204 No Content` (empty body)

**Errors:**

| Status | Condition                            |
|--------|--------------------------------------|
| 401    | No AUTH_TOKEN cookie                 |
| 403    | Role is USER or MODERATOR            |
| 404    | Cost tag not found or is already soft-deleted |

**curl:**

```bash
curl -s -X DELETE -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags/1"
```

---

### POST `/api/dh/cost-tags/{id}/restore`

Restore a soft-deleted cost tag (clears `deleted_at` timestamp).

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description               |
|-----------|------|----------|---------------------------|
| `id`      | Long | Yes      | The cost tag ID to restore |

**Response:** `200 OK` -- `CardCostTagResponse`

```json
{
  "id": 1,
  "label": "Restored Tag",
  "category": "COST",
  "createdAt": "2026-02-09T15:31:42",
  "lastModifiedAt": "2026-02-09T16:30:00"
}
```

**Errors:**

| Status | Condition                                    |
|--------|----------------------------------------------|
| 400    | Cost tag is not deleted (IllegalStateException) |
| 401    | No AUTH_TOKEN cookie                         |
| 403    | Role is USER or MODERATOR                    |
| 404    | Cost tag not found                           |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/cost-tags/1/restore"
```

---

## Service-Only Methods (Not Exposed via REST)

The `CardCostTagService` provides two additional methods used internally by other services (e.g., card and feature creation):

### `findOrCreate(label, category)`

Finds an existing active cost tag by label (case-insensitive match) or creates a new one if no match is found.

- **Parameters:** `String label`, `CostTagCategory category`
- **Returns:** `CardCostTag` entity
- **Behavior:** If an active tag with a matching label exists, returns it (ignores the provided category). Otherwise, creates and persists a new tag with the given label and category.

### `resolveCostTags(costTagIds, costTags)`

Resolves cost tags from both ID-based and label-based inputs, merging the results into a deduplicated set.

- **Parameters:** `List<Long> costTagIds`, `List<CostTagInput> costTags`
- **Returns:** `Set<CardCostTag>` or `null`
- **Behavior:**
  - Both `null` -> returns `null` (signals "do not modify existing tags" in update operations)
  - At least one non-null but both empty -> returns empty `Set` (signals "clear all tags")
  - IDs are looked up via `findAllByIdInAndDeletedAtIsNull`
  - Inputs are resolved via `findOrCreate` for each entry
  - Results are merged and deduplicated

---

## Test Data Samples

From integration tests (`CardCostTagControllerIntegrationTest`):

| Label          | Category     | Usage Context                       |
|----------------|--------------|-------------------------------------|
| `"3 Hope"`     | `COST`       | Create, get-by-id, filter-by-category |
| `"1/session"`  | `TIMING`     | List all, filter-by-category        |
| `"Close range"`| `LIMITATION` | Filter-by-category                  |
| `"Active Tag"` | `COST`       | Soft-deletion exclusion test        |
| `"Deleted Tag"`| `TIMING`     | Soft-deletion exclusion test        |
| `"Original Label"` | `COST`   | Update test (before)                |
| `"Updated Label"`  | `TIMING` | Update test (after)                 |
| `"To Delete"`  | `COST`       | Delete test                         |
| `"Tag 1"` ... `"Tag 5"` | `COST` | Pagination test                 |

From unit tests (`CardCostTagServiceTest`):

| Label         | Category     | Usage Context                 |
|---------------|--------------|-------------------------------|
| `"3 Hope"`    | `COST`       | findOrCreate, resolveCostTags |
| `"1/session"` | `TIMING`     | resolveCostTags merge         |
| `"New Tag"`   | `TIMING`     | findOrCreate (creates new)    |
| `"New Tag"`   | `LIMITATION` | resolveCostTags with inputs   |
