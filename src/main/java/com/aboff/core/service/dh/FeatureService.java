package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
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

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;

import com.aboff.core.model.dto.dh.request.FeatureInput;

import java.util.HashSet;
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
    private final FeatureModifierService featureModifierService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

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
     * @param authentication The authentication context of the requesting user
     * @return FeatureResponse containing the created feature
     */
    @Transactional
    public FeatureResponse createFeature(CreateFeatureRequest request, Authentication authentication) {

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Feature feature = Feature.builder()
                .name(request.getName())
                .description(request.getDescription())
                .featureType(request.getFeatureType())
                .timing(request.getTiming())
                .expansion(expansion)
                .build();

        // Set cost tags if provided
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            feature.setCostTags(resolvedTags);
        }

        // Set modifiers if provided
        Set<FeatureModifier> resolvedModifiers = featureModifierService.resolveModifiers(
                request.getModifierIds(), request.getModifiers());
        if (resolvedModifiers != null) {
            feature.setModifiers(resolvedModifiers);
        }

        Feature savedFeature = featureRepository.save(feature);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedFeature, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("feature").build(),
                "\"" + savedFeature.getName() + "\" (feature_id: " + savedFeature.getId() + ")");

        return toResponse(savedFeature, Set.of());
    }

    /**
     * Updates an existing feature.
     *
     * @param id The feature ID to update
     * @param request The update request containing new feature details
     * @param authentication The authentication context of the requesting user
     * @return FeatureResponse containing the updated feature
     */
    @Transactional
    public FeatureResponse updateFeature(Long id, UpdateFeatureRequest request, Authentication authentication) {

        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            feature.setName(request.getName());
        }
        if (request.getDescription() != null) {
            feature.setDescription(request.getDescription());
        }
        if (request.getFeatureType() != null) {
            feature.setFeatureType(request.getFeatureType());
        }
        if (request.getTiming() != null) {
            feature.setTiming(request.getTiming());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            feature.setExpansion(expansion);
        }

        // Update cost tags
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            feature.setCostTags(resolvedTags);
        }

        // Update modifiers
        Set<FeatureModifier> resolvedModifiers = featureModifierService.resolveModifiers(
                request.getModifierIds(), request.getModifiers());
        if (resolvedModifiers != null) {
            feature.setModifiers(resolvedModifiers);
        }

        Feature updatedFeature = featureRepository.save(feature);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedFeature, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("feature").build(),
                "feature_id: " + updatedFeature.getId());

        return toResponse(updatedFeature, Set.of());
    }

    /**
     * Soft deletes a feature.
     *
     * @param id The feature ID to delete
     * @param authentication The authentication context of the requesting user
     */
    @Transactional
    public void deleteFeature(Long id, Authentication authentication) {
        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        feature.softDelete();
        featureRepository.save(feature);
        eventPublisher.publishEvent(new EntityChangeEvent(this, feature, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("feature").build(),
                "feature_id: " + id);
    }

    /**
     * Restores a soft-deleted feature.
     *
     * @param id The feature ID to restore
     * @param authentication The authentication context of the requesting user
     * @return FeatureResponse containing the restored feature
     */
    @Transactional
    public FeatureResponse restoreFeature(Long id, Authentication authentication) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feature not found with id: " + id));

        if (!feature.isDeleted()) {
            throw new IllegalStateException("Feature with id " + id + " is not deleted");
        }

        feature.restore();
        Feature restoredFeature = featureRepository.save(feature);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredFeature, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("feature").build(),
                "feature_id: " + id);

        return toResponse(restoredFeature, Set.of());
    }

    /**
     * Creates multiple features in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication context of the requesting user
     * @return List of created feature responses
     */
    @Transactional
    public List<FeatureResponse> createFeaturesBulk(List<CreateFeatureRequest> requests, Authentication authentication) {

        List<Feature> features = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Feature feature = Feature.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .featureType(request.getFeatureType())
                            .timing(request.getTiming())
                            .expansion(expansion)
                            .build();

                    Set<CardCostTag> bulkResolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
                    if (bulkResolvedTags != null) {
                        feature.setCostTags(bulkResolvedTags);
                    }

                    Set<FeatureModifier> bulkResolvedModifiers = featureModifierService.resolveModifiers(
                            request.getModifierIds(), request.getModifiers());
                    if (bulkResolvedModifiers != null) {
                        feature.setModifiers(bulkResolvedModifiers);
                    }

                    return feature;
                })
                .toList();

        List<Feature> savedFeatures = featureRepository.saveAll(features);
        savedFeatures.forEach(f -> eventPublisher.publishEvent(new EntityChangeEvent(this, f, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("feature").build(),
                savedFeatures.size() + " created");

        return savedFeatures.stream()
                .map(feature -> toResponse(feature, Set.of()))
                .toList();
    }

    /**
     * Finds an existing feature by name+expansion+type+description (case-insensitive on name,
     * exact/case-sensitive on description) or creates a new one. Description is part of the
     * find-or-create key so that features which share a name but carry different rules text —
     * e.g. the core rulebook's per-tier "Barrier"/"Paired"/"Protective" weapon features — are
     * differentiated into distinct rows instead of being silently collapsed onto whichever one
     * was imported first. When no match is found, a new feature is created with the provided
     * details including any cost tags resolved via {@link CardCostTagService#resolveCostTags}.
     *
     * @param input The feature input containing name, type, expansion, description, and optional cost tags
     * @return The existing or newly created Feature entity
     * @throws EntityNotFoundException if the expansion referenced by the input does not exist
     */
    @Transactional
    public Feature findOrCreate(FeatureInput input) {
        // Only attempt to find an existing feature if a name is provided
        if (input.getName() != null && !input.getName().isBlank()) {
            return featureRepository
                    .findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                            input.getName(), input.getExpansionId(), input.getFeatureType(), input.getDescription())
                    .map(existing -> {
                        log.debug("Found existing feature with name '{}' (id: {})", input.getName(), existing.getId());
                        return existing;
                    })
                    .orElseGet(() -> createFeatureFromInput(input));
        }

        return createFeatureFromInput(input);
    }

    /**
     * Creates a new Feature entity from a FeatureInput, resolving expansion, cost tags, and modifiers.
     *
     * @param input The feature input containing details for the new feature
     * @return The newly created and persisted Feature entity
     * @throws EntityNotFoundException if the expansion referenced by the input does not exist
     */
    private Feature createFeatureFromInput(FeatureInput input) {
        log.debug("Creating new feature with name '{}', type '{}', expansion '{}'",
                input.getName(), input.getFeatureType(), input.getExpansionId());
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(input.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + input.getExpansionId()));
        Feature feature = Feature.builder()
                .name(input.getName())
                .description(input.getDescription())
                .featureType(input.getFeatureType())
                .timing(input.getTiming())
                .expansion(expansion)
                .build();
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(
                input.getCostTagIds(), input.getCostTags());
        if (resolvedTags != null) {
            feature.setCostTags(resolvedTags);
        }
        Set<FeatureModifier> resolvedModifiers = featureModifierService.resolveModifiers(
                input.getModifierIds(), input.getModifiers());
        if (resolvedModifiers != null) {
            feature.setModifiers(resolvedModifiers);
        }
        return featureRepository.save(feature);
    }

    /**
     * Resolves features from both ID-based and input-based sources, merging the results.
     * <p>
     * Returns {@code null} when both inputs are null, signaling that existing features should not be modified
     * (used by update operations). Returns an empty set when at least one input is non-null but both are
     * empty, signaling that features should be cleared.
     * </p>
     *
     * @param featureIds Optional list of existing feature IDs to look up
     * @param features Optional list of feature inputs to find or create
     * @return Merged set of resolved features, or null if both inputs are null
     */
    @Transactional
    public Set<Feature> resolveFeatures(List<Long> featureIds, List<FeatureInput> features) {
        if (featureIds == null && features == null) {
            return null;
        }

        Set<Feature> resolved = new HashSet<>();

        if (featureIds != null && !featureIds.isEmpty()) {
            resolved.addAll(featureRepository.findAllByIdInAndDeletedAtIsNull(featureIds));
        }

        if (features != null && !features.isEmpty()) {
            for (FeatureInput input : features) {
                resolved.add(findOrCreate(input));
            }
        }

        return resolved;
    }

    /**
     * Resolves a single feature from either an ID or an inline input.
     * <p>
     * Returns {@code null} when both inputs are null, signaling that the existing feature should not be modified.
     * When both are provided, the ID takes precedence.
     * </p>
     *
     * @param featureId Optional ID of an existing feature
     * @param feature Optional inline feature input to find or create
     * @return The resolved Feature entity, or null if both inputs are null
     * @throws EntityNotFoundException if the feature ID does not match any non-deleted feature
     */
    @Transactional
    public Feature resolveFeature(Long featureId, FeatureInput feature) {
        if (featureId == null && feature == null) {
            return null;
        }

        if (featureId != null) {
            return featureRepository.findByIdAndDeletedAtIsNull(featureId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Feature not found with id: " + featureId));
        }

        return findOrCreate(feature);
    }

    /**
     * Converts a Feature entity to FeatureResponse DTO.
     *
     * @param feature The feature entity
     * @param expand Set of relationships to expand
     * @return FeatureResponse DTO
     */
    public FeatureResponse toResponse(Feature feature, Set<String> expand) {
        FeatureResponse.FeatureResponseBuilder builder = FeatureResponse.builder()
                .id(feature.getId())
                .name(feature.getName())
                .description(feature.getDescription())
                .featureType(feature.getFeatureType())
                .timing(feature.getTiming())
                .expansionId(feature.getExpansion().getId())
                .createdAt(feature.getCreatedAt())
                .lastModifiedAt(feature.getLastModifiedAt())
                .deletedAt(feature.getDeletedAt());

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
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
        if (ExpandUtil.shouldExpand(expand, "costTags") && feature.getCostTags() != null) {
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

        // Always include modifier IDs
        if (feature.getModifiers() != null) {
            builder.modifierIds(feature.getModifiers().stream()
                    .map(FeatureModifier::getId)
                    .collect(Collectors.toList()));
        }

        // Expand modifiers if requested
        if (ExpandUtil.shouldExpand(expand, "modifiers") && feature.getModifiers() != null) {
            builder.modifiers(feature.getModifiers().stream()
                    .map(modifier -> FeatureModifierResponse.builder()
                            .id(modifier.getId())
                            .target(modifier.getTarget())
                            .operation(modifier.getOperation())
                            .value(modifier.getValue())
                            .createdAt(modifier.getCreatedAt())
                            .lastModifiedAt(modifier.getLastModifiedAt())
                            .deletedAt(modifier.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
