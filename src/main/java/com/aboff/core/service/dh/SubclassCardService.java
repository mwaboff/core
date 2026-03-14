package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
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

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<SubclassCard> cardPage;

        if (includeDeleted) {
            cardPage = subclassCardRepository.findAllWithFilters(expansionId, isOfficial, associatedClassId, subclassPathId, level, pageable);
        } else {
            cardPage = subclassCardRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, associatedClassId, subclassPathId, level, pageable);
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
     *
     * @param request The creation request containing card details
     * @return SubclassCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     * @throws IllegalArgumentException if subclass path resolution fails
     */
    @Transactional
    public SubclassCardResponse createSubclassCard(CreateSubclassCardRequest request) {
        log.info("Creating new subclass card with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        SubclassPath path = subclassPathService.resolvePath(
                request.getSubclassPathId(),
                request.getSubclassPath(),
                request.getAssociatedClassId(),
                request.getExpansionId());

        SubclassCard card = SubclassCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
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
        log.info("Created subclass card with id: {}", savedCard.getId());

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple subclass cards in bulk.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @Transactional
    public List<SubclassCardResponse> createSubclassCardsBulk(List<CreateSubclassCardRequest> requests) {
        log.info("Creating {} subclass cards in bulk", requests.size());

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

                    SubclassCard card = SubclassCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
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
        log.info("Created {} subclass cards in bulk", savedCards.size());

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing subclass card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return SubclassCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     * @throws IllegalArgumentException if subclass path resolution fails
     */
    @Transactional
    public SubclassCardResponse updateSubclassCard(Long id, UpdateSubclassCardRequest request) {
        log.info("Updating subclass card with id: {}", id);

        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        SubclassPath path = subclassPathService.resolvePath(
                request.getSubclassPathId(),
                request.getSubclassPath(),
                request.getAssociatedClassId(),
                request.getExpansionId());

        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setExpansion(expansion);
        card.setIsOfficial(request.getIsOfficial());
        card.setBackgroundImageUrl(request.getBackgroundImageUrl());
        card.setSubclassPath(path);
        card.setLevel(request.getLevel());

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
        log.info("Updated subclass card with id: {}", updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a subclass card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteSubclassCard(Long id) {
        log.info("Soft deleting subclass card with id: {}", id);

        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        card.softDelete();
        subclassCardRepository.save(card);

        log.info("Soft deleted subclass card with id: {}", id);
    }

    /**
     * Restores a soft-deleted subclass card.
     *
     * @param id The card ID to restore
     * @return SubclassCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public SubclassCardResponse restoreSubclassCard(Long id) {
        log.info("Restoring subclass card with id: {}", id);

        SubclassCard card = subclassCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("SubclassCard with id " + id + " is not deleted");
        }

        card.restore();
        SubclassCard restoredCard = subclassCardRepository.save(card);

        log.info("Restored subclass card with id: {}", id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Converts a SubclassCard entity to SubclassCardResponse DTO.
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return SubclassCardResponse DTO
     */
    public SubclassCardResponse toResponse(SubclassCard card, Set<String> expand) {
        SubclassCardResponse.SubclassCardResponseBuilder builder = SubclassCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .expansionName(card.getExpansion().getName())
                .isOfficial(card.getIsOfficial())
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

        // Expand subclass path if requested
        if (expand.contains("subclassPath")) {
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
}
