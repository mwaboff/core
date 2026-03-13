# Loot API

Base path: `/api/dh/loot`

Authentication: JWT token via `AUTH_TOKEN` HttpOnly cookie. All endpoints require authentication; GET endpoints are accessible to any authenticated user. POST/PUT/DELETE endpoints require ADMIN or OWNER role.

---

## Endpoints

### GET `/api/dh/loot`

Retrieve a paginated list of loot items with optional filters.

**Required Role:** Any authenticated user

**Query Parameters:**

| Parameter        | Type    | Required | Default | Description                                              |
|------------------|---------|----------|---------|----------------------------------------------------------|
| `page`           | Integer | No       | 0       | Zero-based page number                                   |
| `size`           | Integer | No       | 20      | Number of items per page (max: 100)                      |
| `includeDeleted` | Boolean | No       | false   | Whether to include soft-deleted loot (ADMIN+ only)       |
| `expansionId`    | Long    | No       | —       | Filter by expansion ID                                   |
| `isOfficial`     | Boolean | No       | —       | Filter by official status                                |
| `tier`           | Integer | No       | —       | Filter by tier (1–4)                                     |
| `isConsumable`   | Boolean | No       | —       | Filter by consumable status                              |
| `expand`         | String  | No       | —       | Comma-separated relationships to expand (see Expand Options) |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Health Potion",
      "expansionId": 1,
      "tier": 1,
      "isOfficial": true,
      "isConsumable": true,
      "description": "Restores health when consumed",
      "featureIds": [1, 2],
      "originalLootId": null,
      "createdAt": "2026-03-13T10:30:00",
      "lastModifiedAt": "2026-03-13T10:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Errors:**

| Status | Condition            |
|--------|----------------------|
| 401    | No AUTH_TOKEN cookie |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/loot?page=0&size=20&isOfficial=true&expand=expansion"
```

---

### GET `/api/dh/loot/{id}`

Retrieve a single loot item by ID.

**Required Role:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Required | Description  |
|-----------|------|----------|--------------|
| `id`      | Long | Yes      | The loot ID  |

**Query Parameters:**

| Parameter | Type   | Required | Description                                              |
|-----------|--------|----------|----------------------------------------------------------|
| `expand`  | String | No       | Comma-separated relationships to expand (see Expand Options) |

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores health when consumed",
  "featureIds": [1],
  "originalLootId": null,
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Errors:**

| Status | Condition            |
|--------|----------------------|
| 401    | No AUTH_TOKEN cookie |
| 404    | Loot not found or soft-deleted |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/loot/1?expand=features,expansion"
```

---

### POST `/api/dh/loot`

Create a new loot item.

**Required Role:** ADMIN or OWNER

**Request Body:** `CreateLootRequest`

| Field            | Type            | Required | Validation                          | Description                                      |
|------------------|-----------------|----------|-------------------------------------|--------------------------------------------------|
| `name`           | String          | Yes      | Not blank, max 200 chars            | Name of the loot item                            |
| `expansionId`    | Long            | Yes      | Not null                            | ID of the expansion this loot belongs to         |
| `tier`           | Integer         | Yes      | Not null, 1–4                       | Tier/rarity: 1=Common, 2=Uncommon, 3=Rare, 4=Legendary |
| `isOfficial`     | Boolean         | Yes      | Not null                            | Whether this is official game content            |
| `isConsumable`   | Boolean         | Yes      | Not null                            | Whether the item is consumable (single-use)      |
| `description`    | String          | No       | —                                   | Description of the loot item                     |
| `featureIds`     | List\<Long\>    | No       | —                                   | IDs of existing features to attach               |
| `features`       | List\<FeatureInput\> | No  | Valid nested objects                | Inline features to find-or-create (see FeatureInput) |
| `originalLootId` | Long            | No       | —                                   | ID of original loot if this is a custom copy     |

**Request Example:**

```json
{
  "name": "Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores health when consumed"
}
```

**Request Example (with inline features):**

```json
{
  "name": "Magic Ring",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": false,
  "description": "A ring with magical properties",
  "features": [
    {
      "name": "Protection Aura",
      "description": "Grants minor protection",
      "featureType": "ITEM",
      "expansionId": 1
    }
  ]
}
```

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores health when consumed",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Errors:**

| Status | Condition                        |
|--------|----------------------------------|
| 401    | No AUTH_TOKEN cookie             |
| 403    | Role is USER or MODERATOR        |
| 404    | Expansion not found              |
| 404    | Original loot not found          |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Health Potion","expansionId":1,"tier":1,"isOfficial":true,"isConsumable":true,"description":"Restores health when consumed"}' \
  "http://localhost:8080/api/dh/loot"
```

---

### POST `/api/dh/loot/bulk`

Create multiple loot items in a single request.

**Required Role:** ADMIN or OWNER

**Request Body:** Array of `CreateLootRequest` (see POST `/api/dh/loot` for field details)

**Request Example:**

```json
[
  {
    "name": "Health Potion",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": true
  },
  {
    "name": "Rope",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": false,
    "description": "50 feet of rope"
  }
]
```

**Response:** `201 Created`

```json
[
  {
    "id": 1,
    "name": "Health Potion",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": true,
    "createdAt": "2026-03-13T10:30:00",
    "lastModifiedAt": "2026-03-13T10:30:00"
  },
  {
    "id": 2,
    "name": "Rope",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": false,
    "description": "50 feet of rope",
    "createdAt": "2026-03-13T10:30:00",
    "lastModifiedAt": "2026-03-13T10:30:00"
  }
]
```

**Errors:**

| Status | Condition                        |
|--------|----------------------------------|
| 401    | No AUTH_TOKEN cookie             |
| 403    | Role is USER or MODERATOR        |
| 404    | Any referenced expansion not found |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '[{"name":"Health Potion","expansionId":1,"tier":1,"isOfficial":true,"isConsumable":true},{"name":"Rope","expansionId":1,"tier":1,"isOfficial":true,"isConsumable":false,"description":"50 feet of rope"}]' \
  "http://localhost:8080/api/dh/loot/bulk"
```

---

### PUT `/api/dh/loot/{id}`

Update an existing loot item. All required fields must be provided (full replacement).

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description            |
|-----------|------|----------|------------------------|
| `id`      | Long | Yes      | The loot ID to update  |

**Request Body:** `UpdateLootRequest`

| Field            | Type            | Required | Validation                          | Description                                      |
|------------------|-----------------|----------|-------------------------------------|--------------------------------------------------|
| `name`           | String          | Yes      | Not blank, max 200 chars            | Name of the loot item                            |
| `expansionId`    | Long            | Yes      | Not null                            | ID of the expansion this loot belongs to         |
| `tier`           | Integer         | Yes      | Not null, 1–4                       | Tier/rarity: 1=Common, 2=Uncommon, 3=Rare, 4=Legendary |
| `isOfficial`     | Boolean         | Yes      | Not null                            | Whether this is official game content            |
| `isConsumable`   | Boolean         | Yes      | Not null                            | Whether the item is consumable (single-use)      |
| `description`    | String          | No       | —                                   | Description of the loot item                     |
| `featureIds`     | List\<Long\>    | No       | —                                   | IDs of existing features to attach               |
| `features`       | List\<FeatureInput\> | No  | Valid nested objects                | Inline features to find-or-create (see FeatureInput) |
| `originalLootId` | Long            | No       | —                                   | ID of original loot (set to null to clear)       |

**Request Example:**

```json
{
  "name": "Greater Health Potion",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores more health"
}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Greater Health Potion",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores more health",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:31:00"
}
```

**Errors:**

| Status | Condition                        |
|--------|----------------------------------|
| 401    | No AUTH_TOKEN cookie             |
| 403    | Role is USER or MODERATOR        |
| 404    | Loot not found or soft-deleted   |
| 404    | Expansion not found              |
| 404    | Original loot not found          |

**curl:**

```bash
curl -s -X PUT -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Greater Health Potion","expansionId":1,"tier":2,"isOfficial":true,"isConsumable":true,"description":"Restores more health"}' \
  "http://localhost:8080/api/dh/loot/1"
```

---

### DELETE `/api/dh/loot/{id}`

Soft-delete a loot item (sets `deletedAt` timestamp).

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description            |
|-----------|------|----------|------------------------|
| `id`      | Long | Yes      | The loot ID to delete  |

**Response:** `204 No Content`

**Errors:**

| Status | Condition                        |
|--------|----------------------------------|
| 401    | No AUTH_TOKEN cookie             |
| 403    | Role is USER or MODERATOR        |
| 404    | Loot not found or already deleted |

**curl:**

```bash
curl -s -X DELETE -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/loot/1"
```

---

### POST `/api/dh/loot/{id}/restore`

Restore a soft-deleted loot item (clears `deletedAt` timestamp).

**Required Role:** ADMIN or OWNER

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | Long | Yes      | The loot ID to restore   |

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores health when consumed",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:32:00"
}
```

**Errors:**

| Status | Condition                               |
|--------|-----------------------------------------|
| 401    | No AUTH_TOKEN cookie                    |
| 403    | Role is USER or MODERATOR               |
| 404    | Loot not found                          |
| 500    | Loot is not deleted (IllegalStateException) |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/dh/loot/1/restore"
```

---

## Expand Options

The `expand` query parameter accepts a comma-separated list of relationship names. Null fields are omitted from the response (`@JsonInclude(NON_NULL)`).

| Value          | Effect                                                    |
|----------------|-----------------------------------------------------------|
| `expansion`    | Includes full `ExpansionResponse` object                  |
| `features`     | Includes full `FeatureResponse` objects in `features` array |
| `originalLoot` | Includes full nested `LootResponse` for the original loot |

**Example:** `?expand=expansion,features,originalLoot`

---

## Data Models

### LootResponse

Returned by all endpoints that return loot data. Null fields are omitted from JSON output.

| Field            | Type                    | Nullable | Description                                      |
|------------------|-------------------------|----------|--------------------------------------------------|
| `id`             | Long                    | No       | Unique identifier                                |
| `name`           | String                  | No       | Name of the loot item                            |
| `expansionId`    | Long                    | No       | ID of the parent expansion                       |
| `expansion`      | ExpansionResponse       | Yes      | Full expansion (only with `?expand=expansion`)   |
| `tier`           | Integer                 | No       | Tier/rarity level (1–4)                          |
| `isOfficial`     | Boolean                 | No       | Whether this is official game content            |
| `isConsumable`   | Boolean                 | No       | Whether the item is consumable                   |
| `description`    | String                  | Yes      | Description of the loot item                     |
| `featureIds`     | List\<Long\>            | Yes      | IDs of attached features (when features exist)   |
| `features`       | List\<FeatureResponse\> | Yes      | Full features (only with `?expand=features`)     |
| `originalLootId` | Long                    | Yes      | ID of original loot if this is a copy            |
| `originalLoot`   | LootResponse            | Yes      | Full original (only with `?expand=originalLoot`) |
| `createdAt`      | LocalDateTime           | No       | Creation timestamp                               |
| `lastModifiedAt` | LocalDateTime           | No       | Last modification timestamp                      |
| `deletedAt`      | LocalDateTime           | Yes      | Soft-delete timestamp (null if active)           |

### PagedResponse\<LootResponse\>

Wrapper returned by the list endpoint.

| Field           | Type                | Description                          |
|-----------------|---------------------|--------------------------------------|
| `content`       | List\<LootResponse\> | Items for the current page          |
| `totalElements` | Long                | Total items across all pages         |
| `totalPages`    | Integer             | Total number of pages                |
| `currentPage`   | Integer             | Current page number (zero-based)     |
| `pageSize`      | Integer             | Number of items per page             |

### ExpansionResponse

Nested within `LootResponse` when `?expand=expansion` is used.

| Field            | Type          | Nullable | Description                     |
|------------------|---------------|----------|---------------------------------|
| `id`             | Long          | No       | Unique identifier               |
| `name`           | String        | No       | Expansion name                  |
| `isPublished`    | Boolean       | No       | Whether the expansion is live   |
| `createdAt`      | LocalDateTime | No       | Creation timestamp              |
| `lastModifiedAt` | LocalDateTime | No       | Last modification timestamp     |
| `deletedAt`      | LocalDateTime | Yes      | Soft-delete timestamp           |

### FeatureResponse

Nested within `LootResponse` when `?expand=features` is used.

| Field            | Type                          | Nullable | Description                          |
|------------------|-------------------------------|----------|--------------------------------------|
| `id`             | Long                          | No       | Unique identifier                    |
| `name`           | String                        | No       | Feature name                         |
| `description`    | String                        | Yes      | What the feature does                |
| `featureType`    | FeatureType                   | No       | Category (see FeatureType enum)      |
| `expansionId`    | Long                          | No       | ID of the parent expansion           |
| `expansion`      | ExpansionResponse             | Yes      | Full expansion (only with expand)    |
| `costTagIds`     | List\<Long\>                  | Yes      | IDs of associated cost tags          |
| `costTags`       | List\<CardCostTagResponse\>   | Yes      | Full cost tags (only with expand)    |
| `modifierIds`    | List\<Long\>                  | Yes      | IDs of associated modifiers          |
| `modifiers`      | List\<FeatureModifierResponse\> | Yes    | Full modifiers (only with expand)    |
| `createdAt`      | LocalDateTime                 | No       | Creation timestamp                   |
| `lastModifiedAt` | LocalDateTime                 | No       | Last modification timestamp          |
| `deletedAt`      | LocalDateTime                 | Yes      | Soft-delete timestamp                |

### FeatureInput

Used in `CreateLootRequest.features` and `UpdateLootRequest.features` for inline feature creation.

| Field         | Type                       | Required | Description                                      |
|---------------|----------------------------|----------|--------------------------------------------------|
| `name`        | String                     | No       | Feature name (max 200 chars, matched case-insensitively) |
| `description` | String                     | No       | What the feature does                            |
| `featureType` | FeatureType                | Yes      | Category of the feature                          |
| `expansionId` | Long                       | Yes      | ID of the parent expansion                       |
| `costTagIds`  | List\<Long\>               | No       | IDs of existing cost tags to attach              |
| `costTags`    | List\<CostTagInput\>       | No       | Inline cost tags to find-or-create               |
| `modifierIds` | List\<Long\>               | No       | IDs of existing modifiers to attach              |
| `modifiers`   | List\<FeatureModifierInput\> | No     | Inline modifiers to find-or-create               |

---

## Enums

### FeatureType

Used in `FeatureInput.featureType` and `FeatureResponse.featureType`.

| Value       | Description              |
|-------------|--------------------------|
| `HOPE`      | Hope feature type        |
| `ANCESTRY`  | Ancestry feature type    |
| `CLASS`     | Class feature type       |
| `COMMUNITY` | Community feature type   |
| `DOMAIN`    | Domain feature type      |
| `ITEM`      | Item feature type        |
| `OTHER`     | Other/miscellaneous      |
| `SUBCLASS`  | Subclass feature type    |

### Tier Values

Used in `CreateLootRequest.tier`, `UpdateLootRequest.tier`, and `LootResponse.tier`.

| Value | Rarity    |
|-------|-----------|
| 1     | Common    |
| 2     | Uncommon  |
| 3     | Rare      |
| 4     | Legendary |

---

## Database Schema

### `loot` table

Created by `V20260124200019469__create_loot_table.sql`, with columns added by subsequent migrations.

| Column              | Type         | Nullable | Default | Description                          |
|---------------------|--------------|----------|---------|--------------------------------------|
| `id`                | BIGSERIAL    | No       | auto    | Primary key                          |
| `name`              | VARCHAR(200) | No       | —       | Loot name                           |
| `description`       | TEXT         | Yes      | —       | Loot description                     |
| `expansion_id`      | BIGINT       | No       | —       | FK to `expansions(id)`               |
| `tier`              | INTEGER      | No       | 1       | Tier level, CHECK 1–4               |
| `is_official`       | BOOLEAN      | No       | true    | Official content flag                |
| `is_consumable`     | BOOLEAN      | No       | false   | Consumable item flag                 |
| `created_by_user_id`| BIGINT       | Yes      | —       | FK to `users(id)` (custom items)     |
| `original_loot_id`  | BIGINT       | Yes      | —       | FK self-reference to `loot(id)`      |
| `created_at`        | TIMESTAMP    | No       | —       | Creation timestamp                   |
| `last_modified_at`  | TIMESTAMP    | No       | —       | Last modification timestamp          |
| `deleted_at`        | TIMESTAMP    | Yes      | —       | Soft-delete timestamp                |

**Indexes:**

| Index                | Columns                        | Condition              |
|----------------------|--------------------------------|------------------------|
| `idx_loot_expansion` | `expansion_id`                 | —                      |
| `idx_loot_is_official` | `is_official`                | —                      |
| `idx_loot_created_by` | `created_by_user_id`          | —                      |
| `idx_loot_original`  | `original_loot_id`             | —                      |
| `idx_loot_active`    | `expansion_id, is_official`    | `WHERE deleted_at IS NULL` |

**Constraints:**

| Constraint       | Type  | Definition         |
|------------------|-------|--------------------|
| `chk_loot_tier`  | CHECK | `tier BETWEEN 1 AND 4` |

### `loot_features` join table

Created by `V20260228145241147__move_features_to_base_item.sql`.

| Column       | Type   | Description             |
|--------------|--------|-------------------------|
| `loot_id`    | BIGINT | FK to `loot(id)`        |
| `feature_id` | BIGINT | FK to `features(id)`    |

Primary key: `(loot_id, feature_id)`

### Migrations (in order)

1. `V20260124200019469__create_loot_table.sql` -- Creates `loot` table with core columns and indexes
2. `V20260228113319717__add_tier_to_items.sql` -- Adds `tier` column with CHECK constraint (1–4)
3. `V20260228114850303__add_consumable_to_loot.sql` -- Adds `is_consumable` column (default false)
4. `V20260228145241147__move_features_to_base_item.sql` -- Creates `loot_features` join table

---

## Test Data Examples

Extracted from integration and unit tests for use in development/testing.

### Minimal create request

```json
{
  "name": "Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isConsumable": true
}
```

### Full create request with feature IDs

```json
{
  "name": "Healing Crystal",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": false,
  "description": "A crystal that heals",
  "featureIds": [1]
}
```

### Create request with inline features

```json
{
  "name": "Magic Ring",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": false,
  "description": "A ring with magical properties",
  "features": [
    {
      "name": "Protection Aura",
      "description": "Grants minor protection",
      "featureType": "ITEM",
      "expansionId": 1
    }
  ]
}
```

### Create request with original loot reference

```json
{
  "name": "Custom Health Potion",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": false,
  "isConsumable": true,
  "description": "A modified version",
  "originalLootId": 1
}
```

### Update request

```json
{
  "name": "Greater Health Potion",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isConsumable": true,
  "description": "Restores more health"
}
```

### Bulk create request

```json
[
  {
    "name": "Health Potion",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": true
  },
  {
    "name": "Rope",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isConsumable": false,
    "description": "50 feet of rope"
  }
]
```
