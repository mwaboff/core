package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.FeatureType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Feature entities.
 * Represents special abilities and traits in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns expansionId, costTagIds, and modifierIds only
 * - With ?expand=expansion: includes full expansion object
 * - With ?expand=costTags: includes full cost tag objects
 * - With ?expand=modifiers: includes full modifier objects
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureResponse {
    /**
     * Unique identifier for the feature
     */
    private Long id;

    /**
     * Name of the feature
     */
    private String name;

    /**
     * Detailed description of what the feature does
     */
    private String description;

    /**
     * Type/category of the feature (HOPE, ANCESTRY, CLASS, COMMUNITY, DOMAIN, OTHER)
     */
    private FeatureType featureType;

    /**
     * ID of the expansion this feature belongs to (always included)
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified)
     */
    private ExpansionResponse expansion;

    /**
     * IDs of cost tags associated with this feature (always included)
     */
    private List<Long> costTagIds;

    /**
     * Full cost tag objects (included only when ?expand=costTags is specified)
     */
    private List<CardCostTagResponse> costTags;

    /**
     * IDs of modifiers associated with this feature (always included)
     */
    private List<Long> modifierIds;

    /**
     * Full modifier objects (included only when ?expand=modifiers is specified)
     */
    private List<FeatureModifierResponse> modifiers;

    /**
     * Timestamp when the feature was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the feature was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the feature was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
