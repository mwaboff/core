package com.aboff.core.model.enums;

import lombok.Getter;

/**
 * Enum defining all auditable user actions in the system.
 * <p>
 * Each action has a human-readable label used in structured audit logs.
 * Grouped by domain for clarity. Designed to serve as a registry for
 * future metrics integration (e.g., New Relic).
 * </p>
 */
@Getter
public enum AuditAction {

    // Authentication
    USER_LOGIN("User login"),
    USER_LOGOUT("User logout"),
    USER_TOKENS_INVALIDATED("User tokens invalidated"),

    // User Management
    USER_PROFILE_UPDATED("User profile updated"),
    USER_USERNAME_CHOSEN("Username chosen"),
    USER_ACCOUNT_DELETED("User account deleted"),
    USER_PROVISIONED("User provisioned"),

    // Campaign
    CAMPAIGN_CREATED("Campaign created"),
    CAMPAIGN_UPDATED("Campaign updated"),
    CAMPAIGN_DELETED("Campaign deleted"),
    CAMPAIGN_ENDED("Campaign ended"),
    CAMPAIGN_GM_ADDED("Game master added"),
    CAMPAIGN_GM_REMOVED("Game master removed"),
    CAMPAIGN_PLAYER_ADDED("Player added"),
    CAMPAIGN_PLAYER_KICKED("Player kicked"),
    CAMPAIGN_PLAYER_LEFT("Player left"),
    CAMPAIGN_INVITE_GENERATED("Campaign invite generated"),
    CAMPAIGN_JOINED_VIA_INVITE("Joined campaign via invite"),
    CAMPAIGN_CHARACTER_SUBMITTED("Character submitted"),
    CAMPAIGN_CHARACTER_APPROVED("Character approved"),
    CAMPAIGN_CHARACTER_REJECTED("Character rejected"),
    CAMPAIGN_NPC_ADDED("NPC added"),
    CAMPAIGN_CHARACTER_REMOVED("Character removed"),
    CAMPAIGN_FEAR_UPDATED("Campaign fear updated"),
    CAMPAIGN_GM_NOTES_UPDATED("Campaign GM notes updated"),
    CAMPAIGN_COUNTDOWN_CREATED("Campaign countdown created"),
    CAMPAIGN_COUNTDOWN_UPDATED("Campaign countdown updated"),
    CAMPAIGN_COUNTDOWN_DELETED("Campaign countdown deleted"),
    CAMPAIGN_TRANSFORMATION_ACCESS_UPDATED("Campaign transformation access updated"),

    // Character Sheet
    CHARACTER_CREATED("Character created"),
    CHARACTER_UPDATED("Character updated"),
    CHARACTER_DELETED("Character deleted"),
    CHARACTER_LEVELED_UP("Character leveled up"),
    CHARACTER_LEVEL_UNDONE("Character level-up undone"),

    // Companion
    COMPANION_CREATED("Companion created"),
    COMPANION_UPDATED("Companion updated"),
    COMPANION_DELETED("Companion deleted"),

    // Experience
    EXPERIENCE_CREATED("Experience created"),
    EXPERIENCE_UPDATED("Experience updated"),
    EXPERIENCE_DELETED("Experience deleted"),

    // Adversary
    ADVERSARY_CREATED("Adversary created"),
    ADVERSARY_BATCH_CREATED("Adversaries batch created"),
    ADVERSARY_UPDATED("Adversary updated"),
    ADVERSARY_DELETED("Adversary deleted"),
    ADVERSARY_RESTORED("Adversary restored"),
    ADVERSARY_COPIED("Adversary copied"),

    // Encounter
    ENCOUNTER_CREATED("Encounter created"),
    ENCOUNTER_UPDATED("Encounter updated"),
    ENCOUNTER_DELETED("Encounter deleted"),
    ENCOUNTER_RESTORED("Encounter restored"),
    ENCOUNTER_COPIED("Encounter copied"),
    ENCOUNTER_ADVERSARY_ADDED("Adversary added to encounter"),
    ENCOUNTER_ADVERSARY_REMOVED("Adversary removed from encounter"),

    // Encounter Run
    ENCOUNTER_RUN_STARTED("Encounter run started"),
    ENCOUNTER_RUN_ADVERSARY_UPDATED("Encounter run adversary updated"),
    ENCOUNTER_RUN_COMPLETED("Encounter run completed"),
    ENCOUNTER_RUN_DELETED("Encounter run deleted"),

    // Game Content (Weapon, Armor, Loot, Domain, Class, Cards, etc.)
    CONTENT_CREATED("Content created"),
    CONTENT_BATCH_CREATED("Content batch created"),
    CONTENT_UPDATED("Content updated"),
    CONTENT_DELETED("Content deleted"),
    CONTENT_RESTORED("Content restored");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }
}
