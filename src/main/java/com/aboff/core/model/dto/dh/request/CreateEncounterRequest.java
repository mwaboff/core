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
     * Whether this encounter is publicly visible to other users.
     */
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * List of adversaries to include in the encounter with their counts.
     */
    @Valid
    private List<EncounterAdversaryRequest> adversaries;

    /**
     * Nested DTO for adversary assignment with count.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncounterAdversaryRequest {

        /**
         * ID of the adversary to include.
         */
        @NotNull(message = "Adversary ID is required")
        private Long adversaryId;

        /**
         * Number of this adversary type in the encounter.
         */
        @NotNull(message = "Count is required")
        @Min(value = 1, message = "Count must be at least 1")
        @Builder.Default
        private Integer count = 1;
    }
}
