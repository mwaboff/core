package com.aboff.core.model.entity;

import com.aboff.core.model.enums.AdminActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Durable record of an admin-initiated action against a user.
 * <p>
 * Written synchronously alongside the state change it audits, in the same
 * transaction, so that a post-commit crash cannot lose audit rows. Parallel
 * to the SLF4J {@link com.aboff.core.service.AuditLogger}; this table is the
 * queryable-by-user-id surface used by the admin UI.
 * </p>
 */
@Entity
@Table(name = "admin_action_log")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLog extends BaseEntity {

    /**
     * Id of the admin who performed the action. Nullable so that deleting an
     * admin account does not cascade-remove the audit trail.
     */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /**
     * Id of the user the action was performed against.
     * <p>
     * Null for actions with no user target, e.g. {@link AdminActionType#CONTENT_SRD_CHANGED}
     * — every other action type always populates this.
     * </p>
     */
    @Column(name = "target_user_id")
    private Long targetUserId;

    /**
     * Type of administrative action recorded.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminActionType action;

    /**
     * Free-form {@code key=value; key2=value2} description of the change.
     * Matches the structured-log format used by {@code AuditLogger}.
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * IP address the action was initiated from.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
