-- Migration: create_username_history_table
-- Created: Thu Apr 23 04:41:36 PM EDT 2026
-- Retains a record of each username change for admin visibility.

CREATE TABLE username_history (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    previous_username   VARCHAR(100) NOT NULL,
    new_username        VARCHAR(100) NOT NULL,
    changed_by_user_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    changed_at          TIMESTAMP NOT NULL DEFAULT now(),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    last_modified_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_username_history_user_changed ON username_history(user_id, changed_at DESC);
