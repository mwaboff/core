# Campaigns API Reference

Base URL: `http://localhost:8080`

All endpoints require authentication via `AUTH_TOKEN` HttpOnly cookie unless otherwise noted. Authentication is JWT-based with 30-day expiration.

---

## Table of Contents

1. [List All Campaigns](#1-list-all-campaigns)
2. [Get Campaign by ID](#2-get-campaign-by-id)
3. [Create Campaign](#3-create-campaign)
4. [Update Campaign](#4-update-campaign)
5. [Delete Campaign](#5-delete-campaign)
6. [Add Game Master](#6-add-game-master)
7. [Remove Game Master](#7-remove-game-master)
8. [Add Player](#8-add-player)
9. [Remove Player](#9-remove-player)
10. [Submit Character Sheet](#10-submit-character-sheet)
11. [Approve Character Sheet](#11-approve-character-sheet)
12. [Reject Character Sheet](#12-reject-character-sheet)
13. [Add NPC](#13-add-npc)
14. [Remove Character Sheet](#14-remove-character-sheet)
15. [Models](#models)
16. [Enums](#enums)

---

## 1. List All Campaigns

Retrieves a paginated list of campaigns with optional filtering.

- **Method:** `GET`
- **Path:** `/api/dh/campaigns`
- **Auth:** MODERATOR, ADMIN, or OWNER role required (`@PreAuthorize`)

### Query Parameters

| Parameter  | Type    | Required | Default | Description                                      |
|------------|---------|----------|---------|--------------------------------------------------|
| page       | integer | No       | 0       | Zero-based page number                           |
| size       | integer | No       | 20      | Items per page (max 100)                         |
| creatorId  | long    | No       | -       | Filter by campaign creator user ID               |
| name       | string  | No       | -       | Filter by name (case-insensitive partial match)  |
| expand     | string  | No       | -       | Comma-separated list of relationships to expand  |

### Response: `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Test Campaign",
      "description": "A test campaign",
      "creatorId": 1,
      "gameMasterIds": [1],
      "playerIds": [],
      "pendingCharacterSheetIds": [],
      "playerCharacterIds": [],
      "nonPlayerCharacterIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00",
      "deletedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

### Error Responses

| Status | Condition                       | Error Body Type |
|--------|---------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie            | ErrorResponse   |
| 403    | User role is USER (not privileged) | ErrorResponse |

### curl Examples

```bash
# List all campaigns (as moderator)
curl -s http://localhost:8080/api/dh/campaigns \
  --cookie "AUTH_TOKEN=<token>"

# Filter by creator ID
curl -s "http://localhost:8080/api/dh/campaigns?creatorId=1" \
  --cookie "AUTH_TOKEN=<token>"

# Filter by name
curl -s "http://localhost:8080/api/dh/campaigns?name=Dragon" \
  --cookie "AUTH_TOKEN=<token>"

# Paginated with expansion
curl -s "http://localhost:8080/api/dh/campaigns?page=0&size=10&expand=creator" \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 2. Get Campaign by ID

Retrieves a single campaign. Only campaign participants (creator, GM, or player) or users with MODERATOR/ADMIN/OWNER role can view.

- **Method:** `GET`
- **Path:** `/api/dh/campaigns/{id}`
- **Auth:** Authenticated; must be a campaign participant or MODERATOR+

### Path Parameters

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| id        | long | Yes      | The campaign ID  |

### Query Parameters

| Parameter | Type   | Required | Description                                     |
|-----------|--------|----------|-------------------------------------------------|
| expand    | string | No       | Comma-separated list of relationships to expand |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [3, 4],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [10],
  "nonPlayerCharacterIds": [11],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00",
  "deletedAt": null
}
```

### Response with `?expand=creator`: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "creator": {
    "id": 1,
    "username": "creator",
    "email": "creator@example.com",
    "avatarUrl": null,
    "timezone": null,
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  },
  "gameMasterIds": [1],
  "playerIds": [],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                  | Error Body Type |
|--------|--------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                       | ErrorResponse   |
| 403    | User is not a participant and not MODERATOR+ | ErrorResponse |
| 404    | Campaign not found or soft-deleted         | ErrorResponse   |

### curl Examples

```bash
# Get campaign by ID
curl -s http://localhost:8080/api/dh/campaigns/1 \
  --cookie "AUTH_TOKEN=<token>"

# Get with creator expansion
curl -s "http://localhost:8080/api/dh/campaigns/1?expand=creator" \
  --cookie "AUTH_TOKEN=<token>"

# Get with all expansions
curl -s "http://localhost:8080/api/dh/campaigns/1?expand=all" \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 3. Create Campaign

Creates a new campaign. The authenticated user becomes the creator and is automatically added as a game master.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns`
- **Auth:** Any authenticated user

### Request Body

| Field          | Type       | Required | Constraints          | Description                                    |
|----------------|------------|----------|----------------------|------------------------------------------------|
| name           | string     | Yes      | Not blank, max 200   | Campaign name                                  |
| description    | string     | No       | Max 2000             | Campaign description                           |
| gameMasterIds  | long[]     | No       | -                    | User IDs to add as additional game masters     |
| playerIds      | long[]     | No       | -                    | User IDs to add as players                     |

### Request Example

```json
{
  "name": "New Campaign",
  "description": "A new adventure",
  "gameMasterIds": [2],
  "playerIds": [3, 4]
}
```

### Response: `201 Created`

```json
{
  "id": 1,
  "name": "New Campaign",
  "description": "A new adventure",
  "creatorId": 1,
  "gameMasterIds": [1, 2],
  "playerIds": [3, 4],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00",
  "deletedAt": null
}
```

### Minimal Request Example

```json
{
  "name": "New Campaign"
}
```

### Error Responses

| Status | Condition                          | Error Body Type          |
|--------|------------------------------------|--------------------------|
| 400    | Missing or blank name              | ValidationErrorResponse  |
| 400    | Name exceeds 200 characters        | ValidationErrorResponse  |
| 401    | No AUTH_TOKEN cookie               | ErrorResponse            |
| 404    | A referenced user ID not found     | ErrorResponse            |

### Validation Error Example (`400`)

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "name": "Campaign name is required"
  },
  "path": "/api/dh/campaigns",
  "timestamp": "2026-03-13T10:00:00"
}
```

### curl Examples

```bash
# Create a minimal campaign
curl -s -X POST http://localhost:8080/api/dh/campaigns \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<token>" \
  -d '{"name": "New Campaign", "description": "A new adventure"}'

# Create with GMs and players
curl -s -X POST http://localhost:8080/api/dh/campaigns \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<token>" \
  -d '{"name": "Full Campaign", "gameMasterIds": [2], "playerIds": [3, 4]}'
```

---

## 4. Update Campaign

Updates an existing campaign. Supports partial updates -- only non-null fields are changed.

- **Method:** `PUT`
- **Path:** `/api/dh/campaigns/{id}`
- **Auth:** Campaign creator or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| id        | long | Yes      | The campaign ID  |

### Request Body

| Field       | Type   | Required | Constraints  | Description               |
|-------------|--------|----------|--------------|---------------------------|
| name        | string | No       | Max 200      | New campaign name          |
| description | string | No       | Max 2000     | New campaign description   |

### Request Example

```json
{
  "name": "Updated Campaign Name",
  "description": "Updated description"
}
```

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Updated Campaign Name",
  "description": "Updated description",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                     | Error Body Type |
|--------|-----------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                          | ErrorResponse   |
| 403    | User is not the creator and not MODERATOR+    | ErrorResponse   |
| 404    | Campaign not found or soft-deleted            | ErrorResponse   |

### curl Example

```bash
curl -s -X PUT http://localhost:8080/api/dh/campaigns/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<token>" \
  -d '{"name": "Updated Campaign Name"}'
```

---

## 5. Delete Campaign

Soft-deletes a campaign. Sets `deletedAt` timestamp; the record remains in the database but is excluded from active queries.

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}`
- **Auth:** Campaign creator or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| id        | long | Yes      | The campaign ID  |

### Response: `204 No Content`

No response body.

### Error Responses

| Status | Condition                                     | Error Body Type |
|--------|-----------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                          | ErrorResponse   |
| 403    | User is not the creator and not MODERATOR+    | ErrorResponse   |
| 404    | Campaign not found or already soft-deleted    | ErrorResponse   |

### curl Example

```bash
curl -s -X DELETE http://localhost:8080/api/dh/campaigns/1 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 6. Add Game Master

Adds a user as a game master to the campaign.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/game-masters/{userId}`
- **Auth:** Campaign creator or MODERATOR/ADMIN/OWNER (GMs who are not the creator cannot add other GMs)

### Path Parameters

| Parameter | Type | Required | Description                       |
|-----------|------|----------|-----------------------------------|
| id        | long | Yes      | The campaign ID                   |
| userId    | long | Yes      | The user ID to add as game master |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1, 2],
  "playerIds": [],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                     | Error Body Type |
|--------|-----------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                          | ErrorResponse   |
| 403    | User is not the creator and not MODERATOR+    | ErrorResponse   |
| 404    | Campaign or target user not found             | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/game-masters/2 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 7. Remove Game Master

Removes a user from the game masters of the campaign. The campaign creator cannot be removed.

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}/game-masters/{userId}`
- **Auth:** Campaign creator or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                            |
|-----------|------|----------|----------------------------------------|
| id        | long | Yes      | The campaign ID                        |
| userId    | long | Yes      | The user ID to remove from game masters |

### Response: `200 OK`

Returns the updated CampaignResponse (same shape as [Add Game Master](#6-add-game-master)).

### Error Responses

| Status | Condition                                       | Error Body Type |
|--------|-------------------------------------------------|-----------------|
| 400    | Attempting to remove the campaign creator        | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                             | ErrorResponse   |
| 403    | User is not the creator and not MODERATOR+       | ErrorResponse   |
| 404    | Campaign not found                               | ErrorResponse   |

### Error Example: Removing Creator (`400`)

```json
{
  "status": 400,
  "error": "Invalid Operation",
  "message": "Cannot remove the campaign creator from game masters",
  "path": "/api/dh/campaigns/1/game-masters/1",
  "timestamp": "2026-03-13T10:00:00"
}
```

### curl Example

```bash
curl -s -X DELETE http://localhost:8080/api/dh/campaigns/1/game-masters/2 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 8. Add Player

Adds a user as a player to the campaign.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/players/{userId}`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                    |
|-----------|------|----------|--------------------------------|
| id        | long | Yes      | The campaign ID                |
| userId    | long | Yes      | The user ID to add as player   |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [3],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                        | Error Body Type |
|--------|--------------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                             | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+      | ErrorResponse   |
| 404    | Campaign or target user not found                | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/players/3 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 9. Remove Player

Removes a user from the players of the campaign.

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}/players/{userId}`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                         |
|-----------|------|----------|-------------------------------------|
| id        | long | Yes      | The campaign ID                     |
| userId    | long | Yes      | The user ID to remove from players  |

### Response: `200 OK`

Returns the updated CampaignResponse (same shape as [Add Player](#8-add-player)).

### Error Responses

| Status | Condition                                        | Error Body Type |
|--------|--------------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                             | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+      | ErrorResponse   |
| 404    | Campaign not found                               | ErrorResponse   |

### curl Example

```bash
curl -s -X DELETE http://localhost:8080/api/dh/campaigns/1/players/3 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 10. Submit Character Sheet

Submits a character sheet to the campaign for GM approval. The sheet is added to `pendingCharacterSheets`.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/character-sheets/{sheetId}/submit`
- **Auth:** Must be the character sheet owner AND a player in the campaign

### Path Parameters

| Parameter | Type | Required | Description                      |
|-----------|------|----------|----------------------------------|
| id        | long | Yes      | The campaign ID                  |
| sheetId   | long | Yes      | The character sheet ID to submit |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [3],
  "pendingCharacterSheetIds": [10],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                          | Error Body Type |
|--------|----------------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                               | ErrorResponse   |
| 403    | User is not the character sheet owner              | ErrorResponse   |
| 403    | User is not a player in the campaign               | ErrorResponse   |
| 404    | Campaign or character sheet not found              | ErrorResponse   |

### Error Example: Not Owner (`403`)

```json
{
  "status": 403,
  "error": "Insufficient Permissions",
  "message": "You must be the owner of the character sheet to submit it",
  "path": "/api/dh/campaigns/1/character-sheets/10/submit",
  "timestamp": "2026-03-13T10:00:00"
}
```

### Error Example: Not a Player (`403`)

```json
{
  "status": 403,
  "error": "Insufficient Permissions",
  "message": "You must be a player in this campaign to submit a character sheet",
  "path": "/api/dh/campaigns/1/character-sheets/10/submit",
  "timestamp": "2026-03-13T10:00:00"
}
```

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/character-sheets/10/submit \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 11. Approve Character Sheet

Approves a pending character sheet, moving it from `pendingCharacterSheets` to `playerCharacters`.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/character-sheets/{sheetId}/approve`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                        |
|-----------|------|----------|------------------------------------|
| id        | long | Yes      | The campaign ID                    |
| sheetId   | long | Yes      | The character sheet ID to approve  |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [3],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [10],
  "nonPlayerCharacterIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                          | Error Body Type |
|--------|----------------------------------------------------|-----------------|
| 400    | Character sheet is not in pending list             | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                               | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+        | ErrorResponse   |
| 404    | Campaign not found                                 | ErrorResponse   |

### Error Example: Not in Pending (`400`)

```json
{
  "status": 400,
  "error": "Invalid Operation",
  "message": "CharacterSheet with id 999 is not in pending list",
  "path": "/api/dh/campaigns/1/character-sheets/999/approve",
  "timestamp": "2026-03-13T10:00:00"
}
```

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/character-sheets/10/approve \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 12. Reject Character Sheet

Rejects a pending character sheet, removing it from `pendingCharacterSheets`.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/character-sheets/{sheetId}/reject`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                       |
|-----------|------|----------|-----------------------------------|
| id        | long | Yes      | The campaign ID                   |
| sheetId   | long | Yes      | The character sheet ID to reject  |

### Response: `200 OK`

Returns the updated CampaignResponse with the sheet removed from `pendingCharacterSheetIds`.

### Error Responses

| Status | Condition                                          | Error Body Type |
|--------|----------------------------------------------------|-----------------|
| 400    | Character sheet is not in pending list             | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                               | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+        | ErrorResponse   |
| 404    | Campaign not found                                 | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/character-sheets/10/reject \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 13. Add NPC

Adds a character sheet as a non-player character to the campaign.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/npcs/{sheetId}`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                        |
|-----------|------|----------|------------------------------------|
| id        | long | Yes      | The campaign ID                    |
| sheetId   | long | Yes      | The character sheet ID to add as NPC |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Test Campaign",
  "description": "A test campaign",
  "creatorId": 1,
  "gameMasterIds": [1],
  "playerIds": [],
  "pendingCharacterSheetIds": [],
  "playerCharacterIds": [],
  "nonPlayerCharacterIds": [11],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition                                        | Error Body Type |
|--------|--------------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                             | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+      | ErrorResponse   |
| 404    | Campaign or character sheet not found            | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/npcs/11 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 14. Remove Character Sheet

Removes a character sheet from the campaign entirely -- from all collections (pending, player characters, and NPCs).

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}/character-sheets/{sheetId}`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description                          |
|-----------|------|----------|--------------------------------------|
| id        | long | Yes      | The campaign ID                      |
| sheetId   | long | Yes      | The character sheet ID to remove     |

### Response: `200 OK`

Returns the updated CampaignResponse with the sheet removed from all collections.

### Error Responses

| Status | Condition                                        | Error Body Type |
|--------|--------------------------------------------------|-----------------|
| 401    | No AUTH_TOKEN cookie                             | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+      | ErrorResponse   |
| 404    | Campaign not found                               | ErrorResponse   |

### curl Example

```bash
curl -s -X DELETE http://localhost:8080/api/dh/campaigns/1/character-sheets/10 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## Models

### CampaignResponse

Null fields are omitted from the JSON response (`@JsonInclude(NON_NULL)`).

| Field                     | Type                      | Always Present | Description                                          |
|---------------------------|---------------------------|----------------|------------------------------------------------------|
| id                        | long                      | Yes            | Campaign ID                                          |
| name                      | string                    | Yes            | Campaign name (max 200 chars)                        |
| description               | string                    | No             | Campaign description (max 2000 chars)                |
| creatorId                 | long                      | Yes            | ID of the campaign creator                           |
| creator                   | UserResponse              | No             | Expanded when `?expand=creator`                      |
| gameMasterIds             | long[]                    | Yes            | IDs of all game masters                              |
| gameMasters               | UserResponse[]            | No             | Expanded when `?expand=gameMasters`                  |
| playerIds                 | long[]                    | Yes            | IDs of all players                                   |
| players                   | UserResponse[]            | No             | Expanded when `?expand=players`                      |
| pendingCharacterSheetIds  | long[]                    | Yes            | IDs of pending character sheets                      |
| pendingCharacterSheets    | CharacterSheetResponse[]  | No             | Expanded when `?expand=pendingCharacterSheets`       |
| playerCharacterIds        | long[]                    | Yes            | IDs of approved player characters                    |
| playerCharacters          | CharacterSheetResponse[]  | No             | Expanded when `?expand=playerCharacters`             |
| nonPlayerCharacterIds     | long[]                    | Yes            | IDs of NPCs                                          |
| nonPlayerCharacters       | CharacterSheetResponse[]  | No             | Expanded when `?expand=nonPlayerCharacters`          |
| createdAt                 | datetime                  | Yes            | ISO 8601 creation timestamp                          |
| lastModifiedAt            | datetime                  | Yes            | ISO 8601 last modified timestamp                     |
| deletedAt                 | datetime                  | No             | ISO 8601 soft-deletion timestamp (null if active)    |

### CreateCampaignRequest

| Field         | Type   | Required | Constraints                           |
|---------------|--------|----------|---------------------------------------|
| name          | string | Yes      | `@NotBlank`, max 200 characters       |
| description   | string | No       | Max 2000 characters                   |
| gameMasterIds | long[] | No       | User IDs for additional game masters  |
| playerIds     | long[] | No       | User IDs for players                  |

### UpdateCampaignRequest

Supports partial updates. Only non-null fields are applied. Blank name values are ignored.

| Field       | Type   | Required | Constraints             |
|-------------|--------|----------|-------------------------|
| name        | string | No       | Max 200 characters      |
| description | string | No       | Max 2000 characters     |

### UserResponse (expanded)

When a user is expanded in a campaign response, these fields are included. Null fields are omitted.

| Field           | Type     | Description                  |
|-----------------|----------|------------------------------|
| id              | long     | User ID                      |
| username        | string   | Username                     |
| email           | string   | Email address                |
| avatarUrl       | string   | Avatar image URL             |
| timezone        | string   | User timezone                |
| createdAt       | datetime | Account creation timestamp   |
| lastModifiedAt  | datetime | Last modified timestamp      |

### CharacterSheetResponse (expanded, basic fields)

When a character sheet is expanded in a campaign response, only basic fields are included (not the full character sheet).

| Field          | Type     | Description                    |
|----------------|----------|--------------------------------|
| id             | long     | Character sheet ID             |
| name           | string   | Character name                 |
| pronouns       | string   | Character pronouns             |
| level          | integer  | Character level (1-10)         |
| ownerId        | long     | ID of the character sheet owner|
| createdAt      | datetime | Creation timestamp             |
| lastModifiedAt | datetime | Last modified timestamp        |

### PagedResponse

Wrapper for all paginated list responses.

| Field         | Type   | Description                           |
|---------------|--------|---------------------------------------|
| content       | T[]    | List of items for the current page    |
| totalElements | long   | Total items across all pages          |
| totalPages    | integer| Total number of pages                 |
| currentPage   | integer| Current page number (zero-based)      |
| pageSize      | integer| Number of items per page              |

### ErrorResponse

| Field     | Type     | Description                    |
|-----------|----------|--------------------------------|
| status    | integer  | HTTP status code               |
| error     | string   | Error category                 |
| message   | string   | Human-readable error message   |
| path      | string   | Request path that caused error |
| timestamp | datetime | When the error occurred        |

### ValidationErrorResponse

| Field       | Type              | Description                              |
|-------------|-------------------|------------------------------------------|
| status      | integer           | HTTP status code (always 400)            |
| error       | string            | Always "Validation Failed"               |
| fieldErrors | map<string,string> | Field name to error message mapping     |
| path        | string            | Request path that caused error           |
| timestamp   | datetime          | When the error occurred                  |

---

## Enums

### Role

User roles in the system, ordered from most to least privileged:

| Value     | Description                                             |
|-----------|---------------------------------------------------------|
| OWNER     | System owner; full access to all resources              |
| ADMIN     | Administrator; can manage users and content             |
| MODERATOR | Moderator; can bypass campaign ownership checks         |
| USER      | Default role; standard access                           |

---

## Expand Support

The `?expand` query parameter is supported on `GET /api/dh/campaigns` and `GET /api/dh/campaigns/{id}`. Mutation endpoints return the response without expansions.

### Available Expand Values

| Value                   | Expands Into                                       |
|-------------------------|----------------------------------------------------|
| creator                 | Full UserResponse for the campaign creator          |
| gameMasters             | List of UserResponse for all game masters           |
| players                 | List of UserResponse for all players                |
| pendingCharacterSheets  | List of CharacterSheetResponse (basic) for pending  |
| playerCharacters        | List of CharacterSheetResponse (basic) for PCs      |
| nonPlayerCharacters     | List of CharacterSheetResponse (basic) for NPCs     |
| all                     | Expands all of the above simultaneously             |

Multiple values can be combined with commas: `?expand=creator,gameMasters,players`

---

## Soft Deletion

Campaigns use soft deletion. When a campaign is deleted via `DELETE /api/dh/campaigns/{id}`:

- The `deletedAt` field is set to the current timestamp
- The campaign record remains in the database
- All active queries (`findActiveById`, `findActiveWithFilters`) exclude soft-deleted records
- The campaign's relationships (GMs, players, character sheets) are preserved
- A `restore()` method exists on the entity but is not exposed via API

---

## Access Control Summary

| Operation                  | Creator | GM (non-creator) | Player | MODERATOR+ |
|----------------------------|---------|-------------------|--------|------------|
| List all campaigns         | No      | No                | No     | Yes        |
| View campaign              | Yes     | Yes               | Yes    | Yes        |
| Create campaign            | N/A     | N/A               | N/A    | Any auth   |
| Update campaign            | Yes     | No                | No     | Yes        |
| Delete campaign            | Yes     | No                | No     | Yes        |
| Add game master            | Yes     | No                | No     | Yes        |
| Remove game master         | Yes     | No                | No     | Yes        |
| Add player                 | Yes     | Yes               | No     | Yes        |
| Remove player              | Yes     | Yes               | No     | Yes        |
| Submit character sheet     | No      | No                | Yes*   | No         |
| Approve character sheet    | Yes     | Yes               | No     | Yes        |
| Reject character sheet     | Yes     | Yes               | No     | Yes        |
| Add NPC                    | Yes     | Yes               | No     | Yes        |
| Remove character sheet     | Yes     | Yes               | No     | Yes        |

\* Must also be the character sheet owner.

---

## Database Schema

### campaigns

| Column           | Type           | Nullable | Constraints                        |
|------------------|----------------|----------|------------------------------------|
| id               | BIGSERIAL      | No       | PRIMARY KEY                        |
| name             | VARCHAR(200)   | No       | CHECK LENGTH(TRIM(name)) > 0      |
| description      | VARCHAR(2000)  | Yes      |                                    |
| creator_id       | BIGINT         | No       | FK -> users(id) ON DELETE CASCADE  |
| deleted_at       | TIMESTAMP      | Yes      |                                    |
| created_at       | TIMESTAMP      | No       | DEFAULT CURRENT_TIMESTAMP          |
| last_modified_at | TIMESTAMP      | No       | DEFAULT CURRENT_TIMESTAMP          |

Indexes: `idx_campaigns_creator_id`, `idx_campaigns_deleted_at`, `idx_campaigns_creator_not_deleted` (partial, WHERE deleted_at IS NULL), `idx_campaigns_name`

### campaign_game_masters

| Column      | Type   | Constraints                             |
|-------------|--------|-----------------------------------------|
| campaign_id | BIGINT | PK, FK -> campaigns(id) ON DELETE CASCADE |
| user_id     | BIGINT | PK, FK -> users(id) ON DELETE CASCADE     |

### campaign_players

| Column      | Type   | Constraints                             |
|-------------|--------|-----------------------------------------|
| campaign_id | BIGINT | PK, FK -> campaigns(id) ON DELETE CASCADE |
| user_id     | BIGINT | PK, FK -> users(id) ON DELETE CASCADE     |

### campaign_pending_character_sheets

| Column             | Type   | Constraints                                    |
|--------------------|--------|------------------------------------------------|
| campaign_id        | BIGINT | PK, FK -> campaigns(id) ON DELETE CASCADE        |
| character_sheet_id | BIGINT | PK, FK -> character_sheets(id) ON DELETE CASCADE |

### campaign_player_characters

| Column             | Type   | Constraints                                    |
|--------------------|--------|------------------------------------------------|
| campaign_id        | BIGINT | PK, FK -> campaigns(id) ON DELETE CASCADE        |
| character_sheet_id | BIGINT | PK, FK -> character_sheets(id) ON DELETE CASCADE |

### campaign_non_player_characters

| Column             | Type   | Constraints                                    |
|--------------------|--------|------------------------------------------------|
| campaign_id        | BIGINT | PK, FK -> campaigns(id) ON DELETE CASCADE        |
| character_sheet_id | BIGINT | PK, FK -> character_sheets(id) ON DELETE CASCADE |
