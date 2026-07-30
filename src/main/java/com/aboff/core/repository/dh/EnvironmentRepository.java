package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.enums.EnvironmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Environment entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * Finds a non-deleted environment by ID.
     *
     * @param id The environment ID
     * @return Optional containing the environment if found and not deleted
     */
    @Query("SELECT e FROM Environment e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Environment> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted environments accessible to a user with optional filters.
     * Returns environments that are official, public, or created by the specified user.
     *
     * @param userId The ID of the requesting user
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param environmentType Optional filter for environment type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param pageable Pagination information
     * @return Page of accessible non-deleted environments matching the criteria
     */
    @Query("SELECT e FROM Environment e WHERE e.deletedAt IS NULL " +
           "AND (e.isOfficial = true OR e.isPublic = true OR e.createdBy.id = :userId) " +
           "AND (:expansionId IS NULL OR e.expansion.id = :expansionId) " +
           "AND (:tier IS NULL OR e.tier = :tier) " +
           "AND (:environmentType IS NULL OR e.environmentType = :environmentType) " +
           "AND (:isOfficial IS NULL OR e.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Environment> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("expansionId") Long expansionId,
            @Param("tier") Integer tier,
            @Param("environmentType") EnvironmentType environmentType,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Finds all environments with optional filters, including soft-deleted ones.
     * For administrative use only.
     *
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param environmentType Optional filter for environment type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match, case-insensitive)
     * @param includeDeleted Whether to include soft-deleted environments
     * @param pageable Pagination information
     * @return Page of all environments matching the criteria
     */
    @Query("SELECT e FROM Environment e WHERE " +
           "(:includeDeleted = true OR e.deletedAt IS NULL) " +
           "AND (:expansionId IS NULL OR e.expansion.id = :expansionId) " +
           "AND (:tier IS NULL OR e.tier = :tier) " +
           "AND (:environmentType IS NULL OR e.environmentType = :environmentType) " +
           "AND (:isOfficial IS NULL OR e.isOfficial = :isOfficial) " +
           "AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))")
    Page<Environment> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("tier") Integer tier,
            @Param("environmentType") EnvironmentType environmentType,
            @Param("isOfficial") Boolean isOfficial,
            @Param("name") String name,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    /**
     * Finds all non-deleted environments by their IDs.
     *
     * @param ids List of environment IDs
     * @return List of non-deleted environments
     */
    @Query("SELECT e FROM Environment e WHERE e.id IN :ids AND e.deletedAt IS NULL")
    List<Environment> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
