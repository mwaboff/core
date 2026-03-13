# Domain Cards API Reference

Base URL: `http://localhost:8080`

## Overview

Domain cards represent magical abilities, spells, grimoires, transformations, and wild powers associated with specific Daggerheart domains. Each domain card has a type, level requirement, recall cost, and belongs to a domain (e.g., Fire, Ice).

**Base path:** `/api/dh/cards/domain`

**Authentication:** All endpoints require a valid JWT in an `AUTH_TOKEN` HttpOnly cookie.

**Authorization:**
- `GET` endpoints: Any authenticated user
- `POST`, `PUT`, `DELETE` endpoints: `ADMIN` or `OWNER` role required

---

## Endpoints

### 1. List Domain Cards

```
GET /api/dh/cards/domain
```

Returns a paginated, filtered list of domain cards. Results are sorted by level ascending, then by name alphabetically.

**Query Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Zero-based page number |
| `size` | integer | `20` | Items per page (max: 100) |
| `includeDeleted` | boolean | `false` | Include soft-deleted cards (ADMIN+ only) |
| `expansionId` | long | -- | Filter by expansion ID |
| `isOfficial` | boolean | -- | Filter by official status |
| `associatedDomainIds` | long[] | -- | Filter by associated domain IDs |
| `type` | string | -- | Filter by domain card type. Values: `SPELL`, `GRIMOIRE`, `ABILITY`, `TRANSFORMATION`, `WILD` |
| `levels` | integer[] | -- | Filter by level(s) |
| `expand` | string | -- | Comma-separated relationships to expand |

**Expand Options:** `expansion`, `features`, `associatedDomain`, `costTags`

**Example Request:**

```
GET /api/dh/cards/domain?type=SPELL&levels=3&expand=expansion,associatedDomain
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Fireball",
      "description": "Fire spell",
      "cardType": "DOMAIN",
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook",
        "isPublished": true,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "isOfficial": true,
      "featureIds": [5, 12],
      "costTagIds": [3],
      "associatedDomainId": 2,
      "associatedDomain": {
        "id": 2,
        "name": "Fire",
        "description": "Fire domain",
        "expansionId": 1,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "level": 3,
      "recallCost": 2,
      "type": "SPELL",
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT

---

### 2. Get Domain Card by ID

```
GET /api/dh/cards/domain/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The domain card ID |

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `expand` | string | Comma-separated relationships to expand |

**Example Request:**

```
GET /api/dh/cards/domain/1?expand=features
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Fireball",
  "description": "Fire spell",
  "cardType": "DOMAIN",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [5],
  "features": [
    {
      "id": 5,
      "name": "Blaze",
      "description": "Ignite enemies in a burst of flame",
      "featureType": "DOMAIN",
      "expansionId": 1,
      "costTagIds": [3],
      "modifierIds": [],
      "createdAt": "2026-03-13T10:00:00",
      "lastModifiedAt": "2026-03-13T10:00:00"
    }
  ],
  "costTagIds": [],
  "associatedDomainId": 2,
  "level": 1,
  "recallCost": 1,
  "type": "SPELL",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `404 Not Found` -- Domain card not found with given ID

---

### 3. Create Domain Card

```
POST /api/dh/cards/domain
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body:**

```json
{
  "name": "Fireball",
  "description": "Fire spell",
  "expansionId": 1,
  "isOfficial": true,
  "backgroundImageUrl": "https://img.url/fireball",
  "featureIds": [5],
  "features": [
    {
      "name": "Blaze",
      "description": "Ignite enemies in a burst of flame",
      "featureType": "DOMAIN",
      "expansionId": 1,
      "costTags": [
        { "label": "3 Hope", "category": "COST" }
      ],
      "modifiers": [
        { "target": "DAMAGE_ROLL", "operation": "ADD", "value": 2 }
      ]
    }
  ],
  "costTagIds": [3],
  "costTags": [
    { "label": "1/session", "category": "TIMING" }
  ],
  "associatedDomainId": 2,
  "level": 1,
  "recallCost": 1,
  "type": "SPELL"
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
| `featureIds` | long[] | No | -- | IDs of existing features |
| `features` | FeatureInput[] | No | Valid | Inline feature find-or-create objects (merged with featureIds) |
| `costTagIds` | long[] | No | -- | IDs of existing cost tags |
| `costTags` | CostTagInput[] | No | Valid | Inline cost tag find-or-create objects (merged with costTagIds) |
| `associatedDomainId` | long | Yes | Not null | ID of the associated domain |
| `level` | integer | Yes | Not null, positive | Level requirement |
| `recallCost` | integer | Yes | Not null, >= 0 | Cost to recall/use this card |
| `type` | string | Yes | Not null | Domain card type: `SPELL`, `GRIMOIRE`, `ABILITY`, `TRANSFORMATION`, `WILD` |

**Example Response (201 Created):**

```json
{
  "id": 1,
  "name": "Fireball",
  "description": "Fire spell",
  "cardType": "DOMAIN",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [5],
  "costTagIds": [3, 7],
  "associatedDomainId": 2,
  "level": 1,
  "recallCost": 1,
  "type": "SPELL",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure (e.g., missing required fields)
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Referenced expansion, domain, or feature not found

---

### 4. Create Domain Cards (Bulk)

```
POST /api/dh/cards/domain/bulk
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body:** Array of `CreateDomainCardRequest` objects (same schema as single create).

**Example Request:**

```json
[
  {
    "name": "Fireball",
    "description": "Fire spell",
    "expansionId": 1,
    "isOfficial": true,
    "associatedDomainId": 2,
    "level": 1,
    "recallCost": 1,
    "type": "SPELL"
  },
  {
    "name": "Flame Shield",
    "description": "Fire defense",
    "expansionId": 1,
    "isOfficial": true,
    "associatedDomainId": 2,
    "level": 2,
    "recallCost": 2,
    "type": "ABILITY"
  }
]
```

**Example Response (201 Created):**

```json
[
  {
    "id": 1,
    "name": "Fireball",
    "cardType": "DOMAIN",
    "expansionId": 1,
    "isOfficial": true,
    "featureIds": [],
    "costTagIds": [],
    "associatedDomainId": 2,
    "level": 1,
    "recallCost": 1,
    "type": "SPELL",
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  },
  {
    "id": 2,
    "name": "Flame Shield",
    "cardType": "DOMAIN",
    "expansionId": 1,
    "isOfficial": true,
    "featureIds": [],
    "costTagIds": [],
    "associatedDomainId": 2,
    "level": 2,
    "recallCost": 2,
    "type": "ABILITY",
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

### 5. Update Domain Card

```
PUT /api/dh/cards/domain/{id}
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The domain card ID to update |

**Request Body:** Same schema as `CreateDomainCardRequest` (all required fields must be provided).

**Example Request:**

```json
{
  "name": "Greater Fireball",
  "description": "Updated description",
  "expansionId": 1,
  "isOfficial": true,
  "associatedDomainId": 2,
  "level": 2,
  "recallCost": 3,
  "type": "GRIMOIRE"
}
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Greater Fireball",
  "description": "Updated description",
  "cardType": "DOMAIN",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [],
  "costTagIds": [],
  "associatedDomainId": 2,
  "level": 2,
  "recallCost": 3,
  "type": "GRIMOIRE",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Domain card, expansion, or domain not found

---

### 6. Delete Domain Card

```
DELETE /api/dh/cards/domain/{id}
```

Performs a soft delete (sets `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Domain card not found

---

### 7. Restore Domain Card

```
POST /api/dh/cards/domain/{id}/restore
```

Restores a soft-deleted domain card (clears `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Fireball",
  "description": "Fire spell",
  "cardType": "DOMAIN",
  "expansionId": 1,
  "isOfficial": true,
  "featureIds": [],
  "costTagIds": [],
  "associatedDomainId": 2,
  "level": 1,
  "recallCost": 1,
  "type": "SPELL",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:10:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Domain card not found

---

## Enums

### DomainCardType

| Value | Description |
|---|---|
| `SPELL` | Spell domain card |
| `GRIMOIRE` | Grimoire domain card |
| `ABILITY` | Ability domain card |
| `TRANSFORMATION` | Transformation domain card |
| `WILD` | Wild domain card |

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

Used in `features` array on create/update requests. Matches existing features case-insensitively by name within the same expansion and type; creates new if no match.

```json
{
  "name": "Blaze",
  "description": "Ignite enemies in a burst of flame",
  "featureType": "DOMAIN",
  "expansionId": 1,
  "costTagIds": [3],
  "costTags": [
    { "label": "3 Hope", "category": "COST" }
  ],
  "modifierIds": [10],
  "modifiers": [
    { "target": "DAMAGE_ROLL", "operation": "ADD", "value": 2 }
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
{ "label": "3 Hope", "category": "COST" }
```

| Field | Type | Required | Description |
|---|---|---|---|
| `label` | string | Yes | Not blank, max 200 chars. Matched case-insensitively |
| `category` | string | Yes | `COST`, `LIMITATION`, or `TIMING` |

### FeatureModifierInput (inline find-or-create)

```json
{ "target": "EVASION", "operation": "ADD", "value": -1 }
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
| `associatedDomain` | Includes full `DomainResponse` object |
| `costTags` | Includes full `CardCostTagResponse` objects; also propagates to nested feature cost tags |

Without expansion, only IDs are returned: `expansionId`, `featureIds`, `costTagIds`, `associatedDomainId`.

---

## Response DTO: DomainCardResponse

| Field | Type | Always Present | Description |
|---|---|---|---|
| `id` | long | Yes | Unique identifier |
| `name` | string | Yes | Card name |
| `description` | string | Yes | Card description |
| `cardType` | string | Yes | Always `"DOMAIN"` |
| `expansionId` | long | Yes | Expansion ID |
| `expansion` | object | Only with `?expand=expansion` | Full ExpansionResponse |
| `isOfficial` | boolean | Yes | Official game content flag |
| `backgroundImageUrl` | string | If set | Background image URL |
| `featureIds` | long[] | Yes | Feature IDs |
| `features` | object[] | Only with `?expand=features` | Full FeatureResponse objects |
| `costTagIds` | long[] | Yes | Cost tag IDs |
| `costTags` | object[] | Only with `?expand=costTags` | Full CardCostTagResponse objects |
| `associatedDomainId` | long | Yes | Domain ID |
| `associatedDomain` | object | Only with `?expand=associatedDomain` | Full DomainResponse |
| `level` | integer | Yes | Level requirement |
| `recallCost` | integer | Yes | Recall cost |
| `type` | string | Yes | Domain card type enum value |
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

### domain_cards (subtype table)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, FK -> cards(id) CASCADE |
| `associated_domain_id` | BIGINT | NOT NULL, FK -> domains(id) |
| `level` | INTEGER | NOT NULL |
| `recall_cost` | INTEGER | NOT NULL, CHECK >= 0 |
| `domain_card_type` | VARCHAR(20) | NOT NULL, CHECK IN ('SPELL', 'GRIMOIRE', 'ABILITY', 'TRANSFORMATION', 'WILD') |

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
