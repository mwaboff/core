package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Condition entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    /**
     * Finds a non-deleted condition by ID.
     *
     * @param id The condition ID
     * @return Optional containing the condition if found and not deleted
     */
    @Query("SELECT c FROM Condition c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Condition> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted conditions with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) content; official
     *                      rows that are not SRD-licensed are excluded when false. See
     *                      {@code ContentAccessService#includeNonSrd()}.
     * @param pageable Pagination information
     * @return Page of non-deleted conditions matching the criteria
     */
    @Query("SELECT c FROM Condition c WHERE c.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial) " +
           "AND (:includeNonSrd = true OR c.isOfficial = false OR c.srd = true)")
    Page<Condition> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all conditions with optional filters, including soft-deleted ones.
     * For administrative use only.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of all conditions matching the criteria
     */
    @Query("SELECT c FROM Condition c WHERE " +
           "(:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial)")
    Page<Condition> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            Pageable pageable);

    /**
     * Finds all non-deleted conditions by their IDs.
     *
     * @param ids List of condition IDs
     * @return List of non-deleted conditions
     */
    @Query("SELECT c FROM Condition c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    List<Condition> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
