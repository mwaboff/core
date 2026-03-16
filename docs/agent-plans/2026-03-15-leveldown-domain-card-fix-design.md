# Level-Down Domain Card Bug Fix

## Date: 2026-03-15

## Context

When a user levels up and gains a domain card (via Step 2 GAIN_DOMAIN_CARD advancement or Step 4 newDomainCard), then later undoes the level-up, the domain card remains on the character sheet.

### Root Cause

`CharacterSheet.characterSheetDomainCards` has `CascadeType.ALL` + `orphanRemoval = true`. During `undoLevelUp`, domain cards are deleted via `characterSheetDomainCardRepository.delete()` but never removed from the entity's collection. When `characterSheetRepository.save(sheet)` executes at the end, the cascade re-persists the deleted entities.

Contrast with subclass cards, which work correctly: `sheet.getSubclassCards().removeIf(...)` operates on the collection directly.

## Approach

**Collection-only removal** — Remove domain cards from `sheet.getCharacterSheetDomainCards()` and let `orphanRemoval` handle the DB delete. This is consistent with the existing subclass card undo pattern.

## File Changes

### `src/main/java/com/aboff/core/service/dh/LevelUpService.java`

**3 deletion sites in `undoLevelUp` + 1 in `reverseAdvancement`:**

1. **Reverse trades — remove traded-in cards** (lines 298-303):
   - Replace `characterSheetDomainCardRepository.findBy...delete()` with `sheet.getCharacterSheetDomainCards().removeIf(csdc -> csdc.getDomainCard().getId().equals(inId.longValue()))`

2. **Reverse trades — re-add traded-out cards** (lines 311-319):
   - After building the `CharacterSheetDomainCard`, add it to `sheet.getCharacterSheetDomainCards()` instead of saving via repository. Cascade will persist it.

3. **Remove Step 4 domain card** (lines 327-332):
   - Replace repository delete with `sheet.getCharacterSheetDomainCards().removeIf(csdc -> csdc.getDomainCard().getId().equals(domainCardId))`

4. **Reverse GAIN_DOMAIN_CARD advancement** (lines 1075-1082):
   - Replace repository delete with collection removal
   - Need to pass `sheet` into `reverseAdvancement` — it's already passed (line 350)

**Re-equip unequipped card** (lines 334-343):
   - This modifies an existing card (sets equipped=true). Since the entity is managed, finding via the collection and modifying it directly is preferred. Update to find in `sheet.getCharacterSheetDomainCards()` and set equipped.

## Testing Strategy

- Update existing unit tests for `undoLevelUp` to verify domain cards are removed from the collection
- Verify existing integration tests pass (they test the full undo flow)
- Add/verify a test case: level up with GAIN_DOMAIN_CARD → undo → assert domain card is gone
