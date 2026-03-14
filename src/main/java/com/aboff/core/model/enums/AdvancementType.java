package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the types of advancements a character can choose when leveling up in Daggerheart.
 * <p>
 * Each advancement type has a description explaining what benefit it provides and a minimum
 * tier requirement. Characters can only select advancements at or above their current tier.
 * </p>
 * <p>
 * Tier 2 advancements are available from the start of leveling, while Tier 3 advancements
 * become available as the character progresses to higher levels.
 * </p>
 */
@Getter
public enum AdvancementType {
    BOOST_TRAITS("+1 to two unmarked traits, mark them", 2),
    GAIN_HP("+1 hit point max", 2),
    GAIN_STRESS("+1 stress max", 2),
    BOOST_EXPERIENCES("+1 modifier to two experiences", 2),
    GAIN_DOMAIN_CARD("Choose a domain card of appropriate level", 2),
    BOOST_EVASION("+1 evasion", 2),
    UPGRADE_SUBCLASS("Take upgraded subclass card", 3),
    BOOST_PROFICIENCY("+1 proficiency", 3),
    MULTICLASS("Choose additional class", 3);

    private final String description;
    private final int minTier;

    /**
     * Constructs an AdvancementType with its description and minimum tier requirement.
     *
     * @param description A brief description of the advancement benefit
     * @param minTier The minimum tier at which this advancement becomes available
     */
    AdvancementType(String description, int minTier) {
        this.description = description;
        this.minTier = minTier;
    }
}
