package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.CompanionOrigin;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
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
 * Entity representing a Ranger companion associated with a character in the Daggerheart
 * TTRPG system.
 * <p>
 * Companions are allied creatures that accompany a Beastbound Ranger (or are granted
 * manually / by a GM). Each companion has its own base combat stats, Stress track, and
 * Experiences, plus a "Training" list of level-up options that improve those base stats.
 * </p>
 * <p>
 * The four printed stats -- Evasion, Stress max, damage dice, and attack range -- are stored
 * here as <strong>base</strong> values only. The Training-adjusted values actually used in
 * play are computed from {@link #trainings} by {@code CompanionDerivationService} and are
 * never stored, so reversing a Training pick (e.g. on level-down) is just deleting the row.
 * </p>
 * <p>
 * Companions are owned by a character sheet and are hard-deleted when the owning character
 * is deleted (cascade delete). A companion may additionally be soft-deleted on its own (see
 * {@link #deletedAt}) when the subclass feature that granted it is lost on level-down, so it
 * can be restored if that subclass is re-taken.
 * </p>
 */
@Entity
@Table(name = "companions")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Companion extends BaseEntity {

    /**
     * Printed cap on a companion's Experience count -- the companion sheet has exactly 5
     * Experience lines (core-01 companion sheet artwork, p.353), independently verified twice
     * from the PDF. Shared between every path that can add an Experience to a companion --
     * {@code ExperienceService.createExperience}'s manual/GM path and
     * {@code LevelUpService.validateCompanionExperienceGrants}'s level-up path -- so the rule
     * is enforced identically everywhere rather than duplicated with its own constant.
     */
    public static final int MAX_EXPERIENCES = 5;

    /**
     * The character sheet that owns this companion.
     * Each companion belongs to exactly one character.
     * When the character sheet is deleted, the companion is also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The companion's name.
     * This is the primary identifier for the companion and is displayed
     * in the UI alongside the character's information.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * A description of the companion.
     * This narrative text describes the companion's appearance, personality,
     * origin, and any other relevant details.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The companion's printed base Evasion, before Training bonuses.
     * Per the rules a new companion's Evasion "starts at 10"; the Training-adjusted Evasion
     * used in play (base + 2 per Aware pick) is derived, never stored.
     */
    @Column(name = "base_evasion", nullable = false)
    @Builder.Default
    private Integer baseEvasion = 10;

    /**
     * The name of the companion's primary attack.
     * Describes the type of attack the companion can perform (e.g., "Bite", "Claw", "Talon Strike").
     */
    @Column(name = "attack_name", nullable = false, length = 200)
    private String attackName;

    /**
     * The companion's printed base attack range, before Vicious (range-axis) Training steps.
     * At level 1 this is always Melee; Vicious can step it up the range ladder.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "base_attack_range", nullable = false, length = 50)
    @Builder.Default
    private Range baseAttackRange = Range.MELEE;

    /**
     * The companion's printed base damage dice, before Vicious (damage-die-axis) Training
     * steps. At level 1 this is always a d6; Vicious can step it up the die ladder.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "base_damage_dice", nullable = false, length = 10)
    @Builder.Default
    private DiceType baseDamageDice = DiceType.D6;

    /**
     * Whether the companion's attack deals physical or magic damage, chosen when the
     * companion is created. Unlike the damage die, this never changes via Training.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false, length = 20)
    @Builder.Default
    private DamageType damageType = DamageType.PHYSICAL;

    /**
     * The companion's printed base Stress maximum, before Resilient Training bonuses.
     */
    @Column(name = "base_stress_max", nullable = false)
    @Builder.Default
    private Integer baseStressMax = 3;

    /**
     * Number of Stress points currently marked (accumulated).
     * Must not exceed the derived Stress max. When it reaches that max the companion is
     * "out of scene" per the rules.
     */
    @Column(name = "stress_marked", nullable = false)
    @Builder.Default
    private Integer stressMarked = 0;

    /**
     * How this companion entered play: a subclass feature (e.g. Beastbound's Companion), a
     * GM grant, or a manual addition by the owner. Drives level-up Training eligibility and
     * level-down reversal -- only a {@link CompanionOrigin#SUBCLASS_FEATURE} companion is
     * soft-deleted and later restorable when its granting subclass is lost and re-taken.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 30)
    @Builder.Default
    private CompanionOrigin origin = CompanionOrigin.MANUAL;

    /**
     * The subclass card that granted this companion, set only when {@link #origin} is
     * {@link CompanionOrigin#SUBCLASS_FEATURE}. Lets a level-down archive this companion and
     * a later re-level match it back to the granting card to offer "Restore".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_subclass_card_id")
    private SubclassCard originSubclassCard;

    /**
     * Whether this companion receives a Training pick during the character's level-up flow.
     * Defaults to true. A GM-granted flavor pet or mount can have this turned off so it never
     * multiplies the per-companion level-up bonuses (Light in the Dark, Armored, Bonded,
     * Creature Comfort) alongside a Beastbound's real companion.
     */
    @Column(name = "advances_on_level_up", nullable = false)
    @Builder.Default
    private Boolean advancesOnLevelUp = true;

    /**
     * Experiences associated with this companion.
     * Experiences represent significant events and learning moments that provide
     * mechanical bonuses when relevant to the situation. Each experience grants
     * a modifier (typically +2) to applicable rolls.
     */
    @OneToMany(mappedBy = "companion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Experience> experiences = new HashSet<>();

    /**
     * The Training selections this companion has taken, one row per checkbox marked on the
     * printed Training list. Derived stats (Evasion, Stress max, damage dice, attack range)
     * are computed from this collection by {@code CompanionDerivationService} rather than
     * stored directly, so reversing a Training pick is just removing its row.
     */
    @OneToMany(mappedBy = "companion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CompanionTraining> trainings = new HashSet<>();

    /**
     * Timestamp indicating when this companion was soft-deleted (archived).
     * If null, the companion is active. Used instead of a hard delete so a
     * {@link CompanionOrigin#SUBCLASS_FEATURE} companion removed by level-down (e.g. losing a
     * multiclassed Beastbound subclass) can be restored if the character re-levels back into
     * it. Manually-created and GM-granted companions are never soft-deleted by level-down.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this companion has been soft-deleted.
     *
     * @return true if the companion is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the companion by setting the deleted_at timestamp to the current time.
     * The companion remains in the database, preserving its Training and Experience history,
     * but is filtered out of normal "active companions" queries.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted companion by clearing the deleted_at timestamp.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
