package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.ViciousAxis;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single Training pick for one companion during a character level-up.
 * <p>
 * Distinct from {@code CreateCompanionTrainingRequest} (the manual/GM path,
 * {@code POST /api/dh/companions/{id}/trainings}): this variant travels inside
 * {@link LevelUpRequest} so it participates in the same {@code advancementData}
 * log/reversal as every other level-up change, and {@code acquiredAtLevel} is the level the
 * character is levelling up *to*, not read from the sheet at request time.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionTrainingChoice {

    /**
     * The companion this Training pick applies to.
     */
    @NotNull(message = "companionId is required")
    private Long companionId;

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
