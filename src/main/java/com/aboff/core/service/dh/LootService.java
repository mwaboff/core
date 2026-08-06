package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCustomLootRequest;
import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.ItemSort;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.LootRepository;
import com.aboff.core.service.AuditLogger;
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
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LootService {

    private final LootRepository lootRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ItemAccessService itemAccessService;

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
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param sort Requested ordering; defaults to {@link ItemSort#ID}
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The current authentication, used to resolve what the caller may see
     * @return Paginated response containing loot items
     * @throws com.aboff.core.exception.InsufficientPermissionsException if a non-moderator
     *         requests soft-deleted loot
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
            Long createdByUserId,
            String name,
            ItemSort sort,
            String expand,
            Authentication authentication) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size,
                (sort == null ? ItemSort.ID : sort).toSort());
        ItemAccessService.VisibilityScope scope = itemAccessService.visibilityScope(authentication);
        Page<Loot> lootPage;

        if (includeDeleted) {
            // Soft-deleted rows are a moderation surface, not a browse surface. This was
            // previously ungated, which was harmless while every loot row was official but would
            // expose other users' private homebrew now that anyone can author one.
            itemAccessService.requireModerator(authentication);
            lootPage = lootRepository.findAllWithFilters(expansionId, createdByUserId, name, isOfficial, tier, isConsumable, pageable);
        } else {
            // Moderators are not branched to a separate query: findAccessibleWithFilters
            // short-circuits on isPrivileged, so routing them elsewhere would buy nothing and
            // duplicate the filter list a second time.
            lootPage = lootRepository.findAccessibleWithFilters(
                    scope.userId(), scope.memberCampaignIds(), scope.privileged(),
                    expansionId, createdByUserId, name, isOfficial, tier, isConsumable, pageable);
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
     * Creates a loot authored by the calling user.
     * <p>
     * Open to any authenticated user. Everything that could make it canon is resolved
     * server-side rather than taken from the request: the author is always the caller, the
     * official and public flags are honoured only for moderators, and custom content never
     * carries a sourcebook. The request type has no field for an original, so a caller cannot
     * claim their creation derives from something it does not — that is set only by
     * {@link #copyLoot}.
     * </p>
     *
     * @param request The creation request
     * @param authentication The authentication of the current user
     * @return LootResponse containing the created record
     * @throws com.aboff.core.exception.InsufficientPermissionsException if the request tags a
     *         campaign the user is not part of
     */
    @Transactional
    public LootResponse createCustomLoot(CreateCustomLootRequest request, Authentication authentication) {
        User user = itemAccessService.currentUser(authentication);
        boolean isOfficial = itemAccessService.resolveIsOfficial(user, false);
        Set<Campaign> campaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());

        Loot loot = Loot.builder()
                .name(request.getName())
                .expansion(itemAccessService.resolveExpansion(user, null, isOfficial))
                .tier(request.getTier())
                .isOfficial(isOfficial)
                .isPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()))
                .createdBy(user)
                .isConsumable(request.getIsConsumable())
                .description(request.getDescription())
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                null, request.getFeatures(), FeatureService.FeatureOrigin.forItem(user, isOfficial));
        if (resolvedFeatures != null) {
            loot.setFeatures(resolvedFeatures);
        }
        if (campaigns != null) {
            loot.setCampaigns(campaigns);
        }

        Loot savedLoot = lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedLoot, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "custom loot_id: " + savedLoot.getId());

        return toResponse(savedLoot, Set.of());
    }

    /**
     * Copies an existing record into new custom content owned by the calling user.
     * <p>
     * This is the primary way players customise equipment: the rules describe reflavouring an
     * existing statline rather than authoring from a blank form. Anything may be copied,
     * including official content — {@code GET} is unrestricted, so there is nothing to protect.
     * </p>
     * <p>
     * The copy is always private and unofficial regardless of its source, carries no sourcebook,
     * and inherits no campaign tags: sharing is a decision the new owner makes for themselves.
     * </p>
     *
     * @param id The ID of the record to copy
     * @param authentication The authentication of the current user
     * @return LootResponse containing the newly created copy
     * @throws EntityNotFoundException if the source is not found or is deleted
     */
    @Transactional
    public LootResponse copyLoot(Long id, Authentication authentication) {
        Loot original = lootRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Loot not found with id: " + id));

        User user = itemAccessService.currentUser(authentication);

        Loot copy = Loot.builder()
                .name(original.getName() + " (Copy)")
                .expansion(null)
                .tier(original.getTier())
                .isOfficial(false)
                .isPublic(false)
                .createdBy(user)
                .originalLoot(original)
                .isConsumable(original.getIsConsumable())
                .description(original.getDescription())
                .features(new HashSet<>(original.getFeatures()))
                .build();

        Loot savedCopy = lootRepository.save(copy);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCopy, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "copied loot_id: " + id + " to loot_id: " + savedCopy.getId());

        return toResponse(savedCopy, Set.of());
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
                "\"" + savedLoot.getName() + "\" (loot_id: " + savedLoot.getId() + ")");

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

        itemAccessService.validateModifyPermission(loot, "loot", authentication);
        User user = itemAccessService.currentUser(authentication);

        if (request.getName() != null && !request.getName().isBlank()) {
            loot.setName(request.getName());
        }
        // The official flag is applied before the expansion because it decides whether an
        // expansion may be kept at all. Create resolves them in this order too.
        if (request.getIsOfficial() != null) {
            loot.setIsOfficial(itemAccessService.resolveIsOfficial(user, request.getIsOfficial()));
        }
        boolean isOfficial = Boolean.TRUE.equals(loot.getIsOfficial());

        if (Boolean.TRUE.equals(request.getClearExpansion())) {
            // A JSON null for expansionId is indistinguishable from an omitted field, so
            // removing a sourcebook needs its own explicit flag.
            loot.setExpansion(null);
        } else if (request.getExpansionId() != null) {
            loot.setExpansion(itemAccessService.resolveExpansion(user, request.getExpansionId(), isOfficial));
        }
        itemAccessService.validateOfficialHasExpansion(
                loot, "loot", Boolean.TRUE.equals(request.getClearExpansion()));

        if (request.getTier() != null) {
            loot.setTier(request.getTier());
        }
        if (request.getIsPublic() != null) {
            loot.setIsPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()));
        }

        Set<Campaign> resolvedCampaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());
        if (resolvedCampaigns != null) {
            loot.setCampaigns(resolvedCampaigns);
        }
        if (request.getIsConsumable() != null) {
            loot.setIsConsumable(request.getIsConsumable());
        }
        if (request.getDescription() != null) {
            loot.setDescription(request.getDescription());
        }

        if (request.getFeatureIds() != null || request.getFeatures() != null) {
            Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(
                    request.getFeatureIds(), request.getFeatures(),
                    FeatureService.FeatureOrigin.forItem(user, isOfficial));
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
                "loot_id: " + updatedLoot.getId());

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

        itemAccessService.validateModifyPermission(loot, "loot", authentication);

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

        // Authors can undo their own deletions; without this a user could delete something
        // they made and have no way to get it back.
        itemAccessService.validateModifyPermission(loot, "loot", authentication);

        loot.restore();
        Loot restoredLoot = lootRepository.save(loot);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredLoot, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("loot").build(),
                "loot_id: " + id);

        return toResponse(restoredLoot, Set.of());
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
                .expansionId(loot.getExpansion() != null ? loot.getExpansion().getId() : null)
                .tier(loot.getTier())
                .isOfficial(loot.getIsOfficial())
                .isPublic(loot.getIsPublic())
                .createdByUserId(loot.getCreatedBy() != null ? loot.getCreatedBy().getId() : null)
                .isConsumable(loot.getIsConsumable())
                .description(loot.getDescription())
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

        if (ExpandUtil.shouldExpand(expand, "expansion") && loot.getExpansion() != null) {
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

        if (loot.getCampaigns() != null && !loot.getCampaigns().isEmpty()) {
            builder.campaignIds(loot.getCampaigns().stream()
                    .map(Campaign::getId)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
