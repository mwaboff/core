package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DomainCardType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new DomainCard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDomainCardRequest {
    /**
     * Name of the card
     */
    @NotBlank(message = "Card name is required")
    @Size(max = 200, message = "Card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the card
     */
    private String description;

    /**
     * ID of the expansion this card belongs to
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this card is from official game content
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * URL to the background image for this card
     */
    @Size(max = 500, message = "Background image URL must not exceed 500 characters")
    private String backgroundImageUrl;

    /**
     * IDs of features granted by this card
     */
    private List<Long> featureIds;

    /**
     * IDs of cost tags associated with this card
     */
    private List<Long> costTagIds;

    /**
     * ID of the domain this card is associated with
     */
    @NotNull(message = "Associated domain ID is required")
    private Long associatedDomainId;

    /**
     * The level requirement for this domain card
     */
    @NotNull(message = "Level is required")
    @Positive(message = "Level must be positive")
    private Integer level;

    /**
     * The cost to recall/use this card
     */
    @NotNull(message = "Recall cost is required")
    @PositiveOrZero(message = "Recall cost must be zero or positive")
    private Integer recallCost;

    /**
     * The type of domain card
     */
    @NotNull(message = "Domain card type is required")
    private DomainCardType type;
}
