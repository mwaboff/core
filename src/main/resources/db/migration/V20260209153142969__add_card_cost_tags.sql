-- Shared tag entity table for card cost/limitation tags
CREATE TABLE card_cost_tags (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_card_cost_tags_label UNIQUE (label),
    CONSTRAINT chk_card_cost_tags_category CHECK (category IN ('COST', 'LIMITATION', 'TIMING'))
);

-- Indexes for card_cost_tags
CREATE INDEX idx_card_cost_tags_label ON card_cost_tags(label);
CREATE INDEX idx_card_cost_tags_category ON card_cost_tags(category);
CREATE INDEX idx_card_cost_tags_deleted_at ON card_cost_tags(deleted_at);
CREATE INDEX idx_card_cost_tags_active ON card_cost_tags(category) WHERE deleted_at IS NULL;

-- Join table linking cards to cost tags
CREATE TABLE card_card_cost_tags (
    card_id BIGINT NOT NULL,
    card_cost_tag_id BIGINT NOT NULL,
    PRIMARY KEY (card_id, card_cost_tag_id),
    CONSTRAINT fk_card_cost_tags_card FOREIGN KEY (card_id)
        REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_cost_tags_tag FOREIGN KEY (card_cost_tag_id)
        REFERENCES card_cost_tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_card_card_cost_tags_tag ON card_card_cost_tags(card_cost_tag_id);
