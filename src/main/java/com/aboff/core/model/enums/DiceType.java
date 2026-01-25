package com.aboff.core.model.enums;

/**
 * Enum representing standard TTRPG dice types.
 * Each dice type has a number of sides and a display code used in dice notation.
 */
public enum DiceType {

    /**
     * Four-sided die.
     */
    D4(4, "d4"),

    /**
     * Six-sided die.
     */
    D6(6, "d6"),

    /**
     * Eight-sided die.
     */
    D8(8, "d8"),

    /**
     * Ten-sided die.
     */
    D10(10, "d10"),

    /**
     * Twelve-sided die.
     */
    D12(12, "d12"),

    /**
     * Twenty-sided die.
     */
    D20(20, "d20");

    private final int sides;
    private final String code;

    /**
     * Constructs a DiceType with the specified number of sides and display code.
     *
     * @param sides the number of sides on the die
     * @param code the display code used in dice notation (e.g., "d10")
     */
    DiceType(int sides, String code) {
        this.sides = sides;
        this.code = code;
    }

    /**
     * Returns the number of sides on this die.
     *
     * @return the number of sides
     */
    public int getSides() {
        return sides;
    }

    /**
     * Returns the display code for this die type.
     *
     * @return the display code (e.g., "d10")
     */
    public String getCode() {
        return code;
    }

    /**
     * Parses a dice code string (e.g., "d10", "D10") and returns the corresponding DiceType.
     *
     * @param code the dice code to parse
     * @return the matching DiceType
     * @throws IllegalArgumentException if no matching dice type is found
     */
    public static DiceType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Dice code cannot be null or blank");
        }
        String normalized = code.toLowerCase().trim();
        for (DiceType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dice type: " + code);
    }
}
