-- Create armors table for Daggerheart armor items
CREATE TABLE armors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    base_major_threshold INTEGER NOT NULL CHECK (base_major_threshold > 0),
    base_severe_threshold INTEGER NOT NULL CHECK (base_severe_threshold > 0),
    base_score INTEGER NOT NULL,
    feature_id BIGINT REFERENCES features(id),
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    is_official BOOLEAN NOT NULL DEFAULT true,
    created_by_user_id BIGINT REFERENCES users(id),
    original_armor_id BIGINT REFERENCES armors(id),
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CHECK (base_severe_threshold >= base_major_threshold)
);

-- Indexes for common query patterns
CREATE INDEX idx_armors_expansion ON armors(expansion_id);
CREATE INDEX idx_armors_is_official ON armors(is_official);
CREATE INDEX idx_armors_created_by ON armors(created_by_user_id);
CREATE INDEX idx_armors_original ON armors(original_armor_id);

-- Partial index for active (non-deleted) armors
CREATE INDEX idx_armors_active ON armors(expansion_id, is_official) WHERE deleted_at IS NULL;
