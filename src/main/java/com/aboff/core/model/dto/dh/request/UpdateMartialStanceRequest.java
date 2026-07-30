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
 * Request DTO for updating an existing MartialStance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMartialStanceRequest {

    /**
     * Name of the martial stance.
     */
    @Size(max = 200, message = "Martial stance name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this martial stance belongs to.
     */
    private Long expansionId;

    /**
     * The tier level of the martial stance (1–4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this martial stance is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Effect text of the martial stance.
     */
    private String description;

    /**
     * Optional list of existing feature IDs to associate with this martial stance.
     */
    private List<Long> featureIds;

    /**
     * Optional list of features to find or create inline.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * Optional ID of the original martial stance if this is a custom copy.
     */
    private Long originalMartialStanceId;
}
