package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a Game Master granting or revoking a character's access to transformations,
 * and optionally assigning or clearing the character's transformation card in the same call.
 * <p>
 * Transformations are something a GM grants, so this request is only accepted on the
 * campaign-scoped GM endpoint. Players cannot set these values through the character sheet
 * update endpoint.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransformationAccessRequest {

    /**
     * Whether transformations are enabled for the character. Required.
     * Disabling only hides the transformation panel; it never discards the character's
     * existing transformation selection.
     */
    @NotNull(message = "Enabled is required")
    private Boolean enabled;

    /**
     * ID of the transformation card to assign to the character. Optional.
     * Ignored when {@link #clearTransformationCard} is true. A card may be assigned while
     * {@link #enabled} is false so a GM can pre-load a transformation before revealing it.
     */
    private Long transformationCardId;

    /**
     * Explicit flag to detach the character's transformation card, clearing
     * {@code transformationCardId}, {@code transformationTokens}, and {@code wolfFormActive}.
     * Takes precedence over {@link #transformationCardId}, mirroring the tri-state convention
     * used by {@code UpdateCharacterSheetRequest}.
     */
    private Boolean clearTransformationCard;
}
