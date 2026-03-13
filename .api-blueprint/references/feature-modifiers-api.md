# Feature Modifiers API Reference

Base URL: `http://localhost:8080`

## Overview

Feature modifiers represent structured, machine-readable stat modifications (e.g., +1 Strength, -1 Evasion) that can be associated with Features. Modifiers are shared across features via a many-to-many relationship. A unique constraint on `(target, operation, value)` for active records prevents duplicate modifier definitions.

Feature modifiers support soft deletion and the find-or-create pattern: when creating features with inline modifiers, the system reuses existing modifiers matching the same (target, operation, value) tuple.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

---

## Endpoints

### GET /api/dh/feature-modifiers

Retrieves a paginated list of feature modifiers.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter       | Type    | Default | Description                                          |
|----------------|---------|---------|------------------------------------------------------|
| page           | int     | 0       | Zero-based page number                               |
| size           | int     | 20      | Items per page (max: 100, capped server-side)        |
| includeDeleted | boolean | false   | Include soft-deleted modifiers (ADMIN+ only)         |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "target": "STRENGTH",
      "operation": "ADD",
      "value": 1,
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    },
    {
      "id": 2,
      "target": "EVASION",
      "operation": "ADD",
      "value": -1,
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token

---

### GET /api/dh/feature-modifiers/{id}

Retrieves a single feature modifier by ID.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description              |
|-----------|------|--------------------------|
| id        | Long | The feature modifier ID  |

**Response:** `200 OK`

```json
{
  "id": 1,
  "target": "STRENGTH",
  "operation": "ADD",
  "value": 1,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Note:** Soft-deleted modifiers return `404` (they are filtered by `findByIdAndDeletedAtIsNull`).

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Modifier with given ID does not exist or is soft-deleted

---

### POST /api/dh/feature-modifiers

Creates a new feature modifier.

**Authorization:** ADMIN or OWNER role required

**Request Body:**

```json
{
  "target": "STRENGTH",
  "operation": "ADD",
  "value": 1
}
```

**Field Validation:**

| Field     | Type              | Required | Constraints                     |
|-----------|-------------------|----------|---------------------------------|
| target    | ModifierTarget    | Yes      | Must be a valid enum value      |
| operation | ModifierOperation | Yes      | Must be a valid enum value      |
| value     | Integer           | Yes      | Any integer (negative allowed)  |

**Response:** `201 Created`

```json
{
  "id": 1,
  "target": "STRENGTH",
  "operation": "ADD",
  "value": 1,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Null target, operation, or value
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role

---

### DELETE /api/dh/feature-modifiers/{id}

Soft deletes a feature modifier (sets `deletedAt` timestamp).

**Authorization:** ADMIN or OWNER role required

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Modifier with given ID does not exist

---

### POST /api/dh/feature-modifiers/{id}/restore

Restores a soft-deleted feature modifier.

**Authorization:** ADMIN or OWNER role required

**Response:** `200 OK`

```json
{
  "id": 1,
  "target": "STRENGTH",
  "operation": "ADD",
  "value": 1,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request / IllegalStateException` -- Modifier is not deleted
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Modifier with given ID does not exist

---

## Enums

### ModifierTarget

18 values:

| Value                    | Description                                          |
|--------------------------|------------------------------------------------------|
| AGILITY                  | Modifies the character's Agility trait score          |
| STRENGTH                 | Modifies the character's Strength trait score         |
| FINESSE                  | Modifies the character's Finesse trait score          |
| INSTINCT                 | Modifies the character's Instinct trait score         |
| PRESENCE                 | Modifies the character's Presence trait score         |
| KNOWLEDGE                | Modifies the character's Knowledge trait score        |
| EVASION                  | Modifies the character's Evasion defense value        |
| MAJOR_DAMAGE_THRESHOLD   | Modifies the character's Major damage threshold       |
| SEVERE_DAMAGE_THRESHOLD  | Modifies the character's Severe damage threshold      |
| HIT_POINT_MAX            | Modifies the character's maximum Hit Points           |
| STRESS_MAX               | Modifies the character's maximum Stress capacity      |
| HOPE_MAX                 | Modifies the character's maximum Hope                 |
| ARMOR_MAX                | Modifies the character's maximum Armor slots          |
| GOLD                     | Modifies the character's starting Gold                |
| ATTACK_ROLL              | Modifies the character's attack roll result           |
| DAMAGE_ROLL              | Modifies the character's damage roll result           |
| PRIMARY_DAMAGE_ROLL      | Modifies the character's primary damage roll result   |
| ARMOR_SCORE              | Modifies the character's armor score                  |

### ModifierOperation

3 values (evaluated in order: SET first, then MULTIPLY, then ADD):

| Value    | Description                                             |
|----------|---------------------------------------------------------|
| ADD      | Adds the value to the target attribute                  |
| SET      | Sets the target attribute to the specified value        |
| MULTIPLY | Multiplies the target attribute by the specified value  |

---

## Find-or-Create Semantics

When modifiers are specified inline via `FeatureModifierInput` (in feature create/update requests), the service uses find-or-create logic:

1. Searches for an existing active modifier with matching `(target, operation, value)`
2. If found, reuses the existing modifier
3. If not found, creates a new modifier

This ensures no duplicate modifiers exist in the system. The `resolveModifiers` method merges results from both `modifierIds` and `modifiers` inputs, deduplicating by entity identity.

---

## Database Schema

**Table:** `feature_modifiers`

| Column          | Type         | Nullable | Notes                                    |
|-----------------|--------------|----------|------------------------------------------|
| id              | BIGSERIAL    | No       | Primary key                              |
| target          | VARCHAR(30)  | No       | ModifierTarget enum                      |
| operation       | VARCHAR(10)  | No       | ModifierOperation enum                   |
| value           | INTEGER      | No       | Numeric modifier value                   |
| created_at      | TIMESTAMP    | No       | Auto-set                                 |
| last_modified_at| TIMESTAMP    | No       | Auto-set                                 |
| deleted_at      | TIMESTAMP    | Yes      | Null = active                            |

**Unique Index:** `uq_feature_modifiers_active` on `(target, operation, value)` WHERE `deleted_at IS NULL`

---

## Test Examples (from integration tests)

### Create Modifier with ADD Operation
```bash
curl -X POST http://localhost:8080/api/dh/feature-modifiers \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{"target": "STRENGTH", "operation": "ADD", "value": 1}'
```

### Create Modifier with SET Operation
```bash
curl -X POST http://localhost:8080/api/dh/feature-modifiers \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{"target": "HIT_POINT_MAX", "operation": "SET", "value": 15}'
```

### Create Modifier with MULTIPLY Operation
```bash
curl -X POST http://localhost:8080/api/dh/feature-modifiers \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{"target": "GOLD", "operation": "MULTIPLY", "value": 2}'
```

### Create Modifier with Negative Value
```bash
curl -X POST http://localhost:8080/api/dh/feature-modifiers \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<admin_jwt>" \
  -d '{"target": "EVASION", "operation": "ADD", "value": -1}'
```

### List Modifiers with Pagination
```bash
curl "http://localhost:8080/api/dh/feature-modifiers?page=1&size=2" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Include Deleted Modifiers
```bash
curl "http://localhost:8080/api/dh/feature-modifiers?includeDeleted=true" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Delete Modifier (soft delete)
```bash
curl -X DELETE http://localhost:8080/api/dh/feature-modifiers/1 \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```

### Restore Modifier
```bash
curl -X POST http://localhost:8080/api/dh/feature-modifiers/1/restore \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```
