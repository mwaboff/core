-- Migration: clear_weapon_dice_count_of_one
-- Created: Fri Jul 31 04:12:48 PM EDT 2026
--
-- Clears weapons.dice_count where it was imported as 1, so the character sheet falls back to the
-- character's Proficiency for the dice count instead of hard-coding a single die.
--
-- weapons.dice_count is nullable by design: NULL means "roll a number of dice equal to your
-- Proficiency", which is how every player weapon in Daggerheart works. A printed weapon reads
-- "d10+4 mag", never "1d10+4". An explicit 1 therefore overrides Proficiency with a single die and
-- silently under-reports damage for every character above Proficiency 1.
--
-- Measured against production and an identical local prod restore, both at flyway version 77:
--   * 134 weapons have dice_count = 1. All 134 belong to the Daggerheart Core Set expansion, which
--     predates the current import pipeline.
--   * No weapon anywhere has dice_count > 1, so 1 is never a legitimate stored value and this
--     repair cannot destroy a deliberate choice.
--   * All 140 Hope & Fear weapons and 66 of the 200 Core Set weapons are already NULL. Tier 1 Core
--     Set weapons are 100% correct; the damage is concentrated in tiers 2-4, consistent with a
--     parser that emitted a spurious 1 whenever a "+N" damage modifier was present.
--   * Spot-checked against the printed book: Ego Blade prints "d12+4 mag" and was stored as
--     dice_count 1, dice_type D12, modifier 4.
--
-- Current import payloads already omit diceCount entirely, so this is a one-off repair of legacy
-- rows rather than a recurring defect.

UPDATE weapons
SET dice_count = NULL, last_modified_at = CURRENT_TIMESTAMP
WHERE dice_count = 1;
