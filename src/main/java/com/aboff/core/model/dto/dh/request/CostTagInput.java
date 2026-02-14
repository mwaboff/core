package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.CostTagCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for finding or creating a cost tag by label.
 * Used in card create/update requests to allow clients to specify cost tags by label
 * instead of (or in addition to) existing tag IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostTagInput {

    /**
     * The display label for the cost tag (e.g., "3 Hope", "1/session").
     * Matched case-insensitively against existing tags.
     */
    @NotBlank(message = "Cost tag label is required")
    @Size(max = 200, message = "Cost tag label must not exceed 200 characters")
    private String label;

    /**
     * The category of the cost tag. Required when creating a new tag;
     * ignored if an existing tag with a matching label is found.
     */
    @NotNull(message = "Cost tag category is required")
    private CostTagCategory category;
}
