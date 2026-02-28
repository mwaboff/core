-- Update the feature_type check constraint to include ITEM
ALTER TABLE features DROP CONSTRAINT chk_features_feature_type;
ALTER TABLE features ADD CONSTRAINT chk_features_feature_type
    CHECK (feature_type IN ('HOPE', 'ANCESTRY', 'CLASS', 'COMMUNITY', 'DOMAIN', 'OTHER', 'SUBCLASS', 'ITEM'));
