-- Migration: Create experiences table
-- Description: Creates the experiences table for tracking character experiences

CREATE TABLE experiences (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    character_sheet_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,

    -- Experience data
    description TEXT NOT NULL,
    modifier INTEGER NOT NULL DEFAULT 2,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_experience_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_experience_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_experiences_character_sheet_id ON experiences(character_sheet_id);
CREATE INDEX idx_experiences_created_by_user_id ON experiences(created_by_user_id);
