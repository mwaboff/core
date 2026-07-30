package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import jakarta.validation.Valid;
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
     * Modifier applied to AGILITY trait while in this beastform.
     */
    @Builder.Default
    private Integer agilityModifier = 0;

    /**
     * Modifier applied to STRENGTH trait while in this beastform.
     */
    @Builder.Default
    private Integer strengthModifier = 0;

    /**
     * Modifier applied to FINESSE trait while in this beastform.
     */
    @Builder.Default
    private Integer finesseModifier = 0;

    /**
     * Modifier applied to INSTINCT trait while in this beastform.
     */
    @Builder.Default
    private Integer instinctModifier = 0;

    /**
     * Modifier applied to PRESENCE trait while in this beastform.
     */
    @Builder.Default
    private Integer presenceModifier = 0;

    /**
     * Modifier applied to KNOWLEDGE trait while in this beastform.
     */
    @Builder.Default
    private Integer knowledgeModifier = 0;

    /**
     * The effective range of attacks in this beastform.
     */
    @NotNull(message = "Attack range is required")
    private Range attackRange;

    /**
     * The trait used for attack rolls in this beastform.
     */
    @NotNull(message = "Attack trait is required")
    private Trait attackTrait;

    /**
     * The damage roll for attacks made in this beastform.
     */
    @Valid
    @NotNull(message = "Damage is required")
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
