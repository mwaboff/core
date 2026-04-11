package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DomainCardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing DomainCard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDomainCardRequest {
    /**
     * Name of the card
     */
    @Size(max = 200, message = "Card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the card
     */
    private String description;

    /**
     * ID of the expansion this card belongs to
     */
    private Long expansionId;

    /**
     * Whether this card is from official game content
     */
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
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * IDs of cost tags associated with this card
     */
    private List<Long> costTagIds;

    /**
     * Cost tags to find or create by label. Merged with costTagIds if both provided.
     */
    @Valid
    private List<CostTagInput> costTags;

    /**
     * ID of the domain this card is associated with
     */
    private Long associatedDomainId;

    /**
     * The level requirement for this domain card
     */
    private Integer level;

    /**
     * The cost to recall/use this card
     */
    @PositiveOrZero(message = "Recall cost must be zero or positive")
    private Integer recallCost;

    /**
     * The type of domain card
     */
    private DomainCardType type;
}
