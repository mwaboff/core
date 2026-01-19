package com.aboff.core.model.dto.dh.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Expansion entities.
 * Represents content packs or expansions in the Daggerheart TTRPG system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpansionResponse {
    /**
     * Unique identifier for the expansion
     */
    private Long id;

    /**
     * Name of the expansion (e.g., "Core Rulebook", "Twilight Mirage")
     */
    private String name;

    /**
     * Whether this expansion is published and available for use
     */
    private Boolean isPublished;

    /**
     * Timestamp when the expansion was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the expansion was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the expansion was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
