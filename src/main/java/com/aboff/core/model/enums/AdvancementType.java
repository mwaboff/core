package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Enum representing the types of advancements available during character level-up
 * in the Daggerheart TTRPG system.
 * <p>
 * Each advancement type has a minimum tier requirement that determines when it becomes
 * available. Characters gain two advancements per level-up.
 * </p>
 */
@Getter
public enum AdvancementType {

    /**
     * Boost two trait modifiers by +1 each and mark them.
     */
    BOOST_TRAITS(1),

    /**
     * Gain +1 to maximum hit points.
     */
    GAIN_HP(1),

    /**
     * Gain +1 to maximum stress.
     */
    GAIN_STRESS(1),

    /**
     * Boost two experience modifiers by +1 each.
     */
    BOOST_EXPERIENCES(2),

    /**
     * Gain a new domain card from an accessible domain.
     */
    GAIN_DOMAIN_CARD(2),

    /**
     * Gain +1 to evasion.
     */
    BOOST_EVASION(2),

    /**
     * Upgrade to the next subclass card within an existing subclass path.
     * Mutually exclusive with MULTICLASS within a tier.
     */
    UPGRADE_SUBCLASS(2),

    /**
     * Gain +1 to proficiency.
     */
    BOOST_PROFICIENCY(3),

    /**
     * Add a foundation subclass card from a new class.
     * Mutually exclusive with UPGRADE_SUBCLASS within a tier.
     */
    MULTICLASS(3),

    /**
     * Domain card granted by a subclass feature's {@code BONUS_DOMAIN_CARD_SELECTIONS} modifier.
     * <p>
     * This advancement type is injected by the client when a qualifying subclass feature is taken.
     * It does <b>not</b> count toward the "exactly 2 player advancements" rule, does not count
     * toward {@link #GAIN_DOMAIN_CARD}'s per-tier limit, and is not returned by
     * {@code getLevelUpOptions} as a selectable option. Cards granted this way are added unequipped.
     * </p>
     */
    FEATURE_DOMAIN_CARD(1),

    /**
     * Step the Brawler's Combo Die up one size (e.g. d4 to d6). Once per tier.
     */
    UPGRADE_COMBO_DIE(1);

    /**
     * The minimum tier at which this advancement type becomes available.
     */
    private final int minTier;

    AdvancementType(int minTier) {
        this.minTier = minTier;
    }
}
