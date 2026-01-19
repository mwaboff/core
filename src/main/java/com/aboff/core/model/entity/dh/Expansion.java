package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing an expansion or content pack in the Daggerheart TTRPG system.
 * <p>
 * Expansions group related cards, classes, domains, and other game content together.
 * They can be marked as published or unpublished to control visibility.
 * </p>
 */
@Entity
@Table(name = "expansions")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Expansion extends BaseEntity {

    /**
     * The name of the expansion.
     * Must be unique and not null.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Indicates whether this expansion is published and available to users.
     * Unpublished expansions may be in development or draft state.
     */
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    /**
     * Timestamp indicating when this expansion was soft-deleted.
     * If null, the expansion is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this expansion has been soft-deleted.
     *
     * @return true if the expansion is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the expansion by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted expansion.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
