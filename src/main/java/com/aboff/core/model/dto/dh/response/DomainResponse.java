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
public class DomainResponse implements Restrictable {
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
     * Whether this domain is from official game content
     */
    private Boolean isOfficial;

    /**
     * Whether this domain is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in.
     */
    private Boolean srd;

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

    /**
     * The display name of the expansion this domain belongs to. Set on a redacted stub so the
     * caller can tell which book to buy, even though {@link #expansion} itself is unset.
     */
    private String expansionName;

    /**
     * True if this response is a redacted stub for gated non-SRD content the caller may not
     * view. When true, every other field except {@link #id} and {@link #expansionName} is
     * unset.
     */
    private Boolean restricted;
}
