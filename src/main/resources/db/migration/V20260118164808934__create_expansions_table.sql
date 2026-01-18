-- Migration: create_expansions_table
-- Description: Creates the expansions table for Daggerheart TTRPG expansion packs

CREATE TABLE expansions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_expansions_name ON expansions(name);
CREATE INDEX idx_expansions_is_published ON expansions(is_published);
CREATE INDEX idx_expansions_deleted_at ON expansions(deleted_at);
CREATE INDEX idx_expansions_active_published ON expansions(is_published) WHERE deleted_at IS NULL;
