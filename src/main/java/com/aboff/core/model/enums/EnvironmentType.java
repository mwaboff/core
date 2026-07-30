package com.aboff.core.model.enums;

/**
 * Represents the narrative role an environment plays during a Daggerheart session.
 * <p>
 * Environments are GM-facing stat blocks (never selected by a player) that set the
 * scene for a scenario. Each printed environment card is categorized as exactly one
 * of the four types below.
 * </p>
 */
public enum EnvironmentType {
    /** A location-driven scene meant to be explored (e.g. a ruin, a grove). */
    EXPLORATION,
    /** A scene centered on getting from one place to another. */
    TRAVERSAL,
    /** A time-boxed happening that unfolds regardless of location (e.g. an ambush). */
    EVENT,
    /** A scene centered on interaction between characters (e.g. a marketplace). */
    SOCIAL
}
