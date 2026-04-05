# Mixed Ancestry Support

## Context

Daggerheart allows players to create mixed-ancestry characters by combining features from two existing ancestries. The player selects two non-mixed ancestries, then picks one feature from each, resulting in a character with a custom ancestry name and exactly 2 features.

## Approach

Add an `is_mixed` boolean column to the `ancestry_cards` table. Mixed ancestries are stored as regular `AncestryCard` entities with `is_mixed=true` and `is_official=false`, using the existing `card_features` join table for the 2 selected features.

A dedicated `POST /api/dh/cards/ancestry/mixed` endpoint allows any authenticated user to create mixed ancestries (unlike standard ancestry CRUD which requires ADMIN/OWNER). The `GET /` list endpoint excludes mixed ancestries by default, with a `?isMixed=true` query parameter to include them.

No parent ancestry references are stored — the name captures the heritage, and the features capture the mechanics.

## File Changes

### Migration

| File | Change |
|------|--------|
| `V{timestamp}__add_mixed_ancestry_support.sql` | Add `is_mixed BOOLEAN NOT NULL DEFAULT false` to `ancestry_cards`, index on `is_mixed` |

### Entity

| File | Change |
|------|--------|
| `AncestryCard.java` | Add `isMixed` boolean field, default `false` |

### DTOs

| File | Change |
|------|--------|
| `CreateMixedAncestryCardRequest.java` (new) | `name` (required), `description`, `expansionId` (required), `featureIds` (required, exactly 2), `backgroundImageUrl` |
| `AncestryCardResponse.java` | Add `isMixed` boolean field |

### Repository

| File | Change |
|------|--------|
| `AncestryCardRepository.java` | Add `Boolean isMixed` parameter to both filter query methods |

### Service

| File | Change |
|------|--------|
| `AncestryCardService.java` | Add `isMixed` param to `getAllAncestryCards` (default `false` when null). New `createMixedAncestryCard` method: validates exactly 2 features, sets `isMixed=true`, `isOfficial=false`. Include `isMixed` in `toResponse`. |

### Controller

| File | Change |
|------|--------|
| `AncestryCardController.java` | Add `@RequestParam(required = false) Boolean isMixed` to `GET /`. New `POST /mixed` endpoint — no `@PreAuthorize` (any authenticated user). |

### API Blueprint

| File | Change |
|------|--------|
| `ancestry-cards-api.md` | Document `POST /mixed` endpoint, `isMixed` query param on `GET /`, updated response schema, client usage examples |

## Validation Rules

- `POST /mixed`: exactly 2 `featureIds` required (400 if not)
- `POST /mixed`: `isMixed` forced to `true`, `isOfficial` forced to `false`
- `GET /`: `isMixed` defaults to `false` when not provided (hides mixed from standard list)

## Testing Strategy

### Unit Tests (`AncestryCardServiceTest`)
- `createMixedAncestryCard` — success with valid 2 features
- `createMixedAncestryCard` — failure with <2 features
- `createMixedAncestryCard` — failure with >2 features
- `getAllAncestryCards` — filters by `isMixed=false` by default
- `getAllAncestryCards` — returns mixed when `isMixed=true`

### Integration Tests (`AncestryCardControllerIntegrationTest`)
- `POST /mixed` — any authenticated user can create
- `POST /mixed` — validates exactly 2 features
- `GET /` — excludes mixed by default
- `GET /?isMixed=true` — returns only mixed
- Response includes `isMixed` field
