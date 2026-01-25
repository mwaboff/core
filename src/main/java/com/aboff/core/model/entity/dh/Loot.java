package com.aboff.core.model.entity.dh;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing general loot items in the Daggerheart TTRPG system.
 * <p>
 * Loot represents miscellaneous items that characters can find, purchase, or carry
 * that don't fall into the specialized categories of weapons or armor. This includes:
 * </p>
 * <ul>
 *   <li>Consumables (potions, scrolls, food)</li>
 *   <li>Tools and equipment (rope, torches, lock picks)</li>
 *   <li>Treasure and valuables (gems, art objects, currency)</li>
 *   <li>Quest items and special objects</li>
 *   <li>Miscellaneous gear and supplies</li>
 * </ul>
 * <p>
 * Unlike weapons and armor, loot items are primarily defined by their name and description,
 * with game mechanics handled through narrative rather than specific numerical values.
 * </p>
 * <p>
 * Custom loot can be created by users as copies of official items or as entirely new items,
 * with the {@code originalLoot} field tracking the source item if applicable.
 * </p>
 */
@Entity
@Table(name = "loot")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Loot extends BaseItem {

    /**
     * Detailed description of the loot item.
     * Describes what the item is, its appearance, potential uses, and any relevant
     * narrative or mechanical effects. Can be quite lengthy for complex or important items.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Reference to the original official loot if this is a custom item.
     * Null for official loot or completely new custom items.
     * Populated when a user creates a custom copy of an existing loot item.
     * This allows tracking the source of custom content and maintaining relationships.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_loot_id")
    private Loot originalLoot;
}
