package com.aboff.core.model.dto.dh.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new CharacterSheet.
 * <p>
 * Contains all fields required to create a character sheet in the Daggerheart TTRPG system,
 * including basic information, combat stats, trait modifiers, resources, economy, and equipment/cards.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterSheetRequest {

    // ========== Basic Information ==========

    /**
     * The character's name
     */
    @NotBlank(message = "Character name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * The character's pronouns (optional)
     */
    @Size(max = 100, message = "Pronouns must not exceed 100 characters")
    private String pronouns;

    /**
     * The character's level (1-10)
     */
    @NotNull(message = "Level is required")
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 10, message = "Level must not exceed 10")
    private Integer level;

    /**
     * The character's proficiency bonus (optional, defaults to 1)
     */
    @Min(value = 1, message = "Proficiency must be at least 1")
    private Integer proficiency;

    // ========== Combat Attributes ==========

    /**
     * Evasion score
     */
    @NotNull(message = "Evasion is required")
    @PositiveOrZero(message = "Evasion must be zero or positive")
    private Integer evasion;

    /**
     * Maximum armor value
     */
    @NotNull(message = "Armor max is required")
    @PositiveOrZero(message = "Armor max must be zero or positive")
    private Integer armorMax;

    /**
     * Currently marked armor slots
     */
    @NotNull(message = "Armor marked is required")
    @PositiveOrZero(message = "Armor marked must be zero or positive")
    private Integer armorMarked;

    /**
     * Major damage threshold
     */
    @NotNull(message = "Major damage threshold is required")
    @Positive(message = "Major damage threshold must be positive")
    private Integer majorDamageThreshold;

    /**
     * Severe damage threshold
     */
    @NotNull(message = "Severe damage threshold is required")
    @Positive(message = "Severe damage threshold must be positive")
    private Integer severeDamageThreshold;

    // ========== Trait Modifiers and Marked Status ==========

    /**
     * AGILITY trait modifier
     */
    @NotNull(message = "Agility modifier is required")
    private Integer agilityModifier;

    /**
     * Whether AGILITY trait is marked
     */
    @NotNull(message = "Agility marked status is required")
    private Boolean agilityMarked;

    /**
     * STRENGTH trait modifier
     */
    @NotNull(message = "Strength modifier is required")
    private Integer strengthModifier;

    /**
     * Whether STRENGTH trait is marked
     */
    @NotNull(message = "Strength marked status is required")
    private Boolean strengthMarked;

    /**
     * FINESSE trait modifier
     */
    @NotNull(message = "Finesse modifier is required")
    private Integer finesseModifier;

    /**
     * Whether FINESSE trait is marked
     */
    @NotNull(message = "Finesse marked status is required")
    private Boolean finesseMarked;

    /**
     * INSTINCT trait modifier
     */
    @NotNull(message = "Instinct modifier is required")
    private Integer instinctModifier;

    /**
     * Whether INSTINCT trait is marked
     */
    @NotNull(message = "Instinct marked status is required")
    private Boolean instinctMarked;

    /**
     * PRESENCE trait modifier
     */
    @NotNull(message = "Presence modifier is required")
    private Integer presenceModifier;

    /**
     * Whether PRESENCE trait is marked
     */
    @NotNull(message = "Presence marked status is required")
    private Boolean presenceMarked;

    /**
     * KNOWLEDGE trait modifier
     */
    @NotNull(message = "Knowledge modifier is required")
    private Integer knowledgeModifier;

    /**
     * Whether KNOWLEDGE trait is marked
     */
    @NotNull(message = "Knowledge marked status is required")
    private Boolean knowledgeMarked;

    // ========== Resources ==========

    /**
     * Maximum hit points
     */
    @NotNull(message = "Hit point max is required")
    @Positive(message = "Hit point max must be positive")
    private Integer hitPointMax;

    /**
     * Currently marked hit points
     */
    @NotNull(message = "Hit point marked is required")
    @PositiveOrZero(message = "Hit point marked must be zero or positive")
    private Integer hitPointMarked;

    /**
     * Maximum stress points
     */
    @NotNull(message = "Stress max is required")
    @Positive(message = "Stress max must be positive")
    private Integer stressMax;

    /**
     * Currently marked stress points
     */
    @NotNull(message = "Stress marked is required")
    @PositiveOrZero(message = "Stress marked must be zero or positive")
    private Integer stressMarked;

    /**
     * Maximum hope points
     */
    @NotNull(message = "Hope max is required")
    @Positive(message = "Hope max must be positive")
    private Integer hopeMax;

    /**
     * Currently marked hope points
     */
    @NotNull(message = "Hope marked is required")
    @PositiveOrZero(message = "Hope marked must be zero or positive")
    private Integer hopeMarked;

    // ========== Economy ==========

    /**
     * Amount of gold the character has
     */
    @NotNull(message = "Gold is required")
    @PositiveOrZero(message = "Gold must be zero or positive")
    private Integer gold;

    // ========== Card IDs ==========

    /**
     * IDs of community cards associated with this character (optional)
     */
    private List<Long> communityCardIds;

    /**
     * IDs of ancestry cards associated with this character (optional)
     */
    private List<Long> ancestryCardIds;

    /**
     * IDs of subclass cards associated with this character (optional)
     */
    private List<Long> subclassCardIds;

    /**
     * IDs of equipped domain cards (must be provided together with vaultDomainCardIds)
     */
    private List<Long> equippedDomainCardIds;

    /**
     * IDs of vault (unequipped) domain cards (must be provided together with equippedDomainCardIds)
     */
    private List<Long> vaultDomainCardIds;

    // ========== Inventory ==========

    /**
     * Weapons in inventory with equipped status and slot assignment (optional).
     * Supports duplicate weapon IDs for multiple copies of the same weapon.
     */
    @Valid
    private List<InventoryWeaponRequest> inventoryWeapons;

    /**
     * Armor pieces in inventory with equipped status (optional).
     * Multiple armor pieces can be equipped simultaneously.
     */
    @Valid
    private List<InventoryArmorRequest> inventoryArmors;

    /**
     * Loot items in inventory (optional).
     * Supports duplicate loot IDs for multiple copies of the same item.
     */
    @Valid
    private List<InventoryLootRequest> inventoryItems;
}
