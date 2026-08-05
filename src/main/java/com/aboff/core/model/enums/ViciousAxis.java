package com.aboff.core.model.enums;

/**
 * Which of a companion's two attack ladders a {@link CompanionTrainingOption#VICIOUS} pick
 * advances: "Increase your companion's damage dice or range by one step."
 */
public enum ViciousAxis {

    /**
     * Steps the companion's damage dice up the D6 -&gt; D8 -&gt; D10 -&gt; D12 ladder.
     */
    DAMAGE_DIE,

    /**
     * Steps the companion's attack range up the Melee -&gt; Very Close -&gt; Close -&gt; Far
     * -&gt; Very Far ladder. {@code OUT_OF_RANGE} is explicitly not part of this ladder.
     */
    RANGE
}
