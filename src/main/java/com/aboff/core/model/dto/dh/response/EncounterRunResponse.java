package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.EncounterRunStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a live run of an encounter.
 * <p>
 * {@code GET /api/dh/encounter-runs/{runId}} always includes each instance's full stat block
 * (the GM needs the whole card to play); the list endpoint (
 * {@code GET /api/dh/encounter-runs}) omits it to keep a multi-run listing lightweight.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterRunResponse {

    private Long id;

    /** ID of the encounter this run was started from. */
    private Long encounterId;

    /**
     * ID of the source encounter's environment (scene stat block), or null if the encounter has
     * none set. Only the ID is included -- fetch the full stat block via
     * {@code GET /api/dh/environments/{id}?expand=features} when needed.
     */
    private Long environmentId;

    /** ID of the campaign this run is tagged to (null for a standalone run). */
    private Long campaignId;

    /** ID of the user who started this run. */
    private Long startedById;

    private EncounterRunStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** The run's snapshotted, live-tracked adversary instances, in display order. */
    private List<EncounterRunAdversaryResponse> adversaries;

    private LocalDateTime createdAt;

    private LocalDateTime lastModifiedAt;

    /**
     * Nested DTO for a single adversary instance's live state within a run.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EncounterRunAdversaryResponse {

        private Long id;

        /** ID of the catalog adversary this instance references (always included). */
        private Long adversaryId;

        /** Full adversary stat block. Only present on the single-run GET. */
        private AdversaryResponse adversary;

        /** GM nickname copied from the source instance at run start, e.g. "Archer A". */
        private String label;

        /** Retier target (1-4) copied from the source instance, null if not retiered. */
        private Integer tierOverride;

        /**
         * Derived statistics for the effective tier, computed on read from
         * {@link com.aboff.core.model.dh.ImprovisedTierStatistics}. Only present when
         * {@link #tierOverride} is set.
         */
        private EncounterResponse.RetieredStatisticsResponse retieredStatistics;

        private Integer hitPointsMarked;

        /** The adversary's hit point maximum, for rendering bounds without expanding {@link #adversary}. */
        private Integer hitPointMax;

        private Integer stressMarked;

        /** The adversary's stress maximum, for rendering bounds without expanding {@link #adversary}. */
        private Integer stressMax;

        private Boolean isDefeated;

        private String note;

        /**
         * Tokens placed on this instance's stat block (Daggerheart Core ch. 4, "Adversary
         * Tokens" -- e.g. the {@code Slow} passive, or Hope &amp; Fear's {@code Pool} feature).
         * Always included, on both the single-run and list endpoints -- unlike {@link #adversary},
         * it's cheap and useful at a glance in a run list. Not clamped to any maximum.
         */
        private Integer tokens;

        private Integer displayOrder;
    }
}
