-- Migration: create_card_subtypes
-- Description: Creates subtype tables for card inheritance (ancestry, community, subclass, domain)

-- Ancestry cards table (no additional fields beyond base Card)
CREATE TABLE ancestry_cards (
    id BIGINT PRIMARY KEY,

    CONSTRAINT fk_ancestry_cards_card FOREIGN KEY (id)
        REFERENCES cards(id) ON DELETE CASCADE
);

-- Community cards table (no additional fields beyond base Card)
CREATE TABLE community_cards (
    id BIGINT PRIMARY KEY,

    CONSTRAINT fk_community_cards_card FOREIGN KEY (id)
        REFERENCES cards(id) ON DELETE CASCADE
);

-- Subclass cards table (with class association and level)
CREATE TABLE subclass_cards (
    id BIGINT PRIMARY KEY,
    associated_class_id BIGINT NOT NULL,
    level VARCHAR(20) NOT NULL,

    CONSTRAINT fk_subclass_cards_card FOREIGN KEY (id)
        REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_subclass_cards_class FOREIGN KEY (associated_class_id)
        REFERENCES classes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_subclass_cards_level CHECK (level IN ('FOUNDATION', 'SPECIALIZATION', 'MASTERY'))
);

CREATE INDEX idx_subclass_cards_class ON subclass_cards(associated_class_id);
CREATE INDEX idx_subclass_cards_level ON subclass_cards(level);

-- Domain cards table (with domain association, level, recall cost, and type)
CREATE TABLE domain_cards (
    id BIGINT PRIMARY KEY,
    associated_domain_id BIGINT NOT NULL,
    level INTEGER NOT NULL,
    recall_cost INTEGER NOT NULL,
    domain_card_type VARCHAR(20) NOT NULL,

    CONSTRAINT fk_domain_cards_card FOREIGN KEY (id)
        REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_domain_cards_domain FOREIGN KEY (associated_domain_id)
        REFERENCES domains(id) ON DELETE RESTRICT,
    CONSTRAINT chk_domain_cards_recall_cost CHECK (recall_cost >= 0),
    CONSTRAINT chk_domain_cards_type CHECK (domain_card_type IN ('SPELL', 'GRIMOIRE', 'ABILITY', 'TRANSFORMATION', 'WILD'))
);

CREATE INDEX idx_domain_cards_domain ON domain_cards(associated_domain_id);
CREATE INDEX idx_domain_cards_level ON domain_cards(level);
CREATE INDEX idx_domain_cards_type ON domain_cards(domain_card_type);
CREATE INDEX idx_domain_cards_domain_level ON domain_cards(associated_domain_id, level);
