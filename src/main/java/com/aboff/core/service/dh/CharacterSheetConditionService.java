package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetConditionResponse;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.CharacterSheetCondition;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.CharacterSheetConditionRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ConditionRepository;
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
 * Service for managing CharacterSheetCondition entities — a character's per-instance
 * conditions, each carrying its own {@code magnitude}.
 * <p>
 * Modelled on {@link ExperienceService} rather than folded into
 * {@link CharacterSheetService}: like an experience, a condition instance carries its own
 * per-row data (the magnitude), so it gets its own dedicated CRUD surface instead of being
 * baked into the character sheet's own create/update payload.
 * </p>
 * <p>
 * Access control:
 * - Read: Any authenticated user
 * - Create/Update/Delete: Character sheet owner OR users with MODERATOR/ADMIN/OWNER role
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CharacterSheetConditionService {

    private final CharacterSheetConditionRepository characterSheetConditionRepository;
    private final CharacterSheetRepository characterSheetRepository;
    private final ConditionRepository conditionRepository;
    private final ConditionService conditionService;
    private final RoleHierarchyService roleHierarchyService;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of condition instances for a character sheet.
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param characterSheetId The character sheet ID to filter by
     * @param expand Comma-separated list of relationships to expand (characterSheet, condition)
     * @return Paginated response containing condition instances
     * @throws EntityNotFoundException if the character sheet is not found
     */
    @Transactional(readOnly = true)
    public PagedResponse<CharacterSheetConditionResponse> getConditionsForCharacterSheet(
            int page, int size, Long characterSheetId, String expand) {

        characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<CharacterSheetCondition> instancePage =
                characterSheetConditionRepository.findByCharacterSheetId(characterSheetId, pageable);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<CharacterSheetConditionResponse>builder()
                .content(instancePage.getContent().stream()
                        .map(instance -> toResponse(instance, expandSet))
                        .toList())
                .totalElements(instancePage.getTotalElements())
                .totalPages(instancePage.getTotalPages())
                .currentPage(instancePage.getNumber())
                .pageSize(instancePage.getSize())
                .build();
    }

    /**
     * Retrieves a single condition instance by ID.
     *
     * @param id The condition instance ID
     * @param expand Comma-separated list of relationships to expand
     * @return CharacterSheetConditionResponse containing the instance details
     * @throws EntityNotFoundException if the instance is not found
     */
    @Transactional(readOnly = true)
    public CharacterSheetConditionResponse getConditionInstanceById(Long id, String expand) {
        CharacterSheetCondition instance = characterSheetConditionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheetCondition not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(instance, expandSet);
    }

    /**
     * Attaches a condition instance to a character sheet.
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can attach a
     * condition -- the sibling {@link #updateCharacterSheetCondition} and
     * {@link #deleteCharacterSheetCondition} methods are already gated this way; without this
     * check any authenticated user could attach a condition to someone else's sheet.
     *
     * @param request The creation request containing character sheet, condition, and magnitude
     * @param auth The authentication object containing the current user
     * @return CharacterSheetConditionResponse containing the created instance
     * @throws EntityNotFoundException if the character sheet or condition is not found
     * @throws InsufficientPermissionsException if the user lacks permission to attach a condition
     */
    @Transactional
    public CharacterSheetConditionResponse createCharacterSheetCondition(
            CreateCharacterSheetConditionRequest request, Authentication auth) {

        CharacterSheet characterSheet = characterSheetRepository.findActiveById(request.getCharacterSheetId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "CharacterSheet not found with id: " + request.getCharacterSheetId()));
        validateSheetAccess(characterSheet, auth, "attach a condition to");

        Condition condition = conditionRepository.findByIdAndDeletedAtIsNull(request.getConditionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Condition not found with id: " + request.getConditionId()));

        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .characterSheet(characterSheet)
                .condition(condition)
                .magnitude(request.getMagnitude())
                .build();

        CharacterSheetCondition savedInstance = characterSheetConditionRepository.save(instance);

        auditLogger.log(AuditAction.CONTENT_CREATED,
                AuditContext.forUser(auth).withEntityType("characterSheetCondition").build(),
                "condition_id: " + condition.getId() + " attached to character_sheet_id: " + characterSheet.getId());

        return toResponse(savedInstance, Set.of());
    }

    /**
     * Updates the magnitude of an existing condition instance.
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can update.
     *
     * @param id The condition instance ID to update
     * @param request The update request containing the new magnitude
     * @param auth The authentication object containing the current user
     * @return CharacterSheetConditionResponse containing the updated instance
     * @throws EntityNotFoundException if the instance is not found
     * @throws InsufficientPermissionsException if the user lacks permission to update
     */
    @Transactional
    public CharacterSheetConditionResponse updateCharacterSheetCondition(
            Long id, UpdateCharacterSheetConditionRequest request, Authentication auth) {

        CharacterSheetCondition instance = characterSheetConditionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheetCondition not found with id: " + id));

        validateAccess(instance, auth, "update");

        if (request.getMagnitude() != null) {
            instance.setMagnitude(request.getMagnitude());
        }

        CharacterSheetCondition updatedInstance = characterSheetConditionRepository.save(instance);

        auditLogger.log(AuditAction.CONTENT_UPDATED,
                AuditContext.forUser(auth).withEntityType("characterSheetCondition").build(),
                "character_sheet_condition_id: " + updatedInstance.getId());

        return toResponse(updatedInstance, Set.of());
    }

    /**
     * Removes a condition instance from a character sheet (hard delete).
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role can remove.
     *
     * @param id The condition instance ID to remove
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the instance is not found
     * @throws InsufficientPermissionsException if the user lacks permission to remove
     */
    @Transactional
    public void deleteCharacterSheetCondition(Long id, Authentication auth) {
        CharacterSheetCondition instance = characterSheetConditionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheetCondition not found with id: " + id));

        validateAccess(instance, auth, "remove");

        characterSheetConditionRepository.delete(instance);

        auditLogger.log(AuditAction.CONTENT_DELETED,
                AuditContext.forUser(auth).withEntityType("characterSheetCondition").build(),
                "character_sheet_condition_id: " + id);
    }

    /**
     * Validates that the current user has access to modify a condition instance, by deferring to
     * {@link #validateSheetAccess(CharacterSheet, Authentication, String)} on the instance's
     * owning character sheet.
     *
     * @param instance The condition instance to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for the error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateAccess(CharacterSheetCondition instance, Authentication auth, String operation) {
        try {
            validateSheetAccess(instance.getCharacterSheet(), auth, operation);
        } catch (InsufficientPermissionsException e) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this condition instance");
        }
    }

    /**
     * Validates that the current user has access to attach a condition to a character sheet.
     * Access is granted if the user owns the character sheet or has a MODERATOR/ADMIN/OWNER role.
     *
     * @param characterSheet The character sheet to validate access for
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
     * Converts a CharacterSheetCondition entity to CharacterSheetConditionResponse DTO.
     *
     * @param instance The condition instance entity
     * @param expand Set of relationships to expand
     * @return CharacterSheetConditionResponse DTO
     */
    private CharacterSheetConditionResponse toResponse(CharacterSheetCondition instance, Set<String> expand) {
        CharacterSheetConditionResponse.CharacterSheetConditionResponseBuilder builder =
                CharacterSheetConditionResponse.builder()
                        .id(instance.getId())
                        .characterSheetId(instance.getCharacterSheet().getId())
                        .conditionId(instance.getCondition().getId())
                        .magnitude(instance.getMagnitude())
                        .createdAt(instance.getCreatedAt())
                        .lastModifiedAt(instance.getLastModifiedAt());

        if (ExpandUtil.shouldExpand(expand, "characterSheet")) {
            CharacterSheet sheet = instance.getCharacterSheet();
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

        if (ExpandUtil.shouldExpand(expand, "condition")) {
            // Delegate to ConditionService.toResponse so non-SRD conditions embedded on a
            // character sheet are redacted the same way the conditions catalogue is.
            builder.condition(conditionService.toResponse(instance.getCondition(), Set.of()));
        }

        return builder.build();
    }
}
