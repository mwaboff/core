package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing an experience entry for a character in the Daggerheart TTRPG system.
 * <p>
 * Experiences track significant events, accomplishments, and learning moments for a character.
 * Each experience provides a modifier that can be applied when the character attempts actions
 * related to that experience. The default modifier is +2, representing the character's
 * enhanced capability in situations where this experience is relevant.
 * </p>
 * <p>
 * Experiences are created by users (typically the GM or the player) and are associated with
 * a specific character sheet. They serve as both narrative history and mechanical bonuses,
 * encouraging players to engage with their character's backstory and development.
 * </p>
 *
 * <h2>Example Experiences</h2>
 * <ul>
 *   <li>"Survived the dragon attack on Redstone Village" - might apply to dragon-related encounters</li>
 *   <li>"Apprenticed with the royal blacksmith" - useful for crafting or metalworking checks</li>
 *   <li>"Negotiated peace between rival merchant guilds" - applies to diplomatic situations</li>
 * </ul>
 */
@Entity
@Table(name = "experiences")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Experience extends BaseEntity {

    /**
     * The character sheet this experience belongs to.
     * Each experience is permanently tied to a specific character.
     * When the character sheet is deleted, all associated experiences are also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_sheet_id", nullable = false)
    private CharacterSheet characterSheet;

    /**
     * The user who created this experience entry.
     * Typically this is the character's player or the GM.
     * When the user is deleted, all experiences they created are also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    /**
     * Detailed description of the experience.
     * <p>
     * This narrative text describes what happened, what the character learned,
     * or how this experience shaped the character. The description helps both
     * the player and GM remember the context and determine when the modifier
     * should apply.
     * </p>
     * <p>
     * The description should be specific enough to be meaningful but general
     * enough to be useful in multiple situations. For example, "Survived a
     * goblin ambush" is better than just "Fought goblins" because it provides
     * context about adversity and survival.
     * </p>
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * The bonus modifier granted by this experience.
     * <p>
     * This value is added to relevant dice rolls when the character attempts
     * actions where this experience would be applicable. The default value is +2,
     * which is the standard experience bonus in Daggerheart.
     * </p>
     * <p>
     * While the default is +2, the modifier can be adjusted by the GM for
     * particularly significant or minor experiences. Most experiences should
     * use the default value to maintain game balance.
     * </p>
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer modifier = 2;
}
