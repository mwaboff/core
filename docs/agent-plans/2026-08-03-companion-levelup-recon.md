# Companion Level-Up Recon: `LevelUpService` As It Actually Works

## Date: 2026-08-03

Read-only recon for the companion feature's WP5 (level-up/level-down). Everything below is verified against the current code (`core/src/main/java/com/aboff/core/service/dh/LevelUpService.java` and its DTOs/entities), not against `core/docs/levelup-process.md`, which is confirmed stale and should not be consulted. No source files were modified to produce this note.

## 1. Current DTO field lists (verbatim)

**`LevelUpRequest`**: `advancements: List<AdvancementChoice>` (`@NotNull @Size(min=2)`), `newExperienceDescription: String`, `newDomainCardId: Long`, `equipNewDomainCard: Boolean` (default false), `unequipDomainCardId: Long`, `trades: List<DomainCardTradeRequest>`.

**`AdvancementChoice`**: `type: AdvancementType`, `traits: List<Trait>`, `experienceIds: List<Long>`, `domainCardId: Long`, `equipDomainCard: Boolean` (default false), `subclassCardId: Long`, `boostNewExperience: Boolean` (default false).

**`AvailableAdvancement`** (response, `com.aboff.core.model.dto.dh.response`): only `type: AdvancementType`, `remaining: int`, `mutuallyExclusiveWith: List<AdvancementType>`. No `description`, `limitPerTier`, or `usedInTier` — confirms the frontend contract-drift bug filed separately (dawn-cku).

## 2. `getLevelUpOptions`

Loads the sheet, computes `nextTier`, pulls `CharacterAdvancementLog` rows for `(characterSheetId, nextTier)` via `buildUsageMap`, then calls `buildAvailableAdvancements(sheet, nextTier, usageMap)` — one `AvailableAdvancement` per `AdvancementType`, `remaining = limit - used`, with `UPGRADE_SUBCLASS`/`MULTICLASS` mutual exclusion applied.

**Slot-in point:** after `buildAvailableAdvancements`, before building the response. Needs a new `companionTraining` list built from the sheet's active, `advancesOnLevelUp` companions.

**Open design question worth flagging to the implementer:** this endpoint runs *before* the player picks their two advancements for this level-up, so it cannot yet know whether `UPGRADE_SUBCLASS` will be chosen at `SPECIALIZATION`/`MASTERY` this level-up. `picksAvailable` returned here can only be the baseline (`1`); the "+1 Specialization / +2 Mastery" bonus (plan §3.3) is necessarily reactive on the frontend once the Advancements step is filled in, the same way `visibleTabs` already reacts to a chosen multiclass. The backend-side truth for *validation* (§3) can and must be advancement-choice-aware since it runs after the choices are submitted.

## 3. `validateLevelUpRequest`

Signals failure by **throwing on first violation** — `IllegalStateException` (business-rule failures) or `EntityNotFoundException` (missing referenced entity). Not an accumulated-errors list; fail-fast.

`buildUsageMap` scans `CharacterAdvancementLog` rows **for the current tier only** (`findByCharacterSheetIdAndTier`), parses each log's `advancementData.advancements[]`, and counts by `AdvancementType`. This is why the per-type limits in `getAdvancementLimitPerTier` are all "per tier."

**Companion caps are structurally different and must not reuse this mechanism**: `CompanionTrainingOption.maxSelections` (Intelligent 3, Vicious 3, Resilient 3, Aware 3, others 1) is a **per-companion lifetime** cap, not a per-tier cap, and tier boundaries don't reset it. The correct count source is `companion_trainings` rows for that `companion_id` + `option` — modeled by `Companion.trainings`, a `@OneToMany(mappedBy="companion", cascade=ALL, orphanRemoval=true)` collection, and the `CompanionTraining` entity. **Correction (verified via `git diff`/`git status` against `HEAD` on `feat/companions`): both `Companion.trainings` and the `CompanionTraining` entity are WP1's in-flight, uncommitted work, not pre-existing.** WP5 depends on WP1 having landed before this count source exists; do not build against it as though it's already merged.

## 4. `applyTierAchievements` — the Experience-grant pattern to mirror

Creates the character's own new Experience directly: `Experience.builder().characterSheet(sheet).createdBy(owner).description(...).modifier(2).build()`, saves it, adds it to `sheet.getExperiences()`, and records `tierAchievements.put("experienceCreatedId", savedExp.getId())` for reversal.

**Ownership is already companion-aware at the entity/service layer, pre-existing at `HEAD`** (confirmed via `git diff` — `Experience.java` has zero diff against `HEAD`, and `ExperienceService.java`'s diff touches only 8 lines in an unrelated `toResponse` field-rename, not `createExperience`): `Experience` has both `characterSheet` and `companion` `@ManyToOne` fields (mutually exclusive, matching the DB's `chk_experience_single_owner` CHECK). `ExperienceService.createExperience` already has a companion branch: `Experience.builder().characterSheet(null).companion(companion).createdBy(...).modifier(2).build()`. **WP5's automatic companion Experience grant must build its own `Experience` the same way** — `companion(companion)` set, `characterSheet` left `null` — inside `applyTierAchievements` (or a sibling helper called from it), recording a `companionExperienceCreatedId`-style entry per companion for reversal. It must not call `sheet.getExperiences().add(...)`; it should add to the companion's own `experiences` collection instead.

## 5. `advancementData` shape and `previousValues`

Serialized via `objectMapper.writeValueAsString` into a `LinkedHashMap` with top-level keys: `previousValues`, `tierAchievements` (only if tier transition), `advancements` (list, one entry per `AdvancementChoice` with `type` plus type-specific fields), `previousDamageThresholds`, `newDomainCard`, `unequipDomainCardId`, `trades`. Deserialized in `undoLevelUp` via `objectMapper.readValue(..., Map.class)` with unchecked casts throughout (`@SuppressWarnings("unchecked")`).

`snapshotPreviousValues` runs *before* any mutation and records `proficiency`, `evasion`, `hitPointMax`, `stressMax`, all six trait modifiers/marks, and — critically for `Intelligent` — `experienceModifiers`: a `Map<expId.toString(), modifier>` built only for experience IDs referenced by this request's `BOOST_EXPERIENCES` choices. **`Intelligent`'s direct `+1` write to `Experience.modifier` must add its target experience ID into this same `previousValues.experienceModifiers` map** so `reverseAdvancement`'s existing `BOOST_EXPERIENCES` restore logic pattern can be reused (or a companion-specific twin of it can read the same map shape).

## 6. `undoLevelUp` / `reverseAdvancement` control flow

`undoLevelUp` reads the top `CharacterAdvancementLog` by `toLevel` descending, checks `sheet.getLevel().equals(logEntry.getToLevel())` (guards against undoing out of order), deserializes, decrements level, restores damage thresholds, reverses trades, reverses Step-4 domain card add/unequip, then **loops `advancements` calling `reverseAdvancement` per entry**, then calls `reverseTierAchievements` once for the whole tier-transition block. At the very end: `characterSheetRepository.save(sheet)` then **`characterAdvancementLogRepository.delete(logEntry)`** — confirmed, it does delete its own log row (full undo, not a soft undo).

`reverseAdvancement` is a `switch` **statement** (not expression) over `AdvancementType` with no `default` case. Confirmed: Java does not require switch statements over enums to be exhaustive, so an `AdvancementType` value with no matching case is **silently skipped, not an error** — this exactly matches the plan's warning. Companion training/experience reversal is data that lives in new *top-level* `advancementData` keys (`companionTrainings`, `companionExperiences`, `companionCreated`), not inside a per-advancement `type`, so it does **not** belong inside `reverseAdvancement`'s switch at all — it should follow the same top-level pattern as `trades`/`tierAchievements`: a new `reverseCompanionChanges(sheet, data)`-style method called directly from `undoLevelUp`, mirroring how `reverseTierAchievements` is already called as its own step.

## 7. The orphanRemoval trap, applied to companion trainings

Confirmed via `CharacterSheet.java`: `characterSheetDomainCards` and `experiences` are `@OneToMany(mappedBy=..., cascade=CascadeType.ALL, orphanRemoval=true)`. The 2026-03-15 bug (still on disk) was calling `repository.delete()` directly on rows in such a collection — the delete happens, but since the parent's collection reference is untouched, the next `characterSheetRepository.save(sheet)` cascades and **re-inserts** the "deleted" row. The fix pattern, already applied for domain cards: mutate only via `sheet.getCharacterSheetDomainCards().removeIf(...)`, never `repository.delete()`, and let `orphanRemoval` do the DB delete on flush.

`Companion.trainings` is being added with the identical annotation: `@OneToMany(mappedBy="companion", cascade=CascadeType.ALL, orphanRemoval=true)`. **Correction: this field and the `CompanionTraining` entity are WP1's in-flight, uncommitted work (confirmed via `git diff`/`git status` — `Companion.java` shows a 158-line uncommitted diff adding it; `CompanionTraining.java` is untracked), not pre-existing prior art.** Once WP1 lands, **the rule for WP5 is identical and non-negotiable**: reversing a `Training` pick must be `companion.getTrainings().removeIf(t -> t.getId().equals(trainingId))`, never `companionTrainingRepository.delete(training)`. Note `subclassCards` is a different shape — `@ManyToMany` with a join table, not cascade-owned — so its existing `removeIf` precedent in `reverseAdvancement` works for a different reason (no orphanRemoval involved) and is not the pattern to copy for trainings.

## 8. The clamp pattern

`reverseAdvancement`'s `GAIN_HP`/`GAIN_STRESS` cases, immediately after decrementing the max: `sheet.setHitPointMarked(Math.min(sheet.getHitPointMarked(), sheet.getHitPointMax()))` / `sheet.setStressMarked(Math.min(sheet.getStressMarked(), sheet.getStressMax()))`. Same method, same file. For companions: after any reversal that shrinks derived `stressMax` (deleting a `RESILIENT` training), `companion.stressMarked` must be clamped the same way against the newly-derived max — `Math.min(companion.getStressMarked(), derivedStressMax)`.

## 9. `knownMartialStanceIds` — independently confirmed absent from `LevelUpService`

Grepped the whole `src/main` tree: `knownMartialStanceIds` appears only in `UpdateCharacterSheetRequest.java`, `CharacterSheetResponse.java`, and `CharacterSheetService.java` — **never** in `LevelUpService.java`, `LevelUpRequest.java`, or `AdvancementChoice.java`. It is written via a separate `PUT` on the character sheet after level-up completes client-side, entirely outside the advancement-log/undo machinery. This independently confirms the plan's claim: level-down has no code path that touches it, so it is never reverted. WP5 must not repeat this — companion training/experience data must travel inside `LevelUpRequest` and `advancementData`, per §3.8, exactly as reasoned above in §5/§6.
