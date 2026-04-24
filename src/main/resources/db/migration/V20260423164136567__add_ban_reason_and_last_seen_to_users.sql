-- Migration: add_ban_reason_and_last_seen_to_users
-- Created: Thu Apr 23 04:41:36 PM EDT 2026
-- Adds admin-facing ban reason and last-seen timestamp columns to users.

ALTER TABLE users
    ADD COLUMN ban_reason VARCHAR(500),
    ADD COLUMN last_seen_at TIMESTAMP;
