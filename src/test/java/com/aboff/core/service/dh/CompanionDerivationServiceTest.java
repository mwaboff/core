package com.aboff.core.service.dh;

import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.ViciousAxis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CompanionDerivationService}.
 * <p>
 * Covers every Training option's derived effect, both Vicious axes, ladder saturation at the
 * printed caps (D12 and Very Far), the "out of scene" boundary, remaining-pick counts, and
 * the companion-granted Hope slot sum (including that it ignores soft-deleted companions).
 * </p>
 */
class CompanionDerivationServiceTest {

    // ==================== EVASION (AWARE) ====================

    @Test
    void evasion_NoAwareTrainings_ReturnsBaseEvasion() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, Set.of());

        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(10);
    }

    @Test
    void evasion_OneAwareTraining_AddsTwo() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.AWARE, null, 2)));

        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(12);
    }

    @Test
    void evasion_ThreeAwareTrainings_AddsSix() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.AWARE, null, 2),
                        training(CompanionTrainingOption.AWARE, null, 3),
                        training(CompanionTrainingOption.AWARE, null, 4)));

        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(16);
    }

    @Test
    void evasion_OtherTrainingsPresent_IgnoresNonAware() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.RESILIENT, null, 2),
                        training(CompanionTrainingOption.BONDED, null, 2)));

        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(10);
    }

    // ==================== STRESS MAX (RESILIENT) ====================

    @Test
    void stressMax_NoResilientTrainings_ReturnsBaseStressMax() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, Set.of());

        assertThat(CompanionDerivationService.stressMax(companion)).isEqualTo(3);
    }

    @Test
    void stressMax_OneResilientTraining_AddsOne() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.RESILIENT, null, 2)));

        assertThat(CompanionDerivationService.stressMax(companion)).isEqualTo(4);
    }

    @Test
    void stressMax_ThreeResilientTrainings_AddsThree() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.RESILIENT, null, 2),
                        training(CompanionTrainingOption.RESILIENT, null, 3),
                        training(CompanionTrainingOption.RESILIENT, null, 4)));

        assertThat(CompanionDerivationService.stressMax(companion)).isEqualTo(6);
    }

    // ==================== DAMAGE DICE (VICIOUS / DAMAGE_DIE) ====================

    @Test
    void damageDice_NoViciousTrainings_ReturnsBaseDamageDice() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, Set.of());

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D6);
    }

    @Test
    void damageDice_OneViciousDamageDieTraining_StepsToD8() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D8);
    }

    @Test
    void damageDice_TwoViciousDamageDieTrainings_StepsToD10() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 3)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D10);
    }

    @Test
    void damageDice_ThreeViciousDamageDieTrainings_SaturatesAtD12() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 3),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 4)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D12);
    }

    @Test
    void damageDice_ViciousOnRangeAxisOnly_DoesNotStepDamageDice() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D6);
    }

    @Test
    void damageDice_AtCapWithFurtherPicksSomehowRecorded_NeverThrowsAndStaysAtCap() {
        // Defensive: even an over-selected companion (shouldn't happen once validation is
        // wired in by a later work package) must never throw or wrap past the cap.
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 3),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 4),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 5)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D12);
    }

    // ==================== ATTACK RANGE (VICIOUS / RANGE) ====================

    @Test
    void attackRange_NoViciousTrainings_ReturnsBaseAttackRange() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, Set.of());

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.MELEE);
    }

    @Test
    void attackRange_OneViciousRangeTraining_StepsToVeryClose() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.VERY_CLOSE);
    }

    @Test
    void attackRange_TwoViciousRangeTrainings_StepsToClose() {
        // Matches the rulebook's own worked example: "Close to Far" is exactly one step.
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.CLOSE, Set.of(
                training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.FAR);
    }

    @Test
    void attackRange_FourViciousRangeTrainings_SaturatesAtVeryFar() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 3),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 4),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 5)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.VERY_FAR);
    }

    @Test
    void attackRange_FiveViciousRangeTrainings_NeverThrowsAndStaysAtCap() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 3),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 4),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 5),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 6)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.VERY_FAR);
    }

    @Test
    void attackRange_ViciousOnDamageDieAxisOnly_DoesNotStepAttackRange() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.MELEE);
    }

    @Test
    void attackRange_BaseNotOnLadder_ReturnsBaseUnchanged() {
        // OUT_OF_RANGE is deliberately excluded from the range ladder; a companion somehow
        // based there (should never happen via the normal creation flow) must not throw.
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.OUT_OF_RANGE,
                Set.of(training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 2)));

        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.OUT_OF_RANGE);
    }

    // ==================== BOTH VICIOUS AXES TOGETHER ====================

    @Test
    void bothViciousAxes_StepIndependently() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.DAMAGE_DIE, 2),
                        training(CompanionTrainingOption.VICIOUS, ViciousAxis.RANGE, 3)));

        assertThat(CompanionDerivationService.damageDice(companion)).isEqualTo(DiceType.D8);
        assertThat(CompanionDerivationService.attackRange(companion)).isEqualTo(Range.VERY_CLOSE);
    }

    // ==================== OUT OF SCENE ====================

    @Test
    void outOfScene_StressBelowMax_ReturnsFalse() {
        Companion companion = companionWithStress(2, 3);

        assertThat(CompanionDerivationService.outOfScene(companion)).isFalse();
    }

    @Test
    void outOfScene_StressExactlyAtMax_ReturnsTrue() {
        Companion companion = companionWithStress(3, 3);

        assertThat(CompanionDerivationService.outOfScene(companion)).isTrue();
    }

    @Test
    void outOfScene_StressAboveMax_ReturnsTrue() {
        Companion companion = companionWithStress(4, 3);

        assertThat(CompanionDerivationService.outOfScene(companion)).isTrue();
    }

    @Test
    void outOfScene_UsesDerivedStressMaxNotBase() {
        // baseStressMax 3 + one Resilient = derived max 4; marked 3 is not yet out of scene.
        Companion companion = Companion.builder()
                .baseEvasion(10)
                .baseStressMax(3)
                .baseDamageDice(DiceType.D6)
                .baseAttackRange(Range.MELEE)
                .stressMarked(3)
                .trainings(Set.of(training(CompanionTrainingOption.RESILIENT, null, 2)))
                .build();

        assertThat(CompanionDerivationService.outOfScene(companion)).isFalse();
    }

    // ==================== REMAINING BY OPTION ====================

    @Test
    void remainingByOption_NoTrainings_ReturnsFullCapsForEveryOption() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, Set.of());

        Map<CompanionTrainingOption, Integer> remaining = CompanionDerivationService.remainingByOption(companion);

        assertThat(remaining).hasSize(CompanionTrainingOption.values().length);
        for (CompanionTrainingOption option : CompanionTrainingOption.values()) {
            assertThat(remaining.get(option)).isEqualTo(option.getMaxSelections());
        }
    }

    @Test
    void remainingByOption_PartiallyTaken_SubtractsCount() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(
                        training(CompanionTrainingOption.AWARE, null, 2),
                        training(CompanionTrainingOption.AWARE, null, 3)));

        Map<CompanionTrainingOption, Integer> remaining = CompanionDerivationService.remainingByOption(companion);

        assertThat(remaining.get(CompanionTrainingOption.AWARE)).isEqualTo(1);
    }

    @Test
    void remainingByOption_FullyTaken_ReturnsZeroNotNegative() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 2)));

        Map<CompanionTrainingOption, Integer> remaining = CompanionDerivationService.remainingByOption(companion);

        assertThat(remaining.get(CompanionTrainingOption.LIGHT_IN_THE_DARK)).isEqualTo(0);
    }

    @ParameterizedTest
    @EnumSource(CompanionTrainingOption.class)
    void remainingByOption_EveryOptionAtCapClampsToZero(CompanionTrainingOption option) {
        Set<CompanionTraining> trainings = new HashSet<>();
        for (int i = 0; i < option.getMaxSelections(); i++) {
            ViciousAxis axis = option == CompanionTrainingOption.VICIOUS ? ViciousAxis.DAMAGE_DIE : null;
            trainings.add(training(option, axis, 2 + i));
        }
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE, trainings);

        Map<CompanionTrainingOption, Integer> remaining = CompanionDerivationService.remainingByOption(companion);

        assertThat(remaining.get(option)).isZero();
    }

    // ==================== COMPANION-GRANTED HOPE SLOTS ====================

    @Test
    void companionGrantedHopeSlots_NoCompanions_ReturnsZero() {
        assertThat(CompanionDerivationService.companionGrantedHopeSlots(List.of())).isZero();
    }

    @Test
    void companionGrantedHopeSlots_NullCompanions_ReturnsZero() {
        assertThat(CompanionDerivationService.companionGrantedHopeSlots(null)).isZero();
    }

    @Test
    void companionGrantedHopeSlots_OneCompanionWithLightInTheDark_ReturnsOne() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 2)));

        assertThat(CompanionDerivationService.companionGrantedHopeSlots(List.of(companion))).isEqualTo(1);
    }

    @Test
    void companionGrantedHopeSlots_MultipleCompanions_SumsAcrossAll() {
        Companion companion1 = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 2)));
        Companion companion2 = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 4)));

        assertThat(CompanionDerivationService.companionGrantedHopeSlots(List.of(companion1, companion2))).isEqualTo(2);
    }

    @Test
    void companionGrantedHopeSlots_SoftDeletedCompanion_IsExcluded() {
        Companion deleted = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 2)));
        deleted.softDelete();

        Companion active = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.LIGHT_IN_THE_DARK, null, 3)));

        int slots = CompanionDerivationService.companionGrantedHopeSlots(List.of(deleted, active));

        assertThat(slots).isEqualTo(1);
    }

    @Test
    void companionGrantedHopeSlots_NoLightInTheDarkTrainings_ReturnsZero() {
        Companion companion = companionWithTrainings(10, 3, DiceType.D6, Range.MELEE,
                Set.of(training(CompanionTrainingOption.AWARE, null, 2)));

        assertThat(CompanionDerivationService.companionGrantedHopeSlots(List.of(companion))).isZero();
    }

    // ==================== HELPERS ====================

    private static Companion companionWithTrainings(
            int baseEvasion, int baseStressMax, DiceType baseDamageDice, Range baseAttackRange,
            Set<CompanionTraining> trainings) {
        return Companion.builder()
                .baseEvasion(baseEvasion)
                .baseStressMax(baseStressMax)
                .baseDamageDice(baseDamageDice)
                .baseAttackRange(baseAttackRange)
                .stressMarked(0)
                .trainings(trainings)
                .build();
    }

    private static Companion companionWithStress(int stressMarked, int baseStressMax) {
        return Companion.builder()
                .baseEvasion(10)
                .baseStressMax(baseStressMax)
                .baseDamageDice(DiceType.D6)
                .baseAttackRange(Range.MELEE)
                .stressMarked(stressMarked)
                .trainings(Set.of())
                .build();
    }

    private static CompanionTraining training(
            CompanionTrainingOption option, ViciousAxis viciousAxis, int acquiredAtLevel) {
        return CompanionTraining.builder()
                .option(option)
                .viciousAxis(viciousAxis)
                .acquiredAtLevel(acquiredAtLevel)
                .build();
    }
}
