-- Migration: add_feature_cost_tags_join_table
-- Created: Mon Feb  9 08:37:27 PM EST 2026

CREATE TABLE feature_card_cost_tags (
    feature_id BIGINT NOT NULL,
    card_cost_tag_id BIGINT NOT NULL,
    PRIMARY KEY (feature_id, card_cost_tag_id),
    CONSTRAINT fk_feature_cost_tags_feature FOREIGN KEY (feature_id) REFERENCES features(id),
    CONSTRAINT fk_feature_cost_tags_tag FOREIGN KEY (card_cost_tag_id) REFERENCES card_cost_tags(id)
);
