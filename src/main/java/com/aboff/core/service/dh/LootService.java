package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.LootRepository;
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

/**
 * Service for managing Loot entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LootService {

    private final LootRepository lootRepository;
    private final ExpansionRepository expansionRepository;

    /**
     * Retrieves a paginated list of loot items.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted loot
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing loot items
     */
    @Transactional(readOnly = true)
    public PagedResponse<LootResponse> getAllLoot(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Loot> lootPage;

        if (includeDeleted) {
            lootPage = lootRepository.findAllWithFilters(expansionId, isOfficial, pageable);
        } else {
            lootPage = lootRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, pageable);
        }

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<LootResponse>builder()
                .content(lootPage.getContent().stream()
                        .map(loot -> toResponse(loot, expandSet))
                        .toList())
                .totalElements(lootPage.getTotalElements())
                .totalPages(lootPage.getTotalPages())
                .currentPage(lootPage.getNumber())
                .pageSize(lootPage.getSize())
                .build();
    }

    /**
     * Retrieves a single loot item by ID.
     *
     * @param id The loot ID
     * @param expand Comma-separated list of relationships to expand
     * @return LootResponse containing the loot details
     * @throws EntityNotFoundException if the loot is not found or is deleted
     */
    @Transactional(readOnly = true)
    public LootResponse getLootById(Long id, String expand) {
        Loot loot = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        Set<String> expandSet = parseExpand(expand);
        return toResponse(loot, expandSet);
    }

    /**
     * Creates a new loot item.
     *
     * @param request The creation request containing loot details
     * @return LootResponse containing the created loot
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public LootResponse createLoot(CreateLootRequest request) {
        log.info("Creating new loot with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Loot loot = Loot.builder()
                .name(request.getName())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .description(request.getDescription())
                .build();

        if (request.getOriginalLootId() != null) {
            Loot originalLoot = lootRepository.findByIdAndDeletedAtIsNull(request.getOriginalLootId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original loot not found with id: " + request.getOriginalLootId()));
            loot.setOriginalLoot(originalLoot);
        }

        Loot savedLoot = lootRepository.save(loot);
        log.info("Created loot with id: {}", savedLoot.getId());

        return toResponse(savedLoot, Set.of());
    }

    /**
     * Creates multiple loot items in bulk.
     *
     * @param requests List of creation requests
     * @return List of created loot responses
     */
    @Transactional
    public List<LootResponse> createLootBulk(List<CreateLootRequest> requests) {
        log.info("Creating {} loot items in bulk", requests.size());

        List<Loot> lootItems = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Loot loot = Loot.builder()
                            .name(request.getName())
                            .expansion(expansion)
                            .isOfficial(request.getIsOfficial())
                            .description(request.getDescription())
                            .build();

                    if (request.getOriginalLootId() != null) {
                        Loot originalLoot = lootRepository.findByIdAndDeletedAtIsNull(request.getOriginalLootId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Original loot not found with id: " + request.getOriginalLootId()));
                        loot.setOriginalLoot(originalLoot);
                    }

                    return loot;
                })
                .toList();

        List<Loot> savedLoot = lootRepository.saveAll(lootItems);
        log.info("Created {} loot items in bulk", savedLoot.size());

        return savedLoot.stream()
                .map(loot -> toResponse(loot, Set.of()))
                .toList();
    }

    /**
     * Updates an existing loot item.
     *
     * @param id The loot ID to update
     * @param request The update request containing new loot details
     * @return LootResponse containing the updated loot
     * @throws EntityNotFoundException if the loot or referenced entities are not found
     */
    @Transactional
    public LootResponse updateLoot(Long id, UpdateLootRequest request) {
        log.info("Updating loot with id: {}", id);

        Loot loot = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        loot.setName(request.getName());
        loot.setExpansion(expansion);
        loot.setIsOfficial(request.getIsOfficial());
        loot.setDescription(request.getDescription());

        if (request.getOriginalLootId() != null) {
            Loot originalLoot = lootRepository.findByIdAndDeletedAtIsNull(request.getOriginalLootId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original loot not found with id: " + request.getOriginalLootId()));
            loot.setOriginalLoot(originalLoot);
        } else {
            loot.setOriginalLoot(null);
        }

        Loot updatedLoot = lootRepository.save(loot);
        log.info("Updated loot with id: {}", updatedLoot.getId());

        return toResponse(updatedLoot, Set.of());
    }

    /**
     * Soft deletes a loot item by setting its deletedAt timestamp.
     *
     * @param id The loot ID to delete
     * @throws EntityNotFoundException if the loot is not found or is already deleted
     */
    @Transactional
    public void deleteLoot(Long id) {
        log.info("Soft deleting loot with id: {}", id);

        Loot loot = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        loot.softDelete();
        lootRepository.save(loot);

        log.info("Soft deleted loot with id: {}", id);
    }

    /**
     * Restores a soft-deleted loot item.
     *
     * @param id The loot ID to restore
     * @return LootResponse containing the restored loot
     * @throws EntityNotFoundException if the loot is not found
     * @throws IllegalStateException if the loot is not deleted
     */
    @Transactional
    public LootResponse restoreLoot(Long id) {
        log.info("Restoring loot with id: {}", id);

        Loot loot = lootRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        if (!loot.isDeleted()) {
            throw new IllegalStateException("Loot with id " + id + " is not deleted");
        }

        loot.restore();
        Loot restoredLoot = lootRepository.save(loot);

        log.info("Restored loot with id: {}", id);

        return toResponse(restoredLoot, Set.of());
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
     * Converts a Loot entity to LootResponse DTO.
     *
     * @param loot The loot entity
     * @param expand Set of relationships to expand
     * @return LootResponse DTO
     */
    private LootResponse toResponse(Loot loot, Set<String> expand) {
        LootResponse.LootResponseBuilder builder = LootResponse.builder()
                .id(loot.getId())
                .name(loot.getName())
                .expansionId(loot.getExpansion().getId())
                .isOfficial(loot.getIsOfficial())
                .description(loot.getDescription())
                .createdAt(loot.getCreatedAt())
                .lastModifiedAt(loot.getLastModifiedAt())
                .deletedAt(loot.getDeletedAt());

        if (loot.getOriginalLoot() != null) {
            builder.originalLootId(loot.getOriginalLoot().getId());
        }

        if (expand.contains("expansion")) {
            Expansion expansion = loot.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (expand.contains("originalLoot") && loot.getOriginalLoot() != null) {
            builder.originalLoot(toResponse(loot.getOriginalLoot(), Set.of()));
        }

        return builder.build();
    }
}
