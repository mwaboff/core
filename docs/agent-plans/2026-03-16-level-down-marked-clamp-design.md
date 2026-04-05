# Fix Level-Down and General Marked Value Constraint Violations

## Context
When undoing a level-up that included `GAIN_HP` or `GAIN_STRESS`, the `reverseAdvancement` method decrements the max value but doesn't clamp the corresponding marked value. If `hitPointMarked == hitPointMax` (or stress), the DB check constraint `check_hit_point_marked_lte_max` is violated.

Additionally, the general character sheet create/update path (`CharacterSheetService.validateConstraints`) threw errors when marked > max instead of auto-clamping, which could cause issues when armor max changes (e.g., unequipping armor).

## Approach
1. In `LevelUpService.reverseAdvancement()`, after decrementing `hitPointMax` or `stressMax`, clamp the marked value to not exceed the new max using `Math.min`.
2. In `CharacterSheetService.validateConstraints()`, replace `IllegalStateException` throws with `Math.min` clamping for all marked-vs-max pairs (armor, HP, stress, hope).

## File Changes

| File | Change |
|------|--------|
| `LevelUpService.java` | After decrementing HP/stress max in `reverseAdvancement`, clamp marked to `Math.min(marked, newMax)` |
| `CharacterSheetService.java` | `validateConstraints` now clamps marked values instead of throwing exceptions |
| `LevelUpServiceTest.java` | Add 2 test cases: undo GAIN_HP and GAIN_STRESS when marked == max |
| `CharacterSheetServiceTest.java` | Update 5 tests from expecting exceptions to verifying clamping |
| `CharacterSheetControllerIntegrationTest.java` | Update 2 integration tests from expecting 400 to verifying clamped values |

## Testing Strategy
- Test undo GAIN_HP when `hitPointMarked == hitPointMax` — verify marked is clamped to new max
- Test undo GAIN_STRESS when `stressMarked == stressMax` — verify marked is clamped to new max
- Create/update tests verify marked values are clamped to max instead of throwing
- Integration tests verify HTTP success with clamped response values
- All 1504 tests passing
