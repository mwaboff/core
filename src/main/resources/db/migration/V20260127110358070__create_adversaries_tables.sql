-- Migration: Create adversaries tables
-- Description: Creates the adversaries table and join tables for experiences and features

-- Main adversaries table
CREATE TABLE adversaries (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Basic information
    name VARCHAR(200) NOT NULL,
    tier INTEGER NOT NULL,
    adversary_type VARCHAR(50) NOT NULL,
    description TEXT,
    motives_and_tactics TEXT,

    -- Difficulty and damage thresholds
    difficulty INTEGER NOT NULL,
    major_threshold INTEGER NOT NULL,
    severe_threshold INTEGER NOT NULL,

    -- Resources (HP and Stress system)
    hit_point_max INTEGER NOT NULL DEFAULT 0,
    hit_point_marked INTEGER NOT NULL DEFAULT 0,
    stress_max INTEGER NOT NULL DEFAULT 0,
    stress_marked INTEGER NOT NULL DEFAULT 0,

    -- Combat information
    attack_modifier INTEGER,
    weapon_name VARCHAR(200),
    attack_range VARCHAR(20),

    -- Damage roll (embedded DamageRoll fields)
    damage_dice_count INTEGER,
    damage_dice_type VARCHAR(10),
    damage_modifier INTEGER,
    damage_type VARCHAR(10),

    -- Content management
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    original_adversary_id BIGINT,
    expansion_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,

    -- Metadata
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_adversary_original FOREIGN KEY (original_adversary_id)
        REFERENCES adversaries(id) ON DELETE SET NULL,
    CONSTRAINT fk_adversary_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE CASCADE,
    CONSTRAINT fk_adversary_creator FOREIGN KEY (creator_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT check_tier_valid CHECK (tier >= 1 AND tier <= 4),
    CONSTRAINT check_difficulty_positive CHECK (difficulty > 0),
    CONSTRAINT check_major_threshold_positive CHECK (major_threshold > 0),
    CONSTRAINT check_severe_threshold_positive CHECK (severe_threshold > 0),
    CONSTRAINT check_severe_gte_major CHECK (severe_threshold >= major_threshold),
    CONSTRAINT check_hit_point_marked_lte_max CHECK (hit_point_marked <= hit_point_max),
    CONSTRAINT check_stress_marked_lte_max CHECK (stress_marked <= stress_max)
);

-- Join table for adversary experiences (many-to-many)
CREATE TABLE adversary_experiences (
    adversary_id BIGINT NOT NULL,
    experience_id BIGINT NOT NULL,

    PRIMARY KEY (adversary_id, experience_id),

    CONSTRAINT fk_adversary_experience_adversary FOREIGN KEY (adversary_id)
        REFERENCES adversaries(id) ON DELETE CASCADE,
    CONSTRAINT fk_adversary_experience_experience FOREIGN KEY (experience_id)
        REFERENCES experiences(id) ON DELETE CASCADE
);

-- Join table for adversary features (many-to-many)
CREATE TABLE adversary_features (
    adversary_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,

    PRIMARY KEY (adversary_id, feature_id),

    CONSTRAINT fk_adversary_feature_adversary FOREIGN KEY (adversary_id)
        REFERENCES adversaries(id) ON DELETE CASCADE,
    CONSTRAINT fk_adversary_feature_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

-- Indexes for adversaries table
CREATE INDEX idx_adversaries_expansion ON adversaries(expansion_id);
CREATE INDEX idx_adversaries_creator ON adversaries(creator_id);
CREATE INDEX idx_adversaries_type ON adversaries(adversary_type);
CREATE INDEX idx_adversaries_tier ON adversaries(tier);
CREATE INDEX idx_adversaries_is_official ON adversaries(is_official);
CREATE INDEX idx_adversaries_is_public ON adversaries(is_public);
CREATE INDEX idx_adversaries_deleted_at ON adversaries(deleted_at);
CREATE INDEX idx_adversaries_original ON adversaries(original_adversary_id);

-- Partial index for active (non-deleted) adversaries - common query pattern
CREATE INDEX idx_adversaries_active ON adversaries(expansion_id, is_official, is_public)
    WHERE deleted_at IS NULL;

-- Composite index for filtering active adversaries by tier and type
CREATE INDEX idx_adversaries_active_filters ON adversaries(tier, adversary_type)
    WHERE deleted_at IS NULL;

-- Index for finding copies of a specific adversary
CREATE INDEX idx_adversaries_copies ON adversaries(original_adversary_id)
    WHERE original_adversary_id IS NOT NULL AND deleted_at IS NULL;

-- Indexes for join tables (inverse lookups)
CREATE INDEX idx_adversary_experiences_experience ON adversary_experiences(experience_id);
CREATE INDEX idx_adversary_features_feature ON adversary_features(feature_id);
