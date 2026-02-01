package com.aboff.core.model.entity.dh;

import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
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
 * Entity representing a beastform in the Daggerheart TTRPG system.
 * <p>
 * Beastforms are creatures that characters can transform into, granting
 * modified traits, combat abilities, and special features. Each beastform has:
 * </p>
 * <ul>
 *   <li><strong>Basic Information:</strong> Name, example description, advantages</li>
 *   <li><strong>Trait Modifiers:</strong> Adjustments to all six character traits</li>
 *   <li><strong>Combat:</strong> Attack range, trait, and damage roll</li>
 *   <li><strong>Features:</strong> Special abilities granted in this form</li>
 *   <li><strong>Content Management:</strong> Official vs custom, public sharing, creator tracking</li>
 * </ul>
 * <p>
 * Custom beastforms can be created by copying official beastforms,
 * with the {@code originalBeastform} field tracking the source. This allows
 * users to customize beastforms while retaining the ability to reference the original.
 * </p>
 */
@Entity
@Table(name = "beastforms")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Beastform extends BaseEntity {

    // ========== Basic Information ==========

    /**
     * The beastform's name.
     * This is the primary identifier for the creature form.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Example description or flavor text for this beastform.
     * Provides narrative context and visual imagery for the transformation.
     */
    @Column(columnDefinition = "TEXT")
    private String example;

    /**
     * Advantages granted by this beastform.
     * Optional text describing special benefits or situational bonuses.
     */
    @Column(columnDefinition = "TEXT")
    private String advantages;

    // ========== Trait Modifiers ==========

    /**
     * Modifier applied to AGILITY trait while in this beastform.
     * Can be positive (bonus), negative (penalty), or zero (no change).
     */
    @Column(name = "agility_modifier", nullable = false)
    @Builder.Default
    private Integer agilityModifier = 0;

    /**
     * Modifier applied to STRENGTH trait while in this beastform.
     */
    @Column(name = "strength_modifier", nullable = false)
    @Builder.Default
    private Integer strengthModifier = 0;

    /**
     * Modifier applied to FINESSE trait while in this beastform.
     */
    @Column(name = "finesse_modifier", nullable = false)
    @Builder.Default
    private Integer finesseModifier = 0;

    /**
     * Modifier applied to INSTINCT trait while in this beastform.
     */
    @Column(name = "instinct_modifier", nullable = false)
    @Builder.Default
    private Integer instinctModifier = 0;

    /**
     * Modifier applied to PRESENCE trait while in this beastform.
     */
    @Column(name = "presence_modifier", nullable = false)
    @Builder.Default
    private Integer presenceModifier = 0;

    /**
     * Modifier applied to KNOWLEDGE trait while in this beastform.
     */
    @Column(name = "knowledge_modifier", nullable = false)
    @Builder.Default
    private Integer knowledgeModifier = 0;

    // ========== Combat Information ==========

    /**
     * The effective range of attacks in this beastform.
     * Determines the distance at which the beastform can effectively engage targets.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_range", nullable = false, length = 20)
    private Range attackRange;

    /**
     * The trait used for attack rolls in this beastform.
     * Determines which character attribute contributes to attack rolls.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_trait", nullable = false, length = 20)
    private Trait attackTrait;

    /**
     * The damage roll for attacks made in this beastform.
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

    // ========== Features ==========

    /**
     * Features granted by this beastform.
     * Special abilities, traits, or mechanics that enhance or modify
     * the character's capabilities while transformed.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "beastform_features",
        joinColumns = @JoinColumn(name = "beastform_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();

    // ========== Content Management ==========

    /**
     * Indicates whether this beastform is from official game content.
     * Official beastforms are created by game designers, while custom
     * beastforms are created by users.
     */
    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = false;

    /**
     * Indicates whether this custom beastform is publicly visible.
     * Public beastforms can be viewed and copied by other users, while
     * private beastforms are only visible to their creator.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Reference to the original beastform if this is a custom copy.
     * Null for official beastforms and original custom creations.
     * Populated when a user copies an existing beastform for customization.
     * This allows tracking the source and provides a way to reference
     * the original or see what has been modified.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_beastform_id")
    private Beastform originalBeastform;

    /**
     * The expansion this beastform belongs to.
     * Groups beastforms by game content releases and supplements.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * The user who created this beastform.
     * Required for both official and custom content to track authorship
     * and ownership for permission checks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User createdBy;

    // ========== Soft Deletion ==========

    /**
     * Timestamp indicating when this beastform was soft-deleted.
     * If null, the beastform is active and available.
     * Soft deletion preserves data while removing the beastform from
     * active queries and lists.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this beastform has been soft-deleted.
     *
     * @return true if the beastform is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the beastform by setting the deleted_at timestamp to the current time.
     * The beastform remains in the database but is filtered out from normal queries.
     * This preserves data integrity and relationships.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted beastform by clearing the deleted_at timestamp.
     * The beastform becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
