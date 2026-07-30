package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.TransformationCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing TransformationCard entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface TransformationCardRepository extends JpaRepository<TransformationCard, Long> {

    /**
     * Finds all non-deleted transformation cards with optional expansion filter.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of non-deleted transformation cards matching the criteria
     */
    @Query("SELECT t FROM TransformationCard t WHERE t.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR t.expansion.id = :expansionId)")
    Page<TransformationCard> findByDeletedAtIsNullAndExpansion(
            @Param("expansionId") Long expansionId,
            Pageable pageable);

    /**
     * Finds all transformation cards with optional expansion filter, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of all transformation cards matching the criteria
     */
    @Query("SELECT t FROM TransformationCard t WHERE :expansionId IS NULL OR t.expansion.id = :expansionId")
    Page<TransformationCard> findAllWithExpansion(
            @Param("expansionId") Long expansionId,
            Pageable pageable);

    /**
     * Finds a non-deleted transformation card by ID.
     *
     * @param id The transformation card ID
     * @return Optional containing the transformation card if found and not deleted
     */
    @Query("SELECT t FROM TransformationCard t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TransformationCard> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted transformation cards by their IDs.
     * Used for batch operations like bulk creation.
     *
     * @param ids List of transformation card IDs
     * @return List of non-deleted transformation cards
     */
    @Query("SELECT t FROM TransformationCard t WHERE t.id IN :ids AND t.deletedAt IS NULL")
    List<TransformationCard> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
