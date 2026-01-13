-- Migration: create_active_tokens_table
-- Created: Tue Jan 13 09:31:00 AM EST 2026

CREATE TABLE active_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_active_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for token validation and cleanup
CREATE INDEX idx_active_tokens_user_id ON active_tokens(user_id);
CREATE INDEX idx_active_tokens_token_hash ON active_tokens(token_hash);
CREATE INDEX idx_active_tokens_expires_at ON active_tokens(expires_at);
CREATE INDEX idx_active_tokens_revoked_at ON active_tokens(revoked_at);

-- Composite index for active token lookup
CREATE INDEX idx_active_tokens_hash_valid
    ON active_tokens(token_hash, expires_at)
    WHERE revoked_at IS NULL;
