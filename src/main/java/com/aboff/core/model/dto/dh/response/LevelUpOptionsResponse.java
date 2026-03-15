package com.aboff.core.model.dto.dh.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * DTO representing the available options for a character's next level-up.
 * <p>
 * Provides information about the level transition, available advancements,
 * domain card constraints, and accessible domains.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpOptionsResponse {

    /**
     * The character's current level.
     */
    private int currentLevel;

    /**
     * The level the character will reach after leveling up.
     */
    private int nextLevel;

    /**
     * The tier corresponding to the current level.
     */
    private int currentTier;

    /**
     * The tier corresponding to the next level.
     */
    private int nextTier;

    /**
     * Whether this level-up crosses a tier boundary.
     */
    private boolean isTierTransition;

    /**
     * Available advancements with remaining usage counts.
     */
    private List<AvailableAdvancement> availableAdvancements;

    /**
     * Maximum domain card level allowed in the next tier, or null if uncapped.
     */
    private Integer domainCardLevelCap;

    /**
     * IDs of domains accessible to this character through their subclass paths.
     */
    private Set<Long> accessibleDomainIds;

    /**
     * Number of currently equipped domain cards.
     */
    private long equippedDomainCardCount;

    /**
     * Maximum number of equipped domain cards allowed.
     */
    private int maxEquippedDomainCards;
}
