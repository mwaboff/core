# Companions WP5: Level-Up / Level-Down — Design

**Date:** 2026-08-04
**bd issue:** core-vyu (P1)
**Depends on:** WP1+WP2+WP3 (`9ecc933`, committed)
**Source of truth:** `dawn/.agents/plans/companions/companions-implementation-plan.md` §3.1, §3.2, §3.3, §3.8, §3.9, §5.4, §7, §10; `core/docs/agent-plans/2026-08-03-companion-levelup-recon.md` (verified against code, corrects the plan where they conflict).

## 1. Context

`LevelUpService` (1309 lines) already has a full advancement/tier-transition/undo pipeline for character sheets. This package extends it so a companion also participates: it gains Training picks and an automatic Experience on tier transitions, a companion can be created via multiclassing into Beastbound mid-campaign, and all of it must be **fully reversible** via the existing `advancementData` JSON + `undoLevelUp` mechanism — the trap this package exists to avoid is `knownMartialStanceIds`, which lives outside that mechanism and is silently never reverted.

## 2. New/changed DTOs

**`LevelUpRequest`** gains:
```java
List<CompanionTrainingChoice> companionTrainings;   // {companionId, option, viciousAxis?, targetExperienceId?}
List<CompanionExperienceGrant> companionExperiences; // {companionId, description} — tier transitions only, silently ignored otherwise (matches existing newExperienceDescription convention)
Long newCompanionId;                                 // see §5
```

**New `CompanionTrainingChoice`** (request): `companionId: Long`, `option: CompanionTrainingOption`, `viciousAxis: ViciousAxis`, `targetExperienceId: Long`.

**New `CompanionExperienceGrant`** (request): `companionId: Long`, `description: String`.

**`LevelUpOptionsResponse`** gains:
```java
List<CompanionLevelUpOptionsResponse> companionTraining;  // one per eligible companion
List<CompanionResponse> restorableCompanions;              // soft-deleted, origin=SUBCLASS_FEATURE, this sheet
```

**New `CompanionLevelUpOptionsResponse`**: `companionId`, `name`, `currentStats: CompanionResponse` (reuses `CompanionService.toResponse` — no parallel stats mapping), `availableOptions: List<AvailableCompanionTrainingOption>`, `picksAvailable: int`.

**New `AvailableCompanionTrainingOption`**: `option: CompanionTrainingOption`, `remaining: int` (from `CompanionDerivationService.remainingByOption`).

`restorableCompanions` reuses `CompanionResponse` directly (via `CompanionService.toResponse`) rather than a bespoke DTO — it's a valid `Companion` entity either way, and the mapper doesn't care whether `deletedAt` is set.

## 3. `getLevelUpOptions` — additions

- `companionTraining`: one entry per **eligible** companion (§4), with `picksAvailable` hardcoded to the baseline `1`. Per the recon (verified against code): this endpoint runs *before* the player picks their two advancements, so it cannot know whether a Beastbound SPECIALIZATION/MASTERY card will be taken this level-up. The `+1`/`+2` bonus is necessarily reactive on the frontend once the Advancements step is filled in (the same pattern already used for `visibleTabs`). **`validateLevelUpRequest` is the only authoritative source for the real count.**
- `restorableCompanions`: every soft-deleted companion on this sheet with `origin == SUBCLASS_FEATURE`, unmapped to any specific advancement — the backend doesn't enumerate "available foundation cards for multiclass" anywhere today (that's client-driven catalog browsing), so returning the flat candidate list and letting the frontend match it against whatever subclass card the player picks is consistent with the existing `picksAvailable` reactivity pattern.

## 4. Eligibility

```java
private List<Companion> getEligibleCompanions(CharacterSheet sheet) {
    return companionRepository.findActiveByCharacterSheetId(sheet.getId()).stream()
            .filter(Companion::getAdvancesOnLevelUp)
            .toList();
}
```

Called **once, at the very start** of both `getLevelUpOptions` and `levelUp` (before any mutation) — this single snapshot is what gives §3.1 "no Training pick / no Experience grant on the level-up that created it" for free: a companion created or restored later in the same `levelUp()` call is never in this list.

## 5. `newCompanionId` — unifying "create new" and "restore"

The plan's two-phase submit pattern means the companion row already exists by the time `levelUp()` runs (the frontend calls the ordinary `POST /api/dh/companions` first, or — for restore — nothing new needs to exist since the row is already there, just soft-deleted). Rather than add a second DTO field or a dedicated restore endpoint, `newCompanionId` is handled uniformly:

1. Find the `MULTICLASS` choice in `request.getAdvancements()` (if any) whose target `SubclassCard` carries a feature named `"Companion"` (case/whitespace-insensitive) with `featureType == SUBCLASS` — call it the **granting card**. Detection is by feature name+type per the team's instruction, not hardcoded ids (Beastbound is ids 19/20/21 in prod today, but homebrew must work identically).
2. If `newCompanionId` is set but no granting card is found this request: `IllegalStateException`.
3. If a granting card is found, load the companion by `newCompanionId`, scoped to this sheet:
   - **Fresh case:** active, `origin == MANUAL` → promote: set `origin = SUBCLASS_FEATURE`, `originSubclassCardId = <granting card id>`.
   - **Restore case:** soft-deleted, `origin == SUBCLASS_FEATURE`, `originSubclassCardId` already equals the granting card's id → `companion.restore()`.
   - Anything else (wrong sheet, active MANUAL... wrong card id, etc.) → `IllegalStateException`.
4. Either way, log `advancementData.companionCreated = {companionId, originSubclassCardId}`. Reversal is identical for both cases: soft-delete the companion again. This is deliberate — "undo this level-up" should undo "this level-up is what brought the companion into active, subclass-granted existence," regardless of whether the row itself is old or new.

This runs as its own step (**Step 2.5**, in `levelUp`) right after advancements are applied (so the new subclass card is already on `sheet.getSubclassCards()`) and before companion Training is applied.

## 6. Picks-available formula (validation-time, authoritative)

```java
picksAvailable = 1
  + 1 * (any UPGRADE_SUBCLASS choice this request targets a card with a feature named "Expert Training")
  + 2 * (any UPGRADE_SUBCLASS choice this request targets a card with a feature named "Advanced Training")
```

Only `UPGRADE_SUBCLASS` is scanned — `MULTICLASS` only ever grants a FOUNDATION card (`validateMulticlass` enforces this), and Expert/Advanced Training are Specialization/Mastery features, so they can never appear on a multiclass grant. Applied identically to every eligible companion (the plan's formula is stated per-companion; a two-companion character taking Expert Training gets the bonus on both, same as every other per-companion multiplier this feature already accepts and mitigates via `advancesOnLevelUp`).

## 7. Validation (`validateLevelUpRequest`)

New checks, run after the existing ones:

1. **Per-eligible-companion pick count.** Group `request.getCompanionTrainings()` by `companionId`. For every eligible companion, the count of choices targeting it must equal `picksAvailable` exactly (§6) — same "exactly N" strictness as the top-level 2-advancement rule. A choice targeting a non-eligible/unknown companion id is rejected.
2. **Per-pick legality, reusing `CompanionTrainingValidator.validatePick`.** Since a companion can receive multiple picks in one request, and the same option's cap must account for earlier picks in *this* request, validation builds a disposable **shadow copy** per companion — `Companion.builder().baseDamageDice(...).baseAttackRange(...).trainings(new HashSet<>(real.getTrainings())).experiences(real.getExperiences()).build()` — and calls `validatePick(shadow, ...)` once per choice, adding a throwaway `CompanionTraining` to the shadow's set after each successful check. **The real managed entity is never touched during validation** — this preserves the codebase's existing validate-then-apply separation (everything else in `levelUp` mutates only after `validateLevelUpRequest` returns cleanly), so a mid-loop failure can't leave a stray in-memory mutation that would matter if a later, unrelated check also failed. This is the one and only place `validatePick`'s cap logic is implemented — no parallel copy.
3. **Companion Experience grants**, tier transitions only (silently ignored on non-tier-transition level-ups, matching the existing `newExperienceDescription` convention of only being consulted `if (isTierTransition)`): exactly one grant per eligible companion, non-blank `description`, and `companion.getExperiences().size() < 5` (the printed cap, plan §2.5/§10.1 — no existing constant for this anywhere in the codebase, so a new `MAX_COMPANION_EXPERIENCES = 5` is added next to the class's other constants).
4. **`newCompanionId`** — §5's checks.

## 8. Apply order in `levelUp`

```
Step 1  Tier Achievements        (existing char Experience+proficiency, PLUS new: one Experience per
                                   eligible companion — added to companion.getExperiences(), NOT
                                   sheet.getExperiences(), companion(...) set / characterSheet(null)
                                   per the chk_experience_single_owner constraint, mirroring
                                   ExperienceService.createExperience's existing companion branch)
Step 2  Advancements              (existing, unchanged)
Step 2.5 Companion create/restore (§5, new — only if newCompanionId present)
Step 2.6 Companion Training       (new — applies request.getCompanionTrainings() against the ELIGIBLE
                                   set from §4; INTELLIGENT writes +1 directly to the target
                                   Experience.modifier, recording the previous value into
                                   previousValues.companionExperienceModifiers exactly like the
                                   existing char-level experienceModifiers map)
Step 3  Damage Thresholds         (existing, unchanged)
Step 4  Domain Card / Trades      (existing, unchanged)
```

Companion Experience grants intentionally apply *before* Training, mirroring the char-level order (tier Experience created before advancements can reference it) — but unlike the char-level `boostNewExperience` escape hatch, **INTELLIGENT cannot target a companion's own brand-new tier Experience in the same level-up**: that would need a second derived-id plumbing path (`newTierExperienceId`-style) that nothing in the plan asks for. A player who wants that combination picks INTELLIGENT the following level-up instead. Flagged as a deliberate, minimal scope call.

`advancementData` (new top-level keys, exact shapes per plan §5.4, verified against the recon's `previousValues.companionExperienceModifiers` example):
```json
{
  "companionTrainings":  [{"companionId":7,"trainingId":31,"option":"AWARE"}],
  "companionExperiences":[{"companionId":7,"experienceId":88}],
  "companionCreated":    {"companionId":9,"originSubclassCardId":204},
  "previousValues": { "companionExperienceModifiers": {"55": 2} }
}
```
`companionTrainings` entries deliberately carry no `targetExperienceId` — reversal restores every key in `previousValues.companionExperienceModifiers` unconditionally (mirrors the map being the single source of truth, avoids a second lookup path).

## 9. Reversal — its own top-level step, not inside `reverseAdvancement`

Per the recon's explicit recommendation (and the team's instruction): `reverseAdvancement` is a `switch` **statement** with no `default` — an unhandled `AdvancementType` silently no-ops, which is exactly the failure shape to avoid. Companion state isn't keyed by `AdvancementType` anyway (it's new top-level `advancementData` keys, like `trades`/`tierAchievements`), so it gets its own method called directly from `undoLevelUp`, right after the existing `reverseTierAchievements` call:

```java
private void reverseCompanionChanges(CharacterSheet sheet, Map<String, Object> data, Map<String, Object> previousValues)
```

Order of operations, each tolerant of a missing companion/row (per item 11 — `findById` returning empty, or `removeIf` matching nothing, is a no-op, never a throw):
1. Delete `companionTrainings` rows: for each `{companionId, trainingId}`, find the companion (skip if gone), `companion.getTrainings().removeIf(t -> t.getId().equals(trainingId))` — **never** `companionTrainingRepository.delete(...)`.
2. Delete `companionExperiences`-granted Experiences: for each `{companionId, experienceId}`, find the companion (skip if gone), `companion.getExperiences().removeIf(e -> e.getId().equals(experienceId))`.
3. Restore `previousValues.companionExperienceModifiers`: for each `expId -> previousModifier`, `experienceRepository.findById(...).ifPresent(exp -> { exp.setModifier(previousModifier); experienceRepository.save(exp); })` (exact mirror of the existing char-level `BOOST_EXPERIENCES` reversal).
4. Soft-delete `companionCreated.companionId` if present (skip if already gone).
5. **Clamp** every touched companion's `stressMarked` to `Math.min(stressMarked, CompanionDerivationService.stressMax(companion))` after training-row deletion (a deleted `RESILIENT` pick shrinks the derived max) — same `Math.min` precedent as `GAIN_HP`/`GAIN_STRESS` reversal.

All mutated companions are saved once at the end of the method (`companionRepository.save(...)` per touched companion) — `undoLevelUp` already saves `sheet` separately; companions are a different aggregate root and need their own save call (existing `CompanionService` methods always end with an explicit `companionRepository.save(...)`, this follows the same pattern).

## 10. Feature-name detection helpers (new, small, static-style private methods)

```java
private boolean hasFeatureNamed(SubclassCard card, String featureName)  // trim+equalsIgnoreCase, featureType == SUBCLASS
```
Reused for `"Companion"`, `"Expert Training"`, `"Advanced Training"` — mirrors the existing `hasComboStrikeFeature` pattern (detect by name, not id, so homebrew and multiclass both work).

## 11. Injected dependencies added to `LevelUpService`

`CompanionRepository`, `CompanionService` (for `toResponse` reuse in the options DTOs — avoids a duplicate mapping). `CompanionTraining`/`Companion`/`CompanionTrainingOption`/`ViciousAxis`/`CompanionOrigin` are already reachable via the existing `com.aboff.core.model.entity.dh.*` / `com.aboff.core.model.enums.*`-style imports in this file. `CompanionTrainingValidator` and `CompanionDerivationService` are called as static utilities from the same package, no injection needed.

## 12. Testing strategy

- **The priority deliverable:** a round-trip `levelUp` → `undoLevelUp` unit test asserting the companion is restored exactly (base stats unchanged, derived evasion/stressMax/damageDice/attackRange back to pre-level-up values, Experience list and modifiers restored, training rows gone).
- Eligibility: companion created this level-up gets no picks/grant; companion with `advancesOnLevelUp=false` gets none; soft-deleted companion is invisible to both `getLevelUpOptions` and validation.
- Picks-available: baseline 1; +1 with Expert Training; +2 with Advanced Training; multi-pick-same-request cap enforcement (two `LIGHT_IN_THE_DARK` picks in one request rejected); `VICIOUS` axis-at-cap rejection; `INTELLIGENT` missing/foreign experience rejection.
- Tier-transition Experience grant: created with `companion(...)`/`characterSheet(null)`, 5-cap enforcement, silently ignored on a non-tier-transition level-up.
- `newCompanionId`: fresh-companion promotion, restore-companion path, missing granting card rejection, wrong-sheet/wrong-card rejection.
- Reversal: each of the above, plus the hard-deleted-companion-after-level-up no-op case, plus `RESILIENT` removal clamping `stressMarked`.

**Gate:** `./mvnw test` and `./mvnw verify` both green, 80%+ on new/modified logic. Do not commit (team lead reviews/commits). Close `core-vyu` on completion.
