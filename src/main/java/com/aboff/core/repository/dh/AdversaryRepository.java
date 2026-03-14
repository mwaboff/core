package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.enums.AdversaryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Adversary entities.
 * Provides data access methods with support for soft deletion, filtering, and access control.
 */
@Repository
public interface AdversaryRepository extends JpaRepository<Adversary, Long> {

    /**
     * Finds a non-deleted adversary by ID.
     *
     * @param id The adversary ID
     * @return Optional containing the adversary if found and not deleted
     */
    @Query("SELECT a FROM Adversary a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Adversary> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted adversaries accessible to a user with optional filters.
     * Returns adversaries that are official, public, or created by the specified user.
     *
     * @param userId The ID of the requesting user
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for adversary tier (1-4)
     * @param adversaryType Optional filter for adversary type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param pageable Pagination information
     * @return Page of accessible non-deleted adversaries matching the criteria
     */
    @Query("SELECT a FROM Adversary a WHERE a.deletedAt IS NULL " +
           "AND (a.isOfficial = true OR a.isPublic = true OR a.createdBy.id = :userId) " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:tier IS NULL OR a.tier = :tier) " +
           "AND (:adversaryType IS NULL OR a.adversaryType = :adversaryType) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Adversary> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("expansionId") Long expansionId,
            @Param("tier") Integer tier,
            @Param("adversaryType") AdversaryType adversaryType,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Finds all adversaries with optional filters, including soft-deleted ones.
     * For administrative use only.
     *
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for adversary tier (1-4)
     * @param adversaryType Optional filter for adversary type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param includeDeleted Whether to include soft-deleted adversaries
     * @param pageable Pagination information
     * @return Page of all adversaries matching the criteria
     */
    @Query("SELECT a FROM Adversary a WHERE " +
           "(:includeDeleted = true OR a.deletedAt IS NULL) " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:tier IS NULL OR a.tier = :tier) " +
           "AND (:adversaryType IS NULL OR a.adversaryType = :adversaryType) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Adversary> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("tier") Integer tier,
            @Param("adversaryType") AdversaryType adversaryType,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    /**
     * Finds all non-deleted adversaries created by a specific user.
     *
     * @param creatorId The ID of the creator
     * @param pageable Pagination information
     * @return Page of adversaries created by the user
     */
    @Query("SELECT a FROM Adversary a WHERE a.createdBy.id = :creatorId AND a.deletedAt IS NULL")
    Page<Adversary> findByCreatedByIdAndDeletedAtIsNull(
            @Param("creatorId") Long creatorId,
            Pageable pageable);

    /**
     * Finds all non-deleted copies of a specific adversary.
     *
     * @param originalId The ID of the original adversary
     * @return List of adversaries that are copies of the original
     */
    @Query("SELECT a FROM Adversary a WHERE a.originalAdversary.id = :originalId AND a.deletedAt IS NULL")
    List<Adversary> findByOriginalAdversaryIdAndDeletedAtIsNull(@Param("originalId") Long originalId);

    /**
     * Finds all non-deleted adversaries by their IDs.
     *
     * @param ids List of adversary IDs
     * @return List of non-deleted adversaries
     */
    @Query("SELECT a FROM Adversary a WHERE a.id IN :ids AND a.deletedAt IS NULL")
    List<Adversary> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
