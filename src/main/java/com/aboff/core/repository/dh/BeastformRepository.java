package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Beastform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Beastform entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface BeastformRepository extends JpaRepository<Beastform, Long> {

    /**
     * Finds a non-deleted beastform by ID.
     *
     * @param id The beastform ID
     * @return Optional containing the beastform if found and not deleted
     */
    @Query("SELECT b FROM Beastform b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Beastform> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted beastforms with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isPublic Optional filter for public visibility
     * @param pageable Pagination information
     * @return Page of non-deleted beastforms matching the criteria
     */
    @Query("SELECT b FROM Beastform b WHERE b.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR b.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR b.isOfficial = :isOfficial) " +
           "AND (:isPublic IS NULL OR b.isPublic = :isPublic)")
    Page<Beastform> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable);

    /**
     * Finds all beastforms with optional filters, including soft-deleted ones.
     * For administrative use only.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isPublic Optional filter for public visibility
     * @param pageable Pagination information
     * @return Page of all beastforms matching the criteria
     */
    @Query("SELECT b FROM Beastform b WHERE " +
           "(:expansionId IS NULL OR b.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR b.isOfficial = :isOfficial) " +
           "AND (:isPublic IS NULL OR b.isPublic = :isPublic)")
    Page<Beastform> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable);

    /**
     * Finds all non-deleted beastforms by their IDs.
     *
     * @param ids List of beastform IDs
     * @return List of non-deleted beastforms
     */
    @Query("SELECT b FROM Beastform b WHERE b.id IN :ids AND b.deletedAt IS NULL")
    List<Beastform> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
