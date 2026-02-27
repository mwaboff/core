package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.enums.SubclassLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing SubclassCard entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface SubclassCardRepository extends JpaRepository<SubclassCard, Long> {

    /**
     * Finds all non-deleted subclass cards with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID (via subclass path)
     * @param subclassPathId Optional filter for subclass path ID
     * @param level Optional filter for subclass level
     * @param pageable Pagination information
     * @return Page of non-deleted subclass cards matching the criteria
     */
    @Query("SELECT s FROM SubclassCard s WHERE s.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR s.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR s.isOfficial = :isOfficial) " +
           "AND (:associatedClassId IS NULL OR s.subclassPath.associatedClass.id = :associatedClassId) " +
           "AND (:subclassPathId IS NULL OR s.subclassPath.id = :subclassPathId) " +
           "AND (:level IS NULL OR s.level = :level)")
    Page<SubclassCard> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("associatedClassId") Long associatedClassId,
            @Param("subclassPathId") Long subclassPathId,
            @Param("level") SubclassLevel level,
            Pageable pageable);

    /**
     * Finds all subclass cards with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID (via subclass path)
     * @param subclassPathId Optional filter for subclass path ID
     * @param level Optional filter for subclass level
     * @param pageable Pagination information
     * @return Page of all subclass cards matching the criteria
     */
    @Query("SELECT s FROM SubclassCard s WHERE " +
           "(:expansionId IS NULL OR s.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR s.isOfficial = :isOfficial) " +
           "AND (:associatedClassId IS NULL OR s.subclassPath.associatedClass.id = :associatedClassId) " +
           "AND (:subclassPathId IS NULL OR s.subclassPath.id = :subclassPathId) " +
           "AND (:level IS NULL OR s.level = :level)")
    Page<SubclassCard> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("associatedClassId") Long associatedClassId,
            @Param("subclassPathId") Long subclassPathId,
            @Param("level") SubclassLevel level,
            Pageable pageable);

    /**
     * Finds a non-deleted subclass card by ID.
     *
     * @param id The card ID
     * @return Optional containing the card if found and not deleted
     */
    @Query("SELECT s FROM SubclassCard s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<SubclassCard> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted subclass cards by their IDs.
     *
     * @param ids List of card IDs
     * @return List of non-deleted subclass cards
     */
    @Query("SELECT s FROM SubclassCard s WHERE s.id IN :ids AND s.deletedAt IS NULL")
    List<SubclassCard> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
