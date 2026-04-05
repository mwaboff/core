# Campaigns API Reference

Base URL: `http://localhost:8080`

All endpoints require authentication via `AUTH_TOKEN` HttpOnly cookie unless otherwise noted. Authentication is JWT-based with 30-day expiration.

---

## Table of Contents

1. [My Campaigns](#1-my-campaigns)
2. [Join Campaign via Invite](#2-join-campaign-via-invite)
3. [List All Campaigns](#3-list-all-campaigns)
4. [Get Campaign by ID](#4-get-campaign-by-id)
5. [Create Campaign](#5-create-campaign)
6. [Update Campaign](#6-update-campaign)
7. [Delete Campaign](#7-delete-campaign)
8. [Generate Invite](#8-generate-invite)
9. [End Campaign](#9-end-campaign)
10. [Leave Campaign](#10-leave-campaign)
11. [Add Game Master](#11-add-game-master)
12. [Remove Game Master](#12-remove-game-master)
13. [Add Player](#13-add-player)
14. [Kick Player](#14-kick-player)
15. [Submit Character Sheet](#15-submit-character-sheet)
16. [Approve Character Sheet](#16-approve-character-sheet)
17. [Reject Character Sheet](#17-reject-character-sheet)
18. [Add NPC](#18-add-npc)
19. [Remove Character Sheet](#19-remove-character-sheet)
20. [Models](#models)
21. [Enums](#enums)

---

## 1. My Campaigns

Retrieves a paginated list of campaigns where the authenticated user is involved (as creator, GM, or player).

- **Method:** `GET`
- **Path:** `/api/dh/campaigns/mine`
- **Auth:** Any authenticated user

### Query Parameters

| Parameter | Type    | Required | Default | Description                                     |
|-----------|---------|----------|---------|-------------------------------------------------|
| page      | integer | No       | 0       | Zero-based page number                          |
| size      | integer | No       | 20      | Items per page (max 100)                        |
| expand    | string  | No       | -       | Comma-separated list of relationships to expand |

### Response: `200 OK`

Returns a PagedResponse of CampaignResponse (same shape as List All Campaigns).

### Error Responses

| Status | Condition            | Error Body Type |
|--------|----------------------|-----------------|
| 401    | No AUTH_TOKEN cookie | ErrorResponse   |

### curl Example

```bash
curl -s "http://localhost:8080/api/dh/campaigns/mine?expand=creator" \
  --cookie "AUTH_TOKEN=<token>"
```

> **See also:** `GET /api/users/{userId}/campaigns` in `references/users-api.md` — retrieves campaigns for a specific user (self or MODERATOR+), useful for admin views or viewing another user's campaign involvement.

---

## 2. Join Campaign via Invite

Joins a campaign using an invite token. The user is added as a player.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/join/{token}`
- **Auth:** Any authenticated user

### Path Parameters

| Parameter | Type   | Required | Description      |
|-----------|--------|----------|------------------|
| token     | string | Yes      | The invite token |

### Response: `200 OK`

```json
{
  "campaignId": 1,
  "campaignName": "Test Campaign",
  "message": "Successfully joined campaign: Test Campaign"
}
```

### Error Responses

| Status | Condition                                      | Error Body Type |
|--------|------------------------------------------------|-----------------|
| 400    | Token expired, already used, or user is already a member | ErrorResponse |
| 400    | Campaign is ended                              | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                           | ErrorResponse   |
| 404    | Token not found or campaign deleted            | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/join/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 3. List All Campaigns

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
      "isEnded": false,
      "endedAt": null,
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

## 4. Get Campaign by ID

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
    "role": "USER",
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

## 5. Create Campaign

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

## 6. Update Campaign

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

## 7. Delete Campaign

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

## 8. Generate Invite

Generates a single-use invite link for the campaign. The invite is valid for 24 hours.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/invites`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| id        | long | Yes      | The campaign ID |

### Response: `201 Created`

```json
{
  "id": 1,
  "campaignId": 1,
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "expiresAt": "2026-03-14T10:00:00",
  "createdAt": "2026-03-13T10:00:00"
}
```

### Error Responses

| Status | Condition                                   | Error Body Type |
|--------|---------------------------------------------|-----------------|
| 400    | Campaign is ended                           | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                        | ErrorResponse   |
| 403    | User is not a creator/GM and not MODERATOR+ | ErrorResponse   |
| 404    | Campaign not found                          | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/invites \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 9. End Campaign

Ends a campaign, locking it from further modifications. The campaign remains visible but most mutating operations are blocked. Distinct from deletion: ended campaigns are locked+visible, deleted campaigns are invisible.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/end`
- **Auth:** Campaign creator or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| id        | long | Yes      | The campaign ID |

### Response: `200 OK`

Returns the updated CampaignResponse with `isEnded: true` and `endedAt` set.

### Error Responses

| Status | Condition                                       | Error Body Type |
|--------|-------------------------------------------------|-----------------|
| 400    | Campaign is already ended                       | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie                            | ErrorResponse   |
| 403    | User is not the creator and not MODERATOR+      | ErrorResponse   |
| 404    | Campaign not found                              | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/end \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 10. Leave Campaign

Allows a player to voluntarily leave a campaign. Does NOT cascade-unlink character sheets (voluntary departure). Works on ended campaigns.

- **Method:** `POST`
- **Path:** `/api/dh/campaigns/{id}/leave`
- **Auth:** Must be a player in the campaign

### Path Parameters

| Parameter | Type | Required | Description     |
|-----------|------|----------|-----------------|
| id        | long | Yes      | The campaign ID |

### Response: `200 OK`

Returns the updated CampaignResponse with the player removed.

### Error Responses

| Status | Condition                | Error Body Type |
|--------|--------------------------|-----------------|
| 400    | User is not a player     | ErrorResponse   |
| 401    | No AUTH_TOKEN cookie     | ErrorResponse   |
| 404    | Campaign not found       | ErrorResponse   |

### curl Example

```bash
curl -s -X POST http://localhost:8080/api/dh/campaigns/1/leave \
  --cookie "AUTH_TOKEN=<token>"
```

---

## 11. Add Game Master

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

## 12. Remove Game Master

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

## 13. Add Player

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

## 14. Kick Player

Kicks a player from the campaign. Cascades to remove all character sheets owned by the kicked player from all three collections (pending, player characters, and NPCs).

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}/players/{userId}`
- **Auth:** Campaign creator/GM or MODERATOR/ADMIN/OWNER

### Path Parameters

| Parameter | Type | Required | Description               |
|-----------|------|----------|---------------------------|
| id        | long | Yes      | The campaign ID           |
| userId    | long | Yes      | The user ID to kick       |

### Response: `200 OK`

Returns the updated CampaignResponse with the player removed and their character sheets unlinked.

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

## 15. Submit Character Sheet

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

## 16. Approve Character Sheet

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

## 17. Reject Character Sheet

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

## 18. Add NPC

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

## 19. Remove Character Sheet

Removes a character sheet from the campaign entirely -- from all collections (pending, player characters, and NPCs). Works on ended campaigns (unlinking is always allowed).

- **Method:** `DELETE`
- **Path:** `/api/dh/campaigns/{id}/character-sheets/{sheetId}`
- **Auth:** Campaign creator/GM, character sheet owner, or MODERATOR/ADMIN/OWNER

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
| 401    | No AUTH_TOKEN cookie                                          | ErrorResponse   |
| 403    | User is not a creator/GM, not the sheet owner, and not MODERATOR+ | ErrorResponse |
| 404    | Campaign not found                                            | ErrorResponse   |

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
| nonPlayerCharacterIds     | long[]                             | Yes            | IDs of NPCs                                          |
| nonPlayerCharacters       | CharacterSheetResponse[]           | No             | Expanded when `?expand=nonPlayerCharacters`          |
| characterSummaries        | CampaignCharacterSummaryResponse[] | No             | Expanded when `?expand=characterSummaries`           |
| isEnded                   | boolean                            | Yes            | Whether the campaign has ended                       |
| endedAt                   | datetime                           | No             | ISO 8601 ended timestamp (null if not ended)         |
| createdAt                 | datetime                           | Yes            | ISO 8601 creation timestamp                          |
| lastModifiedAt            | datetime                           | Yes            | ISO 8601 last modified timestamp                     |
| deletedAt                 | datetime                           | No             | ISO 8601 soft-deletion timestamp (null if active)    |

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
| role            | string   | User role (`USER`, `MODERATOR`, `ADMIN`, `OWNER`) |
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

### CampaignInviteResponse

| Field      | Type     | Description                           |
|------------|----------|---------------------------------------|
| id         | long     | Invite ID                             |
| campaignId | long     | Campaign ID                           |
| token      | string   | UUID invite token                     |
| expiresAt  | datetime | When the invite expires               |
| createdAt  | datetime | When the invite was created           |

### JoinCampaignResponse

| Field        | Type   | Description                        |
|--------------|--------|------------------------------------|
| campaignId   | long   | ID of the campaign joined          |
| campaignName | string | Name of the campaign joined        |
| message      | string | Human-readable success message     |

### CampaignCharacterSummaryResponse

Lightweight character summary used in campaign GET with `?expand=characterSummaries`. Includes all characters across pending, PC, and NPC collections.

| Field          | Type     | Description                              |
|----------------|----------|------------------------------------------|
| id             | long     | Character sheet ID                       |
| name           | string   | Character name                           |
| level          | integer  | Character level                          |
| ownerId        | long     | ID of the character owner                |
| ownerUsername  | string   | Username of the character owner          |
| ancestryNames  | string[] | Names of ancestry cards                  |
| subclassNames  | string[] | Names of subclass cards                  |
| classNames     | string[] | Names of associated classes (via subclass paths) |

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
| characterSummaries      | List of CampaignCharacterSummaryResponse for all characters |
| all                     | Expands all of the above simultaneously             |

Multiple values can be combined with commas: `?expand=creator,gameMasters,players`

---

## Campaign Lifecycle: Ended vs Deleted

Campaigns have two distinct lifecycle states beyond active:

### Ended (`endedAt` set)
- Campaign is locked but remains visible
- Most mutating operations are blocked (update, add players, add GMs, submit/approve characters, generate invites)
- Unlinking character sheets and leaving are still allowed
- Characters in ended campaigns are excluded from the one-campaign constraint (can be linked to new campaigns)

### Deleted (`deletedAt` set)
- Campaign is invisible (excluded from all active queries)
- Soft deletion preserves data in the database
- Campaign relationships (GMs, players, character sheets) are preserved
- A `restore()` method exists on the entity but is not exposed via API

---

## Access Control Summary

| Operation                  | Creator | GM (non-creator) | Player  | MODERATOR+ | Ended OK? |
|----------------------------|---------|-------------------|---------|------------|-----------|
| My campaigns               | N/A     | N/A               | N/A     | Any auth   | N/A       |
| Join via invite            | N/A     | N/A               | N/A     | Any auth   | No        |
| List all campaigns         | No      | No                | No      | Yes        | N/A       |
| View campaign              | Yes     | Yes               | Yes     | Yes        | Yes       |
| Create campaign            | N/A     | N/A               | N/A     | Any auth   | N/A       |
| Update campaign            | Yes     | No                | No      | Yes        | No        |
| Delete campaign            | Yes     | No                | No      | Yes        | Yes       |
| Generate invite            | Yes     | Yes               | No      | Yes        | No        |
| End campaign               | Yes     | No                | No      | Yes        | No**      |
| Leave campaign             | No      | No                | Yes     | No         | Yes       |
| Add game master            | Yes     | No                | No      | Yes        | No        |
| Remove game master         | Yes     | No                | No      | Yes        | Yes       |
| Add player                 | Yes     | Yes               | No      | Yes        | No        |
| Kick player                | Yes     | Yes               | No      | Yes        | Yes       |
| Submit character sheet     | No      | No                | Yes*    | No         | No        |
| Approve character sheet    | Yes     | Yes               | No      | Yes        | No        |
| Reject character sheet     | Yes     | Yes               | No      | Yes        | Yes       |
| Add NPC                    | Yes     | Yes               | No      | Yes        | No        |
| Remove character sheet     | Yes     | Yes               | Yes***  | Yes        | Yes       |

\* Must also be the character sheet owner. Character sheet must not already be in an active campaign.
\*\* Returns error if already ended.
\*\*\* Character sheet owner can only remove their own sheets.

---

## Database Schema

### campaigns

| Column           | Type           | Nullable | Constraints                        |
|------------------|----------------|----------|------------------------------------|
| id               | BIGSERIAL      | No       | PRIMARY KEY                        |
| name             | VARCHAR(200)   | No       | CHECK LENGTH(TRIM(name)) > 0      |
| description      | VARCHAR(2000)  | Yes      |                                    |
| creator_id       | BIGINT         | No       | FK -> users(id) ON DELETE CASCADE  |
| ended_at         | TIMESTAMP      | Yes      |                                    |
| deleted_at       | TIMESTAMP      | Yes      |                                    |
| created_at       | TIMESTAMP      | No       | DEFAULT CURRENT_TIMESTAMP          |
| last_modified_at | TIMESTAMP      | No       | DEFAULT CURRENT_TIMESTAMP          |

Indexes: `idx_campaigns_creator_id`, `idx_campaigns_deleted_at`, `idx_campaigns_ended_at`, `idx_campaigns_creator_not_deleted` (partial, WHERE deleted_at IS NULL), `idx_campaigns_name`

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

### campaign_invites

| Column           | Type          | Nullable | Constraints                             |
|------------------|---------------|----------|-----------------------------------------|
| id               | BIGSERIAL     | No       | PRIMARY KEY                             |
| campaign_id      | BIGINT        | No       | FK -> campaigns(id) ON DELETE CASCADE   |
| token            | VARCHAR(36)   | No       | UNIQUE                                  |
| created_by       | BIGINT        | No       | FK -> users(id) ON DELETE CASCADE       |
| used_by          | BIGINT        | Yes      | FK -> users(id) ON DELETE SET NULL      |
| expires_at       | TIMESTAMP     | No       |                                         |
| used_at          | TIMESTAMP     | Yes      |                                         |
| created_at       | TIMESTAMP     | No       | DEFAULT CURRENT_TIMESTAMP               |
| last_modified_at | TIMESTAMP     | No       | DEFAULT CURRENT_TIMESTAMP               |

Indexes: `idx_campaign_invites_campaign_id`, `idx_campaign_invites_token`, `idx_campaign_invites_expires_at`

Cleanup: Expired unused invites and used invites older than 7 days are cleaned up daily at 3:15 AM.
