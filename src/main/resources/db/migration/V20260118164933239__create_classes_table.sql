-- Migration: create_classes_table
-- Description: Creates the classes table for Daggerheart TTRPG character classes

CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    expansion_id BIGINT NOT NULL,
    starting_class_items TEXT,
    starting_evasion INT NOT NULL,
    starting_hit_points INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_classes_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT
);

-- Indexes for common queries
CREATE INDEX idx_classes_name ON classes(name);
CREATE INDEX idx_classes_expansion ON classes(expansion_id);
CREATE INDEX idx_classes_deleted_at ON classes(deleted_at);
CREATE INDEX idx_classes_active ON classes(expansion_id) WHERE deleted_at IS NULL;
