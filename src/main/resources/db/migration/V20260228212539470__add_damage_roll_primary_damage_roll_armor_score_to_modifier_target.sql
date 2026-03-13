-- Migration: add_damage_roll_primary_damage_roll_armor_score_to_modifier_target
-- Created: Sat Feb 28 09:25:39 PM EST 2026

-- Add DAMAGE_ROLL, PRIMARY_DAMAGE_ROLL, and ARMOR_SCORE to the allowed modifier targets
ALTER TABLE feature_modifiers
    DROP CONSTRAINT chk_feature_modifiers_target,
    ADD CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD',
        'ATTACK_ROLL', 'DAMAGE_ROLL', 'PRIMARY_DAMAGE_ROLL', 'ARMOR_SCORE'
    ));
