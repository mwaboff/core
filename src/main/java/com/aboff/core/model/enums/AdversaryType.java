package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the tactical role and combat behavior of adversaries in Daggerheart.
 * <p>
 * Each type indicates how an adversary functions in combat and provides
 * guidance for game masters on how to use them effectively.
 * </p>
 */
@Getter
public enum AdversaryType {
    /**
     * Tough melee combatants with high HP designed to absorb damage.
     */
    BRUISER("Tough melee combatants with high HP"),

    /**
     * Multiple weak enemies that attack together in large numbers.
     */
    HORDE("Multiple weak enemies that attack together"),

    /**
     * Commanders that buff allies and provide tactical advantages.
     */
    LEADER("Commanders that buff allies"),

    /**
     * Basic enemies with minimal HP, easily defeated.
     */
    MINION("Basic enemies with minimal HP"),

    /**
     * Distance attackers that excel at ranged combat.
     */
    RANGED("Distance attackers"),

    /**
     * Stealthy enemies with evasion bonuses and ambush tactics.
     */
    SKULK("Stealthy enemies with evasion bonuses"),

    /**
     * Non-combat focused adversaries designed for social encounters.
     */
    SOCIAL("Non-combat focused adversaries"),

    /**
     * Single powerful enemy designed to fight alone against a party.
     */
    SOLO("Single powerful enemy designed to fight alone"),

    /**
     * Balanced general-purpose adversary suitable for most encounters.
     */
    STANDARD("Balanced general-purpose adversary"),

    /**
     * Provides utility and healing to allies during combat.
     */
    SUPPORT("Provides utility and healing to allies");

    private final String description;

    AdversaryType(String description) {
        this.description = description;
    }
}
