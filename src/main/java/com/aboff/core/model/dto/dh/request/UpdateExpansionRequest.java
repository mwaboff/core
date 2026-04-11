package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Expansion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpansionRequest {
    /**
     * Name of the expansion
     */
    @Size(max = 255, message = "Expansion name must not exceed 255 characters")
    private String name;

    /**
     * Whether this expansion is published and available for use
     */
    private Boolean isPublished;
}
