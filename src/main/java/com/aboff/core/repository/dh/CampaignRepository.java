package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Campaign entity operations.
 * <p>
 * Provides data access methods for campaigns including queries for finding campaigns
 * by creator, game master, player, and character sheet involvement. Supports filtering
 * by various criteria and respects soft deletion.
 * </p>
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * Finds all active (non-deleted) campaigns.
     *
     * @return list of all active campaigns
     */
    @Query("SELECT c FROM Campaign c WHERE c.deletedAt IS NULL")
    List<Campaign> findAllActive();

    /**
     * Finds a campaign by ID only if it is not deleted.
     *
     * @param id the ID of the campaign
     * @return the campaign if found and not deleted
     */
    @Query("SELECT c FROM Campaign c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Campaign> findActiveById(@Param("id") Long id);

    /**
     * Finds all active campaigns created by a specific user.
     *
     * @param creatorId the ID of the user who created the campaigns
     * @return list of active campaigns created by the user
     */
    @Query("SELECT c FROM Campaign c WHERE c.creator.id = :creatorId AND c.deletedAt IS NULL")
    List<Campaign> findActiveByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * Finds all active campaigns where the user is a game master.
     *
     * @param userId the ID of the user
     * @return list of active campaigns where the user is a GM
     */
    @Query("SELECT c FROM Campaign c JOIN c.gameMasters gm WHERE gm.id = :userId AND c.deletedAt IS NULL")
    List<Campaign> findActiveByGameMasterId(@Param("userId") Long userId);

    /**
     * Finds all active campaigns where the user is a player.
     *
     * @param userId the ID of the user
     * @return list of active campaigns where the user is a player
     */
    @Query("SELECT c FROM Campaign c JOIN c.players p WHERE p.id = :userId AND c.deletedAt IS NULL")
    List<Campaign> findActiveByPlayerId(@Param("userId") Long userId);

    /**
     * Finds all active campaigns where the user is involved in any role
     * (creator, game master, or player).
     *
     * @param userId the ID of the user
     * @return list of active campaigns where the user is involved
     */
    @Query("SELECT DISTINCT c FROM Campaign c " +
           "LEFT JOIN c.gameMasters gm " +
           "LEFT JOIN c.players p " +
           "WHERE c.deletedAt IS NULL AND " +
           "(c.creator.id = :userId OR gm.id = :userId OR p.id = :userId)")
    List<Campaign> findActiveByUserInvolvement(@Param("userId") Long userId);

    /**
     * Finds all active campaigns that contain a specific character sheet
     * in any collection (pending, player characters, or NPCs).
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of active campaigns containing the character sheet
     */
    @Query("SELECT DISTINCT c FROM Campaign c " +
           "LEFT JOIN c.pendingCharacterSheets pcs " +
           "LEFT JOIN c.playerCharacters pc " +
           "LEFT JOIN c.nonPlayerCharacters npc " +
           "WHERE c.deletedAt IS NULL AND " +
           "(pcs.id = :characterSheetId OR pc.id = :characterSheetId OR npc.id = :characterSheetId)")
    List<Campaign> findActiveByCampaignCharacterSheetId(@Param("characterSheetId") Long characterSheetId);

    /**
     * Finds all active campaigns with optional filters.
     * <p>
     * Supports filtering by creator ID and name (case-insensitive partial match).
     * All filter parameters are optional - null values are ignored.
     * </p>
     *
     * @param creatorId the creator ID to filter by (optional)
     * @param name the name to search for with partial match (optional)
     * @param pageable pagination information
     * @return paginated list of campaigns matching the filters
     */
    @Query("SELECT c FROM Campaign c WHERE c.deletedAt IS NULL AND " +
           "(:creatorId IS NULL OR c.creator.id = :creatorId) AND " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Campaign> findActiveWithFilters(
            @Param("creatorId") Long creatorId,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Counts the number of active campaigns created by a specific user.
     *
     * @param creatorId the ID of the creator
     * @return the count of active campaigns
     */
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creator.id = :creatorId AND c.deletedAt IS NULL")
    Long countActiveByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * Finds campaigns by name (case-insensitive partial match).
     * <p>
     * Only returns active campaigns. Useful for searching campaigns by name.
     * </p>
     *
     * @param name the name to search for (partial match supported)
     * @return list of campaigns matching the name
     */
    @Query("SELECT c FROM Campaign c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.deletedAt IS NULL")
    List<Campaign> findByNameContainingIgnoreCaseAndDeletedAtIsNull(@Param("name") String name);

    /**
     * Finds all active campaigns where the user is involved in any role, with pagination.
     *
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return paginated list of active campaigns where the user is involved
     */
    @Query("SELECT DISTINCT c FROM Campaign c " +
           "LEFT JOIN c.gameMasters gm " +
           "LEFT JOIN c.players p " +
           "WHERE c.deletedAt IS NULL AND " +
           "(c.creator.id = :userId OR gm.id = :userId OR p.id = :userId)")
    Page<Campaign> findActiveByUserInvolvement(@Param("userId") Long userId, Pageable pageable);

    /**
     * Checks if a character sheet is in any active (not deleted, not ended) campaign.
     *
     * @param characterSheetId the character sheet ID
     * @return true if the character sheet is in an active campaign
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Campaign c " +
           "LEFT JOIN c.pendingCharacterSheets pcs " +
           "LEFT JOIN c.playerCharacters pc " +
           "LEFT JOIN c.nonPlayerCharacters npc " +
           "WHERE c.deletedAt IS NULL AND c.endedAt IS NULL AND " +
           "(pcs.id = :characterSheetId OR pc.id = :characterSheetId OR npc.id = :characterSheetId)")
    boolean isCharacterSheetInActiveCampaign(@Param("characterSheetId") Long characterSheetId);
}
