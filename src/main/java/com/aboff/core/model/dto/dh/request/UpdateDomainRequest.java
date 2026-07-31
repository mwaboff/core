package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDomainRequest {
    /**
     * Name of the domain
     */
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
     * Whether this domain is from official game content
     */
    private Boolean isOfficial;

    /**
     * ID of the expansion this domain belongs to
     */
    private Long expansionId;
}
