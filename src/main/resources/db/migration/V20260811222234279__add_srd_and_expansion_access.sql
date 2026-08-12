-- Migration: add_srd_and_expansion_access
--
-- Foundation for SRD vs. paid-expansion content gating. Content freely licensed
-- under the Daggerheart SRD stays visible to every authenticated user; content that
-- only exists in a paid book (Hope & Fear, future expansions) is gated behind
-- ADMIN/OWNER role or a per-user "Access All Expansions" grant.
--
-- Backfill decision: every existing row gets srd = false. This is deliberate, not an
-- oversight -- a bulk SRD-flagging tool ships in the same release and is the only
-- thing that should ever turn srd on for imported content. Flagging nothing here
-- means the flag starts meaning exactly what it says (SRD-licensed) rather than
-- "whatever happened to be true the day this column was added."
--
-- srd's DEFAULT false is, like is_official before it (see
-- V20260731141505278__add_is_official_to_domains_and_classes.sql), only reachable
-- from raw SQL. None of the entities below carry @DynamicInsert, so Hibernate always
-- names every mapped column in its INSERT and the database default never applies to
-- a JPA-created row; the create-time default lives in each entity's service layer
-- (and, until the flagging tool runs, is simply false everywhere).
--
-- The kill switch: application.content.srd-gating-enabled defaults to false (see
-- application.yaml). Flipping gating on before the SRD subset is flagged would hide
-- the entire catalogue from every non-privileged user, so the rollout order is: ship
-- this migration inert, run the flagging tool, then flip the switch.
--
-- martial_stances is included in the srd backfill only because it extends BaseItem
-- and ddl-auto: validate requires backing storage on every subclass table, exactly
-- as V20260804225741429__add_visibility_to_items.sql notes for is_public -- custom
-- authoring is not open for stances and SRD gating does not meaningfully apply to
-- them either, but the column has to exist or Hibernate's schema validation fails
-- at startup.

ALTER TABLE cards               ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE weapons             ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE armors              ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE loot                ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE martial_stances     ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE domains             ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE classes             ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE adversaries         ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE beastforms          ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE environments        ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conditions          ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE encounters          ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transformation_cards ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE subclass_paths      ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE questions           ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE features            ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE card_cost_tags      ADD COLUMN srd BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing transformation_cards rows are official content already in production; the
-- DEFAULT true backfills them in the same ALTER, matching the is_official precedent
-- above for domains/classes.
ALTER TABLE transformation_cards ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT TRUE;

-- Per-user override: a plain USER granted this sees paid-expansion content without a
-- role change. Manually granted and revoked by an admin; see AdminActionType.
ALTER TABLE users ADD COLUMN access_all_expansions BOOLEAN NOT NULL DEFAULT FALSE;

-- Partial indexes on the browse hot paths only -- the six list/search endpoints that
-- take the heaviest traffic. Naming and shape follow the idx_*_visibility precedent
-- in V20260804225741429__add_visibility_to_items.sql.
CREATE INDEX idx_cards_srd_visibility       ON cards(is_official, srd)       WHERE deleted_at IS NULL;
CREATE INDEX idx_weapons_srd_visibility     ON weapons(is_official, srd)     WHERE deleted_at IS NULL;
CREATE INDEX idx_armors_srd_visibility      ON armors(is_official, srd)      WHERE deleted_at IS NULL;
CREATE INDEX idx_loot_srd_visibility        ON loot(is_official, srd)        WHERE deleted_at IS NULL;
CREATE INDEX idx_adversaries_srd_visibility ON adversaries(is_official, srd) WHERE deleted_at IS NULL;

-- features has no is_official column and never has -- unlike the five tables above,
-- Feature carries no official/custom distinction anywhere in the codebase today (no
-- column, no service-layer concept, no repository filter). The (is_official, srd)
-- shape used above does not apply here, so this indexes srd alone. Flagged for
-- whichever workstream wires SRD gating into FeatureRepository: ContentAccessService's
-- mayView(isOfficial, srd) treats a null isOfficial as false ("not official"), which
-- makes every Feature row unconditionally visible under the current mayView contract
-- until that gap is resolved.
CREATE INDEX idx_features_srd_visibility    ON features(srd) WHERE deleted_at IS NULL;
