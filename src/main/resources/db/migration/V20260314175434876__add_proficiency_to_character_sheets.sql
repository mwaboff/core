-- Migration: add_proficiency_to_character_sheets
-- Created: Sat Mar 14 05:54:34 PM EDT 2026

ALTER TABLE character_sheets ADD COLUMN proficiency INTEGER NOT NULL DEFAULT 1;
ALTER TABLE character_sheets ADD CONSTRAINT check_proficiency_positive CHECK (proficiency >= 1);
