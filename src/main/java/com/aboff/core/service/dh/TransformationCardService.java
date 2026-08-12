package com.aboff.core.service.dh;

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateTransformationCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationCardRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.TransformationCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.util.ContentRedaction;
import com.aboff.core.util.ExpandUtil;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing TransformationCard entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 * <p>
 * TransformationCard is a standalone entity, not a {@code DomainCard} row, so nothing here
 * touches domain-card loadout accounting.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransformationCardService {

    private final TransformationCardRepository transformationCardRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final QuestionService questionService;
    private final ContentAccessService contentAccessService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of transformation cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted transformation cards
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing transformation cards
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransformationCardResponse> getAllTransformationCards(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<TransformationCard> cardPage;

        if (contentAccessService.resolveIncludeDeleted(includeDeleted)) {
            cardPage = transformationCardRepository.findAllWithExpansion(expansionId, pageable);
        } else {
            cardPage = transformationCardRepository.findByDeletedAtIsNullAndExpansion(
                    expansionId, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<TransformationCardResponse>builder()
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
     * Retrieves a single transformation card by ID.
     *
     * @param id The transformation card ID
     * @param expand Comma-separated list of relationships to expand
     * @return TransformationCardResponse containing the transformation card details
     * @throws EntityNotFoundException if the transformation card is not found or is deleted
     */
    @Transactional(readOnly = true)
    public TransformationCardResponse getTransformationCardById(Long id, String expand) {
        TransformationCard card = transformationCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Transformation card not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new transformation card.
     *
     * @param request The creation request containing transformation card details
     * @param authentication The authentication of the current user
     * @return TransformationCardResponse containing the created transformation card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public TransformationCardResponse createTransformationCard(
            CreateTransformationCardRequest request, Authentication authentication) {
        TransformationCard card = buildFromRequest(request, currentUser(authentication));
        TransformationCard savedCard = transformationCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCard, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED,
                AuditContext.forUser(authentication).withEntityType("transformationCard").build(),
                "\"" + savedCard.getName() + "\" (transformation_card_id: " + savedCard.getId() + ")");

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple transformation cards in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created transformation card responses
     */
    @Transactional
    public List<TransformationCardResponse> createTransformationCardsBulk(
            List<CreateTransformationCardRequest> requests, Authentication authentication) {
        User user = currentUser(authentication);
        List<TransformationCard> cards = requests.stream()
                .map(request -> buildFromRequest(request, user))
                .toList();

        List<TransformationCard> savedCards = transformationCardRepository.saveAll(cards);
        savedCards.forEach(c -> eventPublisher.publishEvent(
                new EntityChangeEvent(this, c, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED,
                AuditContext.forUser(authentication).withEntityType("transformationCard").build(),
                savedCards.size() + " created");

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing transformation card.
     *
     * @param id The transformation card ID to update
     * @param request The update request containing new transformation card details
     * @param authentication The authentication of the current user
     * @return TransformationCardResponse containing the updated transformation card
     * @throws EntityNotFoundException if the transformation card or referenced entities are not found
     */
    @Transactional
    public TransformationCardResponse updateTransformationCard(
            Long id, UpdateTransformationCardRequest request, Authentication authentication) {
        TransformationCard card = transformationCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Transformation card not found with id: " + id));

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
        if (request.getSrd() != null) {
            card.setSrd(contentAccessService.resolveSrd(currentUser(authentication), request.getSrd()));
        }

        // Inline features inherit isOfficial and srd from this card's now-current values,
        // rather than defaulting to official via FeatureService's default origin.
        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                request.getFeatureIds(), request.getFeatures(),
                FeatureService.FeatureOrigin.forParent(
                        currentUser(authentication), card.getIsOfficial(), card.getSrd()));
        if (resolvedFeatures != null) {
            card.setFeatures(resolvedFeatures);
        }

        Set<Question> resolvedQuestions = questionService.resolveQuestions(
                request.getQuestionIds(), request.getQuestions());
        if (resolvedQuestions != null) {
            card.setQuestions(resolvedQuestions);
        }

        TransformationCard updatedCard = transformationCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedCard, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED,
                AuditContext.forUser(authentication).withEntityType("transformationCard").build(),
                "transformation_card_id: " + updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a transformation card by setting its deletedAt timestamp.
     *
     * @param id The transformation card ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the transformation card is not found or is already deleted
     */
    @Transactional
    public void deleteTransformationCard(Long id, Authentication authentication) {
        TransformationCard card = transformationCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Transformation card not found with id: " + id));

        card.softDelete();
        transformationCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, card, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED,
                AuditContext.forUser(authentication).withEntityType("transformationCard").build(),
                "transformation_card_id: " + id);
    }

    /**
     * Restores a soft-deleted transformation card.
     *
     * @param id The transformation card ID to restore
     * @param authentication The authentication of the current user
     * @return TransformationCardResponse containing the restored transformation card
     * @throws EntityNotFoundException if the transformation card is not found
     * @throws IllegalStateException if the transformation card is not deleted
     */
    @Transactional
    public TransformationCardResponse restoreTransformationCard(Long id, Authentication authentication) {
        TransformationCard card = transformationCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transformation card not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("Transformation card with id " + id + " is not deleted");
        }

        card.restore();
        TransformationCard restoredCard = transformationCardRepository.save(card);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredCard, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED,
                AuditContext.forUser(authentication).withEntityType("transformationCard").build(),
                "transformation_card_id: " + id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Builds a TransformationCard entity from a CreateTransformationCardRequest, resolving all relationships.
     * <p>
     * {@code isOfficial} is hardcoded {@code true}: unlike weapons, armor, or loot, transformation
     * cards have no {@code /custom} authoring endpoint — every create/bulk-create call is
     * ADMIN/OWNER-only catalogue content, so there is no case where {@code false} applies.
     * </p>
     *
     * @param request The creation request containing transformation card details
     * @param user The user performing the create, used to resolve the requested srd flag
     * @return The built TransformationCard entity (not yet persisted)
     * @throws EntityNotFoundException if the referenced expansion is not found
     */
    private TransformationCard buildFromRequest(CreateTransformationCardRequest request, User user) {
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        TransformationCard card = TransformationCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(true)
                .srd(contentAccessService.resolveSrd(user, request.getSrd()))
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                request.getFeatureIds(), request.getFeatures(),
                FeatureService.FeatureOrigin.forParent(user, card.getIsOfficial(), card.getSrd()));
        if (resolvedFeatures != null) {
            card.setFeatures(resolvedFeatures);
        }

        Set<Question> resolvedQuestions = questionService.resolveQuestions(
                request.getQuestionIds(), request.getQuestions());
        if (resolvedQuestions != null) {
            card.setQuestions(resolvedQuestions);
        }

        return card;
    }

    /**
     * Converts a TransformationCard entity to TransformationCardResponse DTO.
     * <p>
     * This is the universal funnel — every caller (list, single-get, and any sibling type
     * expanding its transformation cards) must route through this method rather than building a
     * {@link TransformationCardResponse} directly, so a gated non-SRD card redacts to a stub
     * instead of leaking through an expand.
     * </p>
     *
     * @param card The transformation card entity
     * @param expand Set of relationships to expand
     * @return TransformationCardResponse DTO, or a redacted stub if the caller may not view it
     */
    public TransformationCardResponse toResponse(TransformationCard card, Set<String> expand) {
        if (!contentAccessService.mayView(card.getIsOfficial(), card.getSrd())) {
            return ContentRedaction.stub(TransformationCardResponse::new, card.getId(),
                    card.getExpansion() != null ? card.getExpansion().getName() : null);
        }

        TransformationCardResponse.TransformationCardResponseBuilder builder = TransformationCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .isOfficial(card.getIsOfficial())
                .srd(card.getSrd())
                .expansionId(card.getExpansion().getId())
                .createdAt(card.getCreatedAt())
                .lastModifiedAt(card.getLastModifiedAt())
                .deletedAt(card.getDeletedAt());

        if (card.getFeatures() != null) {
            builder.featureIds(card.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

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

        if (ExpandUtil.shouldExpand(expand, "features") && card.getFeatures() != null) {
            builder.features(card.getFeatures().stream()
                    .map(feature -> featureService.toResponse(feature, Set.of()))
                    .collect(Collectors.toList()));
        }

        if (card.getQuestions() != null) {
            builder.questionIds(card.getQuestions().stream()
                    .map(Question::getId)
                    .collect(Collectors.toList()));
        }

        // Routed through QuestionService#toResponse (not built inline) so a gated non-SRD
        // question redacts to a stub here too, instead of leaking through this expand.
        if (ExpandUtil.shouldExpand(expand, "questions") && card.getQuestions() != null) {
            builder.questions(card.getQuestions().stream()
                    .map(question -> questionService.toResponse(question, Set.of()))
                    .collect(Collectors.toList()));
        }

        return builder.build();
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
