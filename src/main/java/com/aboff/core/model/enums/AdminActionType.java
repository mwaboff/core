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
    USER_AVATAR_CHANGED,
    /** An admin granted or revoked a user's "Access All Expansions" override. */
    USER_EXPANSION_ACCESS_CHANGED,
    /**
     * An admin flagged or unflagged a batch of game content as SRD-licensed via the bulk
     * SRD-flagging tool. Unlike every other action type, this one has no target user; see
     * {@code V20260811225540367__add_content_srd_changed_admin_action.sql}.
     */
    CONTENT_SRD_CHANGED
}
