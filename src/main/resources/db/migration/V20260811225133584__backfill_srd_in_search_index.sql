-- Migration: backfill_srd_in_search_index
-- Created: Tue Aug 11 10:51:33 PM EDT 2026
-- Description: Backfills search_index.srd for every row that already exists, now that
--              SearchFieldMapping populates the flag for new/updated rows.
--
-- Why this can't be left to a reindex: a reindex only rewrites rows whose entities happen
-- to get re-saved, so it would silently leave every already-indexed row behind with
-- srd = NULL. The search predicate added alongside this migration
-- (`si.is_official IS NOT TRUE OR si.srd = true`) treats NULL srd on an official row as
-- "not SRD", so every official row indexed before today would incorrectly vanish from
-- search for non-privileged users once srd-gating-enabled is flipped on. A migration
-- applies identically to qa and production on deploy with no manual step, closing that
-- gap for good. This is the same rationale, and the same failure shape, as the
-- COMMUNITY_CARD is_official gap repaired in
-- V20260731141505278__add_is_official_to_domains_and_classes.sql.
--
-- One statement per gated source table (17 tables, matching the ALTER list in
-- V20260811222234279__add_srd_and_expansion_access.sql). The four card-derived
-- search_index entity types (ANCESTRY_CARD, COMMUNITY_CARD, SUBCLASS_CARD, DOMAIN_CARD)
-- all back onto the single `cards` table (JOINED inheritance), so they are covered by one
-- statement with an IN clause, mirroring V20260730151632944__backfill_search_index_card_type.sql.
--
-- EXPANSION is not backfilled: `expansions` carries no srd column (see the note on
-- SearchFieldMapping#buildForExpansion) and its search_index rows correctly stay NULL.
--
-- Section 0 below repairs a second, distinct failure mode before the UPDATE-only backfill
-- runs: UPDATE only touches rows that already exist in search_index, but FeatureService,
-- QuestionService, and CardCostTagService each expose a findOrCreate(...) entry point whose
-- "create" branch calls the repository's save(...) directly and never publishes an
-- EntityChangeEvent -- so a Feature, Question, or CardCostTag minted only through
-- findOrCreate (e.g. inline while importing a card or item that references it) has never
-- been indexed at all, in any environment, since these types were added to search. This is
-- the exact bug V20260731141505278__add_is_official_to_domains_and_classes.sql repaired for
-- SUBCLASS_PATH's own findOrCreate (SubclassPathService now publishes on both its create and
-- update branches; verified while writing this migration). Feature/Question/CardCostTag were
-- not fixed at the time and still have the gap today. A reindex cannot repair this either --
-- SearchIndexService#reindexAll iterates the same repository these rows already live in
-- happily, so it would just re-confirm they are unindexed, not insert them. Fixing the
-- underlying event-publishing gap in those three services is out of scope for this
-- workstream (search_index only); this section only backfills the rows they already left
-- behind. New Feature/Question/CardCostTag rows created via findOrCreate after this deploy
-- will still go unindexed until that service-layer gap is closed separately.

-- ----------------------------------------------------------------------------
-- 0. Missing-row repair (INSERT, not UPDATE) for FEATURE, QUESTION, CARD_COST_TAG
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, feature_type, srd,
    created_at, last_modified_at
)
SELECT
    'FEATURE',
    f.id,
    f.name,
    setweight(to_tsvector('english', f.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(f.description, '')), 'B') ||
    setweight(to_tsvector('english', ''), 'C'),
    f.expansion_id,
    f.feature_type,
    f.srd,
    NOW(),
    NOW()
FROM features f
WHERE f.deleted_at IS NULL
  AND f.name IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM search_index si
      WHERE si.entity_type = 'FEATURE'
        AND si.entity_id = f.id
  );

INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, srd,
    created_at, last_modified_at
)
SELECT
    'QUESTION',
    q.id,
    q.question_text,
    setweight(to_tsvector('english', q.question_text), 'A') ||
    setweight(to_tsvector('english', ''), 'B') ||
    setweight(to_tsvector('english', ''), 'C'),
    q.expansion_id,
    q.srd,
    NOW(),
    NOW()
FROM questions q
WHERE q.deleted_at IS NULL
  AND q.question_text IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM search_index si
      WHERE si.entity_type = 'QUESTION'
        AND si.entity_id = q.id
  );

INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    cost_tag_category, srd,
    created_at, last_modified_at
)
SELECT
    'CARD_COST_TAG',
    cct.id,
    cct.label,
    setweight(to_tsvector('english', cct.label), 'A') ||
    setweight(to_tsvector('english', ''), 'B') ||
    setweight(to_tsvector('english', ''), 'C'),
    cct.category,
    cct.srd,
    NOW(),
    NOW()
FROM card_cost_tags cct
WHERE cct.deleted_at IS NULL
  AND cct.label IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM search_index si
      WHERE si.entity_type = 'CARD_COST_TAG'
        AND si.entity_id = cct.id
  );

-- ----------------------------------------------------------------------------
-- Cards (ANCESTRY_CARD, COMMUNITY_CARD, SUBCLASS_CARD, DOMAIN_CARD)
-- ----------------------------------------------------------------------------
UPDATE search_index si
SET srd = c.srd
FROM cards c
WHERE si.entity_type IN ('ANCESTRY_CARD', 'COMMUNITY_CARD', 'SUBCLASS_CARD', 'DOMAIN_CARD')
  AND si.entity_id = c.id;

-- ----------------------------------------------------------------------------
-- Items
-- ----------------------------------------------------------------------------
UPDATE search_index si
SET srd = w.srd
FROM weapons w
WHERE si.entity_type = 'WEAPON'
  AND si.entity_id = w.id;

UPDATE search_index si
SET srd = a.srd
FROM armors a
WHERE si.entity_type = 'ARMOR'
  AND si.entity_id = a.id;

UPDATE search_index si
SET srd = l.srd
FROM loot l
WHERE si.entity_type = 'LOOT'
  AND si.entity_id = l.id;

UPDATE search_index si
SET srd = ms.srd
FROM martial_stances ms
WHERE si.entity_type = 'MARTIAL_STANCE'
  AND si.entity_id = ms.id;

-- ----------------------------------------------------------------------------
-- Catalogue
-- ----------------------------------------------------------------------------
UPDATE search_index si
SET srd = d.srd
FROM domains d
WHERE si.entity_type = 'DOMAIN'
  AND si.entity_id = d.id;

UPDATE search_index si
SET srd = cl.srd
FROM classes cl
WHERE si.entity_type = 'CLASS'
  AND si.entity_id = cl.id;

UPDATE search_index si
SET srd = sp.srd
FROM subclass_paths sp
WHERE si.entity_type = 'SUBCLASS_PATH'
  AND si.entity_id = sp.id;

UPDATE search_index si
SET srd = tc.srd
FROM transformation_cards tc
WHERE si.entity_type = 'TRANSFORMATION_CARD'
  AND si.entity_id = tc.id;

UPDATE search_index si
SET srd = q.srd
FROM questions q
WHERE si.entity_type = 'QUESTION'
  AND si.entity_id = q.id;

UPDATE search_index si
SET srd = cct.srd
FROM card_cost_tags cct
WHERE si.entity_type = 'CARD_COST_TAG'
  AND si.entity_id = cct.id;

UPDATE search_index si
SET srd = f.srd
FROM features f
WHERE si.entity_type = 'FEATURE'
  AND si.entity_id = f.id;

-- ----------------------------------------------------------------------------
-- GM content
-- ----------------------------------------------------------------------------
UPDATE search_index si
SET srd = adv.srd
FROM adversaries adv
WHERE si.entity_type = 'ADVERSARY'
  AND si.entity_id = adv.id;

UPDATE search_index si
SET srd = bf.srd
FROM beastforms bf
WHERE si.entity_type = 'BEASTFORM'
  AND si.entity_id = bf.id;

UPDATE search_index si
SET srd = env.srd
FROM environments env
WHERE si.entity_type = 'ENVIRONMENT'
  AND si.entity_id = env.id;

UPDATE search_index si
SET srd = cond.srd
FROM conditions cond
WHERE si.entity_type = 'CONDITION'
  AND si.entity_id = cond.id;

UPDATE search_index si
SET srd = e.srd
FROM encounters e
WHERE si.entity_type = 'ENCOUNTER'
  AND si.entity_id = e.id;
