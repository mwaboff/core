-- Create martial_stances table for Daggerheart modal combat stances (Hope & Fear, Stance Fighter)
CREATE TABLE martial_stances (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tier INTEGER NOT NULL DEFAULT 1,
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    is_official BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id BIGINT REFERENCES users(id),
    original_martial_stance_id BIGINT REFERENCES martial_stances(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_martial_stances_tier CHECK (tier BETWEEN 1 AND 4)
);

-- Feature join table, matching the BaseItem features association used by weapons/armor/loot
CREATE TABLE martial_stance_features (
    martial_stance_id BIGINT NOT NULL REFERENCES martial_stances(id) ON DELETE CASCADE,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    PRIMARY KEY (martial_stance_id, feature_id)
);

-- Indexes for common query patterns
CREATE INDEX idx_martial_stances_expansion ON martial_stances(expansion_id);
CREATE INDEX idx_martial_stances_is_official ON martial_stances(is_official);
CREATE INDEX idx_martial_stances_created_by ON martial_stances(created_by_user_id);
CREATE INDEX idx_martial_stances_original ON martial_stances(original_martial_stance_id);
CREATE INDEX idx_martial_stances_tier ON martial_stances(tier);
CREATE INDEX idx_martial_stance_features_martial_stance_id ON martial_stance_features(martial_stance_id);
CREATE INDEX idx_martial_stance_features_feature_id ON martial_stance_features(feature_id);

-- Partial index for active (non-deleted) martial stances
CREATE INDEX idx_martial_stances_active ON martial_stances(expansion_id, is_official) WHERE deleted_at IS NULL;
