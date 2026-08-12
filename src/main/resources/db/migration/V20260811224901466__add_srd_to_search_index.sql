-- Migration: add_srd_to_search_index
-- Created: Tue Aug 11 10:49:01 PM EDT 2026
-- Description: Adds the srd filter column to search_index, mirroring the srd column
--              Workstream A added to the 17 gated content tables (cards, weapons,
--              armors, loot, martial_stances, domains, classes, adversaries,
--              beastforms, environments, conditions, encounters,
--              transformation_cards, subclass_paths, questions, features,
--              card_cost_tags).
--
-- Nullable, matching is_official / is_public on this table: search_index is a
-- denormalized index shared by every entity type, and most types have no srd
-- concept at all (e.g. EXPANSION), so NULL means "not applicable" rather than
-- "not SRD". The access predicate added alongside this column treats NULL the
-- same as false, which is the fail-closed direction.
--
-- Schema only. SearchFieldMapping is updated separately so new/updated rows carry
-- the flag going forward, and a later, separately-timestamped migration backfills
-- srd for rows that already exist -- see the note in
-- V20260731141505278__add_is_official_to_domains_and_classes.sql on why a reindex
-- cannot be relied on to do that job: it only rewrites rows whose entities happen
-- to get re-saved, so it would silently leave every already-indexed row behind.

ALTER TABLE search_index ADD COLUMN srd BOOLEAN;

CREATE INDEX idx_search_index_srd ON search_index(srd) WHERE deleted_at IS NULL AND srd IS NOT NULL;
