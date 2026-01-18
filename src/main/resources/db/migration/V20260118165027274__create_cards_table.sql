-- Migration: create_cards_table
-- Description: Creates the base cards table for Daggerheart TTRPG cards (JOINED inheritance strategy)

CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    card_type VARCHAR(20) NOT NULL,
    expansion_id BIGINT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT true,
    background_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_cards_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cards_card_type CHECK (card_type IN ('ANCESTRY', 'COMMUNITY', 'SUBCLASS', 'DOMAIN'))
);

-- Indexes for common queries
CREATE INDEX idx_cards_name ON cards(name);
CREATE INDEX idx_cards_card_type ON cards(card_type);
CREATE INDEX idx_cards_expansion ON cards(expansion_id);
CREATE INDEX idx_cards_is_official ON cards(is_official);
CREATE INDEX idx_cards_deleted_at ON cards(deleted_at);
CREATE INDEX idx_cards_type_expansion ON cards(card_type, expansion_id);
CREATE INDEX idx_cards_active_official ON cards(is_official, expansion_id) WHERE deleted_at IS NULL;
