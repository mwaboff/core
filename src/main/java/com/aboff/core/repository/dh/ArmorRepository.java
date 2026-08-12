package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Armor;
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
 * Repository for managing Armor entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface ArmorRepository extends JpaRepository<Armor, Long> {

    /**
     * Finds non-deleted armors the given user is allowed to see, with optional filters.
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
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) official content;
     *                      see {@code ContentAccessService#includeNonSrd()}
     * @param pageable Pagination information
     * @return Page of visible armors matching the criteria
     */
    @Query(value = "SELECT DISTINCT a FROM Armor a LEFT JOIN a.campaigns ac " +
           "WHERE a.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR a.isOfficial = true " +
           "     OR a.isPublic = true " +
           "     OR a.createdBy IS NULL " +
           "     OR a.createdBy.id = :userId " +
           "     OR ac.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR a.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier) " +
           "AND (:includeNonSrd = true OR a.isOfficial = false OR a.srd = true) ",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Armor a LEFT JOIN a.campaigns ac " +
           "WHERE a.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR a.isOfficial = true " +
           "     OR a.isPublic = true " +
           "     OR a.createdBy IS NULL " +
           "     OR a.createdBy.id = :userId " +
           "     OR ac.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR a.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier) " +
           "AND (:includeNonSrd = true OR a.isOfficial = false OR a.srd = true) ")
    Page<Armor> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("memberCampaignIds") Collection<Long> memberCampaignIds,
            @Param("isPrivileged") boolean isPrivileged,
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all non-deleted armors with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) official content;
     *                      see {@code ContentAccessService#includeNonSrd()}
     * @param pageable Pagination information
     * @return Page of non-deleted armors matching the criteria
     */
    @Query("SELECT a FROM Armor a WHERE a.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier) " +
           "AND (:includeNonSrd = true OR a.isOfficial = false OR a.srd = true)")
    Page<Armor> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all armors with optional filters, including soft-deleted ones.
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
     * @param pageable Pagination information
     * @return Page of all armors matching the criteria
     */
    @Query("SELECT a FROM Armor a WHERE " +
           "(:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR a.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier)")
    Page<Armor> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            Pageable pageable);

    /**
     * Finds a non-deleted armor by ID.
     *
     * @param id The armor ID
     * @return Optional containing the armor if found and not deleted
     */
    @Query("SELECT a FROM Armor a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Armor> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted armors by their IDs.
     *
     * @param ids List of armor IDs
     * @return List of non-deleted armors
     */
    @Query("SELECT a FROM Armor a WHERE a.id IN :ids AND a.deletedAt IS NULL")
    List<Armor> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
