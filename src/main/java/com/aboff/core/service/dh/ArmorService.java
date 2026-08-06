package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCustomArmorRequest;
import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;

import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.ItemSort;
import com.aboff.core.repository.dh.ArmorRepository;
import com.aboff.core.repository.dh.ExpansionRepository;

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
    private final AuditLogger auditLogger;
    private final ItemAccessService itemAccessService;

    /**
     * Retrieves a paginated list of armors.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted armors
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for armor tier (1–4)
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param sort Requested ordering; defaults to {@link ItemSort#ID}
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The current authentication, used to resolve what the caller may see
     * @return Paginated response containing armors
     * @throws com.aboff.core.exception.InsufficientPermissionsException if a non-moderator
     *         requests soft-deleted armors
     */
    @Transactional(readOnly = true)
    public PagedResponse<ArmorResponse> getAllArmors(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Integer tier,
            Long createdByUserId,
            String name,
            ItemSort sort,
            String expand,
            Authentication authentication) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size,
                (sort == null ? ItemSort.ID : sort).toSort());
        ItemAccessService.VisibilityScope scope = itemAccessService.visibilityScope(authentication);
        Page<Armor> armorPage;

        if (includeDeleted) {
            // Soft-deleted rows are a moderation surface, not a browse surface. This was
            // previously ungated, which was harmless while every armor was official but would
            // expose other users' private homebrew now that anyone can author one.
            itemAccessService.requireModerator(authentication);
            armorPage = armorRepository.findAllWithFilters(expansionId, createdByUserId, name, isOfficial, tier, pageable);
        } else {
            // Moderators are not branched to a separate query: findAccessibleWithFilters
            // short-circuits on isPrivileged, so routing them elsewhere would buy nothing and
            // duplicate the filter list a second time.
            armorPage = armorRepository.findAccessibleWithFilters(
                    scope.userId(), scope.memberCampaignIds(), scope.privileged(),
                    expansionId, createdByUserId, name, isOfficial, tier, pageable);
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
     * Creates an armor authored by the calling user.
     * <p>
     * Open to any authenticated user. Everything that could make it canon is resolved
     * server-side rather than taken from the request: the author is always the caller, the
     * official and public flags are honoured only for moderators, and custom content never
     * carries a sourcebook. Neither this request type nor the update one has a field for an
     * original, so a caller cannot claim their creation derives from something it does not, at
     * creation or afterwards — provenance is set only by {@link #copyArmor}.
     * </p>
     *
     * @param request The creation request
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the created record
     * @throws com.aboff.core.exception.InsufficientPermissionsException if the request tags a
     *         campaign the user is not part of
     */
    @Transactional
    public ArmorResponse createCustomArmor(CreateCustomArmorRequest request, Authentication authentication) {
        User user = itemAccessService.currentUser(authentication);
        boolean isOfficial = itemAccessService.resolveIsOfficial(user, false);
        Set<Campaign> campaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());

        Armor armor = Armor.builder()
                .name(request.getName())
                .expansion(itemAccessService.resolveExpansion(user, null, isOfficial))
                .tier(request.getTier())
                .isOfficial(isOfficial)
                .isPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()))
                .createdBy(user)
                .baseMajorThreshold(request.getBaseMajorThreshold())
                .baseSevereThreshold(request.getBaseSevereThreshold())
                .baseScore(request.getBaseScore())
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                null, request.getFeatures(), FeatureService.FeatureOrigin.forItem(user, isOfficial));
        if (resolvedFeatures != null) {
            armor.setFeatures(resolvedFeatures);
        }
        if (campaigns != null) {
            armor.setCampaigns(campaigns);
        }

        Armor savedArmor = armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedArmor, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "custom armor_id: " + savedArmor.getId());

        return toResponse(savedArmor, Set.of());
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
     * @return ArmorResponse containing the newly created copy
     * @throws EntityNotFoundException if the source is not found or is deleted
     */
    @Transactional
    public ArmorResponse copyArmor(Long id, Authentication authentication) {
        Armor original = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        User user = itemAccessService.currentUser(authentication);

        Armor copy = Armor.builder()
                .name(original.getName() + " (Copy)")
                .expansion(null)
                .tier(original.getTier())
                .isOfficial(false)
                .isPublic(false)
                .createdBy(user)
                .originalArmor(original)
                .baseMajorThreshold(original.getBaseMajorThreshold())
                .baseSevereThreshold(original.getBaseSevereThreshold())
                .baseScore(original.getBaseScore())
                .features(new HashSet<>(original.getFeatures()))
                .build();

        Armor savedCopy = armorRepository.save(copy);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCopy, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "copied armor_id: " + id + " to armor_id: " + savedCopy.getId());

        return toResponse(savedCopy, Set.of());
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
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the created armor
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public ArmorResponse createArmor(CreateArmorRequest request, Authentication authentication) {
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
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedArmor, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "\"" + savedArmor.getName() + "\" (armor_id: " + savedArmor.getId() + ")");

        return toResponse(savedArmor, Set.of());
    }

    /**
     * Creates multiple armors in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created armor responses
     */
    @Transactional
    public List<ArmorResponse> createArmorsBulk(List<CreateArmorRequest> requests, Authentication authentication) {
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
        savedArmors.forEach(a -> eventPublisher.publishEvent(new EntityChangeEvent(this, a, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                savedArmors.size() + " created, 0 failed");

        return savedArmors.stream()
                .map(armor -> toResponse(armor, Set.of()))
                .toList();
    }

    /**
     * Updates an existing armor.
     *
     * @param id The armor ID to update
     * @param request The update request containing new armor details
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the updated armor
     * @throws EntityNotFoundException if the armor or referenced entities are not found
     */
    @Transactional
    public ArmorResponse updateArmor(Long id, UpdateArmorRequest request, Authentication authentication) {
        Armor armor = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        itemAccessService.validateModifyPermission(armor, "armor", authentication);
        User user = itemAccessService.currentUser(authentication);

        if (request.getName() != null && !request.getName().isBlank()) {
            armor.setName(request.getName());
        }
        // The official flag is applied before the expansion because it decides whether an
        // expansion may be kept at all. Create resolves them in this order too.
        if (request.getIsOfficial() != null) {
            armor.setIsOfficial(itemAccessService.resolveIsOfficial(user, request.getIsOfficial()));
        }
        boolean isOfficial = Boolean.TRUE.equals(armor.getIsOfficial());

        if (Boolean.TRUE.equals(request.getClearExpansion())) {
            // A JSON null for expansionId is indistinguishable from an omitted field, so
            // removing a sourcebook needs its own explicit flag.
            armor.setExpansion(null);
        } else if (request.getExpansionId() != null) {
            armor.setExpansion(itemAccessService.resolveExpansion(user, request.getExpansionId(), isOfficial));
        }
        itemAccessService.validateOfficialHasExpansion(
                armor, "armor", Boolean.TRUE.equals(request.getClearExpansion()));

        if (request.getTier() != null) {
            armor.setTier(request.getTier());
        }
        if (request.getIsPublic() != null) {
            armor.setIsPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()));
        }

        Set<Campaign> resolvedCampaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());
        if (resolvedCampaigns != null) {
            armor.setCampaigns(resolvedCampaigns);
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
            Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(
                    request.getFeatureIds(), request.getFeatures(),
                    FeatureService.FeatureOrigin.forItem(user, isOfficial));
            if (resolvedUpdateFeatures != null) {
                armor.setFeatures(resolvedUpdateFeatures);
            }
        }

        Armor updatedArmor = armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedArmor, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "armor_id: " + updatedArmor.getId());

        return toResponse(updatedArmor, Set.of());
    }

    /**
     * Soft deletes an armor by setting its deletedAt timestamp.
     *
     * @param id The armor ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the armor is not found or is already deleted
     */
    @Transactional
    public void deleteArmor(Long id, Authentication authentication) {
        Armor armor = armorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        itemAccessService.validateModifyPermission(armor, "armor", authentication);

        armor.softDelete();
        armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, armor, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "armor_id: " + id);
    }

    /**
     * Restores a soft-deleted armor.
     *
     * @param id The armor ID to restore
     * @param authentication The authentication of the current user
     * @return ArmorResponse containing the restored armor
     * @throws EntityNotFoundException if the armor is not found
     * @throws IllegalStateException if the armor is not deleted
     */
    @Transactional
    public ArmorResponse restoreArmor(Long id, Authentication authentication) {
        Armor armor = armorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Armor not found with id: " + id));

        if (!armor.isDeleted()) {
            throw new IllegalStateException("Armor with id " + id + " is not deleted");
        }

        // Authors can undo their own deletions; without this a user could delete something
        // they made and have no way to get it back.
        itemAccessService.validateModifyPermission(armor, "armor", authentication);

        armor.restore();
        Armor restoredArmor = armorRepository.save(armor);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredArmor, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("armor").build(),
                "armor_id: " + id);

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
                .expansionId(armor.getExpansion() != null ? armor.getExpansion().getId() : null)
                .tier(armor.getTier())
                .isOfficial(armor.getIsOfficial())
                .isPublic(armor.getIsPublic())
                .createdByUserId(armor.getCreatedBy() != null ? armor.getCreatedBy().getId() : null)
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

        if (ExpandUtil.shouldExpand(expand, "expansion") && armor.getExpansion() != null) {
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

        if (armor.getCampaigns() != null && !armor.getCampaigns().isEmpty()) {
            builder.campaignIds(armor.getCampaigns().stream()
                    .map(Campaign::getId)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
