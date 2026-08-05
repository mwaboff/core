# Companions API Reference

Base URL: `http://localhost:8080`

## Overview

Companions are per-character entities in the Daggerheart TTRPG system, granted primarily by the Beastbound Ranger's Companion feature. Each companion has its own attack, base combat stats, Stress tracking, Experiences, and a "Training" list of level-up options that improve those base stats over time.

**Base vs. derived stats:** a companion's four printed stats -- Evasion, Stress max, damage dice, and attack range -- are stored as **base** values (`baseEvasion`, `baseStressMax`, `baseDamageDice`, `baseAttackRange`) and never mutated by Training. The Training-adjusted values actually used in play (`evasion`, `stressMax`, `damageDice`, `attackRange`) are always computed at read time and never stored.

**Soft deletion:** `DELETE` archives a companion (sets `deletedAt`) rather than permanently removing it, so it can later be restored (e.g. a level-down that removes a multiclassed subclass, followed by a re-level that re-takes it). All read endpoints exclude soft-deleted companions.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:** Reads (`GET /api/dh/companions`, `GET /api/dh/companions/{id}`) are open to any authenticated user -- this matches the character sheet that already embeds these same companions unconditionally via `?expand=companions`. `GET /api/dh/companions` always requires a `characterSheetId` filter; there is no unfiltered global listing. Writes (create, update, delete, both Training endpoints) remain ownership-based: the caller must be the owning character sheet's owner OR hold MODERATOR/ADMIN/OWNER role. This is enforced in the service layer, not via `@PreAuthorize`.

---

## Endpoints

### GET /api/dh/companions

Retrieves a paginated list of a character sheet's active (non-soft-deleted) companions.

**Authorization:** Any authenticated user. `characterSheetId` is required and scopes the read to one sheet; there is no unfiltered listing.

**Query Parameters:**

| Parameter        | Type   | Default | Description                                      |
|-----------------|--------|---------|---------------------------------------------------|
| page            | int    | 0       | Zero-based page number                            |
| size            | int    | 20      | Items per page (max: 100)                         |
| characterSheetId| Long   | -       | **Required.** Character sheet to list companions for |
| expand          | string | -       | Comma-separated relationships to expand           |

**Expand Options:** `characterSheet`, `experiences`

**Response:** `200 OK` -- see `CompanionResponse` shape under `GET /{id}` below.

**Error Responses:**
- `400 Bad Request` -- `characterSheetId` missing
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- `characterSheetId` does not reference an existing, active character sheet

---

### GET /api/dh/companions/{id}

Retrieves a single active (non-soft-deleted) companion by ID.

**Authorization:** Any authenticated user.

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The companion ID   |

**Query Parameters:**

| Parameter | Type   | Description                              |
|-----------|--------|-------------------------------------------|
| expand    | string | Comma-separated relationships to expand   |

**Expand Options:** `characterSheet`, `experiences`

**Response:** `200 OK`

```json
{
  "id": 1,
  "characterSheetId": 1,
  "name": "Wolf",
  "description": "A loyal wolf companion",
  "evasion": 14,
  "baseEvasion": 12,
  "attackName": "Bite",
  "attackRange": "CLOSE",
  "baseAttackRange": "CLOSE",
  "damageDice": "D6",
  "baseDamageDice": "D6",
  "attackDiceCount": 2,
  "damageType": "PHYSICAL",
  "stressMax": 3,
  "baseStressMax": 3,
  "stressMarked": 0,
  "outOfScene": false,
  "origin": "MANUAL",
  "advancesOnLevelUp": true,
  "trainings": [
    {
      "id": 5,
      "option": "AWARE",
      "viciousAxis": null,
      "targetExperienceId": null,
      "acquiredAtLevel": 2
    }
  ],
  "remainingByOption": {
    "INTELLIGENT": 3,
    "LIGHT_IN_THE_DARK": 1,
    "CREATURE_COMFORT": 1,
    "ARMORED": 1,
    "VICIOUS": 3,
    "RESILIENT": 3,
    "BONDED": 1,
    "AWARE": 2
  },
  "createdAt": "2026-08-04T10:00:00",
  "lastModifiedAt": "2026-08-04T10:00:00"
}
```

`evasion`/`stressMax`/`damageDice`/`attackRange` are Training-derived; `baseEvasion`/`baseStressMax`/`baseDamageDice`/`baseAttackRange` are the printed values Training is applied on top of. `attackDiceCount` is the owning character's **live** Proficiency, never snapshotted. `trainings` and `remainingByOption` are always included, not expand-gated.

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Companion does not exist or is soft-deleted

---

### POST /api/dh/companions

Creates a new companion for a character.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Request Body:**

```json
{
  "characterSheetId": 1,
  "name": "Wolf",
  "description": "A loyal wolf companion",
  "evasion": 12,
  "attackName": "Bite",
  "attackRange": "CLOSE",
  "damageDice": "D6",
  "damageType": "PHYSICAL",
  "stressMax": 3,
  "stressMarked": 0
}
```

**Field Validation:**

| Field            | Type       | Required | Default   | Constraints                          |
|-----------------|------------|----------|-----------|-----------------------------------------|
| characterSheetId| Long       | Yes      | -         | Must reference an existing, active sheet |
| name            | String     | Yes      | -         | Max 200 characters, not blank          |
| description     | String     | No       | -         | Max 5000 characters                    |
| evasion         | Integer    | No       | 10        | 0-50                                   |
| attackName      | String     | Yes      | -         | Max 200 characters, not blank          |
| attackRange     | Range      | Yes      | -         | Valid Range enum value                 |
| damageDice      | DiceType   | Yes      | -         | Valid DiceType enum value              |
| damageType      | DamageType | No       | PHYSICAL  | `PHYSICAL` or `MAGIC` only -- `PHYSICAL_AND_MAGIC` is rejected (it's a per-attack weapon mechanic, not a companion's one-time choice) |
| stressMax       | Integer    | No       | 3         | 1-20                                   |
| stressMarked    | Integer    | No       | 0         | 0-20, and must not exceed `stressMax`  |

**Response:** `201 Created` -- `CompanionResponse` (see `GET /{id}` above)

**Error Responses:**
- `400 Bad Request` -- Missing/invalid required fields, out-of-bounds values, `stressMarked > stressMax`, or `damageType: "PHYSICAL_AND_MAGIC"`
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Referenced character sheet does not exist or is deleted

---

### PUT /api/dh/companions/{id}

Updates an existing companion's base stats. Supports partial updates -- only non-null fields are updated.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The companion ID   |

**Request Body (all fields optional):**

```json
{
  "name": "Shadow Wolf",
  "stressMarked": 2
}
```

**Field Validation:** Same bounds as `POST` above (evasion 0-50, stressMax 1-20, stressMarked 0-20 and must not exceed the companion's **Training-adjusted** stress max). `damageType`, if provided, is `PHYSICAL` or `MAGIC` only -- `PHYSICAL_AND_MAGIC` is rejected. Omitting `damageType` leaves the companion's existing choice unchanged.

**Response:** `200 OK` -- Updated `CompanionResponse`

**Error Responses:**
- `400 Bad Request` -- Out-of-bounds values, resulting `stressMarked` exceeds the derived stress max, or `damageType: "PHYSICAL_AND_MAGIC"`
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion does not exist or is soft-deleted

---

### DELETE /api/dh/companions/{id}

Soft-deletes a companion (sets `deletedAt`). The companion and its Training/Experience history are preserved for possible restoration; it is simply excluded from active reads.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion does not exist or is already soft-deleted

---

### POST /api/dh/companions/{id}/trainings

Adds a single Training selection to a companion, via the manual/GM path (independent of the character level-up flow). `acquiredAtLevel` is set automatically to the owning character sheet's current level -- Training added here is **never reversed by level-down**.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Path Parameters:**

| Parameter | Type | Description        |
|-----------|------|--------------------|
| id        | Long | The companion ID   |

**Request Body:**

```json
{
  "option": "VICIOUS",
  "viciousAxis": "DAMAGE_DIE"
}
```

| Field                | Type                   | Required                          | Notes |
|----------------------|-------------------------|-----------------------------------|-------|
| option               | CompanionTrainingOption | Yes                                | See enum table below |
| viciousAxis          | ViciousAxis              | Required iff `option == VICIOUS`  | `DAMAGE_DIE` or `RANGE` |
| targetExperienceId   | Long                     | Required iff `option == INTELLIGENT` | Must belong to this companion |

**Validation (all enforced server-side, cap-checked per-companion-lifetime, not per-tier):**
- The option must have a remaining selection (`remainingByOption[option] > 0`)
- `VICIOUS` requires `viciousAxis`, and that axis's derived value must not already be at its ladder cap (D12 for `DAMAGE_DIE`, `VERY_FAR` for `RANGE`)
- `INTELLIGENT` requires `targetExperienceId`, and it must reference an Experience belonging to this companion

**Response:** `201 Created` -- the updated `CompanionResponse`, reflecting the new Training and its effect on derived stats.

**Error Responses:**
- `400 Bad Request` -- Missing `option`, cap exceeded, missing/invalid `viciousAxis` or `targetExperienceId`, or axis already at cap
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion does not exist/is soft-deleted, or `targetExperienceId` does not belong to the companion

---

### DELETE /api/dh/companions/{id}/trainings/{trainingId}

Removes a single Training selection from a companion via the manual/GM path.

**Authorization:** Character sheet owner OR MODERATOR/ADMIN/OWNER role

**Path Parameters:**

| Parameter  | Type | Description                |
|------------|------|-----------------------------|
| id         | Long | The companion ID            |
| trainingId | Long | The Training selection ID   |

**Response:** `200 OK` -- the updated `CompanionResponse`. If the removed Training was `RESILIENT`, `stressMarked` is clamped to the newly-derived (lower) stress max.

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller is not the sheet owner and does not have MODERATOR+ role
- `404 Not Found` -- Companion or Training selection does not exist

---

## Enums

### CompanionTrainingOption

8 values, each with a printed per-companion-lifetime cap (`maxSelections`):

| Value               | Max | Effect |
|----------------------|-----|--------|
| INTELLIGENT          | 3   | Permanent +1 to a chosen companion Experience |
| LIGHT_IN_THE_DARK    | 1   | An additional Hope slot the character can mark |
| CREATURE_COMFORT     | 1   | Once per rest: gain a Hope, or both clear a Stress |
| ARMORED              | 1   | Companion damage can mark an Armor Slot instead of Stress |
| VICIOUS              | 3   | +1 step on the damage-die or range ladder (choose axis) |
| RESILIENT            | 3   | +1 Stress slot |
| BONDED               | 1   | Companion can save the character from their last Hit Point |
| AWARE                | 3   | Permanent +2 Evasion |

### CompanionOrigin

| Value            | Description |
|-------------------|--------------|
| SUBCLASS_FEATURE | Granted by a subclass feature (e.g. Beastbound's Companion). Soft-deleted/restorable on level-down. |
| GM_GRANTED       | Granted directly by a GM. |
| MANUAL           | Added manually by the sheet owner. Never removed by level-down. |

### ViciousAxis

| Value       | Ladder |
|--------------|--------|
| DAMAGE_DIE  | D6 -> D8 -> D10 -> D12 |
| RANGE       | MELEE -> VERY_CLOSE -> CLOSE -> FAR -> VERY_FAR |

### Range

6 values:

| Value        | Description                                            |
|--------------|----------------------------------------------------------|
| MELEE        | Close-quarters combat, under 5 feet                      |
| VERY_CLOSE   | Extended melee or point-blank range, 5-10 feet           |
| CLOSE        | Short throwing distance, 10-30 feet                       |
| FAR          | Standard ranged weapon distance, 30-100 feet              |
| VERY_FAR     | Long-range projectile distance, 100-300 feet               |
| OUT_OF_RANGE | Extreme distance beyond normal effectiveness, 300+ ft      |

### DamageType

3 values, but only 2 are valid for a companion:

| Value               | Code    | Valid for companions? |
|----------------------|---------|-------------------------|
| PHYSICAL             | phy     | Yes (default)           |
| MAGIC                | mag     | Yes                     |
| PHYSICAL_AND_MAGIC   | phy/mag | **No** -- rejected with `400 Bad Request`. This is the "Otherworldly" per-attack weapon mechanic (choose physical or magic on each hit); a companion's damage type is a one-time either/or choice made at creation, never both. |

### DiceType

6 values:

| Value | Sides | Code |
|-------|-------|------|
| D4    | 4     | d4   |
| D6    | 6     | d6   |
| D8    | 8     | d8   |
| D10   | 10    | d10  |
| D12   | 12    | d12  |
| D20   | 20    | d20  |

---

## Database Schema

**Table:** `companions`

| Column                    | Type         | Nullable | Notes                                  |
|----------------------------|--------------|----------|------------------------------------------|
| id                         | BIGSERIAL    | No       | Primary key                              |
| character_sheet_id         | BIGINT       | No       | FK to character_sheets (CASCADE)         |
| name                       | VARCHAR(200) | No       |                                            |
| description                | TEXT         | Yes      |                                            |
| base_evasion               | INTEGER      | No       | Default 10                               |
| attack_name                | VARCHAR(200) | No       |                                            |
| base_attack_range          | VARCHAR(50)  | No       | Range enum, default MELEE                |
| base_damage_dice           | VARCHAR(10)  | No       | DiceType enum, default D6                |
| damage_type                | VARCHAR(20)  | No       | DamageType enum, default PHYSICAL        |
| base_stress_max            | INTEGER      | No       | Default 3                                |
| stress_marked              | INTEGER      | No       | Default 0                                |
| origin                     | VARCHAR(30)  | No       | CompanionOrigin enum, default MANUAL     |
| origin_subclass_card_id    | BIGINT       | Yes      | FK to subclass_cards                     |
| advances_on_level_up       | BOOLEAN      | No       | Default true                             |
| deleted_at                 | TIMESTAMP    | Yes      | Soft-delete marker                       |
| created_at                 | TIMESTAMP    | No       | Auto-set                                 |
| last_modified_at           | TIMESTAMP    | No       | Auto-set                                 |

**Table:** `companion_trainings`

| Column                | Type      | Nullable | Notes                                      |
|-------------------------|-----------|----------|----------------------------------------------|
| id                     | BIGSERIAL | No       | Primary key                                  |
| companion_id           | BIGINT    | No       | FK to companions (CASCADE)                   |
| option                 | VARCHAR(40) | No     | CompanionTrainingOption enum                 |
| vicious_axis           | VARCHAR(20) | Yes    | ViciousAxis enum, set iff option = VICIOUS   |
| target_experience_id   | BIGINT    | Yes      | FK to experiences, set iff option = INTELLIGENT (ON DELETE SET NULL) |
| acquired_at_level      | INTEGER   | No       | Character level when acquired                |
| created_at             | TIMESTAMP | No       | Auto-set                                     |
| last_modified_at       | TIMESTAMP | No       | Auto-set                                     |

The `experiences` table has a nullable `companion_id` (FK to `companions`, CASCADE) alongside its existing nullable `character_sheet_id`, with a `chk_experience_single_owner` CHECK constraint enforcing exactly one owner is set.

---

## Test Examples

### Create Companion (as character sheet owner)
```bash
curl -X POST http://localhost:8080/api/dh/companions \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{
    "characterSheetId": 1,
    "name": "Wolf",
    "description": "A loyal wolf companion",
    "attackName": "Bite",
    "attackRange": "CLOSE",
    "damageDice": "D6",
    "evasion": 12,
    "stressMax": 3,
    "stressMarked": 0
  }'
```

### List a Character's Companions (characterSheetId required)
```bash
curl "http://localhost:8080/api/dh/companions?characterSheetId=1" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Add a Training Selection
```bash
curl -X POST http://localhost:8080/api/dh/companions/1/trainings \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{"option": "AWARE"}'
```

### Remove a Training Selection
```bash
curl -X DELETE http://localhost:8080/api/dh/companions/1/trainings/5 \
  --cookie "AUTH_TOKEN=<owner_jwt>"
```

### Update Companion (partial update -- mark stress)
```bash
curl -X PUT http://localhost:8080/api/dh/companions/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<owner_jwt>" \
  -d '{"stressMarked": 2}'
```

### Soft-Delete Companion
```bash
curl -X DELETE http://localhost:8080/api/dh/companions/1 \
  --cookie "AUTH_TOKEN=<owner_jwt>"
```
