-- ============================================================================
-- Migration: Add Subclass Paths
-- Description: Creates the subclass_paths table and migrates existing
--              subclass_cards data to use path-based associations instead of
--              storing associated_class_id and spellcasting_trait directly.
-- ============================================================================

-- 1. Create subclass_paths table
CREATE TABLE subclass_paths (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    associated_class_id BIGINT NOT NULL REFERENCES classes(id),
    spellcasting_trait VARCHAR(20),
    expansion_id BIGINT NOT NULL REFERENCES expansions(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- 2. Create subclass_path_domains join table
CREATE TABLE subclass_path_domains (
    subclass_path_id BIGINT NOT NULL REFERENCES subclass_paths(id),
    domain_id BIGINT NOT NULL REFERENCES domains(id),
    PRIMARY KEY (subclass_path_id, domain_id)
);

-- 3. Add case-insensitive unique index on (name, associated_class_id)
CREATE UNIQUE INDEX idx_subclass_paths_name_class
    ON subclass_paths (LOWER(name), associated_class_id);

-- 4. Migrate existing data: create SubclassPath records from existing SubclassCard data.
--    Groups by (associated_class_id, spellcasting_trait) to get unique paths.
--    Uses the first card name combined with spellcasting_trait as the path name.
--    If no subclass_cards exist, this INSERT produces no rows (safe for empty tables).
INSERT INTO subclass_paths (name, associated_class_id, spellcasting_trait, expansion_id, created_at, last_modified_at)
SELECT DISTINCT ON (sc.associated_class_id, COALESCE(sc.spellcasting_trait, ''))
    c.name || ' - ' || COALESCE(sc.spellcasting_trait, 'None') AS name,
    sc.associated_class_id,
    sc.spellcasting_trait,
    c2.expansion_id,
    NOW(),
    NOW()
FROM subclass_cards sc
JOIN cards c ON sc.id = c.id
JOIN classes c2 ON sc.associated_class_id = c2.id
ORDER BY sc.associated_class_id, COALESCE(sc.spellcasting_trait, ''), c.id;

-- 5. Add subclass_path_id column to subclass_cards (nullable initially)
ALTER TABLE subclass_cards ADD COLUMN subclass_path_id BIGINT REFERENCES subclass_paths(id);

-- 6. Populate subclass_path_id from migrated paths
UPDATE subclass_cards sc SET subclass_path_id = sp.id
FROM subclass_paths sp
WHERE sc.associated_class_id = sp.associated_class_id
AND (sc.spellcasting_trait = sp.spellcasting_trait
     OR (sc.spellcasting_trait IS NULL AND sp.spellcasting_trait IS NULL));

-- 7. Add NOT NULL constraint (safe because step 6 covers all combinations from step 4)
ALTER TABLE subclass_cards ALTER COLUMN subclass_path_id SET NOT NULL;

-- 8. Migrate subclass_domains data to subclass_path_domains
INSERT INTO subclass_path_domains (subclass_path_id, domain_id)
SELECT DISTINCT sp.id, sd.domain_id
FROM subclass_domains sd
JOIN subclass_cards sc ON sd.subclass_card_id = sc.id
JOIN subclass_paths sp ON sc.subclass_path_id = sp.id
ON CONFLICT DO NOTHING;

-- 9. Drop old columns from subclass_cards (now stored on subclass_paths)
ALTER TABLE subclass_cards DROP COLUMN associated_class_id;
ALTER TABLE subclass_cards DROP COLUMN spellcasting_trait;

-- 10. Drop the old subclass_domains join table (replaced by subclass_path_domains)
DROP TABLE subclass_domains;

-- 11. Add indexes for foreign key lookups
CREATE INDEX idx_subclass_paths_class ON subclass_paths (associated_class_id);
CREATE INDEX idx_subclass_cards_path ON subclass_cards (subclass_path_id);
