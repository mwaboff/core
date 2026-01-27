-- Migration: Create campaign join tables
-- Created: Mon Jan 26 09:43:03 AM EST 2026
-- These tables manage the many-to-many relationships for campaigns

-- ========== Game Masters Join Table ==========
CREATE TABLE campaign_game_masters (
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_id, user_id),
    CONSTRAINT fk_cgm_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgm_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_campaign_game_masters_campaign_id ON campaign_game_masters(campaign_id);
CREATE INDEX idx_campaign_game_masters_user_id ON campaign_game_masters(user_id);

-- ========== Players Join Table ==========
CREATE TABLE campaign_players (
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_id, user_id),
    CONSTRAINT fk_cp_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_campaign_players_campaign_id ON campaign_players(campaign_id);
CREATE INDEX idx_campaign_players_user_id ON campaign_players(user_id);

-- ========== Pending Character Sheets Join Table ==========
CREATE TABLE campaign_pending_character_sheets (
    campaign_id BIGINT NOT NULL,
    character_sheet_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_id, character_sheet_id),
    CONSTRAINT fk_cpcs_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_cpcs_character_sheet FOREIGN KEY (character_sheet_id)
        REFERENCES character_sheets(id) ON DELETE CASCADE
);

CREATE INDEX idx_campaign_pending_cs_campaign_id ON campaign_pending_character_sheets(campaign_id);
CREATE INDEX idx_campaign_pending_cs_character_sheet_id ON campaign_pending_character_sheets(character_sheet_id);

-- ========== Player Characters Join Table ==========
CREATE TABLE campaign_player_characters (
    campaign_id BIGINT NOT NULL,
    character_sheet_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_id, character_sheet_id),
    CONSTRAINT fk_cpc_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_cpc_character_sheet FOREIGN KEY (character_sheet_id)
        REFERENCES character_sheets(id) ON DELETE CASCADE
);

CREATE INDEX idx_campaign_player_chars_campaign_id ON campaign_player_characters(campaign_id);
CREATE INDEX idx_campaign_player_chars_character_sheet_id ON campaign_player_characters(character_sheet_id);

-- ========== Non-Player Characters Join Table ==========
CREATE TABLE campaign_non_player_characters (
    campaign_id BIGINT NOT NULL,
    character_sheet_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_id, character_sheet_id),
    CONSTRAINT fk_cnpc_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_cnpc_character_sheet FOREIGN KEY (character_sheet_id)
        REFERENCES character_sheets(id) ON DELETE CASCADE
);

CREATE INDEX idx_campaign_npcs_campaign_id ON campaign_non_player_characters(campaign_id);
CREATE INDEX idx_campaign_npcs_character_sheet_id ON campaign_non_player_characters(character_sheet_id);
