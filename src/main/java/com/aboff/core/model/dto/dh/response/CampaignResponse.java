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
 * Response DTO for Campaign entities.
 * Represents a campaign in the Daggerheart TTRPG system.
 * <p>
 * Supports expansion via the ?expand parameter for related entities:
 * </p>
 * <ul>
 *   <li>creator: Full user object for the campaign creator</li>
 *   <li>gameMasters: List of user objects for game masters</li>
 *   <li>players: List of user objects for players</li>
 *   <li>pendingCharacterSheets: List of character sheet objects pending approval</li>
 *   <li>playerCharacters: List of approved player character objects</li>
 *   <li>nonPlayerCharacters: List of NPC character sheet objects</li>
 *   <li>all: Expands all relationships</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignResponse {

    /**
     * Unique identifier for the campaign
     */
    private Long id;

    // ========== Basic Information ==========

    /**
     * The campaign's name
     */
    private String name;

    /**
     * The campaign's description
     */
    private String description;

    // ========== Ownership ==========

    /**
     * ID of the user who created this campaign (always included)
     */
    private Long creatorId;

    /**
     * Full user object (included only when ?expand=creator is specified)
     */
    private UserResponse creator;

    // ========== Game Masters ==========

    /**
     * IDs of game masters (always included)
     */
    private List<Long> gameMasterIds;

    /**
     * Full user objects for game masters (included only when ?expand=gameMasters is specified)
     */
    private List<UserResponse> gameMasters;

    // ========== Players ==========

    /**
     * IDs of players (always included)
     */
    private List<Long> playerIds;

    /**
     * Full user objects for players (included only when ?expand=players is specified)
     */
    private List<UserResponse> players;

    // ========== Pending Character Sheets ==========

    /**
     * IDs of pending character sheets (always included)
     */
    private List<Long> pendingCharacterSheetIds;

    /**
     * Full character sheet objects (included only when ?expand=pendingCharacterSheets is specified)
     */
    private List<CharacterSheetResponse> pendingCharacterSheets;

    // ========== Player Characters ==========

    /**
     * IDs of player characters (always included)
     */
    private List<Long> playerCharacterIds;

    /**
     * Full character sheet objects (included only when ?expand=playerCharacters is specified)
     */
    private List<CharacterSheetResponse> playerCharacters;

    // ========== Non-Player Characters ==========

    /**
     * IDs of non-player characters (always included)
     */
    private List<Long> nonPlayerCharacterIds;

    /**
     * Full character sheet objects (included only when ?expand=nonPlayerCharacters is specified)
     */
    private List<CharacterSheetResponse> nonPlayerCharacters;

    // ========== Timestamps ==========

    /**
     * Timestamp when the campaign was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the campaign was last modified
     */
    private LocalDateTime lastModifiedAt;

    /**
     * Timestamp when the campaign was soft-deleted (null if not deleted)
     */
    private LocalDateTime deletedAt;
}
