package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a mixed ancestry card.
 * <p>
 * Mixed ancestry cards combine features from exactly two different ancestries,
 * allowing players to represent characters with diverse heritage.
 * Mixed ancestry cards are always non-official user-created content.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMixedAncestryCardRequest {

    /**
     * Name of the mixed ancestry card
     */
    @NotBlank(message = "Card name is required")
    @Size(max = 200, message = "Card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the mixed ancestry card
     */
    private String description;

    /**
     * ID of the expansion this card belongs to
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * IDs of the two features to include in the mixed ancestry.
     * Exactly two feature IDs must be provided.
     */
    @NotNull(message = "Feature IDs are required")
    @Size(min = 2, max = 2, message = "Exactly two feature IDs must be provided")
    private List<Long> featureIds;

    /**
     * URL to the background image for this card
     */
    @Size(max = 500, message = "Background image URL must not exceed 500 characters")
    private String backgroundImageUrl;
}
