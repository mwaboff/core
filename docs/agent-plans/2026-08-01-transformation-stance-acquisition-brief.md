# Implementation Brief — Acquiring Transformations & Martial Stances

**Date:** 2026-08-01
**Repo:** `dawn` only (backend is complete — see "Backend is done" below)
**Follows:** `2026-08-01-transformations-and-martial-stances-design.md`

## Why this exists

The prior work built panels that **display** an attached transformation and known martial stances, and enforced every acquisition rule server-side. It never built the UI that **sets** that state. Today a transformation can only be attached with raw SQL, and known stances can only be seeded by hand. This brief closes that gap.

---

## 0. MANDATORY FIRST STEP — load the `frontend-design` skill

Before writing any component, template, or CSS, **invoke the `frontend-design` skill** and follow it.

This work adds selection UI to two of the most visually established surfaces in the app — the character creation wizard and the character sheet. It must not read as bolted on. Specifically:

- **Match the existing selection idiom.** Creation and level-up already have card-grid pickers (`create-character/components/ancestry-selector`, `level-up/components/domain-card-step`). A new step must look like it shipped with them — same card chrome, same selected/disabled affordances, same counter language ("2 of 2 chosen").
- **Reuse the established vocabulary, don't invent one.** The sheet uses `.shield`, `.resource-box`, `.card-group`, and the panel classes in `character-sheet-panels.css`. The GM screen uses `.gm-panel__*`. Do not introduce a parallel set.
- **Respect the theme.** Colors come from the existing CSS custom properties (`--ca`, `--cg`, `--cl`, `--cp`); do not hardcode hex values.
- **Tier grouping already has a visual precedent** in `martial-stance-panel` and the beastform section (forms grouped/filtered by tier). The stance picker should feel like the same family.

Use the skill to make deliberate choices about hierarchy, density, and empty states — not to redecorate. If the skill's guidance conflicts with an existing pattern in these files, **the existing pattern wins**; consistency is the goal here.

---

## 1. The rules, verbatim

Everything below is quoted exactly from `resources/rules/chapters/hopeandfear-01-character-options.md`. These quotes are the acceptance criteria.

### 1.1 Transformations — who grants them, and when

> "These are optional aspects of a character's identity that the GM can give out during a campaign as part of the narrative or, at their discretion, present as an option during character creation." (line 952)

> "A PC can have only one transformation." (line 960)

> "Like ancestry and community cards, a transformation card doesn't count against your domain card limit. When your character gains a transformation, add the card to your loadout as if it were part of your character's heritage. Consider how this transformation changes your character physically and psychologically—if you need inspiration, each transformation includes questions to help you." (line 956)

> "Some transformations are more likely to be taken at character creation, such as the shapeshifter or demigod. However, they can still be introduced as a character twist during a campaign. It's ultimately up to you which transformations are available at character creation and how they become available throughout play—decide what works best for you, your players, and the story you're telling together." (line 970)

**Design consequence:** acquisition is **not** gated to a single moment. A transformation can arrive at creation *or* mid-campaign. Therefore the primary control belongs **on the character sheet** (available any time), not exclusively in the creation wizard. There is no class, level, or domain gate to enforce.

### 1.2 Martial Stances — how many, and when

> "***Stance Fighter***
>
> You can channel your inner resolve to shift into martial stances that grant you special benefits in combat. Take the Martial Stances sheet and choose two martial stances from Tier 1. Each time you level up your character, choose an additional stance from your tier or lower." (lines 250–252)

> "You choose two stances from Tier 1 when you first take the Martial Artist's subclass foundation card. Mark the circle next to each to indicate that your character knows and has access to them. Mark a new stance from your tier or below each time you gain a level." (line 300)

> "When you choose the Martial Artist subclass, take the Martial Stances sheet to track which stances your character knows. You can also track which stance you have active and your current Focus." (line 294)

**Design consequence:** two distinct acquisition moments —
1. **On taking the Martial Artist subclass** → choose exactly **2**, both **Tier 1**.
2. **Each level up** → choose exactly **1**, from **your tier or lower**.

### 1.3 Rules already enforced (do not reimplement client-side)

> "You can shift only into stances you've marked as known." (line 306)
> "You can't shift into or have more than one active stance at a time." (line 308)
> "You can spend a Focus to shift into a martial stance and gain its effects." (line 306)

The server already rejects an active stance that isn't known, and a known stance above the character's tier. The UI should *prevent* those states, but must not be the only thing preventing them.

---

## 2. Backend is done — no `core` changes required

Every field needed already exists on `PUT /api/dh/character-sheets/{id}` (`UpdateCharacterSheetRequest`). Do **not** add endpoints.

| Field | Type | Meaning |
|---|---|---|
| `transformationCardId` | `Long` | Attach/replace the transformation |
| `clearTransformationCard` | `Boolean` | Remove it (null can't mean both "unchanged" and "clear") |
| `knownMartialStanceIds` | `List<Long>` | **Full replacement** of the known set |
| `activeMartialStanceId` | `Long` | Shift into a stance |
| `clearActiveMartialStance` | `Boolean` | Drop out of the stance |
| `focusMarked` / `focusMax` | `Integer` | Clamped `0..focusMax` server-side |

> ⚠️ `knownMartialStanceIds` **replaces** the whole collection. When adding one stance at level-up you must send the existing ids plus the new one, not just the new one.

### 2.1 Example interactions

**Fetch the catalogs** (services already exist — `shared/services/transformation-card.service.ts`, `martial-stance.service.ts`):

```ts
// TransformationCardService — already requests expand=features,questions
this.transformationCardService.getAllTransformationCards();  // TransformationCardResponse[]
this.martialStanceService.getAllMartialStances();            // MartialStanceResponse[]
```

Raw equivalents:
```
GET /api/dh/transformation-cards?expand=features,questions&size=100
GET /api/dh/martial-stances?size=100
```

**Attach a transformation:**
```ts
this.characterSheetService.updateCharacterSheet(sheetId, { transformationCardId: 5 });
```
```json
PUT /api/dh/character-sheets/2
{ "transformationCardId": 5 }
```

**Remove a transformation:**
```ts
this.characterSheetService.updateCharacterSheet(sheetId, { clearTransformationCard: true });
```

**Choose the first two stances (creation / subclass pick):**
```json
PUT /api/dh/character-sheets/12
{ "knownMartialStanceIds": [1, 4] }
```

**Add one stance at level-up — send existing + new:**
```ts
const next = [...this.knownStanceIds(), newStanceId];
this.characterSheetService.updateCharacterSheet(sheetId, { knownMartialStanceIds: next });
```

**Local catalog ids for testing** — transformations: `1` Demigod, `2` Ghost, `3` Reanimated, `4` Shapeshifter, `5` Vampire, `6` Werewolf. Stances: tier 1 = `1–4`, tier 2 = `5–8`, tier 3 = `9–12`, tier 4 = `13–16`.

---

## 3. Work item A — Transformation acquisition (character sheet)

Because a transformation can be granted at any point in a campaign, the control lives on the sheet.

**Files:**
- `dawn/src/app/features/character-sheet/components/transformation-panel/` — extend
- `dawn/src/app/features/character-sheet/character-sheet.ts` / `.html` — wire handler

**Behavior:**
1. When **no** transformation is attached and the viewer is the owner, the panel renders an empty state with a **"Add a Transformation"** action rather than not rendering at all. Currently `@if (transformationCard(); as card)` hides the panel entirely — that must change, or the player has no entry point.
2. Choosing opens a picker listing all 6 cards with name, description, and their 2 features. Reuse the card-grid idiom from `ancestry-selector`.
3. Selecting sends `{ transformationCardId }`, optimistically updates, rolls back on error — mirror the existing `onTransformationTokensChange` handler's pattern exactly.
4. An attached transformation offers **Change** and **Remove**. Because *"A PC can have only one transformation,"* Change must **replace**, never add — the single FK enforces this, but the UI must not imply a multi-select.
5. Surface the 6 questions as reflection prompts, per *"each transformation includes questions to help you."* They are narrative aids — display only, no persistence.

**Empty state copy** should reflect that this is GM-granted, not self-serve shopping — e.g. "Your GM may grant a transformation during play."

---

## 4. Work item B — Initial 2 stances (character creation)

**Files:**
- `create-character/models/create-character.model.ts` — add `'martial-stances'` to `TabId` and a `CHARACTER_TABS` entry, placed **immediately after `'subclass'`**
- `create-character/components/` — new `martial-stance-selector/` (`.ts/.html/.css/.spec.ts`), modeled on `ancestry-selector`
- `create-character/create-character.ts` — conditional inclusion + validation

**Behavior:**
1. The step appears **only** when the chosen subclass grants Stance Fighter. Reuse `hasMartialStances()` from `character-sheet/utils/martial-stance-access.utils.ts` — if importing across features is awkward, move that util to `shared/utils/`, but do **not** duplicate the predicate.
2. Offer **Tier 1 only** — *"choose two martial stances from Tier 1."* Tiers 2–4 must not be selectable here. Showing them greyed with a "Tier 2 — unlocked at level 5" hint is good UX; showing them as choosable is a rules bug.
3. Require **exactly 2**. Block advancing at 1 or 3, using the same validation affordance other steps use.
4. `CHARACTER_TABS` is filtered elsewhere (`create-character.ts` already does `CHARACTER_TABS.filter(t => t.id !== 'bonuses')`) — follow that mechanism rather than inventing new conditional-tab logic.
5. Persist via `knownMartialStanceIds: [a, b]` on sheet creation/update.

---

## 5. Work item C — +1 stance per level (level-up)

**Files:**
- `level-up/models/level-up.model.ts` — add to `LevelUpTabId` / `ALL_LEVEL_UP_TABS`
- `level-up/components/` — new `martial-stance-step/`, modeled on `domain-card-step/`
- `level-up/level-up.ts` — conditional inclusion

**Behavior:**
1. Step appears only for characters with Stance Fighter, once per level-up.
2. Offer stances of **the character's tier or lower** — *"choose an additional stance from your tier or lower."* Tier from level: 1 → T1, ≤4 → T2, ≤7 → T3, else T4. `tierForLevel()` already exists in `beastform-access.utils.ts` — **reuse it**, don't rewrite it.
3. Already-known stances render as selected/disabled, not hidden — the player should see their whole sheet filling in, matching the printed tracking sheet's "mark the circle" metaphor.
4. Require **exactly 1**.
5. Persist as **existing ids + new id** (see the §2.1 warning).

---

## 6. Testing

Per `dawn` conventions (Vitest, co-located `.spec.ts`):

- Selector specs: renders only for Stance Fighter characters; Tier 1 only at creation; tier-or-lower at level-up; exact-count validation at boundaries (1, 2, 3 selected); known stances disabled.
- Transformation panel specs: empty state renders the add action for owners and not for non-owners; select → PUT payload shape; replace sends `transformationCardId` (not an array); remove sends `clearTransformationCard: true`; error path rolls back.
- Level-up spec: the PUT contains **existing + new** ids — this is the most likely regression, since sending only the new id silently wipes prior stances.
- Update `create-character` and `level-up` tab-count assertions; several specs hardcode tab counts and will fail otherwise.

**Gates (all must pass):** `npm run test:run` · `npm run lint` · `npx tsc --noEmit` · `npm run build`

---

## 7. Out of scope

- No `core` changes. If something seems to need one, re-read §2 first.
- No GM-side granting flow in the GM screen. The rules make granting a conversation (*"GMs should discuss transformations with your players"*), and the sheet-side picker covers both creation and mid-campaign acquisition. A campaign-level grant UI can come later if wanted.
- Focus refresh, stance shifting, Feed tokens, and Wolf Form are **already built** — this brief only covers acquisition.
