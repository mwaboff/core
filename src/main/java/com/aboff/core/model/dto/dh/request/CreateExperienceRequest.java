package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Experience.
 * <p>
 * Experiences represent significant events, accomplishments, and learning moments
 * for a character or companion in the Daggerheart TTRPG system. Each experience provides a
 * modifier (default +2) that applies when the character/companion attempts actions related
 * to that experience.
 * </p>
 * <p>
 * Exactly one of characterSheetId or companionId must be provided.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperienceRequest {

    /**
     * ID of the character sheet this experience belongs to.
     * Either this or companionId must be provided, but not both.
     */
    private Long characterSheetId;

    /**
     * ID of the companion this experience belongs to.
     * Either this or characterSheetId must be provided, but not both.
     */
    private Long companionId;

    /**
     * Detailed description of the experience.
     * <p>
     * This narrative text describes what happened, what the character learned,
     * or how this experience shaped the character. The description helps both
     * the player and GM remember the context and determine when the modifier
     * should apply.
     * </p>
     */
    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * The bonus modifier granted by this experience.
     * <p>
     * This value is added to relevant dice rolls when the character attempts
     * actions where this experience would be applicable. Defaults to +2 if not
     * specified, which is the standard experience bonus in Daggerheart.
     * </p>
     */
    @Builder.Default
    private Integer modifier = 2;
}
