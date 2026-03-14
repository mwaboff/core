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
 * Each log entry records a single level-up event for a character, tracking which level
 * transition occurred, the tier at the time of advancement, and the serialized advancement
 * data describing the choices made during leveling.
 * </p>
 * <p>
 * Advancement logs provide an audit trail of all level-up decisions, enabling features
 * such as viewing advancement history and potentially supporting level respec in the future.
 * </p>
 */
@Entity
@Table(name = "character_advancement_log")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterAdvancementLog extends BaseEntity {

    /**
     * The character sheet this advancement log entry belongs to.
     * When the character sheet is deleted, all associated advancement logs are also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The level the character was at before this advancement (1-9).
     */
    @Column(name = "from_level", nullable = false)
    private Integer fromLevel;

    /**
     * The level the character reached after this advancement (2-10).
     * Must be exactly one greater than fromLevel.
     */
    @Column(name = "to_level", nullable = false)
    private Integer toLevel;

    /**
     * The tier at which this advancement occurred (2-4).
     * Tier determines which advancement options are available.
     */
    @Column(nullable = false)
    private Integer tier;

    /**
     * Serialized JSON data describing the advancement choices made.
     * Contains details about which advancement types were selected and
     * any associated parameters (e.g., which traits were boosted).
     */
    @Column(name = "advancement_data", nullable = false, columnDefinition = "TEXT")
    private String advancementData;
}
