-- Migration: add_fear_and_gm_notes_to_campaigns
-- Created: Fri Jul 31 05:14:58 PM EDT 2026
--
-- Adds the persisted GM Screen state to campaigns:
--   fear     - shared, table-visible Fear counter (range 0-12 enforced in the application layer)
--   gm_notes - GM-only free-text prep notes, never exposed to players

ALTER TABLE campaigns ADD COLUMN fear INT NOT NULL DEFAULT 0;
ALTER TABLE campaigns ADD COLUMN gm_notes TEXT;
