package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Armor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Armor entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface ArmorRepository extends JpaRepository<Armor, Long> {

    /**
     * Finds all non-deleted armors with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of non-deleted armors matching the criteria
     */
    @Query("SELECT a FROM Armor a WHERE a.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier)")
    Page<Armor> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            Pageable pageable);

    /**
     * Finds all armors with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of all armors matching the criteria
     */
    @Query("SELECT a FROM Armor a WHERE " +
           "(:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR a.tier = :tier)")
    Page<Armor> findAllWithFilters(
            @Param("expansionId") Long expansionId,
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

    /**
     * Counts non-deleted armors created by the given user.
     * Used to enforce the per-user custom item creation cap.
     *
     * @param userId The creator's user ID
     * @return Count of non-deleted armors created by the user
     */
    long countByCreatedByIdAndDeletedAtIsNull(Long userId);
}
