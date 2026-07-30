-- Migration: add_adversary_evolves_into_fk
-- Description: Adds a nullable self-referential FK on adversaries so an evolution pair
-- (one adversary transforming into another on a trigger) can be recorded structurally.
-- Bulk import creates adversaries in arbitrary order, so this must tolerate being set
-- after both rows already exist (via a later update), like original_adversary_id does.

ALTER TABLE adversaries ADD COLUMN evolves_into_adversary_id BIGINT;

-- ON DELETE SET NULL, matching fk_adversary_original: this repo soft-deletes adversaries
-- (deleted_at), so the row referenced by this FK is essentially never hard-deleted in
-- normal operation. SET NULL is the safe fallback if a hard delete ever does happen,
-- rather than CASCADE (which would delete an unrelated adversary) or RESTRICT (which
-- would block deleting the evolved-into adversary at all).
ALTER TABLE adversaries ADD CONSTRAINT fk_adversary_evolves_into FOREIGN KEY (evolves_into_adversary_id)
    REFERENCES adversaries(id) ON DELETE SET NULL;

CREATE INDEX idx_adversaries_evolves_into ON adversaries(evolves_into_adversary_id);
