# Questions API

Base path: `/api/dh/questions`
Authentication: All endpoints require a valid JWT token (HttpOnly `AUTH_TOKEN` cookie).
Write access: `POST`, `PUT`, `DELETE` endpoints require `ADMIN` or `OWNER` role.

Questions are prompts used during character creation in the Daggerheart TTRPG system. They come in two types: background questions (about the character's history) and connection questions (about relationships with other characters).

## Endpoints

### GET `/api/dh/questions`

Retrieve a paginated list of questions.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Items per page (max: 100) |
| `includeDeleted` | `boolean` | `false` | Include soft-deleted questions (ADMIN+ only) |
| `expansionId` | `Long` | — | Filter by expansion ID |
| `questionType` | `QuestionType` | — | Filter by question type (`BACKGROUND` or `CONNECTION`) |
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Expand Options

| Value | Effect |
|---|---|
| `expansion` | Includes full `ExpansionResponse` object |

#### `QuestionType` Enum Values

| Value | Description |
|---|---|
| `BACKGROUND` | Questions about the character's history and backstory |
| `CONNECTION` | Questions about relationships with other characters |

#### Response: `PagedResponse<QuestionResponse>` — `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "questionText": "What is your greatest fear?",
      "questionType": "BACKGROUND",
      "expansionId": 1,
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    },
    {
      "id": 2,
      "questionText": "Who do you trust most in the party?",
      "questionType": "CONNECTION",
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
# List all questions
curl -b AUTH_TOKEN=<token> http://localhost:8080/api/dh/questions

# Filter by question type
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/questions?questionType=BACKGROUND"

# Filter by expansion with expanded relationship
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/questions?expansionId=1&expand=expansion"

# Paginated request
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/questions?page=1&size=2"
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |

---

### GET `/api/dh/questions/{id}`

Retrieve a single question by ID.

**Authentication:** Authenticated users
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The question ID |

#### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `expand` | `String` | — | Comma-separated list of relationships to expand |

#### Response: `QuestionResponse` — `200 OK`

```json
{
  "id": 1,
  "questionText": "What is your greatest fear?",
  "questionType": "BACKGROUND",
  "expansionId": 1,
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Example with expand

```bash
curl -b AUTH_TOKEN=<token> \
  "http://localhost:8080/api/dh/questions/1?expand=expansion"
```

Response with expansion expanded:

```json
{
  "id": 1,
  "questionText": "What is your greatest fear?",
  "questionType": "BACKGROUND",
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
| `404 Not Found` | Question with the given ID does not exist |

---

### POST `/api/dh/questions`

Create a new question.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateQuestionRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `questionText` | `String` | Yes | Not blank | The question text |
| `questionType` | `QuestionType` | Yes | `BACKGROUND` or `CONNECTION` | Type of question |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this question belongs to |

```json
{
  "questionText": "What is your greatest fear?",
  "questionType": "BACKGROUND",
  "expansionId": 1
}
```

#### Response: `QuestionResponse` — `201 Created`

(Same structure as GET by ID response)

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/questions \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "questionText": "What is your greatest fear?",
    "questionType": "BACKGROUND",
    "expansionId": 1
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed (missing question text, invalid question type) |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Referenced expansion ID does not exist |

---

### POST `/api/dh/questions/bulk`

Create multiple questions in a single request.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `201 Created`

#### Request Body: `CreateQuestionRequest[]`

Array of `CreateQuestionRequest` objects (same schema as single create).

```json
[
  {
    "questionText": "What is your background?",
    "questionType": "BACKGROUND",
    "expansionId": 1
  },
  {
    "questionText": "Who do you know in the party?",
    "questionType": "CONNECTION",
    "expansionId": 1
  }
]
```

#### Response: `QuestionResponse[]` — `201 Created`

Array of created `QuestionResponse` objects.

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed on one or more items |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |

---

### PUT `/api/dh/questions/{id}`

Update an existing question.

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The question ID to update |

#### Request Body: `UpdateQuestionRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `questionText` | `String` | Yes | Not blank | The question text |
| `questionType` | `QuestionType` | Yes | `BACKGROUND` or `CONNECTION` | Type of question |
| `expansionId` | `Long` | Yes | Must reference existing expansion | Expansion this question belongs to |

```json
{
  "questionText": "Updated question text",
  "questionType": "CONNECTION",
  "expansionId": 1
}
```

#### Response: `QuestionResponse` — `200 OK`

#### Example

```bash
curl -X PUT http://localhost:8080/api/dh/questions/1 \
  -b AUTH_TOKEN=<token> \
  -H "Content-Type: application/json" \
  -d '{
    "questionText": "Updated question text",
    "questionType": "CONNECTION",
    "expansionId": 1
  }'
```

#### Error Responses

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failed |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Question or referenced expansion does not exist |

---

### DELETE `/api/dh/questions/{id}`

Soft-delete a question (sets `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `204 No Content`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The question ID to delete |

#### Example

```bash
curl -X DELETE http://localhost:8080/api/dh/questions/1 \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Question does not exist |

---

### POST `/api/dh/questions/{id}/restore`

Restore a soft-deleted question (clears `deletedAt` timestamp).

**Authentication:** `ADMIN` or `OWNER` role required
**Status:** `200 OK`

#### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `id` | `Long` | The question ID to restore |

#### Response: `QuestionResponse` — `200 OK`

#### Example

```bash
curl -X POST http://localhost:8080/api/dh/questions/1/restore \
  -b AUTH_TOKEN=<token>
```

#### Error Responses

| Status | Condition |
|---|---|
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | User does not have ADMIN or OWNER role |
| `404 Not Found` | Question does not exist |

---

## Response Model: `QuestionResponse`

Uses `@JsonInclude(NON_NULL)` -- null fields are omitted from the JSON response.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique identifier |
| `questionText` | `String` | No | The question text |
| `questionType` | `QuestionType` | No | `BACKGROUND` or `CONNECTION` |
| `expansionId` | `Long` | No | ID of the associated expansion |
| `expansion` | `ExpansionResponse` | Yes | Full expansion object (only with `?expand=expansion`) |
| `createdAt` | `LocalDateTime` | No | Creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (null if active) |
