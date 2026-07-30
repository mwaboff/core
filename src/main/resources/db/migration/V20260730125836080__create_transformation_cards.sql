-- Migration: create_transformation_cards
-- Description: Creates the transformation_cards table for Hope & Fear transformation cards.
-- This is a standalone entity, modeled after classes/domains, NOT a Card subtype and NOT a
-- DomainCard row — transformation cards must never count against the 5-card domain-card
-- loadout cap, so they deliberately have no relationship to the cards/domain_cards tables.
-- No CHECK constraint on cards.card_type is touched or needed here.

CREATE TABLE transformation_cards (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    expansion_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_transformation_cards_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_transformation_cards_name ON transformation_cards(name);
CREATE INDEX idx_transformation_cards_expansion ON transformation_cards(expansion_id);
CREATE INDEX idx_transformation_cards_deleted_at ON transformation_cards(deleted_at);
CREATE INDEX idx_transformation_cards_active ON transformation_cards(expansion_id) WHERE deleted_at IS NULL;

-- Transformation card to Features relationship
CREATE TABLE transformation_card_features (
    transformation_card_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (transformation_card_id, feature_id),

    CONSTRAINT fk_transformation_card_features_card FOREIGN KEY (transformation_card_id)
        REFERENCES transformation_cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_transformation_card_features_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

CREATE INDEX idx_transformation_card_features_feature ON transformation_card_features(feature_id);
