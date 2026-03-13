# Domains API

Base path: `/api/dh/domains`
Authentication: All endpoints require a valid JWT token (HttpOnly `AUTH_TOKEN` cookie).
Write access: `POST`, `PUT`, `DELETE` endpoints require `ADMIN` or `OWNER` role.

Domains represent magical or thematic categories in the Daggerheart TTRPG system (e.g., Fire, Ice, Nature). They are associated with classes and subclass paths.

## Endpoints

### GET `/api/dh/domains`

Retrieve a paginated list of domains.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Items per page (max: 100) |
| `includeDeleted` | `boolean` | `false` | Include soft-deleted domains (ADMIN+ only) |
| `expansionId` | `Long` | — | Filter by expansion ID |
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Expand Options

| Value | Effect |
|---|---|
| `expansion` | Includes full `ExpansionResponse` object |

#### Response: `PagedResponse<DomainResponse>` — `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Fire",
      "iconUrl": "https://example.com/fire.png",
      "description": "Fire magic domain",
      "expansionId": 1,
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    },
    {
      "id": 2,
      "name": "Ice",
      "iconUrl": "https://example.com/ice.png",
      "description": "Ice magic domain",
      "expansionId": 1,
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
# List all domains
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/domains

# Filter by expansion with expanded relationship
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/domains?expansionId=1&expand=expansion"

# Paginated request
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/domains?page=1&size=2"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |

---

### GET `/api/dh/domains/{id}`

Retrieve a single domain by ID.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The domain ID |

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Response: `DomainResponse` — `200 OK`

```json
{
  "id": 1,
  "name": "Fire",
  "iconUrl": "https://example.com/fire.png",
  "description": "Fire magic domain",
  "expansionId": 1,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example with expand

```bash
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/domains/1?expand=expansion"
```

Response with expansion expanded:

```json
{
  "id": 1,
  "name": "Fire",
  "description": "Fire magic domain",
  "expansionId": 1,
  "expansion": {
    "id": 1,
    "name": "Core Rulebook",
    "isPublished": true,
    "createdAt": "2026-03-13T12:00:00",
    "lastModifiedAt": "2026-03-13T12:00:00"
  },
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `404 Not Found` | Domain with the given ID does not exist |

---

### POST `/api/dh/domains`

Create a new domain.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateDomainRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 100 chars; not blank | Domain name |
| `iconUrl` | `String` | No | Max 500 chars | URL to domain icon |
| `description` | `String` | No | — | Domain description |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this domain belongs to |

```json
{
  "name": "Fire",
  "description": "Fire magic domain",
  "iconUrl": "https://example.com/fire.png",
  "expansionId": 1
}
```

#### Response: `DomainResponse` — `201 Created`

(Same structure as GET by ID response)

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/domains \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fire",
    "description": "Fire magic domain",
    "iconUrl": "https://example.com/fire.png",
    "expansionId": 1
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed (missing name, etc.) |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Referenced expansion ID does not exist |

---

### POST `/api/dh/domains/bulk`

Create multiple domains in a single request.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateDomainRequest[]`

Array of `CreateDomainRequest` objects (same schema as single create).

```json
[
  {
    "name": "Fire",
    "description": "Fire domain",
    "expansionId": 1
  },
  {
    "name": "Ice",
    "description": "Ice domain",
    "expansionId": 1
  }
]
```

#### Response: `DomainResponse[]` — `201 Created`

Array of created `DomainResponse` objects.

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed on one or more items |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |

---

### PUT `/api/dh/domains/{id}`

Update an existing domain.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The domain ID to update |

#### Request Body: `UpdateDomainRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 100 chars; not blank | Domain name |
| `iconUrl` | `String` | No | Max 500 chars | URL to domain icon |
| `description` | `String` | No | — | Domain description |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this domain belongs to |

```json
{
  "name": "Flame",
  "description": "Updated description",
  "iconUrl": "https://example.com/flame.png",
  "expansionId": 1
}
```

#### Response: `DomainResponse` — `200 OK`

#### Example

```bash
curl -X PUT http://localhost:8080/api/dh/domains/1 \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Flame",
    "description": "Updated description",
    "iconUrl": "https://example.com/flame.png",
    "expansionId": 1
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Domain or referenced expansion does not exist |

---

### DELETE `/api/dh/domains/{id}`

Soft-delete a domain (sets `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `204 No Content`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The domain ID to delete |

#### Example

```bash
curl -X DELETE http://localhost:8080/api/dh/domains/1 \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Domain does not exist |

---

### POST `/api/dh/domains/{id}/restore`

Restore a soft-deleted domain (clears `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The domain ID to restore |

#### Response: `DomainResponse` — `200 OK`

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/domains/1/restore \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Domain does not exist |

---

## Response Model: `DomainResponse`

Uses `@JsonInclude(NON_NULL)` -- null fields are omitted from the JSON response.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique identifier |
| `name` | `String` | No | Domain name (e.g., "Fire", "Ice", "Nature") |
| `iconUrl` | `String` | Yes | URL to the domain's icon |
| `description` | `String` | Yes | Domain description |
| `expansionId` | `Long` | No | ID of the associated expansion |
| `expansion` | `ExpansionResponse` | Yes | Full expansion object (only with `?expand=expansion`) |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active) |
