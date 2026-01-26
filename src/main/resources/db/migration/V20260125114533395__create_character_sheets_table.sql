-- Migration: Create character_sheets table
-- Description: Creates the main character sheet table for Daggerheart TTRPG characters

CREATE TABLE character_sheets (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Basic character information
    name VARCHAR(200) NOT NULL,
    pronouns VARCHAR(100),
    level INTEGER NOT NULL DEFAULT 1,

    -- Combat attributes
    evasion INTEGER NOT NULL DEFAULT 0,
    armor_max INTEGER NOT NULL DEFAULT 0,
    armor_marked INTEGER NOT NULL DEFAULT 0,
    major_damage_threshold INTEGER NOT NULL,
    severe_damage_threshold INTEGER NOT NULL,

    -- Trait modifiers and marked status (6 traits: AGILITY, STRENGTH, FINESSE, INSTINCT, PRESENCE, KNOWLEDGE)
    agility_modifier INTEGER NOT NULL DEFAULT 0,
    agility_marked BOOLEAN NOT NULL DEFAULT FALSE,
    strength_modifier INTEGER NOT NULL DEFAULT 0,
    strength_marked BOOLEAN NOT NULL DEFAULT FALSE,
    finesse_modifier INTEGER NOT NULL DEFAULT 0,
    finesse_marked BOOLEAN NOT NULL DEFAULT FALSE,
    instinct_modifier INTEGER NOT NULL DEFAULT 0,
    instinct_marked BOOLEAN NOT NULL DEFAULT FALSE,
    presence_modifier INTEGER NOT NULL DEFAULT 0,
    presence_marked BOOLEAN NOT NULL DEFAULT FALSE,
    knowledge_modifier INTEGER NOT NULL DEFAULT 0,
    knowledge_marked BOOLEAN NOT NULL DEFAULT FALSE,

    -- Resources
    hit_point_max INTEGER NOT NULL DEFAULT 6,
    hit_point_marked INTEGER NOT NULL DEFAULT 0,
    stress_max INTEGER NOT NULL DEFAULT 6,
    stress_marked INTEGER NOT NULL DEFAULT 0,
    hope_max INTEGER NOT NULL DEFAULT 2,
    hope_marked INTEGER NOT NULL DEFAULT 0,

    -- Economy
    gold INTEGER NOT NULL DEFAULT 0,

    -- Active equipment (foreign keys)
    active_primary_weapon_id BIGINT,
    active_secondary_weapon_id BIGINT,
    active_armor_id BIGINT,

    -- Ownership
    owner_id BIGINT NOT NULL,

    -- Metadata
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_character_sheet_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_character_sheet_active_primary_weapon FOREIGN KEY (active_primary_weapon_id) REFERENCES weapons(id) ON DELETE SET NULL,
    CONSTRAINT fk_character_sheet_active_secondary_weapon FOREIGN KEY (active_secondary_weapon_id) REFERENCES weapons(id) ON DELETE SET NULL,
    CONSTRAINT fk_character_sheet_active_armor FOREIGN KEY (active_armor_id) REFERENCES armors(id) ON DELETE SET NULL,

    -- Constraints
    CONSTRAINT check_level_positive CHECK (level >= 1),
    CONSTRAINT check_severe_gte_major CHECK (severe_damage_threshold >= major_damage_threshold),
    CONSTRAINT check_hit_point_marked_lte_max CHECK (hit_point_marked <= hit_point_max),
    CONSTRAINT check_stress_marked_lte_max CHECK (stress_marked <= stress_max),
    CONSTRAINT check_hope_marked_lte_max CHECK (hope_marked <= hope_max),
    CONSTRAINT check_armor_marked_lte_max CHECK (armor_marked <= armor_max)
);

-- Indexes for common queries
CREATE INDEX idx_character_sheets_owner_id ON character_sheets(owner_id);
CREATE INDEX idx_character_sheets_deleted_at ON character_sheets(deleted_at);
CREATE INDEX idx_character_sheets_owner_not_deleted ON character_sheets(owner_id, deleted_at) WHERE deleted_at IS NULL;
