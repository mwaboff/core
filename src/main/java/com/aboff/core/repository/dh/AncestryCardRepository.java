package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.AncestryCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing AncestryCard entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface AncestryCardRepository extends JpaRepository<AncestryCard, Long> {

    /**
     * Finds all non-deleted ancestry cards with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) cards; when
     *                      false, only custom (non-official) or SRD-flagged cards are returned
     * @param pageable Pagination information
     * @return Page of non-deleted ancestry cards matching the criteria
     */
    @Query("SELECT a FROM AncestryCard a WHERE a.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:isMixed IS NULL OR a.isMixed = :isMixed) " +
           "AND (:includeNonSrd = true OR a.isOfficial = false OR a.srd = true)")
    Page<AncestryCard> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("isMixed") Boolean isMixed,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all ancestry cards with optional filters, including soft-deleted ones.
     * <p>
     * ADMIN-only per the controller's {@code includeDeleted} contract; carries no SRD
     * visibility clause because {@code ContentAccessService#resolveIncludeDeleted} already
     * restricts this path to privileged callers.
     * </p>
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of all ancestry cards matching the criteria
     */
    @Query("SELECT a FROM AncestryCard a WHERE " +
           "(:expansionId IS NULL OR a.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR a.isOfficial = :isOfficial) " +
           "AND (:isMixed IS NULL OR a.isMixed = :isMixed)")
    Page<AncestryCard> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("isMixed") Boolean isMixed,
            Pageable pageable);

    /**
     * Finds a non-deleted ancestry card by ID.
     *
     * @param id The card ID
     * @return Optional containing the card if found and not deleted
     */
    @Query("SELECT a FROM AncestryCard a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<AncestryCard> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted ancestry cards by their IDs.
     *
     * @param ids List of card IDs
     * @return List of non-deleted ancestry cards
     */
    @Query("SELECT a FROM AncestryCard a WHERE a.id IN :ids AND a.deletedAt IS NULL")
    List<AncestryCard> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
