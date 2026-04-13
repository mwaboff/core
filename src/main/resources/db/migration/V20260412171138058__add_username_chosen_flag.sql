-- Migration: add_username_chosen_flag
-- Created: Sun Apr 12 05:11:38 PM EDT 2026

ALTER TABLE users ADD COLUMN username_chosen BOOLEAN NOT NULL DEFAULT false;

