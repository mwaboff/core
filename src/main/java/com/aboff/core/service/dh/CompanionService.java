package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.CreateCompanionTrainingRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.dh.response.CompanionTrainingResponse;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.CompanionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Service for managing Companion entities.
 * <p>
 * Handles business logic for CRUD operations on character companions, including
 * access control validation, soft deletion, pagination, filtering, Training selections,
 * and relationship expansion.
 * </p>
 * <p>
 * Access control (owner-or-privileged, uniformly on every operation including reads):
 * a caller must be the owning character sheet's owner OR hold MODERATOR/ADMIN/OWNER role.
 * A companion previously leaked every user's companions -- including Experience text -- to
 * any logged-in visitor via an unauthenticated, unfiltered list/get; every method here is
 * scoped to a specific character sheet and access-checked against it.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CompanionService {

    private final CompanionRepository companionRepository;
    private final CharacterSheetRepository characterSheetRepository;
    private final RoleHierarchyService roleHierarchyService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of a character sheet's active (non-soft-deleted) companions.
     * <p>
     * {@code characterSheetId} is required: this endpoint is scoped to one character sheet and
     * access-checked against it, never a global unfiltered listing.
     * </p>
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param characterSheetId The character sheet to list companions for (required)
     * @param expand Comma-separated list of relationships to expand (characterSheet, experiences)
     * @param auth The authentication object containing the current user
     * @return Paginated response containing the character sheet's active companions
     * @throws IllegalStateException if {@code characterSheetId} is null
     * @throws EntityNotFoundException if the character sheet is not found or is deleted
     * @throws InsufficientPermissionsException if the caller lacks permission to view the sheet's companions
     */
    @Transactional(readOnly = true)
    public PagedResponse<CompanionResponse> getAllCompanions(
            int page,
            int size,
            Long characterSheetId,
            String expand,
            Authentication auth) {

        if (characterSheetId == null) {
            throw new IllegalStateException("characterSheetId is required");
        }

        CharacterSheet characterSheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));
        validateSheetAccess(characterSheet, auth, "view companions for");

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Companion> companionPage = companionRepository.findActiveByCharacterSheetId(characterSheetId, pageable);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<CompanionResponse>builder()
                .content(companionPage.getContent().stream()
                        .map(companion -> toResponse(companion, expandSet))
                        .toList())
                .totalElements(companionPage.getTotalElements())
                .totalPages(companionPage.getTotalPages())
                .currentPage(companionPage.getNumber())
                .pageSize(companionPage.getSize())
                .build();
    }

    /**
     * Retrieves a single active (non-soft-deleted) companion by ID.
     *
     * @param id The companion ID
     * @param expand Comma-separated list of relationships to expand (characterSheet, experiences)
     * @param auth The authentication object containing the current user
     * @return CompanionResponse containing the companion details
     * @throws EntityNotFoundException if the companion is not found or is soft-deleted
     * @throws InsufficientPermissionsException if the caller lacks permission to view the companion
     */
    @Transactional(readOnly = true)
    public CompanionResponse getCompanionById(Long id, String expand, Authentication auth) {
        Companion companion = findActiveCompanionOrThrow(id);
        validateAccess(companion, auth, "view");

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(companion, expandSet);
    }

    /**
     * Creates a new companion for a character.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can create a companion.
     * </p>
     *
     * @param request The creation request containing companion details
     * @param auth The authentication object containing the current user
     * @return CompanionResponse containing the created companion
     * @throws EntityNotFoundException if the character sheet is not found or is deleted
     * @throws InsufficientPermissionsException if the user lacks permission to create
     * @throws IllegalStateException if {@code stressMarked} would exceed the companion's stress max,
     *         or if {@code damageType} is {@link DamageType#PHYSICAL_AND_MAGIC}
     */
    @Transactional
    public CompanionResponse createCompanion(CreateCompanionRequest request, Authentication auth) {
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(request.getCharacterSheetId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "CharacterSheet not found with id: " + request.getCharacterSheetId()));
        validateSheetAccess(characterSheet, auth, "create a companion for");

        DamageType damageType = request.getDamageType() != null ? request.getDamageType() : DamageType.PHYSICAL;
        validateDamageType(damageType);

        Companion companion = Companion.builder()
                .characterSheet(characterSheet)
                .name(request.getName())
                .description(request.getDescription())
                .baseEvasion(request.getEvasion() != null ? request.getEvasion() : 10)
                .attackName(request.getAttackName())
                .baseAttackRange(request.getAttackRange())
                .baseDamageDice(request.getDamageDice())
                .damageType(damageType)
                .baseStressMax(request.getStressMax() != null ? request.getStressMax() : 3)
                .stressMarked(request.getStressMarked() != null ? request.getStressMarked() : 0)
                .build();

        validateStressMarkedWithinMax(companion);

        Companion savedCompanion = companionRepository.save(companion);

        auditLogger.log(AuditAction.COMPANION_CREATED,
                AuditContext.forUser(auth).withCharacterSheetId(characterSheet.getId()).build(),
                "\"" + savedCompanion.getName() + "\" (companion_id: " + savedCompanion.getId()
                        + ", character_sheet_id: " + characterSheet.getId() + ")");

        return toResponse(savedCompanion, Set.of());
    }

    /**
     * Updates an existing companion.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update a companion. Supports partial updates - only non-null fields
     * are updated.
     * </p>
     *
     * @param id The companion ID to update
     * @param request The update request containing new companion details
     * @param auth The authentication object containing the current user
     * @return CompanionResponse containing the updated companion
     * @throws EntityNotFoundException if the companion is not found or is soft-deleted
     * @throws InsufficientPermissionsException if the user lacks permission to update
     * @throws IllegalStateException if the resulting {@code stressMarked} would exceed the companion's
     *         derived stress max, or if {@code damageType} is {@link DamageType#PHYSICAL_AND_MAGIC}
     */
    @Transactional
    public CompanionResponse updateCompanion(Long id, UpdateCompanionRequest request, Authentication auth) {
        Companion companion = findActiveCompanionOrThrow(id);
        validateAccess(companion, auth, "update");

        if (request.getName() != null) {
            companion.setName(request.getName());
        }
        if (request.getDescription() != null) {
            companion.setDescription(request.getDescription());
        }
        if (request.getEvasion() != null) {
            companion.setBaseEvasion(request.getEvasion());
        }
        if (request.getAttackName() != null) {
            companion.setAttackName(request.getAttackName());
        }
        if (request.getAttackRange() != null) {
            companion.setBaseAttackRange(request.getAttackRange());
        }
        if (request.getDamageDice() != null) {
            companion.setBaseDamageDice(request.getDamageDice());
        }
        if (request.getDamageType() != null) {
            validateDamageType(request.getDamageType());
            companion.setDamageType(request.getDamageType());
        }
        if (request.getStressMax() != null) {
            companion.setBaseStressMax(request.getStressMax());
        }
        if (request.getStressMarked() != null) {
            companion.setStressMarked(request.getStressMarked());
        }

        validateStressMarkedWithinMax(companion);

        Companion updatedCompanion = companionRepository.save(companion);

        auditLogger.log(AuditAction.COMPANION_UPDATED,
                AuditContext.forUser(auth).withCharacterSheetId(updatedCompanion.getCharacterSheet().getId()).build(),
                "companion_id: " + updatedCompanion.getId());

        return toResponse(updatedCompanion, Set.of());
    }

    /**
     * Soft-deletes a companion.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a companion. The companion is archived (its {@code deletedAt} timestamp is
     * set) rather than hard-deleted, so a level-down that later removes the granting subclass
     * feature can still restore it -- see {@link Companion#softDelete()}.
     * </p>
     *
     * @param id The companion ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the companion is not found or is already soft-deleted
     * @throws InsufficientPermissionsException if the user lacks permission to delete
     */
    @Transactional
    public void deleteCompanion(Long id, Authentication auth) {
        Companion companion = findActiveCompanionOrThrow(id);
        validateAccess(companion, auth, "delete");

        companion.softDelete();
        companionRepository.save(companion);

        // A deleted companion's LIGHT_IN_THE_DARK Training (if any) no longer grants its bonus
        // Hope slot -- clamp the owning sheet's hopeMarked back down if it now overflows.
        CharacterSheet characterSheet = companion.getCharacterSheet();
        List<Companion> remainingActive = companionRepository.findActiveByCharacterSheetId(characterSheet.getId());
        CompanionDerivationService.clampHopeMarked(characterSheet, remainingActive);
        characterSheetRepository.save(characterSheet);

        auditLogger.log(AuditAction.COMPANION_DELETED,
                AuditContext.forUser(auth).withCharacterSheetId(companion.getCharacterSheet().getId()).build(),
                "companion_id: " + id);
    }

    /**
     * Adds a single Training selection to a companion via the manual/GM path.
     * <p>
     * The Training's {@code acquiredAtLevel} is set to the owning character sheet's current
     * level automatically -- this endpoint is intentionally outside the level-up advancement
     * log, so manually-added Training is never reversed by level-down (see the companions
     * implementation plan, section 3.8 and 5.4).
     * </p>
     *
     * @param companionId The companion to add the Training selection to
     * @param request The Training selection to add
     * @param auth The authentication object containing the current user
     * @return CompanionResponse containing the companion with its updated Training and derived stats
     * @throws EntityNotFoundException if the companion is not found or is soft-deleted
     * @throws InsufficientPermissionsException if the user lacks permission to modify the companion
     * @throws IllegalStateException if the pick violates {@link CompanionTrainingValidator#validatePick}
     */
    @Transactional
    public CompanionResponse addTraining(Long companionId, CreateCompanionTrainingRequest request, Authentication auth) {
        Companion companion = findActiveCompanionOrThrow(companionId);
        validateAccess(companion, auth, "add training to");

        CompanionTrainingValidator.validatePick(companion, request.getOption(), request.getViciousAxis(), request.getTargetExperienceId());

        Experience targetExperience = null;
        if (request.getOption() == CompanionTrainingOption.INTELLIGENT) {
            targetExperience = companion.getExperiences().stream()
                    .filter(experience -> experience.getId().equals(request.getTargetExperienceId()))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Experience not found on this companion with id: " + request.getTargetExperienceId()));
        }

        CompanionTraining training = CompanionTraining.builder()
                .companion(companion)
                .option(request.getOption())
                .viciousAxis(request.getViciousAxis())
                .targetExperience(targetExperience)
                .acquiredAtLevel(companion.getCharacterSheet().getLevel())
                .build();
        companion.getTrainings().add(training);

        Companion savedCompanion = companionRepository.save(companion);

        auditLogger.log(AuditAction.COMPANION_TRAINING_ADDED,
                AuditContext.forUser(auth).withCharacterSheetId(companion.getCharacterSheet().getId()).build(),
                "companion_id: " + companionId + ", option: " + request.getOption());

        return toResponse(savedCompanion, Set.of());
    }

    /**
     * Removes a single Training selection from a companion via the manual/GM path.
     * <p>
     * Mutates through the parent collection ({@code companion.getTrainings().removeIf(...)})
     * rather than deleting the row directly, since {@code CompanionTraining} is an
     * {@code orphanRemoval} child of {@code Companion} -- a direct repository delete would be
     * resurrected by the next cascade save of this already-loaded parent. See
     * {@code core/docs/agent-plans/2026-03-15-leveldown-domain-card-fix-design.md}.
     * </p>
     *
     * @param companionId The companion to remove the Training selection from
     * @param trainingId The Training selection to remove
     * @param auth The authentication object containing the current user
     * @return CompanionResponse containing the companion with its updated Training and derived stats
     * @throws EntityNotFoundException if the companion or the Training selection is not found
     * @throws InsufficientPermissionsException if the user lacks permission to modify the companion
     */
    @Transactional
    public CompanionResponse removeTraining(Long companionId, Long trainingId, Authentication auth) {
        Companion companion = findActiveCompanionOrThrow(companionId);
        validateAccess(companion, auth, "remove training from");

        boolean removed = companion.getTrainings().removeIf(training -> training.getId().equals(trainingId));
        if (!removed) {
            throw new EntityNotFoundException(
                    "Training not found with id: " + trainingId + " on companion: " + companionId);
        }

        companion.setStressMarked(Math.min(companion.getStressMarked(), CompanionDerivationService.stressMax(companion)));

        Companion savedCompanion = companionRepository.save(companion);

        // A removed LIGHT_IN_THE_DARK Training no longer grants its bonus Hope slot -- clamp the
        // owning sheet's hopeMarked back down if it now overflows.
        CharacterSheet characterSheet = savedCompanion.getCharacterSheet();
        List<Companion> activeCompanions = companionRepository.findActiveByCharacterSheetId(characterSheet.getId());
        CompanionDerivationService.clampHopeMarked(characterSheet, activeCompanions);
        characterSheetRepository.save(characterSheet);

        auditLogger.log(AuditAction.COMPANION_TRAINING_REMOVED,
                AuditContext.forUser(auth).withCharacterSheetId(companion.getCharacterSheet().getId()).build(),
                "companion_id: " + companionId + ", training_id: " + trainingId);

        return toResponse(savedCompanion, Set.of());
    }

    /**
     * Loads a companion by id, treating a soft-deleted companion the same as a missing one.
     *
     * @param id the companion ID
     * @return the active companion
     * @throws EntityNotFoundException if no active companion exists with that id
     */
    private Companion findActiveCompanionOrThrow(Long id) {
        Companion companion = companionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Companion not found with id: " + id));
        if (companion.isDeleted()) {
            throw new EntityNotFoundException("Companion not found with id: " + id);
        }
        return companion;
    }

    /**
     * Validates that {@code stressMarked} does not exceed the companion's Training-adjusted
     * Stress max. Bean Validation bounds on the request DTOs only cover each field in
     * isolation, so this cross-field rule is enforced here, against the value that will
     * actually be persisted.
     *
     * @param companion the companion to validate, with all pending field changes already applied
     * @throws IllegalStateException if {@code stressMarked} exceeds the derived Stress max
     */
    private void validateStressMarkedWithinMax(Companion companion) {
        int derivedStressMax = CompanionDerivationService.stressMax(companion);
        if (companion.getStressMarked() > derivedStressMax) {
            throw new IllegalStateException(
                    "stressMarked (" + companion.getStressMarked() + ") must not exceed stressMax (" + derivedStressMax + ")");
        }
    }

    /**
     * Validates that a damage type is a valid choice for a companion's attack.
     * <p>
     * Per the printed rule, a companion's attack deals either physical or magic damage -- a
     * one-time either/or choice made when the companion is created (core-01:1327).
     * {@link DamageType#PHYSICAL_AND_MAGIC} is the "Otherworldly" per-attack weapon mechanic
     * (see its Javadoc), not a companion concept, so it is rejected here before anything is
     * persisted.
     * </p>
     *
     * @param damageType the damage type to validate
     * @throws IllegalStateException if {@code damageType} is {@link DamageType#PHYSICAL_AND_MAGIC}
     */
    private void validateDamageType(DamageType damageType) {
        if (damageType == DamageType.PHYSICAL_AND_MAGIC) {
            throw new IllegalStateException(
                    "Companion damage type must be PHYSICAL or MAGIC, not PHYSICAL_AND_MAGIC "
                            + "(a companion's attack is always one or the other, not a per-attack choice)");
        }
    }

    /**
     * Validates that the current user has access to act on a character sheet's companions.
     * Access is granted if the user is the character sheet owner OR has a
     * MODERATOR/ADMIN/OWNER role. This is the single authorization pattern reused by every
     * companion operation, including the companion-scoped overload below.
     *
     * @param characterSheet The character sheet whose companions are being acted on
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for the error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateSheetAccess(CharacterSheet characterSheet, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Long ownerId = characterSheet.getOwner().getId();
        boolean isOwner = ownerId.equals(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isOwner && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this character sheet");
        }
    }

    /**
     * Validates that the current user has access to act on a specific companion, by deferring
     * to {@link #validateSheetAccess(CharacterSheet, Authentication, String)} on the
     * companion's owning character sheet.
     *
     * @param companion The companion to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for the error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateAccess(Companion companion, Authentication auth, String operation) {
        try {
            validateSheetAccess(companion.getCharacterSheet(), auth, operation);
        } catch (InsufficientPermissionsException e) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this companion");
        }
    }

    /**
     * Converts a Companion entity to CompanionResponse DTO.
     * <p>
     * Always includes IDs for relationships, the Training-derived stats (computed by
     * {@code CompanionDerivationService}), and the full Training list. Optionally expands full
     * relationship objects based on the expand set.
     * </p>
     * <p>
     * Public so other services can delegate to this single mapping (e.g.
     * {@code CharacterSheetService.toResponse} builds its {@code companions} expansion by
     * calling this method per companion), the same delegate-and-pass-expand-down pattern
     * {@code CharacterSheetService.toWeaponResponse} uses for weapons. Do not re-implement this
     * mapping elsewhere -- a duplicated copy in {@code ExperienceService} already drifted out of
     * sync with the real field names once.
     * </p>
     *
     * @param companion The companion entity
     * @param expand Set of relationships to expand
     * @return CompanionResponse DTO
     */
    public CompanionResponse toResponse(Companion companion, Set<String> expand) {
        CompanionResponse.CompanionResponseBuilder builder = CompanionResponse.builder()
                .id(companion.getId())
                .characterSheetId(companion.getCharacterSheet().getId())
                .name(companion.getName())
                .description(companion.getDescription())
                .evasion(CompanionDerivationService.evasion(companion))
                .baseEvasion(companion.getBaseEvasion())
                .attackName(companion.getAttackName())
                .attackRange(CompanionDerivationService.attackRange(companion))
                .baseAttackRange(companion.getBaseAttackRange())
                .damageDice(CompanionDerivationService.damageDice(companion))
                .baseDamageDice(companion.getBaseDamageDice())
                .attackDiceCount(companion.getCharacterSheet().getProficiency())
                .damageType(companion.getDamageType())
                .stressMax(CompanionDerivationService.stressMax(companion))
                .baseStressMax(companion.getBaseStressMax())
                .stressMarked(companion.getStressMarked())
                .outOfScene(CompanionDerivationService.outOfScene(companion))
                .origin(companion.getOrigin())
                .advancesOnLevelUp(companion.getAdvancesOnLevelUp())
                .trainings(companion.getTrainings().stream()
                        .sorted(Comparator.comparing(CompanionTraining::getId))
                        .map(this::toTrainingResponse)
                        .toList())
                .remainingByOption(CompanionDerivationService.remainingByOption(companion))
                .createdAt(companion.getCreatedAt())
                .lastModifiedAt(companion.getLastModifiedAt());

        if (ExpandUtil.shouldExpand(expand, "characterSheet")) {
            CharacterSheet sheet = companion.getCharacterSheet();
            builder.characterSheet(CharacterSheetResponse.builder()
                    .id(sheet.getId())
                    .name(sheet.getName())
                    .pronouns(sheet.getPronouns())
                    .level(sheet.getLevel())
                    .ownerId(sheet.getOwner().getId())
                    .createdAt(sheet.getCreatedAt())
                    .lastModifiedAt(sheet.getLastModifiedAt())
                    .deletedAt(sheet.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "experiences")) {
            builder.experiences(companion.getExperiences().stream()
                    .map(experience -> ExperienceResponse.builder()
                            .id(experience.getId())
                            .companionId(experience.getCompanion() != null ? experience.getCompanion().getId() : null)
                            .createdById(experience.getCreatedBy().getId())
                            .description(experience.getDescription())
                            .modifier(experience.getModifier())
                            .createdAt(experience.getCreatedAt())
                            .lastModifiedAt(experience.getLastModifiedAt())
                            .build())
                    .toList());
        }

        return builder.build();
    }

    /**
     * Converts a CompanionTraining entity to CompanionTrainingResponse DTO.
     *
     * @param training The Training selection entity
     * @return CompanionTrainingResponse DTO
     */
    private CompanionTrainingResponse toTrainingResponse(CompanionTraining training) {
        return CompanionTrainingResponse.builder()
                .id(training.getId())
                .option(training.getOption())
                .viciousAxis(training.getViciousAxis())
                .targetExperienceId(training.getTargetExperience() != null ? training.getTargetExperience().getId() : null)
                .acquiredAtLevel(training.getAcquiredAtLevel())
                .build();
    }
}
