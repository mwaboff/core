# Encounter Runs API Reference

Base URL: `http://localhost:8080`

## Overview

An encounter run is the server-side live state for *playing* a fight, distinct from the saved `Encounter` it was started from. Starting a run snapshots the encounter's adversary instances into a run-owned copy, so editing the saved encounter mid-fight cannot corrupt a run already in progress. All live combat state -- marked HP/Stress, defeated, GM notes -- lives on those snapshot rows, never on the catalog `Adversary`, which two instances of the same adversary share.

**Campaign-free by design.** `campaignId` is nullable throughout. A campaign is an optional tag that widens who else can see and mutate a run (its game masters) -- it is never required to start or play one. A user who belongs to no campaign at all can start and run a standalone fight from end to end.

**Runs are deliberately top-level** (`/api/dh/encounter-runs/...`), not nested under campaigns -- a nested path would imply a campaign is required. Only starting a run is nested under its source encounter (`POST /api/dh/encounters/{id}/runs`), since that is the one action that always has an encounter as its subject.

**Hard delete.** Unlike `Encounter`, a run has no `deletedAt` -- discarding a run permanently removes it, matching `Countdown`'s treatment of ephemeral GM/session state rather than durable content.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:** A single rule, with no "GM mode" branch:
> A run is visible and mutable to the user who started it (`startedBy`), plus -- only when `campaignId` is set -- that campaign's game masters (delegated to `CampaignService.hasGameMasterAccess`), plus any MODERATOR/ADMIN/OWNER regardless of campaign tag.

For a standalone run (`campaignId` null) this collapses to owner-or-moderator-only, which is exactly what lets a user with no campaign at all start and play a run.

**Note:** Covered by `EncounterRunServiceTest` (unit) and `EncounterRunControllerIntegrationTest` (integration).

---

## Endpoints

### POST /api/dh/encounters/{encounterId}/runs

Starts a run of an encounter, snapshotting its current adversary instances. **Omitting `campaignId` starts a standalone, campaign-free run.**

**Authorization:** Any user who can view the source encounter (delegated to `EncounterService.validateViewPermission` -- official/public encounters are open to anyone, private ones require the creator or MODERATOR+).

**Path Parameters:**

| Parameter   | Type | Description        |
|-------------|------|---------------------|
| encounterId | Long | The encounter to run|

**Request Body (optional; an absent or empty body starts a standalone run):**

```json
{
  "campaignId": null
}
```

| Field      | Type | Required | Description                                          |
|------------|------|----------|-------------------------------------------------------|
| campaignId | Long | No       | Tags the run to a campaign; must reference an active, non-ended campaign |

**Response:** `201 Created`

```json
{
  "id": 1,
  "encounterId": 5,
  "environmentId": 9,
  "campaignId": null,
  "startedById": 12,
  "status": "ACTIVE",
  "startedAt": "2026-08-02T21:00:00",
  "adversaries": [
    {
      "id": 1,
      "adversaryId": 7,
      "adversary": {
        "id": 7,
        "name": "Goblin Scout",
        "tier": 1,
        "adversaryType": "STANDARD",
        "difficulty": 11,
        "majorThreshold": 5,
        "severeThreshold": 10,
        "hitPointMax": 6,
        "stressMax": 3,
        "attackModifier": 1,
        "weaponName": "Shortbow",
        "attackRange": "FAR",
        "damage": {"diceCount": 1, "diceType": "D6", "modifier": 2, "damageType": "PHYSICAL", "notation": "1d6+2 phy"},
        "experienceIds": [3],
        "experiences": [
          {"id": 3, "description": "Ambush Tactics", "modifier": 2}
        ],
        "featureIds": [14],
        "features": [
          {"id": 14, "name": "Group Attack - Action", "description": "Spend a Fear to have this and up to two other Goblin Scouts attack as a group...", "featureType": "ADVERSARY", "expansionId": 1, "costTagIds": [], "modifierIds": []}
        ]
      },
      "label": "Archer A",
      "tierOverride": null,
      "hitPointsMarked": 0,
      "hitPointMax": 6,
      "stressMarked": 0,
      "stressMax": 3,
      "isDefeated": false,
      "note": null,
      "tokens": 0,
      "displayOrder": 0
    }
  ],
  "createdAt": "2026-08-02T21:00:00",
  "lastModifiedAt": "2026-08-02T21:00:00"
}
```

**Error Responses:**
- `400 Bad Request` -- `campaignId` references an ended campaign
- `401 Unauthorized` -- Missing or invalid JWT token
- `404 Not Found` -- Encounter does not exist or user cannot view it; or `campaignId` does not reference an active campaign

---

### GET /api/dh/encounter-runs/{runId}

Retrieves a single run. **Every instance's full adversary stat block is always expanded, including `features` and `experiences`** -- a GM needs the whole card (Passives, Actions, Reactions, and Experiences) to actually play the fight, so none of it is gated behind an `?expand=` parameter.

**Authorization:** `startedBy`, that campaign's GMs (if tagged), or MODERATOR+

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|--------------|
| runId     | Long | The run ID   |

**Response:** `200 OK` -- shape as above (POST response)

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller lacks access to the run
- `404 Not Found` -- Run does not exist

---

### GET /api/dh/encounter-runs

Lists the runs visible to the caller. Adversary stat blocks are **not** expanded here, to keep a multi-run listing lightweight.

**Authorization:**
- No `campaignId` -- any authenticated user (returns only their own runs)
- With `campaignId` -- requires game master-level access to that campaign

**Query Parameters:**

| Parameter  | Type              | Description                                                              |
|------------|-------------------|----------------------------------------------------------------------------|
| status     | ACTIVE\|COMPLETED | Optional filter                                                          |
| campaignId | Long              | Optional. **Omitting it lists the caller's own runs** (standalone page's "resume" list); providing it lists that campaign's tagged runs (the GM screen panel) |

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "encounterId": 5,
    "environmentId": 9,
    "campaignId": null,
    "startedById": 12,
    "status": "ACTIVE",
    "startedAt": "2026-08-02T21:00:00",
    "adversaries": [
      {"id": 1, "adversaryId": 7, "label": "Archer A", "hitPointsMarked": 0, "hitPointMax": 6,
       "stressMarked": 0, "stressMax": 3, "isDefeated": false, "tokens": 0, "displayOrder": 0}
    ],
    "createdAt": "2026-08-02T21:00:00",
    "lastModifiedAt": "2026-08-02T21:00:00"
  }
]
```

`environmentId` is a cheap scalar (the repository eagerly joins it, no per-run query) so, unlike the adversary stat block, it is **not** gated behind the single-run endpoint -- it appears on the list response too.

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- `campaignId` given but caller lacks game master access to it
- `404 Not Found` -- `campaignId` given but does not reference an active campaign

---

### PATCH /api/dh/encounter-runs/{runId}/adversaries/{instanceId}

Updates a single adversary instance's live state within a run: marked HP/Stress, defeated, and/or note. **Partial update** -- a null/omitted field is left unchanged. Every provided field is an **absolute value, never a delta** (there is no optimistic locking anywhere in this codebase; absolute values are how concurrent GM clicks or two open tabs resolve to last-write-wins instead of compounding).

**Authorization:** `startedBy`, that campaign's GMs (if tagged), or MODERATOR+

**Path Parameters:**

| Parameter  | Type | Description                        |
|------------|------|--------------------------------------|
| runId      | Long | The run ID                          |
| instanceId | Long | The run adversary instance ID       |

**Request Body:**

```json
{
  "hitPointsMarked": 4,
  "stressMarked": 2,
  "isDefeated": false,
  "note": "Flanking the party's rogue",
  "tokens": 1
}
```

**Field Validation:**

| Field           | Type    | Constraints                                                        |
|-----------------|---------|-----------------------------------------------------------------------|
| hitPointsMarked | Integer | >= 0. **Clamped** (not rejected) to the adversary's `hitPointMax` if it exceeds it |
| stressMarked    | Integer | >= 0. **Clamped** to the adversary's `stressMax` if it exceeds it     |
| isDefeated      | Boolean | -                                                                     |
| note            | String  | Max 2000 characters; sanitized before persistence                    |
| tokens          | Integer | >= 0 (400 if negative). **No maximum** -- unlike `hitPointsMarked`/`stressMarked` there is no ceiling to clamp against (Daggerheart Core ch. 4, "Adversary Tokens"; Hope & Fear's `Pool` can hold any number) |

**Response:** `200 OK` -- the full updated run (same shape as `GET /api/dh/encounter-runs/{runId}`)

**Error Responses:**
- `400 Bad Request` -- Negative `hitPointsMarked`/`stressMarked`; note over 2000 characters; the run is already `COMPLETED`; or the run's tagged campaign has ended
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller lacks access to the run
- `404 Not Found` -- Run or instance does not exist, or the instance does not belong to the run

---

### POST /api/dh/encounter-runs/{runId}/complete

Marks a run complete: sets `status` to `COMPLETED` and stamps `endedAt`.

**Authorization:** `startedBy`, that campaign's GMs (if tagged), or MODERATOR+

**Response:** `200 OK` -- the completed run

**Error Responses:**
- `400 Bad Request` -- The run is already completed, or its tagged campaign has ended
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller lacks access to the run
- `404 Not Found` -- Run does not exist

---

### DELETE /api/dh/encounter-runs/{runId}

Permanently discards a run (hard delete -- no `deletedAt`). Unlike the PATCH and complete endpoints, this does **not** check whether a tagged campaign has ended: discarding is cleanup, not play, matching `Countdown`'s and `Campaign#removeCharacterSheet`'s treatment of deletion as always allowed.

**Authorization:** `startedBy`, that campaign's GMs (if tagged), or MODERATOR+

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` -- Missing or invalid JWT token
- `403 Forbidden` -- Caller lacks access to the run
- `404 Not Found` -- Run does not exist

---

## Response DTOs

### EncounterRunResponse

| Field           | Type                              | Description                                                        |
|-----------------|------------------------------------|----------------------------------------------------------------------|
| id              | Long                               | Run ID                                                              |
| encounterId     | Long                               | The source encounter this run was started from                     |
| environmentId   | Long                               | The source encounter's environment ID, null if none set. Only the ID -- fetch the full stat block via `GET /api/dh/environments/{id}?expand=features` |
| campaignId      | Long                               | Null for a standalone run                                          |
| startedById     | Long                               | The user who started the run                                       |
| status          | ACTIVE \| COMPLETED                | -                                                                    |
| startedAt       | LocalDateTime                      | -                                                                    |
| endedAt         | LocalDateTime                      | Null while `ACTIVE`                                                |
| adversaries     | List\<EncounterRunAdversaryResponse\> | The run's snapshotted, live-tracked instances, in display order |
| createdAt       | LocalDateTime                      | -                                                                    |
| lastModifiedAt  | LocalDateTime                      | -                                                                    |

`null` fields are omitted from JSON output (`@JsonInclude(NON_NULL)`).

### EncounterRunAdversaryResponse (nested)

| Field              | Type                        | Description                                                                 |
|--------------------|------------------------------|--------------------------------------------------------------------------------|
| id                 | Long                        | Run adversary instance ID                                                    |
| adversaryId        | Long                        | The catalog adversary ID (always included)                                  |
| adversary          | AdversaryResponse           | Full stat block -- present on every endpoint that returns a single run (start, get, patch, complete), omitted only on the list endpoint (`GET /api/dh/encounter-runs`) |
| label              | String                      | GM nickname copied from the source instance at run start                    |
| tierOverride       | Integer                     | Retier target copied from the source instance, null if not retiered         |
| retieredStatistics | RetieredStatisticsResponse  | Derived stats for the effective tier (see `encounters-api.md`); only present when `tierOverride` is set. Retiering does not change `hitPointMax`/`stressMax` |
| hitPointsMarked    | Integer                     | Live, clamped to `hitPointMax`                                              |
| hitPointMax        | Integer                     | The adversary's HP max, included directly so bounds are available without expanding `adversary` |
| stressMarked       | Integer                     | Live, clamped to `stressMax`                                                |
| stressMax          | Integer                     | The adversary's Stress max                                                  |
| isDefeated         | Boolean                     | -                                                                            |
| note               | String                      | Free-text GM note for this instance during the run                          |
| tokens             | Integer                     | Adversary Tokens placed on this instance's stat block (Daggerheart Core ch. 4). **Not clamped to any maximum.** Always included on both the single-run and list endpoints |
| displayOrder       | Integer                     | Copied from the source instance at run start                               |

The `adversary` stat block on this DTO is the full catalog `AdversaryResponse` -- id, name, tier, type, description, motives/tactics, difficulty, thresholds, HP/Stress max, attack modifier, weapon, range, damage, **and `features`/`experiences`** (both the ID sets and the full expanded objects, unconditionally -- there is no `?expand=` parameter on this endpoint to gate them behind). Features and experiences are batch-loaded once per distinct adversary referenced by the run, not once per instance, so a run holding several copies of the same adversary costs one query per collection, not one per copy. It never includes `hitPointMarked`/`stressMarked` from the catalog `Adversary` -- those columns exist on `Adversary` but are never written to by a run; a run's live state lives entirely in `encounter_run_adversaries`.

**Known data issue (not specific to this endpoint):** `Feature.timing` is null on every row in the database, so it is always omitted from `features[].timing`. The Action/Reaction/Passive tag instead lives as a `" - Passive"` / `" - Action"` / `" - Reaction"` suffix on `features[].name`, as printed on the card and passed through as-is -- this endpoint does not parse or correct it.

---

## Database Schema

**Table:** `encounter_runs`

| Column           | Type         | Nullable | Notes                                                    |
|-------------------|--------------|----------|-------------------------------------------------------------|
| id                | BIGSERIAL    | No       | Primary key                                                 |
| encounter_id      | BIGINT       | No       | FK to encounters (CASCADE)                                  |
| campaign_id       | BIGINT       | Yes      | FK to campaigns (SET NULL on delete) -- null for a standalone run |
| started_by_id     | BIGINT       | No       | FK to users (CASCADE)                                       |
| status            | VARCHAR(20)  | No       | `ACTIVE` \| `COMPLETED`                                     |
| started_at        | TIMESTAMP    | Yes      | -                                                             |
| ended_at          | TIMESTAMP    | Yes      | Null while `ACTIVE`                                          |
| created_at        | TIMESTAMP    | No       | Auto-set                                                     |
| last_modified_at  | TIMESTAMP    | No       | Auto-set                                                     |

**Check Constraint:** `check_encounter_run_status` -- `status IN ('ACTIVE', 'COMPLETED')`

**Indexes:**
- `idx_encounter_runs_started_by_status` on `(started_by_id, status)` -- "my active runs"
- `idx_encounter_runs_campaign` on `(campaign_id) WHERE campaign_id IS NOT NULL` -- the GM screen panel's campaign-scoped list (partial: most runs are standalone)

No `deleted_at` -- runs hard-delete.

**Table:** `encounter_run_adversaries`

| Column             | Type          | Nullable | Notes                                                     |
|--------------------|---------------|----------|---------------------------------------------------------------|
| id                 | BIGSERIAL     | No       | Primary key                                                    |
| encounter_run_id   | BIGINT        | No       | FK to encounter_runs (CASCADE)                                |
| adversary_id       | BIGINT        | No       | FK to adversaries -- read-only stat block reference, never written to |
| label              | VARCHAR(100)  | Yes      | Copied from the source instance at run start                  |
| tier_override      | INTEGER       | Yes      | 1-4; copied from the source instance                          |
| hit_points_marked  | INTEGER       | No       | Default 0; clamped by the service to the adversary's `hitPointMax` |
| stress_marked      | INTEGER       | No       | Default 0; clamped by the service to the adversary's `stressMax` |
| is_defeated        | BOOLEAN       | No       | Default false                                                  |
| note               | TEXT          | Yes      | Free-text GM note                                              |
| tokens             | INTEGER       | No       | Default 0; floor-only (no ceiling -- a Pool can hold any number of tokens) |
| display_order      | INTEGER       | No       | Default 0; copied from the source instance                     |
| created_at         | TIMESTAMP     | No       | Auto-set                                                        |
| last_modified_at   | TIMESTAMP     | No       | Auto-set                                                        |

**Check Constraints:**
- `check_encounter_run_adversary_tier_override` -- `tier_override IS NULL OR tier_override BETWEEN 1 AND 4`
- `check_encounter_run_adversary_hit_points_marked` -- `hit_points_marked >= 0`
- `check_encounter_run_adversary_stress_marked` -- `stress_marked >= 0`
- `check_encounter_run_adversary_tokens` -- `tokens >= 0`

No `deleted_at` -- deleting the parent run cascades.

---

## Test Examples

### Start a Standalone Run
```bash
curl -X POST http://localhost:8080/api/dh/encounters/5/runs \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{}'
```

### Start a Run Tagged to a Campaign
```bash
curl -X POST http://localhost:8080/api/dh/encounters/5/runs \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{"campaignId": 3}'
```

### Get a Run
```bash
curl http://localhost:8080/api/dh/encounter-runs/1 \
  --cookie "AUTH_TOKEN=<jwt>"
```

### List My Own Runs
```bash
curl "http://localhost:8080/api/dh/encounter-runs?status=ACTIVE" \
  --cookie "AUTH_TOKEN=<jwt>"
```

### List a Campaign's Runs (GM screen panel)
```bash
curl "http://localhost:8080/api/dh/encounter-runs?campaignId=3" \
  --cookie "AUTH_TOKEN=<gm_jwt>"
```

### Mark Damage on an Instance
```bash
curl -X PATCH http://localhost:8080/api/dh/encounter-runs/1/adversaries/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{"hitPointsMarked": 4}'
```

### Place an Adversary Token (e.g. the `Slow` passive)
```bash
curl -X PATCH http://localhost:8080/api/dh/encounter-runs/1/adversaries/1 \
  -H "Content-Type: application/json" \
  --cookie "AUTH_TOKEN=<jwt>" \
  -d '{"tokens": 1}'
```

### Complete a Run
```bash
curl -X POST http://localhost:8080/api/dh/encounter-runs/1/complete \
  --cookie "AUTH_TOKEN=<jwt>"
```

### Discard a Run
```bash
curl -X DELETE http://localhost:8080/api/dh/encounter-runs/1 \
  --cookie "AUTH_TOKEN=<jwt>"
```
