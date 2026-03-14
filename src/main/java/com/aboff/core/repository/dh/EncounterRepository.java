package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Encounter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Encounter entities.
 * Provides data access methods with support for soft deletion, filtering, and access control.
 */
@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    /**
     * Finds a non-deleted encounter by ID.
     *
     * @param id The encounter ID
     * @return Optional containing the encounter if found and not deleted
     */
    @Query("SELECT e FROM Encounter e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Encounter> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted encounters accessible to a user with optional filters.
     * Returns encounters that are official, public, or created by the specified user.
     *
     * @param userId The ID of the requesting user
     * @param campaignId Optional filter for campaign ID
     * @param tier Optional filter for encounter tier (1-4)
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param pageable Pagination information
     * @return Page of accessible non-deleted encounters matching the criteria
     */
    @Query("SELECT e FROM Encounter e WHERE e.deletedAt IS NULL " +
           "AND (e.isOfficial = true OR e.isPublic = true OR e.createdBy.id = :userId) " +
           "AND (:campaignId IS NULL OR e.campaign.id = :campaignId) " +
           "AND (:tier IS NULL OR e.tier = :tier) " +
           "AND (:isOfficial IS NULL OR e.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Encounter> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("campaignId") Long campaignId,
            @Param("tier") Integer tier,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Finds all encounters with optional filters, including soft-deleted ones.
     * For administrative use only.
     *
     * @param campaignId Optional filter for campaign ID
     * @param tier Optional filter for encounter tier (1-4)
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param includeDeleted Whether to include soft-deleted encounters
     * @param pageable Pagination information
     * @return Page of all encounters matching the criteria
     */
    @Query("SELECT e FROM Encounter e WHERE " +
           "(:includeDeleted = true OR e.deletedAt IS NULL) " +
           "AND (:campaignId IS NULL OR e.campaign.id = :campaignId) " +
           "AND (:tier IS NULL OR e.tier = :tier) " +
           "AND (:isOfficial IS NULL OR e.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Encounter> findAllWithFilters(
            @Param("campaignId") Long campaignId,
            @Param("tier") Integer tier,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    /**
     * Finds all non-deleted encounters for a specific campaign.
     *
     * @param campaignId The ID of the campaign
     * @return List of encounters for the campaign
     */
    @Query("SELECT e FROM Encounter e WHERE e.campaign.id = :campaignId AND e.deletedAt IS NULL")
    List<Encounter> findByCampaignIdAndDeletedAtIsNull(@Param("campaignId") Long campaignId);

    /**
     * Finds all non-deleted copies of a specific encounter.
     *
     * @param originalId The ID of the original encounter
     * @return List of encounters that are copies of the original
     */
    @Query("SELECT e FROM Encounter e WHERE e.originalEncounter.id = :originalId AND e.deletedAt IS NULL")
    List<Encounter> findByOriginalEncounterIdAndDeletedAtIsNull(@Param("originalId") Long originalId);
}
