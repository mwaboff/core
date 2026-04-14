package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.enums.AuditAction;
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

import java.util.Set;

/**
 * Service for managing Companion entities.
 * <p>
 * Handles business logic for CRUD operations on character companions, including
 * access control validation, pagination, filtering, and relationship expansion.
 * </p>
 * <p>
 * Access control:
 * - Create: Character sheet owner OR users with MODERATOR/ADMIN/OWNER role
 * - Read: Any authenticated user
 * - Update/Delete: Character sheet owner OR users with MODERATOR/ADMIN/OWNER role
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
     * Retrieves a paginated list of companions.
     * <p>
     * Optionally filters by character sheet ID. All authenticated users can
     * view companions.
     * </p>
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param characterSheetId Optional filter for character sheet ID
     * @param expand Comma-separated list of relationships to expand (characterSheet, experiences)
     * @return Paginated response containing companions
     */
    @Transactional(readOnly = true)
    public PagedResponse<CompanionResponse> getAllCompanions(
            int page,
            int size,
            Long characterSheetId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Companion> companionPage;
        if (characterSheetId != null) {
            // Verify character sheet exists and is not deleted
            characterSheetRepository.findActiveById(characterSheetId)
                    .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

            // Filter by character sheet
            companionPage = companionRepository.findByCharacterSheetId(characterSheetId, pageable);
        } else {
            companionPage = companionRepository.findAll(pageable);
        }

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
     * Retrieves a single companion by ID.
     *
     * @param id The companion ID
     * @param expand Comma-separated list of relationships to expand (characterSheet, experiences)
     * @return CompanionResponse containing the companion details
     * @throws EntityNotFoundException if the companion is not found
     */
    @Transactional(readOnly = true)
    public CompanionResponse getCompanionById(Long id, String expand) {
        Companion companion = companionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Companion not found with id: " + id));

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
     */
    @Transactional
    public CompanionResponse createCompanion(CreateCompanionRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        // Verify character sheet exists and is not deleted
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(request.getCharacterSheetId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "CharacterSheet not found with id: " + request.getCharacterSheetId()));

        // Validate access - must be character sheet owner or moderator+
        Long ownerId = characterSheet.getOwner().getId();
        boolean isOwner = ownerId.equals(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isOwner && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to create a companion for this character sheet");
        }

        // Build the companion
        Companion companion = Companion.builder()
                .characterSheet(characterSheet)
                .name(request.getName())
                .description(request.getDescription())
                .evasion(request.getEvasion() != null ? request.getEvasion() : 0)
                .attackName(request.getAttackName())
                .attackRange(request.getAttackRange())
                .damageDice(request.getDamageDice())
                .stressMax(request.getStressMax() != null ? request.getStressMax() : 3)
                .stressMarked(request.getStressMarked() != null ? request.getStressMarked() : 0)
                .build();

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
     * @throws EntityNotFoundException if the companion is not found
     * @throws InsufficientPermissionsException if the user lacks permission to update
     */
    @Transactional
    public CompanionResponse updateCompanion(Long id, UpdateCompanionRequest request, Authentication auth) {
        Companion companion = companionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Companion not found with id: " + id));

        // Validate access - must be character sheet owner or moderator+
        validateAccess(companion, auth, "update");

        // Update only non-null fields
        if (request.getName() != null) {
            companion.setName(request.getName());
        }
        if (request.getDescription() != null) {
            companion.setDescription(request.getDescription());
        }
        if (request.getEvasion() != null) {
            companion.setEvasion(request.getEvasion());
        }
        if (request.getAttackName() != null) {
            companion.setAttackName(request.getAttackName());
        }
        if (request.getAttackRange() != null) {
            companion.setAttackRange(request.getAttackRange());
        }
        if (request.getDamageDice() != null) {
            companion.setDamageDice(request.getDamageDice());
        }
        if (request.getStressMax() != null) {
            companion.setStressMax(request.getStressMax());
        }
        if (request.getStressMarked() != null) {
            companion.setStressMarked(request.getStressMarked());
        }

        Companion updatedCompanion = companionRepository.save(companion);

        auditLogger.log(AuditAction.COMPANION_UPDATED,
                AuditContext.forUser(auth).withCharacterSheetId(updatedCompanion.getCharacterSheet().getId()).build(),
                "companion_id: " + updatedCompanion.getId());

        return toResponse(updatedCompanion, Set.of());
    }

    /**
     * Deletes a companion (hard delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete a companion. This is a permanent deletion that cascades to
     * associated experiences.
     * </p>
     *
     * @param id The companion ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the companion is not found
     * @throws InsufficientPermissionsException if the user lacks permission to delete
     */
    @Transactional
    public void deleteCompanion(Long id, Authentication auth) {
        Companion companion = companionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Companion not found with id: " + id));

        // Validate access - must be character sheet owner or moderator+
        validateAccess(companion, auth, "delete");

        companionRepository.delete(companion);

        auditLogger.log(AuditAction.COMPANION_DELETED,
                AuditContext.forUser(auth).withCharacterSheetId(companion.getCharacterSheet().getId()).build(),
                "companion_id: " + id);
    }

    /**
     * Validates that the current user has access to modify the companion.
     * <p>
     * Access is granted if the user is the character sheet owner OR has a
     * MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param companion The companion to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateAccess(Companion companion, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Long ownerId = companion.getCharacterSheet().getOwner().getId();
        boolean isOwner = ownerId.equals(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isOwner && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this companion");
        }
    }

    /**
     * Converts a Companion entity to CompanionResponse DTO.
     * <p>
     * Always includes IDs for relationships. Optionally expands full relationship
     * objects based on the expand set.
     * </p>
     *
     * @param companion The companion entity
     * @param expand Set of relationships to expand
     * @return CompanionResponse DTO
     */
    private CompanionResponse toResponse(Companion companion, Set<String> expand) {
        CompanionResponse.CompanionResponseBuilder builder = CompanionResponse.builder()
                .id(companion.getId())
                .characterSheetId(companion.getCharacterSheet().getId())
                .name(companion.getName())
                .description(companion.getDescription())
                .evasion(companion.getEvasion())
                .attackName(companion.getAttackName())
                .attackRange(companion.getAttackRange())
                .damageDice(companion.getDamageDice())
                .stressMax(companion.getStressMax())
                .stressMarked(companion.getStressMarked())
                .createdAt(companion.getCreatedAt())
                .lastModifiedAt(companion.getLastModifiedAt());

        // Expand character sheet if requested
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

        // Expand experiences if requested
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
}
