-- Create beastforms table
CREATE TABLE beastforms (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Basic Information
    name VARCHAR(200) NOT NULL,
    example TEXT,
    advantages TEXT,

    -- Trait Modifiers
    agility_modifier INTEGER NOT NULL DEFAULT 0,
    strength_modifier INTEGER NOT NULL DEFAULT 0,
    finesse_modifier INTEGER NOT NULL DEFAULT 0,
    instinct_modifier INTEGER NOT NULL DEFAULT 0,
    presence_modifier INTEGER NOT NULL DEFAULT 0,
    knowledge_modifier INTEGER NOT NULL DEFAULT 0,

    -- Combat
    attack_range VARCHAR(20) NOT NULL,
    attack_trait VARCHAR(20) NOT NULL,
    damage_dice_count INTEGER,
    damage_dice_type VARCHAR(10) NOT NULL,
    damage_modifier INTEGER,
    damage_type VARCHAR(10) NOT NULL,

    -- Content Management
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    original_beastform_id BIGINT REFERENCES beastforms(id),
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    creator_id BIGINT NOT NULL REFERENCES users(id),

    -- Soft Delete
    deleted_at TIMESTAMP
);

-- Create beastform_features join table
CREATE TABLE beastform_features (
    beastform_id BIGINT NOT NULL REFERENCES beastforms(id) ON DELETE CASCADE,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    PRIMARY KEY (beastform_id, feature_id)
);

-- Indexes for beastforms table
CREATE INDEX idx_beastforms_expansion_id ON beastforms(expansion_id);
CREATE INDEX idx_beastforms_creator_id ON beastforms(creator_id);
CREATE INDEX idx_beastforms_deleted_at ON beastforms(deleted_at);
CREATE INDEX idx_beastforms_is_official ON beastforms(is_official);
CREATE INDEX idx_beastforms_is_public ON beastforms(is_public);
