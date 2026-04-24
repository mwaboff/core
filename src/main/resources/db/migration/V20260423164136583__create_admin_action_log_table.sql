-- Migration: create_admin_action_log_table
-- Created: Thu Apr 23 04:41:36 PM EDT 2026
-- Durable audit trail for admin-initiated actions against users.

CREATE TABLE admin_action_log (
    id                BIGSERIAL PRIMARY KEY,
    actor_user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    target_user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action            VARCHAR(40) NOT NULL,
    details           TEXT,
    ip_address        VARCHAR(45),
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    last_modified_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_admin_action_log_action CHECK (action IN (
        'USER_BANNED',
        'USER_UNBANNED',
        'USER_ROLE_CHANGED',
        'USER_USERNAME_CHANGED',
        'USER_AVATAR_CHANGED'
    ))
);

CREATE INDEX idx_admin_action_log_target_created ON admin_action_log(target_user_id, created_at DESC);
CREATE INDEX idx_admin_action_log_actor_created ON admin_action_log(actor_user_id, created_at DESC);
