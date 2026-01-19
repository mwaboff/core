package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CommunityCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing CommunityCard entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface CommunityCardRepository extends JpaRepository<CommunityCard, Long> {

    /**
     * Finds all non-deleted community cards with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of non-deleted community cards matching the criteria
     */
    @Query("SELECT c FROM CommunityCard c WHERE c.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial)")
    Page<CommunityCard> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            Pageable pageable);

    /**
     * Finds all community cards with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of all community cards matching the criteria
     */
    @Query("SELECT c FROM CommunityCard c WHERE " +
           "(:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial)")
    Page<CommunityCard> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            Pageable pageable);

    /**
     * Finds a non-deleted community card by ID.
     *
     * @param id The card ID
     * @return Optional containing the card if found and not deleted
     */
    @Query("SELECT c FROM CommunityCard c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CommunityCard> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted community cards by their IDs.
     *
     * @param ids List of card IDs
     * @return List of non-deleted community cards
     */
    @Query("SELECT c FROM CommunityCard c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    List<CommunityCard> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
