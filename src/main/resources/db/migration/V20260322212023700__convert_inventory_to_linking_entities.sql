-- Migration: convert_inventory_to_linking_entities
-- Created: Sun Mar 22 09:20:23 PM EDT 2026

-- ========== Convert character_sheet_inventory_weapons to linking entity ==========
ALTER TABLE character_sheet_inventory_weapons DROP CONSTRAINT character_sheet_inventory_weapons_pkey;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN slot VARCHAR(20);
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_weapons ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== Convert character_sheet_inventory_armors to linking entity ==========
ALTER TABLE character_sheet_inventory_armors DROP CONSTRAINT character_sheet_inventory_armors_pkey;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_armors ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== Convert character_sheet_inventory_items to linking entity ==========
ALTER TABLE character_sheet_inventory_items DROP CONSTRAINT character_sheet_inventory_items_pkey;
ALTER TABLE character_sheet_inventory_items ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_inventory_items ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_inventory_items ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ========== Migrate active equipment data into inventory tables ==========

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

-- ========== Drop old active equipment columns from character_sheets ==========
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_primary_weapon;
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_secondary_weapon;
ALTER TABLE character_sheets DROP CONSTRAINT fk_character_sheet_active_armor;
ALTER TABLE character_sheets DROP COLUMN active_primary_weapon_id;
ALTER TABLE character_sheets DROP COLUMN active_secondary_weapon_id;
ALTER TABLE character_sheets DROP COLUMN active_armor_id;
