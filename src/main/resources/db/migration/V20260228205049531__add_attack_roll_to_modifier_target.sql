-- Migration: add_attack_roll_to_modifier_target
-- Created: Sat Feb 28 08:50:49 PM EST 2026

-- Add ATTACK_ROLL to the allowed modifier targets
ALTER TABLE feature_modifiers
    DROP CONSTRAINT chk_feature_modifiers_target,
    ADD CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD',
        'ATTACK_ROLL'
    ));
