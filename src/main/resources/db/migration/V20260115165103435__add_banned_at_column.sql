-- Migration: add_banned_at_column
-- Created: Wed Jan 15 04:51:03 PM EST 2026

-- Add banned_at column to track user bans
ALTER TABLE users ADD COLUMN banned_at TIMESTAMP;

-- Index for banned user checks
CREATE INDEX idx_users_banned_at ON users(banned_at);
