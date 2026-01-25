-- Create loot table for Daggerheart miscellaneous items
CREATE TABLE loot (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    is_official BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id BIGINT REFERENCES users(id),
    original_loot_id BIGINT REFERENCES loot(id),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX idx_loot_expansion ON loot(expansion_id);
CREATE INDEX idx_loot_is_official ON loot(is_official);
CREATE INDEX idx_loot_created_by ON loot(created_by_user_id);
CREATE INDEX idx_loot_original ON loot(original_loot_id);

-- Partial index for active (non-deleted) loot
CREATE INDEX idx_loot_active ON loot(expansion_id, is_official) WHERE deleted_at IS NULL;
