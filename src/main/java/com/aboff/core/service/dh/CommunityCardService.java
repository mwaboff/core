package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.dto.dh.response.CommunityCardResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.repository.dh.CommunityCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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
    private final FeatureRepository featureRepository;

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

        Set<String> expandSet = parseExpand(expand);

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

        Set<String> expandSet = parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new community card.
     *
     * @param request The creation request containing card details
     * @return CommunityCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public CommunityCardResponse createCommunityCard(CreateCommunityCardRequest request) {
        log.info("Creating new community card with name: {}", request.getName());

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
        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
            card.setFeatures(features);
        }

        CommunityCard savedCard = communityCardRepository.save(card);
        log.info("Created community card with id: {}", savedCard.getId());

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple community cards in bulk.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @Transactional
    public List<CommunityCardResponse> createCommunityCardsBulk(List<CreateCommunityCardRequest> requests) {
        log.info("Creating {} community cards in bulk", requests.size());

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

                    if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
                        Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                        card.setFeatures(features);
                    }

                    return card;
                })
                .toList();

        List<CommunityCard> savedCards = communityCardRepository.saveAll(cards);
        log.info("Created {} community cards in bulk", savedCards.size());

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing community card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return CommunityCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     */
    @Transactional
    public CommunityCardResponse updateCommunityCard(Long id, UpdateCommunityCardRequest request) {
        log.info("Updating community card with id: {}", id);

        CommunityCard card = communityCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setExpansion(expansion);
        card.setIsOfficial(request.getIsOfficial());
        card.setBackgroundImageUrl(request.getBackgroundImageUrl());

        // Update features
        if (request.getFeatureIds() != null) {
            if (request.getFeatureIds().isEmpty()) {
                card.setFeatures(new HashSet<>());
            } else {
                Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                card.setFeatures(features);
            }
        }

        CommunityCard updatedCard = communityCardRepository.save(card);
        log.info("Updated community card with id: {}", updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a community card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteCommunityCard(Long id) {
        log.info("Soft deleting community card with id: {}", id);

        CommunityCard card = communityCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        card.softDelete();
        communityCardRepository.save(card);

        log.info("Soft deleted community card with id: {}", id);
    }

    /**
     * Restores a soft-deleted community card.
     *
     * @param id The card ID to restore
     * @return CommunityCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public CommunityCardResponse restoreCommunityCard(Long id) {
        log.info("Restoring community card with id: {}", id);

        CommunityCard card = communityCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommunityCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("CommunityCard with id " + id + " is not deleted");
        }

        card.restore();
        CommunityCard restoredCard = communityCardRepository.save(card);

        log.info("Restored community card with id: {}", id);

        return toResponse(restoredCard, Set.of());
    }

    /**
     * Parses the expand parameter into a set of relationship names.
     *
     * @param expand Comma-separated list of relationships to expand
     * @return Set of relationship names
     */
    private Set<String> parseExpand(String expand) {
        if (expand == null || expand.trim().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(List.of(expand.split(",")));
    }

    /**
     * Converts a CommunityCard entity to CommunityCardResponse DTO.
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return CommunityCardResponse DTO
     */
    private CommunityCardResponse toResponse(CommunityCard card, Set<String> expand) {
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
                    .map(feature -> FeatureResponse.builder()
                            .id(feature.getId())
                            .name(feature.getName())
                            .description(feature.getDescription())
                            .featureType(feature.getFeatureType())
                            .expansionId(feature.getExpansion().getId())
                            .createdAt(feature.getCreatedAt())
                            .lastModifiedAt(feature.getLastModifiedAt())
                            .deletedAt(feature.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
