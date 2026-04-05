-- Migration: add_mixed_ancestry_support
-- Adds is_mixed column to ancestry_cards for mixed-ancestry character support

ALTER TABLE ancestry_cards ADD COLUMN is_mixed BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_ancestry_cards_is_mixed ON ancestry_cards (is_mixed);
