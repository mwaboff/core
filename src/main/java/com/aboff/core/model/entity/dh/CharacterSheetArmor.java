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
 * Entity representing the association between a character sheet and an armor piece
 * in the Daggerheart TTRPG system.
 * <p>
 * This join entity tracks which armor pieces a character possesses in their inventory
 * and whether each piece is currently equipped.
 * </p>
 */
@Entity
@Table(name = "character_sheet_inventory_armors")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetArmor extends BaseEntity {

    /**
     * The character sheet that owns this armor association.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The armor piece associated with this character.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armor_id", nullable = false)
    private Armor armor;

    /**
     * Whether this armor piece is currently equipped.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean equipped = false;
}
