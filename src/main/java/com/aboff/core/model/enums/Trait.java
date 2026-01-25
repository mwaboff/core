package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the six core character traits in Daggerheart.
 * Each trait encompasses specific physical, mental, or social capabilities.
 */
@Getter
public enum Trait {
    AGILITY(
        "Quick reflexes, nimbleness, and coordination",
        "Dodging attacks, acrobatics, sleight of hand, stealth"
    ),
    STRENGTH(
        "Raw physical power and endurance",
        "Melee attacks, athletics, breaking objects, carrying heavy loads"
    ),
    FINESSE(
        "Precision, grace, and careful execution",
        "Ranged attacks, lockpicking, crafting, precise movements"
    ),
    INSTINCT(
        "Intuition, awareness, and natural understanding",
        "Perception, survival, animal handling, reading situations"
    ),
    PRESENCE(
        "Force of personality and social influence",
        "Persuasion, intimidation, performance, leadership"
    ),
    KNOWLEDGE(
        "Learning, reasoning, and mental acuity",
        "Spellcasting, history, investigation, arcana"
    );

    private final String description;
    private final String examples;

    /**
     * Constructs a Trait with its description and examples.
     *
     * @param description A brief description of what the trait represents
     * @param examples Common examples of when this trait is used
     */
    Trait(String description, String examples) {
        this.description = description;
        this.examples = examples;
    }
}
