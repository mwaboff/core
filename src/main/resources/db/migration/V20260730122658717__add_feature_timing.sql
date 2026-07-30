-- Migration: add_feature_timing
-- Description: Adds a nullable timing column to features, capturing the Action/Reaction/
-- Passive/Evolution tag printed as part of a feature's heading in the source material.
-- Most features carry no timing tag, so the column stays null.

ALTER TABLE features ADD COLUMN timing VARCHAR(20);

ALTER TABLE features ADD CONSTRAINT chk_features_timing
    CHECK (timing IS NULL OR timing IN ('ACTION', 'REACTION', 'PASSIVE', 'EVOLUTION'));
