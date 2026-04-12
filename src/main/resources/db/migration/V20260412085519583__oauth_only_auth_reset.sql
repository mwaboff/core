-- OAuth-only authentication migration
-- Clears all user data and removes password-based auth infrastructure.
-- Pre-launch wipe: no data needs to be preserved.

-- Clear data in FK-respecting order
DELETE FROM active_tokens;
DELETE FROM login_attempts;
DELETE FROM users;

-- Drop password-based login audit table
DROP TABLE IF EXISTS login_attempts;

-- Remove password and account-lock columns from users
ALTER TABLE users
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS failed_login_attempts,
    DROP COLUMN IF EXISTS last_failed_login,
    DROP COLUMN IF EXISTS account_locked_until;

-- Email is no longer required (some OAuth providers don't expose it)
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Uniqueness on email moves to user_identities; drop the old constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- OAuth identity table: one row per (provider, provider_sub) pair
CREATE TABLE user_identities (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider          VARCHAR(32) NOT NULL,
    provider_sub      VARCHAR(255) NOT NULL,
    email             VARCHAR(255),
    display_name      VARCHAR(255),
    avatar_url        VARCHAR(500),
    linked_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at      TIMESTAMPTZ,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    last_modified_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_sub)
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);
