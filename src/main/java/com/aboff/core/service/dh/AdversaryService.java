package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.BatchCreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.CreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.UpdateAdversaryRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.BatchCreateAdversaryResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.ExpansionRepository;

import com.aboff.core.security.CustomUserDetails;
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

import com.aboff.core.util.ExpandUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Adversary entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion,
 * relationship expansion, and permission validation.
 * </p>
 * <p>
 * Permission model:
 * </p>
 * <ul>
 *   <li>Official adversaries: Only OWNER role can modify</li>
 *   <li>Non-official adversaries: Creator OR MODERATOR+ can modify</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdversaryService {

    private final AdversaryRepository adversaryRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;
    private final RoleHierarchyService roleHierarchyService;

    /**
     * Retrieves a paginated list of adversaries accessible to the authenticated user.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted adversaries (ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param adversaryType Optional filter for adversary type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match)
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return Paginated response containing adversaries
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdversaryResponse> getAllAdversaries(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Integer tier,
            AdversaryType adversaryType,
            Boolean isOfficial,
            String name,
            String expand,
            Authentication auth) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Adversary> adversaryPage;

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (includeDeleted && roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            adversaryPage = adversaryRepository.findAllWithFilters(
                    expansionId, tier, adversaryType, isOfficial, name, true, pageable);
        } else {
            adversaryPage = adversaryRepository.findAccessibleWithFilters(
                    user.getId(), expansionId, tier, adversaryType, isOfficial, name, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<AdversaryResponse>builder()
                .content(adversaryPage.getContent().stream()
                        .map(adversary -> toResponse(adversary, expandSet))
                        .toList())
                .totalElements(adversaryPage.getTotalElements())
                .totalPages(adversaryPage.getTotalPages())
                .currentPage(adversaryPage.getNumber())
                .pageSize(adversaryPage.getSize())
                .build();
    }

    /**
     * Retrieves a single adversary by ID.
     *
     * @param id The adversary ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return AdversaryResponse containing the adversary details
     * @throws EntityNotFoundException if the adversary is not found or not accessible
     */
    @Transactional(readOnly = true)
    public AdversaryResponse getAdversaryById(Long id, String expand, Authentication auth) {
        Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + id));

        validateViewPermission(adversary, auth);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(adversary, expandSet);
    }

    /**
     * Creates a new adversary.
     *
     * @param request The creation request containing adversary details
     * @param auth Authentication context
     * @return AdversaryResponse containing the created adversary
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public AdversaryResponse createAdversary(CreateAdversaryRequest request, Authentication auth) {
        log.info("Creating new adversary with name: {}", request.getName());

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        validateThresholds(request.getMajorThreshold(), request.getSevereThreshold());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Adversary adversary = Adversary.builder()
                .name(request.getName())
                .tier(request.getTier())
                .adversaryType(request.getAdversaryType())
                .description(request.getDescription())
                .motivesAndTactics(request.getMotivesAndTactics())
                .difficulty(request.getDifficulty())
                .majorThreshold(request.getMajorThreshold())
                .severeThreshold(request.getSevereThreshold())
                .hitPointMax(request.getHitPointMax() != null ? request.getHitPointMax() : 0)
                .stressMax(request.getStressMax() != null ? request.getStressMax() : 0)
                .attackModifier(request.getAttackModifier())
                .weaponName(request.getWeaponName())
                .attackRange(request.getAttackRange())
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        if (request.getDamage() != null) {
            adversary.setDamage(toDamageRoll(request.getDamage()));
        }

        if (request.getOriginalAdversaryId() != null) {
            Adversary original = adversaryRepository.findByIdAndDeletedAtIsNull(request.getOriginalAdversaryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original adversary not found with id: " + request.getOriginalAdversaryId()));
            adversary.setOriginalAdversary(original);
        }

        if (request.getExperienceIds() != null && !request.getExperienceIds().isEmpty()) {
            Set<Experience> experiences = new HashSet<>(
                    experienceRepository.findAllById(request.getExperienceIds()));
            adversary.setExperiences(experiences);
        }

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                request.getFeatureIds() != null ? new ArrayList<>(request.getFeatureIds()) : null,
                request.getFeatures());
        if (resolvedFeatures != null) {
            adversary.setFeatures(resolvedFeatures);
        }

        Adversary savedAdversary = adversaryRepository.save(adversary);
        log.info("Created adversary with id: {}", savedAdversary.getId());

        return toResponse(savedAdversary, Set.of());
    }

    /**
     * Creates multiple adversaries in a batch operation.
     * Supports partial success - individual failures do not affect other creates.
     *
     * @param request The batch creation request
     * @param auth Authentication context
     * @return BatchCreateAdversaryResponse containing created adversaries and errors
     */
    @Transactional
    public BatchCreateAdversaryResponse batchCreateAdversaries(
            BatchCreateAdversaryRequest request, Authentication auth) {
        log.info("Batch creating {} adversaries", request.getAdversaries().size());

        List<AdversaryResponse> created = new ArrayList<>();
        List<BatchCreateAdversaryResponse.BatchError> errors = new ArrayList<>();

        for (int i = 0; i < request.getAdversaries().size(); i++) {
            CreateAdversaryRequest adversaryRequest = request.getAdversaries().get(i);
            try {
                AdversaryResponse response = createAdversary(adversaryRequest, auth);
                created.add(response);
            } catch (Exception e) {
                log.warn("Failed to create adversary at index {}: {}", i, e.getMessage());
                errors.add(BatchCreateAdversaryResponse.BatchError.builder()
                        .index(i)
                        .name(adversaryRequest.getName())
                        .error(e.getMessage())
                        .build());
            }
        }

        log.info("Batch create complete: {} created, {} failed",
                created.size(), errors.size());

        return BatchCreateAdversaryResponse.builder()
                .created(created)
                .errors(errors.isEmpty() ? null : errors)
                .totalRequested(request.getAdversaries().size())
                .totalCreated(created.size())
                .totalFailed(errors.size())
                .build();
    }

    /**
     * Updates an existing adversary.
     *
     * @param id The adversary ID to update
     * @param request The update request containing new adversary details
     * @param auth Authentication context
     * @return AdversaryResponse containing the updated adversary
     * @throws EntityNotFoundException if the adversary or referenced entities are not found
     * @throws InsufficientPermissionsException if user lacks permission to modify
     */
    @Transactional
    public AdversaryResponse updateAdversary(Long id, UpdateAdversaryRequest request, Authentication auth) {
        log.info("Updating adversary with id: {}", id);

        Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + id));

        validateModifyPermission(adversary, auth);

        // Partial updates - only update non-null fields
        if (request.getName() != null) {
            adversary.setName(request.getName());
        }
        if (request.getTier() != null) {
            adversary.setTier(request.getTier());
        }
        if (request.getAdversaryType() != null) {
            adversary.setAdversaryType(request.getAdversaryType());
        }
        if (request.getDescription() != null) {
            adversary.setDescription(request.getDescription());
        }
        if (request.getMotivesAndTactics() != null) {
            adversary.setMotivesAndTactics(request.getMotivesAndTactics());
        }
        if (request.getDifficulty() != null) {
            adversary.setDifficulty(request.getDifficulty());
        }
        if (request.getMajorThreshold() != null) {
            adversary.setMajorThreshold(request.getMajorThreshold());
        }
        if (request.getSevereThreshold() != null) {
            adversary.setSevereThreshold(request.getSevereThreshold());
        }
        if (request.getHitPointMax() != null) {
            adversary.setHitPointMax(request.getHitPointMax());
        }
        if (request.getHitPointMarked() != null) {
            adversary.setHitPointMarked(request.getHitPointMarked());
        }
        if (request.getStressMax() != null) {
            adversary.setStressMax(request.getStressMax());
        }
        if (request.getStressMarked() != null) {
            adversary.setStressMarked(request.getStressMarked());
        }
        if (request.getAttackModifier() != null) {
            adversary.setAttackModifier(request.getAttackModifier());
        }
        if (request.getWeaponName() != null) {
            adversary.setWeaponName(request.getWeaponName());
        }
        if (request.getAttackRange() != null) {
            adversary.setAttackRange(request.getAttackRange());
        }
        if (request.getDamage() != null) {
            adversary.setDamage(toDamageRoll(request.getDamage()));
        }
        if (request.getIsPublic() != null) {
            adversary.setIsPublic(request.getIsPublic());
        }

        // Validate thresholds after updates
        validateThresholds(adversary.getMajorThreshold(), adversary.getSevereThreshold());
        validateMarkedValues(adversary);

        // Update relationships if provided
        if (request.getExperienceIds() != null) {
            Set<Experience> experiences = new HashSet<>(
                    experienceRepository.findAllById(request.getExperienceIds()));
            adversary.setExperiences(experiences);
        }
        Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(
                request.getFeatureIds() != null ? new ArrayList<>(request.getFeatureIds()) : null,
                request.getFeatures());
        if (resolvedUpdateFeatures != null) {
            adversary.setFeatures(resolvedUpdateFeatures);
        }

        Adversary updatedAdversary = adversaryRepository.save(adversary);
        log.info("Updated adversary with id: {}", updatedAdversary.getId());

        return toResponse(updatedAdversary, Set.of());
    }

    /**
     * Soft deletes an adversary.
     *
     * @param id The adversary ID to delete
     * @param auth Authentication context
     * @throws EntityNotFoundException if the adversary is not found
     * @throws InsufficientPermissionsException if user lacks permission to delete
     */
    @Transactional
    public void deleteAdversary(Long id, Authentication auth) {
        log.info("Soft deleting adversary with id: {}", id);

        Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + id));

        validateModifyPermission(adversary, auth);

        adversary.softDelete();
        adversaryRepository.save(adversary);

        log.info("Soft deleted adversary with id: {}", id);
    }

    /**
     * Restores a soft-deleted adversary.
     * Only ADMIN or OWNER can restore adversaries.
     *
     * @param id The adversary ID to restore
     * @param auth Authentication context
     * @return AdversaryResponse containing the restored adversary
     * @throws EntityNotFoundException if the adversary is not found
     * @throws IllegalStateException if the adversary is not deleted
     * @throws InsufficientPermissionsException if user lacks ADMIN+ role
     */
    @Transactional
    public AdversaryResponse restoreAdversary(Long id, Authentication auth) {
        log.info("Restoring adversary with id: {}", id);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (!roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            throw new InsufficientPermissionsException("Only admins can restore adversaries");
        }

        Adversary adversary = adversaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + id));

        if (!adversary.isDeleted()) {
            throw new IllegalStateException("Adversary with id " + id + " is not deleted");
        }

        adversary.restore();
        Adversary restoredAdversary = adversaryRepository.save(adversary);

        log.info("Restored adversary with id: {}", id);

        return toResponse(restoredAdversary, Set.of());
    }

    /**
     * Creates a copy of an existing adversary for the authenticated user.
     *
     * @param id The ID of the adversary to copy
     * @param auth Authentication context
     * @return AdversaryResponse containing the new copy
     * @throws EntityNotFoundException if the original adversary is not found
     */
    @Transactional
    public AdversaryResponse copyAdversary(Long id, Authentication auth) {
        log.info("Copying adversary with id: {}", id);

        Adversary original = adversaryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + id));

        validateViewPermission(original, auth);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        Adversary copy = Adversary.builder()
                .name(original.getName() + " (Copy)")
                .tier(original.getTier())
                .adversaryType(original.getAdversaryType())
                .description(original.getDescription())
                .motivesAndTactics(original.getMotivesAndTactics())
                .difficulty(original.getDifficulty())
                .majorThreshold(original.getMajorThreshold())
                .severeThreshold(original.getSevereThreshold())
                .hitPointMax(original.getHitPointMax())
                .stressMax(original.getStressMax())
                .attackModifier(original.getAttackModifier())
                .weaponName(original.getWeaponName())
                .attackRange(original.getAttackRange())
                .damage(original.getDamage())
                .expansion(original.getExpansion())
                .createdBy(creator)
                .originalAdversary(original)
                .isOfficial(false)
                .isPublic(false)
                .experiences(new HashSet<>(original.getExperiences()))
                .features(new HashSet<>(original.getFeatures()))
                .build();

        Adversary savedCopy = adversaryRepository.save(copy);
        log.info("Created copy of adversary {} with new id: {}", id, savedCopy.getId());

        return toResponse(savedCopy, Set.of());
    }

    /**
     * Validates that the authenticated user has permission to modify the adversary.
     * <ul>
     *   <li>Official adversaries: Only OWNER role can modify</li>
     *   <li>Non-official adversaries: Creator OR MODERATOR+ can modify</li>
     * </ul>
     *
     * @param adversary The adversary being modified
     * @param auth Authentication context
     * @throws InsufficientPermissionsException if user lacks permission
     */
    private void validateModifyPermission(Adversary adversary, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (adversary.getIsOfficial()) {
            // Official adversaries: Only OWNER can modify
            if (user.getRole() != Role.OWNER) {
                throw new InsufficientPermissionsException(
                        "Only owners can modify official adversaries");
            }
        } else {
            // Non-official: Creator OR Moderator+ can modify
            boolean isCreator = adversary.getCreatedBy().getId().equals(user.getId());
            boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

            if (!isCreator && !isModeratorPlus) {
                throw new InsufficientPermissionsException(
                        "You do not have permission to modify this adversary");
            }
        }
    }

    /**
     * Validates that the authenticated user has permission to view the adversary.
     * Private non-official adversaries are only visible to creator and moderators+.
     *
     * @param adversary The adversary being viewed
     * @param auth Authentication context
     * @throws EntityNotFoundException if user cannot view the adversary
     */
    private void validateViewPermission(Adversary adversary, Authentication auth) {
        if (adversary.getIsOfficial() || adversary.getIsPublic()) {
            return; // Anyone can view official or public adversaries
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        boolean isCreator = adversary.getCreatedBy().getId().equals(user.getId());
        boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

        if (!isCreator && !isModeratorPlus) {
            throw new EntityNotFoundException("Adversary not found with id: " + adversary.getId());
        }
    }

    /**
     * Validates that severe threshold is greater than or equal to major threshold.
     */
    private void validateThresholds(Integer majorThreshold, Integer severeThreshold) {
        if (majorThreshold != null && severeThreshold != null
                && severeThreshold < majorThreshold) {
            throw new IllegalArgumentException(
                    "Severe threshold must be greater than or equal to major threshold");
        }
    }

    /**
     * Validates that marked values don't exceed maximum values.
     */
    private void validateMarkedValues(Adversary adversary) {
        if (adversary.getHitPointMarked() > adversary.getHitPointMax()) {
            throw new IllegalArgumentException(
                    "Hit points marked cannot exceed hit points max");
        }
        if (adversary.getStressMarked() > adversary.getStressMax()) {
            throw new IllegalArgumentException(
                    "Stress marked cannot exceed stress max");
        }
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable.
     */
    private DamageRoll toDamageRoll(CreateAdversaryRequest.DamageRollRequest request) {
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable (for update requests).
     */
    private DamageRoll toDamageRoll(UpdateAdversaryRequest.DamageRollRequest request) {
        if (request.getDiceType() == null) {
            return null;
        }
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts an Adversary entity to AdversaryResponse DTO.
     */
    private AdversaryResponse toResponse(Adversary adversary, Set<String> expand) {
        AdversaryResponse.AdversaryResponseBuilder builder = AdversaryResponse.builder()
                .id(adversary.getId())
                .name(adversary.getName())
                .tier(adversary.getTier())
                .adversaryType(adversary.getAdversaryType())
                .description(adversary.getDescription())
                .motivesAndTactics(adversary.getMotivesAndTactics())
                .difficulty(adversary.getDifficulty())
                .majorThreshold(adversary.getMajorThreshold())
                .severeThreshold(adversary.getSevereThreshold())
                .hitPointMax(adversary.getHitPointMax())
                .hitPointMarked(adversary.getHitPointMarked())
                .stressMax(adversary.getStressMax())
                .stressMarked(adversary.getStressMarked())
                .attackModifier(adversary.getAttackModifier())
                .weaponName(adversary.getWeaponName())
                .attackRange(adversary.getAttackRange())
                .isOfficial(adversary.getIsOfficial())
                .isPublic(adversary.getIsPublic())
                .expansionId(adversary.getExpansion().getId())
                .creatorId(adversary.getCreatedBy().getId())
                .createdAt(adversary.getCreatedAt())
                .lastModifiedAt(adversary.getLastModifiedAt())
                .deletedAt(adversary.getDeletedAt());

        // Damage roll
        if (adversary.getDamage() != null) {
            builder.damage(AdversaryResponse.DamageRollResponse.builder()
                    .diceCount(adversary.getDamage().getDiceCount())
                    .diceType(adversary.getDamage().getDiceType())
                    .modifier(adversary.getDamage().getModifier())
                    .damageType(adversary.getDamage().getDamageType())
                    .notation(adversary.getDamage().toNotation())
                    .build());
        }

        // Original adversary ID
        if (adversary.getOriginalAdversary() != null) {
            builder.originalAdversaryId(adversary.getOriginalAdversary().getId());
        }

        // Experience IDs (always included)
        if (adversary.getExperiences() != null && !adversary.getExperiences().isEmpty()) {
            builder.experienceIds(adversary.getExperiences().stream()
                    .map(Experience::getId)
                    .collect(Collectors.toSet()));
        }

        // Feature IDs (always included)
        if (adversary.getFeatures() != null && !adversary.getFeatures().isEmpty()) {
            builder.featureIds(adversary.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toSet()));
        }

        // Expanded relationships
        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = adversary.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "creator")) {
            User creator = adversary.getCreatedBy();
            builder.creator(UserResponse.builder()
                    .id(creator.getId())
                    .username(creator.getUsername())
                    .avatarUrl(creator.getAvatarUrl())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "originalAdversary") && adversary.getOriginalAdversary() != null) {
            builder.originalAdversary(toResponse(adversary.getOriginalAdversary(), Set.of()));
        }

        if (ExpandUtil.shouldExpand(expand, "experiences") && adversary.getExperiences() != null) {
            builder.experiences(adversary.getExperiences().stream()
                    .map(exp -> ExperienceResponse.builder()
                            .id(exp.getId())
                            .description(exp.getDescription())
                            .modifier(exp.getModifier())
                            .createdAt(exp.getCreatedAt())
                            .lastModifiedAt(exp.getLastModifiedAt())
                            .build())
                    .collect(Collectors.toSet()));
        }

        if (ExpandUtil.shouldExpand(expand, "features") && adversary.getFeatures() != null) {
            builder.features(adversary.getFeatures().stream()
                    .map(feature -> featureService.toResponse(feature, expand))
                    .collect(Collectors.toSet()));
        }

        return builder.build();
    }
}
