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
 * Request DTO for creating a new CardCostTag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardCostTagRequest {
    /**
     * Display label for the cost tag
     */
    @NotBlank(message = "Label is required")
    @Size(max = 200, message = "Label must not exceed 200 characters")
    private String label;

    /**
     * Category of the cost tag
     */
    @NotNull(message = "Category is required")
    private CostTagCategory category;

    /**
     * Whether this cost tag is SRD-licensed content. Only ADMIN/OWNER may set this to true;
     * see {@code ContentAccessService#resolveSrd}. Optional and omittable so existing bulk
     * import payloads that predate SRD gating continue to work unchanged.
     */
    private Boolean srd;
}
