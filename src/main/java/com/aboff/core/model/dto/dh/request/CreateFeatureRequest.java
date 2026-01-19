package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.FeatureType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Feature.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeatureRequest {
    /**
     * Name of the feature
     */
    @NotBlank(message = "Feature name is required")
    @Size(max = 200, message = "Feature name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of what the feature does
     */
    private String description;

    /**
     * Type/category of the feature
     */
    @NotNull(message = "Feature type is required")
    private FeatureType featureType;

    /**
     * ID of the expansion this feature belongs to
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;
}
