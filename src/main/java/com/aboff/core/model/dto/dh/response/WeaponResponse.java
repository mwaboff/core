package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.Burden;
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
 * Response DTO for Weapon entities.
 * Represents weapons in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter:
 * <ul>
 *   <li>By default: returns expansionId, featureIds, originalWeaponId only</li>
 *   <li>With ?expand=expansion: includes full expansion object</li>
 *   <li>With ?expand=features: includes full feature objects</li>
 *   <li>With ?expand=originalWeapon: includes full original weapon object</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeaponResponse {

    /**
     * Unique identifier for the weapon.
     */
    private Long id;

    /**
     * Name of the weapon.
     */
    private String name;

    /**
     * ID of the sourcebook this weapon was published in (always included).
     * <p>
     * Null for custom items, which came from no book.
     * </p>
     */
    private Long expansionId;

    /**
     * Full expansion object (included only when ?expand=expansion is specified).
     */
    private ExpansionResponse expansion;

    /**
     * The tier level of the weapon (1–4).
     */
    private Integer tier;

    /**
     * Whether this weapon is from official game content.
     */
    private Boolean isOfficial;

    /**
     * Whether this weapon is visible to every user.
     */
    private Boolean isPublic;

    /**
     * ID of the user who authored this weapon.
     * <p>
     * Null for official imports and for any row created before user authoring existed.
     * A non-null value alongside {@code isOfficial=false} is what marks a weapon as homebrew.
     * </p>
     */
    private Long createdByUserId;

    /**
     * IDs of the campaigns this weapon has been explicitly shared with.
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
     * The damage roll information for this weapon.
     */
    private DamageRollResponse damage;

    /**
     * IDs of features granted by this weapon (null if none).
     */
    private List<Long> featureIds;

    /**
     * Full feature objects (included only when ?expand=features is specified).
     */
    private List<FeatureResponse> features;

    /**
     * ID of the original weapon if this is a custom copy (null if original).
     */
    private Long originalWeaponId;

    /**
     * Full original weapon object (included only when ?expand=originalWeapon is specified).
     */
    private WeaponResponse originalWeapon;

    /**
     * Timestamp when the weapon was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the weapon was last modified.
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the weapon was soft-deleted (null if not deleted).
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
