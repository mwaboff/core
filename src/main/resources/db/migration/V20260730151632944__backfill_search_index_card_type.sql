-- Migration: backfill_search_index_card_type
-- Description: Backfills search_index.card_type for existing card-derived rows.
--
-- Prior to this change, SearchFieldMapping#buildSearchIndexData never populated cardType,
-- so every search_index row indexed before this fix has card_type = NULL, making the
-- cardType search filter permanently unmatchable for pre-existing data. New/updated cards
-- are now indexed correctly by the fixed application code; this migration repairs the
-- rows that already exist.
--
-- card_type on the cards table is the JOINED-inheritance discriminator (ANCESTRY, COMMUNITY,
-- SUBCLASS, DOMAIN) and is joined here by entity_id for each of the four card-derived
-- search_index entity types.

UPDATE search_index si
SET card_type = c.card_type
FROM cards c
WHERE si.entity_id = c.id
  AND si.entity_type IN ('ANCESTRY_CARD', 'COMMUNITY_CARD', 'SUBCLASS_CARD', 'DOMAIN_CARD')
  AND si.card_type IS NULL;
