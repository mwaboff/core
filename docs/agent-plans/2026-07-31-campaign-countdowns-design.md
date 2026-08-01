# Campaign countdown tracker — design

**Date:** 2026-07-31
**Status:** implemented
**Tracking:** `dawn-q2r` (supersedes `dawn-4dm`, which proposed a static-reference-only panel)

## Context

The GM screen references countdowns in four places — Long Rest ("can advance a long-term
countdown"), underwater Hold Breath, Work on a Project, and the Consequence Menu — and
defines them **nowhere**. A GM reading the screen is told to advance a thing the screen
never explains.

The user's framing is the design constraint: *"I always have difficulty understanding how
to run countdowns so please make sure to include concise instructions."* This is not only a
persistence feature. The panel has to answer **"when do I tick this?"** at the moment the
GM is looking at it, or it has failed.

Requirements as stated: add multiple countdowns, remove them as needed, GM version, linked
to a campaign.

### Rules basis

All rules text is taken verbatim from the **official Daggerheart SRD**
(`https://www.daggerheart.com/wp-content/uploads/2025/09/Daggerheart-SRD-9-09-25.pdf`,
printed pp. 68–69), extracted in-session. The Core Rulebook is not available locally; the
SRD is the authoritative source we can actually cite.

> Countdowns represent a period of time or series of events preceding a future effect. A
> countdown begins at a starting value. When a countdown advances, it's reduced by 1. The
> countdown's effect is triggered when the countdown reaches 0.

Advancement modes (SRD p. 68):

| Type | When it advances |
|---|---|
| `STANDARD` | Every time a player makes an action roll |
| `PROGRESS` | Dynamic, toward a **positive** effect — per the table below |
| `CONSEQUENCE` | Dynamic, toward a **negative** effect — per the table below |
| `LONG_TERM` | On rests (long rest: "generally tick down a relevant long-term countdown once") |

**Dynamic Countdown Advancement** (SRD p. 68, verbatim):

| Roll Result | Progress | Consequence |
|---|---|---|
| Failure with Fear | No advancement | Tick down 3 |
| Failure with Hope | No advancement | Tick down 2 |
| Success with Fear | Tick down 1 | Tick down 1 |
| Success with Hope | Tick down 2 | No advancement |
| Critical Success | Tick down 3 | No advancement |

Note there is **no critical-failure row** — Critical Success is the only crit in the table.
"Dynamic" is not itself a type; `PROGRESS` and `CONSEQUENCE` *are* the two dynamic kinds.

Loop variants, from "Advanced Countdown Features" (SRD p. 69) — one bullet each, no worked
examples:

- Loop countdowns that reset to their starting value after their effect is triggered.
- Increasing countdowns that increase their starting value by 1 every time they loop.
- Decreasing countdowns that decrease their starting value by 1 every time they loop.

### Decisions taken

| Question | Decision |
|---|---|
| Visibility | **GM-only**, read and write. `validateGameMasterAccess` (creator OR GM OR moderator+) |
| Rule scope | Four core types **plus** loop variants (user chose over core-only) |
| URL shape | **Flat** — `/api/dh/countdowns?campaignId=`, matching every other child resource |
| Delete | **Hard delete**. Every child row in this codebase hard-deletes |
| Tick payload | **Absolute value, not a delta** — matches `updateFear`, avoids lost-update races |
| Inverse collection | **None** on `Campaign`. Matches `CampaignInvite` / `CharacterSheetCondition` |
| Search index | **Not indexed.** Private per-campaign state, not catalogue content |

### Explicitly NOT in the SRD

The SRD gives **no** guidance on picking a starting value and **no** general rule for
picking a trigger beyond the three advancement modes above. The panel must not invent
either. Where the UI needs a default it will be labelled as a suggestion, not a rule.

"Ticking up" is a genuine inconsistency in published text — the rules say countdowns only
reduce, but some Hope & Fear environment features say "tick up the countdown by 2". We
support increment in the UI as a correction affordance, without claiming it as a rule.

### Out of scope

- Linked progress/consequence pairs that advance off the same roll (SRD p. 69 bullet).
- Randomised starting values (`Countdown (1d6)` notation).
- Player-visible countdowns. Data model leaves room; no player surface is built.
- Auto-advancing countdowns from actual dice-roller results.

---

## Backend (`core`)

### Enums — `com.aboff.core.model.enums`

```java
public enum CountdownType { STANDARD, PROGRESS, CONSEQUENCE, LONG_TERM }
public enum CountdownLoop { NONE, LOOP, LOOP_INCREASING, LOOP_DECREASING }
```

Persisted as `@Enumerated(EnumType.STRING)` with a DB CHECK constraint listing exactly
these values. Per `core/CLAUDE.md`, enum/CHECK drift is this repo's most-repeated
production bug — the lists must be kept identical.

### Entity — `model/entity/dh/Countdown.java`

Extends `BaseEntity` (`Long` id, `IDENTITY`, `createdAt`, `lastModifiedAt`). Standard
Lombok stack (`@Data`, `@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)`,
`@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`). Unidirectional
`@ManyToOne(fetch = LAZY, optional = false)` to `Campaign`.

| Field | Type | Notes |
|---|---|---|
| `campaign` | `Campaign` | `@JoinColumn(name = "campaign_id", nullable = false)` |
| `name` | `String` | 1–200 |
| `type` | `CountdownType` | |
| `loopBehavior` | `CountdownLoop` | `@Builder.Default NONE` |
| `startingValue` | `Integer` | 1–99. **Mutable** — increasing/decreasing loops change it |
| `currentValue` | `Integer` | 0–99 |
| `note` | `String` | `TEXT`, nullable. "What happens at 0". Markdown-sanitised |
| `displayOrder` | `Integer` | `@Builder.Default 0`. New precedent — no existing convention |

### Loop semantics

Applied server-side when a tick brings `currentValue` to 0:

| `loopBehavior` | Effect at 0 |
|---|---|
| `NONE` | Stays at 0. Effect has triggered; GM deletes or resets manually |
| `LOOP` | `currentValue = startingValue` |
| `LOOP_INCREASING` | `startingValue += 1`, then `currentValue = startingValue` |
| `LOOP_DECREASING` | `startingValue -= 1` (floored at 1), then `currentValue = startingValue` |

**The floor at 1 is an application decision, not a rule** — the SRD specifies no floor. A
decreasing loop that reached 0 would otherwise trigger forever. Documented in the entity.

### Migration

Created via `./scripts/create-migration.sh create_countdowns_table` — never hand-numbered.

```sql
CREATE TABLE countdowns (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    countdown_type VARCHAR(20) NOT NULL,
    loop_behavior VARCHAR(20) NOT NULL DEFAULT 'NONE',
    starting_value INTEGER NOT NULL,
    current_value INTEGER NOT NULL,
    note TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_countdown_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT check_countdown_type
        CHECK (countdown_type IN ('STANDARD','PROGRESS','CONSEQUENCE','LONG_TERM')),
    CONSTRAINT check_countdown_loop
        CHECK (loop_behavior IN ('NONE','LOOP','LOOP_INCREASING','LOOP_DECREASING')),
    CONSTRAINT check_countdown_starting_value CHECK (starting_value BETWEEN 1 AND 99),
    CONSTRAINT check_countdown_current_value CHECK (current_value BETWEEN 0 AND 99)
);
CREATE INDEX idx_countdowns_campaign_id ON countdowns(campaign_id);
```

⚠️ Tests run on H2 with `spring.flyway.enabled=false` and `ddl-auto=create-drop`, so a green
suite proves **nothing** about this SQL. Verification requires running the app against
Postgres — handed to the user per standing convention.

### Repository — `repository/dh/CountdownRepository.java`

```java
@Query("SELECT c FROM Countdown c WHERE c.campaign.id = :campaignId "
     + "ORDER BY c.displayOrder ASC, c.id ASC")
List<Countdown> findByCampaignId(@Param("campaignId") Long campaignId);

@Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM Countdown c WHERE c.campaign.id = :campaignId")
Integer findMaxDisplayOrderByCampaignId(@Param("campaignId") Long campaignId);
```

### Service — `service/dh/CountdownService.java`

Dedicated service, not folded into `CampaignService` — a countdown carries per-row data and
has an independent lifecycle, same reasoning as `CharacterSheetConditionService`.

Every mutation follows the established body:
**load-active → authorize → validate-not-ended → mutate+save → audit → toResponse.**

Authorization reuses **one** definition of GM access rather than duplicating
`CampaignService`'s private helper (exact mechanism pending a verification pass; either a
public overload or a small extracted `CampaignAccessGuard`).

**Cross-campaign guard:** every by-id route must verify
`countdown.getCampaign().getId()` matches before authorizing, or a GM of campaign A could
mutate a countdown in campaign B.

New `AuditAction` constants beside `CAMPAIGN_FEAR_UPDATED`:
`CAMPAIGN_COUNTDOWN_CREATED`, `_UPDATED`, `_DELETED`.

### Controller — `controller/dh/CountdownController.java`

```
GET    /api/dh/countdowns?campaignId={id}   200 List<CountdownResponse>
POST   /api/dh/countdowns                   201 (campaignId in body)
PUT    /api/dh/countdowns/{id}              200 (name, type, loop, startingValue, note)
PATCH  /api/dh/countdowns/{id}/value        200 (absolute currentValue; applies loop rules)
DELETE /api/dh/countdowns/{id}              204
```

Thin controllers with audit bookends. No authorization logic — it lives in the service.
Errors thrown, not caught: `EntityNotFoundException` → 404,
`InsufficientPermissionsException` → 403, `IllegalStateException` → 400, bean validation → 400.

### DTOs — Lombok classes, not records

`request/CreateCountdownRequest`, `request/UpdateCountdownRequest`,
`request/UpdateCountdownValueRequest`, `response/CountdownResponse`. Explicit human-readable
`message =` on every constraint. `note` runs through `MarkdownSanitizerUtil.sanitize(...)`.

---

## Frontend (`dawn`)

### Panel registration — `campaign/campaign-panels.ts`

```typescript
{
  id: 'countdowns',                 // localStorage key + DOM id. Never rename.
  title: 'Countdowns',
  category: 'This Campaign',
  colSpan: 2,
  defaultOrder: -150,               // between Session Notes (-200) and Encounter Builder (-100)
  body: { kind: 'component', component: CountdownsPanel },
  keywords: ['countdown', 'clock', 'progress', 'consequence', 'timer', 'tick', 'loop'],
}
```

`campaign-panels.spec.ts` hard-asserts the exact id list and colSpan array — updated in the
same commit.

### Components

Split to respect the ~150-line TS / ~80-line template thresholds:

- `panels/countdowns-panel/` — list state, load, create, delete, empty state, instructions
- `panels/countdowns-panel/components/countdown-row/` — one row: value, tick controls,
  type/loop badges, inline delete

### Answering "when do I tick this?"

This is the feature's actual purpose, so the guidance is **in the row**, not only in a help
section:

1. Each row shows its type as a badge with a one-line trigger hint — e.g. `STANDARD` →
   "every action roll"; `LONG_TERM` → "on a rest".
2. `PROGRESS` / `CONSEQUENCE` rows carry a hint line giving **that type's** advancement
   amounts, so the relevant row of the table is present without a lookup — e.g. a
   consequence row reads `Fail+Fear 3 · Fail+Hope 2 · Succ+Fear 1`.
3. A collapsible "How countdowns work" section holds the definition and the full
   advancement table, quoted from the SRD.

Ticking itself is plain −/+ on every row, uniform across all four types. Considered and
rejected: per-outcome tick buttons that apply the table automatically. They remove the
arithmetic but cost significant row width, apply to only two of the four types, and make
the codebase's first list-CRUD panel materially more complex. The hint line delivers most
of the benefit at a fraction of the surface.

### Data flow

Follows `fear-counter-panel` exactly: **optimistic local write first**, then save;
`switchMap` so rapid taps cancel in-flight requests (last value wins); `catchError` restores
the pre-edit snapshot and returns `EMPTY` so the stream survives;
`takeUntilDestroyed(destroyRef)`; clamp before issuing any request. Per-row saving state via
the existing string-keyed `context.markSaving('countdown-' + id)`.

Countdowns are their own resource, **not** a field on `CampaignResponse`, so no
`countdowns` signal is added to `GmScreenContext` — the panel reads only `campaignId()`,
which is `null` on first render and must be guarded. Initial load fires from an `effect()`
reading `campaignId()` with the work in `untracked()`.

Collections replaced immutably (`new Map(cur)`), `@for` tracked by `id`.

### Service + models

- `shared/models/countdown-api.model.ts` — response/request interfaces, the two union types,
  and const option arrays for the type/loop selects
- `shared/services/countdown.service.ts` — `withCredentials: true` on every call, raw
  Observables, no `catchError` in the service

### Reuse, not rebuild

`inline-delete-confirm` (per-row delete), `saving-spinner`, `.gm-panel__btn` (44px targets,
all states), `.gm-panel__note` (empty state), `.gm-panel__table--dense` (advancement table),
`.gm-panel__callout`, global `forms.css`. No new shared components.

---

## Testing

**Backend** — both layers, per `core/.claude/rules/testing.md` (80%+, aim 100% logic):

- `CountdownServiceTest` — Mockito unit tests. Loop transitions at 0 for all four
  behaviours, the decreasing floor, cross-campaign rejection, clamping.
- `CountdownControllerIntegrationTest` — modelled on `CampaignGmScreenIntegrationTest`, real
  JWT + `AUTH_TOKEN` cookie, five personas. Per mutation: creator→200, GM→200,
  moderator→200, player→403, outsider→403, unauthenticated→401, unknown id→404, ended
  campaign→400, validation boundaries→400/200. Plus cross-campaign→403/404 and
  `<script>` stripped from `note`.

**Frontend** — Vitest + `HttpTestingController`, real seeded `GmScreenContext`:

initial GET renders rows; empty state; create POSTs and appends; tick PATCHes optimistically;
tick failure rolls back; delete removes then DELETEs; delete failure restores; clamp at
min/max issues no request; `campaignId() === null` issues no request; loop display at 0;
form validation.

**Gates:** `./mvnw test` green; `npm run test:run`, `npm run lint`, `npm run build` green.

## Side-effects not to miss

1. `core/.api-blueprint/` — add `references/countdowns-api.md`, update the controller index
   and endpoint count in `SKILL.md`.
2. The GM screen's four dangling countdown references can now point at a real panel.
3. Migration correctness is unverifiable by the test suite — needs a real app start.
