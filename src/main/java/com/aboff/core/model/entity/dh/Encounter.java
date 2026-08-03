package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.SearchableEntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an encounter in the Daggerheart TTRPG system.
 * <p>
 * An encounter is a grouping of adversaries designed for combat scenarios.
 * It tracks:
 * </p>
 * <ul>
 *   <li><strong>Basic Information:</strong> Name, description, tier</li>
 *   <li><strong>Adversaries:</strong> Collection of individual adversary instances</li>
 *   <li><strong>Battle Points:</strong> Calculated total for encounter balancing</li>
 *   <li><strong>Content Management:</strong> Official vs custom, public sharing, creator tracking</li>
 *   <li><strong>Campaign Association:</strong> Optional link to a specific campaign</li>
 * </ul>
 * <p>
 * Custom encounters can be created by copying official encounters,
 * with the {@code originalEncounter} field tracking the source.
 * </p>
 */
@Entity
@SearchIndexed(type = SearchableEntityType.ENCOUNTER)
@Table(name = "encounters")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Encounter extends BaseEntity {

    // ========== Basic Information ==========

    /**
     * The encounter's name.
     * This is the primary identifier displayed in encounters and game sessions.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * General description of the encounter.
     * Provides flavor text, setup details, and background information.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The encounter's tier level (1-4).
     * Indicates the power level and appropriate challenge rating for player parties.
     * Tier 1: Levels 1-3, Tier 2: Levels 4-6, Tier 3: Levels 7-9, Tier 4: Level 10
     * May be null if the encounter spans multiple tiers.
     */
    @Column(name = "tier")
    private Integer tier;

    // ========== Content Management ==========

    /**
     * Indicates whether this encounter is from official game content.
     * Official encounters are created by the game designers, while custom
     * encounters are created by users.
     */
    @Column(name = "is_official", nullable = false)
    private Boolean isOfficial;

    /**
     * Indicates whether this custom encounter is publicly visible.
     * Public encounters can be viewed and copied by other users, while
     * private encounters are only visible to their creator.
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    /**
     * Reference to the original encounter if this is a custom copy.
     * Null for official encounters and original custom creations.
     * Populated when a user copies an existing encounter for customization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_encounter_id")
    private Encounter originalEncounter;

    /**
     * The user who created this encounter.
     * Required for both official and custom content to track authorship
     * and ownership for permission checks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User createdBy;

    // ========== Campaign Association ==========

    /**
     * Optional campaign this encounter is associated with.
     * Null if the encounter is standalone or used across multiple campaigns.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    /**
     * Optional environment (scene stat block) this encounter takes place in.
     * Null if no environment has been chosen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    private Environment environment;

    // ========== Battle Points ==========

    /**
     * The number of PCs in combat, manually entered by the GM.
     * Drives both the suggested Battle Point budget and Minion grouping in
     * {@link BattlePointCalculator}. Never derived from a campaign roster -- an encounter can
     * be built and run with no campaign at all. Null until the GM sets it.
     */
    @Column(name = "party_size")
    private Integer partySize;

    /**
     * Battle Point adjustment: -1, the fight should be less difficult or shorter.
     */
    @Column(name = "adjustment_easier", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentEasier = false;

    /**
     * Battle Point adjustment: -2, using 2 or more Solo adversaries.
     */
    @Column(name = "adjustment_two_plus_solos", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentTwoPlusSolos = false;

    /**
     * Battle Point adjustment: -2, adding +1d4 (or a static +2) to all adversaries' damage rolls.
     */
    @Column(name = "adjustment_bonus_damage", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentBonusDamage = false;

    /**
     * Battle Point adjustment: +1, choosing an adversary from a lower tier.
     */
    @Column(name = "adjustment_lower_tier", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentLowerTier = false;

    /**
     * Battle Point adjustment: +1, including no Bruisers, Hordes, Leaders, or Solos.
     */
    @Column(name = "adjustment_no_elites", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentNoElites = false;

    /**
     * Battle Point adjustment: +2, the fight should be more dangerous or last longer.
     */
    @Column(name = "adjustment_harder", nullable = false)
    @lombok.Builder.Default
    private Boolean adjustmentHarder = false;

    // ========== Adversaries ==========

    /**
     * Adversaries included in this encounter.
     * Each entry represents a single adversary instance. Multiple instances of the
     * same adversary type should be represented as separate entries.
     */
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @lombok.Builder.Default
    private List<EncounterAdversary> encounterAdversaries = new ArrayList<>();

    // ========== Soft Deletion ==========

    /**
     * Timestamp indicating when this encounter was soft-deleted.
     * If null, the encounter is active and available.
     * Soft deletion preserves data while removing the encounter from
     * active queries and lists.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this encounter has been soft-deleted.
     *
     * @return true if the encounter is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the encounter by setting the deleted_at timestamp to the current time.
     * The encounter remains in the database but is filtered out from normal queries.
     * This preserves data integrity and relationships.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted encounter by clearing the deleted_at timestamp.
     * The encounter becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }

}
