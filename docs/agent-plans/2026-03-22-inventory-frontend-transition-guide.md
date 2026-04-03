# Inventory Linking Entities - Frontend Transition Guide

**Date:** 2026-03-22

## Summary

The character sheet inventory system has been converted from simple ID-based many-to-many relationships to linking entities. This enables:

1. **Duplicate items** - A character can now have two of the same weapon/armor/loot
2. **Multiple equipped armor** - No longer limited to a single active armor piece
3. **Explicit equip/unequip** - Equipment state is managed through the inventory arrays, not separate active equipment fields
4. **Per-instance state** - Each inventory entry has its own ID, equipped status, and (for weapons) slot assignment

The old `activePrimaryWeaponId`, `activeSecondaryWeaponId`, `activeArmorId`, and inventory ID arrays have been removed. Equipment state is now embedded in the inventory entries themselves.

---

## Field Mapping: Old to New

### Removed Request Fields

| Old Field | Replacement |
|-----------|-------------|
| `activePrimaryWeaponId` | Set `equipped: true, slot: "PRIMARY"` on a weapon in `inventoryWeapons` |
| `activeSecondaryWeaponId` | Set `equipped: true, slot: "SECONDARY"` on a weapon in `inventoryWeapons` |
| `activeArmorId` | Set `equipped: true` on an armor in `inventoryArmors` |
| `inventoryWeaponIds` (long[]) | `inventoryWeapons` (InventoryWeaponRequest[]) |
| `inventoryArmorIds` (long[]) | `inventoryArmors` (InventoryArmorRequest[]) |
| `inventoryItemIds` (long[]) | `inventoryItems` (InventoryLootRequest[]) |

### Removed Response Fields

| Old Field | Replacement |
|-----------|-------------|
| `activePrimaryWeaponId` | Find entry in `inventoryWeapons` where `slot === "PRIMARY"` |
| `activePrimaryWeapon` | Find entry in `inventoryWeapons` where `slot === "PRIMARY"`, use `.weapon` |
| `activeSecondaryWeaponId` | Find entry in `inventoryWeapons` where `slot === "SECONDARY"` |
| `activeSecondaryWeapon` | Find entry in `inventoryWeapons` where `slot === "SECONDARY"`, use `.weapon` |
| `activeArmorId` | Filter `inventoryArmors` where `equipped === true` |
| `activeArmor` | Filter `inventoryArmors` where `equipped === true`, use `.armor` |
| `inventoryWeaponIds` (long[]) | `inventoryWeapons[].weaponId` |
| `inventoryArmorIds` (long[]) | `inventoryArmors[].armorId` |
| `inventoryItemIds` (long[]) | `inventoryItems[].lootId` |

---

## Request Body Changes

### Before (Create/Update)

```json
{
  "name": "Strider",
  "level": 3,
  "activePrimaryWeaponId": 5,
  "activeSecondaryWeaponId": 8,
  "activeArmorId": 6,
  "inventoryWeaponIds": [5, 8, 12],
  "inventoryArmorIds": [6, 9],
  "inventoryItemIds": [7, 7]
}
```

Note: The old system could NOT have `[7, 7]` (duplicate loot). Duplicates were silently deduplicated.

### After (Create/Update)

```json
{
  "name": "Strider",
  "level": 3,
  "inventoryWeapons": [
    {"weaponId": 5, "equipped": true, "slot": "PRIMARY"},
    {"weaponId": 8, "equipped": true, "slot": "SECONDARY"},
    {"weaponId": 12, "equipped": false}
  ],
  "inventoryArmors": [
    {"armorId": 6, "equipped": true},
    {"armorId": 9, "equipped": false}
  ],
  "inventoryItems": [
    {"lootId": 7},
    {"lootId": 7}
  ]
}
```

Key differences:
- Active equipment is declared inline via `equipped` and `slot` fields
- Duplicate items are now supported (two entries with the same `lootId`)
- `equipped` defaults to `false` if omitted
- `slot` is only for weapons: `"PRIMARY"` or `"SECONDARY"`

---

## Response Body Changes

### Before

```json
{
  "id": 1,
  "name": "Strider",
  "activePrimaryWeaponId": 5,
  "activePrimaryWeapon": {"id": 5, "name": "Longsword", "...": "..."},
  "activeSecondaryWeaponId": 8,
  "activeSecondaryWeapon": {"id": 8, "name": "Dagger", "...": "..."},
  "activeArmorId": 6,
  "activeArmor": {"id": 6, "name": "Chainmail", "...": "..."},
  "inventoryWeaponIds": [5, 8, 12],
  "inventoryWeapons": [
    {"id": 5, "name": "Longsword", "...": "..."},
    {"id": 8, "name": "Dagger", "...": "..."},
    {"id": 12, "name": "Bow", "...": "..."}
  ],
  "inventoryArmorIds": [6, 9],
  "inventoryItemIds": [7]
}
```

### After

```json
{
  "id": 1,
  "name": "Strider",
  "inventoryWeapons": [
    {"id": 100, "weaponId": 5, "equipped": true, "slot": "PRIMARY"},
    {"id": 101, "weaponId": 8, "equipped": true, "slot": "SECONDARY"},
    {"id": 102, "weaponId": 12, "equipped": false}
  ],
  "inventoryArmors": [
    {"id": 200, "armorId": 6, "equipped": true},
    {"id": 201, "armorId": 9, "equipped": false}
  ],
  "inventoryItems": [
    {"id": 300, "lootId": 7},
    {"id": 301, "lootId": 7}
  ]
}
```

Key differences:
- `inventoryWeapons`, `inventoryArmors`, `inventoryItems` are **always included** (not expand-dependent)
- Each entry has its own `id` (the linking entity ID, not the item ID)
- `weaponId`/`armorId`/`lootId` reference the actual item
- `equipped` and `slot` are included on weapon/armor entries
- No more `activePrimaryWeaponId`, `activeSecondaryWeaponId`, `activeArmorId` fields
- No more `inventoryWeaponIds`, `inventoryArmorIds`, `inventoryItemIds` arrays

### After (with `?expand=inventoryWeapons,inventoryArmors`)

```json
{
  "id": 1,
  "name": "Strider",
  "inventoryWeapons": [
    {
      "id": 100,
      "weaponId": 5,
      "equipped": true,
      "slot": "PRIMARY",
      "weapon": {"id": 5, "name": "Longsword", "tier": 2, "...": "..."}
    },
    {
      "id": 101,
      "weaponId": 8,
      "equipped": true,
      "slot": "SECONDARY",
      "weapon": {"id": 8, "name": "Dagger", "tier": 1, "...": "..."}
    },
    {
      "id": 102,
      "weaponId": 12,
      "equipped": false,
      "weapon": {"id": 12, "name": "Bow", "tier": 2, "...": "..."}
    }
  ],
  "inventoryArmors": [
    {
      "id": 200,
      "armorId": 6,
      "equipped": true,
      "armor": {"id": 6, "name": "Chainmail", "tier": 2, "...": "..."}
    },
    {
      "id": 201,
      "armorId": 9,
      "equipped": false,
      "armor": {"id": 9, "name": "Shield Amulet", "tier": 1, "...": "..."}
    }
  ],
  "inventoryItems": [
    {"id": 300, "lootId": 7},
    {"id": 301, "lootId": 7}
  ]
}
```

---

## Equip/Unequip Patterns

All equipment changes go through the `PUT /api/dh/character-sheets/{id}` endpoint by providing the full `inventoryWeapons` or `inventoryArmors` array. This uses **full-replacement semantics** -- the provided array replaces all existing entries.

### Equip a weapon as PRIMARY

Send the full inventory with the target weapon's `equipped` set to `true` and `slot` set to `"PRIMARY"`:

```json
{
  "inventoryWeapons": [
    {"weaponId": 5, "equipped": true, "slot": "PRIMARY"},
    {"weaponId": 8, "equipped": false},
    {"weaponId": 12, "equipped": false}
  ]
}
```

### Unequip a weapon

Set `equipped` to `false` and remove the `slot`:

```json
{
  "inventoryWeapons": [
    {"weaponId": 5, "equipped": false},
    {"weaponId": 8, "equipped": false},
    {"weaponId": 12, "equipped": false}
  ]
}
```

### Swap PRIMARY and SECONDARY weapons

```json
{
  "inventoryWeapons": [
    {"weaponId": 5, "equipped": true, "slot": "SECONDARY"},
    {"weaponId": 8, "equipped": true, "slot": "PRIMARY"},
    {"weaponId": 12, "equipped": false}
  ]
}
```

### Equip a new PRIMARY weapon (replacing the current one)

```json
{
  "inventoryWeapons": [
    {"weaponId": 5, "equipped": false},
    {"weaponId": 8, "equipped": true, "slot": "SECONDARY"},
    {"weaponId": 12, "equipped": true, "slot": "PRIMARY"}
  ]
}
```

---

## Multiple Armor Equipping

Unlike the old system (single `activeArmorId`), you can now equip multiple armor pieces:

```json
{
  "inventoryArmors": [
    {"armorId": 6, "equipped": true},
    {"armorId": 9, "equipped": true},
    {"armorId": 15, "equipped": false}
  ]
}
```

To unequip all armor:

```json
{
  "inventoryArmors": [
    {"armorId": 6, "equipped": false},
    {"armorId": 9, "equipped": false},
    {"armorId": 15, "equipped": false}
  ]
}
```

---

## Validation Rules

### Weapon Slots
- Maximum one weapon with `slot: "PRIMARY"`
- Maximum one weapon with `slot: "SECONDARY"`
- `equipped: true` requires a `slot` value
- `equipped: false` must not have a `slot` value
- `slot` must be `"PRIMARY"` or `"SECONDARY"`

### General
- `weaponId`, `armorId`, `lootId` must reference existing items
- Providing the array replaces all entries (full-replacement semantics)
- Omitting the array leaves the collection unchanged (null = no change)

---

## Expand Parameter Changes

### Removed expand values
- `activePrimaryWeapon`
- `activeSecondaryWeapon`
- `activeArmor`

### Changed behavior
- `inventoryWeapons` -- previously expanded the full `WeaponResponse[]` at the top level. Now expands the nested `weapon` object within each `InventoryWeaponResponse` entry.
- `inventoryArmors` -- same pattern, expands nested `armor` within each `InventoryArmorResponse`.
- `inventoryItems` -- same pattern, expands nested `loot` within each `InventoryLootResponse`.

### Examples

Without expand -- inventory entries include IDs and state only:
```
GET /api/dh/character-sheets/1
```

With expand -- inventory entries include full nested objects:
```
GET /api/dh/character-sheets/1?expand=inventoryWeapons,inventoryArmors,inventoryItems
```

Nested expand still works -- to get weapon features:
```
GET /api/dh/character-sheets/1?expand=inventoryWeapons,features
```

---

## Frontend Helper Patterns

### Get equipped primary weapon from response

```javascript
// Old
const primaryWeapon = response.activePrimaryWeapon;
const primaryWeaponId = response.activePrimaryWeaponId;

// New
const primaryEntry = response.inventoryWeapons.find(w => w.slot === "PRIMARY");
const primaryWeapon = primaryEntry?.weapon;     // requires ?expand=inventoryWeapons
const primaryWeaponId = primaryEntry?.weaponId;
```

### Get equipped secondary weapon from response

```javascript
// Old
const secondaryWeapon = response.activeSecondaryWeapon;

// New
const secondaryEntry = response.inventoryWeapons.find(w => w.slot === "SECONDARY");
const secondaryWeapon = secondaryEntry?.weapon;
```

### Get all equipped armor from response

```javascript
// Old (single armor only)
const armor = response.activeArmor;

// New (multiple armor supported)
const equippedArmors = response.inventoryArmors.filter(a => a.equipped);
const armorObjects = equippedArmors.map(a => a.armor); // requires ?expand=inventoryArmors
```

### Get inventory weapon IDs (for backward-compatible logic)

```javascript
// Old
const weaponIds = response.inventoryWeaponIds;

// New
const weaponIds = response.inventoryWeapons.map(w => w.weaponId);
```
