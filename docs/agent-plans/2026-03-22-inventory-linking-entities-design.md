# Inventory Linking Entities Design

**Date:** 2026-03-22
**Status:** Approved

## Context

Character sheet inventory currently uses `@ManyToMany` join tables with composite primary keys:
- `character_sheet_inventory_weapons` — PK `(character_sheet_id, weapon_id)`
- `character_sheet_inventory_armors` — PK `(character_sheet_id, armor_id)`
- `character_sheet_inventory_items` — PK `(character_sheet_id, loot_id)`

Active equipment uses direct FK columns on `character_sheets`:
- `active_primary_weapon_id`, `active_secondary_weapon_id`, `active_armor_id`

### Problems
1. **No duplicate items** — composite PK prevents a character from having two of the same weapon/armor/loot
2. **Single armor only** — only one `active_armor_id` column; can't equip multiple armor pieces (amulets, etc.)
3. **Can't unequip via update API** — `null` in the update request means "don't change", not "set to null", so there's no way to unequip weapons/armor through the existing PUT endpoint
4. **No per-instance state** — items can't carry character-specific state (equipped status, slot assignment)

### Approach

Convert the three inventory join tables into proper linking entities (same pattern as `CharacterSheetDomainCard`). Move equipped status and weapon slot tracking into the linking entities. Remove the `active_*` FK columns from `character_sheets`. Use full-replacement semantics through the existing character sheet update API (consistent with domain cards).

## Deliverables

| # | Deliverable | Files |
|---|-------------|-------|
| 1 | DB Migration | `V{timestamp}__convert_inventory_to_linking_entities.sql` |
| 2 | Linking entities | `CharacterSheetWeapon.java`, `CharacterSheetArmor.java`, `CharacterSheetLoot.java` |
| 3 | Repositories | `CharacterSheetWeaponRepository.java`, `CharacterSheetArmorRepository.java`, `CharacterSheetLootRepository.java` |
| 4 | Request DTOs | `InventoryWeaponRequest.java`, `InventoryArmorRequest.java`, `InventoryLootRequest.java` + update `CreateCharacterSheetRequest`, `UpdateCharacterSheetRequest` |
| 5 | Response DTOs | `InventoryWeaponResponse.java`, `InventoryArmorResponse.java`, `InventoryLootResponse.java` + update `CharacterSheetResponse` |
| 6 | CharacterSheet entity | Remove old fields, add new `@OneToMany` sets |
| 7 | CharacterSheetService | Update create/update/toResponse + add slot validation |
| 8 | LevelUpService | Update if it references active equipment fields |
| 9 | Unit tests | Update `CharacterSheetServiceTest` |
| 10 | Integration tests | Update `CharacterSheetControllerIntegrationTest` |
| 11 | API Blueprint | Update `.api-blueprint/references/character-sheets-api.md` |
| 12 | Frontend transition guide | `docs/agent-plans/2026-03-22-inventory-frontend-transition-guide.md` |

---

## 1. Database Migration

Single migration file: `V{timestamp}__convert_inventory_to_linking_entities.sql`

Follows the established pattern from `V20260314175439350__convert_domain_cards_to_entity.sql`.

### Weapons table conversion

```sql
-- Convert character_sheet_inventory_weapons to a linking entity table
ALTER TABLE character_sheet_inventory_weapons DROP CONSTRAINT character_sheet_inventory_weapons_pkey;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN slot VARCHAR(20);
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

### Armor table conversion

```sql
-- Convert character_sheet_inventory_armors to a linking entity table
ALTER TABLE character_sheet_inventory_armors DROP CONSTRAINT character_sheet_inventory_armors_pkey;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

### Loot table conversion

```sql
-- Convert character_sheet_inventory_items to a linking entity table
ALTER TABLE character_sheet_inventory_items DROP CONSTRAINT character_sheet_inventory_items_pkey;
ALTER TABLE character_sheet_inventory_items ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_items ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_items ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

### Migrate active equipment data

```sql
-- Migrate active primary weapons into inventory with equipped status
UPDATE character_sheet_inventory_weapons csw
SET equipped = TRUE, slot = 'PRIMARY'
FROM character_sheets cs
WHERE cs.id = csw.character_sheet_id
  AND cs.active_primary_weapon_id = csw.weapon_id;

-- Insert primary weapons not already in inventory
INSERT INTO character_sheet_inventory_weapons (character_sheet_id, weapon_id, equipped, slot, created_at, last_modified_at)
SELECT cs.id, cs.active_primary_weapon_id, TRUE, 'PRIMARY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_sheets cs
WHERE cs.active_primary_weapon_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM character_sheet_inventory_weapons csw
    WHERE csw.character_sheet_id = cs.id AND csw.weapon_id = cs.active_primary_weapon_id
  );

-- Migrate active secondary weapons into inventory with equipped status
UPDATE character_sheet_inventory_weapons csw
SET equipped = TRUE, slot = 'SECONDARY'
FROM character_sheets cs
WHERE cs.id = csw.character_sheet_id
  AND cs.active_secondary_weapon_id = csw.weapon_id
  AND csw.equipped = FALSE;

-- Insert secondary weapons not already in inventory
INSERT INTO character_sheet_inventory_weapons (character_sheet_id, weapon_id, equipped, slot, created_at, last_modified_at)
SELECT cs.id, cs.active_secondary_weapon_id, TRUE, 'SECONDARY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_sheets cs
WHERE cs.active_secondary_weapon_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM character_sheet_inventory_weapons csw
    WHERE csw.character_sheet_id = cs.id AND csw.weapon_id = cs.active_secondary_weapon_id AND csw.slot = 'SECONDARY'
  );

-- Migrate active armor into inventory with equipped status
UPDATE character_sheet_inventory_armors csa
SET equipped = TRUE
FROM character_sheets cs
WHERE cs.id = csa.character_sheet_id
  AND cs.active_armor_id = csa.armor_id;

-- Insert armor not already in inventory
INSERT INTO character_sheet_inventory_armors (character_sheet_id, armor_id, equipped, created_at, last_modified_at)
SELECT cs.id, cs.active_armor_id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM character_sheets cs
WHERE cs.active_armor_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM character_sheet_inventory_armors csa
    WHERE csa.character_sheet_id = cs.id AND csa.armor_id = cs.active_armor_id
  );
```

### Drop old active equipment columns

```sql
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_primary_weapon;
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_secondary_weapon;
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_armor;
ALTER TABLE character_sheets DROP COLUMN active_primary_weapon_id;
ALTER TABLE character_sheets DROP COLUMN active_secondary_weapon_id;
ALTER TABLE character_sheets DROP COLUMN active_armor_id;
```

---

## 2. Linking Entities

All three follow the `CharacterSheetDomainCard` pattern (extend `BaseEntity`, `@ManyToOne` to both sides).

### CharacterSheetWeapon

```java
@Entity
@Table(name = "character_sheet_inventory_weapons")
public class CharacterSheetWeapon extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weapon_id", nullable = false)
    private Weapon weapon;

    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;

    @Column(length = 20)
    private String slot; // "PRIMARY" or "SECONDARY", null when not equipped
}
```

### CharacterSheetArmor

```java
@Entity
@Table(name = "character_sheet_inventory_armors")
public class CharacterSheetArmor extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armor_id", nullable = false)
    private Armor armor;

    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;
}
```

### CharacterSheetLoot

```java
@Entity
@Table(name = "character_sheet_inventory_items")
public class CharacterSheetLoot extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loot_id", nullable = false)
    private Loot loot;
}
```

---

## 3. Repositories

### CharacterSheetWeaponRepository

```java
public interface CharacterSheetWeaponRepository extends JpaRepository<CharacterSheetWeapon, Long> {
    List<CharacterSheetWeapon> findByCharacterSheetId(Long characterSheetId);
    List<CharacterSheetWeapon> findByCharacterSheetIdAndEquippedTrue(Long characterSheetId);
    Optional<CharacterSheetWeapon> findByCharacterSheetIdAndSlot(Long characterSheetId, String slot);
}
```

### CharacterSheetArmorRepository

```java
public interface CharacterSheetArmorRepository extends JpaRepository<CharacterSheetArmor, Long> {
    List<CharacterSheetArmor> findByCharacterSheetId(Long characterSheetId);
    List<CharacterSheetArmor> findByCharacterSheetIdAndEquippedTrue(Long characterSheetId);
}
```

### CharacterSheetLootRepository

```java
public interface CharacterSheetLootRepository extends JpaRepository<CharacterSheetLoot, Long> {
    List<CharacterSheetLoot> findByCharacterSheetId(Long characterSheetId);
}
```

---

## 4. Request DTOs

### InventoryWeaponRequest

```java
public class InventoryWeaponRequest {
    @NotNull(message = "Weapon ID is required")
    private Long weaponId;

    @Builder.Default
    private Boolean equipped = false;

    @Size(max = 20)
    private String slot; // "PRIMARY" or "SECONDARY"
}
```

### InventoryArmorRequest

```java
public class InventoryArmorRequest {
    @NotNull(message = "Armor ID is required")
    private Long armorId;

    @Builder.Default
    private Boolean equipped = false;
}
```

### InventoryLootRequest

```java
public class InventoryLootRequest {
    @NotNull(message = "Loot ID is required")
    private Long lootId;
}
```

### CreateCharacterSheetRequest changes

Remove:
- `activePrimaryWeaponId`
- `activeSecondaryWeaponId`
- `activeArmorId`
- `inventoryWeaponIds` (`List<Long>`)
- `inventoryArmorIds` (`List<Long>`)
- `inventoryItemIds` (`List<Long>`)

Add:
- `inventoryWeapons` (`List<InventoryWeaponRequest>`)
- `inventoryArmors` (`List<InventoryArmorRequest>`)
- `inventoryItems` (`List<InventoryLootRequest>`)

### UpdateCharacterSheetRequest changes

Same removals and additions. Null list = don't change (existing pattern). Provided list = full replacement.

---

## 5. Response DTOs

### InventoryWeaponResponse

```java
public class InventoryWeaponResponse {
    private Long id;          // linking entity ID
    private Long weaponId;
    private Boolean equipped;
    private String slot;
    private WeaponResponse weapon; // expanded only when ?expand=inventoryWeapons
}
```

### InventoryArmorResponse

```java
public class InventoryArmorResponse {
    private Long id;
    private Long armorId;
    private Boolean equipped;
    private ArmorResponse armor; // expanded only when ?expand=inventoryArmors
}
```

### InventoryLootResponse

```java
public class InventoryLootResponse {
    private Long id;
    private Long lootId;
    private LootResponse loot; // expanded only when ?expand=inventoryItems
}
```

### CharacterSheetResponse changes

Remove:
- `activePrimaryWeaponId`, `activePrimaryWeapon`
- `activeSecondaryWeaponId`, `activeSecondaryWeapon`
- `activeArmorId`, `activeArmor`
- `inventoryWeaponIds`, `inventoryWeapons` (old `List<WeaponResponse>`)
- `inventoryArmorIds`, `inventoryArmors` (old `List<ArmorResponse>`)
- `inventoryItemIds`, `inventoryItems` (old `List<LootResponse>`)

Add:
- `inventoryWeapons` (`List<InventoryWeaponResponse>`) — always included with IDs, equipped status, and slot
- `inventoryArmors` (`List<InventoryArmorResponse>`) — always included
- `inventoryItems` (`List<InventoryLootResponse>`) — always included

---

## 6. CharacterSheet Entity Changes

### Remove

```java
// Active equipment FK fields
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "active_primary_weapon_id")
private Weapon activePrimaryWeapon;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "active_secondary_weapon_id")
private Weapon activeSecondaryWeapon;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "active_armor_id")
private Armor activeArmor;

// ManyToMany inventory sets
@ManyToMany(...) private Set<Weapon> inventoryWeapons;
@ManyToMany(...) private Set<Armor> inventoryArmors;
@ManyToMany(...) private Set<Loot> inventoryItems;
```

### Add

```java
@OneToMany(mappedBy = "characterSheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private Set<CharacterSheetWeapon> characterSheetWeapons = new HashSet<>();

@OneToMany(mappedBy = "characterSheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private Set<CharacterSheetArmor> characterSheetArmors = new HashSet<>();

@OneToMany(mappedBy = "characterSheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
private Set<CharacterSheetLoot> characterSheetLoot = new HashSet<>();
```

---

## 7. CharacterSheetService Changes

### Create flow

Replace inventory Set-building loops with linking entity construction:

```java
if (request.getInventoryWeapons() != null) {
    Set<CharacterSheetWeapon> weapons = new HashSet<>();
    for (InventoryWeaponRequest req : request.getInventoryWeapons()) {
        Weapon weapon = weaponRepository.findById(req.getWeaponId())
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + req.getWeaponId()));
        weapons.add(CharacterSheetWeapon.builder()
                .characterSheet(characterSheet)
                .weapon(weapon)
                .equipped(req.getEquipped() != null ? req.getEquipped() : false)
                .slot(req.getSlot())
                .build());
    }
    characterSheet.setCharacterSheetWeapons(weapons);
}
// Same pattern for armor and loot
```

Remove active equipment FK lookups (`activePrimaryWeaponId`, `activeSecondaryWeaponId`, `activeArmorId` blocks).

### Update flow

Same clear-flush-rebuild pattern as domain cards:

```java
if (request.getInventoryWeapons() != null) {
    characterSheet.getCharacterSheetWeapons().clear();
    characterSheetRepository.flush();
    for (InventoryWeaponRequest req : request.getInventoryWeapons()) {
        // ... build and add CharacterSheetWeapon entities
    }
}
```

Remove active equipment update blocks.

### toResponse changes

Replace inventory ID lists and active equipment fields with linking entity responses:

```java
// Always include inventory weapon responses
builder.inventoryWeapons(sheet.getCharacterSheetWeapons().stream()
        .map(csw -> {
            InventoryWeaponResponse.InventoryWeaponResponseBuilder iwb = InventoryWeaponResponse.builder()
                    .id(csw.getId())
                    .weaponId(csw.getWeapon().getId())
                    .equipped(csw.getEquipped())
                    .slot(csw.getSlot());
            if (expand.contains("inventoryWeapons")) {
                iwb.weapon(toWeaponResponse(csw.getWeapon(), expand));
            }
            return iwb.build();
        })
        .collect(Collectors.toList()));
// Same pattern for armor and loot
```

Remove active equipment ID/expansion blocks.

### New validation: validateWeaponSlots

```java
private void validateWeaponSlots(Set<CharacterSheetWeapon> weapons) {
    long primaryCount = weapons.stream()
            .filter(w -> "PRIMARY".equals(w.getSlot()))
            .count();
    long secondaryCount = weapons.stream()
            .filter(w -> "SECONDARY".equals(w.getSlot()))
            .count();

    if (primaryCount > 1) {
        throw new IllegalStateException("Only one PRIMARY weapon slot is allowed");
    }
    if (secondaryCount > 1) {
        throw new IllegalStateException("Only one SECONDARY weapon slot is allowed");
    }

    for (CharacterSheetWeapon w : weapons) {
        if (Boolean.TRUE.equals(w.getEquipped()) && w.getSlot() == null) {
            throw new IllegalStateException("Equipped weapons must have a slot (PRIMARY or SECONDARY)");
        }
        if (!Boolean.TRUE.equals(w.getEquipped()) && w.getSlot() != null) {
            throw new IllegalStateException("Unequipped weapons must not have a slot");
        }
        if (w.getSlot() != null && !"PRIMARY".equals(w.getSlot()) && !"SECONDARY".equals(w.getSlot())) {
            throw new IllegalStateException("Weapon slot must be PRIMARY or SECONDARY");
        }
    }
}
```

Called in both `createCharacterSheet` and `updateCharacterSheet` after building the weapon set.

---

## 8. LevelUpService

Check if `LevelUpService` references `activePrimaryWeapon`, `activeSecondaryWeapon`, or `activeArmor` on `CharacterSheet`. If so, update to use the new linking entity collections with equipped/slot filtering.

---

## 9. Unit Tests (CharacterSheetServiceTest)

Update all existing tests that reference old inventory/equipment fields.

### New test cases

- Create with inventory weapons: equipped PRIMARY, equipped SECONDARY, and unequipped
- Create with multiple equipped armor pieces
- Create with duplicate weapon IDs in inventory (same weapon twice)
- Create with duplicate loot IDs in inventory
- Update: replace inventory weapons, change equipped status
- Update: equip a weapon (set equipped=true, slot=PRIMARY)
- Update: unequip a weapon (set equipped=false, slot=null)
- Validation: two PRIMARY weapons fails
- Validation: two SECONDARY weapons fails
- Validation: equipped=true without slot fails
- Validation: equipped=false with slot fails
- Validation: invalid slot value fails
- toResponse includes inventory responses with IDs, equipped, slot
- toResponse expands nested weapon/armor/loot when requested

---

## 10. Integration Tests (CharacterSheetControllerIntegrationTest)

Update all existing tests for new request/response shapes.

### New test cases

- Full CRUD with new inventory structure (create, read, update, delete)
- Create character with equipped weapons and verify response
- Update character to equip/unequip weapons
- Update character with multiple equipped armor
- Verify expand on inventoryWeapons, inventoryArmors, inventoryItems
- Verify duplicate items in inventory work correctly
- Verify validation errors return appropriate HTTP status

---

## 11. API Blueprint Update

Update `.api-blueprint/references/character-sheets-api.md` to reflect:
- New request body structure for Create and Update endpoints
- New response body structure with `inventoryWeapons`, `inventoryArmors`, `inventoryItems` as object arrays
- Removal of `activePrimaryWeaponId`, `activeSecondaryWeaponId`, `activeArmorId` from request and response
- Removal of `inventoryWeaponIds`, `inventoryArmorIds`, `inventoryItemIds` from request and response
- Updated expand parameter documentation
- Updated example requests and responses

---

## 12. Frontend Transition Guide

Write to `docs/agent-plans/2026-03-22-inventory-frontend-transition-guide.md` with:
- Summary of what changed and why
- Before/after request body examples for Create and Update
- Before/after response body examples
- Mapping guide: old fields to new fields
- Equip/unequip patterns (how to equip PRIMARY, unequip, swap weapons)
- Multiple armor equipping examples
- Updated expand parameter usage

---

## Agent Team Task Assignment

The implementation can be parallelized across agents as follows:

### Agent 1: Database & Entities (must complete first)
1. Create the Flyway migration using `./scripts/create-migration.sh`
2. Create `CharacterSheetWeapon.java`, `CharacterSheetArmor.java`, `CharacterSheetLoot.java` entities
3. Create `CharacterSheetWeaponRepository.java`, `CharacterSheetArmorRepository.java`, `CharacterSheetLootRepository.java`
4. Update `CharacterSheet.java` entity (remove old fields, add new `@OneToMany` sets)

### Agent 2: DTOs (can start in parallel with Agent 1)
1. Create `InventoryWeaponRequest.java`, `InventoryArmorRequest.java`, `InventoryLootRequest.java`
2. Create `InventoryWeaponResponse.java`, `InventoryArmorResponse.java`, `InventoryLootResponse.java`
3. Update `CreateCharacterSheetRequest.java`
4. Update `UpdateCharacterSheetRequest.java`
5. Update `CharacterSheetResponse.java`

### Agent 3: Service Layer (depends on Agent 1 + Agent 2)
1. Update `CharacterSheetService.java` — create, update, toResponse, add validateWeaponSlots
2. Check and update `LevelUpService.java` if needed

### Agent 4: Tests (depends on Agent 3)
1. Update `CharacterSheetServiceTest.java` with new test cases
2. Update `CharacterSheetControllerIntegrationTest.java` with new test cases
3. Update `CharacterSheetTest.java` entity tests

### Agent 5: Documentation (can start after Agent 2)
1. Update `.api-blueprint/references/character-sheets-api.md`
2. Write `docs/agent-plans/2026-03-22-inventory-frontend-transition-guide.md`
