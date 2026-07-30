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
public class ConditionResponse {

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
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * Whether this condition is from official game content.
     */
    private Boolean isOfficial;

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
}
