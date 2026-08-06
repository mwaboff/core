-- Migration: backfill_item_is_public_in_search_index
--
-- SearchFieldMapping.buildForWeapon/Armor/Loot never set isPublic, so search_index.is_public
-- has been NULL for every weapon, armor, and loot row since the index was introduced. The
-- access clause in SearchIndexRepository tests `si.is_public = true`, and NULL = true is
-- UNKNOWN rather than TRUE, so the public-visibility branch silently never fired for items.
-- Nothing was exposed that shouldn't have been -- the failure was closed, not open -- but a
-- moderator publishing a custom item would have found it unsearchable.
--
-- The mapping now populates the flag. This backfills the rows already indexed. Every existing
-- item is official and unpublished, so false is correct for all of them; official content is
-- visible through its own branch of the access clause regardless.

UPDATE search_index
   SET is_public = FALSE
 WHERE entity_type IN ('WEAPON', 'ARMOR', 'LOOT')
   AND is_public IS NULL;
