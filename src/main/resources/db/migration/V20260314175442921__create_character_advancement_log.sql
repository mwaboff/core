-- Migration: create_character_advancement_log
-- Created: Sat Mar 14 05:54:42 PM EDT 2026

CREATE TABLE character_advancement_log (
    id BIGSERIAL PRIMARY KEY,
    character_sheet_id BIGINT NOT NULL,
    from_level INTEGER NOT NULL,
    to_level INTEGER NOT NULL,
    tier INTEGER NOT NULL,
    advancement_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_advancement_log_cs FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT check_adv_from_level CHECK (from_level >= 1 AND from_level <= 9),
    CONSTRAINT check_adv_to_level CHECK (to_level >= 2 AND to_level <= 10),
    CONSTRAINT check_adv_to_gt_from CHECK (to_level = from_level + 1),
    CONSTRAINT check_adv_tier CHECK (tier >= 2 AND tier <= 4)
);
CREATE INDEX idx_advancement_log_cs ON character_advancement_log(character_sheet_id);
CREATE INDEX idx_advancement_log_tier ON character_advancement_log(character_sheet_id, tier);
