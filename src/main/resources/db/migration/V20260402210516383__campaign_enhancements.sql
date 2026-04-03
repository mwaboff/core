-- Migration: campaign_enhancements
-- Created: Thu Apr  2 09:05:16 PM EDT 2026

-- 1. Add ended_at to campaigns (distinct from deleted_at: ended=locked+visible, deleted=invisible)
ALTER TABLE campaigns ADD COLUMN ended_at TIMESTAMP;
CREATE INDEX idx_campaigns_ended_at ON campaigns(ended_at);

-- 2. Campaign invites table
CREATE TABLE campaign_invites (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    token VARCHAR(36) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL,
    used_by BIGINT,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ci_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_used_by FOREIGN KEY (used_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_campaign_invites_campaign_id ON campaign_invites(campaign_id);
CREATE INDEX idx_campaign_invites_token ON campaign_invites(token);
CREATE INDEX idx_campaign_invites_expires_at ON campaign_invites(expires_at);
