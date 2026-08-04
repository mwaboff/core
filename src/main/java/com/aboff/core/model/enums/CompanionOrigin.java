package com.aboff.core.model.enums;

/**
 * How a companion entered play. Drives level-up Training eligibility and level-down
 * reversal: only a {@link #SUBCLASS_FEATURE} companion is soft-deleted and later restorable
 * when the granting subclass is lost and re-taken.
 */
public enum CompanionOrigin {

    /**
     * Granted by a subclass feature (e.g. the Beastbound Ranger's Companion foundation
     * feature). Soft-deleted on level-down if the granting subclass is lost, and restorable
     * if the subclass is re-taken.
     */
    SUBCLASS_FEATURE,

    /**
     * Granted directly by a Game Master, independent of any subclass feature.
     */
    GM_GRANTED,

    /**
     * Added manually by the character sheet's owner. Never removed by level-down.
     */
    MANUAL
}
