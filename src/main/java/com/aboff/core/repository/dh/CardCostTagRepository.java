package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing CardCostTag entities.
 * Provides data access methods with support for soft deletion and filtering by category.
 */
@Repository
public interface CardCostTagRepository extends JpaRepository<CardCostTag, Long> {

    /**
     * Finds all non-deleted cost tags with optional category filter.
     *
     * @param category Optional filter for cost tag category
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) tags; when
     *                      false, only SRD-flagged tags are returned
     * @param pageable Pagination information
     * @return Page of non-deleted cost tags matching the criteria
     */
    @Query("SELECT t FROM CardCostTag t WHERE t.deletedAt IS NULL " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:includeNonSrd = true OR t.srd = true)")
    Page<CardCostTag> findByDeletedAtIsNullAndFilters(
            @Param("category") CostTagCategory category,
            @Param("includeNonSrd") boolean includeNonSrd,
            Pageable pageable);

    /**
     * Finds all cost tags with optional category filter, including soft-deleted ones.
     *
     * @param category Optional filter for cost tag category
     * @param pageable Pagination information
     * @return Page of all cost tags matching the criteria
     */
    @Query("SELECT t FROM CardCostTag t WHERE " +
           "(:category IS NULL OR t.category = :category)")
    Page<CardCostTag> findAllWithFilters(
            @Param("category") CostTagCategory category,
            Pageable pageable);

    /**
     * Finds a non-deleted cost tag by ID.
     *
     * @param id The cost tag ID
     * @return Optional containing the cost tag if found and not deleted
     */
    @Query("SELECT t FROM CardCostTag t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<CardCostTag> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted cost tags by their IDs.
     *
     * @param ids List of cost tag IDs
     * @return List of non-deleted cost tags
     */
    @Query("SELECT t FROM CardCostTag t WHERE t.id IN :ids AND t.deletedAt IS NULL")
    List<CardCostTag> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);

    /**
     * Finds a non-deleted cost tag by label, ignoring case.
     *
     * @param label The cost tag label to search for
     * @return Optional containing the cost tag if found and not deleted
     */
    @Query("SELECT t FROM CardCostTag t WHERE LOWER(t.label) = LOWER(:label) AND t.deletedAt IS NULL")
    Optional<CardCostTag> findByLabelIgnoreCaseAndDeletedAtIsNull(@Param("label") String label);
}
