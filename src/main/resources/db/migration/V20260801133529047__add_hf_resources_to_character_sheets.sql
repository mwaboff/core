-- Migration: add_hf_resources_to_character_sheets
-- Created: Sat Aug  1 01:35:29 PM EDT 2026
-- Description: Adds Hope & Fear resource fields (Focus, Favor, Combo Die) and transformation /
-- martial stance state to character sheets. Numeric resource columns follow the existing
-- hit_point_*/stress_*/hope_*/armor_* convention (NOT NULL DEFAULT). References are nullable.

ALTER TABLE character_sheets
    ADD COLUMN focus_marked INT NOT NULL DEFAULT 0,
    ADD COLUMN focus_max INT NOT NULL DEFAULT 6,
    ADD COLUMN favor INT NOT NULL DEFAULT 0,
    ADD COLUMN combo_die VARCHAR(10) NULL,
    ADD COLUMN transformation_card_id BIGINT NULL,
    ADD COLUMN transformation_tokens INT NULL,
    ADD COLUMN wolf_form_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN active_martial_stance_id BIGINT NULL;

ALTER TABLE character_sheets
    ADD CONSTRAINT fk_character_sheets_transformation_card FOREIGN KEY (transformation_card_id)
        REFERENCES transformation_cards(id),
    ADD CONSTRAINT fk_character_sheets_active_martial_stance FOREIGN KEY (active_martial_stance_id)
        REFERENCES martial_stances(id);

CREATE INDEX idx_character_sheets_transformation_card ON character_sheets(transformation_card_id);
CREATE INDEX idx_character_sheets_active_martial_stance ON character_sheets(active_martial_stance_id);

