package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
 *   <li>Expansion association (null for custom items)</li>
 *   <li>Official vs custom content tracking, and public visibility</li>
 *   <li>User ownership for custom items</li>
 *   <li>Feature associations (multiple features per item)</li>
 *   <li>Campaign sharing for custom items</li>
 *   <li>Soft delete support</li>
 * </ul>
 * <p>
 * Subclasses must use {@code @AssociationOverrides} to specify their own join tables
 * for the {@code features} and {@code campaigns} relationships (e.g. weapon_features
 * and weapon_campaigns).
 * </p>
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
     * The tier level of the item (1–4).
     * For weapons and armor, this represents the power tier.
     * For loot, this maps to rarity: 1=Common, 2=Uncommon, 3=Rare, 4=Legendary.
     */
    @Column(name = "tier", nullable = false)
    private Integer tier;

    /**
     * The sourcebook this item was published in.
     * <p>
     * Null for custom items, which came from no book. A database constraint enforces
     * the one direction that matters: official content must name its expansion.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id")
    private Expansion expansion;

    /**
     * Indicates whether this item is from official game content.
     * Custom items created by users will have this set to false.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * Indicates whether this custom item is visible to every user.
     * <p>
     * Only MODERATOR and above may set this. Official content is universally visible
     * regardless of this flag, so official rows leave it false.
     * </p>
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * The user who created this item.
     * Null for official content, populated for custom items.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    /**
     * The features associated with this item.
     * <p>
     * Subclasses override the join table name via {@code @AssociationOverride}. The
     * {@code @Builder.Default} matters: without it Lombok ignores the field initializer and a
     * builder-created item carries a null collection, so callers that reasonably expect an
     * empty set — copying an item, counting its features — fail with a
     * {@link NullPointerException}.
     * </p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "item_features",
        joinColumns = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    /**
     * Campaigns this item has been explicitly shared with.
     * <p>
     * Everyone involved in a tagged campaign can see and equip the item, even when it
     * is neither official nor public. Sharing is deliberate: an untagged custom item
     * stays private to its creator. Subclasses override the join table name via
     * {@code @AssociationOverride}.
     * </p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "item_campaigns",
        joinColumns = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "campaign_id")
    )
    @Builder.Default
    private Set<Campaign> campaigns = new HashSet<>();

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
