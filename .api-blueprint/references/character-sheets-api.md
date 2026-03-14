# Character Sheets API

Base path: `/api/dh/character-sheets`
Authentication: JWT via `AUTH_TOKEN` HttpOnly cookie (all endpoints require authentication)

---

## Endpoints

### List All Character Sheets

```
GET /api/dh/character-sheets
```

**Authorization:** MODERATOR, ADMIN, or OWNER role required. Regular USER role returns `403 Forbidden`.

**Query Parameters:**

| Parameter  | Type    | Required | Default | Description                                      |
|------------|---------|----------|---------|--------------------------------------------------|
| `page`     | integer | No       | `0`     | Zero-based page number                           |
| `size`     | integer | No       | `20`    | Items per page (max: 100, clamped server-side)   |
| `ownerId`  | long    | No       | --      | Filter by owner user ID                          |
| `name`     | string  | No       | --      | Filter by name (case-insensitive partial match)  |
| `minLevel` | integer | No       | --      | Filter by minimum character level (inclusive)     |
| `maxLevel` | integer | No       | --      | Filter by maximum character level (inclusive)     |
| `expand`   | string  | No       | --      | Comma-separated list of relationships to expand   |

**Response:** `200 OK` with `PagedResponse<CharacterSheetResponse>`

**Example Request:**

```bash
curl -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/character-sheets?page=0&size=2&name=Ara&minLevel=4&maxLevel=6&expand=owner"
```

**Example Response:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Aragorn",
      "pronouns": "he/him",
      "level": 5,
      "evasion": 10,
      "armorMax": 5,
      "armorMarked": 0,
      "majorDamageThreshold": 3,
      "severeDamageThreshold": 6,
      "agilityModifier": 0,
      "agilityMarked": false,
      "strengthModifier": 0,
      "strengthMarked": false,
      "finesseModifier": 0,
      "finesseMarked": false,
      "instinctModifier": 0,
      "instinctMarked": false,
      "presenceModifier": 0,
      "presenceMarked": false,
      "knowledgeModifier": 0,
      "knowledgeMarked": false,
      "hitPointMax": 10,
      "hitPointMarked": 0,
      "stressMax": 6,
      "stressMarked": 0,
      "hopeMax": 3,
      "hopeMarked": 0,
      "gold": 50,
      "ownerId": 1,
      "owner": {
        "id": 1,
        "username": "player1",
        "email": "player1@example.com"
      },
      "activePrimaryWeaponId": null,
      "activeSecondaryWeaponId": null,
      "activeArmorId": null,
      "communityCardIds": [],
      "ancestryCardIds": [],
      "subclassCardIds": [],
      "domainCardIds": [],
      "inventoryWeaponIds": [],
      "inventoryArmorIds": [],
      "inventoryItemIds": [],
      "experienceIds": [],
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 2
}
```

---

### Get Character Sheet by ID

```
GET /api/dh/character-sheets/{id}
```

**Authorization:** Any authenticated user.

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Query Parameters:**

| Parameter | Type   | Required | Description                                     |
|-----------|--------|----------|-------------------------------------------------|
| `expand`  | string | No       | Comma-separated list of relationships to expand  |

**Response:** `200 OK` with `CharacterSheetResponse`

**Example Request:**

```bash
curl -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/character-sheets/1?expand=owner,experiences"
```

**Example Response (with expand=owner,experiences):**

```json
{
  "id": 1,
  "name": "Aragorn",
  "pronouns": "he/him",
  "level": 5,
  "evasion": 10,
  "armorMax": 5,
  "armorMarked": 0,
  "majorDamageThreshold": 3,
  "severeDamageThreshold": 6,
  "agilityModifier": 0,
  "agilityMarked": false,
  "strengthModifier": 0,
  "strengthMarked": false,
  "finesseModifier": 0,
  "finesseMarked": false,
  "instinctModifier": 0,
  "instinctMarked": false,
  "presenceModifier": 0,
  "presenceMarked": false,
  "knowledgeModifier": 0,
  "knowledgeMarked": false,
  "hitPointMax": 10,
  "hitPointMarked": 0,
  "stressMax": 6,
  "stressMarked": 0,
  "hopeMax": 3,
  "hopeMarked": 0,
  "gold": 50,
  "ownerId": 1,
  "owner": {
    "id": 1,
    "username": "player1",
    "email": "player1@example.com",
    "avatarUrl": null,
    "timezone": null,
    "createdAt": "2026-03-13T12:00:00",
    "lastModifiedAt": "2026-03-13T12:00:00"
  },
  "activePrimaryWeaponId": null,
  "activeSecondaryWeaponId": null,
  "activeArmorId": null,
  "communityCardIds": [],
  "ancestryCardIds": [],
  "subclassCardIds": [],
  "inventoryWeaponIds": [],
  "inventoryArmorIds": [],
  "inventoryItemIds": [],
  "experienceIds": [10],
  "experiences": [
    {
      "id": 10,
      "characterSheetId": 1,
      "createdById": 1,
      "description": "Survived dragon attack",
      "modifier": 2,
      "createdAt": "2026-03-13T12:30:00",
      "lastModifiedAt": "2026-03-13T12:30:00"
    }
  ],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

---

### Create Character Sheet

```
POST /api/dh/character-sheets
```

**Authorization:** Any authenticated user. The authenticated user becomes the owner.

**Request Body:** `CreateCharacterSheetRequest` (JSON)

**Response:** `201 Created` with `CharacterSheetResponse`

**Example Request:**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Strider",
    "pronouns": "he/him",
    "level": 3,
    "evasion": 10,
    "armorMax": 5,
    "armorMarked": 0,
    "majorDamageThreshold": 3,
    "severeDamageThreshold": 6,
    "agilityModifier": 2,
    "agilityMarked": false,
    "strengthModifier": 3,
    "strengthMarked": false,
    "finesseModifier": 1,
    "finesseMarked": false,
    "instinctModifier": 2,
    "instinctMarked": false,
    "presenceModifier": 2,
    "presenceMarked": false,
    "knowledgeModifier": 0,
    "knowledgeMarked": false,
    "hitPointMax": 10,
    "hitPointMarked": 0,
    "stressMax": 6,
    "stressMarked": 0,
    "hopeMax": 3,
    "hopeMarked": 0,
    "gold": 50,
    "communityCardIds": [1],
    "ancestryCardIds": [2],
    "subclassCardIds": [3, 4],
    "domainCardIds": [8, 9],
    "inventoryWeaponIds": [5],
    "inventoryArmorIds": [6],
    "inventoryItemIds": [7]
  }' \
  "http://localhost:8080/api/dh/character-sheets"
```

**Example Response:**

```json
{
  "id": 2,
  "name": "Strider",
  "pronouns": "he/him",
  "level": 3,
  "evasion": 10,
  "armorMax": 5,
  "armorMarked": 0,
  "majorDamageThreshold": 3,
  "severeDamageThreshold": 6,
  "agilityModifier": 2,
  "agilityMarked": false,
  "strengthModifier": 3,
  "strengthMarked": false,
  "finesseModifier": 1,
  "finesseMarked": false,
  "instinctModifier": 2,
  "instinctMarked": false,
  "presenceModifier": 2,
  "presenceMarked": false,
  "knowledgeModifier": 0,
  "knowledgeMarked": false,
  "hitPointMax": 10,
  "hitPointMarked": 0,
  "stressMax": 6,
  "stressMarked": 0,
  "hopeMax": 3,
  "hopeMarked": 0,
  "gold": 50,
  "ownerId": 1,
  "activePrimaryWeaponId": null,
  "activeSecondaryWeaponId": null,
  "activeArmorId": null,
  "communityCardIds": [1],
  "ancestryCardIds": [2],
  "subclassCardIds": [3, 4],
  "inventoryWeaponIds": [5],
  "inventoryArmorIds": [6],
  "inventoryItemIds": [7],
  "experienceIds": [],
  "createdAt": "2026-03-13T14:00:00",
  "lastModifiedAt": "2026-03-13T14:00:00"
}
```

---

### Update Character Sheet

```
PUT /api/dh/character-sheets/{id}
```

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role.

Supports **partial updates** -- only non-null fields in the request body are updated. Omitted fields remain unchanged. Collection fields (card IDs, inventory IDs) replace the entire collection when provided; omit them to leave unchanged.

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Request Body:** `UpdateCharacterSheetRequest` (JSON, all fields optional)

**Response:** `200 OK` with `CharacterSheetResponse`

**Example Request (partial update):**

```bash
curl -X PUT -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Aragorn II",
    "level": 6,
    "evasion": 12,
    "agilityModifier": 3,
    "hitPointMax": 12,
    "gold": 100
  }' \
  "http://localhost:8080/api/dh/character-sheets/1"
```

**Example Response:**

```json
{
  "id": 1,
  "name": "Aragorn II",
  "pronouns": "he/him",
  "level": 6,
  "evasion": 12,
  "armorMax": 5,
  "armorMarked": 0,
  "majorDamageThreshold": 3,
  "severeDamageThreshold": 6,
  "agilityModifier": 3,
  "agilityMarked": false,
  "strengthModifier": 0,
  "strengthMarked": false,
  "finesseModifier": 0,
  "finesseMarked": false,
  "instinctModifier": 0,
  "instinctMarked": false,
  "presenceModifier": 0,
  "presenceMarked": false,
  "knowledgeModifier": 0,
  "knowledgeMarked": false,
  "hitPointMax": 12,
  "hitPointMarked": 0,
  "stressMax": 6,
  "stressMarked": 0,
  "hopeMax": 3,
  "hopeMarked": 0,
  "gold": 100,
  "ownerId": 1,
  "activePrimaryWeaponId": null,
  "activeSecondaryWeaponId": null,
  "activeArmorId": null,
  "communityCardIds": [],
  "ancestryCardIds": [],
  "subclassCardIds": [],
  "inventoryWeaponIds": [],
  "inventoryArmorIds": [],
  "inventoryItemIds": [],
  "experienceIds": [],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T15:00:00"
}
```

---

### Delete Character Sheet

```
DELETE /api/dh/character-sheets/{id}
```

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role.

Performs a **soft delete** -- sets `deletedAt` timestamp. The character sheet is excluded from active queries but remains in the database. Associated experiences are preserved (they remain in the database and reference the soft-deleted sheet).

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Response:** `204 No Content` (empty body)

**Example Request:**

```bash
curl -X DELETE -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/character-sheets/1"
```

---

## Expansion Support

The `expand` query parameter accepts a comma-separated list of relationship names. When a relationship is expanded, the full object is included in the response alongside the ID field (which is always present).

Expand options come in two categories: **item/card expansion** (top-level, brings related objects into the character sheet response) and **nested expansion** (applied within the already-expanded item or card objects).

### Item / Card Expand Options

| Value                  | Description                                  | Adds Field                  | Type                        |
|------------------------|----------------------------------------------|-----------------------------|-----------------------------|
| `owner`                | Full user profile of the character owner      | `owner`                     | `UserResponse`              |
| `experiences`          | All experiences for this character            | `experiences`               | `ExperienceResponse[]`      |
| `activePrimaryWeapon`  | Equipped primary weapon details              | `activePrimaryWeapon`       | `WeaponResponse`            |
| `activeSecondaryWeapon`| Equipped secondary weapon details            | `activeSecondaryWeapon`     | `WeaponResponse`            |
| `activeArmor`          | Equipped armor details                       | `activeArmor`               | `ArmorResponse`             |
| `communityCards`       | All assigned community cards                 | `communityCards`            | `CommunityCardResponse[]`   |
| `ancestryCards`        | All assigned ancestry cards                  | `ancestryCards`             | `AncestryCardResponse[]`    |
| `subclassCards`        | All assigned subclass cards                  | `subclassCards`             | `SubclassCardResponse[]`    |
| `domainCards`          | All assigned domain cards                    | `domainCards`               | `DomainCardResponse[]`      |
| `inventoryWeapons`     | All weapons in inventory                     | `inventoryWeapons`          | `WeaponResponse[]`          |
| `inventoryArmors`      | All armor pieces in inventory                | `inventoryArmors`           | `ArmorResponse[]`           |
| `inventoryItems`       | All loot items in inventory                  | `inventoryItems`            | `LootResponse[]`            |

### Nested Expand Options

These options apply **within** expanded weapons, armor, cards, and loot items. They have no effect unless at least one item/card expand option is also present.

| Value       | Description                                                                                   | Applies To                                                                    |
|-------------|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `features`  | Replaces `featureIds` arrays with full `FeatureResponse` objects on expanded items and cards  | Weapons, Armor, Loot, AncestryCard, CommunityCard, SubclassCard, DomainCard  |
| `costTags`  | Replaces `costTagIds` arrays with full `CardCostTagResponse` objects on expanded cards and features | Cards (directly), Features (when `features` is also expanded)          |
| `modifiers` | Replaces `modifierIds` arrays with full `FeatureModifierResponse` objects on expanded features | Features (requires `features` to also be expanded)                           |
| `expansion` | Replaces `expansionId` with a full `ExpansionResponse` object on expanded items, cards, and features | Weapons, Armor, Loot, Cards, Features (when `features` is also expanded) |

### Combining Expand Options

Nested options compose freely with any item/card expand options. Examples:

| Query String                                        | Effect                                                                              |
|-----------------------------------------------------|-------------------------------------------------------------------------------------|
| `?expand=activePrimaryWeapon,features`              | Returns the primary weapon with its features fully populated                        |
| `?expand=domainCards,features,costTags`             | Returns domain cards with full feature objects, each feature with full cost tags    |
| `?expand=inventoryWeapons,features,modifiers`       | Returns inventory weapons with full features, each feature with full modifiers      |
| `?expand=ancestryCards,features,expansion`          | Returns ancestry cards with full features, and each item/card's expansion populated |
| `?expand=domainCards,features,costTags,modifiers`   | Returns domain cards with features, their cost tags, and their modifiers all expanded |
| `?expand=owner,experiences,activePrimaryWeapon,inventoryWeapons` | Returns owner, experiences, and both weapon slots/inventory |

Null fields are omitted from JSON responses (uses `@JsonInclude(NON_NULL)`). Equipment expand fields (activePrimaryWeapon, activeSecondaryWeapon, activeArmor) are only included if the equipment slot is actually occupied.

---

## Error Responses

All error responses use a standard format:

### Standard Error

```json
{
  "status": 404,
  "error": "Entity Not Found",
  "message": "CharacterSheet not found with id: 99999",
  "path": "/api/dh/character-sheets/99999",
  "timestamp": "2026-03-13T12:00:00"
}
```

### Validation Error

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "name": "Character name is required",
    "evasion": "Evasion is required",
    "level": "Level is required"
  },
  "path": "/api/dh/character-sheets",
  "timestamp": "2026-03-13T12:00:00"
}
```

### Error Status Codes

| Status | Condition                                                              |
|--------|------------------------------------------------------------------------|
| `400`  | Validation failure (missing required fields, out-of-range values)      |
| `400`  | Constraint violation (e.g., armorMarked > armorMax)                    |
| `401`  | Missing or invalid authentication token                                |
| `403`  | Insufficient permissions (non-owner on update/delete, non-moderator on list all) |
| `404`  | Character sheet not found or soft-deleted                              |
| `404`  | Referenced entity not found (weapon, armor, card, loot ID)             |
| `500`  | Unexpected server error                                                |

---

## Models

### CreateCharacterSheetRequest

All fields marked **required** must be present. Equipment and collection IDs are optional.

| Field                    | Type      | Required | Validation                                    |
|--------------------------|-----------|----------|-----------------------------------------------|
| `name`                   | string    | Yes      | Not blank, max 200 chars                      |
| `pronouns`               | string    | No       | Max 100 chars                                 |
| `level`                  | integer   | Yes      | 1-10                                          |
| `evasion`                | integer   | Yes      | >= 0                                          |
| `armorMax`               | integer   | Yes      | >= 0                                          |
| `armorMarked`            | integer   | Yes      | >= 0, must be <= armorMax                     |
| `majorDamageThreshold`   | integer   | Yes      | > 0                                           |
| `severeDamageThreshold`  | integer   | Yes      | > 0, must be >= majorDamageThreshold          |
| `agilityModifier`        | integer   | Yes      | --                                            |
| `agilityMarked`          | boolean   | Yes      | --                                            |
| `strengthModifier`       | integer   | Yes      | --                                            |
| `strengthMarked`         | boolean   | Yes      | --                                            |
| `finesseModifier`        | integer   | Yes      | --                                            |
| `finesseMarked`          | boolean   | Yes      | --                                            |
| `instinctModifier`       | integer   | Yes      | --                                            |
| `instinctMarked`         | boolean   | Yes      | --                                            |
| `presenceModifier`       | integer   | Yes      | --                                            |
| `presenceMarked`         | boolean   | Yes      | --                                            |
| `knowledgeModifier`      | integer   | Yes      | --                                            |
| `knowledgeMarked`        | boolean   | Yes      | --                                            |
| `hitPointMax`            | integer   | Yes      | > 0                                           |
| `hitPointMarked`         | integer   | Yes      | >= 0, must be <= hitPointMax                  |
| `stressMax`              | integer   | Yes      | > 0                                           |
| `stressMarked`           | integer   | Yes      | >= 0, must be <= stressMax                    |
| `hopeMax`                | integer   | Yes      | > 0                                           |
| `hopeMarked`             | integer   | Yes      | >= 0, must be <= hopeMax                      |
| `gold`                   | integer   | Yes      | >= 0                                          |
| `activePrimaryWeaponId`  | long      | No       | Must reference existing Weapon                |
| `activeSecondaryWeaponId`| long      | No       | Must reference existing Weapon                |
| `activeArmorId`          | long      | No       | Must reference existing Armor                 |
| `communityCardIds`       | long[]    | No       | Each must reference existing CommunityCard    |
| `ancestryCardIds`        | long[]    | No       | Each must reference existing AncestryCard     |
| `subclassCardIds`        | long[]    | No       | Each must reference existing SubclassCard     |
| `domainCardIds`          | long[]    | No       | Each must reference existing DomainCard       |
| `inventoryWeaponIds`     | long[]    | No       | Each must reference existing Weapon           |
| `inventoryArmorIds`      | long[]    | No       | Each must reference existing Armor            |
| `inventoryItemIds`       | long[]    | No       | Each must reference existing Loot             |

### UpdateCharacterSheetRequest

All fields are optional. Only non-null fields are applied. Same validation rules as create but no required fields.

Collection fields (`communityCardIds`, `ancestryCardIds`, `subclassCardIds`, `domainCardIds`, `inventoryWeaponIds`, `inventoryArmorIds`, `inventoryItemIds`) replace the entire collection when provided. Omit to leave the collection unchanged.

### CharacterSheetResponse

| Field                    | Type                      | Always Present | Notes                                      |
|--------------------------|---------------------------|----------------|--------------------------------------------|
| `id`                     | long                      | Yes            | --                                         |
| `name`                   | string                    | Yes            | --                                         |
| `pronouns`               | string                    | No             | Omitted if null                            |
| `level`                  | integer                   | Yes            | --                                         |
| `evasion`                | integer                   | Yes            | --                                         |
| `armorMax`               | integer                   | Yes            | --                                         |
| `armorMarked`            | integer                   | Yes            | --                                         |
| `majorDamageThreshold`   | integer                   | Yes            | --                                         |
| `severeDamageThreshold`  | integer                   | Yes            | --                                         |
| `agilityModifier`        | integer                   | Yes            | --                                         |
| `agilityMarked`          | boolean                   | Yes            | --                                         |
| `strengthModifier`       | integer                   | Yes            | --                                         |
| `strengthMarked`         | boolean                   | Yes            | --                                         |
| `finesseModifier`        | integer                   | Yes            | --                                         |
| `finesseMarked`          | boolean                   | Yes            | --                                         |
| `instinctModifier`       | integer                   | Yes            | --                                         |
| `instinctMarked`         | boolean                   | Yes            | --                                         |
| `presenceModifier`       | integer                   | Yes            | --                                         |
| `presenceMarked`         | boolean                   | Yes            | --                                         |
| `knowledgeModifier`      | integer                   | Yes            | --                                         |
| `knowledgeMarked`        | boolean                   | Yes            | --                                         |
| `hitPointMax`            | integer                   | Yes            | --                                         |
| `hitPointMarked`         | integer                   | Yes            | --                                         |
| `stressMax`              | integer                   | Yes            | --                                         |
| `stressMarked`           | integer                   | Yes            | --                                         |
| `hopeMax`                | integer                   | Yes            | --                                         |
| `hopeMarked`             | integer                   | Yes            | --                                         |
| `gold`                   | integer                   | Yes            | --                                         |
| `ownerId`                | long                      | Yes            | --                                         |
| `owner`                  | UserResponse              | No             | Only with `?expand=owner`                  |
| `activePrimaryWeaponId`  | long                      | No             | Null if no weapon equipped                 |
| `activePrimaryWeapon`    | WeaponResponse            | No             | Only with `?expand=activePrimaryWeapon`    |
| `activeSecondaryWeaponId`| long                      | No             | Null if no weapon equipped                 |
| `activeSecondaryWeapon`  | WeaponResponse            | No             | Only with `?expand=activeSecondaryWeapon`  |
| `activeArmorId`          | long                      | No             | Null if no armor equipped                  |
| `activeArmor`            | ArmorResponse             | No             | Only with `?expand=activeArmor`            |
| `communityCardIds`       | long[]                    | Yes            | --                                         |
| `communityCards`         | CommunityCardResponse[]   | No             | Only with `?expand=communityCards`         |
| `ancestryCardIds`        | long[]                    | Yes            | --                                         |
| `ancestryCards`          | AncestryCardResponse[]    | No             | Only with `?expand=ancestryCards`          |
| `subclassCardIds`        | long[]                    | Yes            | --                                         |
| `subclassCards`          | SubclassCardResponse[]    | No             | Only with `?expand=subclassCards`          |
| `domainCardIds`          | long[]                    | Yes            | --                                         |
| `domainCards`            | DomainCardResponse[]      | No             | Only with `?expand=domainCards`            |
| `inventoryWeaponIds`     | long[]                    | Yes            | --                                         |
| `inventoryWeapons`       | WeaponResponse[]          | No             | Only with `?expand=inventoryWeapons`       |
| `inventoryArmorIds`      | long[]                    | Yes            | --                                         |
| `inventoryArmors`        | ArmorResponse[]           | No             | Only with `?expand=inventoryArmors`        |
| `inventoryItemIds`       | long[]                    | Yes            | --                                         |
| `inventoryItems`         | LootResponse[]            | No             | Only with `?expand=inventoryItems`         |
| `experienceIds`          | long[]                    | Yes            | --                                         |
| `experiences`            | ExperienceResponse[]      | No             | Only with `?expand=experiences`            |
| `createdAt`              | datetime                  | Yes            | ISO 8601 format                            |
| `lastModifiedAt`         | datetime                  | Yes            | ISO 8601 format                            |
| `deletedAt`              | datetime                  | No             | Omitted if null (not soft-deleted)         |

### PagedResponse\<T\>

| Field           | Type    | Description                              |
|-----------------|---------|------------------------------------------|
| `content`       | T[]     | Items for the current page               |
| `totalElements` | long    | Total items across all pages             |
| `totalPages`    | integer | Total number of pages                    |
| `currentPage`   | integer | Current page number (zero-based)         |
| `pageSize`      | integer | Number of items per page                 |

---

## Expanded Nested Types

### UserResponse

Returned when expanding `owner`.

| Field               | Type     | Notes                            |
|---------------------|----------|----------------------------------|
| `id`                | long     | --                               |
| `username`          | string   | --                               |
| `email`             | string   | --                               |
| `avatarUrl`         | string   | Omitted if null                  |
| `timezone`          | string   | Omitted if null                  |
| `createdAt`         | datetime | --                               |
| `lastModifiedAt`    | datetime | --                               |

### ExperienceResponse

Returned when expanding `experiences`.

| Field              | Type     | Notes                            |
|--------------------|----------|----------------------------------|
| `id`               | long     | --                               |
| `characterSheetId` | long     | --                               |
| `createdById`      | long     | --                               |
| `description`      | string   | e.g., "Survived dragon attack"   |
| `modifier`         | integer  | Typically +2                     |
| `createdAt`        | datetime | --                               |
| `lastModifiedAt`   | datetime | --                               |

### WeaponResponse

Returned when expanding `activePrimaryWeapon`, `activeSecondaryWeapon`, or `inventoryWeapons`. All fields are populated when the weapon is expanded from a character sheet.

Add `?expand=features` to also expand feature objects within the weapon. Add `?expand=expansion` to expand the expansion object. Add `?expand=features,modifiers` to also expand modifiers within each feature.

| Field              | Type                        | Notes                                                         |
|--------------------|-----------------------------|---------------------------------------------------------------|
| `id`               | long                        | --                                                            |
| `name`             | string                      | --                                                            |
| `expansionId`      | long                        | Omitted if null                                               |
| `expansion`        | ExpansionResponse           | Only with `?expand=expansion`; omitted otherwise              |
| `tier`             | integer                     | 1-4                                                           |
| `isOfficial`       | boolean                     | --                                                            |
| `isPrimary`        | boolean                     | --                                                            |
| `trait`            | Trait enum                  | Attack trait                                                  |
| `range`            | Range enum                  | --                                                            |
| `burden`           | Burden enum                 | --                                                            |
| `damage`           | DamageRollResponse          | --                                                            |
| `featureIds`       | long[]                      | Always present                                                |
| `features`         | FeatureResponse[]           | Only with `?expand=features`; omitted otherwise               |
| `originalWeaponId` | long                        | Omitted if null                                               |
| `createdAt`        | datetime                    | --                                                            |
| `lastModifiedAt`   | datetime                    | --                                                            |
| `deletedAt`        | datetime                    | Omitted if null                                               |

#### DamageRollResponse (nested in WeaponResponse)

| Field        | Type           | Notes                           |
|--------------|----------------|---------------------------------|
| `diceCount`  | integer        | Number of dice to roll          |
| `diceType`   | DiceType enum  | --                              |
| `modifier`   | integer        | Added to roll result            |
| `damageType` | DamageType enum| --                              |
| `notation`   | string         | e.g., "2d10+3 phy"             |

### ArmorResponse

Returned when expanding `activeArmor` or `inventoryArmors`. All fields are populated when the armor is expanded from a character sheet.

Add `?expand=features` to also expand feature objects within the armor. Add `?expand=expansion` to expand the expansion object. Add `?expand=features,modifiers` to also expand modifiers within each feature.

| Field                | Type              | Notes                                                         |
|----------------------|-------------------|---------------------------------------------------------------|
| `id`                 | long              | --                                                            |
| `name`               | string            | --                                                            |
| `expansionId`        | long              | Omitted if null                                               |
| `expansion`          | ExpansionResponse | Only with `?expand=expansion`; omitted otherwise              |
| `tier`               | integer           | 1-4                                                           |
| `isOfficial`         | boolean           | --                                                            |
| `baseMajorThreshold` | integer           | --                                                            |
| `baseSevereThreshold`| integer           | --                                                            |
| `baseScore`          | integer           | --                                                            |
| `featureIds`         | long[]            | Always present                                                |
| `features`           | FeatureResponse[] | Only with `?expand=features`; omitted otherwise               |
| `originalArmorId`    | long              | Omitted if null                                               |
| `createdAt`          | datetime          | --                                                            |
| `lastModifiedAt`     | datetime          | --                                                            |
| `deletedAt`          | datetime          | Omitted if null                                               |

### CommunityCardResponse

Returned when expanding `communityCards`. All fields are populated when the card is expanded from a character sheet.

Add `?expand=features` to also expand feature objects on the card. Add `?expand=costTags` to expand cost tag objects directly on the card (and within features if `features` is also expanded). Add `?expand=expansion` to expand the expansion object.

| Field               | Type                    | Notes                                                         |
|---------------------|-------------------------|---------------------------------------------------------------|
| `id`                | long                    | --                                                            |
| `name`              | string                  | --                                                            |
| `description`       | string                  | --                                                            |
| `cardType`          | CardType enum           | Always `COMMUNITY`                                            |
| `expansionId`       | long                    | Omitted if null                                               |
| `expansion`         | ExpansionResponse       | Only with `?expand=expansion`; omitted otherwise              |
| `isOfficial`        | boolean                 | --                                                            |
| `backgroundImageUrl`| string                  | Omitted if null                                               |
| `featureIds`        | long[]                  | Always present                                                |
| `features`          | FeatureResponse[]       | Only with `?expand=features`; omitted otherwise               |
| `costTagIds`        | long[]                  | Always present                                                |
| `costTags`          | CardCostTagResponse[]   | Only with `?expand=costTags`; omitted otherwise               |
| `createdAt`         | datetime                | --                                                            |
| `lastModifiedAt`    | datetime                | --                                                            |
| `deletedAt`         | datetime                | Omitted if null                                               |

### AncestryCardResponse

Returned when expanding `ancestryCards`. All fields are populated when the card is expanded from a character sheet.

Add `?expand=features` to also expand feature objects on the card. Add `?expand=costTags` to expand cost tag objects directly on the card (and within features if `features` is also expanded). Add `?expand=expansion` to expand the expansion object.

| Field               | Type                    | Notes                                                         |
|---------------------|-------------------------|---------------------------------------------------------------|
| `id`                | long                    | --                                                            |
| `name`              | string                  | --                                                            |
| `description`       | string                  | --                                                            |
| `cardType`          | CardType enum           | Always `ANCESTRY`                                             |
| `expansionId`       | long                    | Omitted if null                                               |
| `expansion`         | ExpansionResponse       | Only with `?expand=expansion`; omitted otherwise              |
| `isOfficial`        | boolean                 | --                                                            |
| `backgroundImageUrl`| string                  | Omitted if null                                               |
| `featureIds`        | long[]                  | Always present                                                |
| `features`          | FeatureResponse[]       | Only with `?expand=features`; omitted otherwise               |
| `costTagIds`        | long[]                  | Always present                                                |
| `costTags`          | CardCostTagResponse[]   | Only with `?expand=costTags`; omitted otherwise               |
| `createdAt`         | datetime                | --                                                            |
| `lastModifiedAt`    | datetime                | --                                                            |
| `deletedAt`         | datetime                | Omitted if null                                               |

### SubclassCardResponse

Returned when expanding `subclassCards`. All fields are populated when the card is expanded from a character sheet.

Add `?expand=features` to also expand feature objects on the card. Add `?expand=costTags` to expand cost tag objects directly on the card (and within features if `features` is also expanded). Add `?expand=expansion` to expand the expansion object.

| Field                | Type                    | Notes                                                         |
|----------------------|-------------------------|---------------------------------------------------------------|
| `id`                 | long                    | --                                                            |
| `name`               | string                  | --                                                            |
| `description`        | string                  | --                                                            |
| `cardType`           | CardType enum           | Always `SUBCLASS`                                             |
| `expansionId`        | long                    | Omitted if null                                               |
| `expansion`          | ExpansionResponse       | Only with `?expand=expansion`; omitted otherwise              |
| `expansionName`      | string                  | --                                                            |
| `isOfficial`         | boolean                 | --                                                            |
| `backgroundImageUrl` | string                  | Omitted if null                                               |
| `featureIds`         | long[]                  | Always present                                                |
| `features`           | FeatureResponse[]       | Only with `?expand=features`; omitted otherwise               |
| `costTagIds`         | long[]                  | Always present                                                |
| `costTags`           | CardCostTagResponse[]   | Only with `?expand=costTags`; omitted otherwise               |
| `associatedClassId`  | long                    | --                                                            |
| `associatedClassName`| string                  | --                                                            |
| `subclassPathId`     | long                    | --                                                            |
| `subclassPathName`   | string                  | --                                                            |
| `domainNames`        | string[]                | --                                                            |
| `domainIds`          | long[]                  | --                                                            |
| `spellcastingTrait`  | TraitInfo               | Null if no spellcasting                                       |
| `level`              | SubclassLevel enum      | --                                                            |
| `createdAt`          | datetime                | --                                                            |
| `lastModifiedAt`     | datetime                | --                                                            |
| `deletedAt`          | datetime                | Omitted if null                                               |

### LootResponse

Returned when expanding `inventoryItems`. All fields are populated when the loot item is expanded from a character sheet.

Add `?expand=features` to also expand feature objects within the loot item. Add `?expand=expansion` to expand the expansion object. Add `?expand=features,modifiers` to also expand modifiers within each feature.

| Field            | Type              | Notes                                                         |
|------------------|-------------------|---------------------------------------------------------------|
| `id`             | long              | --                                                            |
| `name`           | string            | --                                                            |
| `expansionId`    | long              | Omitted if null                                               |
| `expansion`      | ExpansionResponse | Only with `?expand=expansion`; omitted otherwise              |
| `tier`           | integer           | 1-4                                                           |
| `isOfficial`     | boolean           | --                                                            |
| `isConsumable`   | boolean           | --                                                            |
| `description`    | string            | --                                                            |
| `featureIds`     | long[]            | Always present                                                |
| `features`       | FeatureResponse[] | Only with `?expand=features`; omitted otherwise               |
| `originalLootId` | long              | Omitted if null                                               |
| `createdAt`      | datetime          | --                                                            |
| `lastModifiedAt` | datetime          | --                                                            |
| `deletedAt`      | datetime          | Omitted if null                                               |

### DomainCardResponse

Returned when expanding `domainCards`. All fields are populated when the card is expanded from a character sheet.

Add `?expand=features` to also expand feature objects on the card. Add `?expand=costTags` to expand cost tag objects directly on the card (and within features if `features` is also expanded). Add `?expand=expansion` to expand the expansion object.

| Field                | Type                    | Notes                                                         |
|----------------------|-------------------------|---------------------------------------------------------------|
| `id`                 | long                    | --                                                            |
| `name`               | string                  | --                                                            |
| `description`        | string                  | --                                                            |
| `cardType`           | CardType enum           | Always `DOMAIN`                                               |
| `expansionId`        | long                    | Omitted if null                                               |
| `expansion`          | ExpansionResponse       | Only with `?expand=expansion`; omitted otherwise              |
| `isOfficial`         | boolean                 | --                                                            |
| `backgroundImageUrl` | string                  | Omitted if null                                               |
| `featureIds`         | long[]                  | Always present                                                |
| `features`           | FeatureResponse[]       | Only with `?expand=features`; omitted otherwise               |
| `costTagIds`         | long[]                  | Always present                                                |
| `costTags`           | CardCostTagResponse[]   | Only with `?expand=costTags`; omitted otherwise               |
| `associatedDomainId` | long                    | --                                                            |
| `level`              | integer                 | Level requirement for the card                                |
| `recallCost`         | integer                 | Cost to recall/use this card (>= 0)                           |
| `type`               | DomainCardType enum     | `SPELL`, `GRIMOIRE`, `ABILITY`, `TRANSFORMATION`, or `WILD`   |
| `createdAt`          | datetime                | --                                                            |
| `lastModifiedAt`     | datetime                | --                                                            |
| `deletedAt`          | datetime                | Omitted if null                                               |

### FeatureResponse

Returned within expanded items and cards when `?expand=features` is used. Includes the full feature definition. Nested cost tags and modifiers can be further expanded with `?expand=costTags` and `?expand=modifiers` respectively.

| Field            | Type                        | Notes                                                         |
|------------------|-----------------------------|---------------------------------------------------------------|
| `id`             | long                        | --                                                            |
| `name`           | string                      | Omitted if null                                               |
| `description`    | string                      | Omitted if null                                               |
| `featureType`    | FeatureType enum            | e.g., `HOPE`, `ANCESTRY`, `CLASS`, `DOMAIN`, `ITEM`, etc.    |
| `expansionId`    | long                        | --                                                            |
| `expansion`      | ExpansionResponse           | Only with `?expand=expansion`; omitted otherwise              |
| `costTagIds`     | long[]                      | Always present                                                |
| `costTags`       | CardCostTagResponse[]       | Only with `?expand=costTags`; omitted otherwise               |
| `modifierIds`    | long[]                      | Always present                                                |
| `modifiers`      | FeatureModifierResponse[]   | Only with `?expand=modifiers`; omitted otherwise              |
| `createdAt`      | datetime                    | --                                                            |
| `lastModifiedAt` | datetime                    | --                                                            |
| `deletedAt`      | datetime                    | Omitted if null                                               |

### CardCostTagResponse

Returned within expanded cards and features when `?expand=costTags` is used.

| Field            | Type                | Notes                                                         |
|------------------|---------------------|---------------------------------------------------------------|
| `id`             | long                | --                                                            |
| `label`          | string              | Display label, e.g., `"3 Hope"`, `"1/session"`, `"Close range"` |
| `category`       | CostTagCategory enum| `COST`, `LIMITATION`, or `TIMING`                             |
| `createdAt`      | datetime            | --                                                            |
| `lastModifiedAt` | datetime            | --                                                            |
| `deletedAt`      | datetime            | Omitted if null                                               |

### FeatureModifierResponse

Returned within expanded features when `?expand=modifiers` is used (requires `?expand=features` to also be present).

| Field            | Type                    | Notes                                                         |
|------------------|-------------------------|---------------------------------------------------------------|
| `id`             | long                    | --                                                            |
| `target`         | ModifierTarget enum     | The character attribute this modifier affects                 |
| `operation`      | ModifierOperation enum  | `ADD`, `SET`, or `MULTIPLY`                                   |
| `value`          | integer                 | The numeric value used in the operation                       |
| `createdAt`      | datetime                | --                                                            |
| `lastModifiedAt` | datetime                | --                                                            |

### ExpansionResponse

Returned within expanded items, cards, and features when `?expand=expansion` is used.

| Field            | Type     | Notes |
|------------------|----------|-------|
| `id`             | long     | --    |
| `name`           | string   | --    |
| `isPublished`    | boolean  | --    |
| `createdAt`      | datetime | --    |
| `lastModifiedAt` | datetime | --    |

### ErrorResponse

| Field       | Type     | Description                    |
|-------------|----------|--------------------------------|
| `status`    | integer  | HTTP status code               |
| `error`     | string   | Error category                 |
| `message`   | string   | Human-readable message         |
| `path`      | string   | Request path                   |
| `timestamp` | datetime | When the error occurred         |

### ValidationErrorResponse

| Field         | Type              | Description                    |
|---------------|-------------------|--------------------------------|
| `status`      | integer           | Always `400`                   |
| `error`       | string            | Always `"Validation Failed"`   |
| `fieldErrors` | map<string,string>| Field name to error message    |
| `path`        | string            | Request path                   |
| `timestamp`   | datetime          | When the error occurred         |

---

## Enums

### Trait

The six core character traits in Daggerheart.

| Value       | Description                                        |
|-------------|----------------------------------------------------|
| `AGILITY`   | Quick reflexes, nimbleness, and coordination       |
| `STRENGTH`  | Raw physical power and endurance                   |
| `FINESSE`   | Precision, grace, and careful execution            |
| `INSTINCT`  | Intuition, awareness, and natural understanding    |
| `PRESENCE`  | Force of personality and social influence           |
| `KNOWLEDGE` | Learning, reasoning, and mental acuity             |

### CardType

| Value       | Description           |
|-------------|-----------------------|
| `ANCESTRY`  | Ancestry card type    |
| `COMMUNITY` | Community card type   |
| `SUBCLASS`  | Subclass card type    |
| `DOMAIN`    | Domain card type      |

### SubclassLevel

| Value            | Description                        |
|------------------|------------------------------------|
| `FOUNDATION`     | Foundation level subclass          |
| `SPECIALIZATION` | Specialization level subclass      |
| `MASTERY`        | Mastery level subclass             |

### Burden

| Value        | Description                      |
|--------------|----------------------------------|
| `ONE_HANDED` | Can be wielded with one hand     |
| `TWO_HANDED` | Requires both hands              |

### Range

| Value          | Description                           |
|----------------|---------------------------------------|
| `MELEE`        | Close-quarters, under 5 feet          |
| `VERY_CLOSE`   | Extended melee, 5-10 feet             |
| `CLOSE`        | Short throwing, 10-30 feet            |
| `FAR`          | Standard ranged, 30-100 feet          |
| `VERY_FAR`     | Long-range, 100-300 feet              |
| `OUT_OF_RANGE` | Beyond normal effectiveness, 300+ feet|

### DiceType

| Value | Sides | Code  |
|-------|-------|-------|
| `D4`  | 4     | `d4`  |
| `D6`  | 6     | `d6`  |
| `D8`  | 8     | `d8`  |
| `D10` | 10    | `d10` |
| `D12` | 12    | `d12` |
| `D20` | 20    | `d20` |

### DamageType

| Value      | Code  | Description                               |
|------------|-------|-------------------------------------------|
| `PHYSICAL` | `phy` | Physical damage from weapons/melee        |
| `MAGIC`    | `mag` | Magic damage from spells/abilities        |

### Role (for access control context)

| Value       | Description                          |
|-------------|--------------------------------------|
| `OWNER`     | Highest privilege                    |
| `ADMIN`     | Administrative access                |
| `MODERATOR` | Can bypass ownership checks          |
| `USER`      | Standard authenticated user          |

### FeatureType

| Value       | Description                        |
|-------------|------------------------------------|
| `HOPE`      | Hope feature type                  |
| `ANCESTRY`  | Ancestry feature type              |
| `CLASS`     | Class feature type                 |
| `COMMUNITY` | Community feature type             |
| `DOMAIN`    | Domain feature type                |
| `ITEM`      | Item feature type                  |
| `SUBCLASS`  | Subclass feature type              |
| `OTHER`     | Other feature type                 |

### DomainCardType

| Value            | Description                  |
|------------------|------------------------------|
| `SPELL`          | Spell domain card            |
| `GRIMOIRE`       | Grimoire domain card         |
| `ABILITY`        | Ability domain card          |
| `TRANSFORMATION` | Transformation domain card   |
| `WILD`           | Wild domain card             |

### CostTagCategory

| Value        | Description                                                         |
|--------------|---------------------------------------------------------------------|
| `COST`       | Resource expenditure tags (e.g., "3 Hope", "1 Stress")              |
| `LIMITATION` | Restriction or requirement tags (e.g., "Close range")               |
| `TIMING`     | Frequency or action type tags (e.g., "1/session", "Action")         |

### ModifierTarget

| Value                   | Description                                         |
|-------------------------|-----------------------------------------------------|
| `AGILITY`               | Modifies the character's Agility trait score        |
| `STRENGTH`              | Modifies the character's Strength trait score       |
| `FINESSE`               | Modifies the character's Finesse trait score        |
| `INSTINCT`              | Modifies the character's Instinct trait score       |
| `PRESENCE`              | Modifies the character's Presence trait score       |
| `KNOWLEDGE`             | Modifies the character's Knowledge trait score      |
| `EVASION`               | Modifies the character's Evasion defense value      |
| `MAJOR_DAMAGE_THRESHOLD`| Modifies the character's Major damage threshold     |
| `SEVERE_DAMAGE_THRESHOLD`| Modifies the character's Severe damage threshold   |
| `HIT_POINT_MAX`         | Modifies the character's maximum Hit Points         |
| `STRESS_MAX`            | Modifies the character's maximum Stress capacity    |
| `HOPE_MAX`              | Modifies the character's maximum Hope               |
| `ARMOR_MAX`             | Modifies the character's maximum Armor slots        |
| `GOLD`                  | Modifies the character's starting Gold              |
| `ATTACK_ROLL`           | Modifies the character's attack roll result         |
| `DAMAGE_ROLL`           | Modifies the character's damage roll result         |
| `PRIMARY_DAMAGE_ROLL`   | Modifies the character's primary damage roll result |
| `ARMOR_SCORE`           | Modifies the character's armor score                |

### ModifierOperation

Evaluated in this order: SET first, then MULTIPLY, then ADD.

| Value      | Description                                             |
|------------|---------------------------------------------------------|
| `ADD`      | Adds the value to the target attribute                  |
| `SET`      | Sets the target attribute to the specified value        |
| `MULTIPLY` | Multiplies the target attribute by the specified value  |

---

## Database Constraints

The `character_sheets` table enforces these constraints at the database level:

| Constraint                          | Rule                                                |
|-------------------------------------|-----------------------------------------------------|
| `check_level_positive`              | `level >= 1`                                        |
| `check_severe_gte_major`            | `severe_damage_threshold >= major_damage_threshold`  |
| `check_hit_point_marked_lte_max`    | `hit_point_marked <= hit_point_max`                  |
| `check_stress_marked_lte_max`       | `stress_marked <= stress_max`                        |
| `check_hope_marked_lte_max`         | `hope_marked <= hope_max`                            |
| `check_armor_marked_lte_max`        | `armor_marked <= armor_max`                          |

These constraints are also validated in the service layer before persistence, returning `400 Bad Request` with descriptive error messages.

### Foreign Key Behavior

| Relationship            | On Delete     |
|-------------------------|---------------|
| `owner_id` -> `users`  | CASCADE       |
| `active_primary_weapon_id` -> `weapons` | SET NULL |
| `active_secondary_weapon_id` -> `weapons` | SET NULL |
| `active_armor_id` -> `armors` | SET NULL |
| All join table references | CASCADE     |

### Sorting

List results are sorted by `createdAt` descending (newest first).
