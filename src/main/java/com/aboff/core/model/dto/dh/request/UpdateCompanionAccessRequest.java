package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a Game Master granting or revoking a character's access to
 * <strong>creating new</strong> companions.
 * <p>
 * Companions are something a GM grants access to, so this request is only accepted on the
 * campaign-scoped GM endpoint. Unlike {@link UpdateTransformationAccessRequest}, there is no
 * companion assignment to make here and no player-side write gate to bypass: disabling this
 * flag never hides, disables, or orphans a companion that already exists -- it only stops a
 * new one from being created (see the companions implementation plan, section 3.4).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanionAccessRequest {

    /**
     * Whether creating new companions is enabled for the character. Required.
     * Disabling only stops new companions from being created; it never removes or hides an
     * existing one.
     */
    @NotNull(message = "Enabled is required")
    private Boolean enabled;
}
