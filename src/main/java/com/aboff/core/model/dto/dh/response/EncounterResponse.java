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
public class EncounterResponse {

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
     * Total battle points for this encounter (calculated).
     * Used for encounter balancing.
     */
    private Integer totalBattlePoints;

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
    }
}
