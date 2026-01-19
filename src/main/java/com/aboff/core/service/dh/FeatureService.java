package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing Feature entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {

    private final FeatureRepository featureRepository;
    private final ExpansionRepository expansionRepository;

    /**
     * Retrieves a paginated list of features.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted features
     * @param expansionId Optional filter for expansion ID
     * @param featureType Optional filter for feature type
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing features
     */
    @Transactional(readOnly = true)
    public PagedResponse<FeatureResponse> getAllFeatures(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            FeatureType featureType,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Feature> featurePage;

        if (includeDeleted) {
            featurePage = featureRepository.findAllWithFilters(expansionId, featureType, pageable);
        } else {
            featurePage = featureRepository.findByDeletedAtIsNullAndFilters(expansionId, featureType, pageable);
        }

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<FeatureResponse>builder()
                .content(featurePage.getContent().stream()
                        .map(feature -> toResponse(feature, expandSet))
                        .toList())
                .totalElements(featurePage.getTotalElements())
                .totalPages(featurePage.getTotalPages())
                .currentPage(featurePage.getNumber())
                .pageSize(featurePage.getSize())
                .build();
    }

    /**
     * Retrieves a single feature by ID.
     *
     * @param id The feature ID
     * @param expand Comma-separated list of relationships to expand
     * @return FeatureResponse containing the feature details
     */
    @Transactional(readOnly = true)
    public FeatureResponse getFeatureById(Long id, String expand) {
        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        Set<String> expandSet = parseExpand(expand);
        return toResponse(feature, expandSet);
    }

    /**
     * Creates a new feature.
     *
     * @param request The creation request containing feature details
     * @return FeatureResponse containing the created feature
     */
    @Transactional
    public FeatureResponse createFeature(CreateFeatureRequest request) {
        log.info("Creating new feature with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Feature feature = Feature.builder()
                .name(request.getName())
                .description(request.getDescription())
                .featureType(request.getFeatureType())
                .expansion(expansion)
                .build();

        Feature savedFeature = featureRepository.save(feature);
        log.info("Created feature with id: {}", savedFeature.getId());

        return toResponse(savedFeature, Set.of());
    }

    /**
     * Updates an existing feature.
     *
     * @param id The feature ID to update
     * @param request The update request containing new feature details
     * @return FeatureResponse containing the updated feature
     */
    @Transactional
    public FeatureResponse updateFeature(Long id, UpdateFeatureRequest request) {
        log.info("Updating feature with id: {}", id);

        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        feature.setName(request.getName());
        feature.setDescription(request.getDescription());
        feature.setFeatureType(request.getFeatureType());
        feature.setExpansion(expansion);

        Feature updatedFeature = featureRepository.save(feature);
        log.info("Updated feature with id: {}", updatedFeature.getId());

        return toResponse(updatedFeature, Set.of());
    }

    /**
     * Soft deletes a feature.
     *
     * @param id The feature ID to delete
     */
    @Transactional
    public void deleteFeature(Long id) {
        log.info("Soft deleting feature with id: {}", id);

        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        feature.softDelete();
        featureRepository.save(feature);

        log.info("Soft deleted feature with id: {}", id);
    }

    /**
     * Restores a soft-deleted feature.
     *
     * @param id The feature ID to restore
     * @return FeatureResponse containing the restored feature
     */
    @Transactional
    public FeatureResponse restoreFeature(Long id) {
        log.info("Restoring feature with id: {}", id);

        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        if (!feature.isDeleted()) {
            throw new IllegalStateException("Feature with id " + id + " is not deleted");
        }

        feature.restore();
        Feature restoredFeature = featureRepository.save(feature);

        log.info("Restored feature with id: {}", id);

        return toResponse(restoredFeature, Set.of());
    }

    /**
     * Parses the expand parameter into a set of relationship names.
     *
     * @param expand Comma-separated list of relationships to expand
     * @return Set of relationship names
     */
    private Set<String> parseExpand(String expand) {
        if (expand == null || expand.trim().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(List.of(expand.split(",")));
    }

    /**
     * Converts a Feature entity to FeatureResponse DTO.
     *
     * @param feature The feature entity
     * @param expand Set of relationships to expand
     * @return FeatureResponse DTO
     */
    private FeatureResponse toResponse(Feature feature, Set<String> expand) {
        FeatureResponse.FeatureResponseBuilder builder = FeatureResponse.builder()
                .id(feature.getId())
                .name(feature.getName())
                .description(feature.getDescription())
                .featureType(feature.getFeatureType())
                .expansionId(feature.getExpansion().getId())
                .createdAt(feature.getCreatedAt())
                .lastModifiedAt(feature.getLastModifiedAt())
                .deletedAt(feature.getDeletedAt());

        if (expand.contains("expansion")) {
            Expansion expansion = feature.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        return builder.build();
    }
}
