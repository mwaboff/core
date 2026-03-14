package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a companion associated with a character in the Daggerheart TTRPG system.
 * <p>
 * Companions are allied creatures, familiars, or followers that accompany a player character.
 * Each companion has its own combat capabilities (attack and evasion), stress tracking,
 * and can accumulate experiences that provide bonuses to relevant actions.
 * </p>
 * <p>
 * Companions are owned by a character sheet and are deleted when the owning character
 * is deleted (cascade delete).
 * </p>
 *
 * <h2>Example Companions</h2>
 * <ul>
 *   <li>A ranger's wolf companion with a bite attack</li>
 *   <li>A wizard's familiar owl that can scout ahead</li>
 *   <li>A knight's loyal warhorse trained for combat</li>
 * </ul>
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
     * The companion's evasion score.
     * Used to avoid or mitigate incoming attacks. Higher evasion makes the
     * companion harder to hit in combat.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer evasion = 0;

    /**
     * The name of the companion's primary attack.
     * Describes the type of attack the companion can perform (e.g., "Bite", "Claw", "Talon Strike").
     */
    @Column(name = "attack_name", nullable = false, length = 200)
    private String attackName;

    /**
     * The range category for the companion's attack.
     * Determines at what distance the companion can effectively attack targets.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "attack_range", nullable = false, length = 50)
    private Range attackRange;

    /**
     * The type of damage dice rolled for the companion's attack.
     * Represents the size of dice used when calculating attack damage (e.g., D6, D8, D10, D12).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "damage_dice", nullable = false, length = 10)
    private DiceType damageDice;

    /**
     * Maximum stress points for this companion.
     * Stress represents mental and emotional strain accumulated through
     * difficult situations. When stress reaches the maximum, the companion
     * may become incapacitated or flee.
     */
    @Column(name = "stress_max", nullable = false)
    @Builder.Default
    private Integer stressMax = 3;

    /**
     * Number of stress points currently marked (accumulated).
     * Must not exceed stressMax. High stress can lead to negative consequences
     * for the companion.
     */
    @Column(name = "stress_marked", nullable = false)
    @Builder.Default
    private Integer stressMarked = 0;

    /**
     * Experiences associated with this companion.
     * Experiences represent significant events and learning moments that provide
     * mechanical bonuses when relevant to the situation. Each experience grants
     * a modifier (typically +2) to applicable rolls.
     */
    @OneToMany(mappedBy = "companion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Experience> experiences = new HashSet<>();
}
