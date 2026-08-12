package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CardType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for AncestryCard entities.
 * Represents an ancestry card in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns IDs only for relationships
 * - With ?expand=expansion: includes full expansion object
 * - With ?expand=features: includes full feature objects
 * - Multiple expansions can be comma-separated: ?expand=expansion,features
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AncestryCardResponse implements Restrictable {
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
     * Type of card (always ANCESTRY for this type)
     */
    private CardType cardType;

    /**
     * ID of the expansion this card belongs to (always included)
     */
    private Long expansionId;

    /**
     * Name of the expansion this card belongs to (always included). On a redacted stub, this is
     * the only content-identifying field carried, so the frontend can tell the viewer which book
     * to buy without exposing the card's real content.
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
     * Whether this card is SRD-licensed content, freely usable without owning the sourcebook it
     * was printed in.
     */
    private Boolean srd;

    /**
     * Whether this ancestry card represents a mixed ancestry
     */
    private Boolean isMixed;

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

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * browse directly. When true, every field except {@code id}, {@code cardType}, and
     * {@code expansionName} is omitted from the response.
     */
    private Boolean restricted;
}
