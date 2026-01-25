-- Create weapons table for Daggerheart weapon items
CREATE TABLE weapons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    is_primary BOOLEAN NOT NULL,
    trait VARCHAR(20) NOT NULL,
    range VARCHAR(20) NOT NULL,
    burden VARCHAR(20) NOT NULL,
    dice_count INTEGER,
    dice_type VARCHAR(10) NOT NULL,
    modifier INTEGER,
    damage_type VARCHAR(10) NOT NULL,
    feature_id BIGINT REFERENCES features(id),
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    is_official BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id BIGINT REFERENCES users(id),
    original_weapon_id BIGINT REFERENCES weapons(id),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX idx_weapons_expansion ON weapons(expansion_id);
CREATE INDEX idx_weapons_is_official ON weapons(is_official);
CREATE INDEX idx_weapons_trait ON weapons(trait);
CREATE INDEX idx_weapons_range ON weapons(range);
CREATE INDEX idx_weapons_burden ON weapons(burden);
CREATE INDEX idx_weapons_is_primary ON weapons(is_primary);
CREATE INDEX idx_weapons_created_by ON weapons(created_by_user_id);
CREATE INDEX idx_weapons_original ON weapons(original_weapon_id);

-- Partial index for active (non-deleted) weapons - common query pattern
CREATE INDEX idx_weapons_active ON weapons(expansion_id, is_official) WHERE deleted_at IS NULL;

-- Composite index for filtering active weapons by type characteristics
CREATE INDEX idx_weapons_active_filters ON weapons(trait, range, burden) WHERE deleted_at IS NULL;
