-- Migration: widen_feature_type_and_retag_features
-- Created: Fri Jul 31 03:58:03 PM EDT 2026
--
-- Adds BEASTFORM, MARTIAL_STANCE and ADVERSARY to the feature_type check constraint and
-- retags the feature rows that were imported under the wrong type.
--
-- Counts below were measured against production and an identical local prod restore, both at
-- flyway version 77:
--   * 38 distinct feature rows are attached to beastforms and were all imported as DOMAIN, a
--     leftover from an abandoned domain-card payload shape. They pollute the ?featureType=DOMAIN
--     filter and the admin feature picker.
--   * 818 distinct feature rows are attached to adversaries and were all imported as OTHER. The
--     OTHER total is exactly 818, i.e. every OTHER feature is an adversary feature.
--   * Neither set shares a feature row with any other parent entity (verified against
--     card_features, class_class_features, class_hope_features, environment_features and
--     beastform_features), so both retags are surgical and cannot mutate a shared row.
--   * MARTIAL_STANCE has zero rows today (martial_stance_features is empty); the value is added
--     so the next Hope & Fear import cannot repeat the beastform mistake.
--   * feature_type is VARCHAR(20) and the longest new value, MARTIAL_STANCE, is 14 characters,
--     so no column widening is needed.
--
-- The final statement re-syncs search_index.feature_type so the search filters agree with the
-- retagged rows.

ALTER TABLE features DROP CONSTRAINT chk_features_feature_type;
ALTER TABLE features ADD CONSTRAINT chk_features_feature_type
    CHECK (feature_type IN ('HOPE', 'ANCESTRY', 'CLASS', 'COMMUNITY', 'DOMAIN', 'OTHER', 'SUBCLASS', 'ITEM', 'TRANSFORMATION', 'ENVIRONMENT', 'CAMPAIGN_FRAME', 'BEASTFORM', 'MARTIAL_STANCE', 'ADVERSARY'));

UPDATE features f
SET feature_type = 'BEASTFORM', last_modified_at = CURRENT_TIMESTAMP
WHERE f.feature_type = 'DOMAIN'
  AND EXISTS (SELECT 1 FROM beastform_features bf WHERE bf.feature_id = f.id);

UPDATE features f
SET feature_type = 'ADVERSARY', last_modified_at = CURRENT_TIMESTAMP
WHERE f.feature_type = 'OTHER'
  AND EXISTS (SELECT 1 FROM adversary_features af WHERE af.feature_id = f.id);

UPDATE search_index si
SET feature_type = f.feature_type
FROM features f
WHERE si.entity_type = 'FEATURE'
  AND si.entity_id = f.id
  AND si.feature_type IS DISTINCT FROM f.feature_type;
