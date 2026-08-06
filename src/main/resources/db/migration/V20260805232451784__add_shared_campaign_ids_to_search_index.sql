-- Migration: add_shared_campaign_ids_to_search_index
--
-- Campaign-shared custom items were invisible to global search. The access clause in
-- SearchIndexRepository admits official, public, own, and unowned rows -- but an item that
-- is none of those and is only visible because its author tagged it to a campaign the
-- caller is involved in had no branch to match. The item list endpoints already honour
-- those tags (weapon_campaigns / armor_campaigns / loot_campaigns), so the same sword was
-- browsable but not searchable.
--
-- Denormalising the tags onto the index row rather than joining the four per-type join
-- tables into the search query: search_index is deliberately type-agnostic, and a UNION of
-- four joins would have to be re-edited every time a new item type is added. The array is
-- rewritten on every reindex, and item campaign tags are a property of the item itself, so
-- the existing UPDATED change event already keeps it current.
--
-- The GIN index backs the `&&` overlap operator the access clause uses.

ALTER TABLE search_index ADD COLUMN shared_campaign_ids BIGINT[];

CREATE INDEX idx_search_index_shared_campaigns
    ON search_index USING GIN(shared_campaign_ids)
    WHERE deleted_at IS NULL;

-- ============================================================================
-- Backfill from the per-type join tables
-- ============================================================================
-- Rows with no tags are left NULL. NULL && '{...}' is UNKNOWN, which the access clause
-- treats the same as an empty array: no match. Re-indexing overwrites NULL with '{}'.

UPDATE search_index si
   SET shared_campaign_ids = tags.campaign_ids
  FROM (
        SELECT 'WEAPON' AS entity_type, weapon_id AS entity_id,
               array_agg(campaign_id ORDER BY campaign_id) AS campaign_ids
          FROM weapon_campaigns
         GROUP BY weapon_id
         UNION ALL
        SELECT 'ARMOR', armor_id, array_agg(campaign_id ORDER BY campaign_id)
          FROM armor_campaigns
         GROUP BY armor_id
         UNION ALL
        SELECT 'LOOT', loot_id, array_agg(campaign_id ORDER BY campaign_id)
          FROM loot_campaigns
         GROUP BY loot_id
         UNION ALL
        SELECT 'MARTIAL_STANCE', martial_stance_id, array_agg(campaign_id ORDER BY campaign_id)
          FROM martial_stance_campaigns
         GROUP BY martial_stance_id
       ) tags
 WHERE si.entity_type = tags.entity_type
   AND si.entity_id = tags.entity_id;

-- ============================================================================
-- Backfill is_public for martial stances
-- ============================================================================
-- Same defect V20260805094107346 fixed for weapons, armor, and loot: the mapping never set
-- the flag, so is_public stayed NULL and `si.is_public = true` never fired. Martial stances
-- were missed there. Every existing stance is official and unpublished, so false is correct;
-- official content is visible through its own branch regardless.

UPDATE search_index
   SET is_public = FALSE
 WHERE entity_type = 'MARTIAL_STANCE'
   AND is_public IS NULL;
