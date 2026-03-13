# Armors API

Base path: `/api/dh/armors`
Authentication: All endpoints require a valid JWT token via `AUTH_TOKEN` HttpOnly cookie.
Authorization: GET endpoints are accessible to all authenticated users. POST/PUT/DELETE endpoints require `ADMIN` or `OWNER` role.

---

## Endpoints

### GET /api/dh/armors

Retrieve a paginated list of armors with optional filters.

**Authentication:** Required (JWT cookie)
**Authorization:** Any authenticated user
**Status:** `200 OK`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | `int` | No | `0` | Zero-based page number |
| `size` | `int` | No | `20` | Items per page (max: 100; values above 100 are clamped) |
| `includeDeleted` | `boolean` | No | `false` | Include soft-deleted armors (ADMIN+ only) |
| `expansionId` | `Long` | No | - | Filter by expansion ID |
| `isOfficial` | `Boolean` | No | - | Filter by official status |
| `tier` | `Integer` | No | - | Filter by tier (1--4) |
| `expand` | `String` | No | - | Comma-separated list of relationships to expand: `expansion`, `features`, `originalArmor` |

**Response Body:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Leather Armor",
      "expansionId": 1,
      "tier": 1,
      "isOfficial": true,
      "baseMajorThreshold": 5,
      "baseSevereThreshold": 10,
      "baseScore": 1,
      "featureIds": [1, 2],
      "originalArmorId": null,
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

Note: `featureIds`, `originalArmorId`, and `deletedAt` are omitted from the response when null (`@JsonInclude(NON_NULL)`).

**Response with `?expand=expansion`:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Leather Armor",
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook",
        "isPublished": true,
        "createdAt": "2026-03-13T10:30:00",
        "lastModifiedAt": "2026-03-13T10:30:00"
      },
      "tier": 1,
      "isOfficial": true,
      "baseMajorThreshold": 5,
      "baseSevereThreshold": 10,
      "baseScore": 1,
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

**Response with `?expand=features`:**

Each feature object in the `features` array follows the FeatureResponse schema (see [Expanded Objects](#expanded-objects)).

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid/revoked token | (no body) |

**curl:**

```bash
curl -s http://localhost:8080/api/dh/armors \
  --cookie "AUTH_TOKEN=<jwt_token>"

# With filters and expansion
curl -s "http://localhost:8080/api/dh/armors?expansionId=1&tier=2&expand=expansion,features" \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### GET /api/dh/armors/{id}

Retrieve a single armor by ID.

**Authentication:** Required (JWT cookie)
**Authorization:** Any authenticated user
**Status:** `200 OK`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `Long` | Yes | The armor ID |

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `expand` | `String` | No | - | Comma-separated list: `expansion`, `features`, `originalArmor` |

**Response Body:**

```json
{
  "id": 1,
  "name": "Leather Armor",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 5,
  "baseSevereThreshold": 10,
  "baseScore": 1,
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `404 Not Found` | Armor ID does not exist or is soft-deleted | `{"status": 404, "error": "Not Found", "message": "Armor not found with id: 99999", ...}` |

**curl:**

```bash
curl -s http://localhost:8080/api/dh/armors/1 \
  --cookie "AUTH_TOKEN=<jwt_token>"

# With expansion
curl -s "http://localhost:8080/api/dh/armors/1?expand=expansion,features,originalArmor" \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### POST /api/dh/armors

Create a new armor.

**Authentication:** Required (JWT cookie)
**Authorization:** `ADMIN` or `OWNER` role (`@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")`)
**Status:** `201 Created`

**Request Body (CreateArmorRequest):**

```json
{
  "name": "Leather Armor",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 5,
  "baseSevereThreshold": 10,
  "baseScore": 1,
  "featureIds": [1, 2],
  "features": [
    {
      "name": "Shield Block",
      "description": "Block incoming damage",
      "featureType": "OTHER",
      "expansionId": 1,
      "costTagIds": [1],
      "costTags": [
        { "label": "1/session", "category": "TIMING" }
      ],
      "modifierIds": [1],
      "modifiers": [
        { "target": "ARMOR_SCORE", "operation": "ADD", "value": 2 }
      ]
    }
  ],
  "originalArmorId": null
}
```

**Field Validation:**

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `name` | `String` | Yes | `@NotBlank`, max 200 chars | Armor name |
| `expansionId` | `Long` | Yes | `@NotNull` | Expansion this armor belongs to |
| `tier` | `Integer` | Yes | `@NotNull`, 1--4 | Tier level |
| `isOfficial` | `Boolean` | Yes | `@NotNull` | Whether from official content |
| `baseMajorThreshold` | `Integer` | Yes | `@NotNull`, `@Positive` | Min damage for major injury |
| `baseSevereThreshold` | `Integer` | Yes | `@NotNull`, `@Positive` | Min damage for severe injury |
| `baseScore` | `Integer` | Yes | `@NotNull`, `@PositiveOrZero` | Base defensive score |
| `featureIds` | `List<Long>` | No | - | IDs of existing features to attach (takes precedence over `features`) |
| `features` | `List<FeatureInput>` | No | `@Valid` | Inline features to find-or-create (used if `featureIds` not provided) |
| `originalArmorId` | `Long` | No | - | ID of original armor if this is a custom copy |

**Response Body:**

```json
{
  "id": 1,
  "name": "Leather Armor",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 5,
  "baseSevereThreshold": 10,
  "baseScore": 1,
  "featureIds": [1],
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `400 Bad Request` | Validation failure (blank name, missing required fields, tier out of range, negative thresholds) | `{"status": 400, "error": "Bad Request", "message": "Validation failed", "fieldErrors": {...}}` |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | User does not have ADMIN/OWNER role | (no body) |
| `404 Not Found` | Referenced `expansionId` or `originalArmorId` not found | `{"status": 404, "error": "Not Found", "message": "Expansion not found with id: 999", ...}` |

**curl:**

```bash
curl -s -X POST http://localhost:8080/api/dh/armors \
  --cookie "AUTH_TOKEN=<jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Leather Armor",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "baseMajorThreshold": 5,
    "baseSevereThreshold": 10,
    "baseScore": 1
  }'
```

---

### POST /api/dh/armors/bulk

Create multiple armors in a single request.

**Authentication:** Required (JWT cookie)
**Authorization:** `ADMIN` or `OWNER` role
**Status:** `201 Created`

**Request Body:** `List<CreateArmorRequest>` -- an array of CreateArmorRequest objects (same schema as single create).

```json
[
  {
    "name": "Leather Armor",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "baseMajorThreshold": 5,
    "baseSevereThreshold": 10,
    "baseScore": 1
  },
  {
    "name": "Plate Mail",
    "expansionId": 1,
    "tier": 2,
    "isOfficial": true,
    "baseMajorThreshold": 8,
    "baseSevereThreshold": 16,
    "baseScore": 3
  }
]
```

**Response Body:** `List<ArmorResponse>` -- an array of created armor responses.

```json
[
  {
    "id": 1,
    "name": "Leather Armor",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "baseMajorThreshold": 5,
    "baseSevereThreshold": 10,
    "baseScore": 1,
    "createdAt": "2026-03-13T10:30:00",
    "lastModifiedAt": "2026-03-13T10:30:00"
  },
  {
    "id": 2,
    "name": "Plate Mail",
    "expansionId": 1,
    "tier": 2,
    "isOfficial": true,
    "baseMajorThreshold": 8,
    "baseSevereThreshold": 16,
    "baseScore": 3,
    "createdAt": "2026-03-13T10:30:00",
    "lastModifiedAt": "2026-03-13T10:30:00"
  }
]
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `400 Bad Request` | Validation failure on any item in the list | `{"status": 400, ...}` |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | User does not have ADMIN/OWNER role | (no body) |
| `404 Not Found` | Referenced `expansionId` or `originalArmorId` not found | `{"status": 404, ...}` |

**curl:**

```bash
curl -s -X POST http://localhost:8080/api/dh/armors/bulk \
  --cookie "AUTH_TOKEN=<jwt_token>" \
  -H "Content-Type: application/json" \
  -d '[
    {"name": "Leather Armor", "expansionId": 1, "tier": 1, "isOfficial": true, "baseMajorThreshold": 5, "baseSevereThreshold": 10, "baseScore": 1},
    {"name": "Plate Mail", "expansionId": 1, "tier": 2, "isOfficial": true, "baseMajorThreshold": 8, "baseSevereThreshold": 16, "baseScore": 3}
  ]'
```

---

### PUT /api/dh/armors/{id}

Update an existing armor. All fields are required (full replacement).

**Authentication:** Required (JWT cookie)
**Authorization:** `ADMIN` or `OWNER` role
**Status:** `200 OK`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `Long` | Yes | The armor ID to update |

**Request Body (UpdateArmorRequest):**

```json
{
  "name": "Reinforced Leather Armor",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "baseMajorThreshold": 6,
  "baseSevereThreshold": 12,
  "baseScore": 2,
  "featureIds": [1, 2],
  "features": [],
  "originalArmorId": null
}
```

**Field Validation:** Identical to CreateArmorRequest (see [POST /api/dh/armors](#post-apidharmors)).

**Response Body:**

```json
{
  "id": 1,
  "name": "Reinforced Leather Armor",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "baseMajorThreshold": 6,
  "baseSevereThreshold": 12,
  "baseScore": 2,
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:31:00"
}
```

Note: Setting `originalArmorId` to `null` (or omitting it) clears the original armor reference on update.

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `400 Bad Request` | Validation failure | `{"status": 400, ...}` |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | User does not have ADMIN/OWNER role | (no body) |
| `404 Not Found` | Armor ID not found, or referenced `expansionId`/`originalArmorId` not found | `{"status": 404, "message": "Armor not found with id: 99999", ...}` |

**curl:**

```bash
curl -s -X PUT http://localhost:8080/api/dh/armors/1 \
  --cookie "AUTH_TOKEN=<jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Reinforced Leather Armor",
    "expansionId": 1,
    "tier": 2,
    "isOfficial": true,
    "baseMajorThreshold": 6,
    "baseSevereThreshold": 12,
    "baseScore": 2
  }'
```

---

### DELETE /api/dh/armors/{id}

Soft-delete an armor by setting its `deletedAt` timestamp.

**Authentication:** Required (JWT cookie)
**Authorization:** `ADMIN` or `OWNER` role
**Status:** `204 No Content`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `Long` | Yes | The armor ID to delete |

**Response Body:** None

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | User does not have ADMIN/OWNER role | (no body) |
| `404 Not Found` | Armor ID not found or already soft-deleted | `{"status": 404, "message": "Armor not found with id: 99999", ...}` |

**curl:**

```bash
curl -s -X DELETE http://localhost:8080/api/dh/armors/1 \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### POST /api/dh/armors/{id}/restore

Restore a soft-deleted armor by clearing its `deletedAt` timestamp.

**Authentication:** Required (JWT cookie)
**Authorization:** `ADMIN` or `OWNER` role
**Status:** `200 OK`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | `Long` | Yes | The armor ID to restore |

**Response Body:**

```json
{
  "id": 1,
  "name": "Leather Armor",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 5,
  "baseSevereThreshold": 10,
  "baseScore": 1,
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `400 Bad Request` | Armor exists but is not currently deleted | `{"status": 400, "message": "Armor with id 1 is not deleted", ...}` |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | User does not have ADMIN/OWNER role | (no body) |
| `404 Not Found` | Armor ID does not exist at all | `{"status": 404, "message": "Armor not found with id: 99999", ...}` |

**curl:**

```bash
curl -s -X POST http://localhost:8080/api/dh/armors/1/restore \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

## Data Models

### ArmorResponse

Returned by all endpoints that produce armor data. Uses `@JsonInclude(NON_NULL)` so null fields are omitted.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | `Long` | No | Unique identifier |
| `name` | `String` | No | Armor name (max 200 chars) |
| `expansionId` | `Long` | No | ID of the parent expansion (always included) |
| `expansion` | `ExpansionResponse` | Yes | Full expansion object (only with `?expand=expansion`) |
| `tier` | `Integer` | No | Tier level (1--4) |
| `isOfficial` | `Boolean` | No | Whether from official content |
| `baseMajorThreshold` | `Integer` | No | Min damage for major injury (positive) |
| `baseSevereThreshold` | `Integer` | No | Min damage for severe injury (positive, >= baseMajorThreshold) |
| `baseScore` | `Integer` | No | Base defensive score (zero or positive) |
| `featureIds` | `List<Long>` | Yes | IDs of associated features (null if none) |
| `features` | `List<FeatureResponse>` | Yes | Full feature objects (only with `?expand=features`) |
| `originalArmorId` | `Long` | Yes | ID of source armor for custom copies (null if original) |
| `originalArmor` | `ArmorResponse` | Yes | Full original armor object (only with `?expand=originalArmor`) |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active; omitted from JSON when null) |

### CreateArmorRequest

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `name` | `String` | Yes | `@NotBlank`, `@Size(max=200)` | Armor name |
| `expansionId` | `Long` | Yes | `@NotNull` | Parent expansion ID |
| `tier` | `Integer` | Yes | `@NotNull`, `@Min(1)`, `@Max(4)` | Tier level (1--4) |
| `isOfficial` | `Boolean` | Yes | `@NotNull` | Official content flag |
| `baseMajorThreshold` | `Integer` | Yes | `@NotNull`, `@Positive` | Major injury threshold |
| `baseSevereThreshold` | `Integer` | Yes | `@NotNull`, `@Positive` | Severe injury threshold |
| `baseScore` | `Integer` | Yes | `@NotNull`, `@PositiveOrZero` | Base defensive score |
| `featureIds` | `List<Long>` | No | - | Existing feature IDs (takes precedence over `features`) |
| `features` | `List<FeatureInput>` | No | `@Valid` | Inline features to find-or-create |
| `originalArmorId` | `Long` | No | - | Source armor ID for custom copies |

### UpdateArmorRequest

Identical field set and validation to CreateArmorRequest. This is a full replacement -- all required fields must be provided. Setting `originalArmorId` to `null` clears the reference.

### FeatureInput (nested in create/update requests)

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `name` | `String` | No | `@Size(max=200)` | Feature name (matched case-insensitively within same expansion+type) |
| `description` | `String` | No | - | What the feature does |
| `featureType` | `FeatureType` | Yes | `@NotNull` | Type/category of the feature |
| `expansionId` | `Long` | Yes | `@NotNull` | Expansion the feature belongs to |
| `costTagIds` | `List<Long>` | No | - | IDs of existing cost tags |
| `costTags` | `List<CostTagInput>` | No | `@Valid` | Inline cost tags to find-or-create (merged with `costTagIds`) |
| `modifierIds` | `List<Long>` | No | - | IDs of existing modifiers |
| `modifiers` | `List<FeatureModifierInput>` | No | `@Valid` | Inline modifiers to find-or-create (merged with `modifierIds`) |

### CostTagInput (nested in FeatureInput)

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `label` | `String` | Yes | `@NotBlank`, `@Size(max=200)` | Display label (e.g., "3 Hope", "1/session") |
| `category` | `CostTagCategory` | Yes | `@NotNull` | Tag category |

### FeatureModifierInput (nested in FeatureInput)

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `target` | `ModifierTarget` | Yes | `@NotNull` | Stat/attribute to modify |
| `operation` | `ModifierOperation` | Yes | `@NotNull` | How the value is applied |
| `value` | `Integer` | Yes | `@NotNull` | Numeric modifier value |

---

## Expanded Objects

When using `?expand=`, the following nested objects may appear in responses.

### ExpansionResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Expansion ID |
| `name` | `String` | Expansion name (e.g., "Core Rulebook") |
| `isPublished` | `Boolean` | Whether published and available |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Soft-deletion timestamp (omitted when null) |

### FeatureResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Feature ID |
| `name` | `String` | Feature name |
| `description` | `String` | Feature description |
| `featureType` | `FeatureType` | Feature type/category |
| `expansionId` | `Long` | Parent expansion ID |
| `expansion` | `ExpansionResponse` | Expanded expansion (only if `expansion` is in expand set) |
| `costTagIds` | `List<Long>` | Associated cost tag IDs |
| `costTags` | `List<CardCostTagResponse>` | Expanded cost tags (only if `costTags` is in expand set) |
| `modifierIds` | `List<Long>` | Associated modifier IDs |
| `modifiers` | `List<FeatureModifierResponse>` | Expanded modifiers (only if `modifiers` is in expand set) |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Soft-deletion timestamp (omitted when null) |

### CardCostTagResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Cost tag ID |
| `label` | `String` | Display label (e.g., "3 Hope") |
| `category` | `CostTagCategory` | Tag category |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Soft-deletion timestamp (omitted when null) |

### FeatureModifierResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Modifier ID |
| `target` | `ModifierTarget` | Stat/attribute targeted |
| `operation` | `ModifierOperation` | Operation applied |
| `value` | `Integer` | Numeric value |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Soft-deletion timestamp (omitted when null) |

---

## Enums

### FeatureType

```
HOPE, ANCESTRY, CLASS, COMMUNITY, DOMAIN, ITEM, OTHER, SUBCLASS
```

### ModifierTarget

```
AGILITY, STRENGTH, FINESSE, INSTINCT, PRESENCE, KNOWLEDGE,
EVASION, MAJOR_DAMAGE_THRESHOLD, SEVERE_DAMAGE_THRESHOLD,
HIT_POINT_MAX, STRESS_MAX, HOPE_MAX, ARMOR_MAX, GOLD,
ATTACK_ROLL, DAMAGE_ROLL, PRIMARY_DAMAGE_ROLL, ARMOR_SCORE
```

### ModifierOperation

```
ADD, SET, MULTIPLY
```

Evaluation order: `SET` first, then `MULTIPLY`, then `ADD`.

### CostTagCategory

```
COST, LIMITATION, TIMING
```

---

## Database Schema

### `armors` table

Created by migration `V20260124200019230__create_armors_table.sql`, updated by subsequent migrations.

```sql
CREATE TABLE armors (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    tier            INTEGER NOT NULL DEFAULT 1 CHECK (tier BETWEEN 1 AND 4),
    base_major_threshold INTEGER NOT NULL CHECK (base_major_threshold > 0),
    base_severe_threshold INTEGER NOT NULL CHECK (base_severe_threshold > 0),
    base_score      INTEGER NOT NULL,
    expansion_id    BIGINT NOT NULL REFERENCES expansions(id),
    is_official     BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id BIGINT REFERENCES users(id),
    original_armor_id  BIGINT REFERENCES armors(id),
    created_at      TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    deleted_at      TIMESTAMP,
    CHECK (base_severe_threshold >= base_major_threshold)
);
```

**Indexes:**
- `idx_armors_expansion` on `expansion_id`
- `idx_armors_is_official` on `is_official`
- `idx_armors_created_by` on `created_by_user_id`
- `idx_armors_original` on `original_armor_id`
- `idx_armors_active` partial index on `(expansion_id, is_official) WHERE deleted_at IS NULL`

### `armor_features` join table

Created by migration `V20260228145241147__move_features_to_base_item.sql`.

```sql
CREATE TABLE armor_features (
    armor_id   BIGINT NOT NULL REFERENCES armors(id),
    feature_id BIGINT NOT NULL REFERENCES features(id),
    PRIMARY KEY (armor_id, feature_id)
);
```

**Related migrations:**
- `V20260124200019230` -- Initial `armors` table creation
- `V20260228113319717` -- Added `tier` column (1--4) to armors
- `V20260228145241147` -- Moved feature relationship from `feature_id` column to `armor_features` join table (many-to-many)
- `V20260228212539470` -- Added `ARMOR_SCORE` to allowed modifier targets

---

## Test Data Reference

Values used in integration and unit tests for crafting valid requests.

### Standard Armor Payloads

**Leather Armor (Tier 1):**

```json
{
  "name": "Leather Armor",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 5,
  "baseSevereThreshold": 10,
  "baseScore": 1
}
```

**Plate Mail (Tier 2--3):**

```json
{
  "name": "Plate Mail",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "baseMajorThreshold": 8,
  "baseSevereThreshold": 16,
  "baseScore": 3
}
```

**Magic Shield with Feature (Tier 1):**

```json
{
  "name": "Magic Shield",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "baseMajorThreshold": 7,
  "baseSevereThreshold": 14,
  "baseScore": 2,
  "featureIds": [1]
}
```

**Update Payload (Reinforced Leather Armor):**

```json
{
  "name": "Reinforced Leather Armor",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "baseMajorThreshold": 6,
  "baseSevereThreshold": 12,
  "baseScore": 2
}
```

### Test Expansion

```json
{
  "name": "Core Rulebook",
  "isPublished": true
}
```

### Test Users

| Username | Role | Password |
|----------|------|----------|
| `admin` | `ADMIN` | `Password123!` |
| `user` | `USER` | `Password123!` |

### Authentication

All requests use an `AUTH_TOKEN` HttpOnly cookie containing a JWT. Admin-only endpoints (POST, PUT, DELETE) return `403 Forbidden` when called with a USER-role token.
