# Weapons API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/dh/weapons`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)

**Visibility:** list endpoints return only what the caller may see: official content, public
content, content they authored, content with no author (every imported row), or content
explicitly tagged to a campaign they are involved in. MODERATOR+ bypasses this filter.
There is no derived "we share a campaign so you see my things" rule — sharing is always a
deliberate act by the author.

`GET /{id}` is deliberately **not** filtered: any authenticated user can fetch any record by ID.
Custom items have to render on other people's character sheets and profiles, and they carry no
secrets. Restricting it would silently blank those pages.
**Content-Type:** `application/json`

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/dh/weapons` | Authenticated | List weapons (paginated, filterable) |
| 2 | GET | `/api/dh/weapons/{id}` | Authenticated | Get weapon by ID |
| 3 | POST | `/api/dh/weapons` | ADMIN, OWNER | Create a weapon |
| 4 | POST | `/api/dh/weapons/bulk` | ADMIN, OWNER | Create multiple weapons |
| 5 | PUT | `/api/dh/weapons/{id}` | Author, MODERATOR+, or ADMIN+ for official | Update a weapon |
| 6 | DELETE | `/api/dh/weapons/{id}` | Author, MODERATOR+, or ADMIN+ for official | Soft-delete a weapon |
| 7 | POST | `/api/dh/weapons/{id}/restore` | Author, MODERATOR+, or ADMIN+ for official | Restore a soft-deleted weapon |
| 8 | POST | `/api/dh/weapons/custom` | Authenticated | Create custom weapon owned by the caller |
| 9 | POST | `/api/dh/weapons/{id}/copy` | Authenticated | Copy any weapon into a new custom one |

---

## 1. GET `/api/dh/weapons`

List all active weapons with optional filters and pagination.

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |
| `includeDeleted` | boolean | `false` | No | Include soft-deleted weapons (MODERATOR+ only; 403 otherwise) |
| `expansionId` | Long | -- | No | Filter by expansion ID |
| `isOfficial` | Boolean | -- | No | Filter by official status |
| `trait` | Trait | -- | No | Filter by weapon trait |
| `range` | Range | -- | No | Filter by weapon range |
| `burden` | Burden | -- | No | Filter by weapon burden |
| `isPrimary` | Boolean | -- | No | Filter by primary/secondary |
| `tier` | Integer | -- | No | Filter by tier (1-4) |
| `damageType` | DamageType | -- | No | Filter by damage type (PHYSICAL, MAGIC) |
| `createdByUserId` | Long | -- | No | Narrow to one author. Filters within visibility; never widens it |
| `name` | String | -- | No | Case-insensitive substring match on the name. Filters within visibility; never widens it |
| `sort` | ItemSort | `ID` | No | Ordering: `ID` (insertion), `NAME`, `TIER`, `NEWEST`. `ID` puts official content first, so lists people read should ask for `NAME` |
| `expand` | String | -- | No | Comma-separated relationships to expand (see [Expand Parameter](#expand-parameter)) |

### Response: `200 OK`

```json
{
  "content": [ WeaponResponse, ... ],
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
| 500 | Invalid enum value in filter parameter (e.g., `damageType=INVALID`) |

---

## 2. GET `/api/dh/weapons/{id}`

Retrieve a single weapon by ID.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Weapon ID |

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `expand` | String | No | Comma-separated relationships to expand |

### Response: `200 OK`

```json
{
  "id": 1,
  "name": "Longsword",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": {
    "diceCount": 2,
    "diceType": "D10",
    "modifier": 3,
    "damageType": "PHYSICAL",
    "notation": "2d10+3 phy"
  },
  "featureIds": [1, 2],
  "originalWeaponId": null,
  "createdAt": "2026-01-24T20:00:00",
  "lastModifiedAt": "2026-01-24T20:00:00",
  "deletedAt": null
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 404 | Weapon not found (or soft-deleted) |

---

## 3. POST `/api/dh/weapons`

Create a new weapon. Requires ADMIN or OWNER role.

### Request Body: `CreateWeaponRequest`

```json
{
  "name": "Flaming Sword",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": {
    "diceCount": 2,
    "diceType": "D10",
    "modifier": 3,
    "damageType": "PHYSICAL"
  },
  "featureIds": [1],
  "features": [
    {
      "name": "Flame Burst",
      "description": "Deal extra fire damage",
      "featureType": "OTHER",
      "expansionId": 1,
      "costTags": [
        { "label": "1/rest", "category": "LIMITATION" }
      ]
    }
  ],
  "originalWeaponId": null
}
```

### Field Validation

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | Yes | Not blank, max 200 chars |
| `expansionId` | Long | Yes | Must reference an active expansion |
| `tier` | Integer | Yes | 1-4 |
| `isOfficial` | Boolean | Yes | |
| `isPrimary` | Boolean | Yes | |
| `trait` | Trait | Yes | See [Trait enum](#trait) |
| `range` | Range | Yes | See [Range enum](#range) |
| `burden` | Burden | Yes | See [Burden enum](#burden) |
| `damage` | DamageRollRequest | Yes | Nested object (see below) |
| `damage.diceCount` | Integer | No | Null = uses character proficiency |
| `damage.diceType` | DiceType | Yes | See [DiceType enum](#dicetype) |
| `damage.modifier` | Integer | No | Positive or negative bonus |
| `damage.damageType` | DamageType | Yes | See [DamageType enum](#damagetype) |
| `featureIds` | List\<Long\> | No | IDs of existing features to attach |
| `features` | List\<FeatureInput\> | No | Inline features to create and attach (merged with featureIds) |
| `originalWeaponId` | Long | No | Source weapon ID if this is a custom copy |

### FeatureInput (inline feature creation)

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | String | No | Max 200 chars |
| `description` | String | No | |
| `featureType` | FeatureType | Yes | See [FeatureType enum](#featuretype) |
| `expansionId` | Long | Yes | |
| `costTagIds` | List\<Long\> | No | IDs of existing cost tags |
| `costTags` | List\<CostTagInput\> | No | Inline cost tags to create (merged with costTagIds) |
| `modifierIds` | List\<Long\> | No | IDs of existing modifiers |
| `modifiers` | List\<FeatureModifierInput\> | No | Inline modifiers to create (merged with modifierIds) |

### CostTagInput (inline cost tag creation)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `label` | String | Yes | Display label (e.g., "1/rest", "1/session", "Close range") |
| `category` | CostTagCategory | Yes | See [CostTagCategory enum](#costtagcategory) |

### Response: `201 Created`

Returns a `WeaponResponse` object.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure (missing required fields, invalid values) |
| 401 | Unauthenticated |
| 403 | Insufficient role (not ADMIN/OWNER) |
| 404 | Referenced expansion not found |

---

## 4. POST `/api/dh/weapons/bulk`

Create multiple weapons in a single request. Requires ADMIN or OWNER role.

### Request Body

Array of `CreateWeaponRequest` objects (same schema as single create).

```json
[
  {
    "name": "Longsword",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isPrimary": true,
    "trait": "STRENGTH",
    "range": "MELEE",
    "burden": "ONE_HANDED",
    "damage": { "diceType": "D10", "damageType": "PHYSICAL" }
  },
  {
    "name": "Shortbow",
    "expansionId": 1,
    "tier": 1,
    "isOfficial": true,
    "isPrimary": true,
    "trait": "FINESSE",
    "range": "FAR",
    "burden": "TWO_HANDED",
    "damage": { "diceType": "D6", "damageType": "PHYSICAL" }
  }
]
```

### Response: `201 Created`

Returns an array of `WeaponResponse` objects.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure on any item |
| 401 | Unauthenticated |
| 403 | Insufficient role |

---

## 5. PUT `/api/dh/weapons/{id}`

Update an existing weapon. Requires ADMIN or OWNER role.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Weapon ID to update |

### Request Body: `UpdateWeaponRequest`

Same structure and validation as `CreateWeaponRequest`. All required fields must be provided (full replacement, not partial).

### Response: `200 OK`

Returns the updated `WeaponResponse`.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Weapon not found |

---

## 6. DELETE `/api/dh/weapons/{id}`

Soft-delete a weapon (sets `deletedAt` timestamp). Requires ADMIN or OWNER role.

### Response: `204 No Content`

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Weapon not found |

---

## 7. POST `/api/dh/weapons/{id}/restore`

Restore a soft-deleted weapon (clears `deletedAt`). Requires ADMIN or OWNER role.

### Response: `200 OK`

Returns the restored `WeaponResponse`.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Weapon is not deleted (IllegalStateException) |
| 401 | Unauthenticated |
| 403 | Insufficient role |
| 404 | Weapon not found |

---

## 8. POST `/api/dh/weapons/custom`

Create weapon authored by the calling user. Open to **any authenticated user**.

Distinct from `POST /api/dh/weapons`, which remains the admin import path and takes the stricter
`CreateWeaponRequest`. Keeping the two separate means an import payload that omits `isOfficial`
still fails loudly rather than silently landing as un-attributed homebrew.

### Request Body: `CreateCustomWeaponRequest`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Max 200 characters |
| `tier` | Integer | Yes | 1-4 |
| `isPublic` | Boolean | No | Visible to everyone. **Honoured only for MODERATOR+; silently coerced to false otherwise** |
| `campaignIds` | Long[] | No | Campaigns to share with. The caller must be involved in each; 403 otherwise |
| `isPrimary` | Boolean | Yes | Primary (true) or secondary (false) |
| `trait` | Trait | Yes | AGILITY, STRENGTH, FINESSE, INSTINCT, PRESENCE, KNOWLEDGE |
| `range` | Range | Yes | MELEE, VERY_CLOSE, CLOSE, FAR, VERY_FAR, OUT_OF_RANGE |
| `burden` | Burden | Yes | ONE_HANDED, TWO_HANDED |
| `damage` | DamageRollRequest | Yes | Dice type, optional modifier, damage type. Omit `diceCount` — it comes from Proficiency |
| `features` | FeatureInput[] | No | Inline features, max 20 |

**Fields deliberately absent:** `isOfficial` and `expansionId` (custom content is never canon and
belongs to no sourcebook) and `originalWeaponId` (set only by the copy endpoint, so a caller cannot
claim their creation derives from something it does not).

### Response: `201 Created`

`WeaponResponse` with `isOfficial: false`, `expansionId: null`, and `createdByUserId` set to the
caller.

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure (missing required field, tier out of range, >20 features) |
| 401 | Missing or invalid AUTH_TOKEN cookie |
| 403 | `campaignIds` names a campaign the caller is not involved in |
| 404 | A named campaign does not exist or has been deleted |

---

## 9. POST `/api/dh/weapons/{id}/copy`

Copy any existing record into new custom content owned by the caller. Open to **any
authenticated user**, including copies of official content — records are already readable by ID,
so restricting this would protect nothing.

The copy is always private and unofficial regardless of its source, carries no sourcebook, and
inherits **no** campaign tags: sharing is a decision the new owner makes for themselves. Features
are carried over. The name gains a `" (Copy)"` suffix and `originalWeaponId` points at the source.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | ID of the record to copy |

### Response: `201 Created`

### Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid AUTH_TOKEN cookie |
| 404 | Source not found or soft-deleted |

---

## Expand Parameter

The `?expand=` query parameter controls which related objects are embedded in the response. By default, only foreign-key IDs are returned. Pass a comma-separated list of relationship names to include full objects.

### Write responses always expand features

A single-record write -- create (custom or admin), update, copy, and restore -- returns the record
with `features` (and each feature's `costTags` and `modifiers`) already expanded, regardless of any
`expand` parameter. A client that seeds an editor from the write response has no other way to learn
them, and an omitted list reads as an emptied one: because a supplied `features` array replaces the
whole relation, the next update would send that empty list back and delete the features for real.

Bulk create is the exception and still returns unexpanded records.

### Supported Values

| Value | Effect |
|-------|--------|
| `expansion` | Include full `ExpansionResponse` in `expansion` field |
| `features` | Include full `FeatureResponse[]` in `features` field |
| `originalWeapon` | Include full `WeaponResponse` in `originalWeapon` field |

### Nested Expansion (via features)

When `features` is expanded, each `FeatureResponse` also supports nested expansion:

| Value | Effect |
|-------|--------|
| `costTags` | Include full `CardCostTagResponse[]` in each feature |
| `modifiers` | Include full `FeatureModifierResponse[]` in each feature |

### Example

```
GET /api/dh/weapons?expand=expansion,features
GET /api/dh/weapons/1?expand=expansion,features,originalWeapon
```

### Default Response (no expand)

```json
{
  "id": 1,
  "name": "Longsword",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": { "diceCount": 2, "diceType": "D10", "modifier": 3, "damageType": "PHYSICAL", "notation": "2d10+3 phy" },
  "featureIds": [1],
  "originalWeaponId": null,
  "createdAt": "...",
  "lastModifiedAt": "..."
}
```

### Expanded Response (`?expand=expansion,features`)

```json
{
  "id": 1,
  "name": "Longsword",
  "expansionId": 1,
  "expansion": {
    "id": 1,
    "name": "Core Rulebook",
    "isPublished": true,
    "createdAt": "...",
    "lastModifiedAt": "..."
  },
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": { "diceCount": 2, "diceType": "D10", "modifier": 3, "damageType": "PHYSICAL", "notation": "2d10+3 phy" },
  "featureIds": [1],
  "features": [
    {
      "id": 1,
      "name": "Keen Edge",
      "description": "...",
      "featureType": "OTHER",
      "expansionId": 1,
      "costTagIds": [],
      "modifierIds": [],
      "createdAt": "...",
      "lastModifiedAt": "..."
    }
  ],
  "originalWeaponId": null,
  "createdAt": "...",
  "lastModifiedAt": "..."
}
```

---

## Response DTOs

### WeaponResponse

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `id` | Long | Yes | Unique identifier |
| `name` | String | Yes | Weapon name |
| `expansionId` | Long | If non-null | Sourcebook ID. **Null for custom content**, which came from no book |
| `expansion` | ExpansionResponse | Only with `?expand=expansion` | Full expansion object |
| `tier` | Integer | Yes | Power tier (1-4) |
| `isOfficial` | Boolean | Yes | Official game content flag |
| `isPublic` | Boolean | Yes | Visible to every user (custom content only) |
| `createdByUserId` | Long | If non-null | Author. Null for imports; non-null with `isOfficial: false` marks homebrew |
| `campaignIds` | List\<Long\> | If non-empty | Campaigns this record is explicitly shared with |
| `isPrimary` | Boolean | Yes | Primary vs secondary weapon |
| `trait` | Trait | Yes | Attack trait |
| `range` | Range | Yes | Effective range |
| `burden` | Burden | Yes | Hands required |
| `damage` | DamageRollResponse | Yes | Damage roll info (nested) |
| `featureIds` | List\<Long\> | Yes (if non-null) | Associated feature IDs |
| `features` | List\<FeatureResponse\> | Only with `?expand=features` | Full feature objects |
| `originalWeaponId` | Long | If non-null | Source weapon for custom copies |
| `originalWeapon` | WeaponResponse | Only with `?expand=originalWeapon` | Full source weapon |
| `createdAt` | LocalDateTime | Yes | Creation timestamp |
| `lastModifiedAt` | LocalDateTime | Yes | Last update timestamp |
| `deletedAt` | LocalDateTime | If non-null | Soft-deletion timestamp |

**Note:** `@JsonInclude(NON_NULL)` is applied -- null fields are omitted from the JSON response.

### DamageRollResponse

| Field | Type | Description |
|-------|------|-------------|
| `diceCount` | Integer | Number of dice (null = use character proficiency) |
| `diceType` | DiceType | Die type |
| `modifier` | Integer | Roll modifier (null = no modifier) |
| `damageType` | DamageType | Physical or magic |
| `notation` | String | Human-readable notation (e.g., `"2d10+3 phy"`, `"d6+2 mag"`) |

### Notation Format

```
[count]dX[+/-modifier] [damageType]
```

| Example | Meaning |
|---------|---------|
| `2d10+3 phy` | Roll 2d10, add 3, physical damage |
| `d6+2 mag` | Roll proficiency d6s, add 2, magic damage |
| `3d12+5 phy` | Roll 3d12, add 5, physical damage |
| `d6-1 phy` | Roll proficiency d6s, subtract 1, physical damage |
| `2d12 phy` | Roll 2d12, no modifier, physical damage |

### ExpansionResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique identifier |
| `name` | String | Expansion name |
| `isPublished` | Boolean | Publication status |
| `createdAt` | LocalDateTime | Creation timestamp |
| `lastModifiedAt` | LocalDateTime | Last update timestamp |
| `deletedAt` | LocalDateTime | Soft-deletion timestamp |

### FeatureResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique identifier |
| `name` | String | Feature name |
| `description` | String | Detailed description |
| `featureType` | FeatureType | Category of feature |
| `expansionId` | Long | Owning expansion ID |
| `expansion` | ExpansionResponse | Full expansion (with nested expand) |
| `costTagIds` | List\<Long\> | Associated cost tag IDs |
| `costTags` | List\<CardCostTagResponse\> | Full cost tags (with nested expand) |
| `modifierIds` | List\<Long\> | Associated modifier IDs |
| `modifiers` | List\<FeatureModifierResponse\> | Full modifiers (with nested expand) |
| `createdAt` | LocalDateTime | Creation timestamp |
| `lastModifiedAt` | LocalDateTime | Last update timestamp |
| `deletedAt` | LocalDateTime | Soft-deletion timestamp |

### PagedResponse\<T\>

| Field | Type | Description |
|-------|------|-------------|
| `content` | List\<T\> | Items for the current page |
| `totalElements` | long | Total items across all pages |
| `totalPages` | int | Total number of pages |
| `currentPage` | int | Current page number (zero-based) |
| `pageSize` | int | Items per page |

---

## Enums

### ItemSort

| Value | Ordering |
|-------|----------|
| `ID` | Insertion order (default). Official content holds the low ids, so user-authored content sorts last |
| `NAME` | Alphabetical. The sensible choice for pickers and browse screens |
| `TIER` | Lowest tier first, alphabetical within a tier |
| `NEWEST` | Most recently created first |

An allowlist rather than a free-text Spring `sort` parameter: binding an arbitrary property path
from a query string would let a caller order by, and so infer, fields the response never exposes.

### Trait

| Value | Description | Examples |
|-------|-------------|----------|
| `AGILITY` | Quick reflexes, nimbleness, and coordination | Dodging attacks, acrobatics, sleight of hand, stealth |
| `STRENGTH` | Raw physical power and endurance | Melee attacks, athletics, breaking objects, carrying heavy loads |
| `FINESSE` | Precision, grace, and careful execution | Ranged attacks, lockpicking, crafting, precise movements |
| `INSTINCT` | Intuition, awareness, and natural understanding | Perception, survival, animal handling, reading situations |
| `PRESENCE` | Force of personality and social influence | Persuasion, intimidation, performance, leadership |
| `KNOWLEDGE` | Learning, reasoning, and mental acuity | Spellcasting, history, investigation, arcana |

### DiceType

| Value | Sides | Notation Code |
|-------|-------|---------------|
| `D4` | 4 | `d4` |
| `D6` | 6 | `d6` |
| `D8` | 8 | `d8` |
| `D10` | 10 | `d10` |
| `D12` | 12 | `d12` |
| `D20` | 20 | `d20` |

### DamageType

| Value | Notation Code | Description |
|-------|---------------|-------------|
| `PHYSICAL` | `phy` | Physical damage from weapons and melee attacks |
| `MAGIC` | `mag` | Magic damage from spells and magical abilities |

### Range

| Value | Description |
|-------|-------------|
| `MELEE` | Close-quarters combat, under 5 feet |
| `VERY_CLOSE` | Extended melee or point-blank range, 5-10 feet |
| `CLOSE` | Short throwing distance, 10-30 feet |
| `FAR` | Standard ranged weapon distance, 30-100 feet |
| `VERY_FAR` | Long-range projectile distance, 100-300 feet |
| `OUT_OF_RANGE` | Extreme distance, beyond 300 feet |

### Burden

| Value | Description |
|-------|-------------|
| `ONE_HANDED` | Can be wielded with one hand, allowing shield or off-hand weapon |
| `TWO_HANDED` | Requires both hands to wield effectively |

### FeatureType

| Value            |
|------------------|
| `HOPE`           |
| `ANCESTRY`       |
| `CLASS`          |
| `COMMUNITY`      |
| `DOMAIN`         |
| `ITEM`           |
| `OTHER`          |
| `SUBCLASS`       |
| `TRANSFORMATION` |
| `ENVIRONMENT`    |
| `CAMPAIGN_FRAME` |
| `BEASTFORM`      |
| `MARTIAL_STANCE` |
| `ADVERSARY`      |

### CostTagCategory

| Value | Description |
|-------|-------------|
| `COST` | Resource expenditure (e.g., "3 Hope", "1 Stress") |
| `LIMITATION` | Restriction or requirement (e.g., "Close range", "Requires Level 5") |
| `TIMING` | Frequency or action type (e.g., "1/session", "Action", "Reaction") |

---

## Database Schema

### `weapons` Table

Created by: `V20260124200018995__create_weapons_table.sql`
Modified by: `V20260228113319717__add_tier_to_items.sql`, `V20260228145241147__move_features_to_base_item.sql`

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | BIGSERIAL | PK | Auto-generated ID |
| `name` | VARCHAR(200) | NOT NULL | Weapon name |
| `tier` | INTEGER | NOT NULL | Power tier 1-4 (CHECK constraint) |
| `is_primary` | BOOLEAN | NOT NULL | Primary vs secondary |
| `trait` | VARCHAR(20) | NOT NULL | Attack trait enum |
| `range` | VARCHAR(20) | NOT NULL | Range category enum |
| `burden` | VARCHAR(20) | NOT NULL | Hands required enum |
| `dice_count` | INTEGER | NULL | Number of dice (null = proficiency) |
| `dice_type` | VARCHAR(10) | NOT NULL | Die type (D4-D20) |
| `modifier` | INTEGER | NULL | Roll modifier |
| `damage_type` | VARCHAR(10) | NOT NULL | PHYSICAL or MAGIC |
| `expansion_id` | BIGINT | NOT NULL | FK -> expansions(id) |
| `is_official` | BOOLEAN | NOT NULL | Official content flag (default: true) |
| `created_by_user_id` | BIGINT | NULL | FK -> users(id) |
| `original_weapon_id` | BIGINT | NULL | FK -> weapons(id), self-reference |
| `created_at` | TIMESTAMP | NOT NULL | |
| `last_modified_at` | TIMESTAMP | NOT NULL | |
| `deleted_at` | TIMESTAMP | NULL | Soft-delete marker |

### `weapon_features` Join Table

Created by: `V20260228145241147__move_features_to_base_item.sql`

| Column | Type | Description |
|--------|------|-------------|
| `weapon_id` | BIGINT | FK -> weapons(id), part of composite PK |
| `feature_id` | BIGINT | FK -> features(id), part of composite PK |

### Indexes

| Index | Columns | Condition |
|-------|---------|-----------|
| `idx_weapons_expansion` | `expansion_id` | |
| `idx_weapons_is_official` | `is_official` | |
| `idx_weapons_trait` | `trait` | |
| `idx_weapons_range` | `range` | |
| `idx_weapons_burden` | `burden` | |
| `idx_weapons_is_primary` | `is_primary` | |
| `idx_weapons_created_by` | `created_by_user_id` | |
| `idx_weapons_original` | `original_weapon_id` | |
| `idx_weapons_active` | `expansion_id, is_official` | WHERE `deleted_at IS NULL` |
| `idx_weapons_active_filters` | `trait, range, burden` | WHERE `deleted_at IS NULL` |
| `idx_weapon_features_weapon_id` | `weapon_id` | |
| `idx_weapon_features_feature_id` | `feature_id` | |

---

## Test Data Examples

### Weapons Used in Tests

| Name | Trait | Range | Burden | Tier | Damage | Context |
|------|-------|-------|--------|------|--------|---------|
| Longsword | STRENGTH | MELEE | ONE_HANDED | 1 | 2d10+3 phy | Standard melee weapon |
| Shortbow | FINESSE | FAR | TWO_HANDED | 1 | 2d10+3 phy | Ranged weapon |
| Magic Staff | INSTINCT / KNOWLEDGE | FAR | TWO_HANDED | 1-2 | 2d6+1 mag / d6+2 mag | Magic ranged weapon |
| Greater Longsword | STRENGTH | MELEE | TWO_HANDED | 2 | 3d12+5 phy | Upgraded weapon (update test) |
| Flaming Sword | STRENGTH | MELEE | ONE_HANDED | 1 | 2d10+3 phy | Weapon with inline feature |
| Enchanted Blade | STRENGTH | MELEE | ONE_HANDED | 1 | 2d10+3 mag | Physical weapon with magic damage |
| Tagged Weapon | FINESSE | MELEE | ONE_HANDED | 1 | 1d6 phy | Weapon with cost-tagged feature |

### Create Request (Minimal)

```json
{
  "name": "Longsword",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": {
    "diceType": "D10",
    "damageType": "PHYSICAL"
  }
}
```

### Create Request (Full with Features)

```json
{
  "name": "Flaming Sword",
  "description": "A sword wreathed in flame",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": {
    "diceCount": 2,
    "diceType": "D10",
    "modifier": 3,
    "damageType": "PHYSICAL"
  },
  "features": [
    {
      "name": "Flame Burst",
      "description": "Deal extra fire damage",
      "featureType": "OTHER",
      "expansionId": 1,
      "costTags": [
        { "label": "1/rest", "category": "LIMITATION" }
      ]
    }
  ]
}
```

### Create Request (Mixed featureIds + Inline Features)

```json
{
  "name": "Multi-Feature Sword",
  "expansionId": 1,
  "tier": 1,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "ONE_HANDED",
  "damage": { "diceCount": 1, "diceType": "D8", "damageType": "PHYSICAL" },
  "featureIds": [1],
  "features": [
    {
      "name": "Inline Feature",
      "featureType": "OTHER",
      "expansionId": 1
    }
  ]
}
```

### Update Request

```json
{
  "name": "Greater Longsword",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "STRENGTH",
  "range": "MELEE",
  "burden": "TWO_HANDED",
  "damage": {
    "diceCount": 3,
    "diceType": "D12",
    "modifier": 5,
    "damageType": "PHYSICAL"
  }
}
```

### Proficiency-Based Damage (null diceCount)

```json
{
  "name": "Magic Staff",
  "expansionId": 1,
  "tier": 2,
  "isOfficial": true,
  "isPrimary": true,
  "trait": "KNOWLEDGE",
  "range": "FAR",
  "burden": "TWO_HANDED",
  "damage": {
    "diceCount": null,
    "diceType": "D6",
    "modifier": 2,
    "damageType": "MAGIC"
  }
}
```

Response notation: `"d6+2 mag"` (no count prefix when proficiency-based).
