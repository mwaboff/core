-- Migration: create_transformation_card_questions
-- Created: Sat Aug  1 01:35:28 PM EDT 2026
-- Description: Join table linking transformation cards to their character-creation questions.
-- Mirrors class_background_questions (V20260118164940167__create_class_relationships.sql).

CREATE TABLE transformation_card_questions (
    transformation_card_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (transformation_card_id, question_id),

    CONSTRAINT fk_transformation_card_questions_card FOREIGN KEY (transformation_card_id)
        REFERENCES transformation_cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_transformation_card_questions_question FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_transformation_card_questions_question ON transformation_card_questions(question_id);

