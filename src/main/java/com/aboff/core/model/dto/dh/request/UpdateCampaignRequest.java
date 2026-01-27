package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Campaign.
 * <p>
 * Contains optional fields for updating campaign information.
 * Only non-null fields are updated (partial update support).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignRequest {

    /**
     * The campaign's new name (optional).
     * If provided, must not be blank and must not exceed 200 characters.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * The campaign's new description (optional).
     * If provided, must not exceed 2000 characters.
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}
