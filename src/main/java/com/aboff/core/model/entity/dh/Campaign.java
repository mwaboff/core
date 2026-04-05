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
 * Entity representing a campaign in the Daggerheart TTRPG system.
 * <p>
 * A campaign is a collection of game sessions that allows groups of users to play together.
 * It tracks:
 * </p>
 * <ul>
 *   <li>Basic information (name, description)</li>
 *   <li>Creator - the user who created and owns the campaign</li>
 *   <li>Game Masters - users who can manage the campaign and NPCs</li>
 *   <li>Players - users participating in the campaign</li>
 *   <li>Pending Character Sheets - characters awaiting GM approval</li>
 *   <li>Player Characters - approved characters controlled by players</li>
 *   <li>Non-Player Characters - NPCs controlled by the GM</li>
 * </ul>
 * <p>
 * Campaigns support soft deletion to preserve game history when a campaign ends.
 * The same character sheet can exist in multiple campaigns with different states
 * (e.g., an NPC in one campaign and a PC in another).
 * </p>
 */
@Entity
@Table(name = "campaigns")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign extends BaseEntity {

    // ========== Basic Information ==========

    /**
     * The campaign's name.
     * This is the primary identifier for the campaign and is displayed prominently
     * in the UI.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The campaign's description.
     * Optional field providing additional context about the campaign setting,
     * rules, or other details.
     */
    @Column(length = 2000)
    private String description;

    // ========== Ownership ==========

    /**
     * The user who created and owns this campaign.
     * The creator has full control over the campaign including adding/removing GMs,
     * updating campaign details, and deleting the campaign.
     * When the creator is deleted, the campaign is also deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // ========== User Collections ==========

    /**
     * Game Masters for this campaign.
     * GMs can manage players, approve character sheets, add NPCs, and run game sessions.
     * The creator is typically also a GM but this is not enforced at the entity level.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_game_masters",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> gameMasters = new HashSet<>();

    /**
     * Players participating in this campaign.
     * Players can submit character sheets for approval and participate in game sessions.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_players",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> players = new HashSet<>();

    // ========== Character Sheet Collections ==========

    /**
     * Character sheets pending approval.
     * When a player submits a character for this campaign, it goes into the pending
     * collection until a GM approves or rejects it.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_pending_character_sheets",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "character_sheet_id")
    )
    @Builder.Default
    private Set<CharacterSheet> pendingCharacterSheets = new HashSet<>();

    /**
     * Approved player characters for this campaign.
     * These are active characters controlled by players during game sessions.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_player_characters",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "character_sheet_id")
    )
    @Builder.Default
    private Set<CharacterSheet> playerCharacters = new HashSet<>();

    /**
     * Non-player characters for this campaign.
     * NPCs are controlled by the GM and include allies, enemies, and other characters
     * that populate the game world.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "campaign_non_player_characters",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "character_sheet_id")
    )
    @Builder.Default
    private Set<CharacterSheet> nonPlayerCharacters = new HashSet<>();

    // ========== Campaign Lifecycle ==========

    /**
     * Timestamp indicating when this campaign was ended.
     * If null, the campaign is still active.
     * <p>
     * An ended campaign is locked (no new players, character submissions, or updates)
     * but remains visible. This is distinct from soft deletion: ended campaigns are
     * still accessible for viewing and limited operations (like unlinking characters
     * or leaving), while deleted campaigns are invisible.
     * </p>
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // ========== Soft Deletion ==========

    /**
     * Timestamp indicating when this campaign was soft-deleted.
     * If null, the campaign is not deleted.
     * <p>
     * Soft deletion makes the campaign invisible from normal queries.
     * This is distinct from ending: a deleted campaign is fully hidden,
     * while an ended campaign is locked but still visible.
     * </p>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ========== Helper Methods ==========

    /**
     * Returns whether this campaign has been soft-deleted.
     *
     * @return true if the campaign is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the campaign by setting the deleted_at timestamp to the current time.
     * The campaign remains in the database but is filtered out from normal queries.
     * This preserves campaign history and relationships.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted campaign by clearing the deleted_at timestamp.
     * The campaign becomes active and available for use again.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Returns whether this campaign has been ended.
     *
     * @return true if the campaign is ended, false otherwise
     */
    public boolean isEnded() {
        return endedAt != null;
    }

    /**
     * Ends the campaign by setting the ended_at timestamp to the current time.
     * An ended campaign is locked but remains visible.
     */
    public void endCampaign() {
        this.endedAt = LocalDateTime.now();
    }

    /**
     * Checks if the specified user is the creator of this campaign.
     *
     * @param userId the ID of the user to check
     * @return true if the user is the creator, false otherwise
     */
    public boolean isCreator(Long userId) {
        return creator != null && creator.getId().equals(userId);
    }

    /**
     * Checks if the specified user is a game master of this campaign.
     *
     * @param userId the ID of the user to check
     * @return true if the user is a game master, false otherwise
     */
    public boolean isGameMaster(Long userId) {
        return gameMasters.stream().anyMatch(gm -> gm.getId().equals(userId));
    }

    /**
     * Checks if the specified user is a player in this campaign.
     *
     * @param userId the ID of the user to check
     * @return true if the user is a player, false otherwise
     */
    public boolean isPlayer(Long userId) {
        return players.stream().anyMatch(player -> player.getId().equals(userId));
    }

    /**
     * Checks if the specified user is involved in this campaign in any role
     * (creator, game master, or player).
     *
     * @param userId the ID of the user to check
     * @return true if the user is involved in the campaign, false otherwise
     */
    public boolean isInvolved(Long userId) {
        return isCreator(userId) || isGameMaster(userId) || isPlayer(userId);
    }
}
