# Encounters API Reference

Base URL: `http://localhost:8080`

## Overview

Encounters represent groups of adversaries designed for combat scenarios in the Daggerheart TTRPG system. They track adversary compositions, battle point totals for encounter balancing, tier levels, and support the official/public/custom content management pattern. Encounters support copying (for customization), soft deletion, and visibility filtering.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:**
- GET endpoints: All authenticated users (filtered by visibility -- official, public, or user's own)
- POST (create/copy): All authenticated users (creator is set to current user)
- PUT/DELETE: Creator OR MODERATOR+ for non-official; OWNER only for official encounters
- POST restore: ADMIN or OWNER only (`@PreAuthorize`)

**Note:** No integration tests or service tests exist for this controller at the time of writing.

---

## Endpoints

### GET /api/dh/encounters

Retrieves a paginated list of encounters. Returns encounters that are official, public, or created by the authenticated user.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter       | Type    | Default | Description                                           |
|----------------|---------|---------|-------------------------------------------------------|
| page           | int     | 0       | Zero-based page number                                |
| size           | int     | 20      | Items per page (max: 100)                             |
| includeDeleted | boolean | false   | Include soft-deleted encounters (ADMIN+ only)         |
| campaignId     | Long    | -       | Filter by campaign ID                                 |
| tier           | Integer | -       | Filter by tier (1-4)                                  |
| isOfficial     | Boolean | -       | Filter by official status                             |
| name           | String  | -       | Filter by name (partial match, case-insensitive)      |
| expand         | string  | -       | Comma-separated relationships to expand               |

**Expand Options:** `creator`, `campaign`, `originalEncounter`, `adversaryDetails`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Goblin Ambush",
      "description": "A group of goblins attacks the party on the forest road",
      "tier": 1,
      "isOfficial": false,
      "isPublic": true,
      "campaignId": null,
      "originalEncounterId": null,
      "creatorId": 1,
      "adversaries": [
        {
          "id": 1,
          "adversaryId": 5
        },
        {
          "id": 2,
          "adversaryId": 5
        },
        {
          "id": 3,
          "adversaryId": 7
        }
      ],
      "totalBattlePoints": 12,
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

**With `?expand=creator,adversaryDetails`:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Goblin Ambush",
      "creatorId": 1,
      "creator": {
        "id": 1,
        "username": "player1",
        ...
      },
      "adversaries": [
        {
          "id": 1,
          "adversaryId": 5,
          "adversary": {
            "id": 5,
            "name": "Goblin Scout",
            ...
          }
        }
      ],
      ...
    }
  ],
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token

---

### GET /api/dh/encounters/{id}

Retrieves a single encounter by ID. Access is restricted to official, public, or user's own encounters.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|------------------------------------------|
| expand    | string | Comma-separated relationships to expand  |

**Expand Options:** `creator`, `campaign`, `originalEncounter`, `adversaryDetails`

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "isOfficial": false,
  "isPublic": true,
  "campaignId": null,
  "originalEncounterId": null,
  "creatorId": 1,
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 5
    }
  ],
  "totalBattlePoints": 4,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Encounter does not exist or user cannot access it

---

### POST /api/dh/encounters

Creates a new encounter. The authenticated user becomes the creator.

**Authorization:** Any authenticated user

**Request Body:**

```json
{
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "campaignId": null,
  "isPublic": false,
  "adversaryIds": [5, 5, 7]
}
```

**Field Validation:**

| Field        | Type          | Required | Default | Constraints                              |
|-------------|---------------|----------|---------|------------------------------------------|
| name        | String        | Yes      | -       | Not blank, max 200 characters            |
| description | String        | No       | -       | -                                        |
| tier        | Integer       | No       | -       | 1-4 (null if multi-tier)                 |
| campaignId  | Long          | No       | -       | Must reference existing campaign         |
| isPublic    | Boolean       | No       | false   | -                                        |
| adversaryIds| List\<Long\>  | No       | -       | Adversary IDs; repeat ID for multiples   |

**Note on adversaryIds:** Each entry in the list represents a single adversary instance. To include 2 Goblin Scouts (adversary ID 5), include `[5, 5]`.

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "isOfficial": false,
  "isPublic": false,
  "creatorId": 1,
  "adversaries": [
    {"id": 1, "adversaryId": 5},
    {"id": 2, "adversaryId": 5},
    {"id": 3, "adversaryId": 7}
  ],
  "totalBattlePoints": 12,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Missing name or invalid tier
- `401 Unauthorized` -- Missing or invalid JWT token

---

### POST /api/dh/encounters/{id}/copy

Creates a copy of an existing encounter for the authenticated user. The copy is private by default and linked to the original via `originalEncounterId`.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description                    |
|-----------|------|--------------------------------|
| id        | Long | ID of the encounter to copy    |

**Response:** `201 Created` -- New encounter with `originalEncounterId` set to the source.

```json
{
  "id": 2,
  "name": "Goblin Ambush",
  "originalEncounterId": 1,
  "isOfficial": false,
  "isPublic": false,
  "creatorId": 2,
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Source encounter does not exist or user cannot access it

---

### PUT /api/dh/encounters/{id}

Updates an existing encounter. Supports partial updates.

**Authorization:**
- Official encounters: OWNER role only
- Non-official encounters: Creator OR MODERATOR+ role

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Request Body (all fields optional):**

```json
{
  "name": "Updated Goblin Ambush",
  "description": "More goblins join the fray",
  "tier": 2,
  "campaignId": 1,
  "isPublic": true,
  "adversaryIds": [5, 5, 7, 7, 8]
}
```

**Note on adversaryIds:** If provided, completely replaces the existing adversary list.

**Field Validation:**

| Field        | Type          | Constraints              |
|-------------|---------------|--------------------------|
| name        | String        | Max 200 characters       |
| description | String        | -                        |
| tier        | Integer       | 1-4                      |
| campaignId  | Long          | Valid campaign ID or null |
| isPublic    | Boolean       | -                        |
| adversaryIds| List\<Long\>  | Replaces all if provided |

**Response:** `200 OK` -- Updated `EncounterResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter with given ID does not exist

---

### DELETE /api/dh/encounters/{id}

Soft deletes an encounter (sets `deletedAt` timestamp).

**Authorization:**
- Official encounters: OWNER role only
- Non-official encounters: Creator OR MODERATOR+ role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter with given ID does not exist

---

### POST /api/dh/encounters/{id}/restore

Restores a soft-deleted encounter.

**Authorization:** ADMIN or OWNER role required (`@PreAuthorize`)

**Response:** `200 OK` -- Restored `EncounterResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Encounter with given ID does not exist

---

### POST /api/dh/encounters/{id}/adversaries

Adds a single adversary instance to an encounter.

**Authorization:** Same as PUT (creator or MODERATOR+ for non-official; OWNER for official)

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Query Parameters:**

| Parameter   | Type | Required | Description            |
|-------------|------|----------|------------------------|
| adversaryId | Long | Yes      | The adversary ID to add|

**Response:** `200 OK` -- Updated `EncounterResponse` with the new adversary included

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter or adversary does not exist

---

### DELETE /api/dh/encounters/{id}/adversaries/{encounterAdversaryId}

Removes a specific adversary instance from an encounter.

**Authorization:** Same as PUT (creator or MODERATOR+ for non-official; OWNER for official)

**Path Parameters:**

| Parameter            | Type | Description                              |
|----------------------|------|------------------------------------------|
| id                   | Long | The encounter ID                         |
| encounterAdversaryId | Long | The encounter adversary instance ID      |

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter or encounter adversary does not exist

---

## Response DTOs

### EncounterResponse

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party",
  "tier": 1,
  "isOfficial": false,
  "isPublic": true,
  "campaignId": null,
  "campaign": null,
  "originalEncounterId": null,
  "originalEncounter": null,
  "creatorId": 1,
  "creator": null,
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 5,
      "adversary": null
    }
  ],
  "totalBattlePoints": 4,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00",
  "deletedAt": null
}
```

`null` fields are omitted from JSON output (`@JsonInclude(NON_NULL)`).

### EncounterAdversaryResponse (nested)

| Field       | Type             | Description                                           |
|-------------|------------------|-------------------------------------------------------|
| id          | Long             | Encounter adversary instance ID                       |
| adversaryId | Long             | The adversary ID (always included)                    |
| adversary   | AdversaryResponse| Full adversary object (only with `?expand=adversaryDetails`) |

---

## Database Schema

**Table:** `encounters`

| Column                | Type         | Nullable | Notes                                 |
|-----------------------|--------------|----------|---------------------------------------|
| id                    | BIGSERIAL    | No       | Primary key                           |
| name                  | VARCHAR(200) | No       |                                       |
| description           | TEXT         | Yes      |                                       |
| tier                  | INTEGER      | Yes      | 1-4, null for multi-tier              |
| is_official           | BOOLEAN      | No       | Default false                         |
| is_public             | BOOLEAN      | No       | Default false                         |
| original_encounter_id | BIGINT       | Yes      | FK to encounters (SET NULL on delete) |
| creator_id            | BIGINT       | No       | FK to users (CASCADE)                 |
| campaign_id           | BIGINT       | Yes      | FK to campaigns (SET NULL on delete)  |
| deleted_at            | TIMESTAMP    | Yes      | Null = active                         |
| created_at            | TIMESTAMP    | No       | Auto-set                              |
| last_modified_at      | TIMESTAMP    | No       | Auto-set                              |

**Check Constraint:** `check_encounter_tier_valid` -- tier IS NULL OR (tier >= 1 AND tier <= 4)

**Table:** `encounter_adversaries`

| Column       | Type      | Nullable | Notes                             |
|--------------|-----------|----------|-----------------------------------|
| id           | BIGSERIAL | No       | Primary key                       |
| encounter_id | BIGINT    | No       | FK to encounters (CASCADE)        |
| adversary_id | BIGINT    | No       | FK to adversaries (CASCADE)       |
| created_at   | TIMESTAMP | No       | Auto-set                          |
| last_modified_at | TIMESTAMP | No   | Auto-set                          |

**Note:** The original unique constraint on `(encounter_id, adversary_id)` was removed in migration `V20260130225724303` to allow multiple instances of the same adversary in one encounter.

---

## Battle Points

The `totalBattlePoints` field is calculated server-side by summing the battle points of each adversary's `adversaryType`. This is used for encounter balancing and is read-only in the response.

---

## Test Examples

**Note:** No integration tests or service unit tests exist for this controller yet. The following examples are derived from the controller and DTO definitions.

### Create Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{
    "name": "Goblin Ambush",
    "description": "A group of goblins attacks the party on the forest road",
    "tier": 1,
    "isPublic": false,
    "adversaryIds": [5, 5, 7]
  }'
```

### Copy Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters/1/copy \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Get Encounters with Filters
```bash
curl "http://localhost:8080/api/dh/encounters?tier=1&isOfficial=true&name=goblin" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Get Encounter with All Expansions
```bash
curl "http://localhost:8080/api/dh/encounters/1?expand=creator,campaign,adversaryDetails" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Update Encounter
```bash
curl -X PUT http://localhost:8080/api/dh/encounters/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<creator_jwt>" \
  -d '{
    "name": "Updated Goblin Ambush",
    "tier": 2,
    "isPublic": true,
    "adversaryIds": [5, 5, 7, 7, 8]
  }'
```

### Add Adversary to Encounter
```bash
curl -X POST "http://localhost:8080/api/dh/encounters/1/adversaries?adversaryId=9" \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Remove Adversary Instance from Encounter
```bash
curl -X DELETE http://localhost:8080/api/dh/encounters/1/adversaries/3 \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Delete Encounter (soft delete)
```bash
curl -X DELETE http://localhost:8080/api/dh/encounters/1 \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Restore Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters/1/restore \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```
