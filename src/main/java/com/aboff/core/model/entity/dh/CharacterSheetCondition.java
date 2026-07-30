package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a character's instance of a {@link Condition} in the Daggerheart TTRPG
 * system.
 * <p>
 * This join entity tracks which conditions currently affect a character sheet. Unlike a plain
 * many-to-many link, each instance carries its own {@code magnitude} snapshot — some conditions
 * stack (e.g., multiple stacks of Ignited), and the magnitude records how many stacks (or what
 * intensity) currently apply to this specific character. Conditions without a stacking mechanic
 * simply leave {@code magnitude} null.
 * </p>
 * <p>
 * Modelled on {@code Experience} rather than {@code CharacterSheetLoot}: like an experience, a
 * condition instance carries its own per-row data (the magnitude) beyond a bare foreign-key link,
 * so it gets its own dedicated repository/service/controller instead of being folded into
 * {@code CharacterSheetService}'s bulk create/update payload handling.
 * </p>
 */
@Entity
@Table(name = "character_sheet_conditions")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheetCondition extends BaseEntity {

    /**
     * The character sheet currently affected by this condition instance.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The catalogue condition this instance represents.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    /**
     * The magnitude (stack count or intensity) of this condition instance, where applicable.
     * Null for conditions that do not stack.
     */
    @Column(name = "magnitude")
    private Integer magnitude;
}
