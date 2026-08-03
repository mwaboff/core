-- Migration: add_tokens_to_encounter_run_adversaries
-- Created: Sun Aug  2 11:25:42 PM EDT 2026
--
-- Adversary Tokens (Daggerheart Core ch. 4): some adversaries require tokens placed on their
-- stat block for certain features -- e.g. the `Slow` passive, which keeps the adversary from
-- acting until a token is placed and then cleared, and Hope & Fear's `Pool`/`Evolution`
-- features. Like hit_points_marked/stress_marked, this is live combat state for the duration of
-- a fight, so it lives on the run instance, never on the catalog `adversaries` table.
--
-- Unlike hit_points_marked/stress_marked there is no ceiling to check against (a Pool can hold
-- any number of tokens), so only a floor constraint is added, following the same
-- check_encounter_run_adversary_* naming convention as the existing marked-value constraints.

ALTER TABLE encounter_run_adversaries
    ADD COLUMN tokens INTEGER NOT NULL DEFAULT 0;

ALTER TABLE encounter_run_adversaries
    ADD CONSTRAINT check_encounter_run_adversary_tokens
        CHECK (tokens >= 0);
