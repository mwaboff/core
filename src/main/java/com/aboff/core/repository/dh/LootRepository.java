package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Loot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Loot entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface LootRepository extends JpaRepository<Loot, Long> {

    /**
     * Finds all non-deleted loot with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of non-deleted loot matching the criteria
     */
    @Query("SELECT l FROM Loot l WHERE l.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable)")
    Page<Loot> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("tier") Integer tier,
            @Param("isConsumable") Boolean isConsumable,
            Pageable pageable);

    /**
     * Finds all loot with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for tier
     * @param isConsumable Optional filter for consumable status
     * @param pageable Pagination information
     * @return Page of all loot matching the criteria
     */
    @Query("SELECT l FROM Loot l WHERE " +
           "(:expansionId IS NULL OR l.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR l.isOfficial = :isOfficial) " +
           "AND (:tier IS NULL OR l.tier = :tier) " +
           "AND (:isConsumable IS NULL OR l.isConsumable = :isConsumable)")
    Page<Loot> findAllWithFilters(
            @Param("expansionId") Long expansionId,
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

    /**
     * Counts non-deleted loot items created by the given user.
     * Used to enforce the per-user custom item creation cap.
     *
     * @param userId The creator's user ID
     * @return Count of non-deleted loot items created by the user
     */
    long countByCreatedByIdAndDeletedAtIsNull(Long userId);
}
