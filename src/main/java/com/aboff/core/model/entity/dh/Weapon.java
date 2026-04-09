package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.Trait;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a weapon in the Daggerheart TTRPG system.
 * <p>
 * Weapons are equipped by characters for combat and have various properties
 * that determine their effectiveness in different situations:
 * </p>
 * <ul>
 *   <li><strong>Primary/Secondary:</strong> Whether this is a main weapon or off-hand weapon</li>
 *   <li><strong>Trait:</strong> The character trait (AGILITY, STRENGTH, etc.) used to attack with this weapon</li>
 *   <li><strong>Range:</strong> The effective distance category (MELEE, CLOSE, FAR, etc.)</li>
 *   <li><strong>Burden:</strong> Whether the weapon requires one or two hands to wield</li>
 *   <li><strong>Damage:</strong> The damage dice and type dealt on a successful hit</li>
 *   <li><strong>Features:</strong> Optional special abilities or effects granted by the weapon</li>
 * </ul>
 * <p>
 * Custom weapons can be created by users as copies of official weapons,
 * with the {@code originalWeapon} field tracking the source weapon.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.WEAPON)
@Table(name = "weapons")
@AssociationOverride(
    name = "features",
    joinTable = @JoinTable(
        name = "weapon_features",
        joinColumns = @JoinColumn(name = "weapon_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Weapon extends BaseItem {

    /**
     * Indicates whether this is a primary weapon (true) or secondary weapon (false).
     * Primary weapons are typically wielded in the main hand, while secondary weapons
     * are used in the off-hand or as backup weapons.
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    /**
     * The trait used to attack with this weapon.
     * Determines which character attribute contributes to attack rolls.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Trait trait;

    /**
     * The effective range category of the weapon.
     * Determines the distance at which the weapon can be used effectively.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Range range;

    /**
     * The burden type indicating how many hands are required to wield the weapon.
     * ONE_HANDED weapons can be used with a shield or second weapon,
     * while TWO_HANDED weapons require both hands but often deal more damage.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Burden burden;

    /**
     * The damage roll for this weapon, including dice type, count, modifier, and damage type.
     * Uses the embedded DamageRoll component which maps to multiple database columns.
     */
    @Embedded
    private DamageRoll damage;

    /**
     * Reference to the original official weapon if this is a custom weapon.
     * Null for official weapons, populated when a user creates a custom copy.
     * This allows tracking the source of custom content and maintaining relationships.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_weapon_id")
    private Weapon originalWeapon;
}
