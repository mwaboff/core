package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
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

import com.aboff.core.util.ExpandUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CardCostTagService cardCostTagService;

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

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

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

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
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

        // Set cost tags if provided
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            feature.setCostTags(resolvedTags);
        }

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

        // Update cost tags
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            feature.setCostTags(resolvedTags);
        }

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
     * Creates multiple features in bulk.
     *
     * @param requests List of creation requests
     * @return List of created feature responses
     */
    @Transactional
    public List<FeatureResponse> createFeaturesBulk(List<CreateFeatureRequest> requests) {
        log.info("Creating {} features in bulk", requests.size());

        List<Feature> features = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Feature feature = Feature.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .featureType(request.getFeatureType())
                            .expansion(expansion)
                            .build();

                    Set<CardCostTag> bulkResolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
                    if (bulkResolvedTags != null) {
                        feature.setCostTags(bulkResolvedTags);
                    }

                    return feature;
                })
                .toList();

        List<Feature> savedFeatures = featureRepository.saveAll(features);
        log.info("Created {} features in bulk", savedFeatures.size());

        return savedFeatures.stream()
                .map(feature -> toResponse(feature, Set.of()))
                .toList();
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

        // Always include cost tag IDs
        if (feature.getCostTags() != null) {
            builder.costTagIds(feature.getCostTags().stream()
                    .map(CardCostTag::getId)
                    .collect(Collectors.toList()));
        }

        // Expand cost tags if requested
        if (expand.contains("costTags") && feature.getCostTags() != null) {
            builder.costTags(feature.getCostTags().stream()
                    .map(tag -> CardCostTagResponse.builder()
                            .id(tag.getId())
                            .label(tag.getLabel())
                            .category(tag.getCategory())
                            .createdAt(tag.getCreatedAt())
                            .lastModifiedAt(tag.getLastModifiedAt())
                            .deletedAt(tag.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
