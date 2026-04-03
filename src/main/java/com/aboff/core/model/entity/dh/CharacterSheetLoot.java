package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing the association between a character sheet and a loot item
 * in the Daggerheart TTRPG system.
 * <p>
 * This join entity tracks which loot items a character possesses in their inventory.
 * Unlike weapons and armor, loot items do not have equipped status or slot assignments.
 * </p>
 */
@Entity
@Table(name = "character_sheet_inventory_items")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetLoot extends BaseEntity {

    /**
     * The character sheet that owns this loot association.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The loot item associated with this character.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loot_id", nullable = false)
    private Loot loot;
}
