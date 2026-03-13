# Expansions API

Base path: `/api/dh/expansions`
Authentication: All endpoints require a valid JWT token (HttpOnly `AUTH_TOKEN` cookie).
Write access: `POST`, `PUT`, `DELETE` endpoints require `ADMIN` or `OWNER` role.

Expansions represent content packs or rulebooks in the Daggerheart TTRPG system (e.g., "Core Rulebook", "Twilight Mirage"). Most game content entities reference an expansion.

## Endpoints

### GET `/api/dh/expansions`

Retrieve a paginated list of expansions.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Items per page (max: 100) |
| `includeDeleted` | `boolean` | `false` | Include soft-deleted expansions (ADMIN+ only) |
| `published` | `Boolean` | — | Filter by published status (`true` or `false`) |

Note: This endpoint does not support `?expand` -- the `ExpansionResponse` has no expandable relationships.

#### Response: `PagedResponse<ExpansionResponse>` — `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Core Rulebook",
      "isPublished": true,
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    },
    {
      "id": 2,
      "name": "Twilight Mirage",
      "isPublished": false,
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

#### Example

```bash
# List all expansions
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/expansions

# Filter by published status
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/expansions?published=true"

# Paginated request
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/expansions?page=1&size=2"

# Include soft-deleted expansions (ADMIN+ only)
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/expansions?includeDeleted=true"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |

---

### GET `/api/dh/expansions/{id}`

Retrieve a single expansion by ID.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The expansion ID |

#### Response: `ExpansionResponse` — `200 OK`

```json
{
  "id": 1,
  "name": "Core Rulebook",
  "isPublished": true,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example

```bash
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/expansions/1
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `404 Not Found` | Expansion with the given ID does not exist |

---

### POST `/api/dh/expansions`

Create a new expansion.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateExpansionRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 255 chars; not blank | Expansion name |
| `isPublished` | `Boolean` | Yes | — | Whether the expansion is published and available for use |

```json
{
  "name": "Core Rulebook",
  "isPublished": true
}
```

#### Response: `ExpansionResponse` — `201 Created`

```json
{
  "id": 1,
  "name": "Core Rulebook",
  "isPublished": true,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/expansions \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{"name": "Core Rulebook", "isPublished": true}'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed (missing name or isPublished) |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |

---

### PUT `/api/dh/expansions/{id}`

Update an existing expansion.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The expansion ID to update |

#### Request Body: `UpdateExpansionRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 255 chars; not blank | Expansion name |
| `isPublished` | `Boolean` | Yes | — | Whether the expansion is published |

```json
{
  "name": "Updated Name",
  "isPublished": true
}
```

#### Response: `ExpansionResponse` — `200 OK`

#### Example

```bash
curl -X PUT http://localhost:8080/api/dh/expansions/1 \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Name", "isPublished": true}'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Expansion does not exist |

---

### DELETE `/api/dh/expansions/{id}`

Soft-delete an expansion (sets `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `204 No Content`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The expansion ID to delete |

#### Example

```bash
curl -X DELETE http://localhost:8080/api/dh/expansions/1 \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Expansion does not exist |

---

### POST `/api/dh/expansions/{id}/restore`

Restore a soft-deleted expansion (clears `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The expansion ID to restore |

#### Response: `ExpansionResponse` — `200 OK`

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/expansions/1/restore \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Expansion does not exist |

---

## Response Model: `ExpansionResponse`

Note: `ExpansionResponse` does NOT use `@JsonInclude(NON_NULL)` -- all fields are always present in the response (including `deletedAt` as `null`).

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique identifier |
| `name` | `String` | No | Expansion name (e.g., "Core Rulebook", "Twilight Mirage") |
| `isPublished` | `Boolean` | No | Whether the expansion is published and available for use |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active) |
