# Classes API

Base path: `/api/dh/classes`
Authentication: All endpoints require a valid JWT token (HttpOnly `AUTH_TOKEN` cookie).
Write access: `POST`, `PUT`, `DELETE` endpoints require `ADMIN` or `OWNER` role.

## Endpoints

### GET `/api/dh/classes`

Retrieve a paginated list of classes.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Items per page (max: 100) |
| `includeDeleted` | `boolean` | `false` | Include soft-deleted classes (ADMIN+ only) |
| `expansionId` | `Long` | — | Filter by expansion ID |
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Expand Options

| Value | Effect |
|---|---|
| `expansion` | Includes full `ExpansionResponse` object |
| `associatedDomains` | Includes full `DomainResponse[]` array |
| `hopeFeatures` | Includes full `FeatureResponse[]` array |
| `classFeatures` | Includes full `FeatureResponse[]` array |
| `backgroundQuestions` | Includes full `QuestionResponse[]` array |
| `connectionQuestions` | Includes full `QuestionResponse[]` array |

#### Response: `PagedResponse<ClassResponse>` — `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Warrior",
      "description": "A strong fighter",
      "expansionId": 1,
      "startingClassItems": null,
      "startingEvasion": 10,
      "startingHitPoints": 25,
      "associatedDomainIds": [1, 3],
      "hopeFeatureIds": [10],
      "classFeatureIds": [20, 21],
      "backgroundQuestionIds": [5, 6],
      "connectionQuestionIds": [7, 8],
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
# List all classes
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/classes

# Filter by expansion with expanded relationships
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/classes?expansionId=1&expand=expansion,associatedDomains"

# Paginated request
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/classes?page=1&size=2"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |

---

### GET `/api/dh/classes/{id}`

Retrieve a single class by ID.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The class ID |

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expand` | `String` | — | Comma-separated list of relationships to expand (same options as list endpoint) |

#### Response: `ClassResponse` — `200 OK`

```json
{
  "id": 1,
  "name": "Warrior",
  "description": "A strong fighter",
  "expansionId": 1,
  "startingClassItems": null,
  "startingEvasion": 10,
  "startingHitPoints": 25,
  "associatedDomainIds": [1, 3],
  "hopeFeatureIds": [10],
  "classFeatureIds": [20, 21],
  "backgroundQuestionIds": [5, 6],
  "connectionQuestionIds": [7, 8],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example with expand

```bash
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/classes/1?expand=expansion,associatedDomains,hopeFeatures"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `404 Not Found` | Class with the given ID does not exist |

---

### POST `/api/dh/classes`

Create a new class.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateClassRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 100 chars; not blank | Class name |
| `description` | `String` | No | — | Class description |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this class belongs to |
| `startingClassItems` | `String` | No | — | Description of starting class items |
| `startingEvasion` | `Integer` | Yes | Must be positive | Starting evasion score |
| `startingHitPoints` | `Integer` | Yes | Must be positive | Starting hit points |
| `associatedDomainIds` | `Long[]` | No | — | IDs of domains to associate |
| `hopeFeatureIds` | `Long[]` | No | — | IDs of hope features |
| `classFeatureIds` | `Long[]` | No | — | IDs of class features |
| `backgroundQuestionIds` | `Long[]` | No | — | IDs of background questions |
| `connectionQuestionIds` | `Long[]` | No | — | IDs of connection questions |
| `hopeFeatures` | `FeatureInput[]` | No | — | Hope features to find or create inline. Merged with `hopeFeatureIds` |
| `classFeatures` | `FeatureInput[]` | No | — | Class features to find or create inline. Merged with `classFeatureIds` |
| `backgroundQuestions` | `QuestionInput[]` | No | — | Background questions to find or create inline. Merged with `backgroundQuestionIds` |
| `connectionQuestions` | `QuestionInput[]` | No | — | Connection questions to find or create inline. Merged with `connectionQuestionIds` |

```json
{
  "name": "Warrior",
  "description": "A strong fighter",
  "expansionId": 1,
  "startingEvasion": 10,
  "startingHitPoints": 25,
  "associatedDomainIds": [1, 3],
  "hopeFeatureIds": [10],
  "hopeFeatures": [
    {
      "name": "Battle Cry",
      "description": "Inspire allies with a fearsome shout",
      "featureType": "HOPE",
      "expansionId": 1
    }
  ],
  "classFeatures": [
    {
      "name": "Heavy Armor Proficiency",
      "description": "Can wear heavy armor without penalty",
      "featureType": "CLASS",
      "expansionId": 1
    }
  ],
  "backgroundQuestions": [
    {
      "questionText": "What battle shaped you into who you are today?",
      "questionType": "BACKGROUND",
      "expansionId": 1
    }
  ],
  "connectionQuestions": [
    {
      "questionText": "Which party member do you trust to watch your back?",
      "questionType": "CONNECTION",
      "expansionId": 1
    }
  ]
}
```

#### Response: `ClassResponse` — `201 Created`

(Same structure as GET by ID response)

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/classes \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warrior",
    "description": "A strong fighter",
    "expansionId": 1,
    "startingEvasion": 10,
    "startingHitPoints": 25
  }'

# Create a class with inline features and questions
curl -X POST http://localhost:8080/api/dh/classes \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Warrior",
    "description": "A strong fighter",
    "expansionId": 1,
    "startingEvasion": 10,
    "startingHitPoints": 25,
    "hopeFeatures": [
      {
        "name": "Battle Cry",
        "featureType": "HOPE",
        "expansionId": 1
      }
    ],
    "backgroundQuestions": [
      {
        "questionText": "What battle shaped you?",
        "questionType": "BACKGROUND",
        "expansionId": 1
      }
    ]
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed (missing required fields, invalid values) |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Referenced expansion ID does not exist |

---

### POST `/api/dh/classes/bulk`

Create multiple classes in a single request.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateClassRequest[]`

Array of `CreateClassRequest` objects (same schema as single create).

```json
[
  {
    "name": "Warrior",
    "description": "A strong fighter",
    "expansionId": 1,
    "startingEvasion": 10,
    "startingHitPoints": 25,
    "hopeFeatures": [
      {
        "name": "Battle Cry",
        "featureType": "HOPE",
        "expansionId": 1
      }
    ],
    "backgroundQuestions": [
      {
        "questionText": "What battle shaped you?",
        "questionType": "BACKGROUND",
        "expansionId": 1
      }
    ]
  },
  {
    "name": "Mage",
    "description": "A spell caster",
    "expansionId": 1,
    "startingEvasion": 15,
    "startingHitPoints": 20,
    "classFeatures": [
      {
        "name": "Arcane Focus",
        "featureType": "CLASS",
        "expansionId": 1
      }
    ]
  }
]
```

#### Response: `ClassResponse[]` — `201 Created`

Array of created `ClassResponse` objects.

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed on one or more items |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |

---

### PUT `/api/dh/classes/{id}`

Update an existing class.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The class ID to update |

#### Request Body: `UpdateClassRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `String` | Yes | Max 100 chars; not blank | Class name |
| `description` | `String` | No | — | Class description |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this class belongs to |
| `startingClassItems` | `String` | No | — | Description of starting class items |
| `startingEvasion` | `Integer` | Yes | Must be positive | Starting evasion score |
| `startingHitPoints` | `Integer` | Yes | Must be positive | Starting hit points |
| `associatedDomainIds` | `Long[]` | No | — | IDs of domains to associate |
| `hopeFeatureIds` | `Long[]` | No | — | IDs of hope features |
| `classFeatureIds` | `Long[]` | No | — | IDs of class features |
| `backgroundQuestionIds` | `Long[]` | No | — | IDs of background questions |
| `connectionQuestionIds` | `Long[]` | No | — | IDs of connection questions |
| `hopeFeatures` | `FeatureInput[]` | No | — | Hope features to find or create inline. Merged with `hopeFeatureIds` |
| `classFeatures` | `FeatureInput[]` | No | — | Class features to find or create inline. Merged with `classFeatureIds` |
| `backgroundQuestions` | `QuestionInput[]` | No | — | Background questions to find or create inline. Merged with `backgroundQuestionIds` |
| `connectionQuestions` | `QuestionInput[]` | No | — | Connection questions to find or create inline. Merged with `connectionQuestionIds` |

```json
{
  "name": "Guardian",
  "description": "Updated description",
  "expansionId": 1,
  "startingEvasion": 12,
  "startingHitPoints": 30
}
```

#### Response: `ClassResponse` — `200 OK`

#### Example

```bash
curl -X PUT http://localhost:8080/api/dh/classes/1 \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Guardian",
    "description": "Updated description",
    "expansionId": 1,
    "startingEvasion": 12,
    "startingHitPoints": 30
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Class or referenced expansion does not exist |

---

### DELETE `/api/dh/classes/{id}`

Soft-delete a class (sets `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `204 No Content`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The class ID to delete |

#### Example

```bash
curl -X DELETE http://localhost:8080/api/dh/classes/1 \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Class does not exist |

---

### POST `/api/dh/classes/{id}/restore`

Restore a soft-deleted class (clears `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The class ID to restore |

#### Response: `ClassResponse` — `200 OK`

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/classes/1/restore \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Class does not exist |

---

## Inline Creation (Find-or-Create)

The create and update endpoints support inline creation of features and questions. For each relationship, you can provide:

- **IDs only** (`hopeFeatureIds`, `classFeatureIds`, etc.) — references existing entities
- **Inline inputs only** (`hopeFeatures`, `classFeatures`, etc.) — finds or creates entities
- **Both** — results are merged (union of ID lookups and find-or-create results)

### Find-or-Create Behavior

- **Features** are matched case-insensitively by `(name, expansionId, featureType)`. If found, the existing feature is reused. If not, a new feature is created.
- **Questions** are matched case-insensitively by `(questionText, expansionId, questionType)`. If found, the existing question is reused. If not, a new question is created.

### Update Semantics

For `PUT` requests:
- **Both null** (field omitted) — existing relationship is not modified
- **Empty array** (`[]`) — clears the relationship
- **Populated** — replaces the relationship with the resolved set

---

## Response Model: `ClassResponse`

Uses `@JsonInclude(NON_NULL)` -- null fields are omitted from the JSON response.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique identifier |
| `name` | `String` | No | Class name |
| `description` | `String` | Yes | Class description |
| `expansionId` | `Long` | No | ID of the associated expansion |
| `expansion` | `ExpansionResponse` | Yes | Full expansion object (only with `?expand=expansion`) |
| `startingClassItems` | `String` | Yes | Starting class items description |
| `startingEvasion` | `Integer` | No | Starting evasion score |
| `startingHitPoints` | `Integer` | No | Starting hit points |
| `associatedDomainIds` | `Long[]` | Yes | IDs of associated domains |
| `hopeFeatureIds` | `Long[]` | Yes | IDs of hope features |
| `classFeatureIds` | `Long[]` | Yes | IDs of class features |
| `backgroundQuestionIds` | `Long[]` | Yes | IDs of background questions |
| `connectionQuestionIds` | `Long[]` | Yes | IDs of connection questions |
| `associatedDomains` | `DomainResponse[]` | Yes | Full domain objects (only with `?expand=associatedDomains`) |
| `hopeFeatures` | `FeatureResponse[]` | Yes | Full feature objects (only with `?expand=hopeFeatures`) |
| `classFeatures` | `FeatureResponse[]` | Yes | Full feature objects (only with `?expand=classFeatures`) |
| `backgroundQuestions` | `QuestionResponse[]` | Yes | Full question objects (only with `?expand=backgroundQuestions`) |
| `connectionQuestions` | `QuestionResponse[]` | Yes | Full question objects (only with `?expand=connectionQuestions`) |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active) |

## Nested Models

### `ExpansionResponse`

See [expansions-api.md](./expansions-api.md) for full details.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Expansion ID |
| `name` | `String` | Expansion name |
| `isPublished` | `Boolean` | Published status |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |

### `DomainResponse`

See [domains-api.md](./domains-api.md) for full details.

### `FeatureResponse`

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Feature ID |
| `name` | `String` | Feature name |
| `description` | `String` | Feature description |
| `featureType` | `FeatureType` | One of: `HOPE`, `ANCESTRY`, `CLASS`, `COMMUNITY`, `DOMAIN`, `OTHER` |
| `expansionId` | `Long` | Expansion ID |
| `costTagIds` | `Long[]` | Cost tag IDs |
| `modifierIds` | `Long[]` | Modifier IDs |
| `createdAt` | `LocalDateTime` | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | Last modification timestamp |

### `QuestionResponse`

See [questions-api.md](./questions-api.md) for full details.

### `FeatureInput`

Input for inline feature creation. Features are matched case-insensitively by `(name, expansionId, featureType)`. If a match is found, it is reused; otherwise a new feature is created.

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | `String` | No | Feature name (max 200 chars). Matched case-insensitively |
| `description` | `String` | No | Feature description |
| `featureType` | `FeatureType` | Yes | One of: `HOPE`, `ANCESTRY`, `CLASS`, `COMMUNITY`, `DOMAIN`, `OTHER`, `SUBCLASS`, `ITEM` |
| `expansionId` | `Long` | Yes | Expansion ID |
| `costTagIds` | `Long[]` | No | Existing cost tag IDs |
| `costTags` | `CostTagInput[]` | No | Cost tags to find or create by label |
| `modifierIds` | `Long[]` | No | Existing modifier IDs |
| `modifiers` | `FeatureModifierInput[]` | No | Modifiers to find or create by (target, operation, value) |

### `QuestionInput`

Input for inline question creation. Questions are matched case-insensitively by `(questionText, expansionId, questionType)`. If a match is found, it is reused; otherwise a new question is created.

| Field | Type | Required | Description |
|---|---|---|---|
| `questionText` | `String` | Yes | The question text (not blank) |
| `questionType` | `QuestionType` | Yes | `BACKGROUND` or `CONNECTION` |
| `expansionId` | `Long` | Yes | Expansion ID |

### `CostTagInput`

See [card-cost-tags-api.md](./card-cost-tags-api.md) for full details.

### `FeatureModifierInput`

See [feature-modifiers-api.md](./feature-modifiers-api.md) for full details.
