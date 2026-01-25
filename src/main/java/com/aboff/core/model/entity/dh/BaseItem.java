package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Abstract base class for all item types in the Daggerheart TTRPG system.
 * <p>
 * This class uses {@code @MappedSuperclass} strategy, where each item type
 * (Weapon, Armor, Loot) has its own independent table with all fields,
 * including those inherited from BaseItem.
 * </p>
 * <p>
 * Unlike the Card hierarchy which uses JOINED inheritance, items don't share
 * enough common fields to benefit from a shared table, and type-safe references
 * are preferred (weapon originalId should reference weapons only, not all items).
 * </p>
 * <p>
 * Provides common functionality for all items:
 * </p>
 * <ul>
 *   <li>Basic identification (name)</li>
 *   <li>Expansion association</li>
 *   <li>Official vs custom content tracking</li>
 *   <li>User ownership for custom items</li>
 *   <li>Soft delete support</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseItem extends BaseEntity {

    /**
     * The name of the item.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The expansion this item belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Indicates whether this item is from official game content.
     * Custom items created by users will have this set to false.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * The user who created this item.
     * Null for official content, populated for custom items.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * Timestamp indicating when this item was soft-deleted.
     * If null, the item is active and available for use.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this item has been soft-deleted.
     *
     * @return true if the item is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the item by setting the deleted_at timestamp to the current time.
     * The item remains in the database but will be filtered out from normal queries.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted item by clearing the deleted_at timestamp.
     * The item becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
