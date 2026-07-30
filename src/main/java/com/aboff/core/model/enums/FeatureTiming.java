package com.aboff.core.model.enums;

/**
 * Enum representing the timing tag printed as part of a feature's heading in the
 * Daggerheart TTRPG source material (e.g. {@code "Name - Action:"}).
 * <p>
 * Most features carry no timing tag at all; the column backed by this enum is
 * nullable to represent that absence.
 * </p>
 */
public enum FeatureTiming {
    /** The feature is used as an action, e.g. {@code "Name - Action:"}. */
    ACTION,
    /** The feature is used as a reaction, e.g. {@code "Name - Reaction:"}. */
    REACTION,
    /** The feature is always in effect, e.g. {@code "Name - Passive:"}. */
    PASSIVE,
    /** The feature triggers as part of an adversary evolving, e.g. {@code "Name - Evolution:"}. */
    EVOLUTION
}
