package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the mathematical operation applied by a feature modifier in the Daggerheart TTRPG system.
 * <p>
 * The operation determines how the modifier's value is applied to the target attribute.
 * Operations are evaluated in a defined order: SET first, then MULTIPLY, then ADD.
 * </p>
 */
@Getter
public enum ModifierOperation {
    ADD("Adds the value to the target attribute"),
    SET("Sets the target attribute to the specified value"),
    MULTIPLY("Multiplies the target attribute by the specified value");

    private final String description;

    /**
     * Constructs a ModifierOperation with its description.
     *
     * @param description A brief description of how this operation modifies the target
     */
    ModifierOperation(String description) {
        this.description = description;
    }
}