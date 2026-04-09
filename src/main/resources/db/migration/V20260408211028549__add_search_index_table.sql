-- Migration: add_search_index_table
-- Created: Wed Apr  8 09:10:28 PM EDT 2026
-- Description: Creates the search_index table for full-text search across all Daggerheart entities.
--              Populates the index with existing data for all 17 entity types.

-- ============================================================================
-- 1. Create search_index table
-- ============================================================================
CREATE TABLE search_index (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    search_vector TSVECTOR NOT NULL,
    tier INTEGER,
    expansion_id BIGINT,
    is_official BOOLEAN,
    is_public BOOLEAN,
    created_by_user_id BIGINT,
    card_type VARCHAR(50),
    feature_type VARCHAR(50),
    adversary_type VARCHAR(50),
    domain_card_type VARCHAR(50),
    associated_domain_id BIGINT,
    trait VARCHAR(50),
    range VARCHAR(50),
    burden VARCHAR(50),
    is_primary BOOLEAN,
    damage_type VARCHAR(50),
    is_consumable BOOLEAN,
    is_mixed BOOLEAN,
    subclass_level VARCHAR(50),
    cost_tag_category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- ============================================================================
-- 2. Create indexes
-- ============================================================================
CREATE INDEX idx_search_index_vector ON search_index USING GIN(search_vector);
CREATE UNIQUE INDEX idx_search_index_entity ON search_index(entity_type, entity_id);
CREATE INDEX idx_search_index_type ON search_index(entity_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_search_index_tier ON search_index(tier) WHERE deleted_at IS NULL AND tier IS NOT NULL;
CREATE INDEX idx_search_index_expansion ON search_index(expansion_id) WHERE deleted_at IS NULL AND expansion_id IS NOT NULL;
CREATE INDEX idx_search_index_official ON search_index(is_official) WHERE deleted_at IS NULL AND is_official IS NOT NULL;
CREATE INDEX idx_search_index_public ON search_index(is_public) WHERE deleted_at IS NULL AND is_public IS NOT NULL;
CREATE INDEX idx_search_index_creator ON search_index(created_by_user_id) WHERE deleted_at IS NULL AND created_by_user_id IS NOT NULL;
CREATE INDEX idx_search_index_active ON search_index(entity_type, expansion_id) WHERE deleted_at IS NULL;

-- ============================================================================
-- 3. Populate search_index with existing data
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 DOMAIN
--     Table: domains
--     Fields: name (A), description (B)
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id,
    deleted_at
)
SELECT
    'DOMAIN',
    d.id,
    d.name,
    setweight(to_tsvector('english', d.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(d.description, '')), 'B'),
    d.expansion_id,
    d.deleted_at
FROM domains d
WHERE d.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.2 CLASS
--     Table: classes
--     Fields: name (A), description + starting_class_items (B)
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id,
    deleted_at
)
SELECT
    'CLASS',
    cl.id,
    cl.name,
    setweight(to_tsvector('english', cl.name), 'A') ||
    setweight(to_tsvector('english',
        COALESCE(cl.description, '') || ' ' || COALESCE(cl.starting_class_items, '')
    ), 'B'),
    cl.expansion_id,
    cl.deleted_at
FROM classes cl
WHERE cl.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.3 FEATURE
--     Table: features
--     Fields: name (A), description (B), feature_type filter
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id,
    feature_type,
    deleted_at
)
SELECT
    'FEATURE',
    f.id,
    f.name,
    setweight(to_tsvector('english', f.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(f.description, '')), 'B'),
    f.expansion_id,
    f.feature_type,
    f.deleted_at
FROM features f
WHERE f.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.4 ANCESTRY_CARD
--     Tables: cards JOIN ancestry_cards
--     Fields: name (A), description (B), features (C), card_type, is_official, is_mixed
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, card_type, is_official, is_mixed,
    deleted_at
)
SELECT
    'ANCESTRY_CARD',
    c.id,
    c.name,
    setweight(to_tsvector('english', c.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(c.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM card_features cf
         JOIN features f ON cf.feature_id = f.id
         WHERE cf.card_id = c.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    c.expansion_id,
    c.card_type,
    c.is_official,
    ac.is_mixed,
    c.deleted_at
FROM cards c
JOIN ancestry_cards ac ON c.id = ac.id
WHERE c.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.5 COMMUNITY_CARD
--     Tables: cards JOIN community_cards
--     Fields: name (A), description (B), features (C), card_type, is_official
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, card_type, is_official,
    deleted_at
)
SELECT
    'COMMUNITY_CARD',
    c.id,
    c.name,
    setweight(to_tsvector('english', c.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(c.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM card_features cf
         JOIN features f ON cf.feature_id = f.id
         WHERE cf.card_id = c.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    c.expansion_id,
    c.card_type,
    c.is_official,
    c.deleted_at
FROM cards c
JOIN community_cards cc ON c.id = cc.id
WHERE c.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.6 SUBCLASS_CARD
--     Tables: cards JOIN subclass_cards
--     Fields: name (A), description (B), features (C), card_type, is_official, subclass_level
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, card_type, is_official, subclass_level,
    deleted_at
)
SELECT
    'SUBCLASS_CARD',
    c.id,
    c.name,
    setweight(to_tsvector('english', c.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(c.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM card_features cf
         JOIN features f ON cf.feature_id = f.id
         WHERE cf.card_id = c.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    c.expansion_id,
    c.card_type,
    c.is_official,
    sc.level,
    c.deleted_at
FROM cards c
JOIN subclass_cards sc ON c.id = sc.id
WHERE c.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.7 DOMAIN_CARD
--     Tables: cards JOIN domain_cards
--     Fields: name (A), description (B), features (C),
--             card_type, is_official, tier (via domain_cards.level), domain_card_type, associated_domain_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, card_type, is_official, tier, domain_card_type, associated_domain_id,
    deleted_at
)
SELECT
    'DOMAIN_CARD',
    c.id,
    c.name,
    setweight(to_tsvector('english', c.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(c.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM card_features cf
         JOIN features f ON cf.feature_id = f.id
         WHERE cf.card_id = c.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    c.expansion_id,
    c.card_type,
    c.is_official,
    dc.level,
    dc.domain_card_type,
    dc.associated_domain_id,
    c.deleted_at
FROM cards c
JOIN domain_cards dc ON c.id = dc.id
WHERE c.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.8 WEAPON
--     Table: weapons (with weapon_features join table)
--     Fields: name (A), features (C), is_official, is_public, created_by_user_id,
--             tier, expansion_id, trait, range, burden, is_primary, damage_type
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, is_official, created_by_user_id,
    tier, trait, range, burden, is_primary, damage_type,
    deleted_at
)
SELECT
    'WEAPON',
    w.id,
    w.name,
    setweight(to_tsvector('english', w.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM weapon_features wf
         JOIN features f ON wf.feature_id = f.id
         WHERE wf.weapon_id = w.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    w.expansion_id,
    w.is_official,
    w.created_by_user_id,
    w.tier,
    w.trait,
    w.range,
    w.burden,
    w.is_primary,
    w.damage_type,
    w.deleted_at
FROM weapons w
WHERE w.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.9 ARMOR
--     Table: armors (with armor_features join table)
--     Fields: name (A), features (C), is_official, created_by_user_id, tier, expansion_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, is_official, created_by_user_id,
    tier,
    deleted_at
)
SELECT
    'ARMOR',
    a.id,
    a.name,
    setweight(to_tsvector('english', a.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM armor_features af
         JOIN features f ON af.feature_id = f.id
         WHERE af.armor_id = a.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    a.expansion_id,
    a.is_official,
    a.created_by_user_id,
    a.tier,
    a.deleted_at
FROM armors a
WHERE a.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.10 LOOT
--      Table: loot (with loot_features join table)
--      Fields: name (A), description (B), features (C),
--              is_official, created_by_user_id, tier, expansion_id, is_consumable
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, is_official, created_by_user_id,
    tier, is_consumable,
    deleted_at
)
SELECT
    'LOOT',
    l.id,
    l.name,
    setweight(to_tsvector('english', l.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(l.description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM loot_features lf
         JOIN features f ON lf.feature_id = f.id
         WHERE lf.loot_id = l.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    l.expansion_id,
    l.is_official,
    l.created_by_user_id,
    l.tier,
    l.is_consumable,
    l.deleted_at
FROM loot l
WHERE l.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.11 ADVERSARY
--      Table: adversaries (with adversary_features join table)
--      Fields: name (A), description + motives_and_tactics + weapon_name (B), features (C),
--              is_official, is_public, tier, expansion_id, adversary_type
--      Note: creator_id used for created_by_user_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, is_official, is_public, created_by_user_id,
    tier, adversary_type,
    deleted_at
)
SELECT
    'ADVERSARY',
    a.id,
    a.name,
    setweight(to_tsvector('english', a.name), 'A') ||
    setweight(to_tsvector('english',
        COALESCE(a.description, '') || ' ' ||
        COALESCE(a.motives_and_tactics, '') || ' ' ||
        COALESCE(a.weapon_name, '')
    ), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM adversary_features af
         JOIN features f ON af.feature_id = f.id
         WHERE af.adversary_id = a.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    a.expansion_id,
    a.is_official,
    a.is_public,
    a.creator_id,
    a.tier,
    a.adversary_type,
    a.deleted_at
FROM adversaries a
WHERE a.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.12 BEASTFORM
--      Table: beastforms (with beastform_features join table)
--      Fields: name (A), example + advantages (B), features (C),
--              is_official, is_public, expansion_id
--      Note: creator_id used for created_by_user_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id, is_official, is_public, created_by_user_id,
    deleted_at
)
SELECT
    'BEASTFORM',
    b.id,
    b.name,
    setweight(to_tsvector('english', b.name), 'A') ||
    setweight(to_tsvector('english',
        COALESCE(b.example, '') || ' ' || COALESCE(b.advantages, '')
    ), 'B') ||
    setweight(to_tsvector('english', COALESCE(
        (SELECT string_agg(COALESCE(f.name, '') || ' ' || COALESCE(f.description, ''), ' ')
         FROM beastform_features bf
         JOIN features f ON bf.feature_id = f.id
         WHERE bf.beastform_id = b.id AND f.deleted_at IS NULL),
        ''
    )), 'C'),
    b.expansion_id,
    b.is_official,
    b.is_public,
    b.creator_id,
    b.deleted_at
FROM beastforms b
WHERE b.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.13 ENCOUNTER
--      Table: encounters
--      Fields: name (A), description (B),
--              is_official, is_public, tier
--      Note: creator_id used for created_by_user_id. No expansion_id on encounters.
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    is_official, is_public, created_by_user_id,
    tier,
    deleted_at
)
SELECT
    'ENCOUNTER',
    e.id,
    e.name,
    setweight(to_tsvector('english', e.name), 'A') ||
    setweight(to_tsvector('english', COALESCE(e.description, '')), 'B'),
    e.is_official,
    e.is_public,
    e.creator_id,
    e.tier,
    e.deleted_at
FROM encounters e
WHERE e.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.14 EXPANSION
--      Table: expansions
--      Fields: name (A). No description, no soft delete filter (deleted_at used for filter)
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    deleted_at
)
SELECT
    'EXPANSION',
    ex.id,
    ex.name,
    setweight(to_tsvector('english', ex.name), 'A'),
    ex.deleted_at
FROM expansions ex
WHERE ex.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.15 SUBCLASS_PATH
--      Table: subclass_paths
--      Fields: name (A), expansion_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id,
    deleted_at
)
SELECT
    'SUBCLASS_PATH',
    sp.id,
    sp.name,
    setweight(to_tsvector('english', sp.name), 'A'),
    sp.expansion_id,
    sp.deleted_at
FROM subclass_paths sp
WHERE sp.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.16 QUESTION
--      Table: questions
--      Fields: question_text (A), expansion_id
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    expansion_id,
    deleted_at
)
SELECT
    'QUESTION',
    q.id,
    q.question_text,
    setweight(to_tsvector('english', q.question_text), 'A'),
    q.expansion_id,
    q.deleted_at
FROM questions q
WHERE q.deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- 3.17 CARD_COST_TAG
--      Table: card_cost_tags
--      Fields: label (A), cost_tag_category filter
-- ----------------------------------------------------------------------------
INSERT INTO search_index (
    entity_type, entity_id, name, search_vector,
    cost_tag_category,
    deleted_at
)
SELECT
    'CARD_COST_TAG',
    cct.id,
    cct.label,
    setweight(to_tsvector('english', cct.label), 'A'),
    cct.category,
    cct.deleted_at
FROM card_cost_tags cct
WHERE cct.deleted_at IS NULL;
