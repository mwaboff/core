package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for TransformationCard entities.
 * Represents transformation cards in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns expansionId and featureIds only
 * - With ?expand=expansion: includes full expansion object
 * - With ?expand=features: includes full feature objects
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransformationCardResponse implements Restrictable {

    /**
     * Unique identifier for the transformation card.
     */
    private Long id;

    /**
     * Name of the transformation card.
     */
    private String name;

    /**
     * Detailed description of the transformation card and its effects.
     */
    private String description;

    /**
     * Whether this transformation card is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this transformation card is SRD-licensed content, freely usable without owning
     * the sourcebook it was printed in.
     */
    private Boolean srd;

    /**
     * ID of the expansion this transformation card belongs to (always included).
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * IDs of features associated with this transformation card (always included).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * IDs of questions associated with this transformation card (always included).
     */
    private List<Long> questionIds;

    /**
     * Full question objects (included only when ?expand=questions is specified).
     */
    private List<QuestionResponse> questions;

    /**
     * Timestamp when the transformation card was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the transformation card was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the transformation card was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * The display name of the expansion this transformation card belongs to. Set on a redacted
     * stub so the caller can tell which book to buy, even though {@link #expansion} itself is
     * unset.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;
}
