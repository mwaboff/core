package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a condition in the Daggerheart TTRPG system.
 * <p>
 * Conditions are named, catalogued rules effects that can be applied to a character
 * (e.g., Restrained, Vulnerable, Drained, Hexed, Chained, Ignited). A condition describes
 * the rules text of the effect itself; a character's actual instance of a condition — including
 * a per-instance {@code magnitude} for conditions that stack — is tracked separately by
 * {@link CharacterSheetCondition}.
 * </p>
 * <p>
 * Follows the same official/custom content pattern as {@code Weapon}/{@code Armor}/{@code Loot}:
 * {@code isOfficial} distinguishes rulebook content from user-created conditions, and bulk import
 * respects the caller-supplied value rather than hardcoding it, so official conditions can be
 * bulk-imported from the rulebook.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.CONDITION)
@Table(name = "conditions")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Condition extends BaseEntity {

    /**
     * The condition's name (e.g., "Restrained", "Vulnerable").
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The rules text describing the condition's effect.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The expansion this condition belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Indicates whether this condition is from official game content.
     * Custom conditions created by users will have this set to false.
     */
    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = false;

    /**
     * Indicates whether this condition is SRD-licensed content, freely usable without
     * owning the sourcebook it was printed in. Defaults to false at creation time; only an
     * explicit SRD flag opens the condition to users who have not been granted expansion
     * access. See {@code ContentAccessService} for how this is enforced.
     */
    @Column(name = "srd", nullable = false)
    @Builder.Default
    private Boolean srd = false;

    /**
     * The user who created this condition.
     * Null for official content, populated for custom conditions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Timestamp indicating when this condition was soft-deleted.
     * If null, the condition is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this condition has been soft-deleted.
     *
     * @return true if the condition is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the condition by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted condition.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
