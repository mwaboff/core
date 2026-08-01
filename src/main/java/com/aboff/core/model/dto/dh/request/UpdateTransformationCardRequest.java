package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing TransformationCard.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransformationCardRequest {

    /**
     * Name of the transformation card.
     */
    @Size(max = 200, message = "Transformation card name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of the transformation card and its effects.
     */
    private String description;

    /**
     * ID of the expansion this transformation card belongs to.
     */
    private Long expansionId;

    /**
     * IDs of existing features to associate with this transformation card.
     * Replaces existing features when provided.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * IDs of existing questions to associate with this transformation card.
     * Replaces existing questions when provided.
     */
    private List<Long> questionIds;

    /**
     * Questions to find or create inline. Merged with questionIds if both provided.
     */
    @Valid
    private List<QuestionInput> questions;
}
