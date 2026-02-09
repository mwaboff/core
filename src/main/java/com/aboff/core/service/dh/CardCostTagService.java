package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.repository.dh.CardCostTagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        tag.setLabel(request.getLabel());
        tag.setCategory(request.getCategory());

        CardCostTag updatedTag = cardCostTagRepository.save(tag);
        log.info("Updated cost tag with id: {}", updatedTag.getId());

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

        log.info("Restored cost tag with id: {}", id);

        return toResponse(restoredTag);
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
