package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new Beastform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBeastformRequest {

    /**
     * Name of the beastform.
     */
    @NotBlank(message = "Beastform name is required")
    @Size(max = 200, message = "Beastform name must not exceed 200 characters")
    private String name;

    /**
     * Example description or flavor text for this beastform.
     */
    private String example;

    /**
     * Advantages granted by this beastform.
     */
    private String advantages;

    /**
     * Modifier applied to Evasion while in this beastform. Optional: the two "Evolved"
     * meta-cards (Legendary Beast, Mythic Beast) print no Evasion line at all. Omitting this
     * field from the request body leaves it {@code null} on the entity — it is NOT defaulted
     * to 0 — since "no Evasion line printed" and "printed Evasion +0" are different statements
     * (see {@link com.aboff.core.model.entity.dh.Beastform#agilityModifier} for the fuller
     * null-vs-zero rationale, which applies identically here).
     */
    private Integer evasion;

    /**
     * The beastform's tier (1-4). Required — every beastform card prints a tier, including
     * the stat-less "Evolved" meta-cards.
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Modifier applied to AGILITY trait while in this beastform. Optional and NOT defaulted
     * to 0 when omitted — see {@link #evasion} for why.
     */
    private Integer agilityModifier;

    /**
     * Modifier applied to STRENGTH trait while in this beastform. See {@link #evasion} for the
     * null-vs-zero rationale.
     */
    private Integer strengthModifier;

    /**
     * Modifier applied to FINESSE trait while in this beastform. See {@link #evasion} for the
     * null-vs-zero rationale.
     */
    private Integer finesseModifier;

    /**
     * Modifier applied to INSTINCT trait while in this beastform. See {@link #evasion} for the
     * null-vs-zero rationale.
     */
    private Integer instinctModifier;

    /**
     * Modifier applied to PRESENCE trait while in this beastform. See {@link #evasion} for the
     * null-vs-zero rationale.
     */
    private Integer presenceModifier;

    /**
     * Modifier applied to KNOWLEDGE trait while in this beastform. See {@link #evasion} for the
     * null-vs-zero rationale.
     */
    private Integer knowledgeModifier;

    /**
     * The effective range of attacks in this beastform. Optional: the two "Evolved" meta-cards
     * print no combat stat line, upgrading whichever base form the player already has instead.
     */
    private Range attackRange;

    /**
     * The trait used for attack rolls in this beastform. Optional for the same "Evolved" cards
     * as {@link #attackRange}.
     */
    private Trait attackTrait;

    /**
     * The damage roll for attacks made in this beastform. Optional for the same "Evolved"
     * cards as {@link #attackRange}. When present, {@code diceType} and {@code damageType}
     * within it are still required — a partially-specified damage roll is not meaningful.
     */
    @Valid
    private DamageRollRequest damage;

    /**
     * ID of the expansion this beastform belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this beastform is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this custom beastform is publicly visible.
     */
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Optional ID of the original beastform if this is a custom copy.
     */
    private Long originalBeastformId;

    /**
     * Optional IDs of features granted by this beastform.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Used if featureIds is not provided. featureIds takes precedence.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * Nested DTO for damage roll information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageRollRequest {

        /**
         * The number of dice to roll. If null, uses character's proficiency.
         */
        private Integer diceCount;

        /**
         * The type of die to roll.
         */
        @NotNull(message = "Dice type is required")
        private DiceType diceType;

        /**
         * Optional modifier to add to the roll.
         */
        private Integer modifier;

        /**
         * The type of damage dealt.
         */
        @NotNull(message = "Damage type is required")
        private DamageType damageType;
    }
}
