-- Migration: add_bonus_domain_card_selections_to_modifier_target
-- Created: Tue Apr 14 08:55:17 PM EDT 2026

-- Add BONUS_DOMAIN_CARD_SELECTIONS to the allowed modifier targets.
-- This declarative target is attached to subclass foundation features that grant
-- an extra domain card selection at level-up / character creation. See
-- AdvancementType.FEATURE_DOMAIN_CARD for the corresponding runtime advancement.
ALTER TABLE feature_modifiers
    DROP CONSTRAINT chk_feature_modifiers_target,
    ADD CONSTRAINT chk_feature_modifiers_target CHECK (target IN (
        'AGILITY', 'STRENGTH', 'FINESSE', 'INSTINCT', 'PRESENCE', 'KNOWLEDGE',
        'EVASION', 'MAJOR_DAMAGE_THRESHOLD', 'SEVERE_DAMAGE_THRESHOLD',
        'HIT_POINT_MAX', 'STRESS_MAX', 'HOPE_MAX', 'ARMOR_MAX', 'GOLD',
        'ATTACK_ROLL', 'DAMAGE_ROLL', 'PRIMARY_DAMAGE_ROLL', 'ARMOR_SCORE',
        'BONUS_DOMAIN_CARD_SELECTIONS'
    ));
