package com.aboff.core.model.enums;

/**
 * Enumerates admin-initiated actions recorded in {@code admin_action_log}.
 * <p>
 * Each value is persisted as a string and is constrained at the database
 * layer by a CHECK constraint — see
 * {@code V20260423164136583__create_admin_action_log_table.sql}. Any change
 * here requires a corresponding migration.
 * </p>
 */
public enum AdminActionType {
    /** An admin banned a user. */
    USER_BANNED,
    /** An admin unbanned a user. */
    USER_UNBANNED,
    /** An admin changed a user's role. */
    USER_ROLE_CHANGED,
    /** An admin changed a user's username. */
    USER_USERNAME_CHANGED,
    /** An admin changed a user's avatar URL. */
    USER_AVATAR_CHANGED
}
