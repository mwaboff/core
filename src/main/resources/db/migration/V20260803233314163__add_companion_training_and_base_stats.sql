-- Migration: add_companion_training_and_base_stats
-- Created: Mon Aug  3 11:33:14 PM EDT 2026
-- Description: Reworks companions from the earlier generic-pet model into the Daggerheart
-- Ranger Companion sheet. Renames the four printed stats to "base" values -- Training
-- selections layer on top and are derived at read time by CompanionDerivationService, never
-- stored -- and adds damage type, soft delete, origin tracking, and a per-companion
-- advancement opt-out. Adds companion_trainings for the Training checkbox list, and a
-- companions_enabled gate on character_sheets mirroring transformation_enabled.

-- ---- companions: rename to base semantics ----

ALTER TABLE companions RENAME COLUMN evasion TO base_evasion;
-- Evasion "starts at 10" per the rules (core-01:1313); the earlier generic-pet model
-- defaulted to 0. Backfill existing rows left at that wrong default before changing it.
UPDATE companions SET base_evasion = 10 WHERE base_evasion = 0;
ALTER TABLE companions ALTER COLUMN base_evasion SET DEFAULT 10;

ALTER TABLE companions RENAME COLUMN stress_max TO base_stress_max;
ALTER TABLE companions ALTER COLUMN base_stress_max SET DEFAULT 3;

ALTER TABLE companions RENAME COLUMN damage_dice TO base_damage_dice;
ALTER TABLE companions ALTER COLUMN base_damage_dice SET DEFAULT 'D6';

ALTER TABLE companions RENAME COLUMN attack_range TO base_attack_range;
ALTER TABLE companions ALTER COLUMN base_attack_range SET DEFAULT 'MELEE';

-- ---- companions: new columns ----

ALTER TABLE companions ADD COLUMN damage_type VARCHAR(20) NOT NULL DEFAULT 'PHYSICAL';
ALTER TABLE companions ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE companions ADD COLUMN origin VARCHAR(30) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE companions ADD COLUMN origin_subclass_card_id BIGINT NULL REFERENCES subclass_cards(id);
ALTER TABLE companions ADD COLUMN advances_on_level_up BOOLEAN NOT NULL DEFAULT true;

-- ---- companion_trainings: the printed Training checkbox list ----

CREATE TABLE companion_trainings (
    id                   BIGSERIAL PRIMARY KEY,
    companion_id         BIGINT NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    option               VARCHAR(40) NOT NULL,
    vicious_axis         VARCHAR(20),
    target_experience_id BIGINT REFERENCES experiences(id) ON DELETE SET NULL,
    acquired_at_level    INTEGER NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companion_trainings_companion_id ON companion_trainings(companion_id);

-- ---- character_sheets: GM gate for creating new companions ----
-- Mirrors transformation_enabled (V20260801174556711): defaults FALSE. Unlike
-- transformations, existing companions are never hidden by this flag -- only the ability to
-- create a new one is gated (see companions implementation plan, decision 3.4).

ALTER TABLE character_sheets ADD COLUMN companions_enabled BOOLEAN NOT NULL DEFAULT FALSE;
