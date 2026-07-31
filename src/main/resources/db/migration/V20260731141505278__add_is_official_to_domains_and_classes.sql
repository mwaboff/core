-- ============================================================================
-- Migration: add_is_official_to_domains_and_classes
-- Description: Adds is_official to domains and classes, and repairs the
--              search_index rows that the missing column (and two related
--              indexing gaps) left unusable.
--
-- Why DEFAULT true: every domain and class that exists today is published
-- catalog content, and both create endpoints are ADMIN/OWNER-gated, so there
-- is no privilege concern in defaulting to official. The DEFAULT also
-- backfills all existing rows in a single ALTER (9 core + 1 Hope & Fear
-- domain, 9 core + 4 Hope & Fear classes).
--
-- NOTE: the DEFAULT only covers rows that already exist. These entities have
-- no @DynamicInsert, so Hibernate always names the column in its INSERT and
-- the database default never applies to new rows; the create-time default
-- lives in DomainService/ClassService.
--
-- Why the search_index backfills are here rather than a post-deploy reindex:
-- a reindex only rewrites rows whose entities happen to get re-saved, so it
-- would silently leave the existing rows behind. A migration applies
-- identically to qa and production on deploy with no manual step. The
-- subclass-path index gap repaired in section 3 below went unnoticed for
-- exactly that reason. Prior art:
-- V20260730151632944__backfill_search_index_card_type.sql.
-- ============================================================================

ALTER TABLE domains ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE classes ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT true;

-- ----------------------------------------------------------------------------
-- 1. DOMAIN / CLASS rows have never carried is_official, because the column did
--    not exist. A NULL fails the search predicate `si.is_official = :isOfficial`,
--    so every domain and class vanished from any search filtered to official
--    content.
-- ----------------------------------------------------------------------------
UPDATE search_index
SET is_official = true
WHERE entity_type IN ('DOMAIN', 'CLASS')
  AND is_official IS NULL;

-- ----------------------------------------------------------------------------
-- 2. COMMUNITY_CARD rows: SearchFieldMapping#buildForCommunityCard never set
--    is_official, unlike its ancestry and subclass siblings. The code fix only
--    affects future writes, so existing rows need this backfill.
-- ----------------------------------------------------------------------------
UPDATE search_index si
SET is_official = c.is_official
FROM cards c
WHERE si.entity_type = 'COMMUNITY_CARD'
  AND si.entity_id = c.id
  AND si.is_official IS NULL;

-- ----------------------------------------------------------------------------
-- 3. SUBCLASS_PATH rows: paths created implicitly by
--    SubclassPathService#findOrCreate published no EntityChangeEvent, so they
--    were never indexed at all. These rows are missing entirely rather than
--    merely NULL, so this is an INSERT.
--
--    The column set and search_vector expression below mirror
--    SearchIndexRepository#upsertSearchIndex driven by
--    SearchFieldMapping#buildForSubclassPath: name at weight A, no description
--    or feature text, plus expansion_id and associated_domain_id. Concatenating
--    the empty B and C vectors is a no-op, but is spelled out so this stays
--    provably identical to what a reindex would write.
--
--    associated_domain_id: the Java mapping takes the first element of an
--    unordered Set, so any single associated domain is faithful; MIN is used
--    here to keep the migration deterministic.
--
--    is_official is deliberately left NULL to match what the application
--    currently writes for this type — buildForSubclassPath does not set it.
--    Filling it in is out of scope for this change.
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, associated_domain_id,
    created_at, last_modified_at
)
SELECT
    'SUBCLASS_PATH',
    sp.id,
    sp.name,
    setweight(to_tsvector('english', sp.name), 'A') ||
    setweight(to_tsvector('english', ''), 'B') ||
    setweight(to_tsvector('english', ''), 'C'),
    sp.expansion_id,
    (SELECT MIN(spd.domain_id) FROM subclass_path_domains spd WHERE spd.subclass_path_id = sp.id),
    NOW(),
    NOW()
FROM subclass_paths sp
WHERE sp.deleted_at IS NULL
  AND sp.name IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM search_index si
      WHERE si.entity_type = 'SUBCLASS_PATH'
        AND si.entity_id = sp.id
  );
