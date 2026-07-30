package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateEnvironmentRequest;
import com.aboff.core.model.dto.dh.request.UpdateEnvironmentRequest;
import com.aboff.core.model.dto.dh.response.EnvironmentResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.EnvironmentRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Environment entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion,
 * relationship expansion, and permission validation.
 * </p>
 * <p>
 * Permission model mirrors {@code AdversaryService}:
 * </p>
 * <ul>
 *   <li>Official environments: Only OWNER role can modify</li>
 *   <li>Non-official environments: Creator OR MODERATOR+ can modify</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final RoleHierarchyService roleHierarchyService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of environments accessible to the authenticated user.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted environments (ADMIN+ only)
     * @param expansionId Optional filter for expansion ID
     * @param tier Optional filter for tier (1-4)
     * @param environmentType Optional filter for environment type
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match)
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return Paginated response containing environments
     */
    @Transactional(readOnly = true)
    public PagedResponse<EnvironmentResponse> getAllEnvironments(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Integer tier,
            EnvironmentType environmentType,
            Boolean isOfficial,
            String name,
            String expand,
            Authentication auth) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Environment> environmentPage;

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (includeDeleted && roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            environmentPage = environmentRepository.findAllWithFilters(
                    expansionId, tier, environmentType, isOfficial, name, true, pageable);
        } else {
            environmentPage = environmentRepository.findAccessibleWithFilters(
                    user.getId(), expansionId, tier, environmentType, isOfficial, name, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<EnvironmentResponse>builder()
                .content(environmentPage.getContent().stream()
                        .map(environment -> toResponse(environment, expandSet))
                        .toList())
                .totalElements(environmentPage.getTotalElements())
                .totalPages(environmentPage.getTotalPages())
                .currentPage(environmentPage.getNumber())
                .pageSize(environmentPage.getSize())
                .build();
    }

    /**
     * Retrieves a single environment by ID.
     *
     * @param id The environment ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return EnvironmentResponse containing the environment details
     * @throws EntityNotFoundException if the environment is not found or not accessible
     */
    @Transactional(readOnly = true)
    public EnvironmentResponse getEnvironmentById(Long id, String expand, Authentication auth) {
        Environment environment = environmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + id));

        validateViewPermission(environment, auth);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(environment, expandSet);
    }

    /**
     * Creates a new environment.
     *
     * @param request The creation request containing environment details
     * @param auth Authentication context
     * @return EnvironmentResponse containing the created environment
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public EnvironmentResponse createEnvironment(CreateEnvironmentRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        validateDifficultyPresence(request.getDifficulty(), request.getDifficultySpecial());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Environment environment = Environment.builder()
                .name(request.getName())
                .tier(request.getTier())
                .environmentType(request.getEnvironmentType())
                .description(request.getDescription())
                .impulses(request.getImpulses())
                .difficulty(request.getDifficulty())
                .difficultySpecial(request.getDifficultySpecial())
                .potentialAdversaries(request.getPotentialAdversaries())
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(request.getIsOfficial() != null ? request.getIsOfficial() : false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            environment.setFeatures(resolvedFeatures);
        }

        Environment savedEnvironment = environmentRepository.save(environment);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedEnvironment, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(auth).withEntityType("environment").build(),
                "\"" + savedEnvironment.getName() + "\" (environment_id: " + savedEnvironment.getId() + ")");

        return toResponse(savedEnvironment, Set.of());
    }

    /**
     * Creates multiple environments in a bulk operation.
     *
     * @param requests List of creation requests
     * @param auth Authentication context
     * @return List of created environment responses
     */
    @Transactional
    public List<EnvironmentResponse> createEnvironmentsBulk(
            List<CreateEnvironmentRequest> requests, Authentication auth) {
        List<EnvironmentResponse> responses = requests.stream()
                .map(request -> createEnvironment(request, auth))
                .toList();

        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(auth).withEntityType("environment").build(),
                responses.size() + " environments created in bulk");

        return responses;
    }

    /**
     * Updates an existing environment.
     *
     * @param id The environment ID to update
     * @param request The update request containing new environment details
     * @param auth Authentication context
     * @return EnvironmentResponse containing the updated environment
     * @throws EntityNotFoundException if the environment or referenced entities are not found
     * @throws InsufficientPermissionsException if user lacks permission to modify
     */
    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, UpdateEnvironmentRequest request, Authentication auth) {
        Environment environment = environmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + id));

        validateModifyPermission(environment, auth);

        // Partial updates - only update non-null fields
        if (request.getName() != null) {
            environment.setName(request.getName());
        }
        if (request.getTier() != null) {
            environment.setTier(request.getTier());
        }
        if (request.getEnvironmentType() != null) {
            environment.setEnvironmentType(request.getEnvironmentType());
        }
        if (request.getDescription() != null) {
            environment.setDescription(request.getDescription());
        }
        if (request.getImpulses() != null) {
            environment.setImpulses(request.getImpulses());
        }

        // Difficulty / difficultySpecial: partial-update requests only apply non-null
        // fields, so an explicit "clear" flag is required to null out one side when
        // switching an environment between a numeric and a "Special" difficulty.
        if (Boolean.TRUE.equals(request.getClearDifficulty())) {
            environment.setDifficulty(null);
        } else if (request.getDifficulty() != null) {
            environment.setDifficulty(request.getDifficulty());
        }
        if (Boolean.TRUE.equals(request.getClearDifficultySpecial())) {
            environment.setDifficultySpecial(null);
        } else if (request.getDifficultySpecial() != null) {
            environment.setDifficultySpecial(request.getDifficultySpecial());
        }

        if (request.getPotentialAdversaries() != null) {
            environment.setPotentialAdversaries(request.getPotentialAdversaries());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            environment.setExpansion(expansion);
        }
        if (request.getIsOfficial() != null) {
            environment.setIsOfficial(request.getIsOfficial());
        }
        if (request.getIsPublic() != null) {
            environment.setIsPublic(request.getIsPublic());
        }

        // Validate difficulty presence after updates
        validateDifficultyPresence(environment.getDifficulty(), environment.getDifficultySpecial());

        Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(
                request.getFeatureIds(), request.getFeatures());
        if (resolvedUpdateFeatures != null) {
            environment.setFeatures(resolvedUpdateFeatures);
        }

        Environment updatedEnvironment = environmentRepository.save(environment);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedEnvironment, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(auth).withEntityType("environment").build(),
                "environment_id: " + updatedEnvironment.getId());

        return toResponse(updatedEnvironment, Set.of());
    }

    /**
     * Soft deletes an environment.
     *
     * @param id The environment ID to delete
     * @param auth Authentication context
     * @throws EntityNotFoundException if the environment is not found
     * @throws InsufficientPermissionsException if user lacks permission to delete
     */
    @Transactional
    public void deleteEnvironment(Long id, Authentication auth) {
        Environment environment = environmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + id));

        validateModifyPermission(environment, auth);

        environment.softDelete();
        environmentRepository.save(environment);
        eventPublisher.publishEvent(new EntityChangeEvent(this, environment, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(auth).withEntityType("environment").build(),
                "environment_id: " + id);
    }

    /**
     * Restores a soft-deleted environment.
     * Only ADMIN or OWNER can restore environments.
     *
     * @param id The environment ID to restore
     * @param auth Authentication context
     * @return EnvironmentResponse containing the restored environment
     * @throws EntityNotFoundException if the environment is not found
     * @throws IllegalStateException if the environment is not deleted
     * @throws InsufficientPermissionsException if user lacks ADMIN+ role
     */
    @Transactional
    public EnvironmentResponse restoreEnvironment(Long id, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (!roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            throw new InsufficientPermissionsException("Only admins can restore environments");
        }

        Environment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Environment not found with id: " + id));

        if (!environment.isDeleted()) {
            throw new IllegalStateException("Environment with id " + id + " is not deleted");
        }

        environment.restore();
        Environment restoredEnvironment = environmentRepository.save(environment);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredEnvironment, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(auth).withEntityType("environment").build(),
                "environment_id: " + id);

        return toResponse(restoredEnvironment, Set.of());
    }

    /**
     * Validates that the authenticated user has permission to modify the environment.
     * <ul>
     *   <li>Official environments: Only OWNER role can modify</li>
     *   <li>Non-official environments: Creator OR MODERATOR+ role can modify</li>
     * </ul>
     *
     * @param environment The environment being modified
     * @param auth Authentication context
     * @throws InsufficientPermissionsException if user lacks permission
     */
    private void validateModifyPermission(Environment environment, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (environment.getIsOfficial()) {
            if (user.getRole() != Role.OWNER) {
                throw new InsufficientPermissionsException(
                        "Only owners can modify official environments");
            }
        } else {
            boolean isCreator = environment.getCreatedBy().getId().equals(user.getId());
            boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

            if (!isCreator && !isModeratorPlus) {
                throw new InsufficientPermissionsException(
                        "You do not have permission to modify this environment");
            }
        }
    }

    /**
     * Validates that the authenticated user has permission to view the environment.
     * Private non-official environments are only visible to creator and moderators+.
     *
     * @param environment The environment being viewed
     * @param auth Authentication context
     * @throws EntityNotFoundException if user cannot view the environment
     */
    private void validateViewPermission(Environment environment, Authentication auth) {
        if (environment.getIsOfficial() || environment.getIsPublic()) {
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        boolean isCreator = environment.getCreatedBy().getId().equals(user.getId());
        boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

        if (!isCreator && !isModeratorPlus) {
            throw new EntityNotFoundException("Environment not found with id: " + environment.getId());
        }
    }

    /**
     * Validates that exactly one of {@code difficulty} or {@code difficultySpecial} is set.
     * <p>
     * Unlike an adversary's difficulty (which can legitimately be entirely absent for a
     * "framework" stat block), every printed environment card shows a Difficulty entry --
     * either a number or the literal text "Special" with a rules callout. Neither field
     * being set would silently discard printed information; both being set would be an
     * ambiguous double-entry that the physical card never has.
     *
     * @param difficulty the numeric difficulty, or null
     * @param difficultySpecial the verbatim printed Difficulty text, or null
     * @throws IllegalArgumentException if zero or both of the two are set
     */
    private void validateDifficultyPresence(Integer difficulty, String difficultySpecial) {
        boolean hasDifficulty = difficulty != null;
        boolean hasDifficultySpecial = difficultySpecial != null && !difficultySpecial.isBlank();

        if (hasDifficulty == hasDifficultySpecial) {
            log.warn("Difficulty presence validation failed: difficulty={}, difficultySpecial={}",
                    difficulty, difficultySpecial);
            throw new IllegalArgumentException(
                    "Exactly one of difficulty or difficultySpecial must be provided");
        }
    }

    /**
     * Converts an Environment entity to EnvironmentResponse DTO.
     */
    private EnvironmentResponse toResponse(Environment environment, Set<String> expand) {
        EnvironmentResponse.EnvironmentResponseBuilder builder = EnvironmentResponse.builder()
                .id(environment.getId())
                .name(environment.getName())
                .tier(environment.getTier())
                .environmentType(environment.getEnvironmentType())
                .description(environment.getDescription())
                .impulses(environment.getImpulses())
                .difficulty(environment.getDifficulty())
                .difficultySpecial(environment.getDifficultySpecial())
                .potentialAdversaries(environment.getPotentialAdversaries())
                .isOfficial(environment.getIsOfficial())
                .isPublic(environment.getIsPublic())
                .expansionId(environment.getExpansion().getId())
                .creatorId(environment.getCreatedBy().getId())
                .createdAt(environment.getCreatedAt())
                .lastModifiedAt(environment.getLastModifiedAt())
                .deletedAt(environment.getDeletedAt());

        if (environment.getFeatures() != null && !environment.getFeatures().isEmpty()) {
            builder.featureIds(environment.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = environment.getExpansion();
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
            User creator = environment.getCreatedBy();
            builder.creator(UserResponse.builder()
                    .id(creator.getId())
                    .username(creator.getUsername())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "features") && environment.getFeatures() != null
                && !environment.getFeatures().isEmpty()) {
            builder.features(environment.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
