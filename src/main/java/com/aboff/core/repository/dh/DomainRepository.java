package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Domain entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    /**
     * Finds all non-deleted domains (where deletedAt is null).
     *
     * @param pageable Pagination information
     * @return Page of non-deleted domains
     */
    Page<Domain> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Finds all non-deleted domains with optional expansion filter.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of non-deleted domains matching the criteria
     */
    @Query("SELECT d FROM Domain d WHERE d.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR d.expansion.id = :expansionId)")
    Page<Domain> findByDeletedAtIsNullAndExpansion(
            @Param("expansionId") Long expansionId,
            Pageable pageable);

    /**
     * Finds all domains with optional expansion filter, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of all domains matching the criteria
     */
    @Query("SELECT d FROM Domain d WHERE :expansionId IS NULL OR d.expansion.id = :expansionId")
    Page<Domain> findAllWithExpansion(
            @Param("expansionId") Long expansionId,
            Pageable pageable);

    /**
     * Finds a non-deleted domain by ID.
     *
     * @param id The domain ID
     * @return Optional containing the domain if found and not deleted
     */
    @Query("SELECT d FROM Domain d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Domain> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted domains by their IDs.
     * Used for batch operations like bulk creation.
     *
     * @param ids List of domain IDs
     * @return List of non-deleted domains
     */
    @Query("SELECT d FROM Domain d WHERE d.id IN :ids AND d.deletedAt IS NULL")
    List<Domain> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
