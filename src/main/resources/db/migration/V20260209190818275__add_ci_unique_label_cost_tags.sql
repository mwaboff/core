-- Replace case-sensitive unique constraint with case-insensitive partial unique index.
-- The partial index only enforces uniqueness among active (non-deleted) tags,
-- allowing soft-deleted tags to coexist with active ones sharing the same label.
ALTER TABLE card_cost_tags DROP CONSTRAINT uq_card_cost_tags_label;
DROP INDEX idx_card_cost_tags_label;

CREATE UNIQUE INDEX uq_card_cost_tags_label_ci ON card_cost_tags (LOWER(label)) WHERE deleted_at IS NULL;
