-- Migration: add_environment_to_encounters
-- Description: Adds an optional environment association to encounters, so a saved
-- encounter can carry the scene stat block it takes place in.

ALTER TABLE encounters
    ADD COLUMN environment_id BIGINT;

ALTER TABLE encounters
    ADD CONSTRAINT fk_encounter_environment FOREIGN KEY (environment_id)
        REFERENCES environments(id) ON DELETE SET NULL;

CREATE INDEX idx_encounters_environment ON encounters(environment_id);

