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
     * @param includeNonSrd Whether the caller may see paid-expansion (non-SRD) features; when
     *                      false, only custom (non-official) or SRD-flagged features are returned
     * @param pageable Pagination information
     * @return Page of non-deleted features matching the criteria
     */
    @Query("SELECT f FROM Feature f WHERE f.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR f.expansion.id = :expansionId) " +
           "AND (:featureType IS NULL OR f.featureType = :featureType) " +
           "AND (:includeNonSrd = true OR f.isOfficial = false OR f.srd = true)")
    Page<Feature> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("featureType") FeatureType featureType,
            @Param("includeNonSrd") boolean includeNonSrd,
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

    /**
     * Finds a non-deleted feature by name (case-insensitive), expansion, feature type, and
     * description (case-sensitive, exact). This is the find-or-create key used to avoid
     * conflating rules text that happens to share a name — e.g. the core rulebook prints four
     * distinct per-tier texts each for the weapon features "Barrier", "Paired", and "Protective".
     * <p>
     * Both the description and the expansion comparisons are null-safe: a {@code null} incoming
     * value only matches an existing row whose value is also {@code null}. It never matches (or
     * is matched by) a non-null value, and vice versa.
     * <p>
     * The expansion needs that treatment because homebrew features carry no sourcebook. A plain
     * {@code f.expansion.id = :expansionId} evaluates to UNKNOWN when the parameter is null, so
     * every lookup would miss and each save would mint a duplicate row, orphaning the previous
     * one. Two homebrew features are the same feature when their name, type, and description
     * match and neither has an expansion.
     *
     * @param name The feature name to match case-insensitively
     * @param expansionId The expansion ID to match; null matches only expansion-less features
     * @param featureType The feature type to match
     * @param description The description to match exactly (case-sensitive); may be null
     * @return Optional containing the matching feature if found and not deleted
     */
    @Query("SELECT f FROM Feature f WHERE LOWER(f.name) = LOWER(:name) " +
           "AND ((:expansionId IS NULL AND f.expansion IS NULL) " +
           "     OR (:expansionId IS NOT NULL AND f.expansion.id = :expansionId)) " +
           "AND f.featureType = :featureType " +
           "AND ((:description IS NULL AND f.description IS NULL) " +
           "     OR (:description IS NOT NULL AND f.description = :description)) " +
           "AND f.deletedAt IS NULL")
    Optional<Feature> findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
            @Param("name") String name,
            @Param("expansionId") Long expansionId,
            @Param("featureType") FeatureType featureType,
            @Param("description") String description);
}
