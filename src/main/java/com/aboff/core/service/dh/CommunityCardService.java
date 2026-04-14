package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.dto.dh.response.CommunityCardResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.repository.dh.CommunityCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing CommunityCard entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityCardService {

    private final CommunityCardRepository communityCardRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final CardCostTagService cardCostTagService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of community cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing community cards
     */
    @Transactional(readOnly = true)
    public PagedResponse<CommunityCardResponse> getAllCommunityCards(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<CommunityCard> cardPage;

        if (includeDeleted) {
            cardPage = communityCardRepository.findAllWithFilters(expansionId, isOfficial, pageable);
        } else {
            cardPage = communityCardRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<CommunityCardResponse>builder()
                .content(cardPage.getContent().stream()
                        .map(card -> toResponse(card, expandSet))
                        .toList())
                .totalElements(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .currentPage(cardPage.getNumber())
                .pageSize(cardPage.getSize())
                .build();
    }

    /**
     * Retrieves a single community card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand
     * @return CommunityCardResponse containing the card details
     * @throws EntityNotFoundException if the card is not found or is deleted
     */
    @Transactional(readOnly = true)
    public CommunityCardResponse getCommunityCardById(Long id, String expand) {
        CommunityCard card = communityCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new community card.
     *
     * @param request The creation request containing card details
     * @param authentication The authentication of the requesting user
     * @return CommunityCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public CommunityCardResponse createCommunityCard(CreateCommunityCardRequest request, Authentication authentication) {

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        CommunityCard card = CommunityCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .build();

        // Set features if provided
        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            card.setFeatures(resolvedFeatures);
        }

        // Set cost tags if provided
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            card.setCostTags(resolvedTags);
        }

        CommunityCard savedCard = communityCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCard, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("community_card").build(),
                "\"" + savedCard.getName() + "\" (community_card_id: " + savedCard.getId() + ")");

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple community cards in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the requesting user
     * @return List of created card responses
     */
    @Transactional
    public List<CommunityCardResponse> createCommunityCardsBulk(List<CreateCommunityCardRequest> requests, Authentication authentication) {

        List<CommunityCard> cards = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    CommunityCard card = CommunityCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .backgroundImageUrl(request.getBackgroundImageUrl())
                            .build();

                    Set<Feature> bulkResolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
                    if (bulkResolvedFeatures != null) {
                        card.setFeatures(bulkResolvedFeatures);
                    }

                    Set<CardCostTag> bulkResolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
                    if (bulkResolvedTags != null) {
                        card.setCostTags(bulkResolvedTags);
                    }

                    return card;
                })
                .toList();

        List<CommunityCard> savedCards = communityCardRepository.saveAll(cards);
        savedCards.forEach(c -> eventPublisher.publishEvent(new EntityChangeEvent(this, c, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("community_card").build(),
                savedCards.size() + " community cards created");

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing community card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param authentication The authentication of the requesting user
     * @return CommunityCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     */
    @Transactional
    public CommunityCardResponse updateCommunityCard(Long id, UpdateCommunityCardRequest request, Authentication authentication) {

        CommunityCard card = communityCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            card.setName(request.getName());
        }
        if (request.getDescription() != null) {
            card.setDescription(request.getDescription());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            card.setExpansion(expansion);
        }
        if (request.getIsOfficial() != null) {
            card.setIsOfficial(request.getIsOfficial());
        }
        if (request.getBackgroundImageUrl() != null) {
            card.setBackgroundImageUrl(request.getBackgroundImageUrl());
        }

        // Update features
        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            card.setFeatures(resolvedFeatures);
        }

        // Update cost tags
        Set<CardCostTag> resolvedTags = cardCostTagService.resolveCostTags(request.getCostTagIds(), request.getCostTags());
        if (resolvedTags != null) {
            card.setCostTags(resolvedTags);
        }

        CommunityCard updatedCard = communityCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedCard, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("community_card").build(),
                "community_card_id: " + updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a community card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @param authentication The authentication of the requesting user
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteCommunityCard(Long id, Authentication authentication) {
        CommunityCard card = communityCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        card.softDelete();
        communityCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, card, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("community_card").build(),
                "community_card_id: " + id);
    }

    /**
     * Restores a soft-deleted community card.
     *
     * @param id The card ID to restore
     * @param authentication The authentication of the requesting user
     * @return CommunityCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public CommunityCardResponse restoreCommunityCard(Long id, Authentication authentication) {
        CommunityCard card = communityCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("CommunityCard with id " + id + " is not deleted");
        }

        card.restore();
        CommunityCard restoredCard = communityCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredCard, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("community_card").build(),
                "community_card_id: " + id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Converts a CommunityCard entity to CommunityCardResponse DTO.
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return CommunityCardResponse DTO
     */
    public CommunityCardResponse toResponse(CommunityCard card, Set<String> expand) {
        CommunityCardResponse.CommunityCardResponseBuilder builder = CommunityCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .isOfficial(card.getIsOfficial())
                .backgroundImageUrl(card.getBackgroundImageUrl())
                .createdAt(card.getCreatedAt())
                .lastModifiedAt(card.getLastModifiedAt())
                .deletedAt(card.getDeletedAt());

        // Always include feature IDs
        if (card.getFeatures() != null) {
            builder.featureIds(card.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        // Expand expansion if requested
        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = card.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        // Expand features if requested
        if (ExpandUtil.shouldExpand(expand, "features") && card.getFeatures() != null) {
            builder.features(card.getFeatures().stream()
                    .map(feature -> featureService.toResponse(feature, expand))
                    .collect(Collectors.toList()));
        }

        // Always include cost tag IDs
        if (card.getCostTags() != null) {
            builder.costTagIds(card.getCostTags().stream()
                    .map(CardCostTag::getId)
                    .collect(Collectors.toList()));
        }

        // Expand cost tags if requested
        if (ExpandUtil.shouldExpand(expand, "costTags") && card.getCostTags() != null) {
            builder.costTags(card.getCostTags().stream()
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
