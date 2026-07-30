package com.aboff.core.model.enums;

/**
 * Enum representing damage types in the Daggerheart TTRPG system.
 * Each damage type has a display code used in damage notation.
 */
public enum DamageType {

    /**
     * Physical damage type, typically from weapons and melee attacks.
     */
    PHYSICAL("phy"),

    /**
     * Magic damage type, typically from spells and magical abilities.
     */
    MAGIC("mag"),

    /**
     * Dual damage type where the wielder elects, per attack, whether the damage dealt is
     * physical or magic — <strong>not</strong> both simultaneously.
     *
     * <p>This is the "Otherworldly" mechanic (e.g. the Shadowblade weapon, Hope &amp; Fear p.44):
     * "On a successful attack, you can deal physical or magic damage." The choice is made at the
     * time of the attack; a single hit never deals combined or double damage.
     *
     * <p>Despite the constant name implying "and," rendering or display logic must present this
     * as an either/or choice (e.g. "Physical or Magic"), never as combined damage or two separate
     * damage numbers.
     */
    PHYSICAL_AND_MAGIC("phy/mag");

    private final String code;

    /**
     * Constructs a DamageType with the specified display code.
     *
     * @param code the display code used in damage notation (e.g., "phy", "mag")
     */
    DamageType(String code) {
        this.code = code;
    }

    /**
     * Returns the display code for this damage type.
     *
     * @return the display code (e.g., "phy", "mag")
     */
    public String getCode() {
        return code;
    }

    /**
     * Parses a damage code string (e.g., "phy", "mag") and returns the corresponding DamageType.
     *
     * @param code the damage code to parse
     * @return the matching DamageType
     * @throws IllegalArgumentException if no matching damage type is found
     */
    public static DamageType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Damage code cannot be null or blank");
        }
        String normalized = code.toLowerCase().trim();
        for (DamageType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown damage type: " + code);
    }
}
