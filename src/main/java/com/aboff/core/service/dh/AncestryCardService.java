package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.CreateMixedAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.dto.dh.response.AncestryCardResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.dh.AncestryCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.util.ContentRedaction;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing AncestryCard entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AncestryCardService {

    private final AncestryCardRepository ancestryCardRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureRepository featureRepository;
    private final FeatureService featureService;
    private final CardCostTagService cardCostTagService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ContentAccessService contentAccessService;

    /**
     * Retrieves a paginated list of ancestry cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isMixed Optional filter for mixed ancestry status; defaults to false when null to hide mixed ancestries from standard listings
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing ancestry cards
     */
    @Transactional(readOnly = true)
    public PagedResponse<AncestryCardResponse> getAllAncestryCards(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Boolean isMixed,
            String expand) {

        includeDeleted = contentAccessService.resolveIncludeDeleted(includeDeleted);
        Boolean effectiveIsMixed = (isMixed == null) ? false : isMixed;

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<AncestryCard> cardPage;

        if (includeDeleted) {
            cardPage = ancestryCardRepository.findAllWithFilters(expansionId, isOfficial, effectiveIsMixed, pageable);
        } else {
            cardPage = ancestryCardRepository.findByDeletedAtIsNullAndFilters(
                    expansionId, isOfficial, effectiveIsMixed, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<AncestryCardResponse>builder()
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
     * Retrieves a single ancestry card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand
     * @return AncestryCardResponse containing the card details
     * @throws EntityNotFoundException if the card is not found or is deleted
     */
    @Transactional(readOnly = true)
    public AncestryCardResponse getAncestryCardById(Long id, String expand) {
        AncestryCard card = ancestryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new ancestry card.
     *
     * @param request The creation request containing card details
     * @param authentication The authentication of the requesting user
     * @return AncestryCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public AncestryCardResponse createAncestryCard(CreateAncestryCardRequest request, Authentication authentication) {

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        AncestryCard card = AncestryCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .srd(contentAccessService.resolveSrd(currentUser(authentication), request.getSrd()))
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

        AncestryCard savedCard = ancestryCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCard, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                "\"" + savedCard.getName() + "\" (ancestry_card_id: " + savedCard.getId() + ")");

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple ancestry cards in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the requesting user
     * @return List of created card responses
     */
    @Transactional
    public List<AncestryCardResponse> createAncestryCardsBulk(List<CreateAncestryCardRequest> requests, Authentication authentication) {

        User user = currentUser(authentication);
        List<AncestryCard> cards = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    AncestryCard card = AncestryCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .srd(contentAccessService.resolveSrd(user, request.getSrd()))
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

        List<AncestryCard> savedCards = ancestryCardRepository.saveAll(cards);
        savedCards.forEach(c -> eventPublisher.publishEvent(new EntityChangeEvent(this, c, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                savedCards.size() + " ancestry cards created");

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Creates a mixed ancestry card combining features from two different ancestries.
     * Mixed ancestry cards are always non-official user-created content.
     *
     * @param request The creation request containing mixed ancestry details
     * @param authentication The authentication of the requesting user
     * @return AncestryCardResponse containing the created mixed ancestry card
     * @throws IllegalArgumentException if featureIds does not contain exactly two entries
     * @throws EntityNotFoundException if referenced expansion or features are not found
     */
    @Transactional
    public AncestryCardResponse createMixedAncestryCard(CreateMixedAncestryCardRequest request, Authentication authentication) {

        if (request.getFeatureIds() == null || request.getFeatureIds().size() != 2) {
            throw new IllegalArgumentException("Exactly two feature IDs must be provided for a mixed ancestry card");
        }

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        List<Feature> features = featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds());
        if (features.size() != 2) {
            throw new EntityNotFoundException("One or more features not found for the provided feature IDs");
        }

        AncestryCard card = AncestryCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(false)
                .isMixed(true)
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .features(new HashSet<>(features))
                .build();

        AncestryCard savedCard = ancestryCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCard, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                "\"" + savedCard.getName() + "\" (ancestry_card_id: " + savedCard.getId() + ")");

        return toResponse(savedCard, Set.of());
    }

    /**
     * Updates an existing ancestry card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param authentication The authentication of the requesting user
     * @return AncestryCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     */
    @Transactional
    public AncestryCardResponse updateAncestryCard(Long id, UpdateAncestryCardRequest request, Authentication authentication) {

        AncestryCard card = ancestryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

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
        if (request.getSrd() != null) {
            card.setSrd(contentAccessService.resolveSrd(currentUser(authentication), request.getSrd()));
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

        AncestryCard updatedCard = ancestryCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedCard, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                "ancestry_card_id: " + updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes an ancestry card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @param authentication The authentication of the requesting user
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteAncestryCard(Long id, Authentication authentication) {
        AncestryCard card = ancestryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        card.softDelete();
        ancestryCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, card, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                "ancestry_card_id: " + id);
    }

    /**
     * Restores a soft-deleted ancestry card.
     *
     * @param id The card ID to restore
     * @param authentication The authentication of the requesting user
     * @return AncestryCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public AncestryCardResponse restoreAncestryCard(Long id, Authentication authentication) {
        AncestryCard card = ancestryCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("AncestryCard with id " + id + " is not deleted");
        }

        card.restore();
        AncestryCard restoredCard = ancestryCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredCard, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("ancestry_card").build(),
                "ancestry_card_id: " + id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Converts an AncestryCard entity to AncestryCardResponse DTO.
     * <p>
     * If the caller may not view this card (gated non-SRD content outside their access), returns
     * a redacted stub instead — see {@link ContentRedaction#stub}. This is the universal funnel:
     * list endpoints, single-get, and embedded-content resolution (e.g. character sheets, search
     * {@code ?expand=}) all route through this method, so it is the one place redaction can be
     * enforced without every caller having to remember to check.
     * </p>
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return AncestryCardResponse DTO, or a redacted stub if the caller may not view it
     */
    public AncestryCardResponse toResponse(AncestryCard card, Set<String> expand) {
        if (!contentAccessService.mayView(card)) {
            AncestryCardResponse stub = ContentRedaction.stub(AncestryCardResponse::new, card.getId(),
                    card.getExpansion() != null ? card.getExpansion().getName() : null);
            stub.setCardType(card.getCardType());
            return stub;
        }

        AncestryCardResponse.AncestryCardResponseBuilder builder = AncestryCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .expansionName(card.getExpansion() != null ? card.getExpansion().getName() : null)
                .isOfficial(card.getIsOfficial())
                .srd(card.getSrd())
                .isMixed(card.getIsMixed())
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

        // Expand cost tags if requested. Routed through CardCostTagService#toResponse (not built
        // inline) so a gated non-SRD cost tag redacts to a stub here too.
        if (ExpandUtil.shouldExpand(expand, "costTags") && card.getCostTags() != null) {
            builder.costTags(card.getCostTags().stream()
                    .map(cardCostTagService::toResponse)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Extracts the authenticated user from the security context principal.
     *
     * @param authentication the current authentication
     * @return the authenticated user
     */
    private User currentUser(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }
}
