package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Expansion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpansionRequest {
    /**
     * Name of the expansion
     */
    @NotBlank(message = "Expansion name is required")
    @Size(max = 255, message = "Expansion name must not exceed 255 characters")
    private String name;

    /**
     * Whether this expansion is published and available for use
     */
    @NotNull(message = "Published status is required")
    private Boolean isPublished;
}
