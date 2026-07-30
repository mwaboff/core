-- Migration: widen_weapons_damage_type_column
-- Created: HF-02

-- No CHECK constraint exists on weapons.damage_type. However, the column is persisted via
-- @Enumerated(EnumType.STRING) on DamageRoll.damageType, meaning the full enum *name* is
-- stored (e.g. "PHYSICAL", "MAGIC"), not the short display code from getCode(). The column
-- was sized VARCHAR(10), which fits the existing values but is too narrow for the new
-- DamageType.PHYSICAL_AND_MAGIC constant (18 characters). Widen it to avoid a truncation
-- error on insert/update.
ALTER TABLE weapons ALTER COLUMN damage_type TYPE VARCHAR(30);
