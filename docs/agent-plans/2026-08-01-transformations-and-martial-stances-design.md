# Transformations & Martial Stances — Design

**Date:** 2026-08-01
**Repos touched:** `core` (backend), `dawn` (frontend), `hope_and_fear-import` (payloads)

---

## 1. Context

Hope & Fear introduces two player-facing systems that are not yet usable in the app:

- **Transformations** — 6 narrative identity cards (Demigod, Ghost, Reanimated, Shapeshifter, Vampire, Werewolf), any class, GM-granted, **one per PC**, and explicitly *not* counted against the domain card loadout limit.
- **Martial Stances** — 16 stances (4 per tier × 4 tiers) belonging to the **Martial Artist** subclass of **Brawler**, powered by a new **Focus** resource.

### What already exists (verified 2026-08-01)

Both catalogs shipped on 2026-07-30 and are complete:

| Layer | Transformation | Martial Stance |
|---|---|---|
| Migration | `V20260730125836080__create_transformation_cards.sql` | `V20260730130448355__create_martial_stances_table.sql` |
| Entity | `TransformationCard` | `MartialStance` (extends `BaseItem`) |
| Controller | `/api/dh/transformation-cards` | `/api/dh/martial-stances` |
| Bulk | `POST /api/dh/transformation-cards/bulk` | `POST /api/dh/martial-stances/bulk` |
| Service/Repo/DTOs/Search | ✅ | ✅ |
| `dawn` bulk upload + edit schema | ✅ | ✅ |

> **Correction to `.research/OPEN-WORK.md`.** Two errors in that file were confirmed by reading source:
> 1. Line 147 gives the transformation bulk path as `POST /api/dh/cards/transformation/bulk`. **That path does not exist.** The real path is `POST /api/dh/transformation-cards/bulk` (`TransformationCardController.java:30` + `:120`). `dawn`'s `ENDPOINT_MAP` is already correct, so this is a documentation error only.
> 2. Line 139 says **12** Martial Stances. There are **16** — verified against `hopeandfear-05-appendix.md:213-240` and the identical class-writeup copy. A payload authored from that line would silently drop 4 stances.

### What is missing

1. No content payloads for either system.
2. No storage for the 36 Transformation Questions (6 per card × 6 cards).
3. No character-sheet state: no attached transformation, no known/active stances, no Focus.
4. No character-sheet UI for either system.
5. Neither type is browsable in admin (`SUPPORTED_BROWSE_TYPES` excludes both).
6. No GM screen reference content.

### Decisions taken (by the user, this session)

| Decision | Choice |
|---|---|
| Scope | Full vertical slice — payloads, schema, sheet state, sheet UI, admin browse |
| Transformation Questions storage | New `transformation_card_questions` join table |
| Questions in bulk payload | Inline in the card payload, find-or-create like `features` |
| Vampire Feed tokens + Wolf Form | Build now |
| Other new H&F resources | Also build **Combo Die** and **Favor**; document the rest |
| Issue tracking | **Do not use `bd` this session** (overrides `core/CLAUDE.md`) |

---

## 2. Rules Reference

### Transformations
- One transformation per PC; does not count against domain card limit.
- Each card: name, 2–3 paragraphs of prose, **exactly 2** named features, **exactly 6** questions. Verified: no card deviates.
- No statblock — no tier, traits, evasion, or thresholds. All numbers live inside feature prose.
- **Vampire "Feed"**: tokens on the card, cap **6**; gained by marking Stress on a successful Fangs bite (tokens = HP the target marked); spend 1 to upgrade the Fear Die to d20; **remove 1 per long rest** (decay, not reset); at 0 tokens, action and reaction rolls have disadvantage.
- **Werewolf "Wolf Form"**: enter by marking a Stress after marking 1+ HP; +1d10 attack/damage; gaining Hope forces an extra Stress; ends on Howling Rampage (mark last Stress → roll d20s equal to tier, AoE damage) or on a rest.

### Martial Stances
- Granted by the Martial Artist foundation feature **"Stance Fighter"**.
- Know 2 Tier-1 stances at pick; **+1 stance per level** at current tier or lower.
- **One active stance** at a time. Enter by spending **1 Focus**.
- Drops on: Severe damage · marking last Hit Point · shifting to another stance · end of scene.

### Focus
- Cap **6**. Refresh once per rest: **clear the track, roll d6s equal to Instinct, gain Focus equal to the highest single die** — not a sum, not a reset-to-max.
- Also gained mid-scene by some stances (e.g. Invigorating).

---

## 3. Approach

### 3.1 Guiding patterns followed

- **Resources mirror Hope.** `hope_max`/`hope_marked` where *marked = amount currently held*. Focus adopts this exactly.
- **No per-resource endpoints.** All resources are optional fields on the single `PUT /api/dh/character-sheets/{id}` partial-update DTO. Focus, Favor, Combo Die, and transformation state follow suit. The one exception is the Focus refresh roll (§3.3).
- **Numeric resource columns are `NOT NULL DEFAULT`**, matching every existing resource on the sheet (`hit_point_*`, `stress_*`, `hope_*`, `armor_*` are all `@Column(nullable = false)`). An earlier draft proposed nullable columns meaning "not applicable to this class"; that was **rejected on review** — there is no nullable *numeric resource* precedent in the codebase (the only nullable class-specific field, `activeBeastform`, is a foreign key, not a counter), and three-valued logic would force new `?? 0` / `=== null` guards through level-up, response mapping, frontend gating, and every existing clamp path. Visibility is already handled by feature-gating in the UI, so a column that is `0` for a non-Warlock is harmless and invisible.
- **Nullable is still correct for references and genuinely-absent values**: `combo_die`, `transformation_card_id`, `active_martial_stance_id`, `transformation_tokens`.
- **Gate on feature name, not class string** — as `hasBeastformFeature()` does, so multiclass and homebrew work.
- **Additive migrations only**, created via `./scripts/create-migration.sh`.

### 3.2 Migrations (`core`)

| # | Name | Contents |
|---|---|---|
| 1 | `create_transformation_card_questions` | Join table `(transformation_card_id, question_id)`, composite PK, CASCADE both sides. Mirrors `class_background_questions`. |
| 2 | `add_hf_resources_to_character_sheets` | `focus_marked INT NOT NULL DEFAULT 0`, `focus_max INT NOT NULL DEFAULT 6`, `favor INT NOT NULL DEFAULT 0`, `combo_die VARCHAR(10) NULL`, `transformation_card_id BIGINT NULL FK`, `transformation_tokens INT NULL`, `wolf_form_active BOOLEAN NOT NULL DEFAULT FALSE`, `active_martial_stance_id BIGINT NULL FK` |
| 3 | `create_character_sheet_martial_stances` | Known-stance join table `(character_sheet_id, martial_stance_id)`, composite PK, CASCADE. |

`combo_die` stores a `DiceType` enum value (`D4`…`D20`, `model/enums/DiceType.java`) as `VARCHAR(10)` via `@Enumerated(EnumType.STRING)`, matching how `DamageRoll` and `beastforms.damage_dice_type` persist it.

> **No CHECK constraint to update here.** An earlier draft told the implementer to check the `dice_type` CHECK constraint; **there isn't one.** Dice columns are unconstrained `VARCHAR(10)` throughout, unlike `feature_type` / `question_type` / `damage_type`, which do have CHECKs. Do not invent one — it would diverge from existing convention. (`core/CLAUDE.md`'s constraint-checking rule still applies to any *other* enum touched.)

**`wolf_form_active` is deliberately Werewolf-specific.** It was originally drafted as a generic `transformation_active`, which is meaningless for 5 of the 6 cards: Demigod, Ghost, Reanimated and Shapeshifter are always-on passives with no toggle, and Vampire's mechanic is a token pool, not a boolean. "Is the transformation in effect" is not a rules concept. The column is named for the one mechanic it actually gates, and is `FALSE` for every other transformation.

#### Die-size resources: stored vs. derived

Two Hope & Fear resources are *die sizes* rather than counters. They are treated differently based on whether the value is automatic:

| Die | Rule | Storage | Rendering |
|---|---|---|---|
| **Patron Die** (Warlock) | `d6`, automatically `d8` at level 5 | **Not stored** — derived from level on the frontend | Shield |
| **Combo Die** (Brawler) | Starts `d4`; **once per tier** the player *may* spend a level-advancement option to step it up | **Stored** (`combo_die`) — it is a player choice, so two Brawlers of the same tier can differ | Shield |

Both render as a conditional **shield** alongside the existing Evasion / Armor / Prof shields (`character-sheet.html:51-70`) — a plain `.shield` div with `.shield__label` and `.shield__value`, no component involved. Each shield renders **only** when the character has the granting class/feature, using the same gating predicates as the panels (§3.4).

Patron Die is the clean case for a frontend-only calculation: it has no player input, so persisting it would create a value that can silently drift out of sync with level. Combo Die cannot be derived and must be persisted.

### 3.3 Backend changes

**Transformation questions — reuse existing machinery, do NOT build new**

All of this already exists and is production-proven; an implementing agent must **not** create a parallel DTO or service:

| Component | Status |
|---|---|
| `model/dto/dh/request/QuestionInput.java` | ✅ exists |
| `QuestionService.findOrCreate(QuestionInput)` and `.resolveQuestions(List<Long>, List<QuestionInput>)` | ✅ exists (`QuestionService.java:200-257`) |
| `QuestionType.TRANSFORMATION` + widened DB CHECK | ✅ shipped 2026-07-30 |
| `FeatureType.TRANSFORMATION` + widened DB CHECK | ✅ shipped 2026-07-30 |
| Working precedent | `ClassService` — `backgroundQuestions` / `connectionQuestions` on `CreateClassRequest.java:61,65` |

- `TransformationCard` gains `@ManyToMany questions` via the new join table.
- `CreateTransformationCardRequest` / `UpdateTransformationCardRequest` gain `questionIds` + `questions` (`List<QuestionInput>`), resolved via `QuestionService.resolveQuestions` — copy how `ClassService` does it.
- `TransformationCardResponse` gains `questionIds`, with `?expand=questions`.

**Character sheet**
- `CharacterSheet` gains: `transformationCard`, `transformationTokens`, `transformationActive`, `knownMartialStances`, `activeMartialStance`, `focusMarked`, `focusMax`, `favor`, `comboDie`.
- `CharacterSheetResponse` exposes all of them. Unlike the vestigial `activeBeastform` (which exists in the entity but appears in **zero** DTOs/services/controllers), these must be fully wired.
- `UpdateCharacterSheetRequest` gains matching optional fields with the same clamp-on-max-change logic used for HP/Stress/Hope.

**Combo Die must go through the advancement system, not a bare column**

"Once per tier, increase your Combo Die by one step" is a *rationed level-up choice*, mechanically identical to advancements the app already rations. Exposing `comboDie` only on the generic PUT would let any client step it twice in a tier, or change it outside level-up entirely.

The app already has the machinery: the `AdvancementType` enum (with `minTier`), the `CharacterAdvancementLog` entity, and `LevelUpService`'s tier-scoped `usageMap` that enforces per-tier usage counts (`LevelUpService.java:83-99`) for options like `BOOST_PROFICIENCY`. So:

- Add an `UPGRADE_COMBO_DIE` member to `AdvancementType` (this name is already the one used in the project's deferred-work notes, confirming the intended design) and wire it through `LevelUpService` with a per-tier limit of 1.
- **Check the advancement-type CHECK constraint** before adding the enum value — this one *does* exist, unlike the dice one.
- `combo_die` is then written by the level-up path, not by arbitrary client PUTs.

**Rules enforced server-side**
- At most one transformation per sheet (single FK — structural).
- Active stance must be in the known set.
- Known stance tier ≤ character tier.
- Focus clamped to `0..focus_max`; transformation tokens clamped to `0..6`.

**No new endpoint — the Focus refresh roll happens client-side**

An earlier draft proposed `POST /api/dh/character-sheets/{id}/focus/refresh` performing the roll server-side. **Rejected on review.** `core` contains **zero** server-side randomness — no `Random`, `SecureRandom`, `ThreadLocalRandom`, or `.nextInt(` anywhere in `src/main/java` outside tests. Every roll in this app (attack, damage, duality dice, Combo Die chains) is client-side via `dawn`'s `dice-roller.service.ts`, with the server only storing results. That endpoint would have been the first server-side RNG in the codebase, and would have required building a seeded-randomness abstraction from scratch purely to make it testable.

Instead: the **Refresh Focus** button rolls `Instinct`-many d6 through the existing `dice-roller.service.ts`, takes the highest, and PUTs the result as `focusMarked` through the ordinary partial-update DTO. Consistent with every other roll in the app, and no new endpoint.

**API blueprint** — `core/CLAUDE.md` requires updating `core/.api-blueprint/` for any endpoint/DTO change. Add references for both systems.

### 3.4 Frontend changes (`dawn`)

- **Models/services/mappers** for `TransformationCard` and `MartialStance`, mirroring `beastform-api.model.ts` / `beastform.service.ts` / `beastform.mapper.ts`.
- **Admin browse** — add `TRANSFORMATION_CARD` and `MARTIAL_STANCE` to `SUPPORTED_BROWSE_TYPES` (`codex-browse.service.ts:30-33`) and `BROWSABLE_TYPES` (`search.model.ts:141-153`), plus paginated service, `CardType` member, mapper, and `browse()` arm. Closes a known gap.
- **Character sheet**
  - Focus pip row reusing `.resource-box` + `toggleResourceBox`, extending the `'hp' | 'stress' | 'hope' | 'armor'` union with `'focus'`, plus a **Refresh Focus** button calling the new endpoint.
  - Transformation panel: attached card, its 2 features and 6 questions, Vampire token pool, Wolf Form toggle.
  - Martial stance panel: known stances grouped by tier, active-stance selection (spending 1 Focus), and the drop conditions shown as a reminder.
  - Favor counter (Warlock) alongside Focus.
  - Conditional **shields** for Combo Die (Brawler, stored) and Patron Die (Warlock, computed from level — no backend field), rendered beside Evasion/Armor/Prof and shown only when the granting feature is present.
  - Gating via new predicates in `character-sheet/utils/` beside `beastform-access.utils.ts`. **Resolved:** "Stance Fighter" is a *subclass* foundation feature, so the predicate scans `subclassCards[].features[].name` — **not** `classes[].classFeatures` as `hasBeastformFeature` does. `CharacterSheetResponse.subclassCards?: SubclassCardResponse[]` exists (`character-sheet-api.model.ts:300`), `SubclassCardResponse.features` carries name/description plus a `SubclassLevel` of `FOUNDATION`/`SPECIALIZATION`/`MASTERY` (`subclass-api.model.ts:28-49`), and `character-sheet.ts:176-189` already requests `expand=subclassCards`. **No expand-param change needed.** Match case/whitespace-insensitively and return `false` (never throw) when the collection is absent, mirroring `hasBeastformFeature`.
- Save via the existing optimistic-signal + `debounceTime(800)` + `switchMap` PUT pipeline, with rollback on error.

### 3.5 GM screen reference content

Add a static content file `dawn/src/app/features/gm-screen/content/transformations-stances.content.ts` exporting `GmPanelDef[]` with `body: { kind: 'static', blocks: [...] }`, spread into `content/panel-registry.ts`, and add the ids to `panel-registry.spec.ts`'s `EXPECTED_IDS`.

**No component and no route change** — the static block renderer already supports text/list/keyValue/table/steps/callout. (`countdown-help` is *not* the model to copy; it is a bespoke component only because it is nested inside a live, stateful panel.)

Content: what a transformation is and the one-per-PC rule; the Focus economy; stance entry cost and the four drop conditions; a tier→stance table.

### 3.6 Payloads (`hope_and_fear-import`)

- `6 transformation cards` — name, description prose, 2 inline features (`featureType: TRANSFORMATION`), 6 inline questions (`questionType: TRANSFORMATION`), `expansionId: 2`.
- `16 martial stances` — name, `tier`, `description` = effect text, `isOfficial: true`, `expansionId: 2`. **No feature rows**: a printed stance is a name plus one effect sentence, so inventing sub-features would add structure the card does not have.

**Parsing traps** (non-card material interleaved in the chapter, must not land in `description`): `### Granting Transformations` (GM advice), `#### Examining Divinity in Daggerheart`, `#### A Change in Appearance` plus four orphan marginalia sentences, `#### Playing Other Were-Creatures`, and an art caption at line 1105.

Source: `resources/rules/chapters/hopeandfear-01-character-options.md` lines 948–1135 for transformations (the appendix contains **no** transformation content — this is the documented exception to the "parse from card pages" rule, because each transformation *is* its own full card page); `hopeandfear-05-appendix.md:213-240` for stances.

Validate with the repo's `validate.py` before handoff.

---

## 4. File Change Summary

### `core`
- 3 new migrations (§3.2)
- `TransformationCard.java`, `CharacterSheet.java`
- `CreateTransformationCardRequest`, `UpdateTransformationCardRequest`, `TransformationCardResponse`
- `UpdateCharacterSheetRequest`, `CharacterSheetResponse`
- `TransformationCardService`, `CharacterSheetService`
- `AdvancementType` (+ `UPGRADE_COMBO_DIE`), `LevelUpService`
- `.api-blueprint/` reference files
- **No** new controller endpoints; **no** new `QuestionInput` (reuse `QuestionService`)

### `dawn`
- `shared/models/transformation-card-api.model.ts`, `martial-stance-api.model.ts`
- `shared/services/` + `shared/mappers/` for both
- `codex-browse.service.ts`, `search.model.ts`, browse plumbing
- `character-sheet/components/transformation-section/`, `martial-stance-section/`
- `character-sheet/utils/` gating predicates
- `character-sheet.ts` / `.html` / panel CSS
- `gm-screen/content/transformations-stances.content.ts`, `panel-registry.ts`, `panel-registry.spec.ts`

### `hope_and_fear-import`
- `json/` + `upload/resolved/` payloads for 6 transformations and 16 stances

---

## 5. Testing Strategy

Per `core/.claude/rules/testing.md`: 80%+ coverage, `{ClassName}Test` + `{ClassName}IntegrationTest`, `@AutoConfigureMockMvc`, `@Transactional` rollback (no `deleteAll()`), bcrypt strength 4.

**Backend**
- Entity tests for new fields/relations.
- `TransformationCardServiceTest` — question find-or-create, inline + by-id, empty, duplicates.
- `CharacterSheetServiceTest` — Focus clamping at 0 and max; token clamp at 6; active-stance-must-be-known rejection; stance tier > character tier rejection; one-transformation invariant.
- `LevelUpServiceTest` — `UPGRADE_COMBO_DIE` allowed once per tier, rejected twice in the same tier, allowed again in the next tier; die steps exactly one size.
- Integration tests for the extended PUT.
- **Edge cases required:** null (resource not applicable to class), zero, boundary (0 and 6), and the "marked > max" case that is legitimate for other resources.

**Frontend (Vitest)**
- Gating predicate specs — absent classes, absent subclasses, case/whitespace variance, multiclass.
- Focus refresh roll — Instinct-many d6, result is the **highest** die not the sum, clamped to 6, and Instinct of 0 handled.
- Panel component specs — render, toggle, optimistic update, rollback on error.
- `panel-registry.spec.ts` updated `EXPECTED_IDS`.

**Payload validation**
- `validate.py` structural gate.
- Assert 6 transformation records, each with exactly 2 features and 6 questions.
- Assert 16 stance records, 4 per tier.

**Known blind spot:** `./mvnw verify` cannot catch a migration failure — tests run H2 with `spring.flyway.enabled=false`. The migrations must be exercised against real PostgreSQL before the PR is considered green.

---

## 6. Out of Scope (documented for the next packet)

Remaining new Hope & Fear resources, not built here:

| Resource | Owner | Shape |
|---|---|---|
| Hex count | Witch | Cap = Spellcast trait |
| Marked for Death | Assassin | Single-target flag, tier-scaled dice |
| Toxic Concoctions | Poisoners Guild | Card tokens, `1d4+1`, clear on long rest |
| Enchanted Talisman | Hedge Witch | Card tokens from Hope, clear on rest |
| Walk Between Worlds | Hedge Witch | Card tokens = Spellcast |
| Circle of Power | Hedge Witch | Card tokens = Spellcast |
| Lunar Phases die | Moon Witch | Rolled d6 per session |

The four Hedge/Moon Witch pools share one shape — *tokens attached to a specific card instance* — and would justify a generic `character_sheet_card_tokens` table when two or more are built. That table is deliberately **not** built now: the one in-scope case (Vampire Feed) is covered by a column, because the rules guarantee at most one transformation per PC.

Also out of scope: campaign frames that grant transformations by rule, and `evolves_into_adversary_id` back-links.
