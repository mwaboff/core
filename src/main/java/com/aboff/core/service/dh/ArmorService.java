package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.repository.dh.ArmorRepository;
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

import com.aboff.core.util.ExpandUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final FeatureRepository featureRepository;

    /**
     * Retrieves a paginated list of armors.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted armors
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
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
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Armor> armorPage;

        if (includeDeleted) {
            armorPage = armorRepository.findAllWithFilters(expansionId, isOfficial, pageable);
        } else {
            armorPage = armorRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, pageable);
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
                .isOfficial(request.getIsOfficial())
                .baseMajorThreshold(request.getBaseMajorThreshold())
                .baseSevereThreshold(request.getBaseSevereThreshold())
                .baseScore(request.getBaseScore())
                .build();

        if (request.getFeatureId() != null) {
            Feature feature = featureRepository.findByIdAndDeletedAtIsNull(request.getFeatureId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Feature not found with id: " + request.getFeatureId()));
            armor.setFeature(feature);
        }

        if (request.getOriginalArmorId() != null) {
            Armor originalArmor = armorRepository.findByIdAndDeletedAtIsNull(request.getOriginalArmorId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original armor not found with id: " + request.getOriginalArmorId()));
            armor.setOriginalArmor(originalArmor);
        }

        Armor savedArmor = armorRepository.save(armor);
        log.info("Created armor with id: {}", savedArmor.getId());

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
                            .isOfficial(request.getIsOfficial())
                            .baseMajorThreshold(request.getBaseMajorThreshold())
                            .baseSevereThreshold(request.getBaseSevereThreshold())
                            .baseScore(request.getBaseScore())
                            .build();

                    if (request.getFeatureId() != null) {
                        Feature feature = featureRepository.findByIdAndDeletedAtIsNull(request.getFeatureId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Feature not found with id: " + request.getFeatureId()));
                        armor.setFeature(feature);
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

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        armor.setName(request.getName());
        armor.setExpansion(expansion);
        armor.setIsOfficial(request.getIsOfficial());
        armor.setBaseMajorThreshold(request.getBaseMajorThreshold());
        armor.setBaseSevereThreshold(request.getBaseSevereThreshold());
        armor.setBaseScore(request.getBaseScore());

        if (request.getFeatureId() != null) {
            Feature feature = featureRepository.findByIdAndDeletedAtIsNull(request.getFeatureId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Feature not found with id: " + request.getFeatureId()));
            armor.setFeature(feature);
        } else {
            armor.setFeature(null);
        }

        if (request.getOriginalArmorId() != null) {
            Armor originalArmor = armorRepository.findByIdAndDeletedAtIsNull(request.getOriginalArmorId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original armor not found with id: " + request.getOriginalArmorId()));
            armor.setOriginalArmor(originalArmor);
        } else {
            armor.setOriginalArmor(null);
        }

        Armor updatedArmor = armorRepository.save(armor);
        log.info("Updated armor with id: {}", updatedArmor.getId());

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
    private ArmorResponse toResponse(Armor armor, Set<String> expand) {
        ArmorResponse.ArmorResponseBuilder builder = ArmorResponse.builder()
                .id(armor.getId())
                .name(armor.getName())
                .expansionId(armor.getExpansion().getId())
                .isOfficial(armor.getIsOfficial())
                .baseMajorThreshold(armor.getBaseMajorThreshold())
                .baseSevereThreshold(armor.getBaseSevereThreshold())
                .baseScore(armor.getBaseScore())
                .createdAt(armor.getCreatedAt())
                .lastModifiedAt(armor.getLastModifiedAt())
                .deletedAt(armor.getDeletedAt());

        if (armor.getFeature() != null) {
            builder.featureId(armor.getFeature().getId());
        }

        if (armor.getOriginalArmor() != null) {
            builder.originalArmorId(armor.getOriginalArmor().getId());
        }

        if (expand.contains("expansion")) {
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

        if (expand.contains("feature") && armor.getFeature() != null) {
            Feature feature = armor.getFeature();
            builder.feature(FeatureResponse.builder()
                    .id(feature.getId())
                    .name(feature.getName())
                    .description(feature.getDescription())
                    .featureType(feature.getFeatureType())
                    .expansionId(feature.getExpansion().getId())
                    .createdAt(feature.getCreatedAt())
                    .lastModifiedAt(feature.getLastModifiedAt())
                    .deletedAt(feature.getDeletedAt())
                    .build());
        }

        if (expand.contains("originalArmor") && armor.getOriginalArmor() != null) {
            builder.originalArmor(toResponse(armor.getOriginalArmor(), Set.of()));
        }

        return builder.build();
    }
}
