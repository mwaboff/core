package com.aboff.core.service.dh;

import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.ViciousAxis;

/**
 * Shared validator for a proposed {@link CompanionTrainingOption} pick against one companion.
 * <p>
 * Enforces exactly the rules that apply identically whether the pick is being made through the
 * manual/GM training endpoints ({@code CompanionService}) or through the character level-up
 * flow: the option's own per-companion-lifetime cap ({@link CompanionTrainingOption#getMaxSelections()}),
 * and the two option-specific preconditions ({@code VICIOUS} needs an axis not already at its
 * ladder cap; {@code INTELLIGENT} needs a target Experience belonging to this companion). It
 * does <strong>not</strong> enforce how many picks are available in a given level-up -- that is
 * a level-up-specific concern owned by the level-up work, layered on top of this check.
 * </p>
 * <p>
 * Pure and stateless: every fact it needs (a companion's existing trainings and experiences) is
 * already loaded on the {@link Companion} passed in, so this class takes no dependencies and
 * performs no queries of its own.
 * </p>
 */
public final class CompanionTrainingValidator {

    private CompanionTrainingValidator() {
        // Static utility class - prevent instantiation
    }

    /**
     * Validates that a proposed Training pick is legal for the given companion.
     *
     * @param companion the companion the pick would be added to; its current {@code trainings}
     *                  and {@code experiences} collections determine what is still legal
     * @param option the Training option being picked
     * @param viciousAxis the ladder ({@code DAMAGE_DIE} or {@code RANGE}) to advance, required
     *                     if and only if {@code option} is {@link CompanionTrainingOption#VICIOUS}
     * @param targetExperienceId the id of the Experience to grant a permanent +1, required if
     *                            and only if {@code option} is {@link CompanionTrainingOption#INTELLIGENT}
     * @throws IllegalStateException if the option has no remaining selections, if a required
     *                                {@code viciousAxis}/{@code targetExperienceId} is missing or
     *                                invalid, if the chosen Vicious axis is already at its ladder
     *                                cap, or if the target Experience does not belong to this
     *                                companion
     */
    public static void validatePick(
            Companion companion,
            CompanionTrainingOption option,
            ViciousAxis viciousAxis,
            Long targetExperienceId) {

        int remaining = CompanionDerivationService.remainingByOption(companion).getOrDefault(option, 0);
        if (remaining <= 0) {
            throw new IllegalStateException(
                    "Companion has no remaining " + option + " selections available");
        }

        if (option == CompanionTrainingOption.VICIOUS) {
            validateVicious(companion, viciousAxis);
        } else if (option == CompanionTrainingOption.INTELLIGENT) {
            validateIntelligent(companion, targetExperienceId);
        }
    }

    /**
     * Validates the {@code VICIOUS}-specific precondition: an axis must be supplied, and that
     * axis's derived ladder value must not already be at its cap.
     *
     * @param companion the companion the pick would be added to
     * @param viciousAxis the ladder to advance
     * @throws IllegalStateException if {@code viciousAxis} is null or already at its cap
     */
    private static void validateVicious(Companion companion, ViciousAxis viciousAxis) {
        if (viciousAxis == null) {
            throw new IllegalStateException("A viciousAxis is required for the VICIOUS training option");
        }
        if (viciousAxis == ViciousAxis.DAMAGE_DIE
                && CompanionDerivationService.damageDice(companion) == DiceType.D12) {
            throw new IllegalStateException("Companion's damage dice is already at its maximum (D12)");
        }
        if (viciousAxis == ViciousAxis.RANGE
                && CompanionDerivationService.attackRange(companion) == Range.VERY_FAR) {
            throw new IllegalStateException("Companion's attack range is already at its maximum (VERY_FAR)");
        }
    }

    /**
     * Validates the {@code INTELLIGENT}-specific precondition: a target Experience id must be
     * supplied and must belong to this companion.
     *
     * @param companion the companion the pick would be added to
     * @param targetExperienceId the id of the Experience to grant a permanent +1
     * @throws IllegalStateException if {@code targetExperienceId} is null or does not match any
     *                                 of this companion's own Experiences
     */
    private static void validateIntelligent(Companion companion, Long targetExperienceId) {
        if (targetExperienceId == null) {
            throw new IllegalStateException("A targetExperienceId is required for the INTELLIGENT training option");
        }
        boolean belongsToCompanion = companion.getExperiences().stream()
                .map(Experience::getId)
                .anyMatch(targetExperienceId::equals);
        if (!belongsToCompanion) {
            throw new IllegalStateException(
                    "targetExperienceId " + targetExperienceId + " does not belong to this companion");
        }
    }
}
