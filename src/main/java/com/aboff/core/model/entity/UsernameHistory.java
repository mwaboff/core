package com.aboff.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Records a single username change for a user.
 * <p>
 * Rows are written both when a user sets their own username (during initial
 * OAuth selection or later edits) and when an admin updates a user's
 * username. The {@code changedByUserId} column captures who made the change,
 * or is {@code null} for system-driven renames.
 * </p>
 */
@Entity
@Table(name = "username_history")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameHistory extends BaseEntity {

    /**
     * The id of the user whose username changed.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Username value prior to the change.
     */
    @Column(name = "previous_username", nullable = false, length = 100)
    private String previousUsername;

    /**
     * Username value after the change.
     */
    @Column(name = "new_username", nullable = false, length = 100)
    private String newUsername;

    /**
     * Id of the user who made the change, or {@code null} for system-driven
     * renames.
     */
    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    /**
     * Moment the change took effect. Distinct from the inherited
     * {@code createdAt} column to leave that free for Hibernate bookkeeping.
     */
    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
