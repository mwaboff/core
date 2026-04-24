-- Migration: create_login_events_table
-- Created: Thu Apr 23 04:41:36 PM EDT 2026
-- Persistent per-login audit trail, independent of active_tokens.

CREATE TABLE login_events (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         VARCHAR(32),
    ip_address       VARCHAR(45),
    device_info      VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    last_modified_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_events_user_created ON login_events(user_id, created_at DESC);
