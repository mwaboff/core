# Search API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/search`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)
**Content-Type:** `application/json`

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/search` | Authenticated | Full-text search across all indexed game content |

---

## 1. GET `/api/search`

Performs a full-text search across all indexed Daggerheart game content using PostgreSQL
`tsvector` / `plainto_tsquery('english', ...)`. Results are ranked by relevance score
(descending) and paginated. Access control is enforced transparently: non-privileged users
only receive results for content they are permitted to view (official, public, or their own
content). Privileged users (MODERATOR and above) bypass these restrictions.

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `q` | String | -- | **Yes** | Search query string. Natural language input is supported (e.g., `flame sword`). Converted to a PostgreSQL `tsquery` via `plainto_tsquery`. |
| `types` | List\<SearchableEntityType\> | -- | No | Comma-separated list of entity types to restrict results (e.g., `WEAPON,ARMOR`). Omit to search all types. |
| `tier` | Integer | -- | No | Filter by tier level. Applicable to cards and adversaries. |
| `expansionId` | Long | -- | No | Filter by expansion foreign key (the `id` of an expansion resource). |
| `isOfficial` | Boolean | -- | No | `true` — only official content; `false` — only custom/community content. |
| `cardType` | String | -- | No | Filter by card type (e.g., `ANCESTRY`, `DOMAIN`, `COMMUNITY`, `SUBCLASS`). |
| `featureType` | String | -- | No | Filter by feature type (e.g., `CLASS_FEATURE`, `SUBCLASS_FEATURE`). Applicable to `FEATURE` type. |
| `adversaryType` | String | -- | No | Filter by adversary role (e.g., `MINION`, `STANDARD`, `LEADER`, `SOLO`, `BOSS`). Applicable to `ADVERSARY` type. |
| `domainCardType` | String | -- | No | Filter by domain card type (e.g., `ABILITY`, `SPELL`). Applicable to `DOMAIN_CARD` type. |
| `associatedDomainId` | Long | -- | No | Filter by associated domain primary key. Applicable to `DOMAIN_CARD`, `FEATURE`, and related types. |
| `trait` | String | -- | No | Filter by trait (e.g., `AGILITY`, `STRENGTH`, `FINESSE`, `INSTINCT`, `KNOWLEDGE`, `PRESENCE`). Applicable to `WEAPON` type. |
| `range` | String | -- | No | Filter by range (e.g., `MELEE`, `RANGED`, `VERY_CLOSE`, `CLOSE`, `FAR`). Applicable to `WEAPON` type. |
| `burden` | String | -- | No | Filter by burden (e.g., `ONE_HANDED`, `TWO_HANDED`, `NO_HANDS`). Applicable to `WEAPON` and `ARMOR` types. |
| `isConsumable` | Boolean | -- | No | `true` — only consumable items; `false` — non-consumable only. Applicable to `LOOT` type. |
| `expand` | String | -- | No | Pass `entity` or `all` to include full entity response DTOs in each result's `expandedEntity` field. |
| `page` | int | `0` | No | Zero-based page number. |
| `size` | int | `20` | No | Items per page. Maximum `100`; values above `100` are clamped to `100`. |

### Filter Applicability by Entity Type

Not all filters apply to every entity type. Irrelevant filters are silently ignored.

| Filter | Applicable Types |
|--------|-----------------|
| `tier` | `ANCESTRY_CARD`, `COMMUNITY_CARD`, `SUBCLASS_CARD`, `DOMAIN_CARD`, `ADVERSARY` |
| `expansionId` | All types |
| `isOfficial` | All types |
| `cardType` | `ANCESTRY_CARD`, `COMMUNITY_CARD`, `SUBCLASS_CARD`, `DOMAIN_CARD` |
| `featureType` | `FEATURE` |
| `adversaryType` | `ADVERSARY` |
| `domainCardType` | `DOMAIN_CARD` |
| `associatedDomainId` | `DOMAIN_CARD`, `FEATURE` |
| `trait` | `WEAPON` |
| `range` | `WEAPON` |
| `burden` | `WEAPON`, `ARMOR` |
| `isConsumable` | `LOOT` |

### SearchableEntityType Values

| Value | Description |
|-------|-------------|
| `DOMAIN` | Game domains (e.g., Blade, Codex, Grace) |
| `CLASS` | Playable character classes |
| `FEATURE` | Class or subclass features |
| `ANCESTRY_CARD` | Ancestry cards representing character lineage |
| `COMMUNITY_CARD` | Community cards representing character background |
| `SUBCLASS_CARD` | Subclass selection cards |
| `DOMAIN_CARD` | Domain ability cards |
| `WEAPON` | Weapon items |
| `ARMOR` | Armor items |
| `LOOT` | Loot and miscellaneous items |
| `ADVERSARY` | Adversaries (NPCs and enemies) |
| `BEASTFORM` | Beastform transformations (expansion is not supported; `expandedEntity` is always `null`) |
| `ENCOUNTER` | Pre-built encounters |
| `EXPANSION` | Content expansions / source books |
| `SUBCLASS_PATH` | Subclass progression paths |
| `QUESTION` | Character creation questions |
| `CARD_COST_TAG` | Tags that describe card costs |

### Response Schemas

#### `SearchResponse`

| Field | Type | Description |
|-------|------|-------------|
| `results` | `SearchResultResponse[]` | Matched results for the current page, ranked by relevance score descending. |
| `totalElements` | long | Total matching entities across all pages. |
| `totalPages` | int | Total number of pages based on the effective page size. |
| `currentPage` | int | Zero-based index of the current page. |
| `pageSize` | int | Maximum results per page (reflects the clamped `size` value). |
| `query` | String | The original search query string submitted by the caller. |

#### `SearchResultResponse`

| Field | Type | Description |
|-------|------|-------------|
| `type` | `SearchableEntityType` | The type of the matched entity. |
| `id` | Long | Primary key of the matched entity in its own table. |
| `name` | String | Display name of the matched entity. |
| `relevanceScore` | Double | Relevance score assigned by PostgreSQL (higher is more relevant). May be `null` when ranking is not applicable. |
| `expandedEntity` | Object | Full entity response DTO. Populated only when `expand=entity` or `expand=all` is passed. The concrete type depends on `type` (e.g., `WeaponResponse` for `WEAPON`). `null` otherwise. |

### Example Requests

```
# Keyword search across all entity types
GET /api/search?q=flame+sword

# Filter by entity type and tier
GET /api/search?q=flame&types=WEAPON,ARMOR&tier=2

# Filter by adversary type
GET /api/search?q=dragon&types=ADVERSARY&adversaryType=BOSS

# Include full entity DTOs in results
GET /api/search?q=dragon&expand=entity

# Paginate through results
GET /api/search?q=sword&page=1&size=10
```

### Response: `200 OK`

```json
{
  "results": [
    {
      "type": "WEAPON",
      "id": 42,
      "name": "Flame Sword",
      "relevanceScore": 0.0759,
      "expandedEntity": null
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "query": "flame"
}
```

### Response with `expand=entity`: `200 OK`

When `expand=entity` (or `expand=all`) is passed, the `expandedEntity` field is populated
with the full entity response DTO for each result. The concrete type of `expandedEntity`
depends on the matched entity type (e.g., `WeaponResponse` for `WEAPON`).

```json
{
  "results": [
    {
      "type": "WEAPON",
      "id": 42,
      "name": "Flame Sword",
      "relevanceScore": 0.0759,
      "expandedEntity": {
        "id": 42,
        "name": "Flame Sword",
        "trait": "AGILITY",
        "range": "MELEE",
        "burden": "ONE_HANDED"
      }
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "query": "flame sword"
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | `q` parameter is missing or blank |
| `401 Unauthorized` | No valid JWT cookie present |

### Notes

- The `q` parameter is converted to a PostgreSQL `tsquery` via `plainto_tsquery('english', ...)`.
  Natural language input (e.g., `flame sword`) works without special syntax.
- The `size` parameter is clamped to a maximum of `100`. Requests with `size > 100` will
  receive at most 100 results per page.
- Entity expansion (`expand=entity`) is not supported for `BEASTFORM`; the `expandedEntity`
  field will be `null` for beastform results even when expansion is requested.
- Expansion failures (entity not found, access denied) are silently skipped per result so
  that a single unavailable entity does not abort the search response.
- Access control is enforced at the search index level. Non-privileged users only receive
  results for content they are permitted to view. Privileged users (MODERATOR and above)
  bypass content visibility restrictions.
