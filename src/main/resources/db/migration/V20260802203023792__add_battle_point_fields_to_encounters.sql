-- Migration: add_battle_point_fields_to_encounters
-- Description: Adds the manually-entered party size and the six Battle Point adjustment
-- toggles to encounters, per the rulebook's budget formula:
--   suggestedBudget = (3 * partySize) + 2 + sum(adjustment deltas)
-- Party size is never derived from a campaign roster -- it is always entered by the GM.

ALTER TABLE encounters
    ADD COLUMN party_size INTEGER,
    ADD COLUMN adjustment_easier BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN adjustment_two_plus_solos BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN adjustment_bonus_damage BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN adjustment_lower_tier BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN adjustment_no_elites BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN adjustment_harder BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE encounters
    ADD CONSTRAINT check_encounter_party_size_valid
        CHECK (party_size IS NULL OR (party_size >= 1 AND party_size <= 12));
