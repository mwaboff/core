package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CardType;
import com.aboff.core.model.enums.DomainCardType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for DomainCard entities.
 * Represents a domain card in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns IDs only for relationships
 * - With ?expand=expansion: includes full expansion object
 * - With ?expand=features: includes full feature objects
 * - With ?expand=associatedDomain: includes full domain object
 * - Multiple expansions can be comma-separated: ?expand=expansion,features,associatedDomain
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainCardResponse {
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
     * Type of card (always DOMAIN for this type)
     */
    private CardType cardType;

    /**
     * ID of the expansion this card belongs to (always included)
     */
    private Long expansionId;

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
     * ID of the associated domain (always included)
     */
    private Long associatedDomainId;

    /**
     * Full domain object (included only when ?expand=associatedDomain is specified)
     */
    private DomainResponse associatedDomain;

    /**
     * The level requirement for this domain card
     */
    private Integer level;

    /**
     * The cost to recall/use this card
     */
    private Integer recallCost;

    /**
     * The type of domain card
     */
    private DomainCardType type;

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
