package com.aboff.core.model.dh;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ImprovisedTierStatistics}.
 * <p>
 * Verifies every tier's printed values from the rulebook's "Improvised Statistics by Tier"
 * table and that out-of-range or missing tiers resolve to empty rather than throwing.
 * </p>
 */
class ImprovisedTierStatisticsTest {

    @Test
    void forTier_TierOne_ReturnsPrintedStatistics() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(1);

        // Assert
        assertThat(result).isPresent();
        ImprovisedTierStatistics.TierStatistics stats = result.get();
        assertThat(stats.tier()).isEqualTo(1);
        assertThat(stats.attackModifier()).isEqualTo(1);
        assertThat(stats.difficulty()).isEqualTo(11);
        assertThat(stats.majorThreshold()).isEqualTo(7);
        assertThat(stats.severeThreshold()).isEqualTo(12);
        assertThat(stats.damageDiceRange()).isEqualTo("1d6+2 - 1d12+4");
    }

    @Test
    void forTier_TierTwo_ReturnsPrintedStatistics() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(2);

        // Assert
        assertThat(result).isPresent();
        ImprovisedTierStatistics.TierStatistics stats = result.get();
        assertThat(stats.tier()).isEqualTo(2);
        assertThat(stats.attackModifier()).isEqualTo(2);
        assertThat(stats.difficulty()).isEqualTo(14);
        assertThat(stats.majorThreshold()).isEqualTo(10);
        assertThat(stats.severeThreshold()).isEqualTo(20);
        assertThat(stats.damageDiceRange()).isEqualTo("2d6+3 - 2d12+4");
    }

    @Test
    void forTier_TierThree_ReturnsPrintedStatistics() {
        // Act - this is also the manual QA script's example: a Tier 1 Standard retiered to
        // Tier 3 should show Difficulty 17, thresholds 20/32, attack modifier +3
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(3);

        // Assert
        assertThat(result).isPresent();
        ImprovisedTierStatistics.TierStatistics stats = result.get();
        assertThat(stats.tier()).isEqualTo(3);
        assertThat(stats.attackModifier()).isEqualTo(3);
        assertThat(stats.difficulty()).isEqualTo(17);
        assertThat(stats.majorThreshold()).isEqualTo(20);
        assertThat(stats.severeThreshold()).isEqualTo(32);
        assertThat(stats.damageDiceRange()).isEqualTo("3d8+3 - 3d12+5");
    }

    @Test
    void forTier_TierFour_ReturnsPrintedStatistics() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(4);

        // Assert
        assertThat(result).isPresent();
        ImprovisedTierStatistics.TierStatistics stats = result.get();
        assertThat(stats.tier()).isEqualTo(4);
        assertThat(stats.attackModifier()).isEqualTo(4);
        assertThat(stats.difficulty()).isEqualTo(20);
        assertThat(stats.majorThreshold()).isEqualTo(25);
        assertThat(stats.severeThreshold()).isEqualTo(45);
        assertThat(stats.damageDiceRange()).isEqualTo("4d8+10 - 4d12+15");
    }

    @Test
    void forTier_NullTier_ReturnsEmpty() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void forTier_ZeroTier_ReturnsEmpty() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(0);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void forTier_TierAboveFour_ReturnsEmpty() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void forTier_NegativeTier_ReturnsEmpty() {
        // Act
        Optional<ImprovisedTierStatistics.TierStatistics> result = ImprovisedTierStatistics.forTier(-1);

        // Assert
        assertThat(result).isEmpty();
    }
}
