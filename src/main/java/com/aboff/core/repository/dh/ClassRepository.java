package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Class entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {

    /**
     * Finds all non-deleted classes with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) classes; when
     *                      false, only custom (non-official) or SRD-flagged classes are returned
     * @param pageable Pagination information
     * @return Page of non-deleted classes matching the criteria
     */
    @Query("SELECT c FROM Class c WHERE c.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial) " +
           "AND (:includeNonSrd = true OR c.isOfficial = false OR c.srd = true)")
    Page<Class> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all classes with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param pageable Pagination information
     * @return Page of all classes matching the criteria
     */
    @Query("SELECT c FROM Class c WHERE (:expansionId IS NULL OR c.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR c.isOfficial = :isOfficial)")
    Page<Class> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            Pageable pageable);

    /**
     * Finds a non-deleted class by ID.
     *
     * @param id The class ID
     * @return Optional containing the class if found and not deleted
     */
    @Query("SELECT c FROM Class c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Class> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted classes by their IDs.
     * Used for batch operations like bulk creation.
     *
     * @param ids List of class IDs
     * @return List of non-deleted classes
     */
    @Query("SELECT c FROM Class c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    List<Class> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
