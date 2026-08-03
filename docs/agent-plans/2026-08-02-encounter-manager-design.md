# Encounter Manager — Design & Implementation Plan

## Context

Oh Sheet has a GM screen but no way to prepare or run a fight. GMs currently have to
balance encounters by hand against the rulebook's Battle Point budget, then track every
adversary's HP and Stress on paper.

A partial backend already exists (`Encounter`, `EncounterAdversary`, CRUD endpoints, a
battle-point sum) and the campaign GM screen already reserves a panel slot rendering a
"Coming soon" placeholder. Nothing is wired to a UI, the battle-point math is wrong, and
there is no concept of *running* an encounter.

This plan completes the feature: browse adversaries by tier, build a points-budgeted
encounter, save it privately, load it into the reserved GM screen panel, and run the fight
with live per-adversary counters.

### Product decisions (confirmed with the user)

| Decision | Choice |
|---|---|
| Builder location | New full-page `/encounters` route; the GM panel lists saved encounters and **runs** them |
| Running a fight | **Campaign-free.** Any authenticated user can build and run an encounter with no campaign and no GM role. The GM screen panel is one *host* for the run view, not a prerequisite |
| Live run state | New server-side `encounter_runs` tables, modelled on `Countdown` |
| Party size | **Always manually entered** and persisted per encounter — never derived from the campaign roster |
| In scope | Environments on encounters, per-instance retier, Battle Point adjustment toggles |
| Out of scope | Spotlight / Fear-pool tracking in the run view |

### The campaign-free constraint drives the architecture

Running a fight must work for a solo user with no campaign, and must behave identically
inside the campaign GM screen. That rules out putting run logic in the panel, and rules out
any dependency on `GmScreenContext` (which is provided *only* by the campaign page shell —
see the class comment in `gm-screen-context.service.ts`).

So the run view is built **once** as a campaign-agnostic shared component driven purely by
inputs, and hosted in two places:

```
shared/components/encounter-run/          ← the single implementation
        ▲                          ▲
        │                          │
/encounters/:id/run            GM screen panel
(standalone play page)         (campaign host, passes campaignId)
```

`campaign_id` on a run is **nullable throughout**. A campaign is an optional tag that widens
who can see the run — never a requirement to create one.

---

## Rules foundation (verbatim, `resources/rules/chapters/core-04-adversaries-and-environments.md`)

**Budget** (L239): `(3 × the number of PCs in combat) + 2`

**Adjustments** (L249–254):
- −1 if the fight should be less difficult or shorter
- −2 if using 2 or more Solo adversaries
- −2 if adding +1d4 (or a static +2) to all adversaries' damage rolls
- +1 if choosing an adversary from a lower tier
- +1 if including no Bruisers, Hordes, Leaders, or Solos
- +2 if the fight should be more dangerous or last longer

**Costs** (L260–265):
- **1 per _group_ of Minions equal to the size of the party**
- 1 per Social or Support
- 2 per Horde, Ranged, Skulk, or Standard
- 3 per Leader · 4 per Bruiser · 5 per Solo

**Improvised Statistics by Tier** (L935–940) — the retier source of truth:

| Statistic | Tier 1 | Tier 2 | Tier 3 | Tier 4 |
|---|---|---|---|---|
| Attack Modifier | +1 | +2 | +3 | +4 |
| Damage Dice | 1d6+2 – 1d12+4 | 2d6+3 – 2d12+4 | 3d8+3 – 3d12+5 | 4d8+10 – 4d12+15 |
| Difficulty | 11 | 14 | 17 | 20 |
| Thresholds | 7 / 12 | 10 / 20 | 20 / 32 | 25 / 45 |

Moving 1–2 → 3–4 should also raise HP and Stress by 1–3 (L944).

---

## Two bugs this work must fix

**1. Minion battle points are over-charged.**
`Encounter.calculateTotalBattlePoints()` sums `AdversaryType.battlePoints` per row, charging
**1 point per Minion**. The rule is 1 point per *group of Minions equal to party size*. With a
party of 4, eight Minions cost 2 points, not 8.

Because the correct cost depends on party size, the math cannot stay on the entity as a
no-arg method. It moves to a dedicated, unit-tested calculator that takes party size.

**2. Run state must never touch the catalog `Adversary`.**
`Adversary` carries `hitPointMarked` / `stressMarked` columns and `UpdateAdversaryRequest`
exposes both. Writing a fight's damage there would mutate **shared official content for every
user**. All live state goes in the new run tables. (Cleaning up those catalog columns is out
of scope here — noted as a follow-up.)

---

## Data model

### Changes to `encounters`

| Column | Type | Notes |
|---|---|---|
| `party_size` | INTEGER | Manually entered; drives budget + minion grouping. `CHECK (party_size IS NULL OR party_size BETWEEN 1 AND 12)` |
| `adjustment_easier` | BOOLEAN NOT NULL DEFAULT FALSE | −1 |
| `adjustment_two_plus_solos` | BOOLEAN NOT NULL DEFAULT FALSE | −2 |
| `adjustment_bonus_damage` | BOOLEAN NOT NULL DEFAULT FALSE | −2 |
| `adjustment_lower_tier` | BOOLEAN NOT NULL DEFAULT FALSE | +1 |
| `adjustment_no_elites` | BOOLEAN NOT NULL DEFAULT FALSE | +1 |
| `adjustment_harder` | BOOLEAN NOT NULL DEFAULT FALSE | +2 |
| `environment_id` | BIGINT NULL | FK → `environments(id)` ON DELETE SET NULL |

Six discrete booleans rather than one integer: the UI shows six labelled toggles, and a
single opaque integer would lose which modifiers were chosen when the encounter is reopened.

### Changes to `encounter_adversaries`

Keep **one row per instance** — migration `V20260130225724303` deliberately dropped the
`count` column, and per-instance rows are exactly what per-instance HP tracking needs.

| Column | Type | Notes |
|---|---|---|
| `label` | VARCHAR(100) NULL | GM nickname, e.g. "Archer A" |
| `tier_override` | INTEGER NULL | Retier target; `CHECK (tier_override IS NULL OR BETWEEN 1 AND 4)` |
| `display_order` | INTEGER NOT NULL DEFAULT 0 | Mirrors `Countdown.displayOrder` |

Retier stores only the **target tier**, not a copy of the derived stats. The stat table is
static book data, so the derived values are computed on read from a single shared table —
storing them would let the two drift.

### New: `encounter_runs`

One active run per encounter per campaign. Mirrors `Countdown`'s campaign-scoped shape.

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `encounter_id` | BIGINT NOT NULL | FK → `encounters` ON DELETE CASCADE |
| `campaign_id` | BIGINT **NULL** | FK → `campaigns` ON DELETE SET NULL. Null for a standalone run — this nullability is the whole campaign-free story, so no NOT NULL and no default |
| `started_by_id` | BIGINT NOT NULL | FK → `users` ON DELETE CASCADE. The run's owner |
| `status` | VARCHAR(20) NOT NULL | `ACTIVE` / `COMPLETED`; add a CHECK constraint listing both |
| `started_at`, `ended_at` | TIMESTAMP | |
| `created_at`, `last_modified_at` | TIMESTAMP NOT NULL | |

Index `(started_by_id, status)` for "my active runs" and a partial index on
`(campaign_id) WHERE campaign_id IS NOT NULL` for the panel's campaign query.

### New: `encounter_run_adversaries`

Snapshotted at run start so editing the saved encounter mid-fight cannot corrupt the run.

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `encounter_run_id` | BIGINT NOT NULL | FK ON DELETE CASCADE |
| `adversary_id` | BIGINT NOT NULL | FK → `adversaries` (read-only reference for the stat block) |
| `label` | VARCHAR(100) NULL | Copied from the template row |
| `tier_override` | INTEGER NULL | Copied from the template row |
| `hit_points_marked` | INTEGER NOT NULL DEFAULT 0 | |
| `stress_marked` | INTEGER NOT NULL DEFAULT 0 | |
| `is_defeated` | BOOLEAN NOT NULL DEFAULT FALSE | |
| `note` | TEXT NULL | Free-text: conditions, positioning |
| `display_order` | INTEGER NOT NULL DEFAULT 0 | |

Migrations are created with `./scripts/create-migration.sh <name>` — never hand-named. Per
`core/CLAUDE.md`, every new `status` value needs its CHECK constraint updated in a *new*
migration.

---

## Backend

### Battle point calculator — the one place the math lives

New `core/src/main/java/com/aboff/core/service/dh/BattlePointCalculator.java` (or
`util/BattlePointCalculator`), a pure, fully unit-tested class:

```
suggestedBudget(partySize, adjustments) = (3 * partySize) + 2 + sum(adjustment deltas)
spentPoints(adversaries, partySize):
    minionGroups = ceil(minionCount / max(partySize, 1))
    everythingElse = sum(type.battlePoints) for non-minion instances
    return minionGroups + everythingElse
```

`Encounter.calculateTotalBattlePoints()` is removed; `EncounterService` delegates to the
calculator. `AdversaryType.battlePoints` stays as the per-type cost — only the Minion
aggregation changes.

A matching **retier table** lives in one place too:
`core/src/main/java/com/aboff/core/model/dh/ImprovisedTierStatistics.java` — a static lookup
returning attack modifier, difficulty, and thresholds for a tier, applied on read when
`tier_override` is set.

### Endpoints

Existing encounter endpoints keep their paths and authorization model (creator OR
MODERATOR+ for non-official; OWNER for official). Changes:

| Endpoint | Change |
|---|---|
| `POST /api/dh/encounters` | Accept `partySize`, the six adjustment flags, `environmentId`, and richer `adversaries[]` entries (`adversaryId`, `label`, `tierOverride`) |
| `PUT /api/dh/encounters/{id}` | Same new fields |
| `GET /api/dh/encounters` | Add `mine=true` filter; add `environment` + `adversaryDetails` to `expand` |
| `GET /api/dh/encounters/{id}` | `EncounterResponse` gains `partySize`, `suggestedBattlePoints`, `spentBattlePoints`, adjustment flags, `environment` |

New run endpoints (`EncounterRunController`, `EncounterRunService`):

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/dh/encounters/{id}/runs` | Start a run — snapshots instances into `encounter_run_adversaries`. Body `{ campaignId?: number }`; **omitting it starts a standalone run** |
| `GET` | `/api/dh/encounter-runs/{runId}` | Fetch a run with all instances + expanded adversary stat blocks |
| `GET` | `/api/dh/encounter-runs?status=ACTIVE&campaignId=` | Lists runs the caller may see. **No `campaignId` → the caller's own runs** (the standalone page's "resume"); with `campaignId` → that campaign's runs (the panel) |
| `PATCH` | `/api/dh/encounter-runs/{runId}/adversaries/{instanceId}` | Update `hitPointsMarked` / `stressMarked` / `isDefeated` / `note` |
| `POST` | `/api/dh/encounter-runs/{runId}/complete` | Mark `COMPLETED`, set `ended_at` |
| `DELETE` | `/api/dh/encounter-runs/{runId}` | Discard a run |

Runs are deliberately **top-level** (`/api/dh/encounter-runs/...`) rather than nested under
campaigns — a nested path would imply a campaign is required.

**Authorization.** A single rule covers both hosts, so there is no "GM mode" branch:

> A run is visible and mutable to its `startedBy` user, **plus** — only when `campaign_id` is
> set — the GMs of that campaign, plus ADMIN+.

For a standalone run `campaign_id` is null, so the clause collapses to owner-only, satisfying
requirement #7 with no special case. Requirement #8 falls out of the campaign clause. Checks
reuse `RoleHierarchyService` and mirror `EncounterService.validateViewPermission`.

Saved *encounters* keep their existing rule: private to the creator unless `isPublic`.

Cross-cutting work required by `core/CLAUDE.md`:
- Publish `EntityChangeEvent` on run create/update/delete if runs become searchable (they
  should **not** be — skip `@SearchIndexed`; runs are transient session state)
- Add `AuditAction` values for run start/complete/discard
- **Update `core/.api-blueprint/references/encounters-api.md`** — mandated for any endpoint change

---

## Frontend

### The shared run view (built first, hosted twice)

`dawn/CLAUDE.md`: *"if reused across 2+ features, promote to `shared/components/`"* — and
*"never copy an existing component and rename it to create a variant; parameterize the
original"*. The run view has two hosts from day one, so it is born in `shared/`.

```
shared/components/encounter-run/
├── encounter-run-view.ts|html|css|spec.ts   # owns the run: load, poll-free signal state, complete
└── components/
    ├── run-adversary-row/                   # one instance: stat block + live counters
    └── run-counter/                         # +/- stepper, clamped 0..max
```

Contract — inputs only, **no `GmScreenContext`, no router reads, no campaign assumptions**:

| Member | Type | Purpose |
|---|---|---|
| `runId` | `input.required<number>()` | The only thing it needs to work |
| `density` | `input<'comfortable' \| 'compact'>('comfortable')` | `comfortable` on the standalone page, `compact` in the panel. Backed by the existing `shared/styles/density.css` |
| `showHeader` | `input<boolean>(true)` | The panel supplies its own chrome via `gm-panel.css` |
| `completed` | `output<void>()` | Host decides what follows — navigate away, or return the panel to its list |

Because every host difference is an input, the two hosts render the *same* component with the
same state logic. There is no second implementation to drift.

### New routes: `/encounters`

Lazy-loaded per `dawn/.agents/rules/angular.md`, behind the existing auth guard. No campaign
membership or role is required for any of them.

| Route | Component | Purpose |
|---|---|---|
| `/encounters` | `Encounters` | The user's saved encounters + any active runs to resume |
| `/encounters/new`, `/encounters/:id/edit` | `EncounterBuilder` | Build / edit |
| `/encounters/:id/run` | `EncounterRunPage` | **Standalone play mode** — thin shell that resolves/starts a run and renders `<app-encounter-run-view [runId] density="comfortable" />` |

```
features/encounters/
├── encounters.ts|html|css|spec.ts
├── encounter-builder/
│   ├── encounter-builder.ts|html|css|spec.ts
│   └── components/
│       ├── adversary-browser/       # multi-tier + type filters, paginated
│       ├── battle-point-meter/      # spent vs suggested, adjustment toggles
│       ├── encounter-roster/        # chosen instances, retier, label, remove
│       └── environment-picker/
└── encounter-run-page/              # standalone host for the shared run view
    └── encounter-run-page.ts|html|css|spec.ts
```

### Rebuilt GM screen panel

The panel becomes a **thin host** — a saved-encounter list plus the shared run view. It keeps
the `countdowns-panel` patterns (inject `GmScreenContext` for `campaignId`, optimistic updates
with rollback, children under `components/`), but that context is used *only* to tag the run
with a campaign and to scope the list. All fight logic lives in the shared component.

```
encounter-builder-panel/
├── encounter-builder-panel.ts|html|css|spec.ts   # saved list + run switcher
└── components/
    └── panel-encounter-row/                      # one saved encounter: name, tier, points, Run
```

`encounter-run-view` is imported from `shared/`, not redefined here.

Registration in `campaign-panels.ts` changes `colSpan: 1` → **`colSpan: 3`** and
`defaultCollapsed: true` → `false`. A running fight needs the full board width; `id` and
`defaultOrder` must not change (the `id` is the stored layout key). Consider retitling to
"Encounters" since it now runs them as well as lists them.

### Discoverability without a campaign

Add an "Encounters" entry to the navbar (`layout/navbar/`) alongside the existing tools, and
surface active runs on the dashboard, so a user with no campaign can reach the feature. Route
paths come from route constants, never re-typed literals.

### Services & models

- **`AdversaryService` gains multi-tier support.** It currently sets a single `tier` param, and
  `AdversaryController.getAllAdversaries` declares `@RequestParam(required = false) Integer tier`
  (verified). Widen both: `AdversaryFilters.tier` to `number | number[]` emitting repeated `tier`
  params, and the controller/service/repository query to `List<Integer>`. This is the only change
  needed for requirement #1's "view multiple tiers at the same time".
- New `EncounterService` / `EncounterRunService` in `shared/services/`, models in
  `shared/models/encounter-api.model.ts`. Per `dawn/CLAUDE.md` the API contract is declared
  once in `shared/models/` and features never re-declare it.
- **Battle point math is duplicated logic across the stack** — the backend is authoritative,
  but the builder needs instant feedback as adversaries are added. Put the frontend copy in
  `shared/utils/battle-points.utils.ts` with its own spec, as the "domain rules live in
  exactly one module" rule requires, and have the server value win on save.
- Retier table mirrored in `shared/utils/improvised-tier-stats.utils.ts`.

### Reuse (the #1 dawn rule)

| Need | Reuse |
|---|---|
| Adversary stat block | `shared/components/adversary-card/` — **do not fork** |
| Loading / error / grid | `card-selection-grid`, `card-skeleton`, `card-error` |
| Paging | `pagination-controls` |
| Delete confirmation | `inline-delete-confirm`, `confirm-dialog` |
| Save feedback | `saving-spinner` |
| Panel chrome | `shared/styles/gm-panel.css`, `expandable-card.css` |
| Environment display | `daggerheart-card` + the existing `environment.mapper` (maps `EnvironmentResponse` → `CardData`) — no new card component |

---

## Theming

`AdversaryCard` already matches the site: `--font-display` (Cinzel) headers, `--color-accent`
gold labels, dark gradient body. It must be **parameterized, not copied** — forking is
explicitly forbidden.

Add to `AdversaryCard`:
- an `<ng-content select="[card-actions]">` slot in the header for the encounter roster's
  remove/retier controls,
- an `<ng-content select="[card-counters]">` slot beneath the stat row for the run view's
  HP/Stress steppers,
- a `typeBadge` display of `adversaryType` (requirement #2 — the Solo/Minion tag), rendered
  from the existing `adversary-card__subtitle` treatment,
- an optional `effectiveTier` input so a retiered instance shows its adjusted stats and a
  visible "retiered from Tier N" marker.

New CSS uses `--color-*` tokens only. The existing `adversary-card.css` hard-codes hex
literals (`#2d2020`, `#ddd0c2`), which violates the tokens-first rule — new rules must not
copy that habit. A token pass over the existing file is a good follow-up but is not required
here.

---

## Testing

**Backend** (80%+ coverage mandated):
- `BattlePointCalculatorTest` — the critical one. Minion grouping at party sizes 1–6,
  exact group boundaries (4 minions / party 4 = 1 point; 5 minions / party 4 = 2), zero
  party size, empty encounter, every adjustment flag alone and combined, all ten types.
- `ImprovisedTierStatisticsTest` — every tier, out-of-range input.
- `EncounterServiceTest` / `EncounterRunServiceTest` — snapshot-on-start isolation (editing
  the encounter mid-run leaves the run intact), HP clamped to `hitPointMax`, and the
  authorization matrix explicitly:

  | Run's `campaign_id` | Owner | Campaign GM | Unrelated user | ADMIN+ |
  |---|---|---|---|---|
  | null (standalone) | allow | n/a | **deny** | allow |
  | set | allow | allow | deny | allow |

- **A standalone run must be startable by a user who belongs to no campaign at all** — an
  explicit test, since that is the regression this design exists to prevent.
- `EncounterRunControllerIntegrationTest` — full endpoint pass with `@AutoConfigureMockMvc`,
  relying on `@Transactional` rollback (no `deleteAll()` in `@BeforeEach`).

**Frontend** (all-green lint + test + build):
- `battle-points.utils.spec.ts` — mirrors the backend calculator cases exactly, so a drift
  between the two shows up as a failing test on both sides.
- `encounter-run-view.spec.ts` — the shared component tested **once**, against inputs only:
  renders at both densities, `showHeader` toggles chrome, `completed` fires. It must be
  instantiable in `TestBed` with **no `GmScreenContext` provider**; a test asserting that is
  what stops a campaign dependency creeping back in.
- Host specs stay thin: the standalone page and the panel each assert the run view renders and
  receives the right inputs — not its internals (per `.agents/rules/testing.md`, no duplicated
  assertions between parent and child).
- Component specs for the builder (add/remove instance updates the meter) and the run row
  (stepper clamps at 0 and max).
- Extend `adversary-card.spec.ts` for the new slots/inputs.

---

## Sequenced delivery

Each phase ends shippable and green.

| # | Phase | Contents |
|---|---|---|
| 1 | **Battle point correctness** | `BattlePointCalculator` + tests; `party_size` and adjustment columns; wire into `EncounterService`; update the api-blueprint. Fixes the minion bug with no UI yet. |
| 2 | **Encounter model completion** | `label` / `tier_override` / `display_order`, `environment_id`, retier table, expanded DTOs, multi-tier filter on `AdversaryController`. |
| 3 | **`/encounters` page** | Routes, list, builder, adversary browser, battle point meter, retier + environment pickers, navbar entry. Requirements 1–7 land here, campaign-free. |
| 4 | **Run backend** | `encounter_runs` tables (nullable `campaign_id`), entities, service, controller, the authorization matrix, audit actions, api-blueprint. |
| 5 | **Shared run view + standalone play** | `shared/components/encounter-run/` and `/encounters/:id/run`. **Requirement 9 is fully satisfied here with no campaign and no GM role** — this is the phase that proves the decoupling. |
| 6 | **GM screen panel** | Replace the placeholder, widen to `colSpan: 3`, saved list + host the *same* shared run view with `density="compact"`. Requirement 8 lands here and is thin by construction. |
| 7 | **Theming polish** | `AdversaryCard` slots/badges, token pass on new CSS, responsive check for the widened panel and both densities. |

Phase 5 before phase 6 is deliberate: building the standalone runner first forces the run view
to be campaign-agnostic. Doing the panel first would invite a `GmScreenContext` dependency that
phase 5 would then have to unpick.

**Follow-ups, not this work:** removing `hitPointMarked` / `stressMarked` from the catalog
`Adversary` and `UpdateAdversaryRequest`; migrating `adversary-card.css` onto design tokens;
official/prebuilt encounters; sharing a run read-only with players.

---

## Before implementation starts

`core/.claude/skills/planning` requires the approved design to be committed to
`core/docs/agent-plans/2026-08-02-encounter-manager-design.md` before any code is written.
That write is the first action after approval.

Both repos also mandate `bd` (beads) for task tracking rather than markdown TODOs — the six
phases below become `bd` issues.

## Verification

1. `cd core && ./mvnw test` — all green; new calculator tests included.
2. `cd dawn && npm run lint && npm run test:run && npm run build` — all green.
3. Start the DB (`core/scripts/start-db.sh`) and the app (`./mvnw spring-boot:run`) so Flyway
   applies the new migrations against a real Postgres — `core/CLAUDE.md` warns that unit tests
   hit mocks and miss CHECK-constraint violations.
4. Hand the user these manual QA steps (they run the servers):
   - `/encounters` → build a Tier 1 + Tier 2 encounter, confirm both tiers list together and
     each card shows its type tag.
   - Add 8 Minions with party size 4 → meter reads **2** points spent, budget **14**.
   - Toggle "more dangerous" → budget **16**. Toggle "2+ Solos" → **14**.
   - Retier a Tier 1 Standard to Tier 3 → Difficulty 17, thresholds 20/32, ATK +3.
   - Save, reload the page, confirm it persists; log in as another user and confirm it is
     not visible.
   - **Campaign-free path (the key check):** as a user who belongs to **no campaign**, open
     `/encounters/:id/run` → mark HP and Stress → refresh the browser → counters survived.
     Nothing on the page should require a campaign or a GM role.
   - Open the campaign GM screen → the encounter appears in the panel → run it → confirm the
     run view looks and behaves the same, just denser.
   - Start a run standalone, then confirm it is *not* listed in an unrelated campaign's panel;
     start one tagged to a campaign and confirm that campaign's GM can see it.

---

## Addendum — corrections from codebase research (binding)

These were established after the plan above was approved and **override** it where they conflict.

### 1. Absolute values, not deltas
There is **no `@Version` / optimistic locking anywhere** in `core`. Concurrency is handled by
convention: `PATCH /api/dh/campaigns/{id}/fear` and `PATCH /api/dh/countdowns/{id}/value` both
take **absolute** values so a fast-clicking GM or two open tabs cannot race.

The run PATCH follows this: send `hitPointsMarked: 4`, never `delta: +1`. Do not introduce
optimistic locking as a new pattern.

### 2. Delegate campaign GM checks — do not write a new one
`CampaignService.hasGameMasterAccess(campaign, auth)` is the single source of truth (creator OR
gameMaster OR moderator+). `CountdownService` already delegates to it by explicit design note.
`EncounterRunService` must delegate too. Also call
`CampaignService.validateNotEnded(campaign, operation)` before any campaign-scoped mutation.

### 3. Runs hard-delete
`Countdown` hard-deletes rather than soft-deletes because it is small ephemeral GM state, not
durable content. A finished fight is the same. `encounter_runs` gets **no `deleted_at` column**.

### 4. No real-time push exists
No WebSocket / SSE / STOMP anywhere in the backend. The run view is client-driven REST; a second
viewer sees changes on reload. Live multi-client sync is out of scope.

### 5. `ResourceTracker` must be extracted, not inlined
The HP/Stress box tracker is **not a component today** — it is the global `.resource-row` /
`.resource-box` / `.resource-box--marked` CSS in `dawn/src/styles.css`, with markup hand-inlined
in `character-sheet.html`. The run view needs the same tracker per adversary.

Extract `dawn/src/app/shared/components/resource-tracker/` wrapping those existing global classes
(inputs `max`, `marked`, `label`, `variant`; output `markedChange`), use it in the run view, **and
migrate the character sheet onto it in the same commit** — `dawn/CLAUDE.md` is explicit that an
extraction which isn't adopted is dead code with misleading passing tests. If the character-sheet
migration proves risky, file a `bd` issue rather than leaving two hand-inlined copies.

### 6. Promote `isCampaignGameMaster`
`CampaignGmScreen` duplicates a `canManage` computed with an inline comment: *"if a third consumer
appears, promote to `isCampaignGameMaster` in shared/utils/"*. The encounter panel is that third
consumer — do the promotion.

### 7. Route ordering
`/encounters/new` must be declared **before** `/encounters/:id/…`, the same trap already
documented inline in `app.routes.ts` for `campaign/:id/gm-screen`.

### 8. Panel width is functional, not cosmetic
The GM panel grid is `repeat(auto-fill, minmax(300px, 1fr))`. A `colSpan: 1` panel is ~300px —
far too narrow for a stat block. The bump to `colSpan: 3` is required, not decorative.

### 9. `environments-api.md` is missing
`.api-blueprint/references/` documents adversaries and encounters but has **no
`environments-api.md`** despite `EnvironmentController` existing. Worth adding as a small side fix
while touching the blueprint.

### 10. Content reality — measured against the local DB, not the docs

**`.research/OPEN-WORK.md` is STALE about uploads. Do not trust it for content counts.** The
following was queried directly from the local `heartandfear` database on 2026-08-02:

| Content | Local DB | Detail |
|---|---:|---|
| Adversaries | **264** | Core 129 + Hope & Fear 135 — **both books are loaded** |
| — by tier | | T1 86, T2 78, T3 55, T4 45 |
| Environments | **19** | **Core Set only. Hope & Fear's 28 environments are NOT loaded.** |

Consequences:
- Tier filtering has healthy data at every tier — the adversary browser will look properly full.
- **The environment picker can only offer 19 core options locally, and cannot be QA'd against H&F
  content.** Build the environment attachment as designed, but expect a thin picker and do not
  treat the missing H&F environments as a bug in this feature.

**Feature text really is mis-attached — verified live, still true.** This one survives the stale
doc. Queried directly:
- Feature id 414 `"Momentum - Reaction"` reads *"When the **Bear** makes a successful attack…"* and
  is attached to **21 different adversaries**.
- Feature id 434 `"Group Attack - Action"` reads *"…spotlight all **Giant Rats** within Close
  range…"* and is attached to **16**.
- Feature id 419 `"Relentless (2)"` names *"The **Construct**"*, attached to 9.
- **81 of 264 adversaries (31%) carry at least one feature row shared with another adversary.**
  Not every share is necessarily wrong, but every sampled one had creature-specific text.

Adversary *stats* (tier, type, HP, Stress, thresholds, difficulty, damage) are clean — Phases 1–4
are unaffected. **Phase 5 (the run view) is where this bites**, because the run view's whole job is
showing a GM the adversary's features. Get a decision before shipping Phase 5 on this data.

**`Feature.timing` is never populated** by the importer. `ACTION` / `REACTION` / `PASSIVE` /
`EVOLUTION` is buried in the feature `name` as a `" - Passive"` suffix, and H&F names carry an extra
`" [AdversaryName]"` importer artifact that must be stripped for display. Prefer a backfill
migration parsing the suffix; fall back to parsing in `shared/mappers/feature.mapper.ts`.

### 11. `EncounterService` has zero tests today
`encounters-api.md` says so explicitly. Anything touched here must be covered — budget for
backfill, not just new tests.
