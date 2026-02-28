package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a feature modifier in the Daggerheart TTRPG system.
 * <p>
 * Feature modifiers define numerical adjustments to character attributes. Each modifier
 * specifies a target attribute, a mathematical operation, and a value. Modifiers are
 * shared across features via a many-to-many relationship, allowing the same modifier
 * (e.g., "+1 Strength") to be reused by multiple features.
 * </p>
 * <p>
 * A unique constraint on (target, operation, value) for active (non-deleted) records
 * prevents duplicate modifier definitions.
 * </p>
 */
@Entity
@Table(name = "feature_modifiers")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureModifier extends BaseEntity {

    /**
     * The character attribute this modifier targets.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModifierTarget target;

    /**
     * The mathematical operation applied to the target attribute.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModifierOperation operation;

    /**
     * The numerical value used in the modifier operation.
     */
    @Column(name = "\"value\"", nullable = false)
    private Integer value;

    /**
     * Timestamp indicating when this modifier was soft-deleted.
     * If null, the modifier is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this modifier has been soft-deleted.
     *
     * @return true if the modifier is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the modifier by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted modifier.
     */
    public void restore() {
        this.deletedAt = null;
    }
}