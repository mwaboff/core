-- Migration: create_features_table
-- Description: Creates the features table for Daggerheart TTRPG character features and abilities

CREATE TABLE features (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    feature_type VARCHAR(20) NOT NULL,
    expansion_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_features_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_features_feature_type CHECK (feature_type IN ('HOPE', 'ANCESTRY', 'CLASS', 'COMMUNITY', 'DOMAIN', 'OTHER'))
);

-- Indexes for common queries
CREATE INDEX idx_features_name ON features(name);
CREATE INDEX idx_features_feature_type ON features(feature_type);
CREATE INDEX idx_features_expansion ON features(expansion_id);
CREATE INDEX idx_features_deleted_at ON features(deleted_at);
CREATE INDEX idx_features_active ON features(expansion_id) WHERE deleted_at IS NULL;
