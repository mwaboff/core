# Experiences API Reference

Base URL: `http://localhost:8080`

## Overview

Experiences represent significant events, accomplishments, and learning moments for a character or companion in the Daggerheart TTRPG system. Each experience provides a modifier (default +2) that applies when the character/companion attempts actions related to that experience.

An experience belongs to exactly one of: a character sheet OR a companion (enforced by a database check constraint). Experiences use **hard deletion** (not soft delete).

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:**
- GET endpoints: Any authenticated user
- POST: Any authenticated user (current user recorded as creator)
- PUT/DELETE: Character sheet owner OR MODERATOR/ADMIN/OWNER role (enforced in service layer)

---

## Endpoints

### GET /api/dh/experiences

Retrieves a paginated list of experiences.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter        | Type   | Default | Description                                      |
|-----------------|--------|---------|--------------------------------------------------|
| page            | int    | 0       | Zero-based page number                           |
| size            | int    | 20      | Items per page (max: 100)                        |
| characterSheetId| Long   | -       | Filter by character sheet ID                     |
| companionId     | Long   | -       | Filter by companion ID                           |
| expand          | string | -       | Comma-separated relationships to expand          |

**Expand Options:** `characterSheet`, `companion`, `createdBy`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "characterSheetId": 1,
      "companionId": null,
      "createdById": 1,
      "description": "Survived dragon attack",
      "modifier": 2,
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

**With `?expand=characterSheet,createdBy`:**

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
      "createdById": 1,
      "createdBy": {
        "id": 1,
        "username": "player1",
        ...
      },
      "description": "Survived dragon attack",
      "modifier": 2,
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Invalid characterSheetId or companionId filter

---

### GET /api/dh/experiences/{id}

Retrieves a single experience by ID.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description         |
|-----------|------|---------------------|
| id        | Long | The experience ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|------------------------------------------|
| expand    | string | Comma-separated relationships to expand  |

**Expand Options:** `characterSheet`, `companion`, `createdBy`

**Response:** `200 OK`

```json
{
  "id": 1,
  "characterSheetId": 1,
  "createdById": 1,
  "description": "Survived dragon attack",
  "modifier": 2,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Experience with given ID does not exist

---

### POST /api/dh/experiences

Creates a new experience. The authenticated user is recorded as the creator.

**Authorization:** Any authenticated user

**Request Body:**

```json
{
  "characterSheetId": 1,
  "description": "Survived dragon attack on Redstone Village",
  "modifier": 2
}
```

Or for a companion experience:

```json
{
  "companionId": 1,
  "description": "Tracked prey through dense forest",
  "modifier": 2
}
```

**Field Validation:**

| Field            | Type    | Required | Default | Constraints                          |
|-----------------|---------|----------|---------|--------------------------------------|
| characterSheetId| Long    | No*      | -       | Must reference existing sheet        |
| companionId     | Long    | No*      | -       | Must reference existing companion    |
| description     | String  | Yes      | -       | Not blank, max 5000 characters       |
| modifier        | Integer | No       | 2       | Standard Daggerheart experience bonus|

*Exactly one of `characterSheetId` or `companionId` must be provided.

**Response:** `201 Created`

```json
{
  "id": 1,
  "characterSheetId": 1,
  "createdById": 1,
  "description": "Survived dragon attack on Redstone Village",
  "modifier": 2,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Missing required fields (description blank)
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Referenced character sheet or companion does not exist

---

### PUT /api/dh/experiences/{id}

Updates an existing experience. Supports partial updates -- only non-null fields are updated.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Path Parameters:**

| Parameter | Type | Description         |
|-----------|------|---------------------|
| id        | Long | The experience ID   |

**Request Body (all fields optional):**

```json
{
  "description": "Survived dragon attack on Redstone Village",
  "modifier": 3
}
```

**Field Validation:**

| Field       | Type    | Constraints          |
|-------------|---------|----------------------|
| description | String  | Max 5000 characters  |
| modifier    | Integer | -                    |

**Response:** `200 OK` -- Updated `ExperienceResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Experience with given ID does not exist

---

### DELETE /api/dh/experiences/{id}

Permanently deletes an experience (hard delete).

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Experience with given ID does not exist

---

## Database Schema

**Table:** `experiences`

| Column              | Type      | Nullable | Notes                           |
|---------------------|-----------|----------|---------------------------------|
| id                  | BIGSERIAL | No       | Primary key                     |
| character_sheet_id  | BIGINT    | Yes      | FK to character_sheets (CASCADE)|
| companion_id        | BIGINT    | Yes      | FK to companions (CASCADE)      |
| created_by_user_id  | BIGINT    | No       | FK to users (CASCADE)           |
| description         | TEXT      | No       |                                 |
| modifier            | INTEGER   | No       | Default 2                       |
| created_at          | TIMESTAMP | No       | Auto-set                        |
| last_modified_at    | TIMESTAMP | No       | Auto-set                        |

**Check Constraint:** `chk_experience_single_owner` -- Exactly one of `character_sheet_id` or `companion_id` must be non-null.

**Note:** No `deleted_at` column -- experiences use hard deletion.

---

## Test Examples (from integration tests)

### Create Experience for Character Sheet
```bash
curl -X POST http://localhost:8080/api/dh/experiences \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{
    "characterSheetId": 1,
    "description": "Survived dragon attack on Redstone Village",
    "modifier": 2
  }'
```

### Create Experience with Default Modifier
```bash
curl -X POST http://localhost:8080/api/dh/experiences \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{
    "characterSheetId": 1,
    "description": "Survived dragon attack"
  }'
```
Response will have `"modifier": 2` (default).

### Get Experience with Expanded Relationships
```bash
curl "http://localhost:8080/api/dh/experiences/1?expand=characterSheet,createdBy" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Filter Experiences by Character Sheet
```bash
curl "http://localhost:8080/api/dh/experiences?characterSheetId=1" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Update Experience (partial)
```bash
curl -X PUT http://localhost:8080/api/dh/experiences/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{
    "description": "Survived dragon attack on Redstone Village",
    "modifier": 3
  }'
```

### Delete Experience (permanent)
```bash
curl -X DELETE http://localhost:8080/api/dh/experiences/1 \
  --cookie "AUTH_TOKEN=<owner_jwt>"
```
