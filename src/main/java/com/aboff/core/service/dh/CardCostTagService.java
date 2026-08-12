package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CostTagInput;
import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.repository.dh.CardCostTagRepository;
import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.util.ContentRedaction;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
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
    private final ContentAccessService contentAccessService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of cost tags.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cost tags; coerced to false below
     *                       MODERATOR by {@link ContentAccessService#resolveIncludeDeleted}
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

        if (contentAccessService.resolveIncludeDeleted(includeDeleted)) {
            tagPage = cardCostTagRepository.findAllWithFilters(category, pageable);
        } else {
            tagPage = cardCostTagRepository.findByDeletedAtIsNullAndFilters(
                    category, contentAccessService.includeNonSrd(), pageable);
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
     * @param authentication The authentication context of the requesting user
     * @return CardCostTagResponse containing the created cost tag
     */
    @Transactional
    public CardCostTagResponse createCostTag(CreateCardCostTagRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        CardCostTag tag = CardCostTag.builder()
                .label(request.getLabel())
                .category(request.getCategory())
                .srd(contentAccessService.resolveSrd(user, request.getSrd()))
                .build();

        CardCostTag savedTag = cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedTag, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("cost_tag").build(),
                "\"" + savedTag.getLabel() + "\" (cost_tag_id: " + savedTag.getId() + ")");

        return toResponse(savedTag);
    }

    /**
     * Updates an existing cost tag.
     *
     * @param id The cost tag ID to update
     * @param request The update request containing new cost tag details
     * @param authentication The authentication context of the requesting user
     * @return CardCostTagResponse containing the updated cost tag
     * @throws EntityNotFoundException if the cost tag is not found or is deleted
     */
    @Transactional
    public CardCostTagResponse updateCostTag(Long id, UpdateCardCostTagRequest request, Authentication authentication) {
        CardCostTag tag = cardCostTagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            tag.setLabel(request.getLabel());
        }
        if (request.getCategory() != null) {
            tag.setCategory(request.getCategory());
        }
        if (request.getSrd() != null) {
            tag.setSrd(contentAccessService.resolveSrd(currentUser(authentication), request.getSrd()));
        }

        CardCostTag updatedTag = cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedTag, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("cost_tag").build(),
                "cost_tag_id: " + updatedTag.getId());

        return toResponse(updatedTag);
    }

    /**
     * Soft deletes a cost tag.
     *
     * @param id The cost tag ID to delete
     * @param authentication The authentication context of the requesting user
     * @throws EntityNotFoundException if the cost tag is not found or is already deleted
     */
    @Transactional
    public void deleteCostTag(Long id, Authentication authentication) {
        CardCostTag tag = cardCostTagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        tag.softDelete();
        cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, tag, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("cost_tag").build(),
                "cost_tag_id: " + id);
    }

    /**
     * Restores a soft-deleted cost tag.
     *
     * @param id The cost tag ID to restore
     * @param authentication The authentication context of the requesting user
     * @return CardCostTagResponse containing the restored cost tag
     * @throws EntityNotFoundException if the cost tag is not found
     * @throws IllegalStateException if the cost tag is not deleted
     */
    @Transactional
    public CardCostTagResponse restoreCostTag(Long id, Authentication authentication) {
        CardCostTag tag = cardCostTagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CardCostTag not found with id: " + id));

        if (!tag.isDeleted()) {
            throw new IllegalStateException("CardCostTag with id " + id + " is not deleted");
        }

        tag.restore();
        CardCostTag restoredTag = cardCostTagRepository.save(tag);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredTag, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("cost_tag").build(),
                "cost_tag_id: " + id);

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
                    log.debug("Creating new cost tag with label '{}' and category '{}'", label, category);
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
     * <p>
     * CardCostTag has no {@code isOfficial} distinction of its own — every tag is catalogue
     * content, so visibility is decided by {@code srd} alone. {@code true} is passed as the
     * {@code isOfficial} argument to {@link ContentAccessService#mayView(Boolean, Boolean)} to
     * force that check rather than short-circuiting it (a {@code false}/null {@code isOfficial}
     * would make {@code mayView} return {@code true} unconditionally, defeating gating). This is
     * the universal funnel — every caller (list, single-get, and any sibling type expanding its
     * cost tags, e.g. {@code FeatureService}) must route through this method rather than
     * building a {@link CardCostTagResponse} directly.
     * </p>
     *
     * @param tag The cost tag entity
     * @return CardCostTagResponse DTO, or a redacted stub if the caller may not view it
     */
    public CardCostTagResponse toResponse(CardCostTag tag) {
        if (!contentAccessService.mayView(true, tag.getSrd())) {
            return ContentRedaction.stub(CardCostTagResponse::new, tag.getId(), null);
        }

        return CardCostTagResponse.builder()
                .id(tag.getId())
                .label(tag.getLabel())
                .category(tag.getCategory())
                .srd(tag.getSrd())
                .createdAt(tag.getCreatedAt())
                .lastModifiedAt(tag.getLastModifiedAt())
                .deletedAt(tag.getDeletedAt())
                .build();
    }

    /**
     * Resolves the authenticated user from an {@link Authentication}, matching the
     * {@link CustomUserDetails} extraction pattern used throughout {@code service.dh}.
     *
     * @param authentication the authentication context of the requesting user
     * @return the authenticated {@link User}
     */
    private User currentUser(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }
}
