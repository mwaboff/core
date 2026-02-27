package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Request DTO for updating an existing Adversary.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdversaryRequest {

    /**
     * Name of the adversary.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Power tier of the adversary (1-4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * The tactical role/type of the adversary.
     */
    private AdversaryType adversaryType;

    /**
     * General description of the adversary.
     */
    private String description;

    /**
     * Motives and tactical guidance for the GM.
     */
    private String motivesAndTactics;

    /**
     * Difficulty rating of the adversary.
     */
    @Min(value = 1, message = "Difficulty must be at least 1")
    private Integer difficulty;

    /**
     * Damage threshold for major injuries.
     */
    @Min(value = 1, message = "Major threshold must be at least 1")
    private Integer majorThreshold;

    /**
     * Damage threshold for severe injuries.
     */
    @Min(value = 1, message = "Severe threshold must be at least 1")
    private Integer severeThreshold;

    /**
     * Maximum hit points.
     */
    @Min(value = 0, message = "Hit point max cannot be negative")
    private Integer hitPointMax;

    /**
     * Currently marked hit points.
     */
    @Min(value = 0, message = "Hit point marked cannot be negative")
    private Integer hitPointMarked;

    /**
     * Maximum stress points.
     */
    @Min(value = 0, message = "Stress max cannot be negative")
    private Integer stressMax;

    /**
     * Currently marked stress points.
     */
    @Min(value = 0, message = "Stress marked cannot be negative")
    private Integer stressMarked;

    /**
     * Modifier applied to attack rolls.
     */
    private Integer attackModifier;

    /**
     * Name of the adversary's weapon or attack.
     */
    @Size(max = 200, message = "Weapon name must not exceed 200 characters")
    private String weaponName;

    /**
     * Effective range of the adversary's attack.
     */
    private Range attackRange;

    /**
     * Damage roll for the adversary's attack.
     */
    @Valid
    private DamageRollRequest damage;

    /**
     * Whether this adversary is publicly visible to other users.
     */
    private Boolean isPublic;

    /**
     * IDs of experiences to associate with this adversary.
     * Replaces existing experiences when provided.
     */
    private Set<Long> experienceIds;

    /**
     * IDs of features to associate with this adversary.
     * Replaces existing features when provided.
     */
    private Set<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
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
         * The number of dice to roll.
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
    }
}
