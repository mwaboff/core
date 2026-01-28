package com.aboff.core.model.entity.dh;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.Range;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing an adversary (enemy/NPC) in the Daggerheart TTRPG system.
 * <p>
 * Adversaries are creatures and characters that oppose the player characters,
 * providing challenges and obstacles during gameplay. Each adversary has:
 * </p>
 * <ul>
 *   <li><strong>Basic Information:</strong> Name, tier (1-4), type, description</li>
 *   <li><strong>Tactics:</strong> Motives and tactical guidance for GMs</li>
 *   <li><strong>Difficulty:</strong> Difficulty rating and damage thresholds</li>
 *   <li><strong>Resources:</strong> Hit points and stress (same system as character sheets)</li>
 *   <li><strong>Combat:</strong> Attack modifier, weapon, range, and damage roll</li>
 *   <li><strong>Abilities:</strong> Experiences and features that enhance the adversary</li>
 *   <li><strong>Content Management:</strong> Official vs custom, public sharing, creator tracking</li>
 * </ul>
 * <p>
 * Custom adversaries can be created by copying official adversaries,
 * with the {@code originalAdversary} field tracking the source. This allows
 * users to customize adversaries while retaining the ability to revert to defaults.
 * </p>
 */
@Entity
@Table(name = "adversaries")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Adversary extends BaseEntity {

    // ========== Basic Information ==========

    /**
     * The adversary's name.
     * This is the primary identifier displayed in encounters and game sessions.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * The adversary's tier level (1-4).
     * Indicates the power level and appropriate challenge rating for player parties.
     * Tier 1: Levels 1-3, Tier 2: Levels 4-6, Tier 3: Levels 7-9, Tier 4: Level 10
     */
    @Column(name = "tier", nullable = false)
    private Integer tier;

    /**
     * The adversary's tactical role in combat.
     * Determines combat behavior and how the GM should utilize this adversary.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "adversary_type", nullable = false, length = 50)
    private AdversaryType adversaryType;

    /**
     * General description of the adversary.
     * Provides flavor text, appearance details, and background information.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Motives and tactical guidance for the GM.
     * Describes what drives this adversary and how they should be played in combat
     * and social encounters.
     */
    @Column(name = "motives_and_tactics", columnDefinition = "TEXT")
    private String motivesAndTactics;

    // ========== Difficulty and Damage Thresholds ==========

    /**
     * The difficulty rating of this adversary.
     * Higher values indicate more challenging opponents. Used by GMs to
     * balance encounters and determine appropriate challenges for the party.
     */
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;

    /**
     * The damage threshold for suffering a major injury.
     * When this adversary takes damage at or above this threshold (but below severe),
     * they sustain a major wound with mechanical consequences.
     */
    @Column(name = "major_threshold", nullable = false)
    private Integer majorThreshold;

    /**
     * The damage threshold for suffering a severe injury.
     * When damage at or above this threshold is taken, the adversary sustains
     * a severe wound. Must be greater than or equal to majorThreshold.
     */
    @Column(name = "severe_threshold", nullable = false)
    private Integer severeThreshold;

    // ========== Resources ==========

    /**
     * Maximum hit points for this adversary.
     * Hit points represent health and ability to sustain injury.
     * When marked hit points reach the maximum, the adversary is defeated.
     */
    @Column(name = "hit_point_max", nullable = false)
    @Builder.Default
    private Integer hitPointMax = 0;

    /**
     * Number of hit points currently marked (damage taken).
     * Must not exceed hitPointMax. Tracks damage during encounters.
     */
    @Column(name = "hit_point_marked", nullable = false)
    @Builder.Default
    private Integer hitPointMarked = 0;

    /**
     * Maximum stress points for this adversary.
     * Stress represents mental and emotional strain from challenges
     * and supernatural effects.
     */
    @Column(name = "stress_max", nullable = false)
    @Builder.Default
    private Integer stressMax = 0;

    /**
     * Number of stress points currently marked (accumulated).
     * Must not exceed stressMax. High stress can lead to mechanical consequences.
     */
    @Column(name = "stress_marked", nullable = false)
    @Builder.Default
    private Integer stressMarked = 0;

    // ========== Combat Information ==========

    /**
     * Modifier applied to attack rolls made by this adversary.
     * Can be positive (bonus) or negative (penalty). Used when calculating
     * attack success against player characters.
     */
    @Column(name = "attack_modifier")
    private Integer attackModifier;

    /**
     * The name of the adversary's weapon or primary attack.
     * Examples: "Rusty Sword", "Claws", "Fire Breath", "Poisoned Dagger"
     */
    @Column(name = "weapon_name", length = 200)
    private String weaponName;

    /**
     * The effective range of the adversary's attack.
     * Determines the distance at which the adversary can effectively engage targets.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_range", length = 20)
    private Range attackRange;

    /**
     * The damage roll for this adversary's attack.
     * Embedded component containing dice count, dice type, modifier, and damage type.
     * Maps to multiple database columns (damage_dice_count, damage_dice_type, etc.).
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "diceCount", column = @Column(name = "damage_dice_count")),
        @AttributeOverride(name = "diceType", column = @Column(name = "damage_dice_type", length = 10)),
        @AttributeOverride(name = "modifier", column = @Column(name = "damage_modifier")),
        @AttributeOverride(name = "damageType", column = @Column(name = "damage_type", length = 10))
    })
    private DamageRoll damage;

    // ========== Content Management ==========

    /**
     * Indicates whether this adversary is from official game content.
     * Official adversaries are created by the game designers, while custom
     * adversaries are created by users.
     */
    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = false;

    /**
     * Indicates whether this custom adversary is publicly visible.
     * Public adversaries can be viewed and copied by other users, while
     * private adversaries are only visible to their creator.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Reference to the original adversary if this is a custom copy.
     * Null for official adversaries and original custom creations.
     * Populated when a user copies an existing adversary for customization.
     * This allows tracking the source and provides a way to revert changes
     * or see what has been modified from the original.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_adversary_id")
    private Adversary originalAdversary;

    /**
     * The expansion this adversary belongs to.
     * Groups adversaries by game content releases and supplements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * The user who created this adversary.
     * Required for both official and custom content to track authorship
     * and ownership for permission checks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User createdBy;

    // ========== Abilities ==========

    /**
     * Experiences associated with this adversary.
     * Experiences represent knowledge, training, or background that provides
     * mechanical bonuses in relevant situations. Each experience grants a modifier
     * (typically +2) to applicable rolls.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "adversary_experiences",
        joinColumns = @JoinColumn(name = "adversary_id"),
        inverseJoinColumns = @JoinColumn(name = "experience_id")
    )
    @Builder.Default
    private Set<Experience> experiences = new HashSet<>();

    /**
     * Features associated with this adversary.
     * Features are special abilities, traits, or mechanics that enhance
     * or modify the adversary's capabilities. Examples include special attacks,
     * passive abilities, or unique mechanics.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "adversary_features",
        joinColumns = @JoinColumn(name = "adversary_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    // ========== Soft Deletion ==========

    /**
     * Timestamp indicating when this adversary was soft-deleted.
     * If null, the adversary is active and available.
     * Soft deletion preserves data while removing the adversary from
     * active queries and lists.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this adversary has been soft-deleted.
     *
     * @return true if the adversary is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the adversary by setting the deleted_at timestamp to the current time.
     * The adversary remains in the database but is filtered out from normal queries.
     * This preserves data integrity and relationships.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted adversary by clearing the deleted_at timestamp.
     * The adversary becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
