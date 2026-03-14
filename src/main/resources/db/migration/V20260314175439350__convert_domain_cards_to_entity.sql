-- Migration: convert_domain_cards_to_entity
-- Created: Sat Mar 14 05:54:39 PM EDT 2026

ALTER TABLE character_sheet_domain_cards DROP CONSTRAINT character_sheet_domain_cards_pkey;
ALTER TABLE character_sheet_domain_cards ADD COLUMN id BIGSERIAL PRIMARY KEY;
ALTER TABLE character_sheet_domain_cards ADD COLUMN equipped BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE character_sheet_domain_cards ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_domain_cards ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE character_sheet_domain_cards ADD CONSTRAINT uq_cs_domain_card UNIQUE (character_sheet_id, domain_card_id);
