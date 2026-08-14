package com.aboff.core.model.dto.dh.request;

import com.aboff.core.model.dto.dh.PrayerDieDto;
import jakarta.validation.Valid;
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

    /**
     * Updated proficiency bonus
     */
    @Min(value = 1, message = "Proficiency must be at least 1")
    private Integer proficiency;

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

    // ========== Hope & Fear Resources ==========

    /**
     * Updated maximum Focus this character can hold (capped at 6 by rule; not enforced here to
     * allow homebrew, but the marked value is clamped down if it would exceed the new max).
     */
    @PositiveOrZero(message = "Focus max must be zero or positive")
    private Integer focusMax;

    /**
     * Updated Focus currently held. Clamped to {@code 0..focusMax} by the service.
     */
    @PositiveOrZero(message = "Focus marked must be zero or positive")
    private Integer focusMarked;

    /**
     * Updated Favor amount (Warlock resource).
     */
    @PositiveOrZero(message = "Favor must be zero or positive")
    private Integer favor;

    /**
     * Updated Seraph Prayer Dice for the current session, in roll order (null to leave unchanged,
     * empty list to clear the dice). Bounded so a hostile payload cannot overflow the column.
     */
    @Valid
    @Size(max = 16, message = "Prayer dice must not exceed 16 dice")
    private List<PrayerDieDto> prayerDice;

    /**
     * ID of the transformation card to attach to this character.
     * A character may have at most one transformation. Ignored (and left unchanged) if
     * {@link #clearTransformationCard} is true.
     */
    private Long transformationCardId;

    /**
     * Explicit flag to detach the character's transformation card, clearing
     * {@code transformationCardId}, {@code transformationTokens}, and {@code wolfFormActive}
     * together. Partial-update requests only apply non-null fields, so a plain null
     * {@link #transformationCardId} can't otherwise distinguish "leave unchanged" from
     * "detach the transformation" -- see {@link #clearDifficulty} on
     * {@code UpdateEnvironmentRequest} for the same pattern.
     */
    private Boolean clearTransformationCard;

    /**
     * Updated Vampire "Feed" token count. Clamped to {@code 0..6} by the service.
     * Null when the character's transformation does not use a token pool.
     */
    @PositiveOrZero(message = "Transformation tokens must be zero or positive")
    private Integer transformationTokens;

    /**
     * Whether the Werewolf transformation's "Wolf Form" is currently active.
     */
    private Boolean wolfFormActive;

    /**
     * Updated IDs of martial stances this character knows (null to leave unchanged, empty list
     * to clear all known stances).
     */
    private List<Long> knownMartialStanceIds;

    /**
     * ID of the martial stance the character is currently shifted into.
     * Must be a member of the sheet's known stances. Ignored (and left unchanged) if
     * {@link #clearActiveMartialStance} is true.
     */
    private Long activeMartialStanceId;

    /**
     * Explicit flag to drop the character's active stance back to none. See
     * {@link #clearTransformationCard} for why a boolean flag is needed instead of a plain null.
     */
    private Boolean clearActiveMartialStance;

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

    /**
     * Updated IDs of equipped domain cards (must be provided together with vaultDomainCardIds)
     */
    private List<Long> equippedDomainCardIds;

    /**
     * Updated IDs of vault (unequipped) domain cards (must be provided together with equippedDomainCardIds)
     */
    private List<Long> vaultDomainCardIds;

    // ========== Inventory ==========

    /**
     * Updated weapons in inventory (null to leave unchanged, provided list = full replacement).
     * Supports duplicate weapon IDs for multiple copies of the same weapon.
     */
    @Valid
    private List<InventoryWeaponRequest> inventoryWeapons;

    /**
     * Updated armor pieces in inventory (null to leave unchanged, provided list = full replacement).
     * Multiple armor pieces can be equipped simultaneously.
     */
    @Valid
    private List<InventoryArmorRequest> inventoryArmors;

    /**
     * Updated loot items in inventory (null to leave unchanged, provided list = full replacement).
     * Supports duplicate loot IDs for multiple copies of the same item.
     */
    @Valid
    private List<InventoryLootRequest> inventoryItems;
}
