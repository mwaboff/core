package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing the association between a character sheet and a weapon
 * in the Daggerheart TTRPG system.
 * <p>
 * This join entity tracks which weapons a character possesses in their inventory,
 * whether each weapon is currently equipped, and which slot it occupies
 * (PRIMARY or SECONDARY) when equipped.
 * </p>
 */
@Entity
@Table(name = "character_sheet_inventory_weapons")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetWeapon extends BaseEntity {

    /**
     * The character sheet that owns this weapon association.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The weapon associated with this character.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weapon_id", nullable = false)
    private Weapon weapon;

    /**
     * Whether this weapon is currently equipped.
     * Equipped weapons must have a slot assignment.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;

    /**
     * The equipment slot this weapon occupies when equipped.
     * Valid values are "PRIMARY" or "SECONDARY". Must be null when not equipped.
     */
    @Column(length = 20)
    private String slot;
}
