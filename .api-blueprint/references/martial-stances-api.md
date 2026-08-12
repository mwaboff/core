# Martial Stances API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/dh/martial-stances`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)
**Content-Type:** `application/json`

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/dh/martial-stances` | Authenticated | List martial stances (paginated, filterable by `expansionId`, `isOfficial`, `tier`) |
| 2 | GET | `/api/dh/martial-stances/{id}` | Authenticated | Get martial stance by ID |
| 3 | POST | `/api/dh/martial-stances` | ADMIN, OWNER | Create a martial stance |
| 4 | POST | `/api/dh/martial-stances/bulk` | ADMIN, OWNER | Create multiple martial stances |
| 5 | PUT | `/api/dh/martial-stances/{id}` | ADMIN, OWNER | Update a martial stance |
| 6 | DELETE | `/api/dh/martial-stances/{id}` | ADMIN, OWNER | Soft-delete a martial stance |
| 7 | POST | `/api/dh/martial-stances/{id}/restore` | ADMIN, OWNER | Restore a soft-deleted martial stance |

`MartialStance` (Hope & Fear) extends `BaseItem` and is the catalog of stance texts only (name, tier,
effect description) — 16 official stances, 4 per tier. It does **not** track which stances a character
knows or which is active; that character-state lives on the character sheet — see
`references/character-sheets-api.md` (`knownMartialStanceIds`, `activeMartialStanceId`,
`clearActiveMartialStance`).

Martial stances have no user-authoring path — only ADMIN/OWNER can create or update one, unlike
weapons, armor, and loot. **SRD content gating:** among official content, paid-expansion
("non-SRD") stances are further gated behind ADMIN/OWNER role or a per-user "Access All
Expansions" grant; SRD-licensed stances stay visible to everyone. List endpoints filter restricted
rows out entirely. `GET /{id}`, and the stance embedded in `CharacterSheetResponse` (known stances
and `activeMartialStance`), come back as a redacted stub for restricted content: only `id`,
`restricted: true`, and `expansionName` are present. This gate is currently off by default
(kill-switched) until the catalogue's SRD flags are populated.

Granted by the Martial Artist subclass's foundation feature ("Stance Fighter"). A character knows 2
Tier-1 stances at pick, +1 stance per level thereafter (at or below the character's current tier). Only
one stance can be active at a time; entering costs 1 Focus. The active stance drops on Severe damage,
marking the last Hit Point, shifting to another stance, or end of scene (all enforced client-side —
the backend only enforces the structural invariants below).

---

## 1. GET `/api/dh/martial-stances`

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |
| `includeDeleted` | boolean | `false` | No | Include soft-deleted stances (MODERATOR+ only; silently coerced to `false` below that role, no error) |
| `expansionId` | long | -- | No | Filter by expansion |
| `isOfficial` | boolean | -- | No | Filter by official status |
| `tier` | int | -- | No | Filter by tier (1-4) |
| `expand` | string | -- | No | Comma-separated: `expansion`, `features`, `originalMartialStance` |

Response: `PagedResponse<MartialStanceResponse>`.

## 2. GET `/api/dh/martial-stances/{id}`

Returns a single stance. Same `expand` options as above. 404 if not found or soft-deleted.

## 3. POST `/api/dh/martial-stances`

Body: `CreateMartialStanceRequest`. Returns `201` with `MartialStanceResponse`.

## 4. POST `/api/dh/martial-stances/bulk`

Body: `CreateMartialStanceRequest[]`. Returns `201` with `MartialStanceResponse[]`.

## 5. PUT `/api/dh/martial-stances/{id}`

Body: `UpdateMartialStanceRequest` (partial update).

## 6. DELETE `/api/dh/martial-stances/{id}`

Soft-delete. Returns `204`.

## 7. POST `/api/dh/martial-stances/{id}/restore`

Restores a soft-deleted stance. `400` if not currently deleted.

---

## Models

### CreateMartialStanceRequest

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | Yes | Not blank, max 200 chars |
| `expansionId` | long | Yes | Must reference an existing Expansion |
| `tier` | integer | Yes | 1-4 |
| `isOfficial` | boolean | Yes | -- |
| `srd` | boolean | No | SRD-licensed flag. Honoured only for ADMIN+, coerced to `false` otherwise (no error). Omitted by existing bulk-import payloads, which keep working |
| `description` | string | No | Effect text |
| `featureIds` | long[] | No | Each must reference an existing Feature |
| `features` | FeatureInput[] | No | Find-or-create inline, merged with `featureIds` |
| `originalMartialStanceId` | long | No | Set when creating a custom copy of an official stance |

Official stances have **no** feature rows — a printed stance is a name plus one effect sentence in
`description`. `featureIds`/`features` exist for homebrew content that wants structured sub-effects.

### UpdateMartialStanceRequest

Same fields as create, all optional; only non-null fields are applied.

### MartialStanceResponse

| Field | Type | Always Present | Notes |
|-------|------|-----------------|-------|
| `id` | long | Yes | -- |
| `name` | string | Yes | -- |
| `expansionId` | long | Yes | -- |
| `expansionName` | string | Yes | Always included. On a redacted stub this is the only content-identifying field carried |
| `restricted` | boolean | No | `true` when this response is a redacted stub for gated non-SRD content the caller may not view. When present, every field below except `id` and `expansionName` is absent |
| `expansion` | ExpansionResponse | No | Only with `?expand=expansion` |
| `tier` | integer | Yes | 1-4 |
| `isOfficial` | boolean | Yes | -- |
| `srd` | boolean | Yes | Whether this is SRD-licensed content |
| `description` | string | No | Omitted if null |
| `featureIds` | long[] | Yes (when present) | -- |
| `features` | FeatureResponse[] | No | Only with `?expand=features` |
| `originalMartialStanceId` | long | No | Omitted if this is not a custom copy |
| `originalMartialStance` | MartialStanceResponse | No | Only with `?expand=originalMartialStance` |
| `createdAt` | datetime | Yes | -- |
| `lastModifiedAt` | datetime | Yes | -- |
| `deletedAt` | datetime | No | Omitted unless soft-deleted |

---

## Character Sheet Invariants

Enforced by `CharacterSheetService` whenever `knownMartialStanceIds` or `activeMartialStanceId` is
updated via `PUT /api/dh/character-sheets/{id}`:

- **Active stance must be known.** If `activeMartialStanceId` (or the sheet's existing active stance,
  when only `knownMartialStanceIds` is updated) is not a member of the known-stances set, the request
  fails with `400 Bad Request`.
- **Known stance tier <= character tier.** Character tier is derived from `level` (tier 1: levels 1;
  tier 2: 2-4; tier 3: 5-7; tier 4: 8-10 — same mapping `LevelUpService` uses). A known stance whose
  `tier` exceeds the character's current tier is rejected with `400 Bad Request`.

## Error Responses

Same shape as every other catalog endpoint — see `references/quick-start.md`. `404` on missing/deleted
stance or expansion, `400` on validation failure, `403` on insufficient role for mutation endpoints.
