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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a character sheet in the Daggerheart TTRPG system.
 * <p>
 * This is the central entity for player characters, containing all character data including:
 * </p>
 * <ul>
 *   <li>Basic information (name, pronouns, level)</li>
 *   <li>Combat statistics (evasion, armor, damage thresholds)</li>
 *   <li>Six core traits (AGILITY, STRENGTH, FINESSE, INSTINCT, PRESENCE, KNOWLEDGE)</li>
 *   <li>Resources (hit points, stress, hope)</li>
 *   <li>Economy (gold)</li>
 *   <li>Equipment (active weapons and armor)</li>
 *   <li>Collections (community cards, ancestry cards, subclass cards)</li>
 *   <li>Inventory (weapons, armor, and loot items)</li>
 *   <li>Experiences (narrative bonuses from character history)</li>
 * </ul>
 * <p>
 * Character sheets are owned by users and support soft deletion to preserve
 * character history when a character is retired or removed.
 * </p>
 */
@Entity
@Table(name = "character_sheets")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSheet extends BaseEntity {

    // ========== Basic Information ==========

    /**
     * The character's name.
     * This is the primary identifier for the character and is displayed prominently
     * in the UI and in game sessions.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The character's pronouns (e.g., "she/her", "he/him", "they/them").
     * Optional field that helps establish the character's identity and ensures
     * respectful roleplay.
     */
    @Column(length = 100)
    private String pronouns;

    /**
     * The character's current level (1-10).
     * Level represents overall character power and experience, unlocking new
     * abilities and improving existing ones. Characters start at level 1 and
     * advance through gameplay.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    // ========== Combat Attributes ==========

    /**
     * The character's evasion score.
     * Used to avoid or mitigate incoming attacks. Higher evasion makes the
     * character harder to hit.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer evasion = 0;

    /**
     * The maximum armor value for this character.
     * Represents the total armor slots available for marking when taking damage.
     * Armor can be increased through equipment and abilities.
     */
    @Column(name = "armor_max", nullable = false)
    @Builder.Default
    private Integer armorMax = 0;

    /**
     * The number of armor slots currently marked (damaged).
     * When armor is marked, it represents wear and damage to the character's
     * protection. Must not exceed armorMax.
     */
    @Column(name = "armor_marked", nullable = false)
    @Builder.Default
    private Integer armorMarked = 0;

    /**
     * The damage threshold for suffering a major injury.
     * When a character takes damage at or above this threshold (but below severe),
     * they sustain a major wound with significant mechanical effects.
     */
    @Column(name = "major_damage_threshold", nullable = false)
    private Integer majorDamageThreshold;

    /**
     * The damage threshold for suffering a severe injury.
     * When a character takes damage at or above this threshold, they sustain
     * a severe wound with potentially life-threatening consequences.
     * Must be greater than or equal to majorDamageThreshold.
     */
    @Column(name = "severe_damage_threshold", nullable = false)
    private Integer severeDamageThreshold;

    // ========== Trait Modifiers and Marked Status ==========

    /**
     * Modifier for AGILITY trait rolls.
     * AGILITY covers quick reflexes, nimbleness, coordination, dodging, acrobatics,
     * sleight of hand, and stealth.
     */
    @Column(name = "agility_modifier", nullable = false)
    @Builder.Default
    private Integer agilityModifier = 0;

    /**
     * Whether the AGILITY trait is currently marked.
     * Marked traits cannot be used until cleared, representing exhaustion or
     * temporary impairment in that area.
     */
    @Column(name = "agility_marked", nullable = false)
    @Builder.Default
    private Boolean agilityMarked = false;

    /**
     * Modifier for STRENGTH trait rolls.
     * STRENGTH covers raw physical power, endurance, melee attacks, athletics,
     * breaking objects, and carrying heavy loads.
     */
    @Column(name = "strength_modifier", nullable = false)
    @Builder.Default
    private Integer strengthModifier = 0;

    /**
     * Whether the STRENGTH trait is currently marked.
     */
    @Column(name = "strength_marked", nullable = false)
    @Builder.Default
    private Boolean strengthMarked = false;

    /**
     * Modifier for FINESSE trait rolls.
     * FINESSE covers precision, grace, careful execution, ranged attacks,
     * lockpicking, crafting, and precise movements.
     */
    @Column(name = "finesse_modifier", nullable = false)
    @Builder.Default
    private Integer finesseModifier = 0;

    /**
     * Whether the FINESSE trait is currently marked.
     */
    @Column(name = "finesse_marked", nullable = false)
    @Builder.Default
    private Boolean finesseMarked = false;

    /**
     * Modifier for INSTINCT trait rolls.
     * INSTINCT covers intuition, awareness, natural understanding, perception,
     * survival, animal handling, and reading situations.
     */
    @Column(name = "instinct_modifier", nullable = false)
    @Builder.Default
    private Integer instinctModifier = 0;

    /**
     * Whether the INSTINCT trait is currently marked.
     */
    @Column(name = "instinct_marked", nullable = false)
    @Builder.Default
    private Boolean instinctMarked = false;

    /**
     * Modifier for PRESENCE trait rolls.
     * PRESENCE covers force of personality, social influence, persuasion,
     * intimidation, performance, and leadership.
     */
    @Column(name = "presence_modifier", nullable = false)
    @Builder.Default
    private Integer presenceModifier = 0;

    /**
     * Whether the PRESENCE trait is currently marked.
     */
    @Column(name = "presence_marked", nullable = false)
    @Builder.Default
    private Boolean presenceMarked = false;

    /**
     * Modifier for KNOWLEDGE trait rolls.
     * KNOWLEDGE covers learning, reasoning, mental acuity, spellcasting,
     * history, investigation, and arcana.
     */
    @Column(name = "knowledge_modifier", nullable = false)
    @Builder.Default
    private Integer knowledgeModifier = 0;

    /**
     * Whether the KNOWLEDGE trait is currently marked.
     */
    @Column(name = "knowledge_marked", nullable = false)
    @Builder.Default
    private Boolean knowledgeMarked = false;

    // ========== Resources ==========

    /**
     * Maximum hit points for this character.
     * Hit points represent the character's health and ability to sustain injury.
     * When marked hit points reach the maximum, the character is incapacitated.
     */
    @Column(name = "hit_point_max", nullable = false)
    @Builder.Default
    private Integer hitPointMax = 6;

    /**
     * Number of hit points currently marked (damaged).
     * Must not exceed hitPointMax.
     */
    @Column(name = "hit_point_marked", nullable = false)
    @Builder.Default
    private Integer hitPointMarked = 0;

    /**
     * Maximum stress points for this character.
     * Stress represents mental and emotional strain, accumulated through
     * difficult challenges and supernatural effects.
     */
    @Column(name = "stress_max", nullable = false)
    @Builder.Default
    private Integer stressMax = 6;

    /**
     * Number of stress points currently marked (accumulated).
     * Must not exceed stressMax. High stress can lead to negative consequences.
     */
    @Column(name = "stress_marked", nullable = false)
    @Builder.Default
    private Integer stressMarked = 0;

    /**
     * Maximum hope points for this character.
     * Hope is a resource that can be spent to improve rolls, aid allies,
     * and achieve heroic feats.
     */
    @Column(name = "hope_max", nullable = false)
    @Builder.Default
    private Integer hopeMax = 2;

    /**
* Number of hope points currently marked (spent).
     * Must not exceed hopeMax. Hope refreshes during rest periods.
     */
    @Column(name = "hope_marked", nullable = false)
    @Builder.Default
    private Integer hopeMarked = 0;

    // ========== Economy ==========

    /**
     * The amount of gold the character currently possesses.
     * Gold is the primary currency in Daggerheart, used to purchase equipment,
     * services, and other goods.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer gold = 0;

    // ========== Active Equipment ==========

    /**
     * The character's currently equipped primary weapon.
     * This weapon is used for most attacks and is readily accessible.
     * Can be null if the character has no weapon equipped.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_primary_weapon_id")
    private Weapon activePrimaryWeapon;

    /**
     * The character's currently equipped secondary weapon.
     * This might be an off-hand weapon, a backup weapon, or a dual-wielded weapon.
     * Can be null if the character has no secondary weapon equipped.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_secondary_weapon_id")
    private Weapon activeSecondaryWeapon;

    /**
     * The character's currently equipped armor.
     * Determines the character's armor score and damage thresholds.
     * Can be null if the character is unarmored.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_armor_id")
    private Armor activeArmor;

    // ========== Ownership ==========

    /**
     * The user who owns this character sheet.
     * Each character sheet belongs to one player (user account).
     * When the owner is deleted, the character sheet is also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // ========== Card Collections ==========

    /**
     * Community cards associated with this character.
     * Community cards define the character's social background, community ties,
     * and cultural identity. A character typically has one community card but
     * may have multiple in complex backgrounds.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_communities",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "community_card_id")
    )
    @Builder.Default
    private Set<CommunityCard> communityCards = new HashSet<>();

    /**
     * Ancestry cards associated with this character.
     * Ancestry cards define the character's species, heritage, and innate abilities.
     * A character typically has one ancestry card but may have multiple for
     * mixed heritage characters.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_ancestries",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "ancestry_card_id")
    )
    @Builder.Default
    private Set<AncestryCard> ancestryCards = new HashSet<>();

    /**
     * Subclass cards associated with this character.
     * Subclass cards define the character's specialized training, abilities,
     * and role. Characters gain subclass cards as they level up, representing
     * their growing expertise.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_subclasses",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "subclass_card_id")
    )
    @Builder.Default
    private Set<SubclassCard> subclassCards = new HashSet<>();

    // ========== Inventory ==========

    /**
     * Weapons in the character's inventory.
     * This includes all weapons the character owns, whether equipped or stored.
     * The inventory may include multiple weapons for different situations.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_inventory_weapons",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "weapon_id")
    )
    @Builder.Default
    private Set<Weapon> inventoryWeapons = new HashSet<>();

    /**
     * Armor pieces in the character's inventory.
     * This includes all armor the character owns, whether equipped or stored.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_inventory_armors",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "armor_id")
    )
    @Builder.Default
    private Set<Armor> inventoryArmors = new HashSet<>();

    /**
     * Miscellaneous items (loot) in the character's inventory.
     * This includes consumables, tools, treasure, quest items, and other gear
     * that doesn't fall into the weapon or armor categories.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "character_sheet_inventory_items",
        joinColumns = @JoinColumn(name = "character_sheet_id"),
        inverseJoinColumns = @JoinColumn(name = "loot_id")
    )
    @Builder.Default
    private Set<Loot> inventoryItems = new HashSet<>();

    // ========== Experiences ==========

    /**
     * Experiences associated with this character.
     * Experiences represent significant events, accomplishments, and learning
     * moments that provide mechanical bonuses when relevant to the situation.
     * Each experience grants a modifier (typically +2) to applicable rolls.
     */
    @OneToMany(mappedBy = "characterSheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Experience> experiences = new HashSet<>();

    // ========== Companions ==========

    /**
     * Companions associated with this character.
     * Companions are allied creatures, familiars, or followers that accompany
     * the character. Each companion has its own combat capabilities, stress
     * tracking, and can accumulate experiences.
     */
    @OneToMany(mappedBy = "characterSheet", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Companion> companions = new HashSet<>();

    // ========== Soft Deletion ==========

    /**
     * Timestamp indicating when this character sheet was soft-deleted.
     * If null, the character sheet is active and available.
     * Soft deletion preserves character history while removing the sheet from
     * active play.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this character sheet has been soft-deleted.
     *
     * @return true if the character sheet is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the character sheet by setting the deleted_at timestamp to the current time.
     * The character sheet remains in the database but is filtered out from normal queries.
     * This preserves character history and relationships.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted character sheet by clearing the deleted_at timestamp.
     * The character sheet becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
