package com.aboff.core.model.dto.dh.response;

import com.aboff.core.model.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for CharacterSheet entities.
 * Represents a character sheet in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter for related entities:
 * - owner: Full user object for the character owner
 * - experiences: List of experience objects
 * - communityCards: List of community card objects
 * - ancestryCards: List of ancestry card objects
 * - subclassCards: List of subclass card objects
 * - domainCards: List of domain card objects
 * - inventoryWeapons: Full weapon objects nested in inventory weapon entries
 * - inventoryArmors: Full armor objects nested in inventory armor entries
 * - inventoryItems: Full loot objects nested in inventory loot entries
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterSheetResponse {

    /**
     * Unique identifier for the character sheet
     */
    private Long id;

    // ========== Basic Information ==========

    /**
     * The character's name
     */
    private String name;

    /**
     * The character's pronouns
     */
    private String pronouns;

    /**
     * The character's current level (1-10)
     */
    private Integer level;

    /**
     * The character's proficiency bonus
     */
    private Integer proficiency;

    // ========== Combat Attributes ==========

    /**
     * Evasion score
     */
    private Integer evasion;

    /**
     * Maximum armor value
     */
    private Integer armorMax;

    /**
     * Currently marked armor slots
     */
    private Integer armorMarked;

    /**
     * Major damage threshold
     */
    private Integer majorDamageThreshold;

    /**
     * Severe damage threshold
     */
    private Integer severeDamageThreshold;

    // ========== Trait Modifiers and Marked Status ==========

    /**
     * AGILITY trait modifier
     */
    private Integer agilityModifier;

    /**
     * Whether AGILITY trait is marked
     */
    private Boolean agilityMarked;

    /**
     * STRENGTH trait modifier
     */
    private Integer strengthModifier;

    /**
     * Whether STRENGTH trait is marked
     */
    private Boolean strengthMarked;

    /**
     * FINESSE trait modifier
     */
    private Integer finesseModifier;

    /**
     * Whether FINESSE trait is marked
     */
    private Boolean finesseMarked;

    /**
     * INSTINCT trait modifier
     */
    private Integer instinctModifier;

    /**
     * Whether INSTINCT trait is marked
     */
    private Boolean instinctMarked;

    /**
     * PRESENCE trait modifier
     */
    private Integer presenceModifier;

    /**
     * Whether PRESENCE trait is marked
     */
    private Boolean presenceMarked;

    /**
     * KNOWLEDGE trait modifier
     */
    private Integer knowledgeModifier;

    /**
     * Whether KNOWLEDGE trait is marked
     */
    private Boolean knowledgeMarked;

    // ========== Resources ==========

    /**
     * Maximum hit points
     */
    private Integer hitPointMax;

    /**
     * Currently marked hit points
     */
    private Integer hitPointMarked;

    /**
     * Maximum stress points
     */
    private Integer stressMax;

    /**
     * Currently marked stress points
     */
    private Integer stressMarked;

    /**
     * Maximum hope points
     */
    private Integer hopeMax;

    /**
     * Currently marked hope points
     */
    private Integer hopeMarked;

    // ========== Economy ==========

    /**
     * Amount of gold the character has
     */
    private Integer gold;

    // ========== Campaign Info ==========

    /**
     * ID of the active campaign this character is in (populated when viewer has access)
     */
    private Long campaignId;

    /**
     * Name of the active campaign this character is in (populated when viewer has access)
     */
    private String campaignName;

    // ========== Ownership ==========

    /**
     * ID of the user who owns this character sheet (always included)
     */
    private Long ownerId;

    /**
     * Username of the user who owns this character sheet (always included)
     */
    private String ownerName;

    /**
     * Full user object (included only when ?expand=owner is specified)
     */
    private UserResponse owner;

    // ========== Card Collections ==========

    /**
     * IDs of community cards (always included)
     */
    private List<Long> communityCardIds;

    /**
     * Full community card objects (included only when ?expand=communityCards is specified)
     */
    private List<CommunityCardResponse> communityCards;

    /**
     * IDs of ancestry cards (always included)
     */
    private List<Long> ancestryCardIds;

    /**
     * Full ancestry card objects (included only when ?expand=ancestryCards is specified)
     */
    private List<AncestryCardResponse> ancestryCards;

    /**
     * IDs of subclass cards (always included)
     */
    private List<Long> subclassCardIds;

    /**
     * Full subclass card objects (included only when ?expand=subclassCards is specified)
     */
    private List<SubclassCardResponse> subclassCards;

    /**
     * IDs of all domain cards (always included, for backward compatibility)
     */
    private List<Long> domainCardIds;

    /**
     * IDs of equipped domain cards (always included)
     */
    private List<Long> equippedDomainCardIds;

    /**
     * IDs of vault (unequipped) domain cards (always included)
     */
    private List<Long> vaultDomainCardIds;

    /**
     * Full domain card objects (included only when ?expand=domainCards is specified)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DomainCardResponse> domainCards;

    /**
     * Full equipped domain card objects (included only when ?expand=equippedDomainCards is specified)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DomainCardResponse> equippedDomainCards;

    /**
     * Full vault domain card objects (included only when ?expand=vaultDomainCards is specified)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DomainCardResponse> vaultDomainCards;

    // ========== Inventory ==========

    /**
     * Weapons in inventory with linking entity IDs, equipped status, and slot.
     * Always included. Full weapon objects nested when {@code ?expand=inventoryWeapons} is specified.
     */
    private List<InventoryWeaponResponse> inventoryWeapons;

    /**
     * Armor pieces in inventory with linking entity IDs and equipped status.
     * Always included. Full armor objects nested when {@code ?expand=inventoryArmors} is specified.
     */
    private List<InventoryArmorResponse> inventoryArmors;

    /**
     * Loot items in inventory with linking entity IDs.
     * Always included. Full loot objects nested when {@code ?expand=inventoryItems} is specified.
     */
    private List<InventoryLootResponse> inventoryItems;

    // ========== Experiences ==========

    /**
     * IDs of experiences for this character (always included)
     */
    private List<Long> experienceIds;

    /**
     * Full experience objects (included only when ?expand=experiences is specified)
     */
    private List<ExperienceResponse> experiences;

    // ========== Timestamps ==========

    /**
     * Timestamp when the character sheet was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the character sheet was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the character sheet was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
