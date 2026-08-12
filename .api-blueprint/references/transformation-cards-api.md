# Transformation Cards API Reference

**Base URL:** `http://localhost:8080`
**Prefix:** `/api/dh/transformation-cards`
**Authentication:** JWT token in `AUTH_TOKEN` HttpOnly cookie (all endpoints)
**Content-Type:** `application/json`

---

## Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | GET | `/api/dh/transformation-cards` | Authenticated | List transformation cards (paginated, filterable by `expansionId`) |
| 2 | GET | `/api/dh/transformation-cards/{id}` | Authenticated | Get transformation card by ID |
| 3 | POST | `/api/dh/transformation-cards` | ADMIN, OWNER | Create a transformation card |
| 4 | POST | `/api/dh/transformation-cards/bulk` | ADMIN, OWNER | Create multiple transformation cards |
| 5 | PUT | `/api/dh/transformation-cards/{id}` | ADMIN, OWNER | Update a transformation card |
| 6 | DELETE | `/api/dh/transformation-cards/{id}` | ADMIN, OWNER | Soft-delete a transformation card |
| 7 | POST | `/api/dh/transformation-cards/{id}/restore` | ADMIN, OWNER | Restore a soft-deleted transformation card |

`TransformationCard` is a standalone catalog entity (Hope & Fear) — it is **not** a `Card`/`DomainCard`
row and does not count against a character's 5-card domain-card loadout. Rules-wise: one transformation
per PC, GM-granted, no statblock (no tier/traits/evasion/thresholds — every number lives in feature
prose). Official content has exactly 2 features and exactly 6 questions per card.

A character's attached transformation, token count (Vampire "Feed"), and Wolf Form toggle (Werewolf)
live on the character sheet, not here — see `references/character-sheets-api.md` (`transformationCardId`,
`transformationTokens`, `wolfFormActive`, `clearTransformationCard`).

**SRD content gating:** paid-expansion ("non-SRD") transformation cards are gated behind
ADMIN/OWNER role or a per-user "Access All Expansions" grant; SRD-licensed cards stay visible to
everyone. `isOfficial` is always `true` for every transformation card — there is no `/custom`
authoring endpoint for this type, every create is ADMIN/OWNER catalogue content. List endpoints
filter restricted rows out entirely. `GET /{id}` cannot filter, so a restricted card fetched
directly comes back as a **redacted stub**: only `id`, `restricted: true`, and `expansionName` are
present. This gate is currently off by default (kill-switched) until the catalogue's SRD flags are
populated.

---

## 1. GET `/api/dh/transformation-cards`

### Query Parameters

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `page` | int | `0` | No | Zero-based page number |
| `size` | int | `20` | No | Items per page (max: 100; values >100 are clamped) |
| `includeDeleted` | boolean | `false` | No | Include soft-deleted cards |
| `expansionId` | long | -- | No | Filter by expansion |
| `expand` | string | -- | No | Comma-separated: `expansion`, `features`, `questions` |

Response: `PagedResponse<TransformationCardResponse>`.

## 2. GET `/api/dh/transformation-cards/{id}`

Returns a single card. Same `expand` options as above. 404 if not found or soft-deleted.

## 3. POST `/api/dh/transformation-cards`

Body: `CreateTransformationCardRequest`. Returns `201` with `TransformationCardResponse`.

## 4. POST `/api/dh/transformation-cards/bulk`

Body: `CreateTransformationCardRequest[]`. Returns `201` with `TransformationCardResponse[]`.

## 5. PUT `/api/dh/transformation-cards/{id}`

Body: `UpdateTransformationCardRequest` (partial update — only non-null fields applied).

## 6. DELETE `/api/dh/transformation-cards/{id}`

Soft-delete. Returns `204`.

## 7. POST `/api/dh/transformation-cards/{id}/restore`

Restores a soft-deleted card. `400` if the card is not currently deleted.

---

## Models

### CreateTransformationCardRequest

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | Yes | Not blank, max 200 chars |
| `description` | string | No | -- |
| `expansionId` | long | Yes | Must reference an existing Expansion |
| `srd` | boolean | No | ADMIN+ only, coerced to `false` otherwise (no error). Omitted by existing bulk-import payloads, which keep working |
| `featureIds` | long[] | No | Each must reference an existing Feature |
| `features` | FeatureInput[] | No | Find-or-create inline. Merged with `featureIds` if both provided. Each inherits this card's `isOfficial`/`srd` rather than defaulting to official |
| `questionIds` | long[] | No | Each must reference an existing Question |
| `questions` | QuestionInput[] | No | Find-or-create inline via the shared `QuestionService.resolveQuestions` (same machinery `ClassService` uses for `backgroundQuestions`/`connectionQuestions`). Merged with `questionIds` if both provided. |

Official content supplies exactly 2 `features` (typically `featureType: TRANSFORMATION`) and exactly 6
`questions` (`questionType: TRANSFORMATION`) per card, but this is a content convention, not a
server-enforced count.

### UpdateTransformationCardRequest

All fields optional; only non-null fields are applied.

| Field | Type | Notes |
|-------|------|-------|
| `name` | string | Max 200 chars |
| `description` | string | -- |
| `expansionId` | long | Must reference an existing Expansion |
| `srd` | boolean | ADMIN+ only |
| `featureIds` | long[] | Replaces the entire feature set when provided |
| `features` | FeatureInput[] | Find-or-create inline, merged with `featureIds` |
| `questionIds` | long[] | Replaces the entire question set when provided |
| `questions` | QuestionInput[] | Find-or-create inline, merged with `questionIds` |

### QuestionInput

Shared DTO also used by `CreateClassRequest`/`UpdateClassRequest`. See `references/classes-api.md` and
`references/questions-api.md`.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `questionText` | string | Yes | Matched case-insensitively against existing questions within the same expansion + type |
| `questionType` | QuestionType | Yes | `TRANSFORMATION` for transformation card questions |
| `expansionId` | long | Yes | Must reference an existing Expansion |

### TransformationCardResponse

| Field | Type | Always Present | Notes |
|-------|------|-----------------|-------|
| `id` | long | Yes | -- |
| `name` | string | Yes | -- |
| `description` | string | No | Omitted if null |
| `isOfficial` | boolean | Yes | Always `true` (no `/custom` authoring endpoint exists for this type) |
| `srd` | boolean | Yes | Whether this card is SRD-licensed content |
| `expansionId` | long | Yes | -- |
| `expansion` | ExpansionResponse | No | Only with `?expand=expansion` |
| `featureIds` | long[] | Yes | -- |
| `features` | FeatureResponse[] | No | Only with `?expand=features` |
| `questionIds` | long[] | Yes | -- |
| `questions` | QuestionResponse[] | No | Only with `?expand=questions` |
| `createdAt` | datetime | Yes | -- |
| `lastModifiedAt` | datetime | Yes | -- |
| `deletedAt` | datetime | No | Omitted unless soft-deleted |
| `expansionName` | string | No | Only on a redacted stub — display name of the expansion, so the caller can tell which book to buy even though `expansion` itself is unset |
| `restricted` | boolean | No | Only on a redacted stub — `true` when this response is a redacted stub for gated non-SRD content the caller may not view. When present, every field above except `id` and `expansionName` is absent |

---

## Error Responses

Same shape as every other catalog endpoint — see `references/quick-start.md` for the standard error
envelope, validation error format, and status code table. `404` on missing/deleted card or expansion,
`400` on validation failure, `403` on insufficient role for mutation endpoints.
