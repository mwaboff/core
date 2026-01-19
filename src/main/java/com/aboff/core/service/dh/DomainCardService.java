package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainCardRequest;
import com.aboff.core.model.dto.dh.response.DomainCardResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.repository.dh.DomainCardRepository;
import com.aboff.core.repository.dh.DomainRepository;
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
 * Service for managing DomainCard entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainCardService {

    private final DomainCardRepository domainCardRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureRepository featureRepository;
    private final DomainRepository domainRepository;

    /**
     * Retrieves a paginated list of domain cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedDomainId Optional filter for associated domain ID
     * @param type Optional filter for domain card type
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing domain cards
     */
    @Transactional(readOnly = true)
    public PagedResponse<DomainCardResponse> getAllDomainCards(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Long associatedDomainId,
            DomainCardType type,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<DomainCard> cardPage;

        if (includeDeleted) {
            cardPage = domainCardRepository.findAllWithFilters(expansionId, isOfficial, associatedDomainId, type, pageable);
        } else {
            cardPage = domainCardRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, associatedDomainId, type, pageable);
        }

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<DomainCardResponse>builder()
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
     * Retrieves a single domain card by ID.
     *
     * @param id The card ID
     * @param expand Comma-separated list of relationships to expand
     * @return DomainCardResponse containing the card details
     * @throws EntityNotFoundException if the card is not found or is deleted
     */
    @Transactional(readOnly = true)
    public DomainCardResponse getDomainCardById(Long id, String expand) {
        DomainCard card = domainCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + id));

        Set<String> expandSet = parseExpand(expand);
        return toResponse(card, expandSet);
    }

    /**
     * Creates a new domain card.
     *
     * @param request The creation request containing card details
     * @return DomainCardResponse containing the created card
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public DomainCardResponse createDomainCard(CreateDomainCardRequest request) {
        log.info("Creating new domain card with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Domain associatedDomain = domainRepository.findByIdAndDeletedAtIsNull(request.getAssociatedDomainId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Domain not found with id: " + request.getAssociatedDomainId()));

        DomainCard card = DomainCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .associatedDomain(associatedDomain)
                .level(request.getLevel())
                .recallCost(request.getRecallCost())
                .type(request.getType())
                .build();

        // Set features if provided
        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
            card.setFeatures(features);
        }

        DomainCard savedCard = domainCardRepository.save(card);
        log.info("Created domain card with id: {}", savedCard.getId());

        return toResponse(savedCard, Set.of());
    }

    /**
     * Creates multiple domain cards in bulk.
     *
     * @param requests List of creation requests
     * @return List of created card responses
     */
    @Transactional
    public List<DomainCardResponse> createDomainCardsBulk(List<CreateDomainCardRequest> requests) {
        log.info("Creating {} domain cards in bulk", requests.size());

        List<DomainCard> cards = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Domain associatedDomain = domainRepository.findByIdAndDeletedAtIsNull(request.getAssociatedDomainId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Domain not found with id: " + request.getAssociatedDomainId()));

                    DomainCard card = DomainCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .backgroundImageUrl(request.getBackgroundImageUrl())
                            .associatedDomain(associatedDomain)
                            .level(request.getLevel())
                            .recallCost(request.getRecallCost())
                            .type(request.getType())
                            .build();

                    if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
                        Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                        card.setFeatures(features);
                    }

                    return card;
                })
                .toList();

        List<DomainCard> savedCards = domainCardRepository.saveAll(cards);
        log.info("Created {} domain cards in bulk", savedCards.size());

        return savedCards.stream()
                .map(card -> toResponse(card, Set.of()))
                .toList();
    }

    /**
     * Updates an existing domain card.
     *
     * @param id The card ID to update
     * @param request The update request containing new card details
     * @return DomainCardResponse containing the updated card
     * @throws EntityNotFoundException if the card or referenced entities are not found
     */
    @Transactional
    public DomainCardResponse updateDomainCard(Long id, UpdateDomainCardRequest request) {
        log.info("Updating domain card with id: {}", id);

        DomainCard card = domainCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Domain associatedDomain = domainRepository.findByIdAndDeletedAtIsNull(request.getAssociatedDomainId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Domain not found with id: " + request.getAssociatedDomainId()));

        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setExpansion(expansion);
        card.setIsOfficial(request.getIsOfficial());
        card.setBackgroundImageUrl(request.getBackgroundImageUrl());
        card.setAssociatedDomain(associatedDomain);
        card.setLevel(request.getLevel());
        card.setRecallCost(request.getRecallCost());
        card.setType(request.getType());

        // Update features
        if (request.getFeatureIds() != null) {
            if (request.getFeatureIds().isEmpty()) {
                card.setFeatures(new HashSet<>());
            } else {
                Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                card.setFeatures(features);
            }
        }

        DomainCard updatedCard = domainCardRepository.save(card);
        log.info("Updated domain card with id: {}", updatedCard.getId());

        return toResponse(updatedCard, Set.of());
    }

    /**
     * Soft deletes a domain card by setting its deletedAt timestamp.
     *
     * @param id The card ID to delete
     * @throws EntityNotFoundException if the card is not found or is already deleted
     */
    @Transactional
    public void deleteDomainCard(Long id) {
        log.info("Soft deleting domain card with id: {}", id);

        DomainCard card = domainCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + id));

        card.softDelete();
        domainCardRepository.save(card);

        log.info("Soft deleted domain card with id: {}", id);
    }

    /**
     * Restores a soft-deleted domain card.
     *
     * @param id The card ID to restore
     * @return DomainCardResponse containing the restored card
     * @throws EntityNotFoundException if the card is not found
     * @throws IllegalStateException if the card is not deleted
     */
    @Transactional
    public DomainCardResponse restoreDomainCard(Long id) {
        log.info("Restoring domain card with id: {}", id);

        DomainCard card = domainCardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DomainCard not found with id: " + id));

        if (!card.isDeleted()) {
            throw new IllegalStateException("DomainCard with id " + id + " is not deleted");
        }

        card.restore();
        DomainCard restoredCard = domainCardRepository.save(card);

        log.info("Restored domain card with id: {}", id);

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
     * Converts a DomainCard entity to DomainCardResponse DTO.
     *
     * @param card The card entity
     * @param expand Set of relationships to expand
     * @return DomainCardResponse DTO
     */
    private DomainCardResponse toResponse(DomainCard card, Set<String> expand) {
        DomainCardResponse.DomainCardResponseBuilder builder = DomainCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .isOfficial(card.getIsOfficial())
                .backgroundImageUrl(card.getBackgroundImageUrl())
                .associatedDomainId(card.getAssociatedDomain().getId())
                .level(card.getLevel())
                .recallCost(card.getRecallCost())
                .type(card.getType())
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

        // Expand associated domain if requested
        if (expand.contains("associatedDomain")) {
            Domain domain = card.getAssociatedDomain();
            builder.associatedDomain(DomainResponse.builder()
                    .id(domain.getId())
                    .name(domain.getName())
                    .iconUrl(domain.getIconUrl())
                    .description(domain.getDescription())
                    .expansionId(domain.getExpansion().getId())
                    .createdAt(domain.getCreatedAt())
                    .lastModifiedAt(domain.getLastModifiedAt())
                    .deletedAt(domain.getDeletedAt())
                    .build());
        }

        return builder.build();
    }
}
