package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.FeatureTiming;
import com.aboff.core.model.enums.FeatureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Feature.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureRequest {
    /**
     * Name of the feature
     */
    @Size(max = 200, message = "Feature name must not exceed 200 characters")
    private String name;

    /**
     * Detailed description of what the feature does
     */
    private String description;

    /**
     * Type/category of the feature
     */
    private FeatureType featureType;

    /**
     * Timing tag for the feature (e.g. Action, Reaction). Optional; omit for features
     * with no timing.
     */
    private FeatureTiming timing;

    /**
     * ID of the expansion this feature belongs to
     */
    private Long expansionId;

    /**
     * Whether this feature is SRD-licensed content. Optional and ADMIN+ only — see
     * {@code ContentAccessService#resolveSrd}.
     * <p>
     * Note there is deliberately no {@code isOfficial} field here — see
     * {@link CreateFeatureRequest#getSrd()}'s Javadoc for why.
     * </p>
     */
    private Boolean srd;

    /**
     * IDs of cost tags associated with this feature
     */
    private List<Long> costTagIds;

    /**
     * Cost tags to find or create by label. Merged with costTagIds if both provided.
     */
    @Valid
    private List<CostTagInput> costTags;

    /**
     * IDs of existing modifiers to associate with this feature
     */
    private List<Long> modifierIds;

    /**
     * Modifiers to find or create by (target, operation, value). Merged with modifierIds if both provided.
     */
    @Valid
    private List<FeatureModifierInput> modifiers;
}
