-- Migration: create_countdowns_table
-- Created: Fri Jul 31 09:37:09 PM EDT 2026
--
-- GM-facing countdown tracker, scoped to a campaign.
--
-- A countdown represents "a period of time or series of events preceding a future effect"
-- (Daggerheart SRD p. 68). It begins at a starting value, advances toward 0, and triggers
-- its effect on reaching 0.
--
-- countdown_type mirrors the SRD's advancement modes and is what tells a GM WHEN to tick:
--   STANDARD    - advances every time a player makes an action roll
--   PROGRESS    - dynamic, toward a positive effect (Dynamic Countdown Advancement table)
--   CONSEQUENCE - dynamic, toward a negative effect (same table, other column)
--   LONG_TERM   - advances on rests
--
-- loop_behavior mirrors the SRD's "Advanced Countdown Features" (p. 69). starting_value is
-- deliberately mutable: increasing/decreasing loops shift it by 1 on every loop.
--
-- Countdowns are GM-only state, like campaigns.gm_notes. They are not search-indexed.

CREATE TABLE countdowns (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,

    name VARCHAR(200) NOT NULL,
    countdown_type VARCHAR(20) NOT NULL,
    loop_behavior VARCHAR(20) NOT NULL DEFAULT 'NONE',
    starting_value INTEGER NOT NULL,
    current_value INTEGER NOT NULL,
    note TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_countdown_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT check_countdown_type
        CHECK (countdown_type IN ('STANDARD', 'PROGRESS', 'CONSEQUENCE', 'LONG_TERM')),
    CONSTRAINT check_countdown_loop_behavior
        CHECK (loop_behavior IN ('NONE', 'LOOP', 'LOOP_INCREASING', 'LOOP_DECREASING')),
    CONSTRAINT check_countdown_starting_value CHECK (starting_value BETWEEN 1 AND 99),
    CONSTRAINT check_countdown_current_value CHECK (current_value BETWEEN 0 AND 99)
);

CREATE INDEX idx_countdowns_campaign_id ON countdowns(campaign_id);
