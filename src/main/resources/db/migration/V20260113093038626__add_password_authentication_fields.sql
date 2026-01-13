-- Migration: add_password_authentication_fields
-- Created: Tue Jan 13 09:30:38 AM EST 2026

-- Add password authentication fields
ALTER TABLE users ADD COLUMN password_hash VARCHAR(60);
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login TIMESTAMP;

-- Index for locked account checks
CREATE INDEX idx_users_account_locked ON users(account_locked_until);
