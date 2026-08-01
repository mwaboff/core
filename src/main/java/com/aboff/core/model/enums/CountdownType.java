package com.aboff.core.model.enums;

/**
 * The advancement mode of a countdown, as defined by the Daggerheart SRD (p. 68).
 * <p>
 * This is the field that answers "when do I tick this?" — each value corresponds to a
 * different trigger for advancing the countdown. Note that "dynamic" is not itself a value:
 * {@link #PROGRESS} and {@link #CONSEQUENCE} <em>are</em> the two dynamic kinds, differing
 * only in which column of the Dynamic Countdown Advancement table they read.
 * </p>
 * <p>
 * Any change here must be mirrored in the {@code check_countdown_type} constraint on the
 * {@code countdowns} table.
 * </p>
 */
public enum CountdownType {

    /** Advances every time a player makes an action roll. */
    STANDARD,

    /** Dynamic countdown toward a positive effect. Advances per the Progress column. */
    PROGRESS,

    /** Dynamic countdown toward a negative effect. Advances per the Consequence column. */
    CONSEQUENCE,

    /** Advances on rests rather than on action rolls. */
    LONG_TERM
}
