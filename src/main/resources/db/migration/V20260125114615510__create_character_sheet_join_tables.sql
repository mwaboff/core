-- Migration: Create character sheet join tables
-- Description: Creates many-to-many relationship tables for character sheets

-- Join table for character sheets and community cards
CREATE TABLE character_sheet_communities (
    character_sheet_id BIGINT NOT NULL,
    community_card_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, community_card_id),
    CONSTRAINT fk_cs_communities_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_communities_community_card FOREIGN KEY (community_card_id) REFERENCES community_cards(id) ON DELETE CASCADE
);

-- Join table for character sheets and ancestry cards
CREATE TABLE character_sheet_ancestries (
    character_sheet_id BIGINT NOT NULL,
    ancestry_card_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, ancestry_card_id),
    CONSTRAINT fk_cs_ancestries_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_ancestries_ancestry_card FOREIGN KEY (ancestry_card_id) REFERENCES ancestry_cards(id) ON DELETE CASCADE
);

-- Join table for character sheets and subclass cards
CREATE TABLE character_sheet_subclasses (
    character_sheet_id BIGINT NOT NULL,
    subclass_card_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, subclass_card_id),
    CONSTRAINT fk_cs_subclasses_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_subclasses_subclass_card FOREIGN KEY (subclass_card_id) REFERENCES subclass_cards(id) ON DELETE CASCADE
);

-- Join table for character sheet weapon inventory
CREATE TABLE character_sheet_inventory_weapons (
    character_sheet_id BIGINT NOT NULL,
    weapon_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, weapon_id),
    CONSTRAINT fk_cs_inventory_weapons_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_inventory_weapons_weapon FOREIGN KEY (weapon_id) REFERENCES weapons(id) ON DELETE CASCADE
);

-- Join table for character sheet armor inventory
CREATE TABLE character_sheet_inventory_armors (
    character_sheet_id BIGINT NOT NULL,
    armor_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, armor_id),
    CONSTRAINT fk_cs_inventory_armors_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_inventory_armors_armor FOREIGN KEY (armor_id) REFERENCES armors(id) ON DELETE CASCADE
);

-- Join table for character sheet loot inventory
CREATE TABLE character_sheet_inventory_items (
    character_sheet_id BIGINT NOT NULL,
    loot_id BIGINT NOT NULL,
    PRIMARY KEY (character_sheet_id, loot_id),
    CONSTRAINT fk_cs_inventory_items_character_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_inventory_items_loot FOREIGN KEY (loot_id) REFERENCES loot(id) ON DELETE CASCADE
);

-- Indexes for join table queries
CREATE INDEX idx_cs_communities_character_sheet ON character_sheet_communities(character_sheet_id);
CREATE INDEX idx_cs_communities_community_card ON character_sheet_communities(community_card_id);
CREATE INDEX idx_cs_ancestries_character_sheet ON character_sheet_ancestries(character_sheet_id);
CREATE INDEX idx_cs_ancestries_ancestry_card ON character_sheet_ancestries(ancestry_card_id);
CREATE INDEX idx_cs_subclasses_character_sheet ON character_sheet_subclasses(character_sheet_id);
CREATE INDEX idx_cs_subclasses_subclass_card ON character_sheet_subclasses(subclass_card_id);
CREATE INDEX idx_cs_inventory_weapons_character_sheet ON character_sheet_inventory_weapons(character_sheet_id);
CREATE INDEX idx_cs_inventory_weapons_weapon ON character_sheet_inventory_weapons(weapon_id);
CREATE INDEX idx_cs_inventory_armors_character_sheet ON character_sheet_inventory_armors(character_sheet_id);
CREATE INDEX idx_cs_inventory_armors_armor ON character_sheet_inventory_armors(armor_id);
CREATE INDEX idx_cs_inventory_items_character_sheet ON character_sheet_inventory_items(character_sheet_id);
CREATE INDEX idx_cs_inventory_items_loot ON character_sheet_inventory_items(loot_id);
