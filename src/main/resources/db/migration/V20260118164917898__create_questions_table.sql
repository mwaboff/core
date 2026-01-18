-- Migration: create_questions_table
-- Description: Creates the questions table for Daggerheart TTRPG character creation questions

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    expansion_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_questions_expansion FOREIGN KEY (expansion_id)
        REFERENCES expansions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_questions_question_type CHECK (question_type IN ('BACKGROUND', 'CONNECTION'))
);

-- Indexes for common queries
CREATE INDEX idx_questions_question_type ON questions(question_type);
CREATE INDEX idx_questions_expansion ON questions(expansion_id);
CREATE INDEX idx_questions_deleted_at ON questions(deleted_at);
CREATE INDEX idx_questions_active ON questions(expansion_id) WHERE deleted_at IS NULL;
