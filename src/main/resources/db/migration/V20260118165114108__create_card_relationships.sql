-- Migration: create_card_relationships
-- Description: Creates join tables for card many-to-many relationships

-- Card to Features relationship
CREATE TABLE card_features (
    card_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    PRIMARY KEY (card_id, feature_id),

    CONSTRAINT fk_card_features_card FOREIGN KEY (card_id)
        REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_features_feature FOREIGN KEY (feature_id)
        REFERENCES features(id) ON DELETE CASCADE
);

CREATE INDEX idx_card_features_feature ON card_features(feature_id);

-- Subclass Card to Domains relationship
CREATE TABLE subclass_domains (
    subclass_card_id BIGINT NOT NULL,
    domain_id BIGINT NOT NULL,
    PRIMARY KEY (subclass_card_id, domain_id),

    CONSTRAINT fk_subclass_domains_subclass FOREIGN KEY (subclass_card_id)
        REFERENCES subclass_cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_subclass_domains_domain FOREIGN KEY (domain_id)
        REFERENCES domains(id) ON DELETE CASCADE
);

CREATE INDEX idx_subclass_domains_domain ON subclass_domains(domain_id);
