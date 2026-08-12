package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new AncestryCard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAncestryCardRequest {
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
     * Whether this card is SRD-licensed content. Optional and defaults to false; only ADMIN+
     * callers may actually set it true — see {@code ContentAccessService#resolveSrd}. Left
     * optional (not {@code @NotNull}) because existing bulk-import payloads omit it.
     */
    private Boolean srd;

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
}
