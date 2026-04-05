# Duplicate Advancement Selection in Level-Up

## Context

During a level-up, players choose 2 advancements. Currently the system treats each choice independently, but players should be allowed to select the **same advancement type twice** (e.g., two GAIN_HP) subject to per-tier limits. This also requires cross-validation for types where duplicates could cause data corruption (BOOST_TRAITS trait overlap, MULTICLASS class overlap).

Additionally, per-tier limits for UPGRADE_SUBCLASS and MULTICLASS need updating based on game rule clarifications.

## Approach

The existing per-tier limit check (`used + requestedCount > limit`) already handles duplicate types correctly for most cases. The main changes are:

1. Update per-tier limits for UPGRADE_SUBCLASS (3 → 1) and MULTICLASS (3 → 2)
2. Add cross-validation in `validateLevelUpRequest` for BOOST_TRAITS and MULTICLASS when selected twice
3. Simplify the mutual exclusion remaining calculation in `buildAvailableAdvancements`

## File Changes

### `src/main/java/com/aboff/core/service/dh/LevelUpService.java`

#### `getAdvancementLimitPerTier` — Update limits

| Type | Old Limit | New Limit |
|------|-----------|-----------|
| UPGRADE_SUBCLASS | 3 | 1 |
| MULTICLASS | 3 | 2 |

All other limits unchanged.

#### `validateLevelUpRequest` — Add cross-validation block

After the existing per-choice validation loop (after line 658), add a block that handles same-type-twice edge cases:

- **BOOST_TRAITS x2**: Collect all traits across both choices into a set. If set size < total trait count, reject with error: traits must be distinct across both BOOST_TRAITS choices.
- **MULTICLASS x2**: Collect target class IDs from both choices' subclass cards. If same class targeted twice, reject with error.

#### `buildAvailableAdvancements` — Simplify mutual exclusion

The current logic uses a "combined limit of 3" formula. Update to use the new individual limits:
- UPGRADE_SUBCLASS: limit 1, remaining = 0 if multiclass used > 0, else `max(0, 1 - upgradeUsed)`
- MULTICLASS: limit 2, remaining = 0 if upgrade_subclass used > 0, else `max(0, 2 - multiclassUsed)`

### `docs/levelup-process.md`

Update the advancement limits table:

| Advancement | Tier 2 | Tier 3 | Tier 4 |
|-------------|--------|--------|--------|
| UPGRADE_SUBCLASS | -- | 1 | 1 |
| MULTICLASS | -- | 2 | 2 |

Update the mutual exclusion section to reflect the new limits and clarify that both types in the same request is rejected.

### `src/test/java/com/aboff/core/service/dh/LevelUpServiceTest.java`

New unit tests:

| Test | Expects |
|------|---------|
| `levelUp_duplicateGainHp_succeeds` | Two GAIN_HP in one level-up applies +2 HP |
| `levelUp_duplicateGainStress_succeeds` | Two GAIN_STRESS applies +2 stress |
| `levelUp_duplicateBoostTraits_withDistinctTraits_succeeds` | 4 different traits all boosted and marked |
| `levelUp_duplicateBoostTraits_withOverlappingTraits_fails` | Rejected — same trait in both choices |
| `levelUp_duplicateBoostProficiency_succeeds` | Two BOOST_PROFICIENCY applies +2 |
| `levelUp_duplicateMulticlass_differentClasses_succeeds` | Two different classes added |
| `levelUp_duplicateMulticlass_sameClass_fails` | Rejected — same class targeted |
| `levelUp_duplicateUpgradeSubclass_fails` | Rejected — limit is 1/tier |
| `levelUp_duplicateBoostEvasion_fails` | Rejected — limit is 1/tier |
| `levelUp_duplicateBoostExperiences_fails` | Rejected — limit is 1/tier |
| `levelUp_duplicateGainDomainCard_fails` | Rejected — limit is 1/tier |

## What Needs No Changes

- **Level-down/undo**: Each advancement is stored and reversed individually in the advancement log — doubling works automatically.
- **`applyAdvancement`**: Already processes each choice sequentially; a second GAIN_HP just adds another +1.
- **Snapshot/restore**: Already captures all trait state, HP, stress, proficiency, etc.
- **Mutual exclusion in same request**: Already handled at lines 621-626 (UPGRADE_SUBCLASS + MULTICLASS in same request rejected).

## Testing Strategy

- All new tests are unit tests in `LevelUpServiceTest`
- Cover both success (duplicate allowed) and failure (duplicate rejected) paths
- Verify the per-tier limit changes by testing that formerly-allowed counts now fail
- Run full test suite to ensure no regressions
