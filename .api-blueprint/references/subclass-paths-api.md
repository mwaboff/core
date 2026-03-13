# Subclass Paths API

Base path: `/api/dh/subclass-paths`
Authentication: All endpoints require a valid JWT token (HttpOnly `AUTH_TOKEN` cookie).
Write access: `POST`, `PUT`, `DELETE` endpoints require `ADMIN` or `OWNER` role.

Subclass paths group three subclass cards (Foundation, Specialization, Mastery) that share a common theme within a class. Each path has associated domains and an optional spellcasting trait.

## Endpoints

### GET `/api/dh/subclass-paths`

Retrieve a paginated list of subclass paths.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Items per page (max: 100) |
| `includeDeleted` | `boolean` | `false` | Include soft-deleted paths (ADMIN+ only) |
| `classId` | `Long` | — | Filter by associated class ID |
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Expand Options

| Value | Effect |
|---|---|
| `associatedClass` | Includes full `ClassResponse` object |
| `associatedDomains` | Includes full `DomainResponse[]` array |
| `expansion` | Includes full `ExpansionResponse` object |

#### Response: `PagedResponse<SubclassPathResponse>` — `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Warden of Renewal",
      "associatedClassId": 1,
      "spellcastingTrait": {
        "trait": "INSTINCT",
        "description": "Intuition, awareness, and natural understanding",
        "examples": "Perception, survival, animal handling, reading situations"
      },
      "associatedDomainIds": [1, 3],
      "expansionId": 1,
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

#### Example

```bash
# List all subclass paths
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/subclass-paths

# Filter by class with expanded relationships
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/subclass-paths?classId=1&expand=associatedClass,associatedDomains,expansion"

# Paginated request
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/subclass-paths?page=1&size=2"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |

---

### GET `/api/dh/subclass-paths/{id}`

Retrieve a single subclass path by ID.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The subclass path ID |

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expand` | `String` | — | Comma-separated list of relationships to expand (same options as list endpoint) |

#### Response: `SubclassPathResponse` — `200 OK`

```json
{
  "id": 1,
  "name": "Warden of Renewal",
  "associatedClassId": 1,
  "spellcastingTrait": {
    "trait": "INSTINCT",
    "description": "Intuition, awareness, and natural understanding",
    "examples": "Perception, survival, animal handling, reading situations"
  },
  "associatedDomainIds": [1, 3],
  "expansionId": 1,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example with expand

```bash
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/subclass-paths/1?expand=associatedClass,associatedDomains"
```

Response with `associatedClass` expanded:

```json
{
  "id": 1,
  "name": "Warden of Renewal",
  "associatedClassId": 1,
  "associatedClass": {
    "id": 1,
    "name": "Druid",
    "description": "A nature-aligned spellcaster",
    "expansionId": 1,
    "startingEvasion": 9,
    "startingHitPoints": 16,
    "createdAt": "2026-03-13T12:00:00",
    "lastModifiedAt": "2026-03-13T12:00:00"
  },
  "associatedDomainIds": [1, 3],
  "expansionId": 1,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `404 Not Found` | Subclass path with the given ID does not exist |

---

### POST `/api/dh/subclass-paths`

Create a new subclass path.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateSubclassPathRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 200 chars; not blank | Subclass path name |
| `associatedClassId` | `Long` | Yes | Must reference existing class | Class this path belongs to |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this path belongs to |
| `spellcastingTrait` | `Trait` | No | One of the valid Trait values | Spellcasting trait for the path |
| `associatedDomainIds` | `Long[]` | No | — | IDs of domains to associate |

##### `Trait` Enum Values

| Value | Description | Examples |
|---|---|---|
| `AGILITY` | Quick reflexes, nimbleness, and coordination | Dodging attacks, acrobatics, sleight of hand, stealth |
| `STRENGTH` | Raw physical power and endurance | Melee attacks, athletics, breaking objects, carrying heavy loads |
| `FINESSE` | Precision, grace, and careful execution | Ranged attacks, lockpicking, crafting, precise movements |
| `INSTINCT` | Intuition, awareness, and natural understanding | Perception, survival, animal handling, reading situations |
| `PRESENCE` | Force of personality and social influence | Persuasion, intimidation, performance, leadership |
| `KNOWLEDGE` | Learning, reasoning, and mental acuity | Spellcasting, history, investigation, arcana |

```json
{
  "name": "Warden of Renewal",
  "associatedClassId": 1,
  "expansionId": 1,
  "spellcastingTrait": "INSTINCT",
  "associatedDomainIds": [1, 3]
}
```

#### Response: `SubclassPathResponse` — `201 Created`

(Same structure as GET by ID response)

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/subclass-paths \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warden of Renewal",
    "associatedClassId": 1,
    "expansionId": 1,
    "spellcastingTrait": "INSTINCT",
    "associatedDomainIds": [1, 3]
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed (missing required fields, invalid trait value) |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Referenced class or expansion ID does not exist |

---

### POST `/api/dh/subclass-paths/bulk`

Create multiple subclass paths in a single request.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateSubclassPathRequest[]`

Array of `CreateSubclassPathRequest` objects (same schema as single create).

```json
[
  {
    "name": "Warden of Renewal",
    "associatedClassId": 1,
    "expansionId": 1
  },
  {
    "name": "Warden of the Elements",
    "associatedClassId": 1,
    "expansionId": 1,
    "spellcastingTrait": "KNOWLEDGE"
  }
]
```

#### Response: `SubclassPathResponse[]` — `201 Created`

Array of created `SubclassPathResponse` objects.

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed on one or more items |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |

---

### PUT `/api/dh/subclass-paths/{id}`

Update an existing subclass path.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The subclass path ID to update |

#### Request Body: `UpdateSubclassPathRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 200 chars; not blank | Subclass path name |
| `associatedClassId` | `Long` | Yes | Must reference existing class | Class this path belongs to |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this path belongs to |
| `spellcastingTrait` | `Trait` | No | One of the valid Trait values | Spellcasting trait for the path |
| `associatedDomainIds` | `Long[]` | No | — | IDs of domains to associate |

```json
{
  "name": "Warden of Renewal",
  "associatedClassId": 1,
  "expansionId": 1,
  "spellcastingTrait": "INSTINCT",
  "associatedDomainIds": [1, 3]
}
```

#### Response: `SubclassPathResponse` — `200 OK`

#### Example

```bash
curl -X PUT http://localhost:8080/api/dh/subclass-paths/1 \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warden of Renewal",
    "associatedClassId": 1,
    "expansionId": 1,
    "spellcastingTrait": "INSTINCT"
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Subclass path, referenced class, or referenced expansion does not exist |

---

### DELETE `/api/dh/subclass-paths/{id}`

Soft-delete a subclass path (sets `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `204 No Content`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The subclass path ID to delete |

#### Example

```bash
curl -X DELETE http://localhost:8080/api/dh/subclass-paths/1 \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Subclass path does not exist |

---

### POST `/api/dh/subclass-paths/{id}/restore`

Restore a soft-deleted subclass path (clears `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The subclass path ID to restore |

#### Response: `SubclassPathResponse` — `200 OK`

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/subclass-paths/1/restore \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Subclass path does not exist |

---

## Response Model: `SubclassPathResponse`

Uses `@JsonInclude(NON_NULL)` -- null fields are omitted from the JSON response.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique identifier |
| `name` | `String` | No | Subclass path name |
| `associatedClassId` | `Long` | No | ID of the associated class |
| `associatedClass` | `ClassResponse` | Yes | Full class object (only with `?expand=associatedClass`) |
| `spellcastingTrait` | `TraitInfo` | Yes | Spellcasting trait with metadata (null if path has no spellcasting) |
| `associatedDomainIds` | `Long[]` | Yes | IDs of associated domains |
| `associatedDomains` | `DomainResponse[]` | Yes | Full domain objects (only with `?expand=associatedDomains`) |
| `expansionId` | `Long` | No | ID of the associated expansion |
| `expansion` | `ExpansionResponse` | Yes | Full expansion object (only with `?expand=expansion`) |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active) |

### `TraitInfo` (nested object)

Included when the subclass path has a spellcasting trait. Contains the trait enum value along with its metadata.

| Field | Type | Description |
|---|---|---|
| `trait` | `Trait` | The trait enum value (e.g., `AGILITY`, `KNOWLEDGE`) |
| `description` | `String` | Description of the trait |
| `examples` | `String` | Examples of when the trait is used |

```json
{
  "trait": "INSTINCT",
  "description": "Intuition, awareness, and natural understanding",
  "examples": "Perception, survival, animal handling, reading situations"
}
```
