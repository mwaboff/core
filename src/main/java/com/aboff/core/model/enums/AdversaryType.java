package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Represents the tactical role and combat behavior of adversaries in Daggerheart.
 * <p>
 * Each type indicates how an adversary functions in combat and provides
 * guidance for game masters on how to use them effectively.
 * </p>
 * <p>
 * Each type also has an associated battle point value used for encounter balancing:
 * </p>
 * <ul>
 *   <li>1 point: MINION, SOCIAL, SUPPORT</li>
 *   <li>2 points: HORDE, RANGED, SKULK, STANDARD</li>
 *   <li>3 points: LEADER</li>
 *   <li>4 points: BRUISER</li>
 *   <li>5 points: SOLO</li>
 * </ul>
 */
@Getter
public enum AdversaryType {
    /**
     * Tough melee combatants with high HP designed to absorb damage.
     */
    BRUISER("Tough melee combatants with high HP", 4),

    /**
     * Multiple weak enemies that attack together in large numbers.
     */
    HORDE("Multiple weak enemies that attack together", 2),

    /**
     * Commanders that buff allies and provide tactical advantages.
     */
    LEADER("Commanders that buff allies", 3),

    /**
     * Basic enemies with minimal HP, easily defeated.
     */
    MINION("Basic enemies with minimal HP", 1),

    /**
     * Distance attackers that excel at ranged combat.
     */
    RANGED("Distance attackers", 2),

    /**
     * Stealthy enemies with evasion bonuses and ambush tactics.
     */
    SKULK("Stealthy enemies with evasion bonuses", 2),

    /**
     * Non-combat focused adversaries designed for social encounters.
     */
    SOCIAL("Non-combat focused adversaries", 1),

    /**
     * Single powerful enemy designed to fight alone against a party.
     */
    SOLO("Single powerful enemy designed to fight alone", 5),

    /**
     * Balanced general-purpose adversary suitable for most encounters.
     */
    STANDARD("Balanced general-purpose adversary", 2),

    /**
     * Provides utility and healing to allies during combat.
     */
    SUPPORT("Provides utility and healing to allies", 1);

    private final String description;
    private final int battlePoints;

    AdversaryType(String description, int battlePoints) {
        this.description = description;
        this.battlePoints = battlePoints;
    }
}
