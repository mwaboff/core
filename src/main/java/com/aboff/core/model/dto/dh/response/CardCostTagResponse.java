package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CostTagCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for CardCostTag entities.
 * Represents a cost, limitation, or timing tag that can be displayed on cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardCostTagResponse {
    /**
     * Unique identifier for the cost tag
     */
    private Long id;

    /**
     * Display label for the cost tag (e.g., "3 Hope", "1/session")
     */
    private String label;

    /**
     * Category of the cost tag (COST, LIMITATION, TIMING)
     */
    private CostTagCategory category;

    /**
     * Timestamp when the cost tag was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the cost tag was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the cost tag was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
