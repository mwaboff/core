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
`tsvector` / `plainto_tsquery`. Results are ranked by relevance score (descending) and
paginated. Access control is enforced transparently: non-privileged users only receive
results for content they are permitted to view (official, public, or their own content).
Privileged users (MODERATOR and above) bypass these restrictions.

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `q` | String | -- | **Yes** | Search query string |
| `types` | List\<SearchableEntityType\> | -- | No | Restrict to specific entity types (comma-separated) |
| `tier` | Integer | -- | No | Filter by tier level |
| `expansionId` | Long | -- | No | Filter by expansion ID |
| `isOfficial` | Boolean | -- | No | Filter by official status |
| `cardType` | String | -- | No | Filter by card type (e.g., `ANCESTRY`, `DOMAIN`) |
| `featureType` | String | -- | No | Filter by feature type (e.g., `CLASS_FEATURE`) |
| `adversaryType` | String | -- | No | Filter by adversary role (e.g., `MINION`, `LEADER`) |
| `domainCardType` | String | -- | No | Filter by domain card type (e.g., `ABILITY`, `SPELL`) |
| `associatedDomainId` | Long | -- | No | Filter by associated domain ID |
| `trait` | String | -- | No | Filter by trait (e.g., `AGILITY`, `STRENGTH`) |
| `range` | String | -- | No | Filter by range (e.g., `MELEE`, `RANGED`) |
| `burden` | String | -- | No | Filter by burden (e.g., `ONE_HANDED`, `TWO_HANDED`) |
| `isConsumable` | Boolean | -- | No | Filter by consumable flag |
| `expand` | String | -- | No | Pass `entity` or `all` to include full entity DTOs in results |
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |

### SearchableEntityType Values

`DOMAIN`, `CLASS`, `FEATURE`, `ANCESTRY_CARD`, `COMMUNITY_CARD`, `SUBCLASS_CARD`,
`DOMAIN_CARD`, `WEAPON`, `ARMOR`, `LOOT`, `ADVERSARY`, `BEASTFORM`, `ENCOUNTER`,
`EXPANSION`, `SUBCLASS_PATH`, `QUESTION`, `CARD_COST_TAG`

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
| `400 Bad Request` | `q` is missing or blank |
| `401 Unauthorized` | No valid JWT cookie present |

### Notes

- The `q` parameter is converted to a PostgreSQL `tsquery` via `plainto_tsquery('english', ...)`.
  Natural language input (e.g., `flame sword`) works without special syntax.
- Entity expansion (`expand=entity`) is not supported for `BEASTFORM`; the `expandedEntity`
  field will be `null` for beastform results even when expansion is requested.
- Expansion failures (entity not found, access denied) are silently skipped per result so
  that a single unavailable entity does not abort the search response.
