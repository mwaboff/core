package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.SubclassPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing SubclassPath entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface SubclassPathRepository extends JpaRepository<SubclassPath, Long> {

    /**
     * Finds a non-deleted subclass path by ID.
     *
     * @param id the subclass path ID
     * @return Optional containing the subclass path if found and not deleted
     */
    @Query("SELECT sp FROM SubclassPath sp WHERE sp.id = :id AND sp.deletedAt IS NULL")
    Optional<SubclassPath> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds a non-deleted subclass path by name (case-insensitive) and associated class ID.
     * Used for the find-or-create pattern when creating subclass cards.
     *
     * @param name the path name to search for (case-insensitive)
     * @param classId the associated class ID
     * @return Optional containing the matching subclass path if found and not deleted
     */
    Optional<SubclassPath> findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull(
            String name, Long classId);

    /**
     * Finds all non-deleted subclass paths with optional class filter.
     *
     * @param classId optional filter for associated class ID (null to include all)
     * @param pageable pagination information
     * @return Page of non-deleted subclass paths matching the criteria
     */
    @Query("SELECT sp FROM SubclassPath sp WHERE sp.deletedAt IS NULL " +
           "AND (:classId IS NULL OR sp.associatedClass.id = :classId)")
    Page<SubclassPath> findByDeletedAtIsNullAndFilters(
            @Param("classId") Long classId,
            Pageable pageable);

    /**
     * Finds all subclass paths with optional class filter, including soft-deleted ones.
     *
     * @param classId optional filter for associated class ID (null to include all)
     * @param pageable pagination information
     * @return Page of all subclass paths matching the criteria
     */
    @Query("SELECT sp FROM SubclassPath sp WHERE :classId IS NULL OR sp.associatedClass.id = :classId")
    Page<SubclassPath> findAllWithFilters(
            @Param("classId") Long classId,
            Pageable pageable);

    /**
     * Finds all non-deleted subclass paths by their IDs.
     * Used for batch operations.
     *
     * @param ids list of subclass path IDs
     * @return List of non-deleted subclass paths
     */
    @Query("SELECT sp FROM SubclassPath sp WHERE sp.id IN :ids AND sp.deletedAt IS NULL")
    List<SubclassPath> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
