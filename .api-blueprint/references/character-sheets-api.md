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
      "proficiency": 2,
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
      "equippedDomainCardIds": [],
      "vaultDomainCardIds": [],
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
  "proficiency": 2,
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
  "domainCardIds": [],
  "equippedDomainCardIds": [],
  "vaultDomainCardIds": [],
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
    "equippedDomainCardIds": [8],
    "vaultDomainCardIds": [9],
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
  "proficiency": 1,
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
  "domainCardIds": [8, 9],
  "equippedDomainCardIds": [8],
  "vaultDomainCardIds": [9],
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
  "proficiency": 2,
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
  "domainCardIds": [],
  "equippedDomainCardIds": [],
  "vaultDomainCardIds": [],
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

### Get Level-Up Options

```
GET /api/dh/character-sheets/{id}/level-up-options
```

**Authorization:** Any authenticated user (character sheet owner or MODERATOR/ADMIN/OWNER role).

Returns the available advancement options for the character's next level-up, including which advancements are still available in the current tier, domain card constraints, and tier transition information.

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Response:** `200 OK` with `LevelUpOptionsResponse`

**Example Request:**

```bash
curl -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/character-sheets/1/level-up-options"
```

**Example Response:**

```json
{
  "currentLevel": 4,
  "nextLevel": 5,
  "currentTier": 2,
  "nextTier": 3,
  "isTierTransition": true,
  "availableAdvancements": [
    {
      "type": "BOOST_TRAITS",
      "remaining": 2,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "GAIN_HP",
      "remaining": 2,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "GAIN_STRESS",
      "remaining": 1,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "BOOST_EXPERIENCES",
      "remaining": 1,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "GAIN_DOMAIN_CARD",
      "remaining": 1,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "BOOST_EVASION",
      "remaining": 1,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "UPGRADE_SUBCLASS",
      "remaining": 1,
      "mutuallyExclusiveWith": ["MULTICLASS"]
    },
    {
      "type": "BOOST_PROFICIENCY",
      "remaining": 2,
      "mutuallyExclusiveWith": []
    },
    {
      "type": "MULTICLASS",
      "remaining": 2,
      "mutuallyExclusiveWith": ["UPGRADE_SUBCLASS"]
    }
  ],
  "domainCardLevelCap": 7,
  "accessibleDomainIds": [1, 3],
  "equippedDomainCardCount": 4,
  "maxEquippedDomainCards": 5
}
```

**Error Status Codes:**

| Status | Condition                                                  |
|--------|------------------------------------------------------------|
| `400`  | Character is already at max level (10)                     |
| `401`  | Missing or invalid authentication token                    |
| `403`  | Not the character owner and not MODERATOR+                 |
| `404`  | Character sheet not found or soft-deleted                  |

---

### Perform Level-Up

```
POST /api/dh/character-sheets/{id}/level-up
```

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role.

Performs a level-up on the character, applying tier achievements (if transitioning tiers), two chosen advancements, damage threshold increases, a new domain card, and optional domain card trades. All changes are applied atomically.

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Request Body:** `LevelUpRequest` (JSON)

**Response:** `200 OK` with `LevelUpResponse`

**Example Request:**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "advancements": [
      {
        "type": "BOOST_TRAITS",
        "traits": ["AGILITY", "STRENGTH"]
      },
      {
        "type": "GAIN_HP"
      }
    ],
    "newExperienceDescription": "Defeated the Shadow King",
    "newDomainCardId": 15,
    "equipNewDomainCard": true,
    "unequipDomainCardId": 8,
    "trades": [
      {
        "tradeOutCardIds": [3],
        "tradeInCardIds": [12],
        "equipTradedInCardIds": []
      }
    ]
  }' \
  "http://localhost:8080/api/dh/character-sheets/1/level-up"
```

**Example Response:**

```json
{
  "characterSheet": {
    "id": 1,
    "name": "Aragorn",
    "level": 5,
    "proficiency": 2,
    "evasion": 10,
    "hitPointMax": 11,
    "stressMax": 6,
    "majorDamageThreshold": 4,
    "severeDamageThreshold": 7,
    "agilityModifier": 1,
    "agilityMarked": true,
    "strengthModifier": 1,
    "strengthMarked": true,
    "equippedDomainCardIds": [9, 10, 11, 12, 15],
    "vaultDomainCardIds": [8],
    "domainCardIds": [8, 9, 10, 11, 12, 15],
    "...": "other fields"
  },
  "advancementLogId": 4,
  "appliedChanges": [
    "Tier transition: created experience 'Defeated the Shadow King' (+2), proficiency +1, cleared marked traits",
    "Applied BOOST_TRAITS: +1 AGILITY (marked), +1 STRENGTH (marked)",
    "Applied GAIN_HP: +1 hit point max (now 11)",
    "Damage thresholds: +1 major (now 4), +1 severe (now 7)",
    "Added domain card 15 (equipped)",
    "Trade: removed [3], added [12]"
  ]
}
```

**Example Request (Tier Transition with `boostNewExperience`):**

When leveling into a new tier (levels 2, 5, 8), a new experience is created. Setting `boostNewExperience` to `true` allows you to automatically boost the newly created experience along with one existing experience, instead of specifying two existing experience IDs.

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "advancements": [
      {
        "type": "BOOST_EXPERIENCES",
        "experienceIds": [1],
        "boostNewExperience": true
      },
      {
        "type": "GAIN_HP"
      }
    ],
    "newExperienceDescription": "Survived the dragon attack"
  }' \
  "http://localhost:8080/api/dh/character-sheets/1/level-up"
```

**Error Status Codes:**

| Status | Condition                                                              |
|--------|------------------------------------------------------------------------|
| `400`  | Character is already at max level (10)                                 |
| `400`  | Validation failure (wrong number of advancements, invalid types, etc.) |
| `400`  | Advancement type not available in target tier                          |
| `400`  | Advancement type usage exceeded for tier                               |
| `400`  | Mutual exclusion violation (UPGRADE_SUBCLASS vs MULTICLASS)            |
| `400`  | Duplicate BOOST_TRAITS with overlapping traits across choices          |
| `400`  | Duplicate MULTICLASS targeting the same class in both choices          |
| `400`  | BOOST_TRAITS: traits already marked (except at levels 5/8 tier transitions) or wrong count |
| `400`  | BOOST_EXPERIENCES: experience IDs invalid or wrong count               |
| `400`  | Domain card not from accessible domain or exceeds level cap            |
| `400`  | Equipped domain card count would exceed 5                              |
| `400`  | Trade validation failure (unequal count, invalid cards)                |
| `400`  | Missing `newExperienceDescription` at tier transition                  |
| `401`  | Missing or invalid authentication token                                |
| `403`  | Not the character owner and not MODERATOR+                             |
| `404`  | Character sheet not found or soft-deleted                              |
| `404`  | Referenced domain card, subclass card, or experience not found         |

---

### Undo Level-Up

```
DELETE /api/dh/character-sheets/{id}/level-up
```

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role.

Undoes the most recent level-up for the character, reversing all changes including tier achievements, advancements, damage thresholds, domain card additions, and domain card trades. Can be called repeatedly to undo multiple level-ups (e.g., level 7 to 6 to 5 to 4).

**Path Parameters:**

| Parameter | Type | Required | Description          |
|-----------|------|----------|----------------------|
| `id`      | long | Yes      | Character sheet ID   |

**Response:** `200 OK` with `CharacterSheetResponse` (the character sheet after undoing the level-up)

**Example Request:**

```bash
curl -X DELETE -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/character-sheets/1/level-up"
```

**Error Status Codes:**

| Status | Condition                                                  |
|--------|------------------------------------------------------------|
| `400`  | No advancement log exists (character has never leveled up) |
| `401`  | Missing or invalid authentication token                    |
| `403`  | Not the character owner and not MODERATOR+                 |
| `404`  | Character sheet not found or soft-deleted                  |

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
| `domainCards`          | All assigned domain cards (equipped + vault) | `domainCards`               | `DomainCardResponse[]`      |
| `equippedDomainCards`  | Equipped domain cards only (max 5)           | `equippedDomainCards`       | `DomainCardResponse[]`      |
| `vaultDomainCards`     | Vault (unequipped) domain cards              | `vaultDomainCards`          | `DomainCardResponse[]`      |
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
| `proficiency`            | integer   | No       | >= 1, defaults to 1                           |
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
| `equippedDomainCardIds`  | long[]    | No       | Must be provided with `vaultDomainCardIds`. Each must reference existing DomainCard. No duplicates within or across lists. |
| `vaultDomainCardIds`     | long[]    | No       | Must be provided with `equippedDomainCardIds`. Each must reference existing DomainCard. No duplicates within or across lists. |
| `inventoryWeaponIds`     | long[]    | No       | Each must reference existing Weapon           |
| `inventoryArmorIds`      | long[]    | No       | Each must reference existing Armor            |
| `inventoryItemIds`       | long[]    | No       | Each must reference existing Loot             |

**Domain cards** use `equippedDomainCardIds` and `vaultDomainCardIds` instead of a single `domainCardIds` field. Both must be provided together. A card ID must not appear in both lists or be duplicated within a list. The equipped list determines which domain cards are actively equipped (max 5), and the vault list holds unequipped domain cards.

### UpdateCharacterSheetRequest

All fields are optional. Only non-null fields are applied. Same validation rules as create but no required fields.

Collection fields (`communityCardIds`, `ancestryCardIds`, `subclassCardIds`, `inventoryWeaponIds`, `inventoryArmorIds`, `inventoryItemIds`) replace the entire collection when provided. Omit to leave the collection unchanged.

**Domain cards** use `equippedDomainCardIds` and `vaultDomainCardIds` (same rules as create). Both must be provided together to update domain cards.

### CharacterSheetResponse

| Field                    | Type                      | Always Present | Notes                                      |
|--------------------------|---------------------------|----------------|--------------------------------------------|
| `id`                     | long                      | Yes            | --                                         |
| `name`                   | string                    | Yes            | --                                         |
| `pronouns`               | string                    | No             | Omitted if null                            |
| `level`                  | integer                   | Yes            | --                                         |
| `proficiency`            | integer                   | Yes            | Character's proficiency bonus (starts at 1) |
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
| `domainCardIds`          | long[]                    | Yes            | Union of equipped + vault (backward compat) |
| `domainCards`            | DomainCardResponse[]      | No             | Only with `?expand=domainCards`            |
| `equippedDomainCardIds`  | long[]                    | Yes            | IDs of equipped domain cards (max 5)       |
| `equippedDomainCards`    | DomainCardResponse[]      | No             | Only with `?expand=equippedDomainCards`    |
| `vaultDomainCardIds`     | long[]                    | Yes            | IDs of vault (unequipped) domain cards     |
| `vaultDomainCards`       | DomainCardResponse[]      | No             | Only with `?expand=vaultDomainCards`       |
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

### LevelUpRequest

Request body for the `POST /api/dh/character-sheets/{id}/level-up` endpoint.

| Field                      | Type                       | Required | Validation / Notes                                                     |
|----------------------------|----------------------------|----------|------------------------------------------------------------------------|
| `advancements`             | AdvancementChoice[]        | Yes      | Exactly 2 items (`@Size(min=2, max=2)`)                               |
| `newExperienceDescription` | string                     | Cond.    | Required at tier transitions (levels 2, 5, 8). Description for the new experience created during tier achievement. |
| `newDomainCardId`          | long                       | Yes      | ID of the domain card to add in Step 4. Must be from an accessible domain and within the tier's level cap. |
| `equipNewDomainCard`       | boolean                    | No       | Default `false`. Whether to equip the new domain card (equipped count must not exceed 5). |
| `unequipDomainCardId`      | long                       | No       | ID of a currently equipped domain card to unequip, to make room when at 5 equipped. |
| `trades`                   | DomainCardTradeRequest[]   | No       | Optional list of equal-swap domain card trades.                        |

### AdvancementChoice

One of the two advancement choices included in a `LevelUpRequest`.

| Field                       | Type    | Required     | Validation / Notes                                                              |
|-----------------------------|---------|--------------|---------------------------------------------------------------------------------|
| `type`                      | AdvancementType | Yes  | The advancement type to apply. Must be available in the target tier.             |
| `traits`                    | Trait[] | BOOST_TRAITS | Exactly 2 traits. Must be unmarked, except during tier transitions at levels 5 and 8 where marks are cleared. |
| `experienceIds`             | long[]  | BOOST_EXPERIENCES | Exactly 2 experience IDs belonging to the character.                       |
| `boostNewExperience`        | boolean | No           | `false`. When `true`, automatically includes the newly created tier transition experience as the second boost target. Only valid during tier transitions with BOOST_EXPERIENCES. |
| `domainCardId`              | long    | GAIN_DOMAIN_CARD | ID of a domain card from an accessible domain, within level cap.            |
| `equipDomainCard`           | boolean | No           | Default `false`. For GAIN_DOMAIN_CARD: whether to equip the gained card.        |
| `subclassCardId`            | long    | UPGRADE_SUBCLASS / MULTICLASS | For UPGRADE_SUBCLASS: ID of the next-level subclass card in a path the character already has. For MULTICLASS: ID of a FOUNDATION-level subclass card from a class the character doesn't already have. |

**Field usage by AdvancementType:**

| AdvancementType     | Required Fields                                           |
|---------------------|-----------------------------------------------------------|
| `BOOST_TRAITS`      | `traits` (2 Trait values)                                 |
| `GAIN_HP`           | None (type only)                                          |
| `GAIN_STRESS`       | None (type only)                                          |
| `BOOST_EXPERIENCES` | `experienceIds` (2 experience IDs), optionally `boostNewExperience`. When `boostNewExperience` is `true`, only 1 `experienceId` is required. |
| `GAIN_DOMAIN_CARD`  | `domainCardId`, optionally `equipDomainCard`              |
| `BOOST_EVASION`     | None (type only)                                          |
| `UPGRADE_SUBCLASS`  | `subclassCardId`                                          |
| `BOOST_PROFICIENCY` | None (type only)                                          |
| `MULTICLASS`        | `subclassCardId` (must be a FOUNDATION-level card)        |

### DomainCardTradeRequest

Represents an equal-swap trade of domain cards during level-up. Multiple trades can be included in a single level-up.

| Field                  | Type   | Required | Validation / Notes                                                          |
|------------------------|--------|----------|-----------------------------------------------------------------------------|
| `tradeOutCardIds`        | long[] | Yes    | IDs of domain cards the character currently owns to give up. Not empty.     |
| `tradeInCardIds`         | long[] | Yes    | IDs of domain cards to receive. Must be same count as traded out. Not empty. Cards must be from accessible domains and within level cap. |
| `equipTradedInCardIds`   | long[] | No     | Subset of `tradeInCardIds` to equip. Total equipped count must not exceed 5 after all operations. |

### LevelUpOptionsResponse

Returned by `GET /api/dh/character-sheets/{id}/level-up-options`.

| Field                    | Type                      | Description                                                         |
|--------------------------|---------------------------|---------------------------------------------------------------------|
| `currentLevel`           | integer                   | Character's current level                                           |
| `nextLevel`              | integer                   | Level the character will reach after level-up                       |
| `currentTier`            | integer                   | Tier for the current level                                          |
| `nextTier`               | integer                   | Tier for the next level                                             |
| `isTierTransition`       | boolean                   | `true` if leveling up crosses a tier boundary (levels 2, 5, 8)     |
| `availableAdvancements`  | AvailableAdvancement[]    | List of advancement types available for the next level-up           |
| `domainCardLevelCap`     | integer (nullable)        | Maximum domain card level for the next tier. `null` means uncapped (Tier 4). |
| `accessibleDomainIds`    | long[]                    | IDs of domains accessible to the character (determined by subclass paths) |
| `equippedDomainCardCount`| integer                   | Number of currently equipped domain cards                           |
| `maxEquippedDomainCards` | integer                   | Maximum equipped domain cards (always 5)                            |

#### AvailableAdvancement (nested in LevelUpOptionsResponse)

| Field                  | Type                | Description                                                        |
|------------------------|---------------------|--------------------------------------------------------------------|
| `type`                 | AdvancementType     | The advancement type                                               |
| `remaining`            | integer             | How many more times this advancement can be chosen in this tier    |
| `mutuallyExclusiveWith`| AdvancementType[]   | List of advancement types that are mutually exclusive with this one within a tier. Empty array if none. |

### LevelUpResponse

Returned by `POST /api/dh/character-sheets/{id}/level-up`.

| Field              | Type                    | Description                                                       |
|--------------------|-------------------------|-------------------------------------------------------------------|
| `characterSheet`   | CharacterSheetResponse  | The updated character sheet after all level-up changes are applied |
| `advancementLogId` | long                    | ID of the created advancement log entry (used internally for undo)|
| `appliedChanges`   | string[]                | Human-readable summary of all changes applied during the level-up |

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
