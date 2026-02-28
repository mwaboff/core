-- Migration: add_feature_modifiers
-- Created: Sat Feb 28 01:23:47 PM EST 2026

-- Feature modifiers table
CREATE TABLE feature_modifiers (
    id BIGSERIAL PRIMARY KEY,
    target VARCHAR(30) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    value INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD'
    )),
    CONSTRAINT chk_feature_modifiers_operation CHECK (operation IN ('ADD', 'SET', 'MULTIPLY'))
);

-- Unique index on active (non-deleted) modifiers to prevent duplicates
CREATE UNIQUE INDEX uq_feature_modifiers_active
    ON feature_modifiers(target, operation, value) WHERE deleted_at IS NULL;

-- Indexes for feature_modifiers
CREATE INDEX idx_feature_modifiers_target ON feature_modifiers(target);
CREATE INDEX idx_feature_modifiers_deleted_at ON feature_modifiers(deleted_at);
CREATE INDEX idx_feature_modifiers_active ON feature_modifiers(target) WHERE deleted_at IS NULL;

-- Join table linking features to feature modifiers
CREATE TABLE feature_feature_modifiers (
    feature_id BIGINT NOT NULL,
    feature_modifier_id BIGINT NOT NULL,
    PRIMARY KEY (feature_id, feature_modifier_id),
    CONSTRAINT fk_feature_feature_modifiers_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE,
    CONSTRAINT fk_feature_feature_modifiers_modifier FOREIGN KEY (feature_modifier_id)
        REFERENCES feature_modifiers(id) ON DELETE CASCADE
);

CREATE INDEX idx_feature_feature_modifiers_modifier ON feature_feature_modifiers(feature_modifier_id);
