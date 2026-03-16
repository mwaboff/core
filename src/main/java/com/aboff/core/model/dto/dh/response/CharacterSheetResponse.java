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
 * - activePrimaryWeapon: Full weapon object
 * - activeSecondaryWeapon: Full weapon object
 * - activeArmor: Full armor object
 * - communityCards: List of community card objects
 * - ancestryCards: List of ancestry card objects
 * - subclassCards: List of subclass card objects
 * - domainCards: List of domain card objects
 * - inventoryWeapons: List of weapon objects in inventory
 * - inventoryArmors: List of armor objects in inventory
 * - inventoryItems: List of loot objects in inventory
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

    // ========== Ownership ==========

    /**
     * ID of the user who owns this character sheet (always included)
     */
    private Long ownerId;

    /**
     * Full user object (included only when ?expand=owner is specified)
     */
    private UserResponse owner;

    // ========== Active Equipment ==========

    /**
     * ID of currently equipped primary weapon (always included, may be null)
     */
    private Long activePrimaryWeaponId;

    /**
     * Full primary weapon object (included only when ?expand=activePrimaryWeapon is specified)
     */
    private WeaponResponse activePrimaryWeapon;

    /**
     * ID of currently equipped secondary weapon (always included, may be null)
     */
    private Long activeSecondaryWeaponId;

    /**
     * Full secondary weapon object (included only when ?expand=activeSecondaryWeapon is specified)
     */
    private WeaponResponse activeSecondaryWeapon;

    /**
     * ID of currently equipped armor (always included, may be null)
     */
    private Long activeArmorId;

    /**
     * Full armor object (included only when ?expand=activeArmor is specified)
     */
    private ArmorResponse activeArmor;

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
     * IDs of weapons in inventory (always included)
     */
    private List<Long> inventoryWeaponIds;

    /**
     * Full weapon objects in inventory (included only when ?expand=inventoryWeapons is specified)
     */
    private List<WeaponResponse> inventoryWeapons;

    /**
     * IDs of armor pieces in inventory (always included)
     */
    private List<Long> inventoryArmorIds;

    /**
     * Full armor objects in inventory (included only when ?expand=inventoryArmors is specified)
     */
    private List<ArmorResponse> inventoryArmors;

    /**
     * IDs of loot items in inventory (always included)
     */
    private List<Long> inventoryItemIds;

    /**
     * Full loot objects in inventory (included only when ?expand=inventoryItems is specified)
     */
    private List<LootResponse> inventoryItems;

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
