package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing Experience.
 * <p>
 * All fields are optional to support partial updates. Only non-null fields
 * will be updated on the experience entity.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExperienceRequest {

    /**
     * Updated description of the experience.
     * If null, the description will not be changed.
     */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * Updated bonus modifier for this experience.
     * If null, the modifier will not be changed.
     */
    private Integer modifier;
}
