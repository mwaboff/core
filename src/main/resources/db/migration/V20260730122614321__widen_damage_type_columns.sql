-- Migration: widen_weapons_damage_type_column
-- Created: HF-02

-- No CHECK constraint exists on any damage_type column. However, each is persisted via
-- @Enumerated(EnumType.STRING) on an embedded DamageRoll.damageType, meaning the full enum
-- *name* is stored (e.g. "PHYSICAL", "MAGIC"), not the short display code from getCode().
-- weapons.damage_type, beastforms.damage_type, and adversaries.damage_type were all sized
-- VARCHAR(10), which fits the existing values but is too narrow for the new
-- DamageType.PHYSICAL_AND_MAGIC constant (18 characters) and would raise
-- "value too long for type character varying(10)" on insert/update. Widen all three, since
-- they share the same DamageRoll embeddable and the same root cause; the core book may
-- contain further dual-damage content on adversaries or beastforms (unverified until Stage 2's
-- weapon/adversary pass), and widening now avoids hitting this identical wall again later.
--
-- search_index.damage_type is already VARCHAR(50) and is left unchanged.
ALTER TABLE weapons ALTER COLUMN damage_type TYPE VARCHAR(30);
ALTER TABLE beastforms ALTER COLUMN damage_type TYPE VARCHAR(30);
ALTER TABLE adversaries ALTER COLUMN damage_type TYPE VARCHAR(30);
