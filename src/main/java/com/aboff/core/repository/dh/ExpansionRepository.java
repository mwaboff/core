package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Expansion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Expansion entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface ExpansionRepository extends JpaRepository<Expansion, Long> {

    /**
     * Finds all non-deleted expansions (where deletedAt is null).
     *
     * @param pageable Pagination information
     * @return Page of non-deleted expansions
     */
    Page<Expansion> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Finds all non-deleted expansions with optional publishing filter.
     *
     * @param isPublished Optional filter for published status
     * @param pageable Pagination information
     * @return Page of non-deleted expansions matching the criteria
     */
    @Query("SELECT e FROM Expansion e WHERE e.deletedAt IS NULL " +
           "AND (:isPublished IS NULL OR e.isPublished = :isPublished)")
    Page<Expansion> findByDeletedAtIsNullAndPublished(
            @Param("isPublished") Boolean isPublished,
            Pageable pageable);

    /**
     * Finds all expansions with optional publishing filter, including soft-deleted ones.
     *
     * @param isPublished Optional filter for published status
     * @param pageable Pagination information
     * @return Page of all expansions matching the criteria
     */
    @Query("SELECT e FROM Expansion e WHERE :isPublished IS NULL OR e.isPublished = :isPublished")
    Page<Expansion> findAllWithPublished(
            @Param("isPublished") Boolean isPublished,
            Pageable pageable);

    /**
     * Finds a non-deleted expansion by ID.
     *
     * @param id The expansion ID
     * @return Optional containing the expansion if found and not deleted
     */
    @Query("SELECT e FROM Expansion e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Expansion> findByIdAndDeletedAtIsNull(@Param("id") Long id);
}
