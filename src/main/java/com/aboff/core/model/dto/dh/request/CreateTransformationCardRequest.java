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
 * Request DTO for creating a new TransformationCard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransformationCardRequest {

    /**
     * Name of the transformation card.
     */
    @NotBlank(message = "Transformation card name is required")
    @Size(max = 200, message = "Transformation card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the transformation card and its effects.
     */
    private String description;

    /**
     * ID of the expansion this transformation card belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * IDs of existing features to associate with this transformation card.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;
}
