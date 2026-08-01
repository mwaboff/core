package com.aboff.core.model.enums;

/**
 * What a countdown does after its effect triggers at 0, per the Daggerheart SRD's
 * "Advanced Countdown Features" (p. 69).
 * <p>
 * Any change here must be mirrored in the {@code check_countdown_loop_behavior} constraint
 * on the {@code countdowns} table.
 * </p>
 */
public enum CountdownLoop {

    /** Does not loop. The countdown rests at 0 once its effect has triggered. */
    NONE,

    /** Resets to its starting value after its effect is triggered. */
    LOOP,

    /** Loops, increasing its starting value by 1 each time. */
    LOOP_INCREASING,

    /** Loops, decreasing its starting value by 1 each time. */
    LOOP_DECREASING
}
