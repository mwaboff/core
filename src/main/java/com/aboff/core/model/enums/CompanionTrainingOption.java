package com.aboff.core.model.enums;

/**
 * The Training level-up options a Beastbound Ranger's companion can be given, from the
 * printed Ranger Companion sheet's Training checkbox list.
 * <p>
 * Each option carries its own printed cap on how many times it can be taken by the same
 * companion ({@link #getMaxSelections()}), read directly off the sheet artwork (see the
 * companions implementation plan, section 2.5) since the checkbox counts do not appear in
 * the chapter transcription.
 * </p>
 */
public enum CompanionTrainingOption {

    /**
     * "Your companion gains a permanent +1 bonus to a Companion Experience of your choice."
     */
    INTELLIGENT(3),

    /**
     * "Use this as an additional Hope slot your character can mark."
     */
    LIGHT_IN_THE_DARK(1),

    /**
     * "Once per rest... you can gain a Hope or you can both clear a Stress."
     */
    CREATURE_COMFORT(1),

    /**
     * "When your companion takes damage, you can mark one of your Armor Slots instead of
     * marking one of their Stress."
     */
    ARMORED(1),

    /**
     * "Increase your companion's damage dice or range by one step (d6 to d8, Close to Far,
     * etc.)."
     */
    VICIOUS(3),

    /**
     * "Your companion gains an additional Stress slot."
     */
    RESILIENT(3),

    /**
     * "When you mark your last Hit Point, your companion rushes to your side to comfort
     * you... Clear your last Hit Point and return to the scene."
     */
    BONDED(1),

    /**
     * "Your companion gains a permanent +2 bonus to their Evasion."
     */
    AWARE(3);

    private final int maxSelections;

    /**
     * Constructs a CompanionTrainingOption with its printed maximum selection count.
     *
     * @param maxSelections the maximum number of times this option can be selected by one companion
     */
    CompanionTrainingOption(int maxSelections) {
        this.maxSelections = maxSelections;
    }

    /**
     * Returns the maximum number of times this option can be taken by a single companion.
     *
     * @return the printed checkbox count for this option
     */
    public int getMaxSelections() {
        return maxSelections;
    }
}
