package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCustomWeaponRequest;
import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;

import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.ItemSort;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.WeaponRepository;
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
 * Service for managing Weapon entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeaponService {

    private final WeaponRepository weaponRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ItemAccessService itemAccessService;

    /**
     * Retrieves a paginated list of weapons.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted weapons
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param tier Optional filter for weapon tier (1–4)
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC, PHYSICAL_AND_MAGIC)
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param sort Requested ordering; defaults to {@link ItemSort#ID}
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The current authentication, used to resolve what the caller may see
     * @return Paginated response containing weapons
     * @throws com.aboff.core.exception.InsufficientPermissionsException if a non-moderator
     *         requests soft-deleted weapons
     */
    @Transactional(readOnly = true)
    public PagedResponse<WeaponResponse> getAllWeapons(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Trait trait,
            Range range,
            Burden burden,
            Boolean isPrimary,
            Integer tier,
            DamageType damageType,
            Long createdByUserId,
            String name,
            ItemSort sort,
            String expand,
            Authentication authentication) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size,
                (sort == null ? ItemSort.ID : sort).toSort());
        ItemAccessService.VisibilityScope scope = itemAccessService.visibilityScope(authentication);
        Page<Weapon> weaponPage;

        if (includeDeleted) {
            // Soft-deleted rows are a moderation surface, not a browse surface. This was
            // previously ungated, which was harmless while every weapon was official but would
            // expose other users' private homebrew now that anyone can author one.
            itemAccessService.requireModerator(authentication);
            weaponPage = weaponRepository.findAllWithFilters(expansionId, createdByUserId, name, isOfficial, trait, range, burden, isPrimary, tier, damageType, pageable);
        } else {
            // Moderators are not branched to a separate query: findAccessibleWithFilters
            // short-circuits on isPrivileged, so routing them elsewhere would buy nothing and
            // duplicate the filter list a second time.
            weaponPage = weaponRepository.findAccessibleWithFilters(
                    scope.userId(), scope.memberCampaignIds(), scope.privileged(),
                    expansionId, createdByUserId, name, isOfficial, trait, range, burden, isPrimary, tier, damageType, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<WeaponResponse>builder()
                .content(weaponPage.getContent().stream()
                        .map(weapon -> toResponse(weapon, expandSet))
                        .toList())
                .totalElements(weaponPage.getTotalElements())
                .totalPages(weaponPage.getTotalPages())
                .currentPage(weaponPage.getNumber())
                .pageSize(weaponPage.getSize())
                .build();
    }

    /**
     * Creates a weapon authored by the calling user.
     * <p>
     * Open to any authenticated user. Everything that could make the weapon canon is resolved
     * server-side rather than taken from the request: the author is always the caller, the
     * official and public flags are honoured only for moderators, and a custom weapon never
     * carries a sourcebook. The request type has no field for an original weapon, so a caller
     * cannot claim their creation derives from something it does not — that is set only by
     * {@link #copyWeapon}.
     * </p>
     *
     * @param request The creation request containing weapon details
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the created weapon
     * @throws com.aboff.core.exception.InsufficientPermissionsException if the request tags a
     *         campaign the user is not part of
     */
    @Transactional
    public WeaponResponse createCustomWeapon(CreateCustomWeaponRequest request, Authentication authentication) {
        User user = itemAccessService.currentUser(authentication);
        boolean isOfficial = itemAccessService.resolveIsOfficial(user, false);
        Set<Campaign> campaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());

        Weapon weapon = Weapon.builder()
                .name(request.getName())
                .expansion(itemAccessService.resolveExpansion(user, null, isOfficial))
                .tier(request.getTier())
                .isOfficial(isOfficial)
                .isPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()))
                .createdBy(user)
                .isPrimary(request.getIsPrimary())
                .trait(request.getTrait())
                .range(request.getRange())
                .burden(request.getBurden())
                .damage(toDamageRoll(request.getDamage()))
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                null, request.getFeatures(), FeatureService.FeatureOrigin.forItem(user, isOfficial));
        if (resolvedFeatures != null) {
            weapon.setFeatures(resolvedFeatures);
        }
        if (campaigns != null) {
            weapon.setCampaigns(campaigns);
        }

        Weapon savedWeapon = weaponRepository.save(weapon);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedWeapon, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "custom weapon_id: " + savedWeapon.getId());

        return toResponse(savedWeapon, Set.of());
    }

    /**
     * Copies an existing weapon into a new custom weapon owned by the calling user.
     * <p>
     * This is the primary way players customise equipment: the rules describe reflavouring an
     * existing statline rather than authoring from a blank form. Any weapon may be copied,
     * including official ones — {@code GET} is unrestricted, so there is nothing to protect.
     * </p>
     * <p>
     * The copy is always private and unofficial regardless of its source, carries no sourcebook,
     * and inherits no campaign tags: sharing is a decision the new owner makes for themselves.
     * </p>
     *
     * @param id The ID of the weapon to copy
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the newly created copy
     * @throws EntityNotFoundException if the source weapon is not found or is deleted
     */
    @Transactional
    public WeaponResponse copyWeapon(Long id, Authentication authentication) {
        Weapon original = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        User user = itemAccessService.currentUser(authentication);

        Weapon copy = Weapon.builder()
                .name(original.getName() + " (Copy)")
                .expansion(null)
                .tier(original.getTier())
                .isOfficial(false)
                .isPublic(false)
                .createdBy(user)
                .originalWeapon(original)
                .isPrimary(original.getIsPrimary())
                .trait(original.getTrait())
                .range(original.getRange())
                .burden(original.getBurden())
                .damage(original.getDamage())
                .features(new HashSet<>(original.getFeatures()))
                .build();

        Weapon savedCopy = weaponRepository.save(copy);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCopy, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "copied weapon_id: " + id + " to weapon_id: " + savedCopy.getId());

        return toResponse(savedCopy, Set.of());
    }

    /**
     * Retrieves a single weapon by ID.
     *
     * @param id The weapon ID
     * @param expand Comma-separated list of relationships to expand
     * @return WeaponResponse containing the weapon details
     * @throws EntityNotFoundException if the weapon is not found or is deleted
     */
    @Transactional(readOnly = true)
    public WeaponResponse getWeaponById(Long id, String expand) {
        Weapon weapon = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(weapon, expandSet);
    }

    /**
     * Creates a new weapon.
     *
     * @param request The creation request containing weapon details
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the created weapon
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public WeaponResponse createWeapon(CreateWeaponRequest request, Authentication authentication) {
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Weapon weapon = Weapon.builder()
                .name(request.getName())
                .expansion(expansion)
                .tier(request.getTier())
                .isOfficial(request.getIsOfficial())
                .isPrimary(request.getIsPrimary())
                .trait(request.getTrait())
                .range(request.getRange())
                .burden(request.getBurden())
                .damage(toDamageRoll(request.getDamage()))
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            weapon.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalWeaponId() != null) {
            Weapon originalWeapon = weaponRepository.findByIdAndDeletedAtIsNull(request.getOriginalWeaponId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original weapon not found with id: " + request.getOriginalWeaponId()));
            weapon.setOriginalWeapon(originalWeapon);
        }

        Weapon savedWeapon = weaponRepository.save(weapon);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedWeapon, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "\"" + savedWeapon.getName() + "\" (weapon_id: " + savedWeapon.getId() + ")");

        return toResponse(savedWeapon, Set.of());
    }

    /**
     * Creates multiple weapons in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created weapon responses
     */
    @Transactional
    public List<WeaponResponse> createWeaponsBulk(List<CreateWeaponRequest> requests, Authentication authentication) {
        List<Weapon> weapons = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    Weapon weapon = Weapon.builder()
                            .name(request.getName())
                            .expansion(expansion)
                            .tier(request.getTier())
                            .isOfficial(request.getIsOfficial())
                            .isPrimary(request.getIsPrimary())
                            .trait(request.getTrait())
                            .range(request.getRange())
                            .burden(request.getBurden())
                            .damage(toDamageRoll(request.getDamage()))
                            .build();

                    Set<Feature> bulkResolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
                    if (bulkResolvedFeatures != null) {
                        weapon.setFeatures(bulkResolvedFeatures);
                    }

                    if (request.getOriginalWeaponId() != null) {
                        Weapon originalWeapon = weaponRepository.findByIdAndDeletedAtIsNull(request.getOriginalWeaponId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Original weapon not found with id: " + request.getOriginalWeaponId()));
                        weapon.setOriginalWeapon(originalWeapon);
                    }

                    return weapon;
                })
                .toList();

        List<Weapon> savedWeapons = weaponRepository.saveAll(weapons);
        savedWeapons.forEach(w -> eventPublisher.publishEvent(new EntityChangeEvent(this, w, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                savedWeapons.size() + " created, 0 failed");

        return savedWeapons.stream()
                .map(weapon -> toResponse(weapon, Set.of()))
                .toList();
    }

    /**
     * Updates an existing weapon.
     *
     * @param id The weapon ID to update
     * @param request The update request containing new weapon details
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the updated weapon
     * @throws EntityNotFoundException if the weapon or referenced entities are not found
     */
    @Transactional
    public WeaponResponse updateWeapon(Long id, UpdateWeaponRequest request, Authentication authentication) {
        Weapon weapon = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        itemAccessService.validateModifyPermission(weapon, "weapon", authentication);
        User user = itemAccessService.currentUser(authentication);

        if (request.getName() != null && !request.getName().isBlank()) {
            weapon.setName(request.getName());
        }
        // The official flag is applied before the expansion because it decides whether an
        // expansion may be kept at all. Create resolves them in this order too.
        if (request.getIsOfficial() != null) {
            weapon.setIsOfficial(itemAccessService.resolveIsOfficial(user, request.getIsOfficial()));
        }
        boolean isOfficial = Boolean.TRUE.equals(weapon.getIsOfficial());

        if (Boolean.TRUE.equals(request.getClearExpansion())) {
            // A JSON null for expansionId is indistinguishable from an omitted field, so
            // removing a sourcebook needs its own explicit flag.
            weapon.setExpansion(null);
        } else if (request.getExpansionId() != null) {
            weapon.setExpansion(itemAccessService.resolveExpansion(user, request.getExpansionId(), isOfficial));
        }
        itemAccessService.validateOfficialHasExpansion(weapon, "weapon");

        if (request.getTier() != null) {
            weapon.setTier(request.getTier());
        }
        if (request.getIsPublic() != null) {
            weapon.setIsPublic(itemAccessService.resolveIsPublic(user, request.getIsPublic()));
        }

        Set<Campaign> resolvedCampaigns = itemAccessService.resolveCampaigns(user, request.getCampaignIds());
        if (resolvedCampaigns != null) {
            weapon.setCampaigns(resolvedCampaigns);
        }
        if (request.getIsPrimary() != null) {
            weapon.setIsPrimary(request.getIsPrimary());
        }
        if (request.getTrait() != null) {
            weapon.setTrait(request.getTrait());
        }
        if (request.getRange() != null) {
            weapon.setRange(request.getRange());
        }
        if (request.getBurden() != null) {
            weapon.setBurden(request.getBurden());
        }
        if (request.getDamage() != null) {
            weapon.setDamage(toDamageRoll(request.getDamage()));
        }

        Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(
                request.getFeatureIds(), request.getFeatures(),
                FeatureService.FeatureOrigin.forItem(user, isOfficial));
        if (resolvedUpdateFeatures != null) {
            weapon.setFeatures(resolvedUpdateFeatures);
        }

        if (request.getOriginalWeaponId() != null) {
            Weapon originalWeapon = weaponRepository.findByIdAndDeletedAtIsNull(request.getOriginalWeaponId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original weapon not found with id: " + request.getOriginalWeaponId()));
            weapon.setOriginalWeapon(originalWeapon);
        }

        Weapon updatedWeapon = weaponRepository.save(weapon);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedWeapon, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "weapon_id: " + updatedWeapon.getId());

        return toResponse(updatedWeapon, Set.of());
    }

    /**
     * Soft deletes a weapon by setting its deletedAt timestamp.
     *
     * @param id The weapon ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the weapon is not found or is already deleted
     */
    @Transactional
    public void deleteWeapon(Long id, Authentication authentication) {
        Weapon weapon = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        itemAccessService.validateModifyPermission(weapon, "weapon", authentication);

        weapon.softDelete();
        weaponRepository.save(weapon);
        eventPublisher.publishEvent(new EntityChangeEvent(this, weapon, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "weapon_id: " + id);
    }

    /**
     * Restores a soft-deleted weapon.
     *
     * @param id The weapon ID to restore
     * @param authentication The authentication of the current user
     * @return WeaponResponse containing the restored weapon
     * @throws EntityNotFoundException if the weapon is not found
     * @throws IllegalStateException if the weapon is not deleted
     */
    @Transactional
    public WeaponResponse restoreWeapon(Long id, Authentication authentication) {
        Weapon weapon = weaponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        if (!weapon.isDeleted()) {
            throw new IllegalStateException("Weapon with id " + id + " is not deleted");
        }

        // Authors can undo their own deletions; without this a user could delete a weapon they
        // made and have no way to get it back.
        itemAccessService.validateModifyPermission(weapon, "weapon", authentication);

        weapon.restore();
        Weapon restoredWeapon = weaponRepository.save(weapon);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredWeapon, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("weapon").build(),
                "weapon_id: " + id);

        return toResponse(restoredWeapon, Set.of());
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable.
     *
     * @param request The damage roll request
     * @return DamageRoll embeddable
     */
    private DamageRoll toDamageRoll(CreateWeaponRequest.DamageRollRequest request) {
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable (for update requests).
     *
     * @param request The damage roll request
     * @return DamageRoll embeddable
     */
    private DamageRoll toDamageRoll(UpdateWeaponRequest.DamageRollRequest request) {
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts a Weapon entity to WeaponResponse DTO.
     *
     * @param weapon The weapon entity
     * @param expand Set of relationships to expand
     * @return WeaponResponse DTO
     */
    public WeaponResponse toResponse(Weapon weapon, Set<String> expand) {
        WeaponResponse.WeaponResponseBuilder builder = WeaponResponse.builder()
                .id(weapon.getId())
                .name(weapon.getName())
                .expansionId(weapon.getExpansion() != null ? weapon.getExpansion().getId() : null)
                .tier(weapon.getTier())
                .isOfficial(weapon.getIsOfficial())
                .isPublic(weapon.getIsPublic())
                .createdByUserId(weapon.getCreatedBy() != null ? weapon.getCreatedBy().getId() : null)
                .isPrimary(weapon.getIsPrimary())
                .trait(weapon.getTrait())
                .range(weapon.getRange())
                .burden(weapon.getBurden())
                .createdAt(weapon.getCreatedAt())
                .lastModifiedAt(weapon.getLastModifiedAt())
                .deletedAt(weapon.getDeletedAt());

        if (weapon.getDamage() != null) {
            builder.damage(WeaponResponse.DamageRollResponse.builder()
                    .diceCount(weapon.getDamage().getDiceCount())
                    .diceType(weapon.getDamage().getDiceType())
                    .modifier(weapon.getDamage().getModifier())
                    .damageType(weapon.getDamage().getDamageType())
                    .notation(weapon.getDamage().toNotation())
                    .build());
        }

        if (weapon.getFeatures() != null && !weapon.getFeatures().isEmpty()) {
            builder.featureIds(weapon.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (weapon.getOriginalWeapon() != null) {
            builder.originalWeaponId(weapon.getOriginalWeapon().getId());
        }

        if (ExpandUtil.shouldExpand(expand, "expansion") && weapon.getExpansion() != null) {
            Expansion expansion = weapon.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "features") && weapon.getFeatures() != null && !weapon.getFeatures().isEmpty()) {
            builder.features(weapon.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalWeapon") && weapon.getOriginalWeapon() != null) {
            builder.originalWeapon(toResponse(weapon.getOriginalWeapon(), Set.of()));
        }

        if (weapon.getCampaigns() != null && !weapon.getCampaigns().isEmpty()) {
            builder.campaignIds(weapon.getCampaigns().stream()
                    .map(Campaign::getId)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
