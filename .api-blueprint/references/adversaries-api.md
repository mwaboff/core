# Adversaries API

Base path: `/api/dh/adversaries`
Authentication: JWT via `AUTH_TOKEN` HttpOnly cookie (all endpoints require authentication)

---

## Endpoints

### List All Adversaries

```
GET /api/dh/adversaries
```

**Authorization:** Any authenticated user. Results filtered by visibility (official, public, or owned by the user).

**Query Parameters:**

| Parameter        | Type    | Required | Default | Description                                                     |
|------------------|---------|----------|---------|-----------------------------------------------------------------|
| `page`           | integer | No       | `0`     | Zero-based page number                                          |
| `size`           | integer | No       | `20`    | Items per page (max: 100)                                       |
| `includeDeleted` | boolean | No       | `false` | Include soft-deleted adversaries (ADMIN+ only)                  |
| `expansionId`    | long    | No       | --      | Filter by expansion ID                                          |
| `tier`           | integer | No       | --      | Filter by tier (1-4)                                            |
| `adversaryType`  | string  | No       | --      | Filter by adversary type (e.g., `MINION`, `BRUISER`)            |
| `isOfficial`     | boolean | No       | --      | Filter by official status                                       |
| `name`           | string  | No       | --      | Filter by name (case-insensitive partial match)                 |
| `expand`         | string  | No       | --      | Comma-separated list of relationships to expand                 |

**Expand Options:** `expansion`, `creator`, `originalAdversary`, `experiences`, `features`

**Response:** `200 OK` with `PagedResponse<AdversaryResponse>`

**Example Request:**

```bash
curl -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/adversaries?page=0&size=10&tier=1&adversaryType=MINION&expand=expansion"
```

**Example Response:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Goblin",
      "tier": 1,
      "adversaryType": "MINION",
      "description": null,
      "motivesAndTactics": null,
      "difficulty": 5,
      "majorThreshold": 3,
      "severeThreshold": 6,
      "hitPointMax": 10,
      "hitPointMarked": 0,
      "stressMax": 5,
      "stressMarked": 0,
      "attackModifier": null,
      "weaponName": null,
      "attackRange": null,
      "damage": {
        "diceCount": 1,
        "diceType": "D6",
        "modifier": 2,
        "damageType": "PHYSICAL",
        "notation": "1d6+2 phy"
      },
      "isOfficial": true,
      "isPublic": true,
      "expansionId": 1,
      "expansion": {
        "id": 1,
        "name": "Core Rulebook"
      },
      "originalAdversaryId": null,
      "creatorId": 1,
      "experienceIds": [],
      "featureIds": [],
      "createdAt": "2026-03-13T12:00:00",
      "lastModifiedAt": "2026-03-13T12:00:00",
      "deletedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10
}
```

---

### Get Adversary by ID

```
GET /api/dh/adversaries/{id}
```

**Authorization:** Any authenticated user. Access restricted to official, public, or user's own adversaries. Private adversaries created by other users return `404 Not Found`.

**Path Parameters:**

| Parameter | Type | Required | Description    |
|-----------|------|----------|----------------|
| `id`      | long | Yes      | Adversary ID   |

**Query Parameters:**

| Parameter | Type   | Required | Description                                     |
|-----------|--------|----------|-------------------------------------------------|
| `expand`  | string | No       | Comma-separated list of relationships to expand  |

**Expand Options:** `expansion`, `creator`, `originalAdversary`, `experiences`, `features`

**Response:** `200 OK` with `AdversaryResponse`

**Example Request:**

```bash
curl -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/adversaries/1?expand=creator,features"
```

**Example Response (with expand=creator,features):**

```json
{
  "id": 1,
  "name": "Orc Warrior",
  "tier": 2,
  "adversaryType": "BRUISER",
  "description": "A hulking orc wielding a massive greataxe",
  "motivesAndTactics": "Charges the nearest enemy and attacks relentlessly",
  "difficulty": 8,
  "majorThreshold": 5,
  "severeThreshold": 10,
  "hitPointMax": 20,
  "hitPointMarked": 0,
  "stressMax": 10,
  "stressMarked": 0,
  "attackModifier": 3,
  "weaponName": "Greataxe",
  "attackRange": "MELEE",
  "damage": {
    "diceCount": 2,
    "diceType": "D10",
    "modifier": 5,
    "damageType": "PHYSICAL",
    "notation": "2d10+5 phy"
  },
  "isOfficial": true,
  "isPublic": true,
  "expansionId": 1,
  "originalAdversaryId": null,
  "creatorId": 1,
  "creator": {
    "id": 1,
    "username": "owner",
    "role": "USER",
    "email": "owner@example.com"
  },
  "experienceIds": [],
  "featureIds": [10, 11],
  "features": [
    {
      "id": 10,
      "name": "Shadow Breath",
      "description": "Exhales a cone of shadow energy",
      "featureType": "OTHER"
    },
    {
      "id": 11,
      "name": "Dark Resilience",
      "description": "Resistance to shadow damage",
      "featureType": "OTHER"
    }
  ],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00",
  "deletedAt": null
}
```

**Error Responses:**

| Status | Condition                                 |
|--------|-------------------------------------------|
| `404`  | Adversary not found or not accessible     |

---

### Create Adversary

```
POST /api/dh/adversaries
```

**Authorization:** Any authenticated user. The authenticated user becomes the creator.

**Request Body:** `CreateAdversaryRequest` (JSON)

**Response:** `201 Created` with `AdversaryResponse`

**Example Request (minimal):**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Goblin",
    "tier": 1,
    "adversaryType": "MINION",
    "difficulty": 5,
    "majorThreshold": 3,
    "severeThreshold": 6,
    "hitPointMax": 10,
    "stressMax": 5,
    "expansionId": 1,
    "isPublic": false
  }' \
  "http://localhost:8080/api/dh/adversaries"
```

**Example Request (full with combat and inline features):**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Shadow Drake",
    "tier": 3,
    "adversaryType": "BRUISER",
    "description": "A drake wreathed in living shadow",
    "motivesAndTactics": "Targets the weakest party member first",
    "difficulty": 10,
    "majorThreshold": 5,
    "severeThreshold": 10,
    "hitPointMax": 30,
    "stressMax": 10,
    "attackModifier": 3,
    "weaponName": "Greataxe",
    "attackRange": "MELEE",
    "damage": {
      "diceCount": 2,
      "diceType": "D10",
      "modifier": 5,
      "damageType": "PHYSICAL"
    },
    "expansionId": 1,
    "isPublic": false,
    "experienceIds": [1, 2],
    "featureIds": [5],
    "features": [
      {
        "name": "Shadow Breath",
        "description": "Exhales a cone of shadow energy",
        "featureType": "OTHER",
        "expansionId": 1
      },
      {
        "name": "Dark Resilience",
        "description": "Resistance to shadow damage",
        "featureType": "OTHER",
        "expansionId": 1,
        "costTags": [
          { "label": "Recharge 5-6", "category": "LIMITATION" }
        ],
        "modifiers": [
          { "target": "HIT_POINT_MAX", "operation": "ADD", "value": 5 }
        ]
      }
    ]
  }' \
  "http://localhost:8080/api/dh/adversaries"
```

**Example Response:**

```json
{
  "id": 3,
  "name": "Shadow Drake",
  "tier": 3,
  "adversaryType": "BRUISER",
  "description": "A drake wreathed in living shadow",
  "motivesAndTactics": "Targets the weakest party member first",
  "difficulty": 10,
  "majorThreshold": 5,
  "severeThreshold": 10,
  "hitPointMax": 30,
  "hitPointMarked": 0,
  "stressMax": 10,
  "stressMarked": 0,
  "attackModifier": 3,
  "weaponName": "Greataxe",
  "attackRange": "MELEE",
  "damage": {
    "diceCount": 2,
    "diceType": "D10",
    "modifier": 5,
    "damageType": "PHYSICAL",
    "notation": "2d10+5 phy"
  },
  "isOfficial": false,
  "isPublic": false,
  "expansionId": 1,
  "originalAdversaryId": null,
  "creatorId": 2,
  "experienceIds": [1, 2],
  "featureIds": [5, 10, 11],
  "createdAt": "2026-03-13T14:00:00",
  "lastModifiedAt": "2026-03-13T14:00:00",
  "deletedAt": null
}
```

**Error Responses:**

| Status | Condition                                                        |
|--------|------------------------------------------------------------------|
| `400`  | Validation error (missing required fields, invalid values)       |
| `500`  | Severe threshold less than major threshold (business rule error) |

---

### Batch Create Adversaries

```
POST /api/dh/adversaries/batch
```

**Authorization:** MODERATOR, ADMIN, or OWNER role required. Regular USER returns `403 Forbidden`.

Supports partial success: individual failures do not affect other creates.

**Request Body:** `BatchCreateAdversaryRequest` (JSON)

**Response:**

| Status | Condition                                    |
|--------|----------------------------------------------|
| `201`  | All adversaries created successfully         |
| `207`  | Partial success (some created, some failed)  |
| `400`  | All adversaries failed to create             |

**Example Request:**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "adversaries": [
      {
        "name": "Goblin 1",
        "tier": 1,
        "adversaryType": "MINION",
        "difficulty": 5,
        "majorThreshold": 3,
        "severeThreshold": 6,
        "expansionId": 1,
        "features": [
          {
            "name": "Batch Feature",
            "description": "Feature from batch",
            "featureType": "OTHER",
            "expansionId": 1
          }
        ]
      },
      {
        "name": "Goblin 2",
        "tier": 1,
        "adversaryType": "MINION",
        "difficulty": 5,
        "majorThreshold": 3,
        "severeThreshold": 6,
        "expansionId": 1
      }
    ]
  }' \
  "http://localhost:8080/api/dh/adversaries/batch"
```

**Example Response (all success):**

```json
{
  "created": [
    {
      "id": 4,
      "name": "Goblin 1",
      "tier": 1,
      "adversaryType": "MINION",
      "difficulty": 5,
      "majorThreshold": 3,
      "severeThreshold": 6,
      "hitPointMax": 0,
      "hitPointMarked": 0,
      "stressMax": 0,
      "stressMarked": 0,
      "isOfficial": false,
      "isPublic": false,
      "expansionId": 1,
      "creatorId": 3,
      "experienceIds": [],
      "featureIds": [12],
      "createdAt": "2026-03-13T15:00:00",
      "lastModifiedAt": "2026-03-13T15:00:00"
    },
    {
      "id": 5,
      "name": "Goblin 2",
      "tier": 1,
      "adversaryType": "MINION",
      "difficulty": 5,
      "majorThreshold": 3,
      "severeThreshold": 6,
      "hitPointMax": 0,
      "hitPointMarked": 0,
      "stressMax": 0,
      "stressMarked": 0,
      "isOfficial": false,
      "isPublic": false,
      "expansionId": 1,
      "creatorId": 3,
      "experienceIds": [],
      "featureIds": [],
      "createdAt": "2026-03-13T15:00:00",
      "lastModifiedAt": "2026-03-13T15:00:00"
    }
  ],
  "errors": [],
  "totalRequested": 2,
  "totalCreated": 2,
  "totalFailed": 0
}
```

**Example Response (partial failure):**

```json
{
  "created": [
    { "id": 6, "name": "Goblin 1", "..." : "..." }
  ],
  "errors": [
    {
      "index": 1,
      "name": "Invalid Goblin",
      "error": "Severe threshold must be >= major threshold"
    }
  ],
  "totalRequested": 2,
  "totalCreated": 1,
  "totalFailed": 1
}
```

---

### Copy Adversary

```
POST /api/dh/adversaries/{id}/copy
```

**Authorization:** Any authenticated user. The source adversary must be accessible (official, public, or owned by the user).

Creates a private copy linked to the original via `originalAdversaryId`. The copy appends ` (Copy)` to the name.

**Path Parameters:**

| Parameter | Type | Required | Description                     |
|-----------|------|----------|---------------------------------|
| `id`      | long | Yes      | ID of the adversary to copy     |

**Response:** `201 Created` with `AdversaryResponse`

**Example Request:**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/adversaries/1/copy"
```

**Example Response:**

```json
{
  "id": 7,
  "name": "Goblin (Copy)",
  "tier": 1,
  "adversaryType": "MINION",
  "difficulty": 5,
  "majorThreshold": 3,
  "severeThreshold": 6,
  "hitPointMax": 10,
  "hitPointMarked": 0,
  "stressMax": 5,
  "stressMarked": 0,
  "damage": {
    "diceCount": 1,
    "diceType": "D6",
    "modifier": 2,
    "damageType": "PHYSICAL",
    "notation": "1d6+2 phy"
  },
  "isOfficial": false,
  "isPublic": false,
  "expansionId": 1,
  "originalAdversaryId": 1,
  "creatorId": 2,
  "experienceIds": [],
  "featureIds": [],
  "createdAt": "2026-03-13T16:00:00",
  "lastModifiedAt": "2026-03-13T16:00:00",
  "deletedAt": null
}
```

**Error Responses:**

| Status | Condition                                                |
|--------|----------------------------------------------------------|
| `404`  | Source adversary not found or not accessible to the user |

---

### Update Adversary

```
PUT /api/dh/adversaries/{id}
```

**Authorization:**
- Official adversaries: OWNER role only
- Non-official adversaries: Creator OR MODERATOR+ role

All fields are optional (partial update).

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | long | Yes      | Adversary ID to update   |

**Request Body:** `UpdateAdversaryRequest` (JSON)

**Response:** `200 OK` with `AdversaryResponse`

**Example Request:**

```bash
curl -X PUT -b "AUTH_TOKEN=<token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Elite Goblin",
    "tier": 2,
    "features": [
      {
        "name": "Update Feature",
        "description": "Added via update",
        "featureType": "OTHER",
        "expansionId": 1
      }
    ]
  }' \
  "http://localhost:8080/api/dh/adversaries/1"
```

**Example Response:**

```json
{
  "id": 1,
  "name": "Elite Goblin",
  "tier": 2,
  "adversaryType": "MINION",
  "difficulty": 5,
  "majorThreshold": 3,
  "severeThreshold": 6,
  "hitPointMax": 10,
  "hitPointMarked": 0,
  "stressMax": 5,
  "stressMarked": 0,
  "isOfficial": false,
  "isPublic": false,
  "expansionId": 1,
  "creatorId": 2,
  "experienceIds": [],
  "featureIds": [13],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T17:00:00",
  "deletedAt": null
}
```

**Error Responses:**

| Status | Condition                                        |
|--------|--------------------------------------------------|
| `403`  | Non-creator/non-moderator or non-owner editing official |
| `404`  | Adversary not found                              |

---

### Delete Adversary (Soft Delete)

```
DELETE /api/dh/adversaries/{id}
```

**Authorization:**
- Official adversaries: OWNER role only
- Non-official adversaries: Creator OR MODERATOR+ role

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | long | Yes      | Adversary ID to delete   |

**Response:** `204 No Content`

**Example Request:**

```bash
curl -X DELETE -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/adversaries/1"
```

**Error Responses:**

| Status | Condition                                        |
|--------|--------------------------------------------------|
| `403`  | Non-creator/non-moderator or non-owner deleting official |
| `404`  | Adversary not found                              |

---

### Restore Adversary

```
POST /api/dh/adversaries/{id}/restore
```

**Authorization:** ADMIN or OWNER role required. Regular USER and MODERATOR return `403 Forbidden`.

**Path Parameters:**

| Parameter | Type | Required | Description               |
|-----------|------|----------|---------------------------|
| `id`      | long | Yes      | Adversary ID to restore   |

**Response:** `200 OK` with `AdversaryResponse`

**Example Request:**

```bash
curl -X POST -b "AUTH_TOKEN=<token>" \
  "http://localhost:8080/api/dh/adversaries/1/restore"
```

**Example Response:**

```json
{
  "id": 1,
  "name": "Goblin",
  "tier": 1,
  "adversaryType": "MINION",
  "difficulty": 5,
  "majorThreshold": 3,
  "severeThreshold": 6,
  "hitPointMax": 10,
  "hitPointMarked": 0,
  "stressMax": 5,
  "stressMarked": 0,
  "isOfficial": false,
  "isPublic": false,
  "expansionId": 1,
  "creatorId": 2,
  "experienceIds": [],
  "featureIds": [],
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T18:00:00",
  "deletedAt": null
}
```

**Error Responses:**

| Status | Condition          |
|--------|--------------------|
| `403`  | Insufficient role  |
| `404`  | Adversary not found|

---

## Models

### AdversaryResponse

Returned by all adversary endpoints. Fields with `@JsonInclude(NON_NULL)` are omitted when null.

| Field                | Type                       | Nullable | Description                                         |
|----------------------|----------------------------|----------|-----------------------------------------------------|
| `id`                 | long                       | No       | Unique identifier                                   |
| `name`               | string                     | No       | Adversary name                                      |
| `tier`               | integer                    | No       | Power tier (1-4)                                    |
| `adversaryType`      | AdversaryType              | No       | Tactical role                                       |
| `description`        | string                     | Yes      | Flavor text and appearance                          |
| `motivesAndTactics`  | string                     | Yes      | GM tactical guidance                                |
| `difficulty`         | integer                    | No       | Difficulty rating                                   |
| `majorThreshold`     | integer                    | No       | Major injury threshold                              |
| `severeThreshold`    | integer                    | No       | Severe injury threshold                             |
| `hitPointMax`        | integer                    | No       | Maximum hit points                                  |
| `hitPointMarked`     | integer                    | No       | Currently marked hit points                         |
| `stressMax`          | integer                    | No       | Maximum stress points                               |
| `stressMarked`       | integer                    | No       | Currently marked stress points                      |
| `attackModifier`     | integer                    | Yes      | Attack roll modifier                                |
| `weaponName`         | string                     | Yes      | Weapon or attack name                               |
| `attackRange`        | Range                      | Yes      | Effective attack range                              |
| `damage`             | DamageRollResponse         | Yes      | Damage roll details                                 |
| `isOfficial`         | boolean                    | No       | Whether from official content                       |
| `isPublic`           | boolean                    | No       | Whether publicly visible                            |
| `expansionId`        | long                       | No       | Expansion ID (always included)                      |
| `expansion`          | ExpansionResponse          | Yes      | Full expansion (with `?expand=expansion`)           |
| `originalAdversaryId`| long                       | Yes      | Source adversary ID if copied                       |
| `originalAdversary`  | AdversaryResponse          | Yes      | Full original (with `?expand=originalAdversary`)    |
| `creatorId`          | long                       | No       | Creator user ID (always included)                   |
| `creator`            | UserResponse               | Yes      | Full creator (with `?expand=creator`)               |
| `experienceIds`      | Set\<long\>                | No       | Experience IDs (always included)                    |
| `experiences`        | Set\<ExperienceResponse\>  | Yes      | Full experiences (with `?expand=experiences`)        |
| `featureIds`         | Set\<long\>                | No       | Feature IDs (always included)                       |
| `features`           | Set\<FeatureResponse\>     | Yes      | Full features (with `?expand=features`)             |
| `createdAt`          | datetime                   | No       | Creation timestamp                                  |
| `lastModifiedAt`     | datetime                   | No       | Last modification timestamp                         |
| `deletedAt`          | datetime                   | Yes      | Soft deletion timestamp (null if active)            |

### DamageRollResponse (nested in AdversaryResponse)

| Field        | Type       | Nullable | Description                                          |
|--------------|------------|----------|------------------------------------------------------|
| `diceCount`  | integer    | Yes      | Number of dice (null = use proficiency)              |
| `diceType`   | DiceType   | No       | Die type (D4, D6, D8, D10, D12, D20)                |
| `modifier`   | integer    | Yes      | Bonus/penalty to roll                                |
| `damageType` | DamageType | No       | Damage type (PHYSICAL, MAGIC)                        |
| `notation`   | string     | No       | Formatted notation (e.g., `"2d10+5 phy"`)           |

### CreateAdversaryRequest

| Field               | Type                       | Required | Validation                       | Description                                  |
|---------------------|----------------------------|----------|----------------------------------|----------------------------------------------|
| `name`              | string                     | Yes      | Not blank, max 200 chars         | Adversary name                               |
| `tier`              | integer                    | Yes      | 1-4                              | Power tier                                   |
| `adversaryType`     | AdversaryType              | Yes      | Valid enum value                 | Tactical role                                |
| `description`       | string                     | No       | --                               | Flavor text                                  |
| `motivesAndTactics` | string                     | No       | --                               | GM guidance                                  |
| `difficulty`        | integer                    | Yes      | >= 1                             | Difficulty rating                            |
| `majorThreshold`    | integer                    | Yes      | >= 1                             | Major injury threshold                       |
| `severeThreshold`   | integer                    | Yes      | >= 1, must be >= majorThreshold  | Severe injury threshold                      |
| `hitPointMax`       | integer                    | No       | >= 0 (default: 0)               | Maximum hit points                           |
| `stressMax`         | integer                    | No       | >= 0 (default: 0)               | Maximum stress points                        |
| `attackModifier`    | integer                    | No       | --                               | Attack roll modifier                         |
| `weaponName`        | string                     | No       | Max 200 chars                    | Weapon/attack name                           |
| `attackRange`       | Range                      | No       | Valid enum value                 | Attack range                                 |
| `damage`            | DamageRollRequest          | No       | Valid nested object              | Damage roll details                          |
| `expansionId`       | long                       | Yes      | Must exist                       | Expansion ID                                 |
| `originalAdversaryId`| long                      | No       | Must exist if provided           | Source adversary ID                          |
| `experienceIds`     | Set\<long\>                | No       | --                               | Existing experience IDs to link              |
| `featureIds`        | Set\<long\>                | No       | --                               | Existing feature IDs to link                 |
| `features`          | List\<FeatureInput\>       | No       | Valid nested objects             | Inline features to find-or-create, merged with featureIds |
| `isPublic`          | boolean                    | No       | Default: false                   | Public visibility                            |

### DamageRollRequest (nested in Create/UpdateAdversaryRequest)

| Field        | Type       | Required (Create) | Required (Update) | Description                    |
|--------------|------------|-------------------|--------------------|--------------------------------|
| `diceCount`  | integer    | No                | No                 | Number of dice                 |
| `diceType`   | DiceType   | Yes               | No                 | Die type                       |
| `modifier`   | integer    | No                | No                 | Bonus/penalty                  |
| `damageType` | DamageType | Yes               | No                 | Damage type                    |

### UpdateAdversaryRequest

All fields are optional for partial updates. Providing `experienceIds`, `featureIds`, or `features` replaces existing associations.

| Field               | Type                       | Required | Validation                       | Description                                  |
|---------------------|----------------------------|----------|----------------------------------|----------------------------------------------|
| `name`              | string                     | No       | Max 200 chars                    | Adversary name                               |
| `tier`              | integer                    | No       | 1-4                              | Power tier                                   |
| `adversaryType`     | AdversaryType              | No       | Valid enum value                 | Tactical role                                |
| `description`       | string                     | No       | --                               | Flavor text                                  |
| `motivesAndTactics` | string                     | No       | --                               | GM guidance                                  |
| `difficulty`        | integer                    | No       | >= 1                             | Difficulty rating                            |
| `majorThreshold`    | integer                    | No       | >= 1                             | Major injury threshold                       |
| `severeThreshold`   | integer                    | No       | >= 1                             | Severe injury threshold                      |
| `hitPointMax`       | integer                    | No       | >= 0                             | Maximum hit points                           |
| `hitPointMarked`    | integer                    | No       | >= 0                             | Currently marked hit points                  |
| `stressMax`         | integer                    | No       | >= 0                             | Maximum stress points                        |
| `stressMarked`      | integer                    | No       | >= 0                             | Currently marked stress points               |
| `attackModifier`    | integer                    | No       | --                               | Attack roll modifier                         |
| `weaponName`        | string                     | No       | Max 200 chars                    | Weapon/attack name                           |
| `attackRange`       | Range                      | No       | Valid enum value                 | Attack range                                 |
| `damage`            | DamageRollRequest          | No       | Valid nested object              | Damage roll details                          |
| `isPublic`          | boolean                    | No       | --                               | Public visibility                            |
| `experienceIds`     | Set\<long\>                | No       | --                               | Replaces existing experience associations    |
| `featureIds`        | Set\<long\>                | No       | --                               | Replaces existing feature associations       |
| `features`          | List\<FeatureInput\>       | No       | Valid nested objects             | Inline features to find-or-create, merged with featureIds |

### BatchCreateAdversaryRequest

| Field        | Type                          | Required | Validation            | Description                    |
|--------------|-------------------------------|----------|-----------------------|--------------------------------|
| `adversaries`| List\<CreateAdversaryRequest\>| Yes      | 1-100 items, each valid | List of adversaries to create |

### BatchCreateAdversaryResponse

| Field            | Type                     | Description                              |
|------------------|--------------------------|------------------------------------------|
| `created`        | List\<AdversaryResponse\>| Successfully created adversaries         |
| `errors`         | List\<BatchError\>       | Errors for failed adversaries            |
| `totalRequested` | integer                  | Total adversaries in the request         |
| `totalCreated`   | integer                  | Number successfully created              |
| `totalFailed`    | integer                  | Number that failed                       |

### BatchError (nested in BatchCreateAdversaryResponse)

| Field   | Type    | Description                                    |
|---------|---------|------------------------------------------------|
| `index` | integer | Zero-based index in the original request       |
| `name`  | string  | Name of the adversary that failed              |
| `error` | string  | Error message describing the failure           |

### FeatureInput (inline feature creation)

Used in `features` array of Create/Update requests. The service matches by name (case-insensitive) within the same expansion and type; creates a new feature if no match is found.

| Field         | Type                        | Required | Description                                    |
|---------------|-----------------------------|----------|------------------------------------------------|
| `name`        | string                      | No       | Feature name (max 200 chars)                   |
| `description` | string                      | No       | Feature description                            |
| `featureType` | FeatureType                 | Yes      | Feature category                               |
| `expansionId` | long                        | Yes      | Expansion ID                                   |
| `costTagIds`  | List\<long\>                | No       | Existing cost tag IDs                          |
| `costTags`    | List\<CostTagInput\>        | No       | Inline cost tags to find-or-create             |
| `modifierIds` | List\<long\>                | No       | Existing modifier IDs                          |
| `modifiers`   | List\<FeatureModifierInput\> | No       | Inline modifiers to find-or-create             |

### CostTagInput (nested in FeatureInput)

| Field      | Type            | Required | Description                                    |
|------------|-----------------|----------|------------------------------------------------|
| `label`    | string          | Yes      | Display label (e.g., `"3 Hope"`, `"1/session"`) |
| `category` | CostTagCategory | Yes      | Tag category (COST, LIMITATION, TIMING)        |

### FeatureModifierInput (nested in FeatureInput)

| Field       | Type              | Required | Description                                |
|-------------|-------------------|----------|--------------------------------------------|
| `target`    | ModifierTarget    | Yes      | Stat/attribute to modify                   |
| `operation` | ModifierOperation | Yes      | Math operation (ADD, SET, MULTIPLY)        |
| `value`     | integer           | Yes      | Modifier value                             |

### PagedResponse\<T\>

| Field           | Type    | Description                          |
|-----------------|---------|--------------------------------------|
| `content`       | List\<T\> | Items for the current page          |
| `totalElements` | long    | Total items across all pages         |
| `totalPages`    | integer | Total number of pages                |
| `currentPage`   | integer | Current page number (zero-based)     |
| `pageSize`      | integer | Number of items per page             |

---

## Expand Support

The `?expand` query parameter accepts a comma-separated list of relationship names. By default, only IDs are returned for relationships. When expanded, full nested objects are included.

| Expand Value        | Effect                                                    |
|---------------------|-----------------------------------------------------------|
| `expansion`         | Includes full `ExpansionResponse` in `expansion` field    |
| `creator`           | Includes full `UserResponse` in `creator` field           |
| `originalAdversary` | Includes full `AdversaryResponse` in `originalAdversary`  |
| `experiences`       | Includes full `ExperienceResponse` set in `experiences`   |
| `features`          | Includes full `FeatureResponse` set in `features`         |

**Example:** `?expand=expansion,creator,features`

Supported on: `GET /api/dh/adversaries`, `GET /api/dh/adversaries/{id}`

---

## Enums

### AdversaryType

Tactical role and combat behavior. Each type has an associated battle point value for encounter balancing.

| Value      | Battle Points | Description                                     |
|------------|:-------------:|-------------------------------------------------|
| `BRUISER`  | 4             | Tough melee combatants with high HP             |
| `HORDE`    | 2             | Multiple weak enemies that attack together      |
| `LEADER`   | 3             | Commanders that buff allies                     |
| `MINION`   | 1             | Basic enemies with minimal HP                   |
| `RANGED`   | 2             | Distance attackers                              |
| `SKULK`    | 2             | Stealthy enemies with evasion bonuses           |
| `SOCIAL`   | 1             | Non-combat focused adversaries                  |
| `SOLO`     | 5             | Single powerful enemy designed to fight alone   |
| `STANDARD` | 2             | Balanced general-purpose adversary              |
| `SUPPORT`  | 1             | Provides utility and healing to allies          |

### Range

| Value          | Description                            |
|----------------|----------------------------------------|
| `MELEE`        | Close-quarters, under 5 feet           |
| `VERY_CLOSE`   | Extended melee, 5-10 feet              |
| `CLOSE`        | Short throwing, 10-30 feet             |
| `FAR`          | Standard ranged, 30-100 feet           |
| `VERY_FAR`     | Long-range, 100-300 feet               |
| `OUT_OF_RANGE` | Beyond normal effectiveness, 300+ feet |

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

| Value      | Code  | Description                        |
|------------|-------|------------------------------------|
| `PHYSICAL` | `phy` | Physical damage from weapons/melee |
| `MAGIC`    | `mag` | Magic damage from spells/abilities |

### FeatureType

| Value       | Description              |
|-------------|--------------------------|
| `HOPE`      | Hope feature             |
| `ANCESTRY`  | Ancestry feature         |
| `CLASS`     | Class feature            |
| `COMMUNITY` | Community feature        |
| `DOMAIN`    | Domain feature           |
| `ITEM`      | Item feature             |
| `OTHER`     | Other/generic feature    |
| `SUBCLASS`  | Subclass feature         |

### CostTagCategory

| Value       | Description                                          |
|-------------|------------------------------------------------------|
| `COST`      | Resource expenditure (e.g., `"3 Hope"`, `"1 Stress"`)|
| `LIMITATION`| Restriction or requirement (e.g., `"Close range"`)   |
| `TIMING`    | Frequency or action type (e.g., `"1/session"`)       |

### ModifierTarget

| Value                      | Description                                    |
|----------------------------|------------------------------------------------|
| `AGILITY`                  | Modifies Agility trait score                   |
| `STRENGTH`                 | Modifies Strength trait score                  |
| `FINESSE`                  | Modifies Finesse trait score                   |
| `INSTINCT`                 | Modifies Instinct trait score                  |
| `PRESENCE`                 | Modifies Presence trait score                  |
| `KNOWLEDGE`                | Modifies Knowledge trait score                 |
| `EVASION`                  | Modifies Evasion defense value                 |
| `MAJOR_DAMAGE_THRESHOLD`   | Modifies Major damage threshold                |
| `SEVERE_DAMAGE_THRESHOLD`  | Modifies Severe damage threshold               |
| `HIT_POINT_MAX`            | Modifies maximum Hit Points                    |
| `STRESS_MAX`               | Modifies maximum Stress capacity               |
| `HOPE_MAX`                 | Modifies maximum Hope                          |
| `ARMOR_MAX`                | Modifies maximum Armor slots                   |
| `GOLD`                     | Modifies starting Gold                         |
| `ATTACK_ROLL`              | Modifies attack roll result                    |
| `DAMAGE_ROLL`              | Modifies damage roll result                    |
| `PRIMARY_DAMAGE_ROLL`      | Modifies primary damage roll result            |
| `ARMOR_SCORE`              | Modifies armor score                           |

### ModifierOperation

Evaluated in order: SET first, then MULTIPLY, then ADD.

| Value      | Description                                    |
|------------|------------------------------------------------|
| `ADD`      | Adds the value to the target attribute         |
| `SET`      | Sets the target attribute to the value         |
| `MULTIPLY` | Multiplies the target attribute by the value   |

### Role (for access control context)

| Value       | Description                          |
|-------------|--------------------------------------|
| `OWNER`     | Highest privilege                    |
| `ADMIN`     | Administrative access                |
| `MODERATOR` | Can bypass ownership checks          |
| `USER`      | Standard authenticated user          |

---

## Database Constraints

The `adversaries` table enforces these constraints at the database level:

| Constraint                         | Rule                                    |
|------------------------------------|-----------------------------------------|
| `check_tier_valid`                 | `tier >= 1 AND tier <= 4`              |
| `check_difficulty_positive`        | `difficulty > 0`                        |
| `check_major_threshold_positive`   | `major_threshold > 0`                   |
| `check_severe_threshold_positive`  | `severe_threshold > 0`                  |
| `check_severe_gte_major`           | `severe_threshold >= major_threshold`   |
| `check_hit_point_marked_lte_max`   | `hit_point_marked <= hit_point_max`     |
| `check_stress_marked_lte_max`      | `stress_marked <= stress_max`           |

### Foreign Key Behavior

| Relationship                      | On Delete |
|-----------------------------------|-----------|
| `original_adversary_id` -> `adversaries` | SET NULL  |
| `expansion_id` -> `expansions`    | CASCADE   |
| `creator_id` -> `users`           | CASCADE   |
| All join table references          | CASCADE   |

### Join Tables

| Table                   | Columns                         | Description                         |
|-------------------------|---------------------------------|-------------------------------------|
| `adversary_experiences` | `adversary_id`, `experience_id` | Many-to-many: adversaries to experiences |
| `adversary_features`    | `adversary_id`, `feature_id`    | Many-to-many: adversaries to features    |
