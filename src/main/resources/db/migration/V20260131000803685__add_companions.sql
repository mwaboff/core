-- Migration: Add companions table and modify experiences for companion support
-- Companions are entities that can be attached to character sheets, with their own
-- experiences, attacks, and stress tracking.

-- Create companions table
CREATE TABLE companions (
    id                  BIGSERIAL PRIMARY KEY,
    character_sheet_id  BIGINT NOT NULL REFERENCES character_sheets(id) ON DELETE CASCADE,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    evasion             INTEGER NOT NULL DEFAULT 0,
    attack_name         VARCHAR(200) NOT NULL,
    attack_range        VARCHAR(50) NOT NULL,
    damage_dice         VARCHAR(10) NOT NULL,
    stress_max          INTEGER NOT NULL DEFAULT 3,
    stress_marked       INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companions_character_sheet_id ON companions(character_sheet_id);

-- Modify experiences table to support companion ownership
-- Add companion_id column (nullable)
ALTER TABLE experiences ADD COLUMN companion_id BIGINT REFERENCES companions(id) ON DELETE CASCADE;

-- Make character_sheet_id nullable (was NOT NULL)
ALTER TABLE experiences ALTER COLUMN character_sheet_id DROP NOT NULL;

-- Add check constraint: exactly one owner must be set
ALTER TABLE experiences ADD CONSTRAINT chk_experience_single_owner
    CHECK (
        (character_sheet_id IS NOT NULL AND companion_id IS NULL) OR
        (character_sheet_id IS NULL AND companion_id IS NOT NULL)
    );

-- Add index for companion experience lookups
CREATE INDEX idx_experiences_companion_id ON experiences(companion_id);
