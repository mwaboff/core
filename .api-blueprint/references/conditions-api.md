# Conditions API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/dh/conditions`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)
**Content-Type:** `application/json`

---

## Overview

Conditions are the catalogue of named rules effects a character can carry — 2 from the core
rulebook (Restrained, Vulnerable) and 4 from Hope & Fear (Drained, Hexed, Chained, Ignited). This
API manages the catalogue itself. A character's actual instance of a condition — including a
per-instance `magnitude` for conditions that stack — is a separate resource; see
`references/character-sheet-conditions-api.md`.

Follows the same official/custom content pattern as `Weapon`/`Beastform`: mutation endpoints are
ADMIN/OWNER-gated, and creation respects the caller-supplied `isOfficial` value rather than
hardcoding it, so official conditions can be bulk-imported from the rulebook.

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/dh/conditions` | Authenticated | List conditions (paginated, filterable) |
| 2 | GET | `/api/dh/conditions/{id}` | Authenticated | Get condition by ID |
| 3 | POST | `/api/dh/conditions` | ADMIN, OWNER | Create a condition |
| 4 | POST | `/api/dh/conditions/bulk` | ADMIN, OWNER | Create multiple conditions |
| 5 | PUT | `/api/dh/conditions/{id}` | ADMIN, OWNER | Update a condition |
| 6 | DELETE | `/api/dh/conditions/{id}` | ADMIN, OWNER | Soft-delete a condition |
| 7 | POST | `/api/dh/conditions/{id}/restore` | ADMIN, OWNER | Restore a soft-deleted condition |

---

## 1. GET `/api/dh/conditions`

List all active conditions with optional filters and pagination.

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |
| `includeDeleted` | boolean | `false` | No | Include soft-deleted conditions (MODERATOR+ only; see [Content Gating](#content-gating)) |
| `expansionId` | Long | -- | No | Filter by expansion ID |
| `isOfficial` | Boolean | -- | No | Filter by official status |
| `expand` | String | -- | No | Comma-separated relationships to expand (`expansion`) |

### Response: `200 OK`

```json
{
  "content": [ ConditionResponse, ... ],
  "totalElements": 6,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## 2. GET `/api/dh/conditions/{id}`

Retrieve a single condition by ID.

### Response: `200 OK`

```json
{
  "id": 6,
  "name": "Ignited",
  "description": "A stack of Ignited deals fire damage over time.",
  "expansionId": 2,
  "isOfficial": true,
  "createdAt": "2026-07-30T13:17:07.512244",
  "lastModifiedAt": "2026-07-30T13:17:07.512266"
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 404 | Condition not found (or soft-deleted) |

---

## 3. POST `/api/dh/conditions`

Create a new condition. Requires ADMIN or OWNER role.

### Request Body: `CreateConditionRequest`

```json
{
  "name": "Restrained",
  "description": "You cannot move or evade.",
  "expansionId": 1,
  "isOfficial": true
}
```

### Field Validation

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 200 chars |
| `description` | String | No | Rules text |
| `expansionId` | Long | Yes | Must reference an active expansion |
| `isOfficial` | Boolean | Yes | |
| `srd` | Boolean | No | SRD-licensed content flag. Silently coerced to false for non-ADMIN callers; see [Content Gating](#content-gating) |

### Response: `201 Created`

Returns a `ConditionResponse` object.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 401 | Unauthenticated |
| 403 | Insufficient role (not ADMIN/OWNER) |
| 404 | Referenced expansion not found |

---

## 4. POST `/api/dh/conditions/bulk`

Create multiple conditions in a single request — used to bulk-import all 6 rulebook conditions in
one call. Requires ADMIN or OWNER role.

### Request Body

```json
[
  { "name": "Restrained", "expansionId": 1, "isOfficial": true },
  { "name": "Vulnerable", "expansionId": 1, "isOfficial": true },
  { "name": "Drained", "expansionId": 2, "isOfficial": true },
  { "name": "Hexed", "expansionId": 2, "isOfficial": true },
  { "name": "Chained", "expansionId": 2, "isOfficial": true },
  { "name": "Ignited", "expansionId": 2, "isOfficial": true }
]
```

### Response: `201 Created`

Returns an array of `ConditionResponse` objects.

---

## 5. PUT `/api/dh/conditions/{id}`

Update an existing condition. Requires ADMIN or OWNER role. Only non-null fields in the request are
applied (partial update).

### Request Body: `UpdateConditionRequest`

Same fields as `CreateConditionRequest`, all optional.

### Response: `200 OK`

Returns the updated `ConditionResponse`.

---

## 6. DELETE `/api/dh/conditions/{id}`

Soft-delete a condition. Requires ADMIN or OWNER role.

### Response: `204 No Content`

---

## 7. POST `/api/dh/conditions/{id}/restore`

Restore a soft-deleted condition. Requires ADMIN or OWNER role.

### Response: `200 OK`

Returns the restored `ConditionResponse`.

---

## Response DTOs

### ConditionResponse

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `id` | Long | Yes | Unique identifier |
| `name` | String | Yes | Condition name |
| `description` | String | If non-null | Rules text |
| `expansionId` | Long | Yes | Owning expansion ID |
| `expansionName` | String | Yes | Owning expansion name (the only content-identifying field on a redacted stub) |
| `expansion` | ExpansionResponse | Only with `?expand=expansion` | Full expansion object |
| `isOfficial` | Boolean | Yes | Official game content flag |
| `srd` | Boolean | Yes | SRD-licensed content flag; never present on a redacted stub -- see [Content Gating](#content-gating) |
| `createdAt` | LocalDateTime | Yes | Creation timestamp |
| `lastModifiedAt` | LocalDateTime | Yes | Last update timestamp |
| `deletedAt` | LocalDateTime | If non-null | Soft-deletion timestamp |

`@JsonInclude(NON_NULL)` is applied — null fields are omitted from the JSON response.

---

## Content Gating

Official conditions that are not SRD-licensed (`isOfficial: true`, `srd: false`) are only visible
to ADMIN/OWNER or a user with an explicit "Access All Expansions" grant -- see
`ContentAccessService`. This applies while gating is enabled via the
`application.content.srd-gating-enabled` flag; while disabled, every row is visible to every
authenticated user regardless of `srd`.

- **List/get endpoints** silently exclude restricted rows from `GET /api/dh/conditions`, and
  return a redacted stub (`id`, `expansionName`, `restricted: true` only) from
  `GET /api/dh/conditions/{id}` for a restricted condition.
- **Custom conditions are never gated** -- `isOfficial = false` always passes, regardless of
  `srd`.
- **`includeDeleted=true` now requires MODERATOR+** and is coerced to `false` below that role.
  Previously this parameter had no role check at all despite the Javadoc claiming ADMIN-only.
  The admin listing this unlocks bypasses SRD filtering entirely -- a MODERATOR+ caller sees
  every row regardless of licensing.

---

## Search Integration

`Condition` is `@SearchIndexed(type = SearchableEntityType.CONDITION)`, indexed on name/description
text, filterable by `expansionId` and `isOfficial`. It participates in search exactly like every
other registered type:

- Create/update/soft-delete/restore automatically upsert the `search_index` row via the same
  `EntityChangeEvent` mechanism used by every other searchable entity.
- `POST /api/admin/search/reindex?type=CONDITION` (OWNER only) rebuilds the Condition search index
  from the `conditions` table.
- `GET /api/search?q=...&types=CONDITION&expand=entity` resolves `expandedEntity` to a real
  `ConditionResponse`.

`CharacterSheetCondition` (the per-character instance, see
`references/character-sheet-conditions-api.md`) is deliberately **not** indexed — it is per-character
instance state, not catalogue content, the same as `CharacterSheetLoot`/`CharacterSheetWeapon`/
`CharacterSheetArmor`/`CharacterSheetDomainCard`, none of which are indexed either.

---

## Database Schema

### `conditions` Table

Created by: `V20260730130404220__create_conditions_and_character_sheet_conditions.sql`

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | BIGSERIAL | PK | Auto-generated ID |
| `name` | VARCHAR(200) | NOT NULL | Condition name |
| `description` | TEXT | NULL | Rules text |
| `expansion_id` | BIGINT | NOT NULL | FK -> expansions(id) |
| `is_official` | BOOLEAN | NOT NULL | Official content flag (default: false) |
| `created_by_user_id` | BIGINT | NULL | FK -> users(id) |
| `created_at` | TIMESTAMP | NOT NULL | |
| `last_modified_at` | TIMESTAMP | NOT NULL | |
| `deleted_at` | TIMESTAMP | NULL | Soft-delete marker |

### Indexes

| Index | Columns |
|-------|---------|
| `idx_conditions_expansion_id` | `expansion_id` |
| `idx_conditions_is_official` | `is_official` |
| `idx_conditions_deleted_at` | `deleted_at` |
