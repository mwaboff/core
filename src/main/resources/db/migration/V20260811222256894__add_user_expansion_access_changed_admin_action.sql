-- Migration: add_user_expansion_access_changed_admin_action
--
-- AdminActionType gains USER_EXPANSION_ACCESS_CHANGED, logged when an admin grants
-- or revokes a user's "Access All Expansions" override (see
-- V20260811222234279__add_srd_and_expansion_access.sql for the access_all_expansions
-- column this action records changes to). The enum value alone does nothing without
-- this constraint update -- admin_action_log.action is guarded by a CHECK constraint,
-- and PostgreSQL rejects any value outside its list regardless of what the Java enum
-- allows.

ALTER TABLE admin_action_log DROP CONSTRAINT chk_admin_action_log_action;

ALTER TABLE admin_action_log ADD CONSTRAINT chk_admin_action_log_action CHECK (action IN (
    'USER_BANNED',
    'USER_UNBANNED',
    'USER_ROLE_CHANGED',
    'USER_USERNAME_CHANGED',
    'USER_AVATAR_CHANGED',
    'USER_EXPANSION_ACCESS_CHANGED'
));
