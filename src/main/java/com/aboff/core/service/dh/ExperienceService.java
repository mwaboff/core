package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateExperienceRequest;
import com.aboff.core.model.dto.dh.request.UpdateExperienceRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.repository.CharacterSheetRepository;
import com.aboff.core.repository.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.util.ExpandUtil;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing Experience entities.
 * <p>
 * Handles business logic for CRUD operations on character experiences, including
 * access control validation, pagination, filtering, and relationship expansion.
 * </p>
 * <p>
 * Access control:
 * - Create: Any authenticated user
 * - Read: Any authenticated user
 * - Update/Delete: Character sheet owner OR users with MODERATOR/ADMIN/OWNER role
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final CharacterSheetRepository characterSheetRepository;
    private final UserRepository userRepository;
    private final RoleHierarchyService roleHierarchyService;

    /**
     * Retrieves a paginated list of experiences.
     * <p>
     * Optionally filters by character sheet ID. All authenticated users can
     * view experiences.
     * </p>
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param characterSheetId Optional filter for character sheet ID
     * @param expand Comma-separated list of relationships to expand (characterSheet, createdBy)
     * @return Paginated response containing experiences
     */
    @Transactional(readOnly = true)
    public PagedResponse<ExperienceResponse> getAllExperiences(
            int page,
            int size,
            Long characterSheetId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Experience> experiencePage;
        if (characterSheetId != null) {
            // Verify character sheet exists and is not deleted
            characterSheetRepository.findActiveById(characterSheetId)
                    .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

            // Filter by character sheet
            experiencePage = experienceRepository.findByCharacterSheetId(characterSheetId, pageable);
        } else {
            experiencePage = experienceRepository.findAll(pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<ExperienceResponse>builder()
                .content(experiencePage.getContent().stream()
                        .map(experience -> toResponse(experience, expandSet))
                        .toList())
                .totalElements(experiencePage.getTotalElements())
                .totalPages(experiencePage.getTotalPages())
                .currentPage(experiencePage.getNumber())
                .pageSize(experiencePage.getSize())
                .build();
    }

    /**
     * Retrieves a single experience by ID.
     *
     * @param id The experience ID
     * @param expand Comma-separated list of relationships to expand (characterSheet, createdBy)
     * @return ExperienceResponse containing the experience details
     * @throws EntityNotFoundException if the experience is not found
     */
    @Transactional(readOnly = true)
    public ExperienceResponse getExperienceById(Long id, String expand) {
        Experience experience = experienceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(experience, expandSet);
    }

    /**
     * Creates a new experience for a character.
     * <p>
     * Any authenticated user can create an experience. The creating user is
     * recorded as the createdBy user.
     * </p>
     *
     * @param request The creation request containing experience details
     * @param auth The authentication object containing the current user
     * @return ExperienceResponse containing the created experience
     * @throws EntityNotFoundException if the character sheet is not found or is deleted
     */
    @Transactional
    public ExperienceResponse createExperience(CreateExperienceRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        log.info("Creating new experience for character sheet {} by user {}", request.getCharacterSheetId(), userId);

        // Verify character sheet exists and is not deleted
        CharacterSheet characterSheet = characterSheetRepository.findActiveById(request.getCharacterSheetId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "CharacterSheet not found with id: " + request.getCharacterSheetId()));

        // Get the current user
        User createdByUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Build the experience
        Experience experience = Experience.builder()
                .characterSheet(characterSheet)
                .createdBy(createdByUser)
                .description(request.getDescription())
                .modifier(request.getModifier() != null ? request.getModifier() : 2)
                .build();

        Experience savedExperience = experienceRepository.save(experience);
        log.info("Created experience with id: {} for character sheet {}", savedExperience.getId(), characterSheet.getId());

        return toResponse(savedExperience, Set.of());
    }

    /**
     * Updates an existing experience.
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can update an experience. Supports partial updates - only non-null fields
     * are updated.
     * </p>
     *
     * @param id The experience ID to update
     * @param request The update request containing new experience details
     * @param auth The authentication object containing the current user
     * @return ExperienceResponse containing the updated experience
     * @throws EntityNotFoundException if the experience is not found
     * @throws InsufficientPermissionsException if the user lacks permission to update
     */
    @Transactional
    public ExperienceResponse updateExperience(Long id, UpdateExperienceRequest request, Authentication auth) {
        log.info("Updating experience with id: {}", id);

        Experience experience = experienceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + id));

        // Validate access - must be character sheet owner or moderator+
        validateAccess(experience, auth, "update");

        // Update only non-null fields
        if (request.getDescription() != null) {
            experience.setDescription(request.getDescription());
        }
        if (request.getModifier() != null) {
            experience.setModifier(request.getModifier());
        }

        Experience updatedExperience = experienceRepository.save(experience);
        log.info("Updated experience with id: {}", updatedExperience.getId());

        return toResponse(updatedExperience, Set.of());
    }

    /**
     * Deletes an experience (hard delete).
     * <p>
     * Only the character sheet owner or users with MODERATOR/ADMIN/OWNER role
     * can delete an experience. This is a permanent deletion.
     * </p>
     *
     * @param id The experience ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the experience is not found
     * @throws InsufficientPermissionsException if the user lacks permission to delete
     */
    @Transactional
    public void deleteExperience(Long id, Authentication auth) {
        log.info("Deleting experience with id: {}", id);

        Experience experience = experienceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + id));

        // Validate access - must be character sheet owner or moderator+
        validateAccess(experience, auth, "delete");

        experienceRepository.delete(experience);
        log.info("Deleted experience with id: {}", id);
    }

    /**
     * Validates that the current user has access to modify the experience.
     * <p>
     * Access is granted if the user is the character sheet owner OR has a
     * MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param experience The experience to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateAccess(Experience experience, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Long ownerId = experience.getCharacterSheet().getOwner().getId();
        boolean isOwner = ownerId.equals(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isOwner && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this experience");
        }
    }

    /**
     * Converts an Experience entity to ExperienceResponse DTO.
     * <p>
     * Always includes IDs for relationships. Optionally expands full relationship
     * objects based on the expand set.
     * </p>
     *
     * @param experience The experience entity
     * @param expand Set of relationships to expand
     * @return ExperienceResponse DTO
     */
    private ExperienceResponse toResponse(Experience experience, Set<String> expand) {
        ExperienceResponse.ExperienceResponseBuilder builder = ExperienceResponse.builder()
                .id(experience.getId())
                .characterSheetId(experience.getCharacterSheet().getId())
                .createdById(experience.getCreatedBy().getId())
                .description(experience.getDescription())
                .modifier(experience.getModifier())
                .createdAt(experience.getCreatedAt())
                .lastModifiedAt(experience.getLastModifiedAt());

        // Expand character sheet if requested
        if (expand.contains("characterSheet")) {
            CharacterSheet sheet = experience.getCharacterSheet();
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

        // Expand created by user if requested
        if (expand.contains("createdBy")) {
            User user = experience.getCreatedBy();
            builder.createdBy(UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .timezone(user.getTimezone())
                    .createdAt(user.getCreatedAt())
                    .lastModifiedAt(user.getLastModifiedAt())
                    .build());
        }

        return builder.build();
    }
}
