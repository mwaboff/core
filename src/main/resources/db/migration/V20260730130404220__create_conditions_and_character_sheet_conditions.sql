-- Migration: create_conditions_and_character_sheet_conditions
-- Created: HF-18 — Conditions catalogue
--
-- Two tables:
--   conditions: the catalogue (e.g. Restrained, Vulnerable, Drained, Hexed, Chained, Ignited).
--   character_sheet_conditions: per-character instances of a condition, each carrying its own
--   `magnitude` snapshot value for conditions that stack (e.g. multiple stacks of Ignited).

CREATE TABLE conditions (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(200) NOT NULL,
    description TEXT,

    expansion_id BIGINT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_condition_expansion FOREIGN KEY (expansion_id) REFERENCES expansions(id),
    CONSTRAINT fk_condition_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_conditions_expansion_id ON conditions(expansion_id);
CREATE INDEX idx_conditions_is_official ON conditions(is_official);
CREATE INDEX idx_conditions_deleted_at ON conditions(deleted_at);

CREATE TABLE character_sheet_conditions (
    id BIGSERIAL PRIMARY KEY,

    character_sheet_id BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    magnitude INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_character_sheet_condition_sheet FOREIGN KEY (character_sheet_id) REFERENCES character_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_character_sheet_condition_condition FOREIGN KEY (condition_id) REFERENCES conditions(id) ON DELETE CASCADE
);

CREATE INDEX idx_character_sheet_conditions_sheet_id ON character_sheet_conditions(character_sheet_id);
CREATE INDEX idx_character_sheet_conditions_condition_id ON character_sheet_conditions(condition_id);
