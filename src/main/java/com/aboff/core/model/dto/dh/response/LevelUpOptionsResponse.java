package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.enums.AdvancementType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO describing the available level-up options for a character.
 * <p>
 * Provides information about the character's current and next level/tier,
 * which advancements are available (including per-tier usage limits),
 * domain card constraints, and equipped card counts.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelUpOptionsResponse {

    /**
     * The character's current level.
     */
    private Integer currentLevel;

    /**
     * The level the character will advance to.
     */
    private Integer nextLevel;

    /**
     * The character's current tier.
     */
    private Integer currentTier;

    /**
     * The tier the character will be in after leveling up.
     */
    private Integer nextTier;

    /**
     * Whether this level-up crosses a tier boundary (levels 2, 5, 8).
     */
    private Boolean isTierTransition;

    /**
     * List of advancements available for this level-up, including usage limits.
     */
    private List<AvailableAdvancement> availableAdvancements;

    /**
     * Maximum domain card level that can be selected, or {@code null} if uncapped.
     */
    private Integer domainCardLevelCap;

    /**
     * IDs of domains accessible to this character for domain card selection.
     */
    private List<Long> accessibleDomainIds;

    /**
     * Number of domain cards currently equipped by the character.
     */
    private Integer equippedDomainCardCount;

    /**
     * Maximum number of domain cards that can be equipped (always 5).
     */
    private Integer maxEquippedDomainCards;

    /**
     * Describes a single advancement option available during level-up,
     * including its per-tier usage limits and mutual exclusivity constraints.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvailableAdvancement {

        /**
         * The advancement type.
         */
        private AdvancementType type;

        /**
         * Human-readable description of the advancement effect.
         */
        private String description;

        /**
         * Maximum number of times this advancement can be taken per tier.
         */
        private Integer limitPerTier;

        /**
         * Number of times this advancement has been used in the current tier.
         */
        private Integer usedInTier;

        /**
         * Remaining uses of this advancement in the current tier.
         */
        private Integer remaining;

        /**
         * List of advancement types that are mutually exclusive with this one.
         */
        private List<AdvancementType> mutuallyExclusiveWith;
    }
}
