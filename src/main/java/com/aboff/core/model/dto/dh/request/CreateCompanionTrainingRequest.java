package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.ViciousAxis;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a single Training selection to a companion via the manual/GM path
 * ({@code POST /api/dh/companions/{id}/trainings}).
 * <p>
 * Distinct from the level-up flow: {@code acquiredAtLevel} is not accepted here and is instead
 * set by the server to the owning character sheet's current level, since manually-added
 * Training is intentionally outside the level-up advancement log and is never reversed by
 * level-down.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanionTrainingRequest {

    /**
     * Which Training option to select.
     */
    @NotNull(message = "Training option is required")
    private CompanionTrainingOption option;

    /**
     * Which ladder to advance. Required if and only if {@code option} is {@code VICIOUS}.
     */
    private ViciousAxis viciousAxis;

    /**
     * The id of the Experience to grant a permanent +1 to. Required if and only if
     * {@code option} is {@code INTELLIGENT}, and must belong to the target companion.
     */
    private Long targetExperienceId;
}
