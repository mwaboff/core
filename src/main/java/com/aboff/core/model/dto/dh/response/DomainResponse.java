package com.aboff.core.model.dto.dh.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Domain entities.
 * Represents magical/thematic categories in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * - By default: returns expansionId only
 * - With ?expand=expansion: includes full expansion object
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainResponse {
    /**
     * Unique identifier for the domain
     */
    private Long id;

    /**
     * Name of the domain (e.g., "Fire", "Ice", "Nature")
     */
    private String name;

    /**
     * URL to the icon representing this domain
     */
    private String iconUrl;

    /**
     * Detailed description of the domain
     */
    private String description;

    /**
     * ID of the expansion this domain belongs to (always included)
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified)
     */
    private ExpansionResponse expansion;

    /**
     * Timestamp when the domain was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the domain was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the domain was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
