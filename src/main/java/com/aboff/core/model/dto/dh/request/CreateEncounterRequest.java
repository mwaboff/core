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
     * List of adversary IDs to include in the encounter.
     * Each entry represents a single adversary instance.
     * To include multiple instances of the same adversary, include the ID multiple times.
     */
    @Valid
    private List<Long> adversaryIds;
}
