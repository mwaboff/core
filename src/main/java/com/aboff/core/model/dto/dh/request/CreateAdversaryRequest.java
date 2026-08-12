package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
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
import java.util.Set;

/**
 * Request DTO for creating a new Adversary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdversaryRequest {

    /**
     * Name of the adversary.
     */
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Power tier of the adversary (1-4).
     */
    @NotNull(message = "Tier is required")
    @Min(value = 1, message = "Tier must be at least 1")
    @Max(value = 4, message = "Tier must not exceed 4")
    private Integer tier;

    /**
     * The tactical role/type of the adversary.
     */
    @NotNull(message = "Adversary type is required")
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
     * Difficulty rating of the adversary. Optional: "framework" stat blocks for
     * multi-form adversaries (e.g. Forlorne Lykona, Hope &amp; Fear p.143) omit
     * difficulty entirely -- only the form-specific blocks carry one.
     */
    @Min(value = 1, message = "Difficulty must be at least 1")
    private Integer difficulty;

    /**
     * Damage threshold for major injuries. Optional -- "framework" stat blocks for
     * multi-form adversaries omit thresholds entirely. If omitted while
     * {@link #severeThreshold} is provided, remains null.
     */
    @Min(value = 0, message = "Major threshold cannot be negative")
    private Integer majorThreshold;

    /**
     * Damage threshold for severe injuries. Optional -- "framework" stat blocks for
     * multi-form adversaries omit thresholds entirely. If omitted, defaults to
     * {@link #majorThreshold} (which may itself be null).
     */
    @Min(value = 0, message = "Severe threshold cannot be negative")
    private Integer severeThreshold;

    /**
     * Maximum hit points.
     */
    @Builder.Default
    @Min(value = 0, message = "Hit point max cannot be negative")
    private Integer hitPointMax = 0;

    /**
     * Maximum stress points.
     */
    @Builder.Default
    @Min(value = 0, message = "Stress max cannot be negative")
    private Integer stressMax = 0;

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
     * ID of the expansion this adversary belongs to.
     */
    @NotNull(message = "Expansion ID is required")
    private Long expansionId;

    /**
     * Optional ID of the original adversary if this is a copy.
     */
    private Long originalAdversaryId;

    /**
     * Optional ID of the adversary this one evolves into, for evolution pairs.
     * Since bulk import creates adversaries in arbitrary order, the target adversary
     * may not exist yet at creation time; in that case, set this later via update.
     */
    private Long evolvesIntoAdversaryId;

    /**
     * IDs of experiences to associate with this adversary.
     */
    private Set<Long> experienceIds;

    /**
     * IDs of features to associate with this adversary.
     */
    private Set<Long> featureIds;

    /**
     * Features to find or create inline. Merged with featureIds if both provided.
     */
    @Valid
    private List<FeatureInput> features;

    /**
     * Whether this adversary is from official game content.
     */
    @Builder.Default
    private Boolean isOfficial = false;

    /**
     * Whether this adversary is SRD-licensed content, freely usable without owning the
     * sourcebook it was printed in. Optional; only ADMIN+ callers may actually set it to true --
     * see {@code ContentAccessService#resolveSrd}.
     */
    private Boolean srd;

    /**
     * Whether this adversary is publicly visible to other users.
     */
    @Builder.Default
    private Boolean isPublic = false;

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
         * The type of die to roll. Null indicates flat modifier-only damage (no dice).
         */
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
