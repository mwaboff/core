package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new Encounter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEncounterRequest {

    /**
     * Name of the encounter.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * General description of the encounter.
     */
    private String description;

    /**
     * Power tier of the encounter (1-4).
     * Null if the encounter spans multiple tiers.
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * Optional ID of the campaign this encounter belongs to.
     */
    private Long campaignId;

    /**
     * Optional ID of the environment (scene stat block) this encounter takes place in.
     */
    private Long environmentId;

    /**
     * Whether this encounter is publicly visible to other users.
     */
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * The number of PCs in combat, manually entered by the GM.
     * Drives the suggested Battle Point budget and Minion grouping. Never derived from a
     * campaign roster.
     */
    @Min(value = 1, message = "Party size must be at least 1")
    @Max(value = 12, message = "Party size must not exceed 12")
    private Integer partySize;

    /**
     * Battle Point adjustment: -1, the fight should be less difficult or shorter.
     */
    private Boolean adjustmentEasier;

    /**
     * Battle Point adjustment: -2, using 2 or more Solo adversaries.
     */
    private Boolean adjustmentTwoPlusSolos;

    /**
     * Battle Point adjustment: -2, adding +1d4 (or a static +2) to all adversaries' damage rolls.
     */
    private Boolean adjustmentBonusDamage;

    /**
     * Battle Point adjustment: +1, choosing an adversary from a lower tier.
     */
    private Boolean adjustmentLowerTier;

    /**
     * Battle Point adjustment: +1, including no Bruisers, Hordes, Leaders, or Solos.
     */
    private Boolean adjustmentNoElites;

    /**
     * Battle Point adjustment: +2, the fight should be more dangerous or last longer.
     */
    private Boolean adjustmentHarder;

    /**
     * List of adversary instances to include in the encounter, each optionally carrying a
     * GM label and a retier target. Each entry represents a single adversary instance; to
     * include multiple instances of the same adversary, add multiple entries with the same
     * {@code adversaryId}.
     * <p>
     * Preferred over the deprecated {@link #adversaryIds}. If both are provided, this field
     * wins and {@link #adversaryIds} is ignored.
     * </p>
     */
    @Valid
    private List<AdversaryEntry> adversaries;

    /**
     * Deprecated: list of bare adversary IDs to include in the encounter.
     * Each entry represents a single adversary instance with no label or retier target. Kept
     * for backward compatibility with existing clients; prefer {@link #adversaries}, which
     * also supports a label and tier override per instance.
     */
    @Valid
    @Deprecated
    private List<Long> adversaryIds;

    /**
     * A single adversary instance to include in an encounter, with optional GM-facing metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdversaryEntry {

        /**
         * The adversary to include.
         */
        @NotNull(message = "Adversary ID is required")
        private Long adversaryId;

        /**
         * Optional GM nickname for this instance, e.g. "Archer A".
         */
        @Size(max = 100, message = "Label must not exceed 100 characters")
        private String label;

        /**
         * Optional retier target (1-4) for this instance.
         */
        @Min(value = 1, message = "Tier override must be at least 1")
        @Max(value = 4, message = "Tier override must not exceed 4")
        private Integer tierOverride;
    }
}
