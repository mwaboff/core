package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.Burden;
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

/**
 * Request DTO for creating a new Weapon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWeaponRequest {

    /**
     * Name of the weapon.
     */
    @NotBlank(message = "Weapon name is required")
    @Size(max = 200, message = "Weapon name must not exceed 200 characters")
    private String name;

    /**
     * ID of the expansion this weapon belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Whether this weapon is from official game content.
     */
    @NotNull(message = "isOfficial is required")
    private Boolean isOfficial;

    /**
     * Whether this is a primary weapon (true) or secondary weapon (false).
     */
    @NotNull(message = "isPrimary is required")
    private Boolean isPrimary;

    /**
     * The trait used to attack with this weapon.
     */
    @NotNull(message = "Trait is required")
    private Trait trait;

    /**
     * The effective range of the weapon.
     */
    @NotNull(message = "Range is required")
    private Range range;

    /**
     * The burden type (one-handed or two-handed).
     */
    @NotNull(message = "Burden is required")
    private Burden burden;

    /**
     * The damage roll for this weapon.
     */
    @Valid
    @NotNull(message = "Damage is required")
    private DamageRollRequest damage;

    /**
     * Optional ID of the feature granted by this weapon.
     */
    private Long featureId;

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
