-- Migration: add_label_tier_override_display_order_to_encounter_adversaries
-- Description: Completes the per-instance shape of encounter_adversaries. One row per
-- adversary instance is deliberately preserved (migration V20260130225724303 removed the
-- old `count` column for this reason) -- these columns give each instance a GM nickname,
-- an optional retier target, and a stable ordering for display.

ALTER TABLE encounter_adversaries
    ADD COLUMN label VARCHAR(100),
    ADD COLUMN tier_override INTEGER,
    ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE encounter_adversaries
    ADD CONSTRAINT check_encounter_adversary_tier_override_valid
        CHECK (tier_override IS NULL OR (tier_override >= 1 AND tier_override <= 4));

