# Companions API Reference

Base URL: `http://localhost:8080`

## Overview

Companions are entities attached to character sheets in the Daggerheart TTRPG system. Each companion has its own attack, evasion, stress tracking, and can accumulate experiences. Companions use **hard deletion** (not soft delete) -- deleting a companion permanently removes it and cascades to associated experiences.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:** Ownership-based. Write operations (POST/PUT/DELETE) require the caller to be the character sheet owner OR have MODERATOR/ADMIN/OWNER role. This is enforced in the service layer, not via `@PreAuthorize`.

---

## Endpoints

### GET /api/dh/companions

Retrieves a paginated list of companions.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter        | Type   | Default | Description                                      |
|-----------------|--------|---------|--------------------------------------------------|
| page            | int    | 0       | Zero-based page number                           |
| size            | int    | 20      | Items per page (max: 100)                        |
| characterSheetId| Long   | -       | Filter by character sheet ID                     |
| expand          | string | -       | Comma-separated relationships to expand          |

**Expand Options:** `characterSheet`, `experiences`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "characterSheetId": 1,
      "name": "Wolf",
      "description": "A Wolf companion",
      "evasion": 12,
      "attackName": "Bite",
      "attackRange": "CLOSE",
      "damageDice": "D6",
      "stressMax": 3,
      "stressMarked": 0,
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

**With `?expand=characterSheet`:**

```json
{
  "content": [
    {
      "id": 1,
      "characterSheetId": 1,
      "characterSheet": {
        "id": 1,
        "name": "Aragorn",
        ...
      },
      "name": "Wolf",
      ...
    }
  ],
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Invalid characterSheetId filter

---

### GET /api/dh/companions/{id}

Retrieves a single companion by ID.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The companion ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|------------------------------------------|
| expand    | string | Comma-separated relationships to expand  |

**Expand Options:** `characterSheet`, `experiences`

**Response:** `200 OK`

```json
{
  "id": 1,
  "characterSheetId": 1,
  "name": "Wolf",
  "description": "A Wolf companion",
  "evasion": 12,
  "attackName": "Bite",
  "attackRange": "CLOSE",
  "damageDice": "D6",
  "stressMax": 3,
  "stressMarked": 0,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Companion with given ID does not exist

---

### POST /api/dh/companions

Creates a new companion for a character.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Request Body:**

```json
{
  "characterSheetId": 1,
  "name": "Wolf",
  "description": "A loyal wolf companion",
  "evasion": 12,
  "attackName": "Bite",
  "attackRange": "CLOSE",
  "damageDice": "D6",
  "stressMax": 3,
  "stressMarked": 0
}
```

**Field Validation:**

| Field            | Type     | Required | Default | Constraints              |
|-----------------|----------|----------|---------|--------------------------|
| characterSheetId| Long     | Yes      | -       | Must reference existing sheet |
| name            | String   | Yes      | -       | Max 200 characters, not blank |
| description     | String   | No       | -       | Max 5000 characters      |
| evasion         | Integer  | No       | 0       | -                        |
| attackName      | String   | Yes      | -       | Max 200 characters, not blank |
| attackRange     | Range    | Yes      | -       | Valid Range enum value   |
| damageDice      | DiceType | Yes      | -       | Valid DiceType enum value|
| stressMax       | Integer  | No       | 3       | -                        |
| stressMarked    | Integer  | No       | 0       | -                        |

**Response:** `201 Created`

```json
{
  "id": 1,
  "characterSheetId": 1,
  "name": "Wolf",
  "description": "A loyal wolf companion",
  "evasion": 12,
  "attackName": "Bite",
  "attackRange": "CLOSE",
  "damageDice": "D6",
  "stressMax": 3,
  "stressMarked": 0,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Missing required fields (name, attackName, attackRange, damageDice)
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Referenced character sheet does not exist

---

### PUT /api/dh/companions/{id}

Updates an existing companion. Supports partial updates -- only non-null fields are updated.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The companion ID   |

**Request Body (all fields optional):**

```json
{
  "name": "Shadow Wolf",
  "stressMarked": 2
}
```

**Field Validation:**

| Field       | Type     | Constraints              |
|-------------|----------|--------------------------|
| name        | String   | Max 200 characters       |
| description | String   | Max 5000 characters      |
| evasion     | Integer  | -                        |
| attackName  | String   | Max 200 characters       |
| attackRange | Range    | Valid Range enum value   |
| damageDice  | DiceType | Valid DiceType enum value|
| stressMax   | Integer  | -                        |
| stressMarked| Integer  | -                        |

**Response:** `200 OK` -- Updated `CompanionResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion with given ID does not exist

---

### DELETE /api/dh/companions/{id}

Permanently deletes a companion (hard delete). Cascades to associated experiences.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion with given ID does not exist

---

## Enums

### Range

6 values:

| Value        | Description                                            |
|--------------|--------------------------------------------------------|
| MELEE        | Close-quarters combat, under 5 feet                    |
| VERY_CLOSE   | Extended melee or point-blank range, 5-10 feet         |
| CLOSE        | Short throwing distance, 10-30 feet                    |
| FAR          | Standard ranged weapon distance, 30-100 feet           |
| VERY_FAR     | Long-range projectile distance, 100-300 feet           |
| OUT_OF_RANGE | Extreme distance beyond normal effectiveness, 300+ ft  |

### DiceType

6 values:

| Value | Sides | Code |
|-------|-------|------|
| D4    | 4     | d4   |
| D6    | 6     | d6   |
| D8    | 8     | d8   |
| D10   | 10    | d10  |
| D12   | 12    | d12  |
| D20   | 20    | d20  |

---

## Database Schema

**Table:** `companions`

| Column            | Type         | Nullable | Notes                        |
|-------------------|--------------|----------|------------------------------|
| id                | BIGSERIAL    | No       | Primary key                  |
| character_sheet_id| BIGINT       | No       | FK to character_sheets (CASCADE) |
| name              | VARCHAR(200) | No       |                              |
| description       | TEXT         | Yes      |                              |
| evasion           | INTEGER      | No       | Default 0                    |
| attack_name       | VARCHAR(200) | No       |                              |
| attack_range      | VARCHAR(50)  | No       | Range enum                   |
| damage_dice       | VARCHAR(10)  | No       | DiceType enum                |
| stress_max        | INTEGER      | No       | Default 3                    |
| stress_marked     | INTEGER      | No       | Default 0                    |
| created_at        | TIMESTAMP    | No       | Auto-set                     |
| last_modified_at  | TIMESTAMP    | No       | Auto-set                     |

**Note:** No `deleted_at` column -- companions use hard deletion.

---

## Test Examples (from integration tests)

### Create Companion (as character sheet owner)
```bash
curl -X POST http://localhost:8080/api/dh/companions \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{
    "characterSheetId": 1,
    "name": "Wolf",
    "description": "A loyal wolf companion",
    "attackName": "Bite",
    "attackRange": "CLOSE",
    "damageDice": "D6",
    "evasion": 12,
    "stressMax": 3,
    "stressMarked": 0
  }'
```

### Update Companion (partial update -- mark stress)
```bash
curl -X PUT http://localhost:8080/api/dh/companions/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{"stressMarked": 2}'
```

### Get Companions with Expanded Character Sheet
```bash
curl "http://localhost:8080/api/dh/companions?expand=characterSheet" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Filter Companions by Character Sheet
```bash
curl "http://localhost:8080/api/dh/companions?characterSheetId=1" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Delete Companion (permanent)
```bash
curl -X DELETE http://localhost:8080/api/dh/companions/1 \
  --cookie "AUTH_TOKEN=<owner_jwt>"
```
