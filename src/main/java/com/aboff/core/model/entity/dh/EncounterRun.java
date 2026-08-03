package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.EncounterRunStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a live "run" of an encounter in the Daggerheart TTRPG system.
 * <p>
 * Starting a run snapshots the source {@link Encounter}'s adversary instances into
 * {@link EncounterRunAdversary} rows, so editing the saved encounter mid-fight cannot corrupt a
 * run already in progress. All live combat state -- marked HP/Stress, defeated, GM notes --
 * lives on those snapshot rows, never on the catalog {@link Adversary}, which two instances of
 * the same adversary in one encounter would otherwise share.
 * </p>
 * <p>
 * {@code campaign} is deliberately nullable: running a fight is campaign-free by design (see
 * the design doc's "campaign-free constraint"). A campaign tag only widens who else can see and
 * mutate the run via {@code CampaignService.hasGameMasterAccess} -- it is never required to
 * start or play one.
 * </p>
 * <p>
 * Runs hard-delete, like {@link Countdown}: this is small, ephemeral session state rather than
 * durable content, so there is no soft-delete field.
 * </p>
 */
@Entity
@Table(name = "encounter_runs")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterRun extends BaseEntity {

    /**
     * The encounter this run was started from. The relationship is read-only after the run
     * starts -- only used to trace a run back to its source, never to re-derive instances.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    /**
     * The optional campaign this run is tagged to. Null for a standalone run. When set, the
     * campaign's game masters gain visibility and mutation rights via
     * {@code CampaignService.hasGameMasterAccess}, in addition to {@link #startedBy}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    /**
     * The user who started this run. Always has full access to it, regardless of campaign tag.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by_id", nullable = false)
    private User startedBy;

    /**
     * The run's lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EncounterRunStatus status;

    /**
     * When the run was started.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When the run was marked complete. Null while {@link #status} is
     * {@link EncounterRunStatus#ACTIVE}.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * The snapshotted adversary instances being tracked in this run.
     */
    @OneToMany(mappedBy = "encounterRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EncounterRunAdversary> encounterRunAdversaries = new ArrayList<>();
}
