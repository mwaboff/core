package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;

import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.WeaponRepository;
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
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC)
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing weapons
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
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Weapon> weaponPage;

        if (includeDeleted) {
            weaponPage = weaponRepository.findAllWithFilters(expansionId, isOfficial, trait, range, burden, isPrimary, tier, damageType, pageable);
        } else {
            weaponPage = weaponRepository.findByDeletedAtIsNullAndFilters(expansionId, isOfficial, trait, range, burden, isPrimary, tier, damageType, pageable);
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
     * @return WeaponResponse containing the created weapon
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public WeaponResponse createWeapon(CreateWeaponRequest request) {
        log.info("Creating new weapon with name: {}", request.getName());

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
        log.info("Created weapon with id: {}", savedWeapon.getId());

        return toResponse(savedWeapon, Set.of());
    }

    /**
     * Creates multiple weapons in bulk.
     *
     * @param requests List of creation requests
     * @return List of created weapon responses
     */
    @Transactional
    public List<WeaponResponse> createWeaponsBulk(List<CreateWeaponRequest> requests) {
        log.info("Creating {} weapons in bulk", requests.size());

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
        log.info("Created {} weapons in bulk", savedWeapons.size());

        return savedWeapons.stream()
                .map(weapon -> toResponse(weapon, Set.of()))
                .toList();
    }

    /**
     * Updates an existing weapon.
     *
     * @param id The weapon ID to update
     * @param request The update request containing new weapon details
     * @return WeaponResponse containing the updated weapon
     * @throws EntityNotFoundException if the weapon or referenced entities are not found
     */
    @Transactional
    public WeaponResponse updateWeapon(Long id, UpdateWeaponRequest request) {
        log.info("Updating weapon with id: {}", id);

        Weapon weapon = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        weapon.setName(request.getName());
        weapon.setExpansion(expansion);
        weapon.setTier(request.getTier());
        weapon.setIsOfficial(request.getIsOfficial());
        weapon.setIsPrimary(request.getIsPrimary());
        weapon.setTrait(request.getTrait());
        weapon.setRange(request.getRange());
        weapon.setBurden(request.getBurden());
        weapon.setDamage(toDamageRoll(request.getDamage()));

        Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedUpdateFeatures != null) {
            weapon.setFeatures(resolvedUpdateFeatures);
        }

        if (request.getOriginalWeaponId() != null) {
            Weapon originalWeapon = weaponRepository.findByIdAndDeletedAtIsNull(request.getOriginalWeaponId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original weapon not found with id: " + request.getOriginalWeaponId()));
            weapon.setOriginalWeapon(originalWeapon);
        } else {
            weapon.setOriginalWeapon(null);
        }

        Weapon updatedWeapon = weaponRepository.save(weapon);
        log.info("Updated weapon with id: {}", updatedWeapon.getId());

        return toResponse(updatedWeapon, Set.of());
    }

    /**
     * Soft deletes a weapon by setting its deletedAt timestamp.
     *
     * @param id The weapon ID to delete
     * @throws EntityNotFoundException if the weapon is not found or is already deleted
     */
    @Transactional
    public void deleteWeapon(Long id) {
        log.info("Soft deleting weapon with id: {}", id);

        Weapon weapon = weaponRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        weapon.softDelete();
        weaponRepository.save(weapon);

        log.info("Soft deleted weapon with id: {}", id);
    }

    /**
     * Restores a soft-deleted weapon.
     *
     * @param id The weapon ID to restore
     * @return WeaponResponse containing the restored weapon
     * @throws EntityNotFoundException if the weapon is not found
     * @throws IllegalStateException if the weapon is not deleted
     */
    @Transactional
    public WeaponResponse restoreWeapon(Long id) {
        log.info("Restoring weapon with id: {}", id);

        Weapon weapon = weaponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Weapon not found with id: " + id));

        if (!weapon.isDeleted()) {
            throw new IllegalStateException("Weapon with id " + id + " is not deleted");
        }

        weapon.restore();
        Weapon restoredWeapon = weaponRepository.save(weapon);

        log.info("Restored weapon with id: {}", id);

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
    private WeaponResponse toResponse(Weapon weapon, Set<String> expand) {
        WeaponResponse.WeaponResponseBuilder builder = WeaponResponse.builder()
                .id(weapon.getId())
                .name(weapon.getName())
                .expansionId(weapon.getExpansion().getId())
                .tier(weapon.getTier())
                .isOfficial(weapon.getIsOfficial())
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

        if (expand.contains("expansion")) {
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

        if (expand.contains("features") && weapon.getFeatures() != null && !weapon.getFeatures().isEmpty()) {
            builder.features(weapon.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (expand.contains("originalWeapon") && weapon.getOriginalWeapon() != null) {
            builder.originalWeapon(toResponse(weapon.getOriginalWeapon(), Set.of()));
        }

        return builder.build();
    }
}
