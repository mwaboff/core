package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CardType;
import com.aboff.core.model.enums.SubclassLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for SubclassCard entities.
 * Represents a subclass card in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns IDs only for relationships
 * - With ?expand=expansion: includes full expansion object
 * - With ?expand=features: includes full feature objects
 * - With ?expand=subclassPath: includes full subclass path object
 * - Multiple expansions can be comma-separated: ?expand=expansion,features,subclassPath
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubclassCardResponse {
    /**
     * Unique identifier for the card
     */
    private Long id;

    /**
     * Name of the card
     */
    private String name;

    /**
     * Detailed description of the card
     */
    private String description;

    /**
     * Type of card (always SUBCLASS for this type)
     */
    private CardType cardType;

    /**
     * ID of the expansion this card belongs to (always included)
     */
    private Long expansionId;

    /**
     * Name of the expansion this card belongs to (always included)
     */
    private String expansionName;

    /**
     * Full expansion object (included only when ?expand=expansion is specified)
     */
    private ExpansionResponse expansion;

    /**
     * Whether this card is from official game content
     */
    private Boolean isOfficial;

    /**
     * URL to the background image for this card
     */
    private String backgroundImageUrl;

    /**
     * IDs of features granted by this card (always included)
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified)
     */
    private List<FeatureResponse> features;

    /**
     * IDs of cost tags associated with this card (always included)
     */
    private List<Long> costTagIds;

    /**
     * Full cost tag objects (included only when ?expand=costTags is specified)
     */
    private List<CardCostTagResponse> costTags;

    /**
     * ID of the class associated with this card's subclass path (always included)
     */
    private Long associatedClassId;

    /**
     * Name of the class associated with this card's subclass path (always included)
     */
    private String associatedClassName;

    /**
     * ID of the subclass path this card belongs to (always included)
     */
    private Long subclassPathId;

    /**
     * Name of the subclass path this card belongs to (always included)
     */
    private String subclassPathName;

    /**
     * Full subclass path object (included only when ?expand=subclassPath is specified)
     */
    private SubclassPathResponse subclassPath;

    /**
     * Names of the domains associated with this card's subclass path (always included)
     */
    private List<String> domainNames;

    /**
     * IDs of the domains associated with this card's subclass path (always included)
     */
    private List<Long> domainIds;

    /**
     * The spellcasting trait for this card's subclass path (always included, null if no spellcasting)
     */
    private SubclassPathResponse.TraitInfo spellcastingTrait;

    /**
     * The level at which this subclass becomes available
     */
    private SubclassLevel level;

    /**
     * Timestamp when the card was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the card was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the card was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
