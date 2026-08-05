package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.ViciousAxis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a single Training selection taken by a {@link Companion}.
 * <p>
 * Each row is one checkbox marked on the printed Ranger Companion sheet's Training list --
 * e.g. one "Aware" pick, or one "Vicious" pick on a specific axis. Derived companion stats
 * (Evasion, Stress max, damage dice, attack range) are computed from the full collection of
 * a companion's trainings by {@code CompanionDerivationService}; nothing here stores a
 * derived value directly, so reversing a pick (e.g. on level-down) is just deleting the row.
 * </p>
 * <p>
 * This entity is owned by {@link Companion#getTrainings()} ({@code orphanRemoval = true}).
 * Any service code that mutates a companion's trainings in the same transaction where the
 * parent {@code Companion} is also saved must go through that collection
 * ({@code companion.getTrainings().add(...)} / {@code .removeIf(...)}), never
 * {@code CompanionTrainingRepository.delete()} directly -- see
 * {@code core/docs/agent-plans/2026-03-15-leveldown-domain-card-fix-design.md} for why a
 * direct repository delete on an already-loaded parent's orphanRemoval collection resurrects
 * the "deleted" row when the parent is saved again.
 * </p>
 */
@Entity
@Table(name = "companion_trainings")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionTraining extends BaseEntity {

    /**
     * The companion this Training selection belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    /**
     * Which Training option this selection is for (e.g. AWARE, VICIOUS, RESILIENT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "option", nullable = false, length = 40)
    private CompanionTrainingOption option;

    /**
     * Which ladder this pick advances. Required if and only if {@link #option} is
     * {@link CompanionTrainingOption#VICIOUS}: the companion's damage die or its attack
     * range. Null for every other option.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vicious_axis", length = 20)
    private ViciousAxis viciousAxis;

    /**
     * The Experience this selection grants a permanent +1 modifier to. Required if and only
     * if {@link #option} is {@link CompanionTrainingOption#INTELLIGENT}. Null for every
     * other option.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_experience_id")
    private Experience targetExperience;

    /**
     * The character level at which this Training was acquired. Used to scope level-down
     * reversal to only the trainings gained at the level being reverted, and for display.
     */
    @Column(name = "acquired_at_level", nullable = false)
    private Integer acquiredAtLevel;
}
