package com.aboff.core.service.dh;

import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.ViciousAxis;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, stateless calculator for a companion's Training-derived stats.
 * <p>
 * A companion's four base stats (Evasion, Stress max, damage dice, attack range) are stored
 * on {@link Companion} unmodified; every Training pick layers a bonus on top at read time,
 * computed here rather than persisted. This is the single source of truth for that math --
 * see the companions implementation plan, section 4.3:
 * </p>
 * <pre>
 * evasion     = baseEvasion   + 2 * count(AWARE)
 * stressMax   = baseStressMax + 1 * count(RESILIENT)
 * damageDice  = ladder(baseDamageDice,  count(VICIOUS where axis = DAMAGE_DIE))   // D6 -&gt; D8 -&gt; D10 -&gt; D12, cap D12
 * attackRange = ladder(baseAttackRange, count(VICIOUS where axis = RANGE))        // MELEE -&gt; VERY_CLOSE -&gt; CLOSE -&gt; FAR -&gt; VERY_FAR, cap VERY_FAR
 * outOfScene  = stressMarked &gt;= stressMax
 * </pre>
 * <p>
 * {@code OUT_OF_RANGE} is deliberately excluded from the attack-range ladder -- it is not a
 * targetable band a Vicious pick can step into. Both ladders saturate at their cap and never
 * throw, even if every possible pick were somehow taken at once.
 * </p>
 */
public final class CompanionDerivationService {

    /**
     * The damage-die ladder a {@code VICIOUS} pick on the {@link ViciousAxis#DAMAGE_DIE} axis
     * steps up, capped at D12.
     */
    private static final List<DiceType> DAMAGE_DIE_LADDER = List.of(DiceType.D6, DiceType.D8, DiceType.D10, DiceType.D12);

    /**
     * The attack-range ladder a {@code VICIOUS} pick on the {@link ViciousAxis#RANGE} axis
     * steps up, capped at VERY_FAR. {@code OUT_OF_RANGE} is not part of this ladder.
     */
    private static final List<Range> ATTACK_RANGE_LADDER = List.of(Range.MELEE, Range.VERY_CLOSE, Range.CLOSE, Range.FAR, Range.VERY_FAR);

    /** Evasion bonus per Aware pick. */
    private static final int AWARE_EVASION_BONUS = 2;

    /** Stress max bonus per Resilient pick. */
    private static final int RESILIENT_STRESS_BONUS = 1;

    private CompanionDerivationService() {
        // Static utility class - prevent instantiation
    }

    /**
     * Calculates a companion's Training-adjusted Evasion.
     *
     * @param companion the companion to derive Evasion for
     * @return {@code baseEvasion + 2} for every {@code AWARE} training taken
     */
    public static int evasion(Companion companion) {
        return companion.getBaseEvasion() + AWARE_EVASION_BONUS * countOption(companion, CompanionTrainingOption.AWARE);
    }

    /**
     * Calculates a companion's Training-adjusted Stress maximum.
     *
     * @param companion the companion to derive Stress max for
     * @return {@code baseStressMax + 1} for every {@code RESILIENT} training taken
     */
    public static int stressMax(Companion companion) {
        return companion.getBaseStressMax() + RESILIENT_STRESS_BONUS * countOption(companion, CompanionTrainingOption.RESILIENT);
    }

    /**
     * Calculates a companion's Training-adjusted damage dice.
     * <p>
     * Steps up the D6 -&gt; D8 -&gt; D10 -&gt; D12 ladder once per {@code VICIOUS} training taken
     * on the {@link ViciousAxis#DAMAGE_DIE} axis, saturating at D12.
     * </p>
     *
     * @param companion the companion to derive damage dice for
     * @return the Training-adjusted damage dice, never null
     */
    public static DiceType damageDice(Companion companion) {
        int steps = countVicious(companion, ViciousAxis.DAMAGE_DIE);
        return stepLadder(DAMAGE_DIE_LADDER, companion.getBaseDamageDice(), steps);
    }

    /**
     * Calculates a companion's Training-adjusted attack range.
     * <p>
     * Steps up the Melee -&gt; Very Close -&gt; Close -&gt; Far -&gt; Very Far ladder once per
     * {@code VICIOUS} training taken on the {@link ViciousAxis#RANGE} axis, saturating at
     * Very Far. {@code OUT_OF_RANGE} is never returned.
     * </p>
     *
     * @param companion the companion to derive attack range for
     * @return the Training-adjusted attack range, never null
     */
    public static Range attackRange(Companion companion) {
        int steps = countVicious(companion, ViciousAxis.RANGE);
        return stepLadder(ATTACK_RANGE_LADDER, companion.getBaseAttackRange(), steps);
    }

    /**
     * Determines whether a companion is "out of scene": at or past its derived Stress max.
     * <p>
     * Per the rules, when a companion marks its last Stress it drops out of the scene until
     * the character's next long rest. This app has no rest system, so this method only
     * derives the boolean state -- recovery is left to the player clearing Stress manually.
     * </p>
     *
     * @param companion the companion to check
     * @return true if {@code stressMarked >= stressMax()}
     */
    public static boolean outOfScene(Companion companion) {
        return companion.getStressMarked() >= stressMax(companion);
    }

    /**
     * Calculates how many more times each Training option can be selected by a companion.
     *
     * @param companion the companion to derive remaining picks for
     * @return a map from every {@link CompanionTrainingOption} to its remaining pick count,
     *         never negative even if somehow over-selected
     */
    public static Map<CompanionTrainingOption, Integer> remainingByOption(Companion companion) {
        Map<CompanionTrainingOption, Integer> remaining = new EnumMap<>(CompanionTrainingOption.class);
        for (CompanionTrainingOption option : CompanionTrainingOption.values()) {
            int taken = countOption(companion, option);
            remaining.put(option, Math.max(0, option.getMaxSelections() - taken));
        }
        return remaining;
    }

    /**
     * Sums the extra Hope slots granted to a character by all of its active companions'
     * {@code LIGHT_IN_THE_DARK} trainings.
     * <p>
     * Soft-deleted companions are excluded: a companion archived by level-down no longer
     * grants its bonus Hope slot until it is restored.
     * </p>
     *
     * @param companions the character's companions, active and soft-deleted alike
     * @return the total number of bonus Hope slots granted, 0 if none
     */
    public static int companionGrantedHopeSlots(Collection<Companion> companions) {
        if (companions == null) {
            return 0;
        }
        return companions.stream()
                .filter(companion -> !companion.isDeleted())
                .mapToInt(companion -> countOption(companion, CompanionTrainingOption.LIGHT_IN_THE_DARK))
                .sum();
    }

    /**
     * Counts how many times a companion has taken a specific Training option.
     *
     * @param companion the companion whose trainings to inspect
     * @param option the option to count
     * @return the number of matching trainings, 0 if none
     */
    private static int countOption(Companion companion, CompanionTrainingOption option) {
        Set<CompanionTraining> trainings = companion.getTrainings();
        if (trainings == null) {
            return 0;
        }
        return (int) trainings.stream()
                .filter(training -> training.getOption() == option)
                .count();
    }

    /**
     * Counts how many {@code VICIOUS} trainings a companion has taken on a specific axis.
     *
     * @param companion the companion whose trainings to inspect
     * @param axis the Vicious axis to count
     * @return the number of matching trainings, 0 if none
     */
    private static int countVicious(Companion companion, ViciousAxis axis) {
        Set<CompanionTraining> trainings = companion.getTrainings();
        if (trainings == null) {
            return 0;
        }
        return (int) trainings.stream()
                .filter(training -> training.getOption() == CompanionTrainingOption.VICIOUS && training.getViciousAxis() == axis)
                .count();
    }

    /**
     * Steps a base value forward a number of positions along a fixed ladder, saturating at
     * the last entry and never throwing.
     * <p>
     * If {@code base} is not itself a member of the ladder (should not happen for a rules-
     * legal companion, since base damage dice and range are always ladder members), the base
     * value is returned unchanged -- there is no well-defined "one step past an unknown
     * value," and this is safer than guessing or throwing.
     * </p>
     *
     * @param ladder the ordered ladder of values, lowest first
     * @param base the starting value
     * @param steps the number of steps to advance, treated as 0 if negative
     * @param <T> the ladder's element type
     * @return the stepped value, saturated at the ladder's last entry
     */
    private static <T> T stepLadder(List<T> ladder, T base, int steps) {
        int index = ladder.indexOf(base);
        if (index < 0) {
            return base;
        }
        int stepped = Math.min(index + Math.max(steps, 0), ladder.size() - 1);
        return ladder.get(stepped);
    }
}
