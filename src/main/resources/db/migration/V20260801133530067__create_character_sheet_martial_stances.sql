-- Migration: create_character_sheet_martial_stances
-- Created: Sat Aug  1 01:35:30 PM EDT 2026
-- Description: Join table tracking which martial stances a character sheet knows.

CREATE TABLE character_sheet_martial_stances (
    character_sheet_id BIGINT NOT NULL,
    martial_stance_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, martial_stance_id),

    CONSTRAINT fk_cs_martial_stances_sheet FOREIGN KEY (character_sheet_id)
        REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_martial_stances_stance FOREIGN KEY (martial_stance_id)
        REFERENCES martial_stances(id) ON DELETE CASCADE
);

CREATE INDEX idx_cs_martial_stances_stance ON character_sheet_martial_stances(martial_stance_id);

