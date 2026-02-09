package com.aboff.core.model.enums;

/**
 * Enum representing the category of a card cost tag in the Daggerheart TTRPG system.
 * Used for frontend grouping and styling of cost/limitation badges on cards.
 */
public enum CostTagCategory {
    /**
     * Resource expenditure tags (e.g., "3 Hope", "1 Stress").
     */
    COST,

    /**
     * Restriction or requirement tags (e.g., "Close range", "Requires Level 5").
     */
    LIMITATION,

    /**
     * Frequency or action type tags (e.g., "1/session", "Action", "Reaction").
     */
    TIMING
}
