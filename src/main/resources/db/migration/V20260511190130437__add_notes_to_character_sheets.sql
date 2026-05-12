-- Migration: add_notes_to_character_sheets
-- Created: Mon May 11 07:01:30 PM EDT 2026

ALTER TABLE character_sheets ADD COLUMN notes TEXT;
