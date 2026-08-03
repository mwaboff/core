package com.aboff.core.model.dh;

import java.util.Map;
import java.util.Optional;

/**
 * Static lookup for the Daggerheart rulebook's "Improvised Statistics by Tier" retier table
 * (printed in {@code resources/rules/chapters/core-04-adversaries-and-environments.md}).
 * <p>
 * When a GM retiers an adversary instance for an encounter, only the target tier is stored on
 * the instance (see {@code EncounterAdversary.tierOverride}) -- the derived attack modifier,
 * difficulty, and thresholds are computed on read from this table so the two can never drift
 * apart. Damage dice are printed as a range in the book rather than a single value, so they are
 * exposed as a display string instead of a dice roll.
 * </p>
 * <p>
 * Moving an adversary up two tiers (e.g. Tier 1-2 to Tier 3-4) should also raise its HP and
 * Stress by 1-3, per the book's guidance -- that adjustment is a GM judgment call made when
 * authoring the instance, not a value this table can derive, so it is intentionally not
 * modeled here.
 * </p>
 */
public final class ImprovisedTierStatistics {

    private ImprovisedTierStatistics() {
        // Static lookup - prevent instantiation
    }

    /**
     * The derived statistics for a single tier of improvised adversary stats.
     *
     * @param tier The tier these statistics apply to (1-4)
     * @param attackModifier The attack modifier for adversaries retiered to this tier
     * @param difficulty The Difficulty for adversaries retiered to this tier
     * @param majorThreshold The Major damage threshold for adversaries retiered to this tier
     * @param severeThreshold The Severe damage threshold for adversaries retiered to this tier
     * @param damageDiceRange The printed damage dice range for this tier, as display text --
     *                        the book prints a range (e.g. "1d6+2 - 1d12+4"), not a single roll
     */
    public record TierStatistics(
            int tier,
            int attackModifier,
            int difficulty,
            int majorThreshold,
            int severeThreshold,
            String damageDiceRange) {
    }

    private static final Map<Integer, TierStatistics> BY_TIER = Map.of(
            1, new TierStatistics(1, 1, 11, 7, 12, "1d6+2 - 1d12+4"),
            2, new TierStatistics(2, 2, 14, 10, 20, "2d6+3 - 2d12+4"),
            3, new TierStatistics(3, 3, 17, 20, 32, "3d8+3 - 3d12+5"),
            4, new TierStatistics(4, 4, 20, 25, 45, "4d8+10 - 4d12+15"));

    /**
     * Looks up the improvised statistics for a given tier.
     *
     * @param tier The tier to look up, may be null or out of the valid 1-4 range
     * @return The tier's statistics, or empty if {@code tier} is null or not 1-4
     */
    public static Optional<TierStatistics> forTier(Integer tier) {
        if (tier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_TIER.get(tier));
    }
}
