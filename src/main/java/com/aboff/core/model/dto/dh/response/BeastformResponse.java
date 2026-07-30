package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Beastform entities.
 * Represents beastform transformations in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId, featureIds, originalBeastformId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>With ?expand=originalBeastform: includes full original beastform object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeastformResponse {

    /**
     * Unique identifier for the beastform.
     */
    private Long id;

    /**
     * Name of the beastform.
     */
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
     * The damage roll information for attacks made in this beastform.
     */
    private DamageRollResponse damage;

    /**
     * ID of the expansion this beastform belongs to (always included).
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * Whether this beastform is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this custom beastform is publicly visible.
     */
    private Boolean isPublic;

    /**
     * IDs of features granted by this beastform (null if none).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * ID of the original beastform if this is a custom copy (null if original).
     */
    private Long originalBeastformId;

    /**
     * Full original beastform object (included only when ?expand=originalBeastform is specified).
     */
    private BeastformResponse originalBeastform;

    /**
     * Timestamp when the beastform was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the beastform was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the beastform was soft-deleted (null if not deleted).
     */
    private LocalDateTime deletedAt;

    /**
     * Nested DTO for damage roll information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageRollResponse {

        /**
         * The number of dice to roll. If null, uses character's proficiency.
         */
        private Integer diceCount;

        /**
         * The type of die to roll.
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

        /**
         * The formatted damage notation (e.g., "2d10+3 phy").
         */
        private String notation;
    }
}
