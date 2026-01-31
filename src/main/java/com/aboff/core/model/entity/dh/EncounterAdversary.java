package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Join entity representing an adversary within an encounter with a count.
 * <p>
 * This entity tracks individual adversary assignments to encounters, including
 * the count of that adversary type (e.g., "3 Goblin Minions").
 * </p>
 */
@Entity
@Table(name = "encounter_adversaries")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterAdversary extends BaseEntity {

    /**
     * The encounter this adversary belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    /**
     * The adversary included in the encounter.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adversary_id", nullable = false)
    private Adversary adversary;

    /**
     * Number of this adversary type in the encounter.
     * Defaults to 1. Must be at least 1.
     */
    @Column(name = "count", nullable = false)
    private Integer count;

    /**
     * Calculates the battle points contribution of this adversary entry.
     * Battle points = adversary type's base battle points * count
     *
     * @return Total battle points for this adversary count
     */
    public int calculateBattlePoints() {
        if (adversary == null || adversary.getAdversaryType() == null) {
            return 0;
        }
        int basePoints = adversary.getAdversaryType().getBattlePoints();
        return basePoints * (count != null ? count : 1);
    }
}
