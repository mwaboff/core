package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.FeatureType;
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
 * Input DTO for finding or creating a feature by name.
 * Used in card, item, and adversary create/update requests to allow clients to specify
 * features inline instead of (or in addition to) existing feature IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureInput {

    /** Name of the feature. Matched case-insensitively against existing features within the same expansion and type. */
    @NotBlank(message = "Feature name is required")
    @Size(max = 200, message = "Feature name must not exceed 200 characters")
    private String name;

    /** Detailed description of what the feature does. */
    private String description;

    /** Type/category of the feature. */
    @NotNull(message = "Feature type is required")
    private FeatureType featureType;

    /** ID of the expansion this feature belongs to. */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /** IDs of cost tags associated with this feature. */
    private List<Long> costTagIds;

    /** Cost tags to find or create by label. Merged with costTagIds if both provided. */
    @Valid
    private List<CostTagInput> costTags;

    /** IDs of existing modifiers to associate with this feature. */
    private List<Long> modifierIds;

    /** Modifiers to find or create by (target, operation, value). Merged with modifierIds if both provided. */
    @Valid
    private List<FeatureModifierInput> modifiers;
}
