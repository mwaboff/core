package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.FeatureType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Feature entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    /**
     * Finds all non-deleted features with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param featureType Optional filter for feature type
     * @param pageable Pagination information
     * @return Page of non-deleted features matching the criteria
     */
    @Query("SELECT f FROM Feature f WHERE f.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR f.expansion.id = :expansionId) " +
           "AND (:featureType IS NULL OR f.featureType = :featureType)")
    Page<Feature> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("featureType") FeatureType featureType,
            Pageable pageable);

    /**
     * Finds all features with optional filters, including soft-deleted ones.
     *
     * @param expansionId Optional filter for expansion ID
     * @param featureType Optional filter for feature type
     * @param pageable Pagination information
     * @return Page of all features matching the criteria
     */
    @Query("SELECT f FROM Feature f WHERE " +
           "(:expansionId IS NULL OR f.expansion.id = :expansionId) " +
           "AND (:featureType IS NULL OR f.featureType = :featureType)")
    Page<Feature> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("featureType") FeatureType featureType,
            Pageable pageable);

    /**
     * Finds a non-deleted feature by ID.
     *
     * @param id The feature ID
     * @return Optional containing the feature if found and not deleted
     */
    @Query("SELECT f FROM Feature f WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Feature> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted features by their IDs.
     *
     * @param ids List of feature IDs
     * @return List of non-deleted features
     */
    @Query("SELECT f FROM Feature f WHERE f.id IN :ids AND f.deletedAt IS NULL")
    List<Feature> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
