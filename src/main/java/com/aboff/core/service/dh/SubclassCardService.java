package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.dh.response.SubclassPathResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.SubclassCardRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing SubclassCard entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubclassCardService {

    private final SubclassCardRepository subclassCardRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final CardCostTagService cardCostTagService;
    private final SubclassPathService subclassPathService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ContentAccessService contentAccessService;

    /**
     * Retrieves a paginated list of subclass cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID (via subclass path)
     * @param subclassPathId Optional filter for subclass path ID
     * @param level Optional filter for subclass level
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing subclass cards
     */
    @Transactional(readOnly = true)
    public PagedResponse<SubclassCardResponse> getAllSubclassCards(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Long associatedClassId,
            Long subclassPathId,
            SubclassLevel level,
            String expand) {

        includeDeleted = contentAccessService.resolveIncludeDeleted(includeDeleted);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<SubclassCard> cardPage;

        if (includeDeleted) {
            cardPage = subclassCardRepository.findAllWithFilters(expansionId, isOfficial, associatedClassId, subclassPathId, level, pageable);
        } else {
            cardPage = subclassCardRepository.findByDeletedAtIsNullAndFilters(
                    expansionId, isOfficial, associatedClassId, subclassPathId, level, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<SubclassCardResponse>builder()
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
     * Retrieves a single subclass card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand
     * @return SubclassCardResponse containing the card details
     * @throws EntityNotFoundException if the card is not found or is deleted
     */
    @Transactional(readOnly = true)
    public SubclassCardResponse getSubclassCardById(Long id, String expand) {
        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new subclass card.
     * <p>
     * {@code request.getSrd()} is deliberately ignored: a subclass card's {@code srd} flag is
     * always derived from its {@link SubclassPath#getSrd()} rather than accepted from the
     * caller, so the three cards in a path (Foundation/Specialization/Mastery) can never
     * disagree with each other or with the path itself. Only {@code SubclassPathService} may
     * change a path's {@code srd} flag; changing it there cascades to every card in the path.
     * If a caller supplies {@code srd} anyway, it is logged and dropped rather than rejected, so
     * an otherwise valid request does not fail over a field this endpoint does not honor.
     * </p>
     *
     * @param request The creation request containing card details
     * @param authentication The authentication of the requesting user
     * @return SubclassCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     * @throws IllegalArgumentException if subclass path resolution fails
     */
    @Transactional
    public SubclassCardResponse createSubclassCard(CreateSubclassCardRequest request, Authentication authentication) {

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        SubclassPath path = subclassPathService.resolvePath(
                request.getSubclassPathId(),
                request.getSubclassPath(),
                request.getAssociatedClassId(),
                request.getExpansionId());
        warnIfSrdSupplied(request.getSrd());

        SubclassCard card = SubclassCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .srd(path.getSrd())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .subclassPath(path)
                .level(request.getLevel())
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

        SubclassCard savedCard = subclassCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCard, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("subclass_card").build(),
                "\"" + savedCard.getName() + "\" (subclass_card_id: " + savedCard.getId() + ")");

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple subclass cards in bulk.
     * <p>
     * As with {@link #createSubclassCard}, each card's {@code srd} flag is derived from its
     * {@code subclassPath} rather than {@code request.getSrd()} — see that method's Javadoc for
     * why.
     * </p>
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the requesting user
     * @return List of created card responses
     */
    @Transactional
    public List<SubclassCardResponse> createSubclassCardsBulk(List<CreateSubclassCardRequest> requests, Authentication authentication) {

        List<SubclassCard> cards = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    SubclassPath path = subclassPathService.resolvePath(
                            request.getSubclassPathId(),
                            request.getSubclassPath(),
                            request.getAssociatedClassId(),
                            request.getExpansionId());
                    warnIfSrdSupplied(request.getSrd());

                    SubclassCard card = SubclassCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .srd(path.getSrd())
                            .backgroundImageUrl(request.getBackgroundImageUrl())
                            .subclassPath(path)
                            .level(request.getLevel())
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

        List<SubclassCard> savedCards = subclassCardRepository.saveAll(cards);
        savedCards.forEach(c -> eventPublisher.publishEvent(new EntityChangeEvent(this, c, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("subclass_card").build(),
                savedCards.size() + " subclass cards created");

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing subclass card.
     * <p>
     * {@code request.getSrd()} is deliberately ignored — see {@link #createSubclassCard} for
     * why. The card's {@code srd} flag is unconditionally re-derived from its (possibly newly
     * reassigned) {@code subclassPath} on every update, so a path reassignment can never leave
     * the card carrying a stale value.
     * </p>
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @param authentication The authentication of the requesting user
     * @return SubclassCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     * @throws IllegalArgumentException if subclass path resolution fails
     */
    @Transactional
    public SubclassCardResponse updateSubclassCard(Long id, UpdateSubclassCardRequest request, Authentication authentication) {

        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        warnIfSrdSupplied(request.getSrd());

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
        if (request.getSubclassPathId() != null || request.getSubclassPath() != null) {
            SubclassPath path = subclassPathService.resolvePath(
                    request.getSubclassPathId(),
                    request.getSubclassPath(),
                    request.getAssociatedClassId(),
                    request.getExpansionId());
            card.setSubclassPath(path);
        }
        if (request.getLevel() != null) {
            card.setLevel(request.getLevel());
        }

        // Always re-derive srd from the (possibly just-reassigned) subclassPath, never from the request
        card.setSrd(card.getSubclassPath().getSrd());

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

        SubclassCard updatedCard = subclassCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedCard, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("subclass_card").build(),
                "subclass_card_id: " + updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a subclass card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @param authentication The authentication of the requesting user
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteSubclassCard(Long id, Authentication authentication) {
        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        card.softDelete();
        subclassCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, card, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("subclass_card").build(),
                "subclass_card_id: " + id);
    }

    /**
     * Restores a soft-deleted subclass card.
     *
     * @param id The card ID to restore
     * @param authentication The authentication of the requesting user
     * @return SubclassCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public SubclassCardResponse restoreSubclassCard(Long id, Authentication authentication) {
        SubclassCard card = subclassCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("SubclassCard with id " + id + " is not deleted");
        }

        card.restore();
        SubclassCard restoredCard = subclassCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredCard, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("subclass_card").build(),
                "subclass_card_id: " + id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Converts a SubclassCard entity to SubclassCardResponse DTO.
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
     * @return SubclassCardResponse DTO, or a redacted stub if the caller may not view it
     */
    public SubclassCardResponse toResponse(SubclassCard card, Set<String> expand) {
        if (!contentAccessService.mayView(card)) {
            SubclassCardResponse stub = ContentRedaction.stub(SubclassCardResponse::new, card.getId(),
                    card.getExpansion() != null ? card.getExpansion().getName() : null);
            stub.setCardType(card.getCardType());
            return stub;
        }

        SubclassCardResponse.SubclassCardResponseBuilder builder = SubclassCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .expansionName(card.getExpansion().getName())
                .isOfficial(card.getIsOfficial())
                .srd(card.getSrd())
                .backgroundImageUrl(card.getBackgroundImageUrl())
                .associatedClassId(card.getSubclassPath().getAssociatedClass().getId())
                .associatedClassName(card.getSubclassPath().getAssociatedClass().getName())
                .subclassPathId(card.getSubclassPath().getId())
                .subclassPathName(card.getSubclassPath().getName())
                .domainNames(card.getSubclassPath().getAssociatedDomains() != null
                        ? card.getSubclassPath().getAssociatedDomains().stream()
                                .map(Domain::getName)
                                .sorted()
                                .collect(Collectors.toList())
                        : List.of())
                .domainIds(card.getSubclassPath().getAssociatedDomains() != null
                        ? card.getSubclassPath().getAssociatedDomains().stream()
                                .map(Domain::getId)
                                .sorted()
                                .collect(Collectors.toList())
                        : List.of())
                .spellcastingTrait(buildTraitInfo(card.getSubclassPath().getSpellcastingTrait()))
                .level(card.getLevel())
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

        // Expand subclass path if requested
        if (ExpandUtil.shouldExpand(expand, "subclassPath")) {
            builder.subclassPath(subclassPathService.toResponse(card.getSubclassPath(), expand));
        }

        return builder.build();
    }

    /**
     * Builds a TraitInfo response from a Trait enum value.
     *
     * @param trait The trait enum value, may be null
     * @return TraitInfo with trait metadata, or null if trait is null
     */
    private SubclassPathResponse.TraitInfo buildTraitInfo(Trait trait) {
        if (trait == null) {
            return null;
        }
        return SubclassPathResponse.TraitInfo.builder()
                .trait(trait)
                .description(trait.getDescription())
                .examples(trait.getExamples())
                .build();
    }

    /**
     * Logs a warning when a caller supplies an {@code srd} value on a subclass card create or
     * update request, since it is always dropped in favor of the value derived from the card's
     * {@code subclassPath}. See {@link #createSubclassCard} for why.
     *
     * @param requestedSrd the {@code srd} value from the request, or null if not supplied
     */
    private void warnIfSrdSupplied(Boolean requestedSrd) {
        if (requestedSrd != null) {
            log.warn("Caller supplied srd={} on a subclass card request; ignoring — subclass card "
                    + "srd is always derived from its subclassPath", requestedSrd);
        }
    }
}
