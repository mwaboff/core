-- Relax adversary threshold constraints to allow 0 values.
-- Major and severe thresholds are optional game data; some adversaries
-- (e.g. minions) have no meaningful threshold and default to 0.
-- The ordering check (severe >= major) is kept but also allows both to be 0.

ALTER TABLE adversaries DROP CONSTRAINT check_major_threshold_positive;
ALTER TABLE adversaries DROP CONSTRAINT check_severe_threshold_positive;
ALTER TABLE adversaries DROP CONSTRAINT check_severe_gte_major;

ALTER TABLE adversaries ADD CONSTRAINT check_major_threshold_non_negative CHECK (major_threshold >= 0);
ALTER TABLE adversaries ADD CONSTRAINT check_severe_threshold_non_negative CHECK (severe_threshold >= 0);
ALTER TABLE adversaries ADD CONSTRAINT check_severe_gte_major CHECK (severe_threshold >= major_threshold);
