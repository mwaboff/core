package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Encounter entities.
 * Represents encounters (groups of adversaries for combat) in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * </p>
 * <ul>
 *   <li>By default: returns relationship IDs only</li>
 *   <li>With ?expand=creator: includes creator user object</li>
 *   <li>With ?expand=campaign: includes campaign object</li>
 *   <li>With ?expand=environment: includes environment object</li>
 *   <li>With ?expand=originalEncounter: includes full original encounter object</li>
 *   <li>With ?expand=adversaryDetails: includes full adversary objects in adversaries list</li>
 *   <li>Multiple expansions can be comma-separated</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterResponse implements Restrictable {

    /**
     * Unique identifier for the encounter.
     */
    private Long id;

    /**
     * Name of the encounter.
     */
    private String name;

    /**
     * General description of the encounter.
     */
    private String description;

    /**
     * Power tier of the encounter (1-4).
     * Null if the encounter spans multiple tiers.
     */
    private Integer tier;

    /**
     * Whether this encounter is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this encounter is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Never populated on a redacted stub.
     */
    private Boolean srd;

    /**
     * Name of the expansion this encounter's licensing is drawn from, for a redacted stub. Null
     * in the normal (non-restricted) response -- unlike the other four GM content types,
     * {@code Encounter} has no {@code Expansion} relation of its own, since encounters are
     * user-authored GM tools rather than printed book content.
     */
    private String expansionName;

    /**
     * Whether this encounter is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * ID of the campaign this encounter belongs to (null if standalone).
     */
    private Long campaignId;

    /**
     * Full campaign object (included only when ?expand=campaign is specified).
     */
    private CampaignResponse campaign;

    /**
     * ID of the environment (scene stat block) this encounter takes place in (null if none).
     */
    private Long environmentId;

    /**
     * Full environment object (included only when ?expand=environment is specified).
     */
    private EnvironmentResponse environment;

    /**
     * ID of the original encounter if this is a copy (null if original).
     */
    private Long originalEncounterId;

    /**
     * Full original encounter object (included only when ?expand=originalEncounter is specified).
     */
    private EncounterResponse originalEncounter;

    /**
     * ID of the user who created this encounter (always included).
     */
    private Long creatorId;

    /**
     * Full creator user object (included only when ?expand=creator is specified).
     */
    private UserResponse creator;

    /**
     * List of adversary instances in the encounter.
     * Always includes adversary IDs.
     * Full adversary objects included only when ?expand=adversaryDetails is specified.
     */
    private List<EncounterAdversaryResponse> adversaries;

    /**
     * The number of PCs in combat, manually entered by the GM (null until set).
     * Drives {@link #suggestedBattlePoints} and Minion grouping in {@link #spentBattlePoints}.
     */
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
     * The suggested Battle Point budget: {@code (3 * partySize) + 2}, adjusted by whichever of
     * the six adjustment flags above are set. Calculated server-side.
     */
    private Integer suggestedBattlePoints;

    /**
     * The total Battle Points actually spent by this encounter's adversary instances, with
     * Minions billed per group of {@code partySize} rather than individually. Calculated
     * server-side.
     */
    private Integer spentBattlePoints;

    /**
     * Timestamp when the encounter was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the encounter was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the encounter was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * browse directly. When true, every field except {@code id}, {@code expansionName}, and
     * this one is omitted from the response. In practice this only applies to encounters marked
     * official (never true for a user's own custom encounter, regardless of its {@code srd}
     * flag) -- see {@code EncounterService#toResponse}.
     */
    private Boolean restricted;

    /**
     * Nested DTO for adversary instances in the encounter.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EncounterAdversaryResponse {

        /**
         * Unique identifier for this encounter adversary instance.
         */
        private Long id;

        /**
         * ID of the adversary (always included).
         */
        private Long adversaryId;

        /**
         * Full adversary object (included only when ?expand=adversaryDetails is specified).
         */
        private AdversaryResponse adversary;

        /**
         * Optional GM nickname for this instance, e.g. "Archer A".
         */
        private String label;

        /**
         * Optional retier target (1-4) for this instance (null if not retiered).
         */
        private Integer tierOverride;

        /**
         * Statistics for this instance's effective tier, computed on read from
         * {@link com.aboff.core.model.dh.ImprovisedTierStatistics}. Only present when
         * {@link #tierOverride} is set.
         */
        private RetieredStatisticsResponse retieredStatistics;

        /**
         * Display order of this instance within the encounter's adversary list.
         */
        private Integer displayOrder;
    }

    /**
     * Nested DTO for the derived statistics of a retiered adversary instance, computed from
     * {@link com.aboff.core.model.dh.ImprovisedTierStatistics}.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RetieredStatisticsResponse {

        /**
         * The tier these statistics apply to.
         */
        private Integer tier;

        /**
         * The attack modifier for this tier.
         */
        private Integer attackModifier;

        /**
         * The Difficulty for this tier.
         */
        private Integer difficulty;

        /**
         * The Major damage threshold for this tier.
         */
        private Integer majorThreshold;

        /**
         * The Severe damage threshold for this tier.
         */
        private Integer severeThreshold;

        /**
         * The printed damage dice range for this tier, as display text (e.g. "1d6+2 - 1d12+4").
         */
        private String damageDiceRange;
    }
}
