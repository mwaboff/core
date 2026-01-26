-- Migration: Create campaigns table
-- Created: Mon Jan 26 09:42:33 AM EST 2026
-- This table stores campaign information for Daggerheart TTRPG sessions

CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    creator_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_creator FOREIGN KEY (creator_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- Index for finding campaigns by creator
CREATE INDEX idx_campaigns_creator_id ON campaigns(creator_id);

-- Index for filtering by deleted status
CREATE INDEX idx_campaigns_deleted_at ON campaigns(deleted_at);

-- Composite index for finding active campaigns by creator
CREATE INDEX idx_campaigns_creator_not_deleted ON campaigns(creator_id, deleted_at)
    WHERE deleted_at IS NULL;

-- Index for searching by name
CREATE INDEX idx_campaigns_name ON campaigns(name);
