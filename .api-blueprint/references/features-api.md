# Features API Reference

Base URL: `http://localhost:8080`

## Overview

Features represent special abilities, traits, and bonuses in the Daggerheart TTRPG system. They can be granted by cards, classes, ancestries, communities, domains, or other game elements. Features support soft deletion, cost tag associations, modifier associations, and response expansion.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

---

## Endpoints

### GET /api/dh/features

Retrieves a paginated list of features.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter       | Type    | Default | Description                                      |
|----------------|---------|---------|--------------------------------------------------|
| page           | int     | 0       | Zero-based page number                           |
| size           | int     | 20      | Items per page (max: 100)                        |
| includeDeleted | boolean | false   | Include soft-deleted features (ADMIN+ only)      |
| expansionId    | Long    | -       | Filter by expansion ID                           |
| featureType    | string  | -       | Filter by feature type enum value                |
| expand         | string  | -       | Comma-separated relationships to expand          |

**Expand Options:** `expansion`, `costTags`, `modifiers`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Hope Feature",
      "description": "Description for Hope Feature",
      "featureType": "HOPE",
      "expansionId": 1,
      "costTagIds": [],
      "modifierIds": [1, 2],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**With `?expand=expansion,modifiers`:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Hope Feature",
      "description": "Description for Hope Feature",
      "featureType": "HOPE",
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook",
        "isPublished": true,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "costTagIds": [],
      "modifierIds": [1],
      "modifiers": [
        {
          "id": 1,
          "target": "STRENGTH",
          "operation": "ADD",
          "value": 2,
          "createdAt": "2026-03-13T10:00:00",
          "lastModifiedAt": "2026-03-13T10:00:00"
        }
      ],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token

---

### GET /api/dh/features/{id}

Retrieves a single feature by ID.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description      |
|-----------|------|------------------|
| id        | Long | The feature ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|------------------------------------------|
| expand    | string | Comma-separated relationships to expand  |

**Expand Options:** `expansion`, `costTags`, `modifiers`

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Hope Feature",
  "description": "Description for Hope Feature",
  "featureType": "HOPE",
  "expansionId": 1,
  "costTagIds": [],
  "modifierIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Feature with given ID does not exist

---

### POST /api/dh/features

Creates a new feature.

**Authorization:** ADMIN or OWNER role required

**Request Body:**

```json
{
  "name": "New Feature",
  "description": "Feature description",
  "featureType": "HOPE",
  "expansionId": 1,
  "costTagIds": [1, 2],
  "costTags": [
    {
      "label": "3 Hope",
      "category": "COST"
    }
  ],
  "modifierIds": [1],
  "modifiers": [
    {
      "target": "STRENGTH",
      "operation": "ADD",
      "value": 1
    },
    {
      "target": "EVASION",
      "operation": "ADD",
      "value": -1
    }
  ]
}
```

**Field Validation:**

| Field         | Type             | Required | Constraints                              |
|---------------|------------------|----------|------------------------------------------|
| name          | String           | No       | Max 200 characters                       |
| description   | String           | No       | -                                        |
| featureType   | FeatureType      | Yes      | Must be a valid FeatureType enum value   |
| expansionId   | Long             | Yes      | Must reference an existing expansion     |
| costTagIds    | List\<Long\>     | No       | IDs of existing cost tags                |
| costTags      | List\<CostTagInput\> | No   | Find-or-create by label                  |
| modifierIds   | List\<Long\>     | No       | IDs of existing modifiers                |
| modifiers     | List\<FeatureModifierInput\> | No | Find-or-create by (target, operation, value) |

**Note:** `costTagIds` and `costTags` are merged if both provided. Same for `modifierIds` and `modifiers`.

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "New Feature",
  "description": "Feature description",
  "featureType": "HOPE",
  "expansionId": 1,
  "costTagIds": [1, 2, 3],
  "modifierIds": [1, 4, 5],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Referenced expansion ID does not exist

---

### POST /api/dh/features/bulk

Creates multiple features in bulk.

**Authorization:** ADMIN or OWNER role required

**Request Body:** Array of `CreateFeatureRequest` objects (same schema as POST /api/dh/features).

```json
[
  {
    "name": "Feature One",
    "description": "First feature",
    "featureType": "CLASS",
    "expansionId": 1
  },
  {
    "name": "Feature Two",
    "description": "Second feature",
    "featureType": "ANCESTRY",
    "expansionId": 1
  }
]
```

**Response:** `201 Created` -- Array of `FeatureResponse` objects.

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role

---

### PUT /api/dh/features/{id}

Updates an existing feature.

**Authorization:** ADMIN or OWNER role required

**Path Parameters:**

| Parameter | Type | Description      |
|-----------|------|------------------|
| id        | Long | The feature ID   |

**Request Body:**

```json
{
  "name": "Updated Name",
  "description": "Updated description",
  "featureType": "CLASS",
  "expansionId": 1,
  "modifiers": [
    {
      "target": "EVASION",
      "operation": "ADD",
      "value": -2
    }
  ]
}
```

**Field Validation:** Same as create. `featureType` and `expansionId` are required.

**Modifier Behavior:**
- If `modifierIds` or `modifiers` is provided: replaces existing modifiers
- If both `modifierIds` and `modifiers` are null: preserves existing modifiers
- Same logic applies to `costTagIds` and `costTags`

**Response:** `200 OK` -- Updated `FeatureResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Feature with given ID does not exist

---

### DELETE /api/dh/features/{id}

Soft deletes a feature (sets `deletedAt` timestamp).

**Authorization:** ADMIN or OWNER role required

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Feature with given ID does not exist

---

### POST /api/dh/features/{id}/restore

Restores a soft-deleted feature (clears `deletedAt` timestamp).

**Authorization:** ADMIN or OWNER role required

**Response:** `200 OK` -- Restored `FeatureResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Feature with given ID does not exist

---

## Enums

### FeatureType

8 values:

| Value       | Description                  |
|-------------|------------------------------|
| HOPE        | Hope feature type            |
| ANCESTRY    | Ancestry feature type        |
| CLASS       | Class feature type           |
| COMMUNITY   | Community feature type       |
| DOMAIN      | Domain feature type          |
| ITEM        | Item feature type            |
| SUBCLASS    | Subclass feature type        |
| OTHER       | Other feature type           |

### CostTagCategory

3 values:

| Value      | Description                                              |
|------------|----------------------------------------------------------|
| COST       | Resource expenditure tags (e.g., "3 Hope", "1 Stress")  |
| LIMITATION | Restriction/requirement tags (e.g., "Close range")      |
| TIMING     | Frequency/action type tags (e.g., "1/session", "Action") |

---

## Nested DTOs

### CostTagInput (for create/update requests)

```json
{
  "label": "3 Hope",
  "category": "COST"
}
```

| Field    | Type            | Required | Constraints          |
|----------|-----------------|----------|----------------------|
| label    | String          | Yes      | Max 200 characters   |
| category | CostTagCategory | Yes      | Valid enum value     |

### FeatureModifierInput (for create/update requests)

```json
{
  "target": "STRENGTH",
  "operation": "ADD",
  "value": 1
}
```

| Field     | Type              | Required | Constraints       |
|-----------|-------------------|----------|-------------------|
| target    | ModifierTarget    | Yes      | Valid enum value  |
| operation | ModifierOperation | Yes      | Valid enum value  |
| value     | Integer           | Yes      | Any integer       |

### FeatureInput (used by other APIs to inline-create features)

```json
{
  "name": "Inline Feature",
  "description": "Created inline with a card",
  "featureType": "CLASS",
  "expansionId": 1,
  "costTagIds": [],
  "costTags": [],
  "modifierIds": [],
  "modifiers": []
}
```

---

## Database Schema

**Table:** `features`

| Column        | Type         | Nullable | Notes                      |
|---------------|--------------|----------|----------------------------|
| id            | BIGSERIAL    | No       | Primary key                |
| name          | VARCHAR(200) | Yes      | Made nullable in migration |
| description   | TEXT         | Yes      |                            |
| feature_type  | VARCHAR(20)  | No       | FeatureType enum           |
| expansion_id  | BIGINT       | No       | FK to expansions           |
| created_at    | TIMESTAMP    | No       | Auto-set                   |
| last_modified_at | TIMESTAMP | No       | Auto-set                   |
| deleted_at    | TIMESTAMP    | Yes      | Null = active              |

**Join Table:** `feature_card_cost_tags` (feature_id, card_cost_tag_id)

**Join Table:** `feature_feature_modifiers` (feature_id, feature_modifier_id)

---

## Test Examples (from integration tests)

### Create Feature (as ADMIN)
```bash
curl -X POST http://localhost:8080/api/dh/features \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{
    "name": "New Feature",
    "description": "Feature description",
    "featureType": "HOPE",
    "expansionId": 1
  }'
```

### Create Feature with Inline Modifiers
```bash
curl -X POST http://localhost:8080/api/dh/features \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{
    "name": "Modifier Feature",
    "description": "Feature with inline modifiers",
    "featureType": "CLASS",
    "expansionId": 1,
    "modifiers": [
      {"target": "STRENGTH", "operation": "ADD", "value": 1},
      {"target": "EVASION", "operation": "ADD", "value": -1}
    ]
  }'
```

### Get Feature with Expanded Modifiers
```bash
curl http://localhost:8080/api/dh/features/1?expand=modifiers \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Filter by Feature Type
```bash
curl "http://localhost:8080/api/dh/features?featureType=HOPE" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Update Feature
```bash
curl -X PUT http://localhost:8080/api/dh/features/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{
    "name": "Updated Name",
    "description": "Updated description",
    "featureType": "CLASS",
    "expansionId": 1
  }'
```

### Delete Feature (soft delete)
```bash
curl -X DELETE http://localhost:8080/api/dh/features/1 \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```

### Restore Feature
```bash
curl -X POST http://localhost:8080/api/dh/features/1/restore \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```
