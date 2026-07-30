-- Migration: allow_null_adversary_stats_for_framework_blocks
-- Description: Some adversaries (e.g. Forlorne Lykona, Hope & Fear p.143) print a
-- "framework" stat block shared across their forms, plus separate per-form stat
-- blocks. The framework block has no Difficulty or Thresholds of its own -- those
-- are only defined on the form-specific blocks. Allow these three columns to be
-- NULL so the framework block can be persisted as its own adversary record.
--
-- No CHECK constraint changes are required: check_difficulty_positive,
-- check_major_threshold_non_negative, check_severe_threshold_non_negative, and
-- check_severe_gte_major are all plain comparisons, which evaluate to UNKNOWN
-- (and therefore pass) when either operand is NULL.

ALTER TABLE adversaries ALTER COLUMN difficulty DROP NOT NULL;
ALTER TABLE adversaries ALTER COLUMN major_threshold DROP NOT NULL;
ALTER TABLE adversaries ALTER COLUMN severe_threshold DROP NOT NULL;
