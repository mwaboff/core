package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Condition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConditionRequest {

    /**
     * Name of the condition (e.g., "Restrained", "Vulnerable").
     */
    @NotBlank(message = "Condition name is required")
    @Size(max = 200, message = "Condition name must not exceed 200 characters")
    private String name;

    /**
     * The rules text describing the condition's effect.
     */
    private String description;

    /**
     * ID of the expansion this condition belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this condition is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this condition is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Optional; only ADMIN+ callers may actually set it to true --
     * see {@code ContentAccessService#resolveSrd}.
     */
    private Boolean srd;
}
