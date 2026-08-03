package com.aboff.core.service.dh;

import com.aboff.core.model.enums.AdversaryType;

import java.util.List;

/**
 * Pure, stateless calculator for Daggerheart's encounter Battle Point budget and spend.
 * <p>
 * Implements the rules printed in {@code resources/rules/chapters/core-04-adversaries-and-environments.md}:
 * </p>
 * <ul>
 *   <li><strong>Budget:</strong> {@code (3 * party size) + 2}, adjusted by the six optional
 *       modifiers a GM may toggle (easier/harder, extra Solos, bonus damage, lower tier,
 *       no elites).</li>
 *   <li><strong>Spend:</strong> every non-Minion adversary instance costs its
 *       {@link AdversaryType#getBattlePoints()}. Minions are billed per <em>group</em> --
 *       one point per group of Minions equal to the party size, not one point per Minion.</li>
 * </ul>
 * <p>
 * This class intentionally has no dependency on JPA entities so it can be tested with plain
 * {@link AdversaryType} values and is safe to mirror in the frontend for instant feedback.
 * </p>
 */
public final class BattlePointCalculator {

    private BattlePointCalculator() {
        // Static utility class - prevent instantiation
    }

    /**
     * The six optional Battle Point adjustments a GM may toggle for an encounter.
     * <p>
     * Each flag maps to a fixed budget delta:
     * </p>
     * <ul>
     *   <li>{@code easier}: -1 -- the fight should be less difficult or shorter</li>
     *   <li>{@code twoPlusSolos}: -2 -- using 2 or more Solo adversaries</li>
     *   <li>{@code bonusDamage}: -2 -- adding +1d4 (or a static +2) to all adversaries' damage rolls</li>
     *   <li>{@code lowerTier}: +1 -- choosing an adversary from a lower tier</li>
     *   <li>{@code noElites}: +1 -- including no Bruisers, Hordes, Leaders, or Solos</li>
     *   <li>{@code harder}: +2 -- the fight should be more dangerous or last longer</li>
     * </ul>
     *
     * @param easier True if the -1 "easier/shorter" adjustment applies
     * @param twoPlusSolos True if the -2 "2 or more Solos" adjustment applies
     * @param bonusDamage True if the -2 "bonus damage to all adversaries" adjustment applies
     * @param lowerTier True if the +1 "adversary from a lower tier" adjustment applies
     * @param noElites True if the +1 "no Bruisers/Hordes/Leaders/Solos" adjustment applies
     * @param harder True if the +2 "more dangerous/longer" adjustment applies
     */
    public record Adjustments(
            boolean easier,
            boolean twoPlusSolos,
            boolean bonusDamage,
            boolean lowerTier,
            boolean noElites,
            boolean harder) {

        /** Adjustments instance with every flag off, i.e. no change to the base budget. */
        public static final Adjustments NONE = new Adjustments(false, false, false, false, false, false);
    }

    /**
     * Calculates the suggested Battle Point budget for an encounter.
     * <p>
     * {@code budget = (3 * partySize) + 2 + sum(adjustment deltas)}. A null or non-positive
     * party size is treated as zero rather than dividing by zero or throwing, since a GM may
     * not have entered a party size yet.
     * </p>
     *
     * @param partySize The number of PCs in combat, may be null or non-positive
     * @param adjustments The adjustment flags to apply, may be null to apply none
     * @return The suggested Battle Point budget, never negative-infinite but may be negative
     *         if enough positive-cost adjustments are stacked against a tiny party
     */
    public static int suggestedBudget(Integer partySize, Adjustments adjustments) {
        int size = normalize(partySize);
        int budget = (3 * size) + 2;

        if (adjustments != null) {
            if (adjustments.easier()) {
                budget -= 1;
            }
            if (adjustments.twoPlusSolos()) {
                budget -= 2;
            }
            if (adjustments.bonusDamage()) {
                budget -= 2;
            }
            if (adjustments.lowerTier()) {
                budget += 1;
            }
            if (adjustments.noElites()) {
                budget += 1;
            }
            if (adjustments.harder()) {
                budget += 2;
            }
        }

        return budget;
    }

    /**
     * Calculates the total Battle Points spent by a set of adversary instances.
     * <p>
     * Minions are billed per group: {@code ceil(minionCount / max(partySize, 1))}. Every other
     * type is billed individually at its {@link AdversaryType#getBattlePoints()} cost. A null
     * or non-positive party size is treated as 1 for the minion grouping so the calculation
     * never divides by zero.
     * </p>
     *
     * @param adversaryTypes The type of each adversary instance in the encounter, one entry
     *                       per instance (repeat a type for multiple instances); may be null
     * @param partySize The number of PCs in combat, may be null or non-positive
     * @return The total Battle Points spent, 0 if there are no adversary instances
     */
    public static int spentPoints(List<AdversaryType> adversaryTypes, Integer partySize) {
        if (adversaryTypes == null || adversaryTypes.isEmpty()) {
            return 0;
        }

        int groupSize = Math.max(normalize(partySize), 1);

        long minionCount = adversaryTypes.stream()
                .filter(type -> type == AdversaryType.MINION)
                .count();
        int minionGroups = minionCount == 0 ? 0 : (int) Math.ceil((double) minionCount / groupSize);

        int everythingElse = adversaryTypes.stream()
                .filter(type -> type != null && type != AdversaryType.MINION)
                .mapToInt(AdversaryType::getBattlePoints)
                .sum();

        return minionGroups + everythingElse;
    }

    /**
     * Normalizes a party size to a non-negative int, treating null as 0.
     *
     * @param partySize The raw party size, may be null or negative
     * @return 0 if null or negative, otherwise the party size unchanged
     */
    private static int normalize(Integer partySize) {
        return partySize == null ? 0 : Math.max(partySize, 0);
    }
}
