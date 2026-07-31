# Beastforms API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/dh/beastforms`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)
**Content-Type:** `application/json`

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/dh/beastforms` | Authenticated | List beastforms (paginated, filterable) |
| 2 | GET | `/api/dh/beastforms/{id}` | Authenticated | Get beastform by ID |
| 3 | POST | `/api/dh/beastforms` | ADMIN, OWNER | Create a beastform |
| 4 | POST | `/api/dh/beastforms/bulk` | ADMIN, OWNER | Create multiple beastforms |
| 5 | PUT | `/api/dh/beastforms/{id}` | ADMIN, OWNER | Update a beastform |
| 6 | DELETE | `/api/dh/beastforms/{id}` | ADMIN, OWNER | Soft-delete a beastform |
| 7 | POST | `/api/dh/beastforms/{id}/restore` | ADMIN, OWNER | Restore a soft-deleted beastform |

Mutation endpoints are ADMIN/OWNER-gated, matching the Weapon/Armor/Loot catalog-content pattern
(beastform stat blocks are bulk-imported rulebook content, not user-authored content like
Adversary/Encounter). The `isOfficial`/`isPublic`/`originalBeastformId` fields exist on the schema
for a future user-facing customization feature (CORE-01b, currently deferred) and are exposed on the
request/response DTOs so that feature can be layered on later without a schema change.

---

## 1. GET `/api/dh/beastforms`

List all active beastforms with optional filters and pagination.

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |
| `includeDeleted` | boolean | `false` | No | Include soft-deleted beastforms (ADMIN+ only) |
| `expansionId` | Long | -- | No | Filter by expansion ID |
| `isOfficial` | Boolean | -- | No | Filter by official status |
| `isPublic` | Boolean | -- | No | Filter by public visibility |
| `expand` | String | -- | No | Comma-separated relationships to expand (see [Expand Parameter](#expand-parameter)) |

### Response: `200 OK`

```json
{
  "content": [ BeastformResponse, ... ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid AUTH_TOKEN cookie |

---

## 2. GET `/api/dh/beastforms/{id}`

Retrieve a single beastform by ID.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Beastform ID |

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `expand` | String | No | Comma-separated relationships to expand |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Wolf",
  "example": "A lean grey wolf",
  "advantages": "Advantage on Instinct Rolls to track prey",
  "evasion": 2,
  "tier": 1,
  "agilityModifier": 1,
  "strengthModifier": 0,
  "finesseModifier": 0,
  "instinctModifier": 1,
  "presenceModifier": 0,
  "knowledgeModifier": -1,
  "attackRange": "MELEE",
  "attackTrait": "AGILITY",
  "damage": {
    "diceCount": 1,
    "diceType": "D6",
    "modifier": null,
    "damageType": "PHYSICAL",
    "notation": "1d6 phy"
  },
  "expansionId": 1,
  "isOfficial": true,
  "isPublic": false,
  "featureIds": [1],
  "originalBeastformId": null,
  "createdAt": "2026-01-31T17:10:00",
  "lastModifiedAt": "2026-01-31T17:10:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 404 | Beastform not found (or soft-deleted) |

---

## 3. POST `/api/dh/beastforms`

Create a new beastform. Requires ADMIN or OWNER role.

### Request Body: `CreateBeastformRequest`

```json
{
  "name": "Wolf",
  "example": "A lean grey wolf",
  "advantages": "Advantage on Instinct Rolls to track prey",
  "evasion": 2,
  "tier": 1,
  "agilityModifier": 1,
  "strengthModifier": 0,
  "finesseModifier": 0,
  "instinctModifier": 1,
  "presenceModifier": 0,
  "knowledgeModifier": -1,
  "attackRange": "MELEE",
  "attackTrait": "AGILITY",
  "damage": {
    "diceCount": 1,
    "diceType": "D6",
    "damageType": "PHYSICAL"
  },
  "expansionId": 1,
  "isOfficial": true,
  "isPublic": false,
  "featureIds": [1],
  "features": [
    {
      "name": "Keen Senses",
      "description": "Advantage on Perception Rolls that rely on hearing or smell.",
      "featureType": "OTHER",
      "expansionId": 1
    }
  ],
  "originalBeastformId": null
}
```

### Field Validation

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 200 chars |
| `example` | String | No | Flavor text |
| `advantages` | String | No | Special benefits text |
| `evasion` | Integer | No | Evasion bonus while transformed; `null` if omitted (see note) |
| `tier` | Integer | Yes | Beastform tier, 1-4; `chk_beastforms_tier` CHECK constraint |
| `agilityModifier` | Integer | No | `null` if omitted (see note) |
| `strengthModifier` | Integer | No | `null` if omitted (see note) |
| `finesseModifier` | Integer | No | `null` if omitted (see note) |
| `instinctModifier` | Integer | No | `null` if omitted (see note) |
| `presenceModifier` | Integer | No | `null` if omitted (see note) |
| `knowledgeModifier` | Integer | No | `null` if omitted (see note) |
| `attackRange` | Range | No | `null` if omitted; see [Range enum](#range) |
| `attackTrait` | Trait | No | `null` if omitted; see [Trait enum](#trait) |
| `damage` | DamageRollRequest | No | Nested object; `null` if omitted |
| `damage.diceCount` | Integer | No | Null = uses character proficiency |
| `damage.diceType` | DiceType | Yes, if `damage` present | See [DiceType enum](#dicetype) |
| `damage.modifier` | Integer | No | Positive or negative bonus |
| `damage.damageType` | DamageType | Yes, if `damage` present | See [DamageType enum](#damagetype) |
| `expansionId` | Long | Yes | Must reference an active expansion |
| `isOfficial` | Boolean | Yes | |
| `isPublic` | Boolean | No | Defaults to `false` if omitted |
| `featureIds` | List\<Long\> | No | IDs of existing features to attach |
| `features` | List\<FeatureInput\> | No | Inline features to create and attach (merged with featureIds) |
| `originalBeastformId` | Long | No | Source beastform ID if this is a custom copy |

**Important — `evasion`, the six trait modifiers, `attackRange`, `attackTrait`, and `damage` are
all genuinely optional and are NOT defaulted to `0`/a value when omitted.** They persist as SQL
`NULL`. This exists because 2 of the 24 core-book beastform cards ("Legendary Beast", "Mythic
Beast") are "Evolved: upgrade an earlier pick" meta-cards that print no stat line at all — no
Evasion, no attack range/trait/damage, no trait bonus — their mechanical effect is prose in the
feature text that the player applies manually. `NULL` is the honest encoding of "this card prints
no value here"; it is deliberately distinct from an explicit `0`, since a column that silently
turns "omitted from the source data" into "the beastform grants +0" is exactly the defect that
left a large share of the loot catalog mis-tiered in prod (an earlier import omitted `tier` into a
`NOT NULL DEFAULT 1` column, and the default silently stood in as real data with nothing to flag
it). If an ordinary card genuinely has no bonus for a given trait, send that field explicitly as
`0` — the server does not infer it. `tier` is the only field in this group that stays required.
`damage`, when present at all, must be a complete roll — `diceType` and `damageType` inside it are
still required, since a partially-specified damage roll isn't meaningful.

### Response: `201 Created`

Returns a `BeastformResponse` object.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure (missing required fields, invalid values) |
| 401 | Unauthenticated |
| 403 | Insufficient role (not ADMIN/OWNER) |
| 404 | Referenced expansion or original beastform not found |

---

## 4. POST `/api/dh/beastforms/bulk`

Create multiple beastforms in a single request. Requires ADMIN or OWNER role.

### Request Body

Array of `CreateBeastformRequest` objects (same schema as single create).

```json
[
  {
    "name": "Wolf",
    "expansionId": 1,
    "isOfficial": true,
    "evasion": 2,
    "tier": 1,
    "attackRange": "MELEE",
    "attackTrait": "AGILITY",
    "damage": { "diceCount": 1, "diceType": "D6", "damageType": "PHYSICAL" }
  },
  {
    "name": "Bear",
    "expansionId": 1,
    "isOfficial": true,
    "evasion": 1,
    "tier": 1,
    "attackRange": "MELEE",
    "attackTrait": "STRENGTH",
    "damage": { "diceType": "D10", "damageType": "PHYSICAL" }
  },
  {
    "name": "Legendary Beast",
    "example": "Upgrade an earlier pick",
    "expansionId": 1,
    "isOfficial": true,
    "isPublic": true,
    "tier": 3,
    "features": [
      {
        "name": "Evolved",
        "description": "Upgrade the trait bonus, Evasion, and damage of a beastform you've already chosen.",
        "featureType": "OTHER",
        "expansionId": 1
      }
    ]
  }
]
```

The third item is a stat-less "Evolved" meta-card: `evasion`, `attackRange`, `attackTrait`,
`damage`, and all six trait modifiers are omitted entirely rather than sent as `0`/defaults. Its
mechanical effect lives only in the `features` prose; the player applies it manually.

### Response: `201 Created`

Returns an array of `BeastformResponse` objects.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure on any item |
| 401 | Unauthenticated |
| 403 | Insufficient role |

---

## 5. PUT `/api/dh/beastforms/{id}`

Update an existing beastform. Requires ADMIN or OWNER role. Only non-null fields in the request are
applied — this is a partial update, unlike Weapon's full-replacement `PUT`.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Beastform ID to update |

### Request Body: `UpdateBeastformRequest`

Same fields as `CreateBeastformRequest`, all optional.

### Response: `200 OK`

Returns the updated `BeastformResponse`.

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Beastform, expansion, or original beastform not found |

---

## 6. DELETE `/api/dh/beastforms/{id}`

Soft-delete a beastform (sets `deletedAt` timestamp). Requires ADMIN or OWNER role.

### Response: `204 No Content`

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Beastform not found |

---

## 7. POST `/api/dh/beastforms/{id}/restore`

Restore a soft-deleted beastform (clears `deletedAt`). Requires ADMIN or OWNER role.

### Response: `200 OK`

Returns the restored `BeastformResponse`.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Beastform is not deleted (IllegalStateException) |
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Beastform not found |

---

## Expand Parameter

The `?expand=` query parameter controls which related objects are embedded in the response. By
default, only foreign-key IDs are returned.

### Supported Values

| Value | Effect |
|-------|--------|
| `expansion` | Include full `ExpansionResponse` in `expansion` field |
| `features` | Include full `FeatureResponse[]` in `features` field |
| `originalBeastform` | Include full `BeastformResponse` in `originalBeastform` field |

### Example

```
GET /api/dh/beastforms?expand=expansion,features
GET /api/dh/beastforms/1?expand=expansion,features,originalBeastform
```

---

## Response DTOs

### BeastformResponse

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `id` | Long | Yes | Unique identifier |
| `name` | String | Yes | Beastform name |
| `example` | String | If non-null | Flavor text |
| `advantages` | String | If non-null | Special benefits text |
| `evasion` | Integer | If non-null | Evasion bonus while transformed; absent for stat-less "Evolved" cards |
| `tier` | Integer | Yes | Beastform tier (1-4) |
| `agilityModifier` | Integer | If non-null | AGILITY trait modifier while transformed |
| `strengthModifier` | Integer | If non-null | STRENGTH trait modifier while transformed |
| `finesseModifier` | Integer | If non-null | FINESSE trait modifier while transformed |
| `instinctModifier` | Integer | If non-null | INSTINCT trait modifier while transformed |
| `presenceModifier` | Integer | If non-null | PRESENCE trait modifier while transformed |
| `knowledgeModifier` | Integer | If non-null | KNOWLEDGE trait modifier while transformed |
| `attackRange` | Range | If non-null | Effective attack range; absent for stat-less "Evolved" cards |
| `attackTrait` | Trait | If non-null | Trait used for attack rolls; absent for stat-less "Evolved" cards |
| `damage` | DamageRollResponse | If non-null | Damage roll info (nested); absent for stat-less "Evolved" cards |
| `expansionId` | Long | Yes | Owning expansion ID |
| `expansion` | ExpansionResponse | Only with `?expand=expansion` | Full expansion object |
| `isOfficial` | Boolean | Yes | Official game content flag |
| `isPublic` | Boolean | Yes | Public visibility flag (custom content) |
| `featureIds` | List\<Long\> | If non-null | Associated feature IDs |
| `features` | List\<FeatureResponse\> | Only with `?expand=features` | Full feature objects |
| `originalBeastformId` | Long | If non-null | Source beastform for custom copies |
| `originalBeastform` | BeastformResponse | Only with `?expand=originalBeastform` | Full source beastform |
| `createdAt` | LocalDateTime | Yes | Creation timestamp |
| `lastModifiedAt` | LocalDateTime | Yes | Last update timestamp |
| `deletedAt` | LocalDateTime | If non-null | Soft-deletion timestamp |

**Note:** `@JsonInclude(NON_NULL)` is applied -- null fields are omitted from the JSON response.

See `references/weapons-api.md` for the shared `DamageRollResponse`/notation format,
`ExpansionResponse`, `FeatureResponse`, `PagedResponse<T>`, and the `Trait`/`Range`/`DiceType`/
`DamageType`/`FeatureType` enum tables — Beastform reuses all of these unchanged.

---

## Search Integration

`Beastform` is `@SearchIndexed(type = SearchableEntityType.BEASTFORM)` and is indexed on
name/example/advantages/features text, filterable by `expansionId`, `isOfficial`, `isPublic`, and
`createdByUserId` (see `references/search-api.md`). As of this API's introduction, `BEASTFORM`
fully participates in search like every other type:

- Creating, updating, soft-deleting, or restoring a beastform automatically upserts its
  `search_index` row via the same `EntityChangeEvent` mechanism used by every other searchable
  entity — no beastform-specific wiring is needed in `BeastformService`.
- `POST /api/admin/search/reindex?type=BEASTFORM` (OWNER only) fully rebuilds the Beastform search
  index from the `beastforms` table.
- `GET /api/search?q=...&types=BEASTFORM&expand=entity` resolves `expandedEntity` to a real
  `BeastformResponse` via `BeastformService.getBeastformById()`.

(Previously, both the reindex and expand paths were no-ops for `BEASTFORM` because no repository or
service existed for the entity — that gap is what this API closes.)

---

## Database Schema

### `beastforms` Table

Created by: `V20260131171054646__create_beastforms_table.sql`
Referenced by: `V20260131171113996__add_active_beastform_to_character_sheets.sql`
(`character_sheets.active_beastform_id`)

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | BIGSERIAL | PK | Auto-generated ID |
| `name` | VARCHAR(200) | NOT NULL | Beastform name |
| `example` | TEXT | NULL | Flavor text |
| `advantages` | TEXT | NULL | Special benefits text |
| `agility_modifier` | INTEGER | NOT NULL | Default 0 |
| `strength_modifier` | INTEGER | NOT NULL | Default 0 |
| `finesse_modifier` | INTEGER | NOT NULL | Default 0 |
| `instinct_modifier` | INTEGER | NOT NULL | Default 0 |
| `presence_modifier` | INTEGER | NOT NULL | Default 0 |
| `knowledge_modifier` | INTEGER | NOT NULL | Default 0 |
| `attack_range` | VARCHAR(20) | NOT NULL | Range category enum |
| `attack_trait` | VARCHAR(20) | NOT NULL | Attack trait enum |
| `damage_dice_count` | INTEGER | NULL | Number of dice (null = proficiency) |
| `damage_dice_type` | VARCHAR(10) | NOT NULL | Die type |
| `damage_modifier` | INTEGER | NULL | Roll modifier |
| `damage_type` | VARCHAR(10) | NOT NULL | PHYSICAL or MAGIC |
| `is_official` | BOOLEAN | NOT NULL | Official content flag (default: false) |
| `is_public` | BOOLEAN | NOT NULL | Public visibility flag (default: false) |
| `original_beastform_id` | BIGINT | NULL | FK -> beastforms(id), self-reference |
| `expansion_id` | BIGINT | NOT NULL | FK -> expansions(id) |
| `creator_id` | BIGINT | NOT NULL | FK -> users(id) |
| `deleted_at` | TIMESTAMP | NULL | Soft-delete marker |

### `beastform_features` Join Table

Created by: `V20260131171054646__create_beastforms_table.sql`

| Column | Type | Description |
|--------|------|-------------|
| `beastform_id` | BIGINT | FK -> beastforms(id), part of composite PK |
| `feature_id` | BIGINT | FK -> features(id), part of composite PK |

### Indexes

| Index | Columns |
|-------|---------|
| `idx_beastforms_expansion_id` | `expansion_id` |
| `idx_beastforms_creator_id` | `creator_id` |
| `idx_beastforms_deleted_at` | `deleted_at` |
| `idx_beastforms_is_official` | `is_official` |
| `idx_beastforms_is_public` | `is_public` |

No new migration was required for this API — both tables and all indexes already existed prior to
this work; only the application layer (repository, service, controller, DTOs) was added.
