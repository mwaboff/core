-- Migration: create_domains_table
-- Description: Creates the domains table for Daggerheart TTRPG magical/thematic domains

CREATE TABLE domains (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon_url VARCHAR(500),
    description TEXT,
    expansion_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_domains_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT
);

-- Indexes for common queries
CREATE INDEX idx_domains_name ON domains(name);
CREATE INDEX idx_domains_expansion ON domains(expansion_id);
CREATE INDEX idx_domains_deleted_at ON domains(deleted_at);
CREATE INDEX idx_domains_active ON domains(expansion_id) WHERE deleted_at IS NULL;
