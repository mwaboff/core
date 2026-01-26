package com.aboff.core.model.dto.dh.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing CharacterSheet.
 * <p>
 * All fields are optional to support partial updates. Only non-null fields
 * will be updated on the character sheet entity.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCharacterSheetRequest {

    // ========== Basic Information ==========

    /**
     * Updated character name
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    /**
     * Updated character pronouns
     */
    @Size(max = 100, message = "Pronouns must not exceed 100 characters")
    private String pronouns;

    /**
     * Updated character level (1-10)
     */
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 10, message = "Level must not exceed 10")
    private Integer level;

    // ========== Combat Attributes ==========

    /**
     * Updated evasion score
     */
    @PositiveOrZero(message = "Evasion must be zero or positive")
    private Integer evasion;

    /**
     * Updated maximum armor value
     */
    @PositiveOrZero(message = "Armor max must be zero or positive")
    private Integer armorMax;

    /**
     * Updated marked armor slots
     */
    @PositiveOrZero(message = "Armor marked must be zero or positive")
    private Integer armorMarked;

    /**
     * Updated major damage threshold
     */
    @Positive(message = "Major damage threshold must be positive")
    private Integer majorDamageThreshold;

    /**
     * Updated severe damage threshold
     */
    @Positive(message = "Severe damage threshold must be positive")
    private Integer severeDamageThreshold;

    // ========== Trait Modifiers and Marked Status ==========

    /**
     * Updated AGILITY trait modifier
     */
    private Integer agilityModifier;

    /**
     * Updated AGILITY marked status
     */
    private Boolean agilityMarked;

    /**
     * Updated STRENGTH trait modifier
     */
    private Integer strengthModifier;

    /**
     * Updated STRENGTH marked status
     */
    private Boolean strengthMarked;

    /**
     * Updated FINESSE trait modifier
     */
    private Integer finesseModifier;

    /**
     * Updated FINESSE marked status
     */
    private Boolean finesseMarked;

    /**
     * Updated INSTINCT trait modifier
     */
    private Integer instinctModifier;

    /**
     * Updated INSTINCT marked status
     */
    private Boolean instinctMarked;

    /**
     * Updated PRESENCE trait modifier
     */
    private Integer presenceModifier;

    /**
     * Updated PRESENCE marked status
     */
    private Boolean presenceMarked;

    /**
     * Updated KNOWLEDGE trait modifier
     */
    private Integer knowledgeModifier;

    /**
     * Updated KNOWLEDGE marked status
     */
    private Boolean knowledgeMarked;

    // ========== Resources ==========

    /**
     * Updated maximum hit points
     */
    @Positive(message = "Hit point max must be positive")
    private Integer hitPointMax;

    /**
     * Updated marked hit points
     */
    @PositiveOrZero(message = "Hit point marked must be zero or positive")
    private Integer hitPointMarked;

    /**
     * Updated maximum stress points
     */
    @Positive(message = "Stress max must be positive")
    private Integer stressMax;

    /**
     * Updated marked stress points
     */
    @PositiveOrZero(message = "Stress marked must be zero or positive")
    private Integer stressMarked;

    /**
     * Updated maximum hope points
     */
    @Positive(message = "Hope max must be positive")
    private Integer hopeMax;

    /**
     * Updated marked hope points
     */
    @PositiveOrZero(message = "Hope marked must be zero or positive")
    private Integer hopeMarked;

    // ========== Economy ==========

    /**
     * Updated gold amount
     */
    @PositiveOrZero(message = "Gold must be zero or positive")
    private Integer gold;

    // ========== Active Equipment IDs ==========

    /**
     * Updated ID of currently equipped primary weapon (null to unequip)
     */
    private Long activePrimaryWeaponId;

    /**
     * Updated ID of currently equipped secondary weapon (null to unequip)
     */
    private Long activeSecondaryWeaponId;

    /**
     * Updated ID of currently equipped armor (null to unequip)
     */
    private Long activeArmorId;

    // ========== Card IDs ==========

    /**
     * Updated IDs of community cards (null to leave unchanged)
     */
    private List<Long> communityCardIds;

    /**
     * Updated IDs of ancestry cards (null to leave unchanged)
     */
    private List<Long> ancestryCardIds;

    /**
     * Updated IDs of subclass cards (null to leave unchanged)
     */
    private List<Long> subclassCardIds;

    // ========== Inventory IDs ==========

    /**
     * Updated IDs of weapons in inventory (null to leave unchanged)
     */
    private List<Long> inventoryWeaponIds;

    /**
     * Updated IDs of armor pieces in inventory (null to leave unchanged)
     */
    private List<Long> inventoryArmorIds;

    /**
     * Updated IDs of loot items in inventory (null to leave unchanged)
     */
    private List<Long> inventoryItemIds;
}
