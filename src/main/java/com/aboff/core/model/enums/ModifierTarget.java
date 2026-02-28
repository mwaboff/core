package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the possible targets that a feature modifier can affect in the Daggerheart TTRPG system.
 * <p>
 * Modifier targets include the six core traits, defensive thresholds, and resource maximums.
 * Each target has a description explaining what character attribute it modifies.
 * </p>
 */
@Getter
public enum ModifierTarget {
    AGILITY("Modifies the character's Agility trait score"),
    STRENGTH("Modifies the character's Strength trait score"),
    FINESSE("Modifies the character's Finesse trait score"),
    INSTINCT("Modifies the character's Instinct trait score"),
    PRESENCE("Modifies the character's Presence trait score"),
    KNOWLEDGE("Modifies the character's Knowledge trait score"),
    EVASION("Modifies the character's Evasion defense value"),
    MAJOR_DAMAGE_THRESHOLD("Modifies the character's Major damage threshold"),
    SEVERE_DAMAGE_THRESHOLD("Modifies the character's Severe damage threshold"),
    HIT_POINT_MAX("Modifies the character's maximum Hit Points"),
    STRESS_MAX("Modifies the character's maximum Stress capacity"),
    HOPE_MAX("Modifies the character's maximum Hope"),
    ARMOR_MAX("Modifies the character's maximum Armor slots"),
    GOLD("Modifies the character's starting Gold"),
    ATTACK_ROLL("Modifies the character's attack roll result");

    private final String description;

    /**
     * Constructs a ModifierTarget with its description.
     *
     * @param description A brief description of what character attribute this target modifies
     */
    ModifierTarget(String description) {
        this.description = description;
    }
}
