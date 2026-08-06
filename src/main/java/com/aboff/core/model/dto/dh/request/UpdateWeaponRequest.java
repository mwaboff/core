package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing Weapon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWeaponRequest {

    /**
     * Name of the weapon.
     */
    @Size(max = 200, message = "Weapon name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this weapon belongs to.
     */
    private Long expansionId;

    /**
     * The tier level of the weapon (1–4).
     */
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must be at most 4")
    private Integer tier;

    /**
     * Whether this weapon is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this weapon should be visible to every user. Honoured only for MODERATOR+;
     * coerced to false otherwise.
     */
    private Boolean isPublic;

    /**
     * Clears the expansion, marking the weapon as belonging to no sourcebook.
     * <p>
     * A JSON {@code null} for {@code expansionId} is indistinguishable from the field being
     * omitted, and omitted means "leave unchanged". This flag is the only way to actually
     * remove an expansion.
     * </p>
     */
    private Boolean clearExpansion;

    /**
     * Campaigns to share this weapon with, replacing any existing tags. Null leaves tags
     * untouched; an empty list removes them all.
     */
    private List<Long> campaignIds;

    /**
     * Whether this is a primary weapon (true) or secondary weapon (false).
     */
    private Boolean isPrimary;

    /**
     * The trait used to attack with this weapon.
     */
    private Trait trait;

    /**
     * The effective range of the weapon.
     */
    private Range range;

    /**
     * The burden type (one-handed or two-handed).
     */
    private Burden burden;

    /**
     * The damage roll for this weapon.
     */
    @Valid
    private DamageRollRequest damage;

    /**
     * Optional IDs of features granted by this weapon.
     */
    private List<Long> featureIds;

    /**
     * Features to find or create inline. Used if featureIds is not provided. featureIds takes precedence.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * Optional ID of the original weapon if this is a custom copy.
     */
    private Long originalWeaponId;

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
