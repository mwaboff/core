package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.enums.DomainCardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing DomainCard entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface DomainCardRepository extends JpaRepository<DomainCard, Long> {

    /**
     * Finds all non-deleted domain cards with optional filters.
     * Supports filtering by multiple associated domain IDs and/or multiple levels.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedDomainIds Optional list of associated domain IDs to filter by
     * @param type Optional filter for domain card type
     * @param levels Optional list of levels to filter by
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) cards; when
     *                      false, only custom (non-official) or SRD-flagged cards are returned
     * @param pageable Pagination information
     * @return Page of non-deleted domain cards matching the criteria
     */
    @Query("SELECT d FROM DomainCard d WHERE d.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR d.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR d.isOfficial = :isOfficial) " +
           "AND (:associatedDomainIds IS NULL OR d.associatedDomain.id IN :associatedDomainIds) " +
           "AND (:type IS NULL OR d.type = :type) " +
           "AND (:levels IS NULL OR d.level IN :levels) " +
           "AND (:includeNonSrd = true OR d.isOfficial = false OR d.srd = true)")
    Page<DomainCard> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("associatedDomainIds") List<Long> associatedDomainIds,
            @Param("type") DomainCardType type,
            @Param("levels") List<Integer> levels,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all domain cards with optional filters, including soft-deleted ones.
     * Supports filtering by multiple associated domain IDs and/or multiple levels.
     * <p>
     * ADMIN-only per the controller's {@code includeDeleted} contract; carries no SRD
     * visibility clause because {@code ContentAccessService#resolveIncludeDeleted} already
     * restricts this path to privileged callers.
     * </p>
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedDomainIds Optional list of associated domain IDs to filter by
     * @param type Optional filter for domain card type
     * @param levels Optional list of levels to filter by
     * @param pageable Pagination information
     * @return Page of all domain cards matching the criteria
     */
    @Query("SELECT d FROM DomainCard d WHERE " +
           "(:expansionId IS NULL OR d.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR d.isOfficial = :isOfficial) " +
           "AND (:associatedDomainIds IS NULL OR d.associatedDomain.id IN :associatedDomainIds) " +
           "AND (:type IS NULL OR d.type = :type) " +
           "AND (:levels IS NULL OR d.level IN :levels)")
    Page<DomainCard> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("associatedDomainIds") List<Long> associatedDomainIds,
            @Param("type") DomainCardType type,
            @Param("levels") List<Integer> levels,
            Pageable pageable);

    /**
     * Finds a non-deleted domain card by ID.
     *
     * @param id The card ID
     * @return Optional containing the card if found and not deleted
     */
    @Query("SELECT d FROM DomainCard d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<DomainCard> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted domain cards by their IDs.
     *
     * @param ids List of card IDs
     * @return List of non-deleted domain cards
     */
    @Query("SELECT d FROM DomainCard d WHERE d.id IN :ids AND d.deletedAt IS NULL")
    List<DomainCard> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
