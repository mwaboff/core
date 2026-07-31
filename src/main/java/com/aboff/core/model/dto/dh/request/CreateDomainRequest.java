package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDomainRequest {
    /**
     * Name of the domain
     */
    @NotBlank(message = "Domain name is required")
    @Size(max = 100, message = "Domain name must not exceed 100 characters")
    private String name;

    /**
     * URL to the icon representing this domain
     */
    @Size(max = 500, message = "Icon URL must not exceed 500 characters")
    private String iconUrl;

    /**
     * Detailed description of the domain
     */
    private String description;

    /**
     * Whether this domain is from official game content. Defaults to true when omitted
     */
    private Boolean isOfficial;

    /**
     * ID of the expansion this domain belongs to
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;
}
