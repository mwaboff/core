package com.aboff.core.model.entity.dh;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.SearchableEntityType;
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
@SearchIndexed(type = SearchableEntityType.BEASTFORM)
@Table(name = "beastforms")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
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

    /**
     * Modifier applied to Evasion while in this beastform.
     * Printed on most beastform cards (e.g. "Evasion +2"), but the two "Evolved" meta-cards
     * (Legendary Beast, Mythic Beast) print no stat line at all -- their mechanical effects are
     * prose in the feature text, applied manually by the player. NULL means "this card prints
     * no Evasion value"; it is intentionally not defaulted to 0, since a printed "Evasion +0"
     * and "no Evasion line printed" are different statements and a defaulted column cannot
     * distinguish them (see the six trait modifiers below for the fuller rationale).
     */
    @Column(name = "evasion")
    private Integer evasion;

    /**
     * The beastform's tier (1-4), matching the tier grouping printed on the rulebook cards.
     * Determines which forms a Druid can access as they level up. Required with no default,
     * following the same convention as {@code Weapon}/{@code Armor}/{@code Loot}/{@code Adversary}.
     */
    @Column(name = "tier", nullable = false)
    private Integer tier;

    // ========== Trait Modifiers ==========

    /**
     * Modifier applied to AGILITY trait while in this beastform.
     * Can be positive (bonus), negative (penalty), or {@code null}.
     * <p>
     * {@code null} means the card prints no Agility bonus line at all (e.g. the two "Evolved"
     * meta-cards, which apply their trait bonus to whichever base form was already chosen and
     * describe it in prose rather than a per-trait column). This is intentionally distinct
     * from an explicit {@code 0}: a {@code NOT NULL DEFAULT 0} column would silently turn
     * "not specified in the source data" into "the beastform grants +0", which is the same
     * defect shape that left a large fraction of the loot catalog mis-tiered in prod after an
     * earlier import omitted {@code tier} into a defaulted column. Callers that want an
     * on-the-record zero for an untouched trait must send it explicitly.
     */
    @Column(name = "agility_modifier")
    private Integer agilityModifier;

    /**
     * Modifier applied to STRENGTH trait while in this beastform. See {@link #agilityModifier}
     * for the {@code null}-vs-zero rationale.
     */
    @Column(name = "strength_modifier")
    private Integer strengthModifier;

    /**
     * Modifier applied to FINESSE trait while in this beastform. See {@link #agilityModifier}
     * for the {@code null}-vs-zero rationale.
     */
    @Column(name = "finesse_modifier")
    private Integer finesseModifier;

    /**
     * Modifier applied to INSTINCT trait while in this beastform. See {@link #agilityModifier}
     * for the {@code null}-vs-zero rationale.
     */
    @Column(name = "instinct_modifier")
    private Integer instinctModifier;

    /**
     * Modifier applied to PRESENCE trait while in this beastform. See {@link #agilityModifier}
     * for the {@code null}-vs-zero rationale.
     */
    @Column(name = "presence_modifier")
    private Integer presenceModifier;

    /**
     * Modifier applied to KNOWLEDGE trait while in this beastform. See {@link #agilityModifier}
     * for the {@code null}-vs-zero rationale.
     */
    @Column(name = "knowledge_modifier")
    private Integer knowledgeModifier;

    // ========== Combat Information ==========

    /**
     * The effective range of attacks in this beastform.
     * Determines the distance at which the beastform can effectively engage targets.
     * Nullable: the two "Evolved" cards (Legendary Beast T3, Mythic Beast T4) print no
     * attack range at all -- they upgrade an earlier pick's combat stats via prose rather
     * than defining their own.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_range", length = 20)
    private Range attackRange;

    /**
     * The trait used for attack rolls in this beastform.
     * Determines which character attribute contributes to attack rolls.
     * Nullable for the same "Evolved" cards as {@link #attackRange}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_trait", length = 20)
    private Trait attackTrait;

    /**
     * The damage roll for attacks made in this beastform.
     * Embedded component containing dice count, dice type, modifier, and damage type.
     * Maps to multiple database columns (damage_dice_count, damage_dice_type, etc.).
     * Nullable (dice type and damage type overridden to allow NULL below) for the same
     * "Evolved" cards as {@link #attackRange} -- the {@link DamageRoll} embeddable itself
     * declares {@code diceType}/{@code damageType} NOT NULL by default for other entities
     * (e.g. {@code Weapon}), so those two columns are overridden here specifically.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "diceCount", column = @Column(name = "damage_dice_count")),
        @AttributeOverride(name = "diceType", column = @Column(name = "damage_dice_type", length = 10, nullable = true)),
        @AttributeOverride(name = "modifier", column = @Column(name = "damage_modifier")),
        @AttributeOverride(name = "damageType", column = @Column(name = "damage_type", length = 10, nullable = true))
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
