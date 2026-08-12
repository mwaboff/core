-- Migration: add_content_srd_changed_admin_action
--
-- AdminActionType gains CONTENT_SRD_CHANGED, logged once per bulk SRD flag/unflag batch from
-- the admin bulk SRD-flagging tool (PATCH /api/admin/content/srd). The enum value alone does
-- nothing without this constraint update -- admin_action_log.action is guarded by a CHECK
-- constraint, and PostgreSQL rejects any value outside its list regardless of what the Java
-- enum allows. See V20260811222256894__add_user_expansion_access_changed_admin_action.sql for
-- the same pattern applied to the previous new action type.
--
-- Unlike every existing AdminActionType, a content-flagging action has no target user -- it
-- targets game content (weapons, cards, adversaries, ...), which admin_action_log has no
-- column for and does not need one for; the affected type/ids/count are captured in `details`,
-- matching the free-form `key=value` shape AdminUserService#recordAction already uses. So
-- target_user_id, NOT NULL since the table was created, must become nullable for this one
-- action type. Every existing action type continues to always populate it.

ALTER TABLE admin_action_log ALTER COLUMN target_user_id DROP NOT NULL;

ALTER TABLE admin_action_log DROP CONSTRAINT chk_admin_action_log_action;

ALTER TABLE admin_action_log ADD CONSTRAINT chk_admin_action_log_action CHECK (action IN (
    'USER_BANNED',
    'USER_UNBANNED',
    'USER_ROLE_CHANGED',
    'USER_USERNAME_CHANGED',
    'USER_AVATAR_CHANGED',
    'USER_EXPANSION_ACCESS_CHANGED',
    'CONTENT_SRD_CHANGED'
));
