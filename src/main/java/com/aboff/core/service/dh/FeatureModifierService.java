package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureModifierRequest;
import com.aboff.core.model.dto.dh.request.FeatureModifierInput;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.repository.dh.FeatureModifierRepository;
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
 * Service for managing FeatureModifier entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion, and
 * find-or-create semantics for feature modifiers. Feature modifiers represent
 * structured, machine-readable stat modifications (e.g., +1 Strength, -1 Evasion)
 * that can be associated with Features.
 * </p>
 * <p>
 * Modifiers are deduplicated by their composite key of (target, operation, value),
 * allowing the same modifier to be shared across multiple features.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureModifierService {

    private final FeatureModifierRepository featureModifierRepository;

    /**
     * Retrieves a paginated list of feature modifiers.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted modifiers
     * @return Paginated response containing feature modifiers
     */
    @Transactional(readOnly = true)
    public PagedResponse<FeatureModifierResponse> getAllModifiers(int page, int size, boolean includeDeleted) {
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<FeatureModifier> modifierPage;

        if (includeDeleted) {
            modifierPage = featureModifierRepository.findAll(pageable);
        } else {
            modifierPage = featureModifierRepository.findAllByDeletedAtIsNull(pageable);
        }

        return PagedResponse.<FeatureModifierResponse>builder()
                .content(modifierPage.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .totalElements(modifierPage.getTotalElements())
                .totalPages(modifierPage.getTotalPages())
                .currentPage(modifierPage.getNumber())
                .pageSize(modifierPage.getSize())
                .build();
    }

    /**
     * Retrieves a single feature modifier by ID.
     *
     * @param id The modifier ID
     * @return FeatureModifierResponse containing the modifier details
     * @throws EntityNotFoundException if the modifier is not found or is deleted
     */
    @Transactional(readOnly = true)
    public FeatureModifierResponse getModifier(Long id) {
        FeatureModifier modifier = featureModifierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("FeatureModifier not found with id: " + id));

        return toResponse(modifier);
    }

    /**
     * Creates a new feature modifier.
     *
     * @param request The creation request containing modifier details
     * @return FeatureModifierResponse containing the created modifier
     */
    @Transactional
    public FeatureModifierResponse createModifier(CreateFeatureModifierRequest request) {
        log.info("Creating new feature modifier: {} {} {}", request.getOperation(), request.getValue(), request.getTarget());

        FeatureModifier modifier = FeatureModifier.builder()
                .target(request.getTarget())
                .operation(request.getOperation())
                .value(request.getValue())
                .build();

        FeatureModifier savedModifier = featureModifierRepository.save(modifier);
        log.info("Created feature modifier with id: {}", savedModifier.getId());

        return toResponse(savedModifier);
    }

    /**
     * Soft deletes a feature modifier.
     *
     * @param id The modifier ID to delete
     * @throws EntityNotFoundException if the modifier is not found or is already deleted
     */
    @Transactional
    public void deleteModifier(Long id) {
        log.info("Soft deleting feature modifier with id: {}", id);

        FeatureModifier modifier = featureModifierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("FeatureModifier not found with id: " + id));

        modifier.softDelete();
        featureModifierRepository.save(modifier);

        log.info("Soft deleted feature modifier with id: {}", id);
    }

    /**
     * Restores a soft-deleted feature modifier.
     *
     * @param id The modifier ID to restore
     * @return FeatureModifierResponse containing the restored modifier
     * @throws EntityNotFoundException if the modifier is not found
     * @throws IllegalStateException if the modifier is not deleted
     */
    @Transactional
    public FeatureModifierResponse restoreModifier(Long id) {
        log.info("Restoring feature modifier with id: {}", id);

        FeatureModifier modifier = featureModifierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FeatureModifier not found with id: " + id));

        if (!modifier.isDeleted()) {
            throw new IllegalStateException("FeatureModifier with id " + id + " is not deleted");
        }

        modifier.restore();
        FeatureModifier restoredModifier = featureModifierRepository.save(modifier);

        log.info("Restored feature modifier with id: {}", id);

        return toResponse(restoredModifier);
    }

    /**
     * Finds an existing feature modifier by its composite key (target, operation, value)
     * or creates a new one if no match is found.
     *
     * @param input The modifier input containing target, operation, and value
     * @return The existing or newly created FeatureModifier
     */
    @Transactional
    public FeatureModifier findOrCreate(FeatureModifierInput input) {
        return featureModifierRepository
                .findByTargetAndOperationAndValueAndDeletedAtIsNull(
                        input.getTarget(), input.getOperation(), input.getValue())
                .map(existing -> {
                    log.debug("Found existing feature modifier with target '{}', operation '{}', value '{}' (id: {})",
                            input.getTarget(), input.getOperation(), input.getValue(), existing.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Creating new feature modifier: {} {} {}",
                            input.getOperation(), input.getValue(), input.getTarget());
                    FeatureModifier newModifier = FeatureModifier.builder()
                            .target(input.getTarget())
                            .operation(input.getOperation())
                            .value(input.getValue())
                            .build();
                    return featureModifierRepository.save(newModifier);
                });
    }

    /**
     * Resolves feature modifiers from both ID-based and input-based sources, merging the results.
     * <p>
     * Returns {@code null} when both inputs are null, signaling that existing modifiers should not be modified
     * (used by update operations). Returns an empty set when at least one input is non-null but both are
     * empty, signaling that modifiers should be cleared.
     * </p>
     *
     * @param modifierIds Optional list of existing modifier IDs to look up
     * @param modifiers Optional list of modifier inputs to find or create by (target, operation, value)
     * @return Merged set of resolved feature modifiers, or null if both inputs are null
     */
    @Transactional
    public Set<FeatureModifier> resolveModifiers(List<Long> modifierIds, List<FeatureModifierInput> modifiers) {
        if (modifierIds == null && modifiers == null) {
            return null;
        }

        Set<FeatureModifier> resolved = new HashSet<>();

        if (modifierIds != null && !modifierIds.isEmpty()) {
            resolved.addAll(featureModifierRepository.findAllByIdInAndDeletedAtIsNull(modifierIds));
        }

        if (modifiers != null && !modifiers.isEmpty()) {
            for (FeatureModifierInput input : modifiers) {
                resolved.add(findOrCreate(input));
            }
        }

        return resolved;
    }

    /**
     * Converts a FeatureModifier entity to FeatureModifierResponse DTO.
     *
     * @param modifier The feature modifier entity
     * @return FeatureModifierResponse DTO
     */
    public FeatureModifierResponse toResponse(FeatureModifier modifier) {
        return FeatureModifierResponse.builder()
                .id(modifier.getId())
                .target(modifier.getTarget())
                .operation(modifier.getOperation())
                .value(modifier.getValue())
                .createdAt(modifier.getCreatedAt())
                .lastModifiedAt(modifier.getLastModifiedAt())
                .deletedAt(modifier.getDeletedAt())
                .build();
    }
}
