-- Migration: add_transformation_access_to_character_sheets
-- Created: Sat Aug  1 05:45:56 PM EDT 2026
-- Description: Adds the GM-controlled gate for the character sheet transformation panel.
-- Transformations are granted by a Game Master, so the column defaults to FALSE for every
-- existing and future sheet until a GM explicitly enables it.

ALTER TABLE character_sheets
    ADD COLUMN transformation_enabled BOOLEAN NOT NULL DEFAULT FALSE;
