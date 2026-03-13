-- Migration: create_character_sheet_domain_cards_join_table
-- Created: Fri Mar 13 03:54:04 PM EDT 2026

CREATE TABLE character_sheet_domain_cards (
    character_sheet_id BIGINT NOT NULL,
    domain_card_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, domain_card_id),
    CONSTRAINT fk_cs_domain_cards_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_domain_cards_domain_card FOREIGN KEY (domain_card_id) REFERENCES domain_cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_cs_domain_cards_character_sheet ON character_sheet_domain_cards(character_sheet_id);
CREATE INDEX idx_cs_domain_cards_domain_card ON character_sheet_domain_cards(domain_card_id);
