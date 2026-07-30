-- Update the feature_type check constraint to include TRANSFORMATION, ENVIRONMENT, CAMPAIGN_FRAME
ALTER TABLE features DROP CONSTRAINT chk_features_feature_type;
ALTER TABLE features ADD CONSTRAINT chk_features_feature_type
    CHECK (feature_type IN ('HOPE', 'ANCESTRY', 'CLASS', 'COMMUNITY', 'DOMAIN', 'OTHER', 'SUBCLASS', 'ITEM', 'TRANSFORMATION', 'ENVIRONMENT', 'CAMPAIGN_FRAME'));

-- Update the question_type check constraint to include TRANSFORMATION, SESSION_ZERO
ALTER TABLE questions DROP CONSTRAINT chk_questions_question_type;
ALTER TABLE questions ADD CONSTRAINT chk_questions_question_type
    CHECK (question_type IN ('BACKGROUND', 'CONNECTION', 'TRANSFORMATION', 'SESSION_ZERO'));
