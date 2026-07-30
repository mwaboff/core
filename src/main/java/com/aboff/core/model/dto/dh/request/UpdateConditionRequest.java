package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Condition. All fields are optional;
 * only non-null fields are applied to the entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConditionRequest {

    /**
     * Name of the condition.
     */
    @Size(max = 200, message = "Condition name must not exceed 200 characters")
    private String name;

    /**
     * The rules text describing the condition's effect.
     */
    private String description;

    /**
     * ID of the expansion this condition belongs to.
     */
    private Long expansionId;

    /**
     * Whether this condition is from official game content.
     */
    private Boolean isOfficial;
}
