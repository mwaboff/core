package com.aboff.core.model.enums;

/**
 * Represents the range categories for weapons and attacks in Daggerheart.
 * Each range defines the effective distance for weapon usage.
 */
public enum Range {
    /**
     * Close-quarters combat, under 5 feet.
     */
    MELEE,

    /**
     * Extended melee or point-blank range, 5-10 feet.
     */
    VERY_CLOSE,

    /**
     * Short throwing distance, 10-30 feet.
     */
    CLOSE,

    /**
     * Standard ranged weapon distance, 30-100 feet.
     */
    FAR,

    /**
     * Long-range projectile distance, 100-300 feet.
     */
    VERY_FAR,

    /**
     * Extreme distance beyond normal weapon effectiveness, beyond 300 feet.
     */
    OUT_OF_RANGE
}
