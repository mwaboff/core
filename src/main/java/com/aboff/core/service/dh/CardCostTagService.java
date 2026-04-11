package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CostTagInput;
import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.repository.dh.CardCostTagRepository;
import com.aboff.core.event.EntityChangeEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
 * Service for managing CardCostTag entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and filtering by category.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardCostTagService {

    private final CardCostTagRepository cardCostTagRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of cost tags.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cost tags
     * @param category Optional filter for cost tag category
     * @return Paginated response containing cost tags
     */
    @Transactional(readOnly = true)
    public PagedResponse<CardCostTagResponse> getAllCostTags(
            int page,
            int size,
            boolean includeDeleted,
            CostTagCategory category) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<CardCostTag> tagPage;

        if (includeDeleted) {
            tagPage = cardCostTagRepository.findAllWithFilters(category, pageable);
        } else {
            tagPage = cardCostTagRepository.findByDeletedAtIsNullAndFilters(category, pageable);
        }

        return PagedResponse.<CardCostTagResponse>builder()
                .content(tagPage.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .totalElements(tagPage.getTotalElements())
                .totalPages(tagPage.getTotalPages())
                .currentPage(tagPage.getNumber())
                .pageSize(tagPage.getSize())
                .build();
    }

    /**
     * Retrieves a single cost tag by ID.
     *
     * @param id The cost tag ID
     * @return CardCostTagResponse containing the cost tag details
     * @throws EntityNotFoundException if the cost tag is not found or is deleted
     */
    @Transactional(readOnly = true)
    public CardCostTagResponse getCostTagById(Long id) {
        CardCostTag tag = cardCostTagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        return toResponse(tag);
    }

    /**
     * Creates a new cost tag.
     *
     * @param request The creation request containing cost tag details
     * @return CardCostTagResponse containing the created cost tag
     */
    @Transactional
    public CardCostTagResponse createCostTag(CreateCardCostTagRequest request) {
        log.info("Creating new cost tag with label: {}", request.getLabel());

        CardCostTag tag = CardCostTag.builder()
                .label(request.getLabel())
                .category(request.getCategory())
                .build();

        CardCostTag savedTag = cardCostTagRepository.save(tag);
        log.info("Created cost tag with id: {}", savedTag.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedTag, EntityChangeEvent.ChangeType.CREATED));

        return toResponse(savedTag);
    }

    /**
     * Updates an existing cost tag.
     *
     * @param id The cost tag ID to update
     * @param request The update request containing new cost tag details
     * @return CardCostTagResponse containing the updated cost tag
     * @throws EntityNotFoundException if the cost tag is not found or is deleted
     */
    @Transactional
    public CardCostTagResponse updateCostTag(Long id, UpdateCardCostTagRequest request) {
        log.info("Updating cost tag with id: {}", id);

        CardCostTag tag = cardCostTagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            tag.setLabel(request.getLabel());
        }
        if (request.getCategory() != null) {
            tag.setCategory(request.getCategory());
        }

        CardCostTag updatedTag = cardCostTagRepository.save(tag);
        log.info("Updated cost tag with id: {}", updatedTag.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedTag, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedTag);
    }

    /**
     * Soft deletes a cost tag.
     *
     * @param id The cost tag ID to delete
     * @throws EntityNotFoundException if the cost tag is not found or is already deleted
     */
    @Transactional
    public void deleteCostTag(Long id) {
        log.info("Soft deleting cost tag with id: {}", id);

        CardCostTag tag = cardCostTagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        tag.softDelete();
        cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, tag, EntityChangeEvent.ChangeType.SOFT_DELETED));

        log.info("Soft deleted cost tag with id: {}", id);
    }

    /**
     * Restores a soft-deleted cost tag.
     *
     * @param id The cost tag ID to restore
     * @return CardCostTagResponse containing the restored cost tag
     * @throws EntityNotFoundException if the cost tag is not found
     * @throws IllegalStateException if the cost tag is not deleted
     */
    @Transactional
    public CardCostTagResponse restoreCostTag(Long id) {
        log.info("Restoring cost tag with id: {}", id);

        CardCostTag tag = cardCostTagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        if (!tag.isDeleted()) {
            throw new IllegalStateException("CardCostTag with id " + id + " is not deleted");
        }

        tag.restore();
        CardCostTag restoredTag = cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredTag, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored cost tag with id: {}", id);

        return toResponse(restoredTag);
    }

    /**
     * Finds an existing cost tag by label (case-insensitive) or creates a new one.
     *
     * @param label The label to search for
     * @param category The category to use if creating a new tag
     * @return The existing or newly created CardCostTag
     */
    @Transactional
    public CardCostTag findOrCreate(String label, CostTagCategory category) {
        return cardCostTagRepository.findByLabelIgnoreCaseAndDeletedAtIsNull(label)
                .map(existing -> {
                    log.debug("Found existing cost tag with label '{}' (id: {})", label, existing.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Creating new cost tag with label '{}' and category '{}'", label, category);
                    CardCostTag newTag = CardCostTag.builder()
                            .label(label)
                            .category(category)
                            .build();
                    return cardCostTagRepository.save(newTag);
                });
    }

    /**
     * Resolves cost tags from both ID-based and label-based inputs, merging the results.
     * <p>
     * Returns {@code null} when both inputs are null, signaling that existing tags should not be modified
     * (used by update operations). Returns an empty set when at least one input is non-null but both are
     * empty, signaling that tags should be cleared.
     * </p>
     *
     * @param costTagIds Optional list of existing cost tag IDs to look up
     * @param costTags Optional list of cost tag inputs to find or create by label
     * @return Merged set of resolved cost tags, or null if both inputs are null
     */
    @Transactional
    public Set<CardCostTag> resolveCostTags(List<Long> costTagIds, List<CostTagInput> costTags) {
        if (costTagIds == null && costTags == null) {
            return null;
        }

        Set<CardCostTag> resolved = new HashSet<>();

        if (costTagIds != null && !costTagIds.isEmpty()) {
            resolved.addAll(cardCostTagRepository.findAllByIdInAndDeletedAtIsNull(costTagIds));
        }

        if (costTags != null && !costTags.isEmpty()) {
            for (CostTagInput input : costTags) {
                resolved.add(findOrCreate(input.getLabel(), input.getCategory()));
            }
        }

        return resolved;
    }

    /**
     * Converts a CardCostTag entity to CardCostTagResponse DTO.
     *
     * @param tag The cost tag entity
     * @return CardCostTagResponse DTO
     */
    private CardCostTagResponse toResponse(CardCostTag tag) {
        return CardCostTagResponse.builder()
                .id(tag.getId())
                .label(tag.getLabel())
                .category(tag.getCategory())
                .createdAt(tag.getCreatedAt())
                .lastModifiedAt(tag.getLastModifiedAt())
                .deletedAt(tag.getDeletedAt())
                .build();
    }
}
