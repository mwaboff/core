-- Migration: move_features_to_base_item
-- Created: Sat Feb 28 02:52:41 PM EST 2026
-- Move feature relationship from individual item tables to join tables
-- to support multiple features per item via BaseItem ManyToMany

-- Create weapon_features join table
CREATE TABLE weapon_features (
    weapon_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (weapon_id, feature_id),
    CONSTRAINT fk_weapon_features_weapon FOREIGN KEY (weapon_id) REFERENCES weapons(id),
    CONSTRAINT fk_weapon_features_feature FOREIGN KEY (feature_id) REFERENCES features(id)
);

CREATE INDEX idx_weapon_features_weapon_id ON weapon_features(weapon_id);
CREATE INDEX idx_weapon_features_feature_id ON weapon_features(feature_id);

-- Create armor_features join table
CREATE TABLE armor_features (
    armor_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (armor_id, feature_id),
    CONSTRAINT fk_armor_features_armor FOREIGN KEY (armor_id) REFERENCES armors(id),
    CONSTRAINT fk_armor_features_feature FOREIGN KEY (feature_id) REFERENCES features(id)
);

CREATE INDEX idx_armor_features_armor_id ON armor_features(armor_id);
CREATE INDEX idx_armor_features_feature_id ON armor_features(feature_id);

-- Create loot_features join table
CREATE TABLE loot_features (
    loot_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (loot_id, feature_id),
    CONSTRAINT fk_loot_features_loot FOREIGN KEY (loot_id) REFERENCES loot(id),
    CONSTRAINT fk_loot_features_feature FOREIGN KEY (feature_id) REFERENCES features(id)
);

CREATE INDEX idx_loot_features_loot_id ON loot_features(loot_id);
CREATE INDEX idx_loot_features_feature_id ON loot_features(feature_id);

-- Migrate existing weapon feature data
INSERT INTO weapon_features (weapon_id, feature_id)
SELECT id, feature_id FROM weapons WHERE feature_id IS NOT NULL;

-- Migrate existing armor feature data
INSERT INTO armor_features (armor_id, feature_id)
SELECT id, feature_id FROM armors WHERE feature_id IS NOT NULL;

-- Drop old feature_id columns
ALTER TABLE weapons DROP COLUMN feature_id;
ALTER TABLE armors DROP COLUMN feature_id;
