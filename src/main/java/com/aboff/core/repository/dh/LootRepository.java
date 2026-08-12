package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Loot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Loot entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface LootRepository extends JpaRepository<Loot, Long> {

    /**
     * Finds non-deleted loot the given user is allowed to see, with optional filters.
     * <p>
     * A record is visible when any of the following holds: the caller is a moderator or above;
     * it is official; it is public; it has no author (every imported row, plus any official row
     * later demoted to custom — these are treated as system content); the caller authored it;
     * or it is explicitly tagged to a campaign the caller is involved in.
     * </p>
     * <p>
     * {@code memberCampaignIds} <em>grants</em> access rather than narrowing it, unlike the
     * other parameters which are filters. It must never be empty — PostgreSQL rejects an empty
     * {@code IN ()} list — so callers pass a sentinel that matches nothing. See
     * {@code ItemAccessService.VisibilityScope}.
     * </p>
     * <p>
     * The {@code LEFT JOIN} onto campaign tags multiplies rows for a record shared with several
     * campaigns, so the query is {@code DISTINCT} and supplies an explicit {@code countQuery}.
     * Without that count override the derived one would report inflated totals and paginate
     * incorrectly.
     * </p>
     *
     * @param userId The calling user's ID, matched against a record's author
     * @param memberCampaignIds Campaign IDs the caller is involved in; never empty
     * @param isPrivileged True for MODERATOR+, who bypass visibility filtering
     * @param expansionId Optional filter for expansion ID
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param isConsumable Optional filter for consumable status
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) official content;
     *                      see {@code ContentAccessService#includeNonSrd()}
     * @param pageable Pagination information
     * @return Page of visible loot matching the criteria
     */
    @Query(value = "SELECT DISTINCT l FROM Loot l LEFT JOIN l.campaigns lc " +
           "WHERE l.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR l.isOfficial = true " +
           "     OR l.isPublic = true " +
           "     OR l.createdBy IS NULL " +
           "     OR l.createdBy.id = :userId " +
           "     OR lc.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR l.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable) " +
           "AND (:includeNonSrd = true OR l.isOfficial = false OR l.srd = true) ",
           countQuery = "SELECT COUNT(DISTINCT l) FROM Loot l LEFT JOIN l.campaigns lc " +
           "WHERE l.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR l.isOfficial = true " +
           "     OR l.isPublic = true " +
           "     OR l.createdBy IS NULL " +
           "     OR l.createdBy.id = :userId " +
           "     OR lc.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR l.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable) " +
           "AND (:includeNonSrd = true OR l.isOfficial = false OR l.srd = true) ")
    Page<Loot> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("memberCampaignIds") Collection<Long> memberCampaignIds,
            @Param("isPrivileged") boolean isPrivileged,
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("isConsumable") Boolean isConsumable,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all non-deleted loot with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param isConsumable Optional filter for consumable status
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) official content;
     *                      see {@code ContentAccessService#includeNonSrd()}
     * @param pageable Pagination information
     * @return Page of non-deleted loot matching the criteria
     */
    @Query("SELECT l FROM Loot l WHERE l.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable) " +
           "AND (:includeNonSrd = true OR l.isOfficial = false OR l.srd = true)")
    Page<Loot> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("isConsumable") Boolean isConsumable,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all loot with optional filters, including soft-deleted ones.
     * <p>
     * Supports the same filter set as {@link #findAccessibleWithFilters} so that switching a
     * request to {@code includeDeleted=true} narrows the result set rather than silently
     * widening it: this query previously took no {@code name} or {@code createdByUserId} and
     * returned the whole catalogue with a 200 when either was supplied.
     * </p>
     *
     * @param expansionId Optional filter for expansion ID
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param isConsumable Optional filter for consumable status
     * @param pageable Pagination information
     * @return Page of all loot matching the criteria
     */
    @Query("SELECT l FROM Loot l WHERE " +
           "(:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR l.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable)")
    Page<Loot> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("isConsumable") Boolean isConsumable,
            Pageable pageable);

    /**
     * Finds a non-deleted loot by ID.
     *
     * @param id The loot ID
     * @return Optional containing the loot if found and not deleted
     */
    @Query("SELECT l FROM Loot l WHERE l.id = :id AND l.deletedAt IS NULL")
    Optional<Loot> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted loot by their IDs.
     *
     * @param ids List of loot IDs
     * @return List of non-deleted loot
     */
    @Query("SELECT l FROM Loot l WHERE l.id IN :ids AND l.deletedAt IS NULL")
    List<Loot> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
