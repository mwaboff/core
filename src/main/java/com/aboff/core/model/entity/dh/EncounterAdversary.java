package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Join entity representing a single adversary instance within an encounter.
 * <p>
 * This entity tracks individual adversary instances in encounters.
 * Each entry represents one unique adversary, so multiple instances of the
 * same adversary type require multiple EncounterAdversary records.
 * </p>
 */
@Entity
@Table(name = "encounter_adversaries")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterAdversary extends BaseEntity {

    /**
     * The encounter this adversary belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    /**
     * The adversary included in the encounter.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adversary_id", nullable = false)
    private Adversary adversary;

    /**
     * Optional GM nickname for this specific instance, e.g. "Archer A".
     * Distinguishes multiple instances of the same adversary during a run.
     */
    @Column(name = "label", length = 100)
    private String label;

    /**
     * Optional retier target (1-4) for this instance.
     * When set, the instance's effective stats are computed on read from
     * {@link com.aboff.core.model.dh.ImprovisedTierStatistics} rather than stored, so the
     * derived values can never drift from the book's retier table.
     */
    @Column(name = "tier_override")
    private Integer tierOverride;

    /**
     * Display order of this instance within the encounter's adversary list.
     */
    @Column(name = "display_order", nullable = false)
    @lombok.Builder.Default
    private Integer displayOrder = 0;
}
