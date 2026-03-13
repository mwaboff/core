# Ancestry Cards API Reference

Base URL: `http://localhost:8080`

## Overview

Ancestry cards represent the racial or species heritage of a Daggerheart character (e.g., Elf, Dwarf, Ribbet). They have no additional fields beyond the base card properties -- just a name, description, expansion, features, and cost tags.

**Base path:** `/api/dh/cards/ancestry`

**Authentication:** All endpoints require a valid JWT in an `AUTH_TOKEN` HttpOnly cookie.

**Authorization:**
- `GET` endpoints: Any authenticated user
- `POST`, `PUT`, `DELETE` endpoints: `ADMIN` or `OWNER` role required

---

## Endpoints

### 1. List Ancestry Cards

```
GET /api/dh/cards/ancestry
```

Returns a paginated, filtered list of ancestry cards.

**Query Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Zero-based page number |
| `size` | integer | `20` | Items per page (max: 100) |
| `includeDeleted` | boolean | `false` | Include soft-deleted cards (ADMIN+ only) |
| `expansionId` | long | -- | Filter by expansion ID |
| `isOfficial` | boolean | -- | Filter by official status |
| `expand` | string | -- | Comma-separated relationships to expand |

**Expand Options:** `expansion`, `features`, `costTags`

**Example Request:**

```
GET /api/dh/cards/ancestry?isOfficial=true&expand=expansion
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Elf",
      "description": "Elven ancestry",
      "cardType": "ANCESTRY",
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook",
        "isPublished": true,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "isOfficial": true,
      "featureIds": [1, 2],
      "costTagIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    },
    {
      "id": 2,
      "name": "Dwarf",
      "description": "Dwarven ancestry",
      "cardType": "ANCESTRY",
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook",
        "isPublished": true,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "isOfficial": true,
      "featureIds": [],
      "costTagIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Pagination Example:**

```
GET /api/dh/cards/ancestry?page=1&size=2
```

Returns page 1 (zero-indexed) with 2 items per page.

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT

---

### 2. Get Ancestry Card by ID

```
GET /api/dh/cards/ancestry/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The ancestry card ID |

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `expand` | string | Comma-separated relationships to expand |

**Example Request:**

```
GET /api/dh/cards/ancestry/1?expand=features
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Elf",
  "description": "Elven ancestry",
  "cardType": "ANCESTRY",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [1, 2],
  "features": [
    {
      "id": 1,
      "name": "Mighty Leap",
      "description": "Jump great distances",
      "featureType": "ANCESTRY",
      "expansionId": 1,
      "costTagIds": [],
      "modifierIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    },
    {
      "id": 2,
      "name": "Amphibious",
      "description": "Breathe underwater",
      "featureType": "ANCESTRY",
      "expansionId": 1,
      "costTagIds": [],
      "modifierIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "costTagIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `404 Not Found` -- Ancestry card not found with given ID

---

### 3. Create Ancestry Card

```
POST /api/dh/cards/ancestry
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body:**

```json
{
  "name": "Ribbet Ancestry",
  "description": "Frog-like humanoids",
  "expansionId": 1,
  "isOfficial": true,
  "backgroundImageUrl": "https://img.url/ribbet",
  "featureIds": [10],
  "features": [
    {
      "name": "Mighty Leap",
      "description": "Jump great distances",
      "featureType": "ANCESTRY",
      "expansionId": 1
    },
    {
      "name": "Amphibious",
      "description": "Breathe underwater",
      "featureType": "ANCESTRY",
      "expansionId": 1
    }
  ],
  "costTagIds": [],
  "costTags": [
    { "label": "1/session", "category": "LIMITATION" }
  ]
}
```

**Request Body Fields:**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `name` | string | Yes | Not blank, max 200 chars | Card name |
| `description` | string | No | -- | Card description |
| `expansionId` | long | Yes | Not null | Expansion this card belongs to |
| `isOfficial` | boolean | Yes | Not null | Whether this is official game content |
| `backgroundImageUrl` | string | No | Max 500 chars | URL to card background image |
| `featureIds` | long[] | No | -- | IDs of existing features to associate |
| `features` | FeatureInput[] | No | Valid | Inline feature find-or-create objects (merged with featureIds) |
| `costTagIds` | long[] | No | -- | IDs of existing cost tags |
| `costTags` | CostTagInput[] | No | Valid | Inline cost tag find-or-create objects (merged with costTagIds) |

**Feature Deduplication:** When using inline `features`, the system matches case-insensitively by name, expansion, and type. If a matching feature already exists, it is reused rather than duplicated. Two cards can share the same feature.

**Example Response (201 Created):**

```json
{
  "id": 3,
  "name": "Ribbet Ancestry",
  "description": "Frog-like humanoids",
  "cardType": "ANCESTRY",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [10, 11, 12],
  "costTagIds": [5],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure (e.g., missing `name`, missing `featureType` on inline feature, missing `expansionId` on inline feature)
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Referenced expansion or feature not found

---

### 4. Create Ancestry Cards (Bulk)

```
POST /api/dh/cards/ancestry/bulk
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body:** Array of `CreateAncestryCardRequest` objects.

**Example Request:**

```json
[
  {
    "name": "Elf",
    "description": "Elven ancestry",
    "expansionId": 1,
    "isOfficial": true,
    "features": [
      {
        "name": "Bulk Feature",
        "description": "Feature from bulk",
        "featureType": "ANCESTRY",
        "expansionId": 1
      }
    ]
  },
  {
    "name": "Dwarf",
    "description": "Dwarven ancestry",
    "expansionId": 1,
    "isOfficial": true
  }
]
```

**Example Response (201 Created):**

```json
[
  {
    "id": 1,
    "name": "Elf",
    "cardType": "ANCESTRY",
    "expansionId": 1,
    "isOfficial": true,
    "featureIds": [1],
    "costTagIds": [],
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  },
  {
    "id": 2,
    "name": "Dwarf",
    "cardType": "ANCESTRY",
    "expansionId": 1,
    "isOfficial": true,
    "featureIds": [],
    "costTagIds": [],
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  }
]
```

**Error Responses:**
- `400 Bad Request` -- Validation failure
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role

---

### 5. Update Ancestry Card

```
PUT /api/dh/cards/ancestry/{id}
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The ancestry card ID to update |

**Request Body:** Same schema as `CreateAncestryCardRequest`.

**Example Request:**

```json
{
  "name": "High Elf",
  "description": "Updated description",
  "expansionId": 1,
  "isOfficial": false,
  "features": [
    {
      "name": "Update Feature",
      "description": "Added via update",
      "featureType": "ANCESTRY",
      "expansionId": 1
    }
  ]
}
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "High Elf",
  "description": "Updated description",
  "cardType": "ANCESTRY",
  "expansionId": 1,
  "isOfficial": false,
  "featureIds": [5],
  "costTagIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Ancestry card, expansion, or feature not found

---

### 6. Delete Ancestry Card

```
DELETE /api/dh/cards/ancestry/{id}
```

Performs a soft delete (sets `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Ancestry card not found

---

### 7. Restore Ancestry Card

```
POST /api/dh/cards/ancestry/{id}/restore
```

Restores a soft-deleted ancestry card (clears `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Elf",
  "description": "Elven ancestry",
  "cardType": "ANCESTRY",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [],
  "costTagIds": [],
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:10:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Ancestry card not found

---

## Enums

### CardType

| Value |
|---|
| `ANCESTRY` |
| `COMMUNITY` |
| `SUBCLASS` |
| `DOMAIN` |

### FeatureType

| Value |
|---|
| `HOPE` |
| `ANCESTRY` |
| `CLASS` |
| `COMMUNITY` |
| `DOMAIN` |
| `ITEM` |
| `OTHER` |
| `SUBCLASS` |

### CostTagCategory

| Value | Description |
|---|---|
| `COST` | Resource expenditure (e.g., "3 Hope", "1 Stress") |
| `LIMITATION` | Restriction or requirement (e.g., "Close range") |
| `TIMING` | Frequency or action type (e.g., "1/session", "Action") |

### ModifierTarget

| Value | Description |
|---|---|
| `AGILITY` | Modifies the character's Agility trait score |
| `STRENGTH` | Modifies the character's Strength trait score |
| `FINESSE` | Modifies the character's Finesse trait score |
| `INSTINCT` | Modifies the character's Instinct trait score |
| `PRESENCE` | Modifies the character's Presence trait score |
| `KNOWLEDGE` | Modifies the character's Knowledge trait score |
| `EVASION` | Modifies the character's Evasion defense value |
| `MAJOR_DAMAGE_THRESHOLD` | Modifies the character's Major damage threshold |
| `SEVERE_DAMAGE_THRESHOLD` | Modifies the character's Severe damage threshold |
| `HIT_POINT_MAX` | Modifies the character's maximum Hit Points |
| `STRESS_MAX` | Modifies the character's maximum Stress capacity |
| `HOPE_MAX` | Modifies the character's maximum Hope |
| `ARMOR_MAX` | Modifies the character's maximum Armor slots |
| `GOLD` | Modifies the character's starting Gold |
| `ATTACK_ROLL` | Modifies the character's attack roll result |
| `DAMAGE_ROLL` | Modifies the character's damage roll result |
| `PRIMARY_DAMAGE_ROLL` | Modifies the character's primary damage roll result |
| `ARMOR_SCORE` | Modifies the character's armor score |

### ModifierOperation

| Value | Description |
|---|---|
| `ADD` | Adds the value to the target attribute |
| `SET` | Sets the target attribute to the specified value |
| `MULTIPLY` | Multiplies the target attribute by the specified value |

---

## Nested DTOs

### FeatureInput (inline find-or-create)

Used in the `features` array on create/update requests. Matches existing features case-insensitively by name within the same expansion and type; creates new if no match.

```json
{
  "name": "Mighty Leap",
  "description": "Jump great distances",
  "featureType": "ANCESTRY",
  "expansionId": 1,
  "costTagIds": [],
  "costTags": [
    { "label": "1/session", "category": "LIMITATION" }
  ],
  "modifierIds": [],
  "modifiers": [
    { "target": "EVASION", "operation": "ADD", "value": 1 }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | No | Max 200 chars. Feature name |
| `description` | string | No | Feature description |
| `featureType` | string | Yes | One of the FeatureType enum values |
| `expansionId` | long | Yes | Expansion this feature belongs to |
| `costTagIds` | long[] | No | IDs of existing cost tags |
| `costTags` | CostTagInput[] | No | Inline cost tags to find or create |
| `modifierIds` | long[] | No | IDs of existing modifiers |
| `modifiers` | FeatureModifierInput[] | No | Inline modifiers to find or create |

### CostTagInput (inline find-or-create)

```json
{ "label": "1/session", "category": "LIMITATION" }
```

| Field | Type | Required | Description |
|---|---|---|---|
| `label` | string | Yes | Not blank, max 200 chars. Matched case-insensitively |
| `category` | string | Yes | `COST`, `LIMITATION`, or `TIMING` |

### FeatureModifierInput (inline find-or-create)

```json
{ "target": "EVASION", "operation": "ADD", "value": 1 }
```

| Field | Type | Required | Description |
|---|---|---|---|
| `target` | string | Yes | One of the ModifierTarget enum values |
| `operation` | string | Yes | `ADD`, `SET`, or `MULTIPLY` |
| `value` | integer | Yes | Numeric modifier value |

---

## Expand Support

The `expand` query parameter controls which relationships are returned as full objects vs. ID-only references.

| Expand Value | Effect |
|---|---|
| `expansion` | Includes full `ExpansionResponse` object |
| `features` | Includes full `FeatureResponse` objects in `features` array |
| `costTags` | Includes full `CardCostTagResponse` objects; also propagates to nested feature cost tags |

Without expansion, only IDs are returned: `expansionId`, `featureIds`, `costTagIds`.

---

## Response DTO: AncestryCardResponse

| Field | Type | Always Present | Description |
|---|---|---|---|
| `id` | long | Yes | Unique identifier |
| `name` | string | Yes | Card name |
| `description` | string | Yes | Card description |
| `cardType` | string | Yes | Always `"ANCESTRY"` |
| `expansionId` | long | Yes | Expansion ID |
| `expansion` | object | Only with `?expand=expansion` | Full ExpansionResponse |
| `isOfficial` | boolean | Yes | Official game content flag |
| `backgroundImageUrl` | string | If set | Background image URL |
| `featureIds` | long[] | Yes | Feature IDs |
| `features` | object[] | Only with `?expand=features` | Full FeatureResponse objects |
| `costTagIds` | long[] | Yes | Cost tag IDs |
| `costTags` | object[] | Only with `?expand=costTags` | Full CardCostTagResponse objects |
| `createdAt` | datetime | Yes | Creation timestamp |
| `lastModifiedAt` | datetime | Yes | Last modification timestamp |
| `deletedAt` | datetime | If deleted | Soft-deletion timestamp |

---

## Database Schema

### cards (base table -- JOINED inheritance)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `name` | VARCHAR(200) | NOT NULL |
| `description` | TEXT | -- |
| `card_type` | VARCHAR(20) | NOT NULL, CHECK IN ('ANCESTRY', 'COMMUNITY', 'SUBCLASS', 'DOMAIN') |
| `expansion_id` | BIGINT | NOT NULL, FK -> expansions(id) |
| `is_official` | BOOLEAN | NOT NULL, DEFAULT true |
| `background_image_url` | VARCHAR(500) | -- |
| `created_at` | TIMESTAMP | NOT NULL |
| `last_modified_at` | TIMESTAMP | NOT NULL |
| `deleted_at` | TIMESTAMP | -- |

### ancestry_cards (subtype table)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, FK -> cards(id) CASCADE |

No additional columns beyond the base card.

### card_features (join table)

| Column | Type | Constraints |
|---|---|---|
| `card_id` | BIGINT | FK -> cards(id) CASCADE |
| `feature_id` | BIGINT | FK -> features(id) CASCADE |

### card_card_cost_tags (join table)

| Column | Type | Constraints |
|---|---|---|
| `card_id` | BIGINT | FK -> cards(id) CASCADE |
| `card_cost_tag_id` | BIGINT | FK -> card_cost_tags(id) CASCADE |
