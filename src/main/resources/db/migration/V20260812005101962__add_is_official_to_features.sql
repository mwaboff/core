-- Migration: add_is_official_to_features
--
-- features never carried is_official, unlike Card/BaseItem and every standalone gated
-- entity -- V20260811222234279__add_srd_and_expansion_access.sql's own comment says so
-- explicitly, and shipped idx_features_srd_visibility on srd alone as a stopgap because
-- of it. The gap surfaced at boot ("column is_official does not exist") the moment
-- FeatureRepository's browse query tried to reference it.
--
-- A feature has no official/custom distinction of its own: it is only ever official or
-- custom by virtue of what it is attached to (a card, a class, an adversary, ...), via
-- one of eleven join tables. This migration adds the column and backfills it from those
-- parents, then re-derives is_official at create time going forward (FeatureService),
-- never from a request DTO.
--
-- is_official's DEFAULT false is, like srd before it, only reachable from raw SQL --
-- Feature carries no @DynamicInsert, so Hibernate always names the column in its INSERT
-- and the database default never applies to a JPA-created row; the create-time default
-- lives in FeatureService.
ALTER TABLE features ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT FALSE;

-- ----------------------------------------------------------------------------
-- Backfill, joining through every feature_id join table (grepped from the migration
-- history, not guessed):
--   card_features                  -> cards
--   class_hope_features            -> classes
--   class_class_features           -> classes
--   adversary_features             -> adversaries
--   beastform_features             -> beastforms
--   weapon_features                -> weapons
--   armor_features                 -> armors
--   loot_features                  -> loot
--   transformation_card_features   -> transformation_cards
--   environment_features           -> environments
--   martial_stance_features        -> martial_stances
-- A feature is official if ANY parent it is attached to is official (a feature is
-- shared/deduped across parents by FeatureRepository's find-or-create key, so it can in
-- principle be linked from more than one).
--
-- Cross-checked against the signal FeatureService's origin logic already relies on
-- elsewhere (see FeatureService.FeatureOrigin / mayClaimSourcebook): an official feature
-- names an expansion and records no author; a custom one does the reverse. Verified on
-- the local dev database before writing this migration -- of 1690 active features, the
-- join-derived set and the (expansion_id IS NOT NULL AND created_by_user_id IS NULL)
-- signal agree on all 1690 rows (1684 official / 6 custom, 0 disagreements). Had they
-- disagreed, this migration would need to stop and report the counts rather than pick a
-- side silently; that never came up here, so the join derivation below is used directly.
-- ----------------------------------------------------------------------------
UPDATE features f
SET is_official = true
WHERE f.id IN (
    SELECT cf.feature_id FROM card_features cf JOIN cards c ON cf.card_id = c.id WHERE c.is_official = true
    UNION
    SELECT chf.feature_id FROM class_hope_features chf JOIN classes cl ON chf.class_id = cl.id WHERE cl.is_official = true
    UNION
    SELECT ccf.feature_id FROM class_class_features ccf JOIN classes cl ON ccf.class_id = cl.id WHERE cl.is_official = true
    UNION
    SELECT af.feature_id FROM adversary_features af JOIN adversaries a ON af.adversary_id = a.id WHERE a.is_official = true
    UNION
    SELECT bf.feature_id FROM beastform_features bf JOIN beastforms b ON bf.beastform_id = b.id WHERE b.is_official = true
    UNION
    SELECT wf.feature_id FROM weapon_features wf JOIN weapons w ON wf.weapon_id = w.id WHERE w.is_official = true
    UNION
    SELECT arf.feature_id FROM armor_features arf JOIN armors ar ON arf.armor_id = ar.id WHERE ar.is_official = true
    UNION
    SELECT lf.feature_id FROM loot_features lf JOIN loot l ON lf.loot_id = l.id WHERE l.is_official = true
    UNION
    SELECT tcf.feature_id FROM transformation_card_features tcf
        JOIN transformation_cards tc ON tcf.transformation_card_id = tc.id WHERE tc.is_official = true
    UNION
    SELECT ef.feature_id FROM environment_features ef JOIN environments e ON ef.environment_id = e.id WHERE e.is_official = true
    UNION
    SELECT msf.feature_id FROM martial_stance_features msf
        JOIN martial_stances ms ON msf.martial_stance_id = ms.id WHERE ms.is_official = true
);

-- Replace Workstream A's srd-only stopgap index with the standard (is_official, srd)
-- shape, matching the idx_*_srd_visibility precedent in
-- V20260811222234279__add_srd_and_expansion_access.sql now that the column exists.
DROP INDEX idx_features_srd_visibility;
CREATE INDEX idx_features_srd_visibility ON features(is_official, srd) WHERE deleted_at IS NULL;
