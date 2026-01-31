package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Encounter.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEncounterRequest {

    /**
     * Name of the encounter.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * General description of the encounter.
     */
    private String description;

    /**
     * Power tier of the encounter (1-4).
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
    private Boolean isPublic;

    /**
     * List of adversaries to replace the current adversaries in the encounter.
     * If provided, completely replaces the existing adversary list.
     */
    @Valid
    private List<CreateEncounterRequest.EncounterAdversaryRequest> adversaries;
}
