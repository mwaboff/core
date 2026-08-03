# Encounters API Reference

Base URL: `http://localhost:8080`

## Overview

Encounters represent groups of adversaries designed for combat scenarios in the Daggerheart TTRPG system. They track adversary compositions, Battle Point budget/spend for encounter balancing, tier levels, an optional environment (scene stat block), and support the official/public/custom content management pattern. Encounters support copying (for customization), soft deletion, and visibility filtering.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:**
- GET endpoints: All authenticated users (filtered by visibility -- official, public, or user's own)
- POST (create/copy): All authenticated users (creator is set to current user)
- PUT/DELETE: Creator OR MODERATOR+ for non-official; OWNER only for official encounters
- POST restore: ADMIN or OWNER only (`@PreAuthorize`)

**Note:** Covered by `EncounterServiceTest` (unit) and `EncounterControllerIntegrationTest` (integration).

**Running a saved encounter** (live per-adversary HP/Stress tracking during a fight) is a separate resource -- see `encounter-runs-api.md`.

---

## Endpoints

### GET /api/dh/encounters

Retrieves a paginated list of encounters. Returns encounters that are official, public, or created by the authenticated user.

**Authorization:** Any authenticated user

**Query Parameters:**

| Parameter       | Type    | Default | Description                                           |
|----------------|---------|---------|-------------------------------------------------------|
| page           | int     | 0       | Zero-based page number                                |
| size           | int     | 20      | Items per page (max: 100)                             |
| includeDeleted | boolean | false   | Include soft-deleted encounters (ADMIN+ only)         |
| campaignId     | Long    | -       | Filter by campaign ID                                 |
| tier           | Integer | -       | Filter by tier (1-4)                                  |
| isOfficial     | Boolean | -       | Filter by official status                             |
| name           | String  | -       | Filter by name (partial match, case-insensitive)      |
| expand         | string  | -       | Comma-separated relationships to expand               |

**Expand Options:** `creator`, `campaign`, `environment`, `originalEncounter`, `adversaryDetails`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Goblin Ambush",
      "description": "A group of goblins attacks the party on the forest road",
      "tier": 1,
      "isOfficial": false,
      "isPublic": true,
      "campaignId": null,
      "environmentId": null,
      "originalEncounterId": null,
      "creatorId": 1,
      "adversaries": [
        {
          "id": 1,
          "adversaryId": 5,
          "displayOrder": 0
        },
        {
          "id": 2,
          "adversaryId": 5,
          "displayOrder": 1
        },
        {
          "id": 3,
          "adversaryId": 7,
          "label": "Archer A",
          "displayOrder": 2
        }
      ],
      "partySize": 4,
      "adjustmentEasier": false,
      "adjustmentTwoPlusSolos": false,
      "adjustmentBonusDamage": false,
      "adjustmentLowerTier": false,
      "adjustmentNoElites": false,
      "adjustmentHarder": false,
      "suggestedBattlePoints": 14,
      "spentBattlePoints": 12,
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

**With `?expand=creator,adversaryDetails`:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "Goblin Ambush",
      "creatorId": 1,
      "creator": {
        "id": 1,
        "username": "player1",
        ...
      },
      "adversaries": [
        {
          "id": 1,
          "adversaryId": 5,
          "adversary": {
            "id": 5,
            "name": "Goblin Scout",
            ...
          }
        }
      ],
      ...
    }
  ],
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token

---

### GET /api/dh/encounters/{id}

Retrieves a single encounter by ID. Access is restricted to official, public, or user's own encounters.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|------------------------------------------|
| expand    | string | Comma-separated relationships to expand  |

**Expand Options:** `creator`, `campaign`, `environment`, `originalEncounter`, `adversaryDetails`

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "isOfficial": false,
  "isPublic": true,
  "campaignId": null,
  "environmentId": null,
  "originalEncounterId": null,
  "creatorId": 1,
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 5,
      "displayOrder": 0
    }
  ],
  "partySize": 4,
  "adjustmentEasier": false,
  "adjustmentTwoPlusSolos": false,
  "adjustmentBonusDamage": false,
  "adjustmentLowerTier": false,
  "adjustmentNoElites": false,
  "adjustmentHarder": false,
  "suggestedBattlePoints": 14,
  "spentBattlePoints": 4,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**With `?expand=environment` and a retiered instance:**

```json
{
  "id": 1,
  "environmentId": 9,
  "environment": {
    "id": 9,
    "name": "Collapsing Bridge",
    "tier": 1,
    "environmentType": "TRAVERSAL",
    ...
  },
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 5,
      "label": "Elite Bandit",
      "tierOverride": 3,
      "retieredStatistics": {
        "tier": 3,
        "attackModifier": 3,
        "difficulty": 17,
        "majorThreshold": 20,
        "severeThreshold": 32,
        "damageDiceRange": "3d8+3 - 3d12+5"
      },
      "displayOrder": 0
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Encounter does not exist or user cannot access it

---

### POST /api/dh/encounters

Creates a new encounter. The authenticated user becomes the creator.

**Authorization:** Any authenticated user

**Request Body:**

```json
{
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "campaignId": null,
  "environmentId": null,
  "isPublic": false,
  "partySize": 4,
  "adjustmentHarder": true,
  "adversaries": [
    {"adversaryId": 5, "label": "Archer A"},
    {"adversaryId": 5, "label": "Archer B"},
    {"adversaryId": 7, "tierOverride": 3}
  ]
}
```

**Field Validation:**

| Field                    | Type                    | Required | Default | Constraints                                     |
|--------------------------|-------------------------|----------|---------|--------------------------------------------------|
| name                     | String                  | Yes      | -       | Not blank, max 200 characters                    |
| description              | String                  | No       | -       | -                                                 |
| tier                     | Integer                 | No       | -       | 1-4 (null if multi-tier)                         |
| campaignId               | Long                    | No       | -       | Must reference existing campaign                 |
| environmentId            | Long                    | No       | -       | Must reference existing environment              |
| isPublic                 | Boolean                 | No       | false   | -                                                 |
| partySize                | Integer                 | No       | -       | 1-12; manually entered, never derived from a campaign roster |
| adjustmentEasier         | Boolean                 | No       | false   | -1 Battle Point                                   |
| adjustmentTwoPlusSolos   | Boolean                 | No       | false   | -2 Battle Points                                  |
| adjustmentBonusDamage    | Boolean                 | No       | false   | -2 Battle Points                                  |
| adjustmentLowerTier      | Boolean                 | No       | false   | +1 Battle Point                                   |
| adjustmentNoElites       | Boolean                 | No       | false   | +1 Battle Point                                   |
| adjustmentHarder         | Boolean                 | No       | false   | +2 Battle Points                                  |
| adversaries              | List\<AdversaryEntry\>  | No       | -       | Preferred over `adversaryIds` (see below)         |
| adversaryIds             | List\<Long\>            | No       | -       | **Deprecated** -- see below                       |

**AdversaryEntry fields:**

| Field        | Type    | Required | Constraints                        |
|--------------|---------|----------|-------------------------------------|
| adversaryId  | Long    | Yes      | Must reference existing adversary   |
| label        | String  | No       | Max 100 characters, e.g. "Archer A" |
| tierOverride | Integer | No       | 1-4; retiers this instance          |

**Note on `adversaries` vs. `adversaryIds`:** Each entry represents a single adversary instance; to include 2 Goblin Scouts (adversary ID 5), add two entries with `adversaryId: 5`. `adversaries` is preferred -- it also carries a per-instance GM `label` and `tierOverride`. The bare `adversaryIds` list (`[5, 5, 7]`) is kept **only** for backward compatibility with existing clients; if both fields are provided, `adversaries` wins and `adversaryIds` is ignored. Instances get a `displayOrder` assigned in list order starting at 0.

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party on the forest road",
  "tier": 1,
  "isOfficial": false,
  "isPublic": false,
  "creatorId": 1,
  "adversaries": [
    {"id": 1, "adversaryId": 5, "label": "Archer A", "displayOrder": 0},
    {"id": 2, "adversaryId": 5, "label": "Archer B", "displayOrder": 1},
    {"id": 3, "adversaryId": 7, "tierOverride": 3, "retieredStatistics": {
      "tier": 3, "attackModifier": 3, "difficulty": 17,
      "majorThreshold": 20, "severeThreshold": 32, "damageDiceRange": "3d8+3 - 3d12+5"
    }, "displayOrder": 2}
  ],
  "partySize": 4,
  "adjustmentHarder": true,
  "suggestedBattlePoints": 16,
  "spentBattlePoints": 6,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- Missing name, invalid tier, or partySize outside 1-12
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Referenced campaign, environment, or adversary does not exist

---

### POST /api/dh/encounters/{id}/copy

Creates a copy of an existing encounter for the authenticated user. The copy is private by default and linked to the original via `originalEncounterId`.

**Authorization:** Any authenticated user

**Path Parameters:**

| Parameter | Type | Description                    |
|-----------|------|--------------------------------|
| id        | Long | ID of the encounter to copy    |

**Response:** `201 Created` -- New encounter with `originalEncounterId` set to the source.

```json
{
  "id": 2,
  "name": "Goblin Ambush",
  "originalEncounterId": 1,
  "isOfficial": false,
  "isPublic": false,
  "creatorId": 2,
  ...
}
```

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Source encounter does not exist or user cannot access it

---

### PUT /api/dh/encounters/{id}

Updates an existing encounter. Supports partial updates.

**Authorization:**
- Official encounters: OWNER role only
- Non-official encounters: Creator OR MODERATOR+ role

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Request Body (all fields optional):**

```json
{
  "name": "Updated Goblin Ambush",
  "description": "More goblins join the fray",
  "tier": 2,
  "campaignId": 1,
  "environmentId": 9,
  "isPublic": true,
  "partySize": 5,
  "adjustmentHarder": true,
  "adversaries": [
    {"adversaryId": 5, "label": "Archer A"},
    {"adversaryId": 5, "label": "Archer B"},
    {"adversaryId": 7},
    {"adversaryId": 7},
    {"adversaryId": 8, "tierOverride": 2}
  ]
}
```

**Note on `adversaries` / `adversaryIds`:** If either is provided, it completely replaces the existing adversary list (same preference rule as create: `adversaries` wins if both are sent). The deprecated bare `adversaryIds` list still works for backward compatibility.

**Field Validation:**

| Field                    | Type                    | Constraints                                      |
|--------------------------|-------------------------|---------------------------------------------------|
| name                     | String                  | Max 200 characters                                |
| description              | String                  | -                                                  |
| tier                     | Integer                 | 1-4                                                |
| campaignId               | Long                    | Valid campaign ID or null                          |
| environmentId            | Long                    | Valid environment ID or null                        |
| isPublic                 | Boolean                 | -                                                  |
| partySize                | Integer                 | 1-12                                               |
| adjustmentEasier         | Boolean                 | -1 Battle Point when true                          |
| adjustmentTwoPlusSolos   | Boolean                 | -2 Battle Points when true                         |
| adjustmentBonusDamage    | Boolean                 | -2 Battle Points when true                         |
| adjustmentLowerTier      | Boolean                 | +1 Battle Point when true                          |
| adjustmentNoElites       | Boolean                 | +1 Battle Point when true                          |
| adjustmentHarder         | Boolean                 | +2 Battle Points when true                         |
| adversaries              | List\<AdversaryEntry\>  | Replaces all if provided; see AdversaryEntry above |
| adversaryIds             | List\<Long\>            | **Deprecated**; replaces all if provided           |

**Response:** `200 OK` -- Updated `EncounterResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter with given ID does not exist

---

### DELETE /api/dh/encounters/{id}

Soft deletes an encounter (sets `deletedAt` timestamp).

**Authorization:**
- Official encounters: OWNER role only
- Non-official encounters: Creator OR MODERATOR+ role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter with given ID does not exist

---

### POST /api/dh/encounters/{id}/restore

Restores a soft-deleted encounter.

**Authorization:** ADMIN or OWNER role required (`@PreAuthorize`)

**Response:** `200 OK` -- Restored `EncounterResponse`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- User does not have ADMIN or OWNER role
- `404 Not Found` -- Encounter with given ID does not exist

---

### POST /api/dh/encounters/{id}/adversaries

Adds a single adversary instance to an encounter.

**Authorization:** Same as PUT (creator or MODERATOR+ for non-official; OWNER for official)

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The encounter ID   |

**Query Parameters:**

| Parameter   | Type | Required | Description            |
|-------------|------|----------|------------------------|
| adversaryId | Long | Yes      | The adversary ID to add|

**Response:** `200 OK` -- Updated `EncounterResponse` with the new adversary included

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter or adversary does not exist

---

### DELETE /api/dh/encounters/{id}/adversaries/{encounterAdversaryId}

Removes a specific adversary instance from an encounter.

**Authorization:** Same as PUT (creator or MODERATOR+ for non-official; OWNER for official)

**Path Parameters:**

| Parameter            | Type | Description                              |
|----------------------|------|------------------------------------------|
| id                   | Long | The encounter ID                         |
| encounterAdversaryId | Long | The encounter adversary instance ID      |

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Insufficient permissions
- `404 Not Found` -- Encounter or encounter adversary does not exist

---

## Response DTOs

### EncounterResponse

```json
{
  "id": 1,
  "name": "Goblin Ambush",
  "description": "A group of goblins attacks the party",
  "tier": 1,
  "isOfficial": false,
  "isPublic": true,
  "campaignId": null,
  "campaign": null,
  "environmentId": null,
  "environment": null,
  "originalEncounterId": null,
  "originalEncounter": null,
  "creatorId": 1,
  "creator": null,
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 5,
      "adversary": null,
      "label": null,
      "tierOverride": null,
      "retieredStatistics": null,
      "displayOrder": 0
    }
  ],
  "partySize": null,
  "adjustmentEasier": false,
  "adjustmentTwoPlusSolos": false,
  "adjustmentBonusDamage": false,
  "adjustmentLowerTier": false,
  "adjustmentNoElites": false,
  "adjustmentHarder": false,
  "suggestedBattlePoints": 2,
  "spentBattlePoints": 2,
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:00:00",
  "deletedAt": null
}
```

`null` fields are omitted from JSON output (`@JsonInclude(NON_NULL)`). `suggestedBattlePoints` and `spentBattlePoints` are always present -- see [Battle Points](#battle-points) below.

### EncounterAdversaryResponse (nested)

| Field              | Type                        | Description                                                     |
|--------------------|-----------------------------|-------------------------------------------------------------------|
| id                 | Long                        | Encounter adversary instance ID                                   |
| adversaryId        | Long                        | The adversary ID (always included)                                |
| adversary          | AdversaryResponse           | Full adversary object (only with `?expand=adversaryDetails`)      |
| label              | String                      | GM nickname for this instance, e.g. "Archer A" (null if unset)    |
| tierOverride       | Integer                     | Retier target 1-4 (null if not retiered)                          |
| retieredStatistics | RetieredStatisticsResponse  | Derived stats for the effective tier (only present when `tierOverride` is set) |
| displayOrder       | Integer                     | Display order within the encounter's adversary list               |

### RetieredStatisticsResponse (nested)

Computed on read from the static retier table (`ImprovisedTierStatistics`) -- never stored, so it can never drift from the book. Only present on an `EncounterAdversaryResponse` whose `tierOverride` is set.

| Field           | Type    | Description                                                       |
|-----------------|---------|---------------------------------------------------------------------|
| tier            | Integer | The effective tier (equal to `tierOverride`)                       |
| attackModifier  | Integer | Attack modifier for this tier                                      |
| difficulty      | Integer | Difficulty for this tier                                           |
| majorThreshold  | Integer | Major damage threshold for this tier                               |
| severeThreshold | Integer | Severe damage threshold for this tier                              |
| damageDiceRange | String  | Printed damage dice range as display text, e.g. `"3d8+3 - 3d12+5"` (the book prints a range, not a single roll) |

---

## Database Schema

**Table:** `encounters`

| Column                     | Type         | Nullable | Notes                                  |
|-----------------------------|--------------|----------|-----------------------------------------|
| id                          | BIGSERIAL    | No       | Primary key                             |
| name                        | VARCHAR(200) | No       |                                          |
| description                 | TEXT         | Yes      |                                          |
| tier                        | INTEGER      | Yes      | 1-4, null for multi-tier                |
| is_official                 | BOOLEAN      | No       | Default false                           |
| is_public                   | BOOLEAN      | No       | Default false                           |
| original_encounter_id       | BIGINT       | Yes      | FK to encounters (SET NULL on delete)   |
| creator_id                  | BIGINT       | No       | FK to users (CASCADE)                   |
| campaign_id                 | BIGINT       | Yes      | FK to campaigns (SET NULL on delete)    |
| environment_id              | BIGINT       | Yes      | FK to environments (SET NULL on delete) |
| party_size                  | INTEGER      | Yes      | 1-12; manually entered, drives Battle Point math |
| adjustment_easier           | BOOLEAN      | No       | Default false; -1 Battle Point          |
| adjustment_two_plus_solos   | BOOLEAN      | No       | Default false; -2 Battle Points         |
| adjustment_bonus_damage     | BOOLEAN      | No       | Default false; -2 Battle Points         |
| adjustment_lower_tier       | BOOLEAN      | No       | Default false; +1 Battle Point          |
| adjustment_no_elites        | BOOLEAN      | No       | Default false; +1 Battle Point          |
| adjustment_harder           | BOOLEAN      | No       | Default false; +2 Battle Points         |
| deleted_at                  | TIMESTAMP    | Yes      | Null = active                           |
| created_at                  | TIMESTAMP    | No       | Auto-set                                |
| last_modified_at            | TIMESTAMP    | No       | Auto-set                                |

**Check Constraints:**
- `check_encounter_tier_valid` -- tier IS NULL OR (tier >= 1 AND tier <= 4)
- `check_encounter_party_size_valid` -- party_size IS NULL OR (party_size >= 1 AND party_size <= 12)

**Table:** `encounter_adversaries`

| Column           | Type          | Nullable | Notes                                       |
|------------------|---------------|----------|-----------------------------------------------|
| id               | BIGSERIAL     | No       | Primary key                                   |
| encounter_id     | BIGINT        | No       | FK to encounters (CASCADE)                    |
| adversary_id     | BIGINT        | No       | FK to adversaries (CASCADE)                   |
| label            | VARCHAR(100)  | Yes      | GM nickname for this instance                 |
| tier_override    | INTEGER       | Yes      | 1-4; retier target for this instance          |
| display_order    | INTEGER       | No       | Default 0                                     |
| created_at       | TIMESTAMP     | No       | Auto-set                                      |
| last_modified_at | TIMESTAMP     | No       | Auto-set                                      |

**Check Constraint:** `check_encounter_adversary_tier_override_valid` -- tier_override IS NULL OR (tier_override >= 1 AND tier_override <= 4)

**Note:** The original unique constraint on `(encounter_id, adversary_id)` was removed in migration `V20260130225724303` to allow multiple instances of the same adversary in one encounter -- one row per instance is deliberate, since per-instance label/retier/display-order (and eventually per-instance run state) all need a row of their own.

---

## Battle Points

Two values are calculated server-side and returned read-only:

- **`suggestedBattlePoints`** -- the budget: `(3 * partySize) + 2`, adjusted by whichever of the six `adjustment*` flags are set (`easier` -1, `twoPlusSolos` -2, `bonusDamage` -2, `lowerTier` +1, `noElites` +1, `harder` +2). A null or unset `partySize` is treated as 0.
- **`spentBattlePoints`** -- what the encounter's adversary instances actually cost. Every non-Minion instance costs its `adversaryType`'s fixed Battle Point value (1 for Social/Support, 2 for Horde/Ranged/Skulk/Standard, 3 for Leader, 4 for Bruiser, 5 for Solo). **Minions are billed per group**, not individually: `ceil(minionCount / partySize)`, so a party of 4 facing 8 Minions spends 2 points, not 8. A null or non-positive `partySize` is treated as 1 for this grouping, so it never divides by zero.

Both are computed by `BattlePointCalculator`, the single place this math lives (mirrored in the frontend for instant feedback, but the server value always wins on save).

---

## Test Examples

### Create Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{
    "name": "Goblin Ambush",
    "description": "A group of goblins attacks the party on the forest road",
    "tier": 1,
    "isPublic": false,
    "partySize": 4,
    "adversaries": [
      {"adversaryId": 5, "label": "Archer A"},
      {"adversaryId": 5, "label": "Archer B"},
      {"adversaryId": 7}
    ]
  }'
```

### Copy Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters/1/copy \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Get Encounters with Filters
```bash
curl "http://localhost:8080/api/dh/encounters?tier=1&isOfficial=true&name=goblin" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Get Encounter with All Expansions
```bash
curl "http://localhost:8080/api/dh/encounters/1?expand=creator,campaign,environment,adversaryDetails" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Update Encounter
```bash
curl -X PUT http://localhost:8080/api/dh/encounters/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<creator_jwt>" \
  -d '{
    "name": "Updated Goblin Ambush",
    "tier": 2,
    "isPublic": true,
    "partySize": 4,
    "adjustmentHarder": true,
    "adversaries": [
      {"adversaryId": 5, "label": "Archer A"},
      {"adversaryId": 5, "label": "Archer B"},
      {"adversaryId": 7},
      {"adversaryId": 7},
      {"adversaryId": 8, "tierOverride": 2}
    ]
  }'
```

### Add Adversary to Encounter
```bash
curl -X POST "http://localhost:8080/api/dh/encounters/1/adversaries?adversaryId=9" \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Remove Adversary Instance from Encounter
```bash
curl -X DELETE http://localhost:8080/api/dh/encounters/1/adversaries/3 \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Delete Encounter (soft delete)
```bash
curl -X DELETE http://localhost:8080/api/dh/encounters/1 \
  --cookie "AUTH_TOKEN=<creator_jwt>"
```

### Restore Encounter
```bash
curl -X POST http://localhost:8080/api/dh/encounters/1/restore \
  --cookie "AUTH_TOKEN=<admin_jwt>"
```
