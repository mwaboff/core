package com.aboff.core.model.enums;

/**
 * The lifecycle state of an {@link com.aboff.core.model.entity.dh.EncounterRun}.
 * <p>
 * A run starts {@link #ACTIVE} the moment it snapshots an encounter's adversaries and moves to
 * {@link #COMPLETED} when the GM (or whoever started it) marks the fight over. There is no
 * "paused" state -- an active run simply sits idle between updates.
 * </p>
 * <p>
 * Any change here must be mirrored in the {@code check_encounter_run_status} constraint on the
 * {@code encounter_runs} table.
 * </p>
 */
public enum EncounterRunStatus {

    /** The fight is in progress; HP, Stress, and defeated state may still change. */
    ACTIVE,

    /** The fight is over. The run is retained as a record but is no longer mutable. */
    COMPLETED
}
