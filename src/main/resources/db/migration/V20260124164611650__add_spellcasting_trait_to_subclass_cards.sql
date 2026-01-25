-- Add spellcasting_trait column to subclass_cards table
ALTER TABLE subclass_cards
ADD COLUMN spellcasting_trait VARCHAR(20);

-- Add index on spellcasting_trait for query performance
CREATE INDEX idx_subclass_cards_spellcasting_trait ON subclass_cards(spellcasting_trait);
