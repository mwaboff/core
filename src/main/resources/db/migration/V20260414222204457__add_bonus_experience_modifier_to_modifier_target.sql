-- Migration: add_bonus_experience_modifier_to_modifier_target
-- Created: Tue Apr 14 10:22:04 PM EDT 2026

-- Add BONUS_EXPERIENCE_MODIFIER to the allowed modifier targets.
-- This declarative target represents a one-time +N bonus applied to a
-- player-chosen existing experience. Consumed by client-side pickers during
-- character creation / level-up.
ALTER TABLE feature_modifiers
    DROP CONSTRAINT chk_feature_modifiers_target,
    ADD CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD',
        'ATTACK_ROLL', 'DAMAGE_ROLL', 'PRIMARY_DAMAGE_ROLL', 'ARMOR_SCORE',
        'BONUS_DOMAIN_CARD_SELECTIONS', 'BONUS_EXPERIENCE_MODIFIER'
    ));
