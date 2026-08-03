-- Migration: create_encounter_runs_tables
-- Created: Sun Aug  2 09:07:14 PM EDT 2026
--
-- Server-side live state for *running* a fight, as designed in
-- docs/agent-plans/2026-08-02-encounter-manager-design.md (Phase 4).
--
-- A run snapshots an encounter's adversary instances into encounter_run_adversaries at start
-- time, so editing the saved encounter mid-fight cannot corrupt an in-progress run. All live
-- HP/Stress/defeated/note state lives here -- never on the catalog `adversaries` table, since
-- two instances of the same adversary in one encounter share that row.
--
-- campaign_id is deliberately NULLABLE: running a fight is campaign-free by design. A run tags
-- itself to a campaign only to widen who else can see it (the campaign's GMs); a campaign is
-- never required to start or play one. Like `encounters.campaign_id`, ON DELETE SET NULL rather
-- than CASCADE -- deleting a campaign should not destroy a fight in progress.
--
-- Runs hard-delete, like `countdowns`: this is small, ephemeral GM/player session state, not
-- durable content, so there is no deleted_at column.

CREATE TABLE encounter_runs (
    id BIGSERIAL PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    campaign_id BIGINT,
    started_by_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_encounter_run_encounter FOREIGN KEY (encounter_id)
        REFERENCES encounters(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounter_run_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id) ON DELETE SET NULL,
    CONSTRAINT fk_encounter_run_started_by FOREIGN KEY (started_by_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_encounter_run_status
        CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

-- "My active runs" -- the standalone page's resume list when no campaignId is given.
CREATE INDEX idx_encounter_runs_started_by_status ON encounter_runs(started_by_id, status);

-- The GM screen panel's campaign-scoped list. Partial: most runs are standalone (campaign_id
-- NULL) and would otherwise bloat an index that query never uses.
CREATE INDEX idx_encounter_runs_campaign ON encounter_runs(campaign_id) WHERE campaign_id IS NOT NULL;

-- Snapshotted per-instance live state, copied from encounter_adversaries at run start.
-- adversary_id is a read-only reference back to the catalog stat block -- never written to.
CREATE TABLE encounter_run_adversaries (
    id BIGSERIAL PRIMARY KEY,
    encounter_run_id BIGINT NOT NULL,
    adversary_id BIGINT NOT NULL,

    label VARCHAR(100),
    tier_override INTEGER,
    hit_points_marked INTEGER NOT NULL DEFAULT 0,
    stress_marked INTEGER NOT NULL DEFAULT 0,
    is_defeated BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_encounter_run_adversary_run FOREIGN KEY (encounter_run_id)
        REFERENCES encounter_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_encounter_run_adversary_adversary FOREIGN KEY (adversary_id)
        REFERENCES adversaries(id),
    CONSTRAINT check_encounter_run_adversary_tier_override
        CHECK (tier_override IS NULL OR tier_override BETWEEN 1 AND 4),
    CONSTRAINT check_encounter_run_adversary_hit_points_marked
        CHECK (hit_points_marked >= 0),
    CONSTRAINT check_encounter_run_adversary_stress_marked
        CHECK (stress_marked >= 0)
);

CREATE INDEX idx_encounter_run_adversaries_run ON encounter_run_adversaries(encounter_run_id);
