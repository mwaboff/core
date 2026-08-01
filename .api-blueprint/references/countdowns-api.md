# Countdowns API Reference

Base URL: `http://localhost:8080`

## Overview

A `Countdown` is a GM-facing tracker scoped to a campaign. Per the Daggerheart SRD (p. 68), a
countdown "represents a period of time or series of events preceding a future effect": it begins
at a starting value, advances toward 0, and triggers its effect on reaching 0.

Countdowns are **GM-only state**, like `Campaign.gmNotes` — players cannot read them. They use
**hard deletion** and are **not search-indexed**. `Campaign` holds no inverse collection; the FK
carries `ON DELETE CASCADE`, so deleting a campaign removes its countdowns.

**Authentication:** All endpoints require a valid JWT token in an `AUTH_TOKEN` HttpOnly cookie.

**Access Control:** Every endpoint, **reads included**, requires game master-level access to the
owning campaign — campaign creator, any game master, or MODERATOR/ADMIN/OWNER. Enforced in the
service layer, which delegates to `CampaignService.hasGameMasterAccess` so that "is a GM" has a
single definition across services.

---

## Enums

### `CountdownType` — the advancement mode

Answers "when do I tick this?". Note that "dynamic" is not a value: `PROGRESS` and `CONSEQUENCE`
*are* the two dynamic kinds, differing only in which column of the SRD's Dynamic Countdown
Advancement table they read.

| Value | Advances |
|-------|----------|
| `STANDARD` | Every time a player makes an action roll |
| `PROGRESS` | Dynamic, toward a positive effect |
| `CONSEQUENCE` | Dynamic, toward a negative effect |
| `LONG_TERM` | On rests |

**Dynamic Countdown Advancement** (SRD p. 68) — applied by the GM, not the server:

| Roll Result | Progress | Consequence |
|---|---|---|
| Failure with Fear | — | Tick down 3 |
| Failure with Hope | — | Tick down 2 |
| Success with Fear | Tick down 1 | Tick down 1 |
| Success with Hope | Tick down 2 | — |
| Critical Success | Tick down 3 | — |

### `CountdownLoop` — behaviour after the effect triggers

Mirrors the SRD's "Advanced Countdown Features" (p. 69). Applied **server-side** when a tick
brings `currentValue` to 0.

| Value | Effect at 0 |
|-------|-------------|
| `NONE` | Rests at 0 |
| `LOOP` | `currentValue` resets to `startingValue` |
| `LOOP_INCREASING` | `startingValue` += 1, then reset |
| `LOOP_DECREASING` | `startingValue` -= 1 (floored at 1), then reset |

The floor of 1 for decreasing loops is an application decision, not a rule — the SRD sets no lower
bound, and a starting value of 0 would re-trigger forever.

---

## Endpoints

### GET /api/dh/countdowns

Lists a campaign's countdowns in display order (`displayOrder` ascending, then `id`).

**Authorization:** Game master of the campaign

**Query Parameters:**

| Parameter | Type | Default | Required | Description |
|-----------|------|---------|----------|-------------|
| `campaignId` | Long | -- | **Yes** | The campaign to list countdowns for |

**Response:** `200 OK` — an unpaginated array (a campaign's countdown list is small by nature)

```json
[
  {
    "id": 1,
    "campaignId": 7,
    "name": "The ritual completes",
    "type": "CONSEQUENCE",
    "loopBehavior": "NONE",
    "startingValue": 8,
    "currentValue": 6,
    "note": "The gate opens and something steps through.",
    "displayOrder": 0,
    "createdAt": "2026-07-31T21:37:09.762211",
    "lastModifiedAt": "2026-07-31T21:41:02.114003"
  }
]
```

**Error Responses:**
- `401 Unauthorized`
- `403 Forbidden` — not a game master of the campaign
- `404 Not Found` — campaign does not exist

---

### GET /api/dh/countdowns/{id}

Retrieves a single countdown.

**Authorization:** Game master of the countdown's campaign

**Response:** `200 OK` — a single countdown object

**Error Responses:** `401`, `403`, `404`

---

### POST /api/dh/countdowns

Creates a countdown, appended to the end of the campaign's list. `currentValue` is initialised to
`startingValue`.

**Authorization:** Game master of the campaign. Rejected if the campaign has ended.

**Request Body:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `campaignId` | Long | **Yes** | Must exist |
| `name` | string | **Yes** | Not blank, max 200 |
| `type` | `CountdownType` | **Yes** | |
| `loopBehavior` | `CountdownLoop` | No | Defaults to `NONE` |
| `startingValue` | int | **Yes** | 1–99 |
| `note` | string | No | Max 2000; sanitized via `MarkdownSanitizerUtil` |

```json
{
  "campaignId": 7,
  "name": "Reinforcements arrive",
  "type": "CONSEQUENCE",
  "loopBehavior": "NONE",
  "startingValue": 4
}
```

**Response:** `201 Created` — the created countdown

**Error Responses:**
- `400 Bad Request` — validation failure, or the campaign has ended
- `401`, `403`, `404`

---

### PUT /api/dh/countdowns/{id}

Updates a countdown's definition. `currentValue` is **not** editable here — it has its own
endpoint so a tick during play cannot race with a configuration edit. If the new `startingValue`
is below the current value, the current value is clamped down to it.

**Authorization:** Game master of the countdown's campaign. Rejected if the campaign has ended.

**Request Body:** `name`, `type`, `loopBehavior`, `startingValue` all required; `note` optional.

**Response:** `200 OK` — the updated countdown

**Error Responses:** `400`, `401`, `403`, `404`

---

### PATCH /api/dh/countdowns/{id}/value

Ticks a countdown. Takes an **absolute value, not a delta**, matching
`PATCH /api/dh/campaigns/{id}/fear`: a GM tapping quickly can have several requests in flight, and
absolute values make the last one win rather than compounding into a lost update.

The value is clamped to `startingValue`. If it reaches 0, the countdown's loop behaviour is applied
**before the response is returned** — so a tick to 0 on a looping countdown responds with the reset
value, not with 0. Clients should adopt the response rather than assume their optimistic value stuck.

**Authorization:** Game master of the countdown's campaign. Rejected if the campaign has ended.

**Request Body:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `currentValue` | int | **Yes** | 0–99 |

```json
{ "currentValue": 5 }
```

**Response:** `200 OK` — the updated countdown

**Error Responses:** `400`, `401`, `403`, `404`

---

### DELETE /api/dh/countdowns/{id}

Permanently removes a countdown (hard delete).

**Authorization:** Game master of the countdown's campaign

**Response:** `204 No Content`

**Error Responses:** `401`, `403`, `404`
