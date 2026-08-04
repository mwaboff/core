package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.CompanionOrigin;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for companion data.
 * <p>
 * Returns both the Training-adjusted ("derived") values under their original field names
 * ({@code evasion}, {@code stressMax}, {@code damageDice}, {@code attackRange}) for backward
 * compatibility with existing consumers, and the printed base values the edit modal needs to
 * mutate ({@code baseEvasion}, {@code baseStressMax}, {@code baseDamageDice},
 * {@code baseAttackRange}). All derived values are computed by {@code CompanionDerivationService}
 * -- never recomputed here.
 * </p>
 * <p>
 * Supports expansion of related entities via ?expand query parameter for {@code characterSheet}
 * and {@code experiences}. {@code trainings} and {@code remainingByOption} are always included,
 * not expand-gated -- they are small and core to a companion's state.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanionResponse {

    /**
     * Unique identifier for the companion.
     */
    private Long id;

    /**
     * ID of the character sheet this companion belongs to.
     * Always included in response.
     */
    private Long characterSheetId;

    /**
     * Full character sheet data.
     * Included only when ?expand=characterSheet is requested.
     */
    private CharacterSheetResponse characterSheet;

    /**
     * Name of the companion.
     */
    private String name;

    /**
     * Description of the companion.
     */
    private String description;

    /**
     * Training-adjusted Evasion (base Evasion + 2 per Aware pick).
     */
    private Integer evasion;

    /**
     * The printed base Evasion, before Training bonuses.
     */
    private Integer baseEvasion;

    /**
     * Name of the companion's attack.
     */
    private String attackName;

    /**
     * Training-adjusted attack range (base range stepped up the ladder by Vicious picks on the
     * RANGE axis).
     */
    private Range attackRange;

    /**
     * The printed base attack range, before Vicious (range-axis) Training steps.
     */
    private Range baseAttackRange;

    /**
     * Training-adjusted damage dice (base dice stepped up the ladder by Vicious picks on the
     * DAMAGE_DIE axis).
     */
    private DiceType damageDice;

    /**
     * The printed base damage dice, before Vicious (damage-die-axis) Training steps.
     */
    private DiceType baseDamageDice;

    /**
     * The number of dice rolled for the companion's attack -- the character's live
     * Proficiency, per the rule that a commanded companion's damage roll "uses your
     * Proficiency and their damage die." Never snapshotted.
     */
    private Integer attackDiceCount;

    /**
     * Whether the companion's attack deals physical or magic damage.
     */
    private DamageType damageType;

    /**
     * Training-adjusted Stress maximum (base Stress max + 1 per Resilient pick).
     */
    private Integer stressMax;

    /**
     * The printed base Stress maximum, before Resilient Training bonuses.
     */
    private Integer baseStressMax;

    /**
     * Current stress marked on the companion.
     */
    private Integer stressMarked;

    /**
     * Whether the companion is "out of scene": at or past its derived Stress max.
     */
    private Boolean outOfScene;

    /**
     * How this companion entered play (subclass feature, GM grant, or manual addition).
     */
    private CompanionOrigin origin;

    /**
     * Whether this companion receives a Training pick during the character's level-up flow.
     */
    private Boolean advancesOnLevelUp;

    /**
     * The Training selections this companion has taken. Always included, not expand-gated.
     */
    private List<CompanionTrainingResponse> trainings;

    /**
     * How many more times each Training option can still be selected by this companion.
     * Always included, not expand-gated.
     */
    private Map<CompanionTrainingOption, Integer> remainingByOption;

    /**
     * List of experiences associated with this companion.
     * Included only when ?expand=experiences is requested.
     */
    private List<ExperienceResponse> experiences;

    /**
     * Timestamp when the companion was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the companion was last modified.
     */
    private LocalDateTime lastModifiedAt;
}
