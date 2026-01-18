-- Migration: create_login_attempts_table
-- Created: Tue Jan 13 09:30:53 AM EST 2026

CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username_attempted VARCHAR(100) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    ip_address VARCHAR(45), -- IPv6 max length
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_attempts_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for audit queries
CREATE INDEX idx_login_attempts_user_id ON login_attempts(user_id);
CREATE INDEX idx_login_attempts_username ON login_attempts(username_attempted);
CREATE INDEX idx_login_attempts_created_at ON login_attempts(created_at);
CREATE INDEX idx_login_attempts_success ON login_attempts(success);
CREATE INDEX idx_login_attempts_ip_address ON login_attempts(ip_address);

-- Composite index for recent failed attempts
CREATE INDEX idx_login_attempts_username_time_success
    ON login_attempts(username_attempted, created_at DESC, success)
    WHERE success = false;
