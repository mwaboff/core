-- Migration: drop_character_sheet_marked_lte_max_constraints
-- Created: Tue Apr 14 05:03:55 PM EDT 2026
--
-- Drop check constraints that enforced marked <= max on character_sheets.
--
-- Equipped items and features in Daggerheart can temporarily raise a character's
-- resource caps (e.g. a shield adds +1 to armor max). Players mark those boxes
-- during play, producing legitimate states where marked > base max.
--
-- The stored *_max remains the character's *base* max. The frontend computes the
-- effective max from equipped items and sends the appropriate marked value. The
-- backend no longer enforces marked <= max at the DB layer; the service layer
-- only clamps when the user explicitly lowers the base max.

ALTER TABLE character_sheets DROP CONSTRAINT IF EXISTS check_hit_point_marked_lte_max;
ALTER TABLE character_sheets DROP CONSTRAINT IF EXISTS check_stress_marked_lte_max;
ALTER TABLE character_sheets DROP CONSTRAINT IF EXISTS check_hope_marked_lte_max;
ALTER TABLE character_sheets DROP CONSTRAINT IF EXISTS check_armor_marked_lte_max;
