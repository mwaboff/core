# Allow Re-Marking Traits During Tier Upgrades

**Date:** 2026-03-15
**Status:** Approved

## Context

When a player levels up with BOOST_TRAITS, `validateBoostTraits` rejects any trait that is already marked. However, at levels 5 and 8 (entering Tier 3 and Tier 4), `applyTierAchievements` clears all trait marks *after* validation runs. This means players cannot re-select a previously boosted trait during these tier transitions, even though the marks will be cleared.

**Current order of operations in `levelUp()`:**
1. `validateLevelUpRequest` → calls `validateBoostTraits` → rejects marked traits
2. `applyTierAchievements` → clears all trait marks at levels 5 and 8

The fix: make validation aware that marks will be cleared, so it allows marked traits at levels 5 and 8.

## Scope

- **Levels 5 and 8 only** — these are the only tier transitions where marks are cleared.
- Level 2 is a tier transition but does NOT clear marks, so marked traits remain rejected there.

## Approach

Pass `nextLevel` into the validation chain so `validateBoostTraits` can skip the "already marked" check when marks will be cleared by tier achievements.

## File Changes

### `src/main/java/com/aboff/core/service/dh/LevelUpService.java`

1. **`validateLevelUpRequest`** — add `int nextLevel` parameter.
2. **`validateBoostTraits`** — add `int nextLevel` parameter. Skip marked check when `nextLevel == 5 || nextLevel == 8`.
3. **`levelUp` call site** — pass `nextLevel` to `validateLevelUpRequest`.

### `src/test/java/com/aboff/core/service/dh/LevelUpServiceTest.java`

- Add test: marked traits allowed during level 5 tier upgrade
- Add test: marked traits allowed during level 8 tier upgrade
- Add test: marked traits still rejected at non-clearing levels (e.g., level 3)
- Add test: marked traits still rejected at level 2 tier transition

### `src/test/java/com/aboff/core/controller/dh/LevelUpControllerIntegrationTest.java`

- Add integration test: BOOST_TRAITS with previously marked traits succeeds at level 5 tier transition

## Testing Strategy

- Unit tests verify validation logic directly
- Integration test verifies end-to-end level-up with marked trait re-selection
- Existing tests must continue to pass (regression)
