package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new Campaign.
 * <p>
 * Contains fields required to create a campaign in the Daggerheart TTRPG system,
 * including basic information and optional initial game masters and players.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {

    /**
     * The campaign's name.
     * This is the primary identifier for the campaign and is displayed prominently.
     */
    @NotBlank(message = "Campaign name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * The campaign's description (optional).
     * Provides additional context about the campaign setting, rules, or other details.
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /**
     * IDs of users to add as game masters (optional).
     * The campaign creator is automatically added as a game master.
     */
    private List<Long> gameMasterIds;

    /**
     * IDs of users to add as players (optional).
     */
    private List<Long> playerIds;
}
