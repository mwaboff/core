-- Migration: add_proficiency_to_modifier_target
-- Created: Thu Jul 30 12:26:14 PM EDT 2026

-- Add PROFICIENCY to the allowed modifier targets. This is a live target:
-- the frontend stat calculator already applies PROFICIENCY modifiers
-- (character-sheet-view.mapper.ts) to compute the character's proficiency
-- value used in damage dice counts and the character sheet display.
ALTER TABLE feature_modifiers
    DROP CONSTRAINT chk_feature_modifiers_target,
    ADD CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD',
        'ATTACK_ROLL', 'DAMAGE_ROLL', 'PRIMARY_DAMAGE_ROLL', 'ARMOR_SCORE',
        'PROFICIENCY', 'BONUS_DOMAIN_CARD_SELECTIONS', 'BONUS_EXPERIENCE_MODIFIER'
    ));
