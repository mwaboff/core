package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Beastform. All fields are optional;
 * only non-null fields are applied to the entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBeastformRequest {

    /**
     * Name of the beastform.
     */
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
    private Integer agilityModifier;

    /**
     * Modifier applied to STRENGTH trait while in this beastform.
     */
    private Integer strengthModifier;

    /**
     * Modifier applied to FINESSE trait while in this beastform.
     */
    private Integer finesseModifier;

    /**
     * Modifier applied to INSTINCT trait while in this beastform.
     */
    private Integer instinctModifier;

    /**
     * Modifier applied to PRESENCE trait while in this beastform.
     */
    private Integer presenceModifier;

    /**
     * Modifier applied to KNOWLEDGE trait while in this beastform.
     */
    private Integer knowledgeModifier;

    /**
     * The effective range of attacks in this beastform.
     */
    private Range attackRange;

    /**
     * The trait used for attack rolls in this beastform.
     */
    private Trait attackTrait;

    /**
     * The damage roll for attacks made in this beastform.
     */
    @Valid
    private DamageRollRequest damage;

    /**
     * ID of the expansion this beastform belongs to.
     */
    private Long expansionId;

    /**
     * Whether this beastform is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this custom beastform is publicly visible.
     */
    private Boolean isPublic;

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
         * The type of die to roll. Null indicates no update to the dice type.
         */
        private DiceType diceType;

        /**
         * Optional modifier to add to the roll.
         */
        private Integer modifier;

        /**
         * The type of damage dealt.
         */
        private DamageType damageType;
    }
}
