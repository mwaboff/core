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
    MULTICLASS(3);

    /**
     * The minimum tier at which this advancement type becomes available.
     */
    private final int minTier;

    AdvancementType(int minTier) {
        this.minTier = minTier;
    }
}
