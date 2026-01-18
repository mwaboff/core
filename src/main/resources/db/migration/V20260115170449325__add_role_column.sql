-- Migration: add_role_column
-- Created: Wed Jan 15 05:04:49 PM EST 2026

-- Add role column to users table
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Create index for role-based queries
CREATE INDEX idx_users_role ON users(role);
