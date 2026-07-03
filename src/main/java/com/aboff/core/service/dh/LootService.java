package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.exception.TooManyCustomItemsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.LootRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Loot entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion,
 * relationship expansion, and permission validation.
 * </p>
 * <p>
 * Permission model:
 * </p>
 * <ul>
 *   <li>Official loot: Only ADMIN+ can modify</li>
 *   <li>Non-official loot: Creator OR MODERATOR+ can modify</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LootService {

    private static final int MAX_CUSTOM_ITEMS_PER_USER = 200;

    private final LootRepository lootRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final RoleHierarchyService roleHierarchyService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of loot items.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted loot
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for loot tier (1–4)
     * @param isConsumable Optional filter for consumable status
     * @param creatorId Optional filter for the creator's user ID
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
            Integer tier,
            Boolean isConsumable,
            Long creatorId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Loot> lootPage;

        if (includeDeleted) {
            lootPage = lootRepository.findAllWithFilters(expansionId, isOfficial, tier, isConsumable, creatorId, pageable);
        } else {
            lootPage = lootRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, tier, isConsumable, creatorId, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

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

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(loot, expandSet);
    }

    /**
     * Creates a new loot item.
     *
     * @param request The creation request containing loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the created loot
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public LootResponse createLoot(CreateLootRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User creator = userDetails.getUser();

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        boolean isPrivileged = roleHierarchyService.hasRoleOrHigher(creator, Role.ADMIN);
        boolean isOfficial = isPrivileged && Boolean.TRUE.equals(request.getIsOfficial());

        if (!isOfficial && lootRepository.countByCreatedByIdAndDeletedAtIsNull(creator.getId()) >= MAX_CUSTOM_ITEMS_PER_USER) {
            throw new TooManyCustomItemsException("You have reached the maximum of " + MAX_CUSTOM_ITEMS_PER_USER + " custom loot");
        }

        Loot loot = Loot.builder()
                .name(request.getName())
                .expansion(expansion)
                .tier(request.getTier())
                .isOfficial(isOfficial)
                .createdBy(isOfficial ? null : creator)
                .isConsumable(request.getIsConsumable())
                .description(request.getDescription())
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            loot.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalLootId() != null) {
            Loot originalLoot = lootRepository.findByIdAndDeletedAtIsNull(request.getOriginalLootId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original loot not found with id: " + request.getOriginalLootId()));
            loot.setOriginalLoot(originalLoot);
        }

        Loot savedLoot = lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedLoot, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "\"" + savedLoot.getName() + "\" (loot_id: " + savedLoot.getId() + ", isOfficial: " + isOfficial
                        + ", createdBy: " + creator.getId() + ")");

        return toResponse(savedLoot, Set.of());
    }

    /**
     * Creates multiple loot items in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created loot responses
     */
    @Transactional
    public List<LootResponse> createLootBulk(List<CreateLootRequest> requests, Authentication authentication) {
        List<Loot> lootItems = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Loot loot = Loot.builder()
                            .name(request.getName())
                            .expansion(expansion)
                            .tier(request.getTier())
                            .isOfficial(request.getIsOfficial())
                            .isConsumable(request.getIsConsumable())
                            .description(request.getDescription())
                            .build();

                    Set<Feature> bulkResolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
                    if (bulkResolvedFeatures != null) {
                        loot.setFeatures(bulkResolvedFeatures);
                    }

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
        savedLoot.forEach(l -> eventPublisher.publishEvent(new EntityChangeEvent(this, l, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                savedLoot.size() + " created, 0 failed");

        return savedLoot.stream()
                .map(loot -> toResponse(loot, Set.of()))
                .toList();
    }

    /**
     * Updates an existing loot item.
     *
     * @param id The loot ID to update
     * @param request The update request containing new loot details
     * @param authentication The authentication of the current user
     * @return LootResponse containing the updated loot
     * @throws EntityNotFoundException if the loot or referenced entities are not found
     */
    @Transactional
    public LootResponse updateLoot(Long id, UpdateLootRequest request, Authentication authentication) {
        Loot loot = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        validateModifyPermission(loot, authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User caller = userDetails.getUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            loot.setName(request.getName());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            loot.setExpansion(expansion);
        }
        if (request.getTier() != null) {
            loot.setTier(request.getTier());
        }
        if (request.getIsOfficial() != null && roleHierarchyService.hasRoleOrHigher(caller, Role.ADMIN)) {
            loot.setIsOfficial(request.getIsOfficial());
        }
        if (request.getIsConsumable() != null) {
            loot.setIsConsumable(request.getIsConsumable());
        }
        if (request.getDescription() != null) {
            loot.setDescription(request.getDescription());
        }

        if (request.getFeatureIds() != null || request.getFeatures() != null) {
            Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
            if (resolvedUpdateFeatures != null) {
                loot.setFeatures(resolvedUpdateFeatures);
            }
        }

        if (request.getOriginalLootId() != null) {
            Loot originalLoot = lootRepository.findByIdAndDeletedAtIsNull(request.getOriginalLootId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original loot not found with id: " + request.getOriginalLootId()));
            loot.setOriginalLoot(originalLoot);
        }

        Loot updatedLoot = lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedLoot, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "loot_id: " + updatedLoot.getId() + ", isOfficial: " + updatedLoot.getIsOfficial()
                        + ", updatedBy: " + caller.getId());

        return toResponse(updatedLoot, Set.of());
    }

    /**
     * Soft deletes a loot item by setting its deletedAt timestamp.
     *
     * @param id The loot ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the loot is not found or is already deleted
     */
    @Transactional
    public void deleteLoot(Long id, Authentication authentication) {
        Loot loot = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        loot.softDelete();
        lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, loot, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "loot_id: " + id);
    }

    /**
     * Restores a soft-deleted loot item.
     *
     * @param id The loot ID to restore
     * @param authentication The authentication of the current user
     * @return LootResponse containing the restored loot
     * @throws EntityNotFoundException if the loot is not found
     * @throws IllegalStateException if the loot is not deleted
     */
    @Transactional
    public LootResponse restoreLoot(Long id, Authentication authentication) {
        Loot loot = lootRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        if (!loot.isDeleted()) {
            throw new IllegalStateException("Loot with id " + id + " is not deleted");
        }

        loot.restore();
        Loot restoredLoot = lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredLoot, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "loot_id: " + id);

        return toResponse(restoredLoot, Set.of());
    }

    /**
     * Validates that the authenticated user has permission to modify the loot.
     * <ul>
     *   <li>Official loot: Only ADMIN+ can modify</li>
     *   <li>Non-official loot: Creator OR MODERATOR+ can modify</li>
     * </ul>
     *
     * @param loot The loot being modified
     * @param authentication Authentication context
     * @throws InsufficientPermissionsException if user lacks permission
     */
    private void validateModifyPermission(Loot loot, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (Boolean.TRUE.equals(loot.getIsOfficial())) {
            if (!roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
                throw new InsufficientPermissionsException("Only admins can modify official loot");
            }
        } else {
            boolean isCreator = loot.getCreatedBy() != null && loot.getCreatedBy().getId().equals(user.getId());
            boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

            if (!isCreator && !isModeratorPlus) {
                throw new InsufficientPermissionsException("You do not have permission to modify this loot");
            }
        }
    }

    /**
     * Converts a Loot entity to LootResponse DTO.
     *
     * @param loot The loot entity
     * @param expand Set of relationships to expand
     * @return LootResponse DTO
     */
    public LootResponse toResponse(Loot loot, Set<String> expand) {
        LootResponse.LootResponseBuilder builder = LootResponse.builder()
                .id(loot.getId())
                .name(loot.getName())
                .expansionId(loot.getExpansion().getId())
                .tier(loot.getTier())
                .isOfficial(loot.getIsOfficial())
                .isConsumable(loot.getIsConsumable())
                .description(loot.getDescription())
                .creatorId(loot.getCreatedBy() != null ? loot.getCreatedBy().getId() : null)
                .createdAt(loot.getCreatedAt())
                .lastModifiedAt(loot.getLastModifiedAt())
                .deletedAt(loot.getDeletedAt());

        if (loot.getFeatures() != null && !loot.getFeatures().isEmpty()) {
            builder.featureIds(loot.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (loot.getOriginalLoot() != null) {
            builder.originalLootId(loot.getOriginalLoot().getId());
        }

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
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

        if (ExpandUtil.shouldExpand(expand, "features") && loot.getFeatures() != null && !loot.getFeatures().isEmpty()) {
            builder.features(loot.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalLoot") && loot.getOriginalLoot() != null) {
            builder.originalLoot(toResponse(loot.getOriginalLoot(), Set.of()));
        }

        return builder.build();
    }
}
