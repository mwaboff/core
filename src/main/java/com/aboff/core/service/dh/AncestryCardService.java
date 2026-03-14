package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.dto.dh.response.AncestryCardResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.repository.dh.AncestryCardRepository;
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

import com.aboff.core.util.ExpandUtil;

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
    private final FeatureService featureService;
    private final CardCostTagService cardCostTagService;

    /**
     * Retrieves a paginated list of ancestry cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
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
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<AncestryCard> cardPage;

        if (includeDeleted) {
            cardPage = ancestryCardRepository.findAllWithFilters(expansionId, isOfficial, pageable);
        } else {
            cardPage = ancestryCardRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, pageable);
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
     * @return AncestryCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public AncestryCardResponse createAncestryCard(CreateAncestryCardRequest request) {
        log.info("Creating new ancestry card with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        AncestryCard card = AncestryCard.builder()
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

        AncestryCard savedCard = ancestryCardRepository.save(card);
        log.info("Created ancestry card with id: {}", savedCard.getId());

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple ancestry cards in bulk.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @Transactional
    public List<AncestryCardResponse> createAncestryCardsBulk(List<CreateAncestryCardRequest> requests) {
        log.info("Creating {} ancestry cards in bulk", requests.size());

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
        log.info("Created {} ancestry cards in bulk", savedCards.size());

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing ancestry card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return AncestryCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     */
    @Transactional
    public AncestryCardResponse updateAncestryCard(Long id, UpdateAncestryCardRequest request) {
        log.info("Updating ancestry card with id: {}", id);

        AncestryCard card = ancestryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setExpansion(expansion);
        card.setIsOfficial(request.getIsOfficial());
        card.setBackgroundImageUrl(request.getBackgroundImageUrl());

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
        log.info("Updated ancestry card with id: {}", updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes an ancestry card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteAncestryCard(Long id) {
        log.info("Soft deleting ancestry card with id: {}", id);

        AncestryCard card = ancestryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        card.softDelete();
        ancestryCardRepository.save(card);

        log.info("Soft deleted ancestry card with id: {}", id);
    }

    /**
     * Restores a soft-deleted ancestry card.
     *
     * @param id The card ID to restore
     * @return AncestryCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public AncestryCardResponse restoreAncestryCard(Long id) {
        log.info("Restoring ancestry card with id: {}", id);

        AncestryCard card = ancestryCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AncestryCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("AncestryCard with id " + id + " is not deleted");
        }

        card.restore();
        AncestryCard restoredCard = ancestryCardRepository.save(card);

        log.info("Restored ancestry card with id: {}", id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Converts an AncestryCard entity to AncestryCardResponse DTO.
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return AncestryCardResponse DTO
     */
    public AncestryCardResponse toResponse(AncestryCard card, Set<String> expand) {
        AncestryCardResponse.AncestryCardResponseBuilder builder = AncestryCardResponse.builder()
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
        if (expand.contains("expansion")) {
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
        if (expand.contains("features") && card.getFeatures() != null) {
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
        if (expand.contains("costTags") && card.getCostTags() != null) {
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
