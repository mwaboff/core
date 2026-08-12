package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Condition entities.
 * Represents named rules effects (e.g., Restrained, Vulnerable) in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConditionResponse implements Restrictable {

    /**
     * Unique identifier for the condition.
     */
    private Long id;

    /**
     * Name of the condition.
     */
    private String name;

    /**
     * The rules text describing the condition's effect.
     */
    private String description;

    /**
     * ID of the expansion this condition belongs to (always included).
     */
    private Long expansionId;

    /**
     * Name of the expansion this condition belongs to (always included). On a redacted stub,
     * this is the only content-identifying field carried, so the frontend can tell the viewer
     * which book to buy without exposing the condition's real content.
     */
    private String expansionName;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * Whether this condition is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this condition is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Never populated on a redacted stub.
     */
    private Boolean srd;

    /**
     * Timestamp when the condition was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the condition was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the condition was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * True when this response is a redacted stub for gated non-SRD content the caller may not
     * browse directly. When true, every field except {@code id}, {@code expansionName}, and
     * this one is omitted from the response.
     */
    private Boolean restricted;
}
