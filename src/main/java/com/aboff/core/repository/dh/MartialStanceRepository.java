package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.MartialStance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing MartialStance entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface MartialStanceRepository extends JpaRepository<MartialStance, Long> {

    /**
     * Finds all non-deleted martial stances with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for stance tier (1–4)
     * @param pageable Pagination information
     * @return Page of non-deleted martial stances matching the criteria
     */
    @Query("SELECT m FROM MartialStance m WHERE m.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR m.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR m.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR m.tier = :tier)")
    Page<MartialStance> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            Pageable pageable);

    /**
     * Finds all martial stances with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param pageable Pagination information
     * @return Page of all martial stances matching the criteria
     */
    @Query("SELECT m FROM MartialStance m WHERE " +
           "(:expansionId IS NULL OR m.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR m.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR m.tier = :tier)")
    Page<MartialStance> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            Pageable pageable);

    /**
     * Finds a non-deleted martial stance by ID.
     *
     * @param id The martial stance ID
     * @return Optional containing the martial stance if found and not deleted
     */
    @Query("SELECT m FROM MartialStance m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<MartialStance> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted martial stances by their IDs.
     *
     * @param ids List of martial stance IDs
     * @return List of non-deleted martial stances
     */
    @Query("SELECT m FROM MartialStance m WHERE m.id IN :ids AND m.deletedAt IS NULL")
    List<MartialStance> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
