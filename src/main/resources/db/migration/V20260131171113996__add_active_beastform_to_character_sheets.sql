-- Add active beastform reference to character sheets
ALTER TABLE character_sheets
ADD COLUMN active_beastform_id BIGINT REFERENCES beastforms(id);

-- Index for the foreign key
CREATE INDEX idx_character_sheets_active_beastform_id ON character_sheets(active_beastform_id);
