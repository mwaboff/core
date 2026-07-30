# Character Sheet Conditions API Reference

Base URL: `http://localhost:8080`

## Overview

A `CharacterSheetCondition` is a character's instance of a catalogue `Condition` (see
`references/conditions-api.md`). Unlike a plain many-to-many link, each instance carries its own
`magnitude` — some conditions stack (e.g., multiple stacks of Ignited), and the magnitude records
how many stacks (or what intensity) currently apply to this specific character. Conditions without
a stacking mechanic simply leave `magnitude` null.

This entity is modelled on `Experience` rather than folded into `CharacterSheetService`'s bulk
create/update payload: like an experience, a condition instance carries its own per-row data (the
magnitude) beyond a bare foreign-key link, so it gets its own dedicated CRUD surface. Instances use
**hard deletion** (not soft delete), matching `Experience`.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:**
- GET endpoints: Any authenticated user
- POST: Any authenticated user
- PUT/DELETE: Character sheet owner OR MODERATOR/ADMIN/OWNER role (enforced in service layer)

---

## Endpoints

### GET /api/dh/character-sheet-conditions

Retrieves a paginated list of condition instances for a character sheet.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100) |
| `characterSheetId` | Long | -- | **Yes** | The character sheet to list conditions for |
| `expand` | string | -- | No | Comma-separated relationships to expand |

**Expand Options:** `characterSheet`, `condition`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "characterSheetId": 1,
      "conditionId": 6,
      "magnitude": 3,
      "createdAt": "2026-07-30T13:17:28.762211",
      "lastModifiedAt": "2026-07-30T13:17:28.762225"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Error Responses:**
- `401 Unauthorized`
- `404 Not Found` — character sheet does not exist

---

### GET /api/dh/character-sheet-conditions/{id}

Retrieves a single condition instance by ID.

**Response:** `200 OK` — a `CharacterSheetConditionResponse`

**Error Responses:**
- `401 Unauthorized`
- `404 Not Found`

---

### POST /api/dh/character-sheet-conditions

Attaches a condition instance to a character sheet. Any authenticated user can attach a condition.

**Request Body:**

```json
{
  "characterSheetId": 1,
  "conditionId": 6,
  "magnitude": 3
}
```

**Field Validation:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `characterSheetId` | Long | Yes | Must reference an active character sheet |
| `conditionId` | Long | Yes | Must reference an active condition |
| `magnitude` | Integer | No | Null for non-stacking conditions |

**Response:** `201 Created` — the created `CharacterSheetConditionResponse`, magnitude included

**Error Responses:**
- `400 Bad Request` — missing required fields
- `401 Unauthorized`
- `404 Not Found` — referenced character sheet or condition does not exist

---

### PUT /api/dh/character-sheet-conditions/{id}

Updates the magnitude of an existing condition instance.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Request Body:**

```json
{
  "magnitude": 5
}
```

**Response:** `200 OK` — updated `CharacterSheetConditionResponse`

**Error Responses:**
- `401 Unauthorized`
- `403 Forbidden` — user is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found`

---

### DELETE /api/dh/character-sheet-conditions/{id}

Removes a condition instance from a character sheet (hard delete).

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`

---

## Response DTO

### CharacterSheetConditionResponse

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `id` | Long | Yes | Unique identifier |
| `characterSheetId` | Long | Yes | Owning character sheet ID |
| `characterSheet` | CharacterSheetResponse | Only with `?expand=characterSheet` | Full character sheet (minimal fields) |
| `conditionId` | Long | Yes | Catalogue condition ID |
| `condition` | ConditionResponse | Only with `?expand=condition` | Full condition object |
| `magnitude` | Integer | If non-null | Stack count / intensity |
| `createdAt` | LocalDateTime | Yes | |
| `lastModifiedAt` | LocalDateTime | Yes | |

`@JsonInclude(NON_NULL)` is applied — null fields (including `magnitude` for non-stacking
conditions) are omitted from the JSON response.

---

## Database Schema

**Table:** `character_sheet_conditions`

Created by: `V20260730130404220__create_conditions_and_character_sheet_conditions.sql`

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| `id` | BIGSERIAL | No | Primary key |
| `character_sheet_id` | BIGINT | No | FK -> character_sheets(id) ON DELETE CASCADE |
| `condition_id` | BIGINT | No | FK -> conditions(id) ON DELETE CASCADE |
| `magnitude` | INTEGER | Yes | Stack count / intensity, where applicable |
| `created_at` | TIMESTAMP | No | |
| `last_modified_at` | TIMESTAMP | No | |

**Note:** No `deleted_at` column — condition instances use hard deletion, matching `experiences`.

**`CharacterSheet.java` surface:** exactly one field was added —
`Set<CharacterSheetCondition> characterSheetConditions`, mirroring the existing `experiences`
collection declaration. No other part of `CharacterSheet.java`, `CharacterSheetService.java`, or
`CharacterSheetController.java` was touched; this entity does not go through the character sheet's
own create/update payload handling.

---

## Test Examples

### Attach a stacking condition
```bash
curl -X POST http://localhost:8080/api/dh/character-sheet-conditions \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{ "characterSheetId": 1, "conditionId": 6, "magnitude": 3 }'
```

### Attach a non-stacking condition (no magnitude)
```bash
curl -X POST http://localhost:8080/api/dh/character-sheet-conditions \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{ "characterSheetId": 1, "conditionId": 1 }'
```

### List conditions for a character
```bash
curl "http://localhost:8080/api/dh/character-sheet-conditions?characterSheetId=1" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Update magnitude (owner or moderator+)
```bash
curl -X PUT http://localhost:8080/api/dh/character-sheet-conditions/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{ "magnitude": 5 }'
```

### Remove a condition instance
```bash
curl -X DELETE http://localhost:8080/api/dh/character-sheet-conditions/1 \
  --cookie "AUTH_TOKEN=<owner_jwt>"
```
