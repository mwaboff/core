# Subclass Cards API Reference

Base URL: `http://localhost:8080`

## Overview

Subclass cards represent specializations within a Daggerheart character class. Each subclass card belongs to a subclass path (e.g., "Warden of Renewal" under the Warrior class) and has a level tier (Foundation, Specialization, or Mastery). Subclass paths group related cards and carry domain associations and an optional spellcasting trait.

**Base path:** `/api/dh/cards/subclass`

**Authentication:** All endpoints require a valid JWT in an `AUTH_TOKEN` HttpOnly cookie.

**Authorization:**
- `GET` endpoints: Any authenticated user
- `POST`, `PUT`, `DELETE` endpoints: `ADMIN` or `OWNER` role required

---

## Endpoints

### 1. List Subclass Cards

```
GET /api/dh/cards/subclass
```

Returns a paginated, filtered list of subclass cards.

**Query Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Zero-based page number |
| `size` | integer | `20` | Items per page (max: 100) |
| `includeDeleted` | boolean | `false` | Include soft-deleted cards (ADMIN+ only) |
| `expansionId` | long | -- | Filter by expansion ID |
| `isOfficial` | boolean | -- | Filter by official status |
| `associatedClassId` | long | -- | Filter by associated class ID (via subclass path) |
| `subclassPathId` | long | -- | Filter by subclass path ID |
| `level` | string | -- | Filter by subclass level. Values: `FOUNDATION`, `SPECIALIZATION`, `MASTERY` |
| `expand` | string | -- | Comma-separated relationships to expand |

**Expand Options:** `expansion`, `features`, `subclassPath`, `costTags`

**Example Request:**

```
GET /api/dh/cards/subclass?associatedClassId=1&level=FOUNDATION&expand=expansion,subclassPath
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Berserker",
      "description": "Berserker path",
      "cardType": "SUBCLASS",
      "expansionId": 1,
      "expansionName": "Core Rulebook",
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
      "associatedClassId": 1,
      "associatedClassName": "Warrior",
      "subclassPathId": 1,
      "subclassPathName": "Warden of Renewal",
      "subclassPath": {
        "id": 1,
        "name": "Warden of Renewal",
        "associatedClassId": 1,
        "associatedDomainIds": [2, 3],
        "expansionId": 1,
        "createdAt": "2026-03-13T10:00:00",
        "lastModifiedAt": "2026-03-13T10:00:00"
      },
      "domainNames": ["Blade", "Bone"],
      "domainIds": [1, 2],
      "spellcastingTrait": {
        "trait": "INSTINCT",
        "description": "Intuition, awareness, and natural understanding",
        "examples": "Perception, survival, animal handling, reading situations"
      },
      "level": "FOUNDATION",
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

### 2. Get Subclass Card by ID

```
GET /api/dh/cards/subclass/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The subclass card ID |

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `expand` | string | Comma-separated relationships to expand |

**Example Request:**

```
GET /api/dh/cards/subclass/1?expand=expansion,subclassPath
Cookie: AUTH_TOKEN=<jwt>
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Berserker",
  "description": "Berserker path",
  "cardType": "SUBCLASS",
  "expansionId": 1,
  "expansionName": "Core Rulebook",
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
  "associatedClassId": 1,
  "associatedClassName": "Warrior",
  "subclassPathId": 1,
  "subclassPathName": "Warden of Renewal",
  "subclassPath": {
    "id": 1,
    "name": "Warden of Renewal",
    "associatedClassId": 1,
    "associatedDomainIds": [],
    "expansionId": 1,
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  },
  "domainNames": [],
  "domainIds": [],
  "level": "FOUNDATION",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `404 Not Found` -- Subclass card not found with given ID

---

### 3. Create Subclass Card

```
POST /api/dh/cards/subclass
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body (using existing subclass path ID):**

```json
{
  "name": "Berserker",
  "description": "Berserker path",
  "expansionId": 1,
  "isOfficial": true,
  "subclassPathId": 1,
  "level": "FOUNDATION",
  "featureIds": [5],
  "features": [
    {
      "name": "Rage",
      "description": "Enter a berserker rage",
      "featureType": "SUBCLASS",
      "expansionId": 1
    }
  ],
  "costTagIds": [],
  "costTags": []
}
```

**Request Body (using inline subclass path find-or-create):**

```json
{
  "name": "Berserker",
  "description": "Berserker path",
  "expansionId": 1,
  "isOfficial": true,
  "associatedClassId": 1,
  "subclassPath": {
    "name": "Warden of Renewal",
    "associatedDomainIds": [2, 3],
    "spellcastingTrait": "INSTINCT"
  },
  "level": "FOUNDATION"
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
| `subclassPathId` | long | Conditional | -- | ID of existing subclass path. Either this or `subclassPath` + `associatedClassId` must be provided |
| `subclassPath` | SubclassPathInput | Conditional | Valid | Inline subclass path find-or-create. Requires `associatedClassId` |
| `associatedClassId` | long | Conditional | -- | Class ID for path resolution when using inline `subclassPath` |
| `level` | string | Yes | Not null | Subclass level: `FOUNDATION`, `SPECIALIZATION`, or `MASTERY` |

**Example Response (201 Created):**

```json
{
  "id": 1,
  "name": "Berserker",
  "description": "Berserker path",
  "cardType": "SUBCLASS",
  "expansionId": 1,
  "expansionName": "Core Rulebook",
  "isOfficial": true,
  "featureIds": [5, 10],
  "costTagIds": [],
  "associatedClassId": 1,
  "associatedClassName": "Warrior",
  "subclassPathId": 1,
  "subclassPathName": "Warden of Renewal",
  "domainNames": ["Blade", "Bone"],
  "level": "FOUNDATION",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure (e.g., missing `level`)
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Referenced expansion, subclass path, or class not found

---

### 4. Create Subclass Cards (Bulk)

```
POST /api/dh/cards/subclass/bulk
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Request Body:** Array of `CreateSubclassCardRequest` objects.

**Example Request:**

```json
[
  {
    "name": "Berserker",
    "description": "Berserker path",
    "expansionId": 1,
    "isOfficial": true,
    "subclassPathId": 1,
    "level": "FOUNDATION"
  },
  {
    "name": "Paladin",
    "description": "Paladin path",
    "expansionId": 1,
    "isOfficial": true,
    "subclassPathId": 1,
    "level": "SPECIALIZATION"
  }
]
```

**Example Response (201 Created):**

```json
[
  {
    "id": 1,
    "name": "Berserker",
    "cardType": "SUBCLASS",
    "expansionId": 1,
    "expansionName": "Core Rulebook",
    "isOfficial": true,
    "featureIds": [],
    "costTagIds": [],
    "associatedClassId": 1,
    "associatedClassName": "Warrior",
    "subclassPathId": 1,
    "subclassPathName": "Warden of Renewal",
    "domainNames": [],
    "domainIds": [],
    "level": "FOUNDATION",
    "createdAt": "2026-03-13T10:00:00",
    "lastModifiedAt": "2026-03-13T10:00:00"
  },
  {
    "id": 2,
    "name": "Paladin",
    "cardType": "SUBCLASS",
    "expansionId": 1,
    "expansionName": "Core Rulebook",
    "isOfficial": true,
    "featureIds": [],
    "costTagIds": [],
    "associatedClassId": 1,
    "associatedClassName": "Warrior",
    "subclassPathId": 1,
    "subclassPathName": "Warden of Renewal",
    "domainNames": [],
    "domainIds": [],
    "level": "SPECIALIZATION",
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

### 5. Update Subclass Card

```
PUT /api/dh/cards/subclass/{id}
```

**Authorization:** `ADMIN` or `OWNER` role required.

**Path Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `id` | long | The subclass card ID to update |

**Request Body:** Same schema as `CreateSubclassCardRequest`.

**Example Request:**

```json
{
  "name": "Warlord",
  "description": "Updated description",
  "expansionId": 1,
  "isOfficial": true,
  "subclassPathId": 1,
  "level": "SPECIALIZATION"
}
```

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Warlord",
  "description": "Updated description",
  "cardType": "SUBCLASS",
  "expansionId": 1,
  "expansionName": "Core Rulebook",
  "isOfficial": true,
  "featureIds": [],
  "costTagIds": [],
  "associatedClassId": 1,
  "associatedClassName": "Warrior",
  "subclassPathId": 1,
  "subclassPathName": "Warden of Renewal",
  "domainNames": [],
  "domainIds": [],
  "level": "SPECIALIZATION",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:05:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Validation failure
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Subclass card, expansion, or subclass path not found

---

### 6. Delete Subclass Card

```
DELETE /api/dh/cards/subclass/{id}
```

Performs a soft delete (sets `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Subclass card not found

---

### 7. Restore Subclass Card

```
POST /api/dh/cards/subclass/{id}/restore
```

Restores a soft-deleted subclass card (clears `deletedAt` timestamp).

**Authorization:** `ADMIN` or `OWNER` role required.

**Example Response (200 OK):**

```json
{
  "id": 1,
  "name": "Berserker",
  "description": "Berserker path",
  "cardType": "SUBCLASS",
  "expansionId": 1,
  "expansionName": "Core Rulebook",
  "isOfficial": true,
  "featureIds": [],
  "costTagIds": [],
  "associatedClassId": 1,
  "associatedClassName": "Warrior",
  "subclassPathId": 1,
  "subclassPathName": "Warden of Renewal",
  "domainNames": [],
  "domainIds": [],
  "level": "FOUNDATION",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:10:00"
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT
- `403 Forbidden` -- User does not have ADMIN/OWNER role
- `404 Not Found` -- Subclass card not found

---

## Enums

### SubclassLevel

| Value | Description |
|---|---|
| `FOUNDATION` | Foundation level subclass (starting level) |
| `SPECIALIZATION` | Specialization level subclass (mid-tier) |
| `MASTERY` | Mastery level subclass (highest tier) |

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

### Trait

| Value | Description | Examples |
|---|---|---|
| `AGILITY` | Quick reflexes, nimbleness, and coordination | Dodging attacks, acrobatics, sleight of hand, stealth |
| `STRENGTH` | Raw physical power and endurance | Melee attacks, athletics, breaking objects, carrying heavy loads |
| `FINESSE` | Precision, grace, and careful execution | Ranged attacks, lockpicking, crafting, precise movements |
| `INSTINCT` | Intuition, awareness, and natural understanding | Perception, survival, animal handling, reading situations |
| `PRESENCE` | Force of personality and social influence | Persuasion, intimidation, performance, leadership |
| `KNOWLEDGE` | Learning, reasoning, and mental acuity | Spellcasting, history, investigation, arcana |

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

### SubclassPathInput (inline find-or-create)

Used in the `subclassPath` field on create/update requests. Looks up existing path by name and class; creates new if no match.

```json
{
  "name": "Warden of Renewal",
  "associatedDomainIds": [2, 3],
  "spellcastingTrait": "INSTINCT"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Not blank, max 200 chars. Path name |
| `associatedDomainIds` | long[] | No | Domain IDs to associate (used only when creating new path) |
| `spellcastingTrait` | string | No | One of the Trait enum values (used only when creating new path) |

### FeatureInput (inline find-or-create)

Used in the `features` array on create/update requests.

```json
{
  "name": "Rage",
  "description": "Enter a berserker rage",
  "featureType": "SUBCLASS",
  "expansionId": 1,
  "costTagIds": [],
  "costTags": [],
  "modifierIds": [],
  "modifiers": [
    { "target": "STRENGTH", "operation": "ADD", "value": 2 }
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
{ "target": "STRENGTH", "operation": "ADD", "value": 2 }
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
| `subclassPath` | Includes full `SubclassPathResponse` object |
| `costTags` | Includes full `CardCostTagResponse` objects; also propagates to nested feature cost tags |

Without expansion, the response still includes summary fields: `expansionName`, `associatedClassId`, `associatedClassName`, `subclassPathId`, `subclassPathName`, `domainNames`, `domainIds`, `spellcastingTrait`.

---

## Response DTO: SubclassCardResponse

| Field | Type | Always Present | Description |
|---|---|---|---|
| `id` | long | Yes | Unique identifier |
| `name` | string | Yes | Card name |
| `description` | string | Yes | Card description |
| `cardType` | string | Yes | Always `"SUBCLASS"` |
| `expansionId` | long | Yes | Expansion ID |
| `expansionName` | string | Yes | Expansion name |
| `expansion` | object | Only with `?expand=expansion` | Full ExpansionResponse |
| `isOfficial` | boolean | Yes | Official game content flag |
| `backgroundImageUrl` | string | If set | Background image URL |
| `featureIds` | long[] | Yes | Feature IDs |
| `features` | object[] | Only with `?expand=features` | Full FeatureResponse objects |
| `costTagIds` | long[] | Yes | Cost tag IDs |
| `costTags` | object[] | Only with `?expand=costTags` | Full CardCostTagResponse objects |
| `associatedClassId` | long | Yes | Class ID (via subclass path) |
| `associatedClassName` | string | Yes | Class name (via subclass path) |
| `subclassPathId` | long | Yes | Subclass path ID |
| `subclassPathName` | string | Yes | Subclass path name |
| `subclassPath` | object | Only with `?expand=subclassPath` | Full SubclassPathResponse |
| `domainNames` | string[] | Yes | Domain names from path |
| `domainIds` | long[] | Yes | Domain IDs from path |
| `spellcastingTrait` | object | If path has spellcasting | TraitInfo with `trait`, `description`, `examples` |
| `level` | string | Yes | Subclass level enum value |
| `createdAt` | datetime | Yes | Creation timestamp |
| `lastModifiedAt` | datetime | Yes | Last modification timestamp |
| `deletedAt` | datetime | If deleted | Soft-deletion timestamp |

### SubclassPathResponse (expanded)

| Field | Type | Always Present | Description |
|---|---|---|---|
| `id` | long | Yes | Unique identifier |
| `name` | string | Yes | Path name |
| `associatedClassId` | long | Yes | Class ID |
| `associatedClass` | object | Only with nested expand | Full ClassResponse |
| `spellcastingTrait` | object | If set | TraitInfo with `trait`, `description`, `examples` |
| `associatedDomainIds` | long[] | Yes | Domain IDs |
| `associatedDomains` | object[] | Only with nested expand | Full DomainResponse objects |
| `expansionId` | long | Yes | Expansion ID |
| `expansion` | object | Only with nested expand | Full ExpansionResponse |
| `createdAt` | datetime | Yes | Creation timestamp |
| `lastModifiedAt` | datetime | Yes | Last modification timestamp |
| `deletedAt` | datetime | If deleted | Soft-deletion timestamp |

### TraitInfo (nested in SubclassPathResponse and SubclassCardResponse)

```json
{
  "trait": "INSTINCT",
  "description": "Intuition, awareness, and natural understanding",
  "examples": "Perception, survival, animal handling, reading situations"
}
```

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

### subclass_cards (subtype table)

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, FK -> cards(id) CASCADE |
| `subclass_path_id` | BIGINT | NOT NULL, FK -> subclass_paths(id) |
| `level` | VARCHAR(20) | NOT NULL, CHECK IN ('FOUNDATION', 'SPECIALIZATION', 'MASTERY') |

### subclass_paths

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `name` | VARCHAR(200) | NOT NULL |
| `associated_class_id` | BIGINT | NOT NULL, FK -> classes(id) |
| `spellcasting_trait` | VARCHAR(20) | -- |
| `expansion_id` | BIGINT | NOT NULL, FK -> expansions(id) |
| `created_at` | TIMESTAMP | NOT NULL |
| `last_modified_at` | TIMESTAMP | NOT NULL |
| `deleted_at` | TIMESTAMP | -- |

Unique index: `LOWER(name), associated_class_id`

### subclass_path_domains (join table)

| Column | Type | Constraints |
|---|---|---|
| `subclass_path_id` | BIGINT | FK -> subclass_paths(id) |
| `domain_id` | BIGINT | FK -> domains(id) |

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
