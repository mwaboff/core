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
     * Finds all non-deleted classes with optional expansion filter.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of non-deleted classes matching the criteria
     */
    @Query("SELECT c FROM Class c WHERE c.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR c.expansion.id = :expansionId)")
    Page<Class> findByDeletedAtIsNullAndExpansion(
            @Param("expansionId") Long expansionId,
            Pageable pageable);

    /**
     * Finds all classes with optional expansion filter, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param pageable Pagination information
     * @return Page of all classes matching the criteria
     */
    @Query("SELECT c FROM Class c WHERE :expansionId IS NULL OR c.expansion.id = :expansionId")
    Page<Class> findAllWithExpansion(
            @Param("expansionId") Long expansionId,
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
