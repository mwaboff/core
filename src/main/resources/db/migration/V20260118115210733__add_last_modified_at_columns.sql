-- Migration: add_last_modified_at_columns
-- Created: Sun Jan 18 11:52:10 AM EST 2026

-- Add last_modified_at to login_attempts table
ALTER TABLE login_attempts
ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to match created_at
UPDATE login_attempts
SET last_modified_at = created_at;

-- Add last_modified_at to active_tokens table
ALTER TABLE active_tokens
ADD COLUMN last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to match created_at
UPDATE active_tokens
SET last_modified_at = created_at;

-- Add indexes for audit queries
CREATE INDEX idx_login_attempts_last_modified_at ON login_attempts(last_modified_at);
CREATE INDEX idx_active_tokens_last_modified_at ON active_tokens(last_modified_at);
