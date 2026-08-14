# Prayer Dice Tracker — Design

Date: 2026-08-13
Scope: `core/` (persistence + API) and `dawn/` (classic and beta character sheets)

## Context

The Seraph class feature "Prayer Dice" is currently untracked. The sheet has no way to hold
the rolled d4s, mark them spent, or clear them.

### Rules as written

Core Rulebook, Seraph class feature (`resources/rules/chapters/core-01-preparing-for-adventure.md:1493`):

> At the beginning of each session, roll a number of d4s equal to your subclass's Spellcast trait
> and place them on your character sheet in the space provided. These are your Prayer Dice. You can
> spend any number of Prayer Dice to aid yourself or an ally within Far range. You can use a spent
> die's value to reduce incoming damage, add to a roll's result after the roll is made, or gain Hope
> equal to the result. At the end of each session, clear all unspent Prayer Dice.

Divine Wielder specialization feature "Devout"
(`resources/rules/chapters/core-01-preparing-for-adventure.md:1515`):

> When you roll your Prayer Dice, you can roll an additional die and discard the lowest result.

Consequences that shape this design:

| Rule | Design consequence |
|---|---|
| Reset is **per session**, not per rest | No rest-workflow integration. A manual reset control instead. |
| `core-02:800` — "once per session" features "don't refresh during rests" | Confirms the above; the rest flow must not touch Prayer Dice. |
| Count = the **subclass's Spellcast trait** value | Read `subclassCards[].spellcastingTrait`, resolve against the sheet's modified trait value. |
| Devout rolls one extra die, discards the lowest | Roll N+1 d4s, drop the lowest, keep N. |
| Trait value ≤ 0 | Roll zero dice (literal reading). Panel shows an explanatory empty state. |
| Dice are spent for their **value** | Each die must show its face value in both ready and spent states. |

Prayer Dice is a **class** feature, so gating matches `hasWarlockResources` (scans
`classes[].classFeatures[]`), not `hasMartialStances` (which scans subclass features).

### Decisions taken

1. Persist as a single `VARCHAR` column, not a child table.
2. A Spellcast trait of 0 or less rolls zero dice (rules as written).
3. Dice render as triangular d4 faces, distinct from the square pips used by every other tracker.

## Approach

### Persistence

One nullable column on `character_sheets`:

```sql
ALTER TABLE character_sheets
    ADD COLUMN prayer_dice VARCHAR(64) NULL;
```

Wire format: comma-separated face values in roll order, `*` suffix marking a spent die.

```
"3,1*,4,2"   -> rolled 3, 1, 4, 2; the 1 has been spent
NULL / ""    -> no dice rolled this session
```

Chosen over a child table because the list is bounded (at most ~8 small integers), the sheet
stores every other resource as a flat column (`focus_marked`, `favor`, `combo_die`), the backend
has no existing `AttributeConverter`/`@ElementCollection` precedent to follow, and a plain
`VARCHAR` behaves identically on Postgres and on H2 in tests.

The API contract stays structured — the wire format never leaks past the service layer:

```json
"prayerDice": [
  { "value": 3, "spent": false },
  { "value": 1, "spent": true  },
  { "value": 4, "spent": false }
]
```

### Devout

Rolling the extra die and dropping the lowest can only improve the dice kept — every use of a
Prayer Die (reduce damage, add to a roll, gain Hope) wants a higher number — so Devout is applied
by default rather than prompted for at each roll. A "Use Devout" checkbox in the panel, checked by
default, keeps the rules' "can" available.

The toggle is deliberately **not persisted**. It only has an effect at the instant of a roll, and
Prayer Dice are rolled once per session, so a stored value would never be read between the moment
it is set and the moment it is used.

The discarded die is reported as a caption ("Devout — rolled 5, dropped a 1."), matching the
existing Refresh Focus caption, rather than as an unspendable die in the row. This keeps the dice
row meaning exactly one thing — dice you have — and needs no third die state in the schema.

### Rolling

Client-side, consistent with `refreshFocus`: "every roll in the app is client-side, with the
server only storing the result." The existing `DiceRollerService` is used directly, bypassing the
roll overlay, exactly as `refreshFocus` does.

Roll logic lives in one tested pure util:

```
rollPrayerDice(spellcastTrait, hasDevout, roll):
  count = max(spellcastTrait, 0)
  if count == 0            -> []
  if hasDevout             -> roll count+1 d4s, drop one lowest, keep count
  else                     -> roll count d4s
```

### Shared-rule extraction

Spellcast-trait resolution already exists, privately, in the beta rest mapper
(`rest-state.mapper.ts:62`). Two copies of one game rule is the exact hazard `dawn/CLAUDE.md`
calls out, so it moves to a shared util and the rest mapper imports it.

Likewise `hasClassFeatureNamed` is currently private in `hf-class-resource-access.utils.ts`; it
is extracted so the new Prayer Dice gate reuses it rather than re-implementing the scan.

## File changes

### `core/`

| File | Change |
|---|---|
| `src/main/resources/db/migration/V<ts>__add_prayer_dice_to_character_sheets.sql` | New. Adds `prayer_dice VARCHAR(64) NULL`. |
| `model/entity/dh/CharacterSheet.java` | New `prayerDice` String column, documented like `focusMarked`. |
| `model/dto/dh/PrayerDieDto.java` | New. `{ value, spent }`. |
| `model/dto/dh/request/UpdateCharacterSheetRequest.java` | New nullable `List<PrayerDieDto> prayerDice`. |
| `model/dto/dh/response/CharacterSheetResponse.java` | New `List<PrayerDieDto> prayerDice`. |
| `service/dh/PrayerDiceCodec.java` | New. Parse/format the wire string; the only place that knows about `*`. |
| `service/dh/CharacterSheetService.java` | Map the field on read and on update, via the codec. |

Validation: each die value 1–4, at most 16 dice, so a malformed or hostile payload cannot
overflow the column.

### `dawn/`

`CharacterSheetBeta extends CharacterSheet`, so all component logic is written once in the
classic component and inherited by beta. Only templates and stylesheets are touched twice.

| File | Change |
|---|---|
| `shared/models/...` / `create-character/models/character-sheet-api.model.ts` | `PrayerDie` type; `prayerDice` on the sheet response and update request. |
| `features/character-sheet/utils/class-feature-access.utils.ts` | New. Exports `hasClassFeatureNamed`. |
| `features/character-sheet/utils/hf-class-resource-access.utils.ts` | Import the extracted helper; drop the local copy. |
| `features/character-sheet/utils/prayer-dice-access.utils.ts` | New. `hasPrayerDice` (class feature "prayer dice"), `hasDevout` (subclass feature "devout"). |
| `features/character-sheet/utils/spellcast-trait.utils.ts` | New. Extracted `resolveSpellcastTrait`. |
| `features/character-sheet-beta/components/rest/utils/rest-state.mapper.ts` | Use the extracted resolver; delete the private copy. |
| `features/character-sheet/utils/prayer-dice.utils.ts` | New. `rollPrayerDice`, plus `spendPrayerDie` / `restorePrayerDie`. |
| `features/character-sheet/character-sheet.ts` | `showPrayerDice()`, `prayerDice()`, `prayerDiceReadyCount()`, `rollPrayerDice()`, `togglePrayerDie()`, debounced `prayerDiceSave$`. |
| `shared/components/prayer-dice-tracker/` | New presentational component: dice row, reset glyph button, empty state. Used by both sheets. |
| `features/character-sheet/character-sheet.html` | Render the panel after Hope & Stress. |
| `features/character-sheet-beta/character-sheet-beta.html` | Same placement. |
| `features/character-sheet/character-sheet-panels.css` / `character-sheet-beta-cards.css` | Panel-level styles only; the dice themselves are styled inside the new component. |

Placement in both sheets: its own `.panel`, in the same column, immediately after the
Hope & Stress panel and before Focus/Favor — the position the request asked for.

### Visual design

The panel borrows the existing shell wholesale — `.panel`, `.panel__title`, gold hairlines,
Cinzel micro-labels, 2px radii, `--color-accent` `#d4a056` on `--color-bg-dark`. One element
carries the identity: the dice are **triangles**, because a d4 is a tetrahedron and reads as a
triangle in the hand. Every other tracker on the sheet is a square pip, so Prayer Dice are
identifiable without reading the heading.

Each die is a `<button>` containing an inline SVG triangle (`stroke: currentColor`) with the face
value as real centered text, so it scales and stays selectable and screen-reader legible.

| State | Treatment |
|---|---|
| Ready | Solid gold outline, parchment numeral, faint gold inner wash. |
| Spent | Dashed gold outline at reduced opacity, numeral dimmed. Value stays readable until reset. |
| No dice | Short guidance line naming the current Spellcast trait and value. |

Reset control: a small square glyph button (`↻`) on the title row next to the dice, matching
`.favor-btn`'s hairline treatment. Accessible name states what it does — rolls a fresh set for a
new session — rather than just "reset". Owner-only, like `refreshFocus`.

Quality floor: visible keyboard focus on every die and on the reset button, `aria-pressed` on
dice, an `aria-live` summary of how many remain ready, and the whole row wrapping on narrow
viewports.

## Testing strategy

### `core/`
- `PrayerDiceCodec` round-trip: empty, null, all-ready, all-spent, mixed, malformed input.
- Service: update persists dice; read returns the structured list.
- Validation: value outside 1–4 and an over-long list are both rejected.

### `dawn/`
- `prayer-dice.utils.spec.ts`: trait 0 and negative give no dice; trait N gives N dice; Devout rolls
  N+1 and drops exactly one lowest (including when the lowest is tied); every face is 1–4.
- `prayer-dice-access.utils.spec.ts`: gate is true only with the class feature; case/whitespace
  insensitive; missing arrays return false rather than throwing.
- `spellcast-trait.utils.spec.ts`: covers the cases the rest mapper's private copy covered.
- `prayer-dice-tracker.spec.ts`: renders one die per value, spent styling, toggle emits, reset
  emits, empty state renders, controls disabled for non-owners.
- Sheet specs: panel renders only when gated on, and toggling a die persists.

Quality gates: `npm run lint && npm run test:run && npm run build` green in `dawn/`, and the
backend test suite green in `core/`.

## Explicitly out of scope

- Rest integration. Prayer Dice reset per session, not per rest; the rest flow is untouched.
- Applying a spent die's effect (adding to a roll, reducing damage, gaining Hope). This ships as a
  tracker only; the player applies the value themselves, as with every other resource on the sheet.
- The roll overlay. Rolling is deliberately silent.
