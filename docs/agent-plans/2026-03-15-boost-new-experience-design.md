# Boost New Tier Experience in Same Level-Up Request

**Date:** 2026-03-15
**Context:** During a tier transition level-up, a new experience is created with +2 modifier. Users want to immediately boost that experience (to +3) using a BOOST_EXPERIENCES advancement in the same request. Currently impossible because the experience doesn't exist at validation time.

## Approach

Add a `boostNewExperience` boolean field to `AdvancementChoice`. When `true`, the BOOST_EXPERIENCES advancement only requires 1 experience ID (the other existing one). The newly created tier experience is automatically included as the second target.

Pass the new experience ID from tier achievements to the advancement application step via parameter.

## File Changes

### 1. `AdvancementChoice.java` (DTO)
- Add `private Boolean boostNewExperience` field with `@Builder.Default` defaulting to `false`
- Add javadoc explaining the field

### 2. `LevelUpService.java` (Service)

**`validateBoostExperiences()`:**
- Add `boolean isTierTransition` parameter
- If `boostNewExperience == true`:
  - Validate `isTierTransition` is true (throw otherwise)
  - Require exactly 1 experience ID (not 2)
  - That 1 ID must belong to the character
- If `boostNewExperience == false` (default): unchanged behavior (2 IDs required)

**`validateLevelUpRequest()`:**
- Pass `isTierTransition` to `validateBoostExperiences()`

**`performLevelUp()`:**
- After `applyTierAchievements()`, extract `newTierExpId` from `tierAchievements.get("experienceCreatedId")`
- Pass `newTierExpId` (nullable) to `applyAdvancement()`

**`applyAdvancement()`:**
- Add `Long newTierExperienceId` parameter
- In `BOOST_EXPERIENCES` case: if `boostNewExperience == true`, combine `choice.getExperienceIds()` (1 ID) with `newTierExperienceId` to form the full 2-ID list
- Store all boosted experience IDs in advancement data for undo

**`snapshotPreviousValues()`:**
- No change needed here. The new experience modifier snapshot will be captured inline during `applyAdvancement` when `boostNewExperience` is true, since the experience exists by that point (tier achievements run first). Add the new experience's pre-boost modifier (2) to the `experienceModifiers` map in the advancement data.

**`reverseAdvancement()`:**
- No changes needed. The undo path already restores from `previousValues.experienceModifiers` using the stored experience IDs. The new experience ID will be included in the stored data naturally.

### 3. `character-sheets-api.md` (API Blueprint)
- Add `boostNewExperience` to AdvancementChoice field table
- Update BOOST_EXPERIENCES row in field usage table
- Add example showing tier transition with boostNewExperience=true

### 4. Tests

**Unit Tests (`LevelUpServiceTest.java`):**

| Test | Description |
|------|-------------|
| `boostExperiencesWithNewTierExperience_success` | Tier transition + BOOST_EXPERIENCES with boostNewExperience=true, verify both experiences get +1 |
| `boostNewExperience_requiresTierTransition` | Non-tier-transition with boostNewExperience=true should throw |
| `boostNewExperience_requiresExactlyOneExperienceId` | boostNewExperience=true with 0 or 2 experience IDs should throw |
| `boostNewExperience_defaultFalse_existingBehavior` | Default false preserves 2-ID requirement |
| `undoLevelUp_withBoostedNewExperience` | Undo reverses tier experience creation and the boost |

**Integration Test (`LevelUpControllerIntegrationTest.java`):**

| Test | Description |
|------|-------------|
| `levelUp_tierTransition_boostNewExperience_success` | Full end-to-end tier transition with boostNewExperience |

## Testing Strategy

- Existing BOOST_EXPERIENCES tests must continue passing (backward compatibility)
- New tests cover the boostNewExperience=true path including validation, application, and undo
- Edge case: boostNewExperience=true on non-tier-transition should produce clear error
