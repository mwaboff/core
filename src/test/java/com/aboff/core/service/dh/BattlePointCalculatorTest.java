package com.aboff.core.service.dh;

import com.aboff.core.model.enums.AdversaryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BattlePointCalculator}.
 * <p>
 * Covers the rulebook's Battle Point budget formula, the six adjustment flags, Minion
 * grouping (including the exact group-boundary cases), every {@link AdversaryType}, and the
 * worked example from {@code core-04-adversaries-and-environments.md}.
 * </p>
 */
class BattlePointCalculatorTest {

    // ==================== SUGGESTED BUDGET ====================

    @Test
    void suggestedBudget_PartyOfFourNoAdjustments_ReturnsFourteen() {
        // Arrange - the rulebook's worked example: party of 4 -> budget 14
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4, BattlePointCalculator.Adjustments.NONE);

        // Assert
        assertThat(budget).isEqualTo(14);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void suggestedBudget_VariousPartySizes_MatchesFormula(int partySize) {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(partySize, BattlePointCalculator.Adjustments.NONE);

        // Assert
        assertThat(budget).isEqualTo((3 * partySize) + 2);
    }

    @Test
    void suggestedBudget_NullPartySize_TreatsAsZero() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(null, BattlePointCalculator.Adjustments.NONE);

        // Assert
        assertThat(budget).isEqualTo(2);
    }

    @Test
    void suggestedBudget_ZeroPartySize_TreatsAsZero() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(0, BattlePointCalculator.Adjustments.NONE);

        // Assert
        assertThat(budget).isEqualTo(2);
    }

    @Test
    void suggestedBudget_NegativePartySize_TreatsAsZero() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(-3, BattlePointCalculator.Adjustments.NONE);

        // Assert
        assertThat(budget).isEqualTo(2);
    }

    @Test
    void suggestedBudget_NullAdjustments_TreatedAsNone() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4, null);

        // Assert
        assertThat(budget).isEqualTo(14);
    }

    @Test
    void suggestedBudget_Easier_SubtractsOne() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(true, false, false, false, false, false));

        // Assert
        assertThat(budget).isEqualTo(13);
    }

    @Test
    void suggestedBudget_TwoPlusSolos_SubtractsTwo() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(false, true, false, false, false, false));

        // Assert
        assertThat(budget).isEqualTo(12);
    }

    @Test
    void suggestedBudget_BonusDamage_SubtractsTwo() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(false, false, true, false, false, false));

        // Assert
        assertThat(budget).isEqualTo(12);
    }

    @Test
    void suggestedBudget_LowerTier_AddsOne() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(false, false, false, true, false, false));

        // Assert
        assertThat(budget).isEqualTo(15);
    }

    @Test
    void suggestedBudget_NoElites_AddsOne() {
        // Act
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(false, false, false, false, true, false));

        // Assert
        assertThat(budget).isEqualTo(15);
    }

    @Test
    void suggestedBudget_Harder_AddsTwo() {
        // Act - "more dangerous" toggle from party of 4: 14 -> 16
        int budget = BattlePointCalculator.suggestedBudget(4,
                new BattlePointCalculator.Adjustments(false, false, false, false, false, true));

        // Assert
        assertThat(budget).isEqualTo(16);
    }

    @Test
    void suggestedBudget_HarderThenTwoPlusSolos_NetsToBaseBudget() {
        // Arrange - matches the manual QA script: +2 harder, then -2 for 2+ solos nets back to 14
        BattlePointCalculator.Adjustments harder =
                new BattlePointCalculator.Adjustments(false, false, false, false, false, true);
        int budgetAfterHarder = BattlePointCalculator.suggestedBudget(4, harder);

        // Act
        BattlePointCalculator.Adjustments harderAndSolos =
                new BattlePointCalculator.Adjustments(false, true, false, false, false, true);
        int budgetAfterBoth = BattlePointCalculator.suggestedBudget(4, harderAndSolos);

        // Assert
        assertThat(budgetAfterHarder).isEqualTo(16);
        assertThat(budgetAfterBoth).isEqualTo(14);
    }

    @Test
    void suggestedBudget_AllAdjustmentsCombined_SumsAllDeltas() {
        // Arrange - -1 -2 -2 +1 +1 +2 = -1 net delta
        BattlePointCalculator.Adjustments all =
                new BattlePointCalculator.Adjustments(true, true, true, true, true, true);

        // Act
        int budget = BattlePointCalculator.suggestedBudget(4, all);

        // Assert
        assertThat(budget).isEqualTo(13);
    }

    // ==================== SPENT POINTS: MINION GROUPING ====================

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void spentPoints_MinionGroupingAtVariousPartySizes_MatchesCeilingDivision(int partySize) {
        // Arrange - one full group plus one extra Minion should always round up to 2 groups
        List<AdversaryType> minions = minions(partySize + 1);

        // Act
        int spent = BattlePointCalculator.spentPoints(minions, partySize);

        // Assert
        assertThat(spent).isEqualTo(2);
    }

    @Test
    void spentPoints_FourMinionsPartyFour_IsOneGroup() {
        // Arrange - exact boundary: 4 minions / party 4 = 1 point
        // Act
        int spent = BattlePointCalculator.spentPoints(minions(4), 4);

        // Assert
        assertThat(spent).isEqualTo(1);
    }

    @Test
    void spentPoints_FiveMinionsPartyFour_IsTwoGroups() {
        // Arrange - exact boundary: 5 minions / party 4 = 2 points
        // Act
        int spent = BattlePointCalculator.spentPoints(minions(5), 4);

        // Assert
        assertThat(spent).isEqualTo(2);
    }

    @Test
    void spentPoints_EightMinionsPartyFour_IsTwoGroups() {
        // Arrange - the manual QA script's example: 8 minions, party 4 -> 2 points
        // Act
        int spent = BattlePointCalculator.spentPoints(minions(8), 4);

        // Assert
        assertThat(spent).isEqualTo(2);
    }

    @Test
    void spentPoints_NoMinions_ContributesZeroGroups() {
        // Act
        int spent = BattlePointCalculator.spentPoints(List.of(AdversaryType.STANDARD), 4);

        // Assert - just the Standard's 2 points, no minion group charge
        assertThat(spent).isEqualTo(2);
    }

    @Test
    void spentPoints_MinionsWithNullPartySize_TreatsGroupSizeAsOne() {
        // Act - no party size set yet; each Minion is its own group rather than dividing by zero
        int spent = BattlePointCalculator.spentPoints(minions(3), null);

        // Assert
        assertThat(spent).isEqualTo(3);
    }

    @Test
    void spentPoints_MinionsWithZeroPartySize_TreatsGroupSizeAsOne() {
        // Act
        int spent = BattlePointCalculator.spentPoints(minions(3), 0);

        // Assert
        assertThat(spent).isEqualTo(3);
    }

    @Test
    void spentPoints_MinionsWithNegativePartySize_TreatsGroupSizeAsOne() {
        // Act
        int spent = BattlePointCalculator.spentPoints(minions(3), -2);

        // Assert
        assertThat(spent).isEqualTo(3);
    }

    // ==================== SPENT POINTS: GENERAL ====================

    @Test
    void spentPoints_NullList_ReturnsZero() {
        // Act
        int spent = BattlePointCalculator.spentPoints(null, 4);

        // Assert
        assertThat(spent).isZero();
    }

    @Test
    void spentPoints_EmptyList_ReturnsZero() {
        // Act
        int spent = BattlePointCalculator.spentPoints(Collections.emptyList(), 4);

        // Assert
        assertThat(spent).isZero();
    }

    @ParameterizedTest
    @EnumSource(AdversaryType.class)
    void spentPoints_SingleInstanceOfEachType_MatchesTypeCostOrMinionGrouping(AdversaryType type) {
        // Act
        int spent = BattlePointCalculator.spentPoints(List.of(type), 4);

        // Assert
        if (type == AdversaryType.MINION) {
            // A single Minion against a party of 4 is still one (partial) group
            assertThat(spent).isEqualTo(1);
        } else {
            assertThat(spent).isEqualTo(type.getBattlePoints());
        }
    }

    @Test
    void spentPoints_WorkedExample_TwoBruisersTwoStandardsFourMinions_ReturnsThirteen() {
        // Arrange - the rulebook's worked example: party of 4, 2 Bruisers + 2 Standards +
        // 4 Minions = 13 spent (8 + 4 + 1)
        List<AdversaryType> instances = new ArrayList<>();
        instances.add(AdversaryType.BRUISER);
        instances.add(AdversaryType.BRUISER);
        instances.add(AdversaryType.STANDARD);
        instances.add(AdversaryType.STANDARD);
        instances.addAll(minions(4));

        // Act
        int spent = BattlePointCalculator.spentPoints(instances, 4);

        // Assert
        assertThat(spent).isEqualTo(13);
    }

    @Test
    void spentPoints_MixedNonMinionTypes_SumsIndividualCosts() {
        // Arrange - Leader (3) + Solo (5) + Ranged (2) = 10, no minions
        List<AdversaryType> instances = List.of(AdversaryType.LEADER, AdversaryType.SOLO, AdversaryType.RANGED);

        // Act
        int spent = BattlePointCalculator.spentPoints(instances, 4);

        // Assert
        assertThat(spent).isEqualTo(10);
    }

    /**
     * Builds a list of {@code count} MINION type entries, mirroring one row per instance
     * as encounters actually store them.
     */
    private List<AdversaryType> minions(int count) {
        List<AdversaryType> minions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            minions.add(AdversaryType.MINION);
        }
        return minions;
    }
}
