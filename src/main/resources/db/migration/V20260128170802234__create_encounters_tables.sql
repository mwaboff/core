-- Migration: Create encounters tables
-- Description: Creates the encounters table and encounter_adversaries join table
-- for grouping adversaries in combat encounters with count tracking

-- Main encounters table
CREATE TABLE encounters (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Basic information
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tier INTEGER,

    -- Content management
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    original_encounter_id BIGINT,
    creator_id BIGINT NOT NULL,

    -- Optional campaign association
    campaign_id BIGINT,

    -- Metadata
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_encounter_original FOREIGN KEY (original_encounter_id)
        REFERENCES encounters(id) ON DELETE SET NULL,
    CONSTRAINT fk_encounter_creator FOREIGN KEY (creator_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounter_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE SET NULL,

    -- Check constraints
    CONSTRAINT check_encounter_tier_valid CHECK (tier IS NULL OR (tier >= 1 AND tier <= 4))
);

-- Join table for encounter adversaries with count tracking
CREATE TABLE encounter_adversaries (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- References
    encounter_id BIGINT NOT NULL,
    adversary_id BIGINT NOT NULL,

    -- Count of this adversary type in the encounter
    count INTEGER NOT NULL DEFAULT 1,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_encounter_adversary_encounter FOREIGN KEY (encounter_id)
        REFERENCES encounters(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounter_adversary_adversary FOREIGN KEY (adversary_id)
        REFERENCES adversaries(id) ON DELETE CASCADE,

    -- Unique constraint: each adversary can only appear once per encounter
    CONSTRAINT uk_encounter_adversary UNIQUE (encounter_id, adversary_id),

    -- Check constraints
    CONSTRAINT check_adversary_count_positive CHECK (count >= 1)
);

-- Indexes for encounters table
CREATE INDEX idx_encounters_creator ON encounters(creator_id);
CREATE INDEX idx_encounters_campaign ON encounters(campaign_id);
CREATE INDEX idx_encounters_tier ON encounters(tier);
CREATE INDEX idx_encounters_is_official ON encounters(is_official);
CREATE INDEX idx_encounters_is_public ON encounters(is_public);
CREATE INDEX idx_encounters_deleted_at ON encounters(deleted_at);
CREATE INDEX idx_encounters_original ON encounters(original_encounter_id);

-- Partial index for active (non-deleted) encounters - common query pattern
CREATE INDEX idx_encounters_active ON encounters(is_official, is_public)
    WHERE deleted_at IS NULL;

-- Partial index for finding encounters by campaign
CREATE INDEX idx_encounters_active_campaign ON encounters(campaign_id)
    WHERE deleted_at IS NULL AND campaign_id IS NOT NULL;

-- Index for finding copies of a specific encounter
CREATE INDEX idx_encounters_copies ON encounters(original_encounter_id)
    WHERE original_encounter_id IS NOT NULL AND deleted_at IS NULL;

-- Indexes for encounter_adversaries table
CREATE INDEX idx_encounter_adversaries_encounter ON encounter_adversaries(encounter_id);
CREATE INDEX idx_encounter_adversaries_adversary ON encounter_adversaries(adversary_id);
