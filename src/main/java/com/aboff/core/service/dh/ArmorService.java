package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;

import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.repository.dh.ArmorRepository;
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
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Armor entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArmorService {

    private final ArmorRepository armorRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of armors.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted armors
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for armor tier (1–4)
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing armors
     */
    @Transactional(readOnly = true)
    public PagedResponse<ArmorResponse> getAllArmors(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Integer tier,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Armor> armorPage;

        if (includeDeleted) {
            armorPage = armorRepository.findAllWithFilters(expansionId, isOfficial, tier, pageable);
        } else {
            armorPage = armorRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, tier, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<ArmorResponse>builder()
                .content(armorPage.getContent().stream()
                        .map(armor -> toResponse(armor, expandSet))
                        .toList())
                .totalElements(armorPage.getTotalElements())
                .totalPages(armorPage.getTotalPages())
                .currentPage(armorPage.getNumber())
                .pageSize(armorPage.getSize())
                .build();
    }

    /**
     * Retrieves a single armor by ID.
     *
     * @param id The armor ID
     * @param expand Comma-separated list of relationships to expand
     * @return ArmorResponse containing the armor details
     * @throws EntityNotFoundException if the armor is not found or is deleted
     */
    @Transactional(readOnly = true)
    public ArmorResponse getArmorById(Long id, String expand) {
        Armor armor = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(armor, expandSet);
    }

    /**
     * Creates a new armor.
     *
     * @param request The creation request containing armor details
     * @return ArmorResponse containing the created armor
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public ArmorResponse createArmor(CreateArmorRequest request) {
        log.info("Creating new armor with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Armor armor = Armor.builder()
                .name(request.getName())
                .expansion(expansion)
                .tier(request.getTier())
                .isOfficial(request.getIsOfficial())
                .baseMajorThreshold(request.getBaseMajorThreshold())
                .baseSevereThreshold(request.getBaseSevereThreshold())
                .baseScore(request.getBaseScore())
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            armor.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalArmorId() != null) {
            Armor originalArmor = armorRepository.findByIdAndDeletedAtIsNull(request.getOriginalArmorId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original armor not found with id: " + request.getOriginalArmorId()));
            armor.setOriginalArmor(originalArmor);
        }

        Armor savedArmor = armorRepository.save(armor);
        log.info("Created armor with id: {}", savedArmor.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedArmor, EntityChangeEvent.ChangeType.CREATED));

        return toResponse(savedArmor, Set.of());
    }

    /**
     * Creates multiple armors in bulk.
     *
     * @param requests List of creation requests
     * @return List of created armor responses
     */
    @Transactional
    public List<ArmorResponse> createArmorsBulk(List<CreateArmorRequest> requests) {
        log.info("Creating {} armors in bulk", requests.size());

        List<Armor> armors = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Armor armor = Armor.builder()
                            .name(request.getName())
                            .expansion(expansion)
                            .tier(request.getTier())
                            .isOfficial(request.getIsOfficial())
                            .baseMajorThreshold(request.getBaseMajorThreshold())
                            .baseSevereThreshold(request.getBaseSevereThreshold())
                            .baseScore(request.getBaseScore())
                            .build();

                    Set<Feature> bulkResolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
                    if (bulkResolvedFeatures != null) {
                        armor.setFeatures(bulkResolvedFeatures);
                    }

                    if (request.getOriginalArmorId() != null) {
                        Armor originalArmor = armorRepository.findByIdAndDeletedAtIsNull(request.getOriginalArmorId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Original armor not found with id: " + request.getOriginalArmorId()));
                        armor.setOriginalArmor(originalArmor);
                    }

                    return armor;
                })
                .toList();

        List<Armor> savedArmors = armorRepository.saveAll(armors);
        log.info("Created {} armors in bulk", savedArmors.size());
        savedArmors.forEach(a -> eventPublisher.publishEvent(new EntityChangeEvent(this, a, EntityChangeEvent.ChangeType.CREATED)));

        return savedArmors.stream()
                .map(armor -> toResponse(armor, Set.of()))
                .toList();
    }

    /**
     * Updates an existing armor.
     *
     * @param id The armor ID to update
     * @param request The update request containing new armor details
     * @return ArmorResponse containing the updated armor
     * @throws EntityNotFoundException if the armor or referenced entities are not found
     */
    @Transactional
    public ArmorResponse updateArmor(Long id, UpdateArmorRequest request) {
        log.info("Updating armor with id: {}", id);

        Armor armor = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            armor.setName(request.getName());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            armor.setExpansion(expansion);
        }
        if (request.getTier() != null) {
            armor.setTier(request.getTier());
        }
        if (request.getIsOfficial() != null) {
            armor.setIsOfficial(request.getIsOfficial());
        }
        if (request.getBaseMajorThreshold() != null) {
            armor.setBaseMajorThreshold(request.getBaseMajorThreshold());
        }
        if (request.getBaseSevereThreshold() != null) {
            armor.setBaseSevereThreshold(request.getBaseSevereThreshold());
        }
        if (request.getBaseScore() != null) {
            armor.setBaseScore(request.getBaseScore());
        }

        if (request.getFeatureIds() != null || request.getFeatures() != null) {
            Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
            if (resolvedUpdateFeatures != null) {
                armor.setFeatures(resolvedUpdateFeatures);
            }
        }

        if (request.getOriginalArmorId() != null) {
            Armor originalArmor = armorRepository.findByIdAndDeletedAtIsNull(request.getOriginalArmorId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original armor not found with id: " + request.getOriginalArmorId()));
            armor.setOriginalArmor(originalArmor);
        }

        Armor updatedArmor = armorRepository.save(armor);
        log.info("Updated armor with id: {}", updatedArmor.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedArmor, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedArmor, Set.of());
    }

    /**
     * Soft deletes an armor by setting its deletedAt timestamp.
     *
     * @param id The armor ID to delete
     * @throws EntityNotFoundException if the armor is not found or is already deleted
     */
    @Transactional
    public void deleteArmor(Long id) {
        log.info("Soft deleting armor with id: {}", id);

        Armor armor = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        armor.softDelete();
        armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, armor, EntityChangeEvent.ChangeType.SOFT_DELETED));

        log.info("Soft deleted armor with id: {}", id);
    }

    /**
     * Restores a soft-deleted armor.
     *
     * @param id The armor ID to restore
     * @return ArmorResponse containing the restored armor
     * @throws EntityNotFoundException if the armor is not found
     * @throws IllegalStateException if the armor is not deleted
     */
    @Transactional
    public ArmorResponse restoreArmor(Long id) {
        log.info("Restoring armor with id: {}", id);

        Armor armor = armorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        if (!armor.isDeleted()) {
            throw new IllegalStateException("Armor with id " + id + " is not deleted");
        }

        armor.restore();
        Armor restoredArmor = armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredArmor, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored armor with id: {}", id);

        return toResponse(restoredArmor, Set.of());
    }

    /**
     * Converts an Armor entity to ArmorResponse DTO.
     *
     * @param armor The armor entity
     * @param expand Set of relationships to expand
     * @return ArmorResponse DTO
     */
    public ArmorResponse toResponse(Armor armor, Set<String> expand) {
        ArmorResponse.ArmorResponseBuilder builder = ArmorResponse.builder()
                .id(armor.getId())
                .name(armor.getName())
                .expansionId(armor.getExpansion().getId())
                .tier(armor.getTier())
                .isOfficial(armor.getIsOfficial())
                .baseMajorThreshold(armor.getBaseMajorThreshold())
                .baseSevereThreshold(armor.getBaseSevereThreshold())
                .baseScore(armor.getBaseScore())
                .createdAt(armor.getCreatedAt())
                .lastModifiedAt(armor.getLastModifiedAt())
                .deletedAt(armor.getDeletedAt());

        if (armor.getFeatures() != null && !armor.getFeatures().isEmpty()) {
            builder.featureIds(armor.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (armor.getOriginalArmor() != null) {
            builder.originalArmorId(armor.getOriginalArmor().getId());
        }

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = armor.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "features") && armor.getFeatures() != null && !armor.getFeatures().isEmpty()) {
            builder.features(armor.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalArmor") && armor.getOriginalArmor() != null) {
            builder.originalArmor(toResponse(armor.getOriginalArmor(), Set.of()));
        }

        return builder.build();
    }
}
