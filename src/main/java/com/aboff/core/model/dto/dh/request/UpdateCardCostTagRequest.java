package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.CostTagCategory;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing CardCostTag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardCostTagRequest {
    /**
     * Display label for the cost tag
     */
    @Size(max = 200, message = "Label must not exceed 200 characters")
    private String label;

    /**
     * Category of the cost tag
     */
    private CostTagCategory category;
}
