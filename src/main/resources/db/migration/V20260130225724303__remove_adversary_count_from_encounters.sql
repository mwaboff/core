-- Migration: Remove adversary count from encounters
-- Description: Removes the count column from encounter_adversaries table
-- and the unique constraint to allow multiple instances of the same adversary

-- Drop the unique constraint that prevented duplicate adversaries
ALTER TABLE encounter_adversaries DROP CONSTRAINT uk_encounter_adversary;

-- Drop the check constraint on count
ALTER TABLE encounter_adversaries DROP CONSTRAINT check_adversary_count_positive;

-- Drop the count column
ALTER TABLE encounter_adversaries DROP COLUMN count;
