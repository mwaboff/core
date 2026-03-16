# Equipped/Vault Domain Cards Update Support

**Date:** 2026-03-15
**Context:** Frontend sends `equippedDomainCardIds` and `vaultDomainCardIds` in character sheet update requests, but the backend only has `domainCardIds` and marks all as equipped.

## Approach

Replace `domainCardIds` in `UpdateCharacterSheetRequest` with `equippedDomainCardIds` and `vaultDomainCardIds`. When either is provided, both must be present. The service sets the `equipped` flag on `CharacterSheetDomainCard` accordingly.

`CreateCharacterSheetRequest.domainCardIds` is left unchanged (all equipped by default at creation).

## File Changes

### 1. UpdateCharacterSheetRequest.java
- Remove `domainCardIds` field
- Add `equippedDomainCardIds` (List<Long>) — cards with equipped=true
- Add `vaultDomainCardIds` (List<Long>) — cards with equipped=false

### 2. CharacterSheetService.java — updateCharacterSheet()
- Replace `domainCardIds` handling block (lines 453-465) with:
  - Validate: if either list is non-null, both must be non-null (throw IllegalArgumentException otherwise)
  - Clear existing `characterSheetDomainCards`
  - Add equipped cards with `equipped = true`
  - Add vault cards with `equipped = false`
  - Validate no duplicate IDs across both lists

### 3. Tests
- **CharacterSheetServiceTest.java**: Update 3 test usages from `.domainCardIds()` to `.equippedDomainCardIds()`
- **CharacterSheetControllerIntegrationTest.java**: Update existing tests and add test verifying equipped vs vault distinction in response

### 4. API Blueprint
- Update request schema in `.api-blueprint/references/character-sheets-api.md`

## Testing Strategy
- Unit test: equipped and vault cards set correctly
- Unit test: validation error when only one list provided
- Unit test: duplicate card ID across lists rejected
- Integration test: full round-trip with equipped/vault split in response