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
 * Snapshot of a single adversary instance being tracked within an {@link EncounterRun}.
 * <p>
 * Copied from the source {@link EncounterAdversary} at run start ({@code label} and
 * {@code tierOverride}), plus fresh per-run live state ({@code hitPointsMarked},
 * {@code stressMarked}, {@code isDefeated}, {@code note}) that starts at zero/unset regardless
 * of anything on the template. This is a snapshot, not a live view: once a run starts, editing
 * the saved encounter's adversaries has no effect on it.
 * </p>
 * <p>
 * {@link #adversary} is a read-only reference to the catalog stat block. Live combat state is
 * never written back to it -- two instances of the same adversary in one encounter (or across
 * different users' runs) share that row.
 * </p>
 */
@Entity
@Table(name = "encounter_run_adversaries")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterRunAdversary extends BaseEntity {

    /**
     * The run this instance belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_run_id", nullable = false)
    private EncounterRun encounterRun;

    /**
     * The catalog adversary this instance is a stat block reference to. Read-only: live
     * HP/Stress/defeated state is tracked on this row, never written back here.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adversary_id", nullable = false)
    private Adversary adversary;

    /**
     * GM nickname copied from the source instance at run start, e.g. "Archer A".
     */
    @Column(name = "label", length = 100)
    private String label;

    /**
     * Retier target copied from the source instance at run start. Statistics for the effective
     * tier are computed on read from {@link com.aboff.core.model.dh.ImprovisedTierStatistics},
     * same as {@link EncounterAdversary#getTierOverride()}. HP and Stress maximums are not
     * affected by retiering -- see that table's javadoc.
     */
    @Column(name = "tier_override")
    private Integer tierOverride;

    /**
     * Hit points currently marked during this run. Clamped to the adversary's
     * {@code hitPointMax} by the service layer, never by a fixed column constraint (the max
     * varies per adversary).
     */
    @Column(name = "hit_points_marked", nullable = false)
    @Builder.Default
    private Integer hitPointsMarked = 0;

    /**
     * Stress currently marked during this run. Clamped to the adversary's {@code stressMax} by
     * the service layer.
     */
    @Column(name = "stress_marked", nullable = false)
    @Builder.Default
    private Integer stressMarked = 0;

    /**
     * Whether this instance has been defeated during the run.
     */
    @Column(name = "is_defeated", nullable = false)
    @Builder.Default
    private Boolean isDefeated = false;

    /**
     * Free-text GM note for this instance during the run -- conditions, positioning, and the
     * like. Distinct from anything on the source encounter.
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * Display order of this instance within the run's adversary list, copied from the source
     * instance at run start.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
