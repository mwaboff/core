-- Migration: create_environments
-- Description: Creates the environments table and its features join table.
-- Environments are GM-facing stat blocks (Impulses, Difficulty, Potential
-- Adversaries, Features) that set the scene for a scenario; they are never
-- selected or equipped by a player, unlike weapons/armor/loot/adversaries.
--
-- Difficulty representation: most environments print a plain numeric
-- Difficulty, but at least one core-book environment prints
-- "Difficulty: Special (see 'Relative Strength')" instead -- a deliberate
-- rules callout, not an absent stat. difficulty and difficulty_special are
-- therefore mutually exclusive: the CHECK below requires exactly one to be
-- set, never both and never neither, matching what the physical card always
-- shows in that slot.
--
-- feature_type = 'ENVIRONMENT' and the join table below reuse the existing
-- Feature entity; the 'ENVIRONMENT' feature_type CHECK value was already
-- added by V20260730121006629 (HF-42), so no further CHECK widening is
-- needed here.

CREATE TABLE environments (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Basic information
    name VARCHAR(200) NOT NULL,
    tier INTEGER NOT NULL,
    environment_type VARCHAR(50) NOT NULL,
    description TEXT,
    impulses TEXT,

    -- Difficulty (mutually exclusive with difficulty_special -- see CHECK below)
    difficulty INTEGER,
    difficulty_special VARCHAR(255),

    -- Potential adversaries (verbatim printed text, not an FK relation)
    potential_adversaries TEXT,

    -- Content management
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    expansion_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,

    -- Metadata
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_environment_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE CASCADE,
    CONSTRAINT fk_environment_creator FOREIGN KEY (creator_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT check_environment_tier_valid CHECK (tier >= 1 AND tier <= 4),
    CONSTRAINT check_environment_difficulty_positive CHECK (difficulty IS NULL OR difficulty > 0),
    CONSTRAINT check_environment_difficulty_presence CHECK ((difficulty IS NULL) <> (difficulty_special IS NULL))
);

-- Join table for environment features (many-to-many)
CREATE TABLE environment_features (
    environment_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,

    PRIMARY KEY (environment_id, feature_id),

    CONSTRAINT fk_environment_feature_environment FOREIGN KEY (environment_id)
        REFERENCES environments(id) ON DELETE CASCADE,
    CONSTRAINT fk_environment_feature_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

-- Indexes for environments table
CREATE INDEX idx_environments_expansion ON environments(expansion_id);
CREATE INDEX idx_environments_creator ON environments(creator_id);
CREATE INDEX idx_environments_type ON environments(environment_type);
CREATE INDEX idx_environments_tier ON environments(tier);
CREATE INDEX idx_environments_is_official ON environments(is_official);
CREATE INDEX idx_environments_is_public ON environments(is_public);
CREATE INDEX idx_environments_deleted_at ON environments(deleted_at);

-- Partial index for active (non-deleted) environments - common query pattern
CREATE INDEX idx_environments_active ON environments(expansion_id, is_official, is_public)
    WHERE deleted_at IS NULL;

-- Composite index for filtering active environments by tier and type
CREATE INDEX idx_environments_active_filters ON environments(tier, environment_type)
    WHERE deleted_at IS NULL;

-- Index for inverse lookup on the join table
CREATE INDEX idx_environment_features_feature ON environment_features(feature_id);
