package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a character advancement log entry in the Daggerheart TTRPG system.
 * <p>
 * Each log entry records a single level-up event including the level transition,
 * tier, and a JSON blob containing all advancement choices and previous values
 * needed for undo operations.
 * </p>
 */
@Entity
@Table(name = "character_advancement_logs")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterAdvancementLog extends BaseEntity {

    /**
     * The character sheet this advancement log belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The level the character was at before this advancement.
     */
    @Column(name = "from_level", nullable = false)
    private Integer fromLevel;

    /**
     * The level the character reached after this advancement.
     */
    @Column(name = "to_level", nullable = false)
    private Integer toLevel;

    /**
     * The tier of the target level.
     */
    @Column(nullable = false)
    private Integer tier;

    /**
     * JSON string containing all advancement choices and previous values for undo.
     * <p>
     * This includes the advancements chosen, tier achievements applied,
     * domain card changes, trades, and snapshot of previous values.
     * </p>
     */
    @Column(name = "advancement_data", nullable = false, columnDefinition = "TEXT")
    private String advancementData;
}
