package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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

import java.util.HashSet;
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
    private final FeatureRepository featureRepository;
    private final ClassRepository classRepository;
    private final DomainRepository domainRepository;

    /**
     * Retrieves a paginated list of subclass cards.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted cards
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param associatedClassId Optional filter for associated class ID
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
            SubclassLevel level,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<SubclassCard> cardPage;

        if (includeDeleted) {
            cardPage = subclassCardRepository.findAllWithFilters(expansionId, isOfficial, associatedClassId, level, pageable);
        } else {
            cardPage = subclassCardRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, associatedClassId, level, pageable);
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
     */
    @Transactional
    public SubclassCardResponse createSubclassCard(CreateSubclassCardRequest request) {
        log.info("Creating new subclass card with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Class not found with id: " + request.getAssociatedClassId()));

        SubclassCard card = SubclassCard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .associatedClass(associatedClass)
                .level(request.getLevel())
                .spellcastingTrait(request.getSpellcastingTrait())
                .build();

        // Set features if provided
        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
            card.setFeatures(features);
        }

        // Set associated domains if provided
        if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
            Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
            card.setAssociatedDomains(domains);
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

                    Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Class not found with id: " + request.getAssociatedClassId()));

                    SubclassCard card = SubclassCard.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .backgroundImageUrl(request.getBackgroundImageUrl())
                            .associatedClass(associatedClass)
                            .level(request.getLevel())
                            .spellcastingTrait(request.getSpellcastingTrait())
                            .build();

                    if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
                        Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                        card.setFeatures(features);
                    }

                    if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
                        Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                        card.setAssociatedDomains(domains);
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
     */
    @Transactional
    public SubclassCardResponse updateSubclassCard(Long id, UpdateSubclassCardRequest request) {
        log.info("Updating subclass card with id: {}", id);

        SubclassCard card = subclassCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassCard not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Class not found with id: " + request.getAssociatedClassId()));

        card.setName(request.getName());
        card.setDescription(request.getDescription());
        card.setExpansion(expansion);
        card.setIsOfficial(request.getIsOfficial());
        card.setBackgroundImageUrl(request.getBackgroundImageUrl());
        card.setAssociatedClass(associatedClass);
        card.setLevel(request.getLevel());
        card.setSpellcastingTrait(request.getSpellcastingTrait());

        // Update features
        if (request.getFeatureIds() != null) {
            if (request.getFeatureIds().isEmpty()) {
                card.setFeatures(new HashSet<>());
            } else {
                Set<Feature> features = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getFeatureIds()));
                card.setFeatures(features);
            }
        }

        // Update associated domains
        if (request.getAssociatedDomainIds() != null) {
            if (request.getAssociatedDomainIds().isEmpty()) {
                card.setAssociatedDomains(new HashSet<>());
            } else {
                Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                card.setAssociatedDomains(domains);
            }
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
    private SubclassCardResponse toResponse(SubclassCard card, Set<String> expand) {
        SubclassCardResponse.SubclassCardResponseBuilder builder = SubclassCardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .description(card.getDescription())
                .cardType(card.getCardType())
                .expansionId(card.getExpansion().getId())
                .isOfficial(card.getIsOfficial())
                .backgroundImageUrl(card.getBackgroundImageUrl())
                .associatedClassId(card.getAssociatedClass().getId())
                .level(card.getLevel())
                .createdAt(card.getCreatedAt())
                .lastModifiedAt(card.getLastModifiedAt())
                .deletedAt(card.getDeletedAt());

        // Add spellcasting trait info if present
        if (card.getSpellcastingTrait() != null) {
            builder.spellcastingTrait(SubclassCardResponse.TraitInfo.builder()
                    .trait(card.getSpellcastingTrait())
                    .description(card.getSpellcastingTrait().getDescription())
                    .examples(card.getSpellcastingTrait().getExamples())
                    .build());
        }

        // Always include feature IDs
        if (card.getFeatures() != null) {
            builder.featureIds(card.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        // Always include associated domain IDs
        if (card.getAssociatedDomains() != null) {
            builder.associatedDomainIds(card.getAssociatedDomains().stream()
                    .map(Domain::getId)
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

        // Expand associated class if requested (but avoid infinite recursion - don't expand nested relationships)
        if (expand.contains("associatedClass")) {
            Class clazz = card.getAssociatedClass();
            builder.associatedClass(ClassResponse.builder()
                    .id(clazz.getId())
                    .name(clazz.getName())
                    .description(clazz.getDescription())
                    .expansionId(clazz.getExpansion().getId())
                    .startingClassItems(clazz.getStartingClassItems())
                    .startingEvasion(clazz.getStartingEvasion())
                    .startingHitPoints(clazz.getStartingHitPoints())
                    .createdAt(clazz.getCreatedAt())
                    .lastModifiedAt(clazz.getLastModifiedAt())
                    .deletedAt(clazz.getDeletedAt())
                    .build());
        }

        // Expand associated domains if requested
        if (expand.contains("associatedDomains") && card.getAssociatedDomains() != null) {
            builder.associatedDomains(card.getAssociatedDomains().stream()
                    .map(domain -> DomainResponse.builder()
                            .id(domain.getId())
                            .name(domain.getName())
                            .iconUrl(domain.getIconUrl())
                            .description(domain.getDescription())
                            .expansionId(domain.getExpansion().getId())
                            .createdAt(domain.getCreatedAt())
                            .lastModifiedAt(domain.getLastModifiedAt())
                            .deletedAt(domain.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
