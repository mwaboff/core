package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateEncounterRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.EncounterResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.CampaignRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.EncounterAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Encounter entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion,
 * relationship expansion, and permission validation.
 * </p>
 * <p>
 * Permission model:
 * </p>
 * <ul>
 *   <li>Official encounters: Only OWNER role can modify</li>
 *   <li>Non-official encounters: Creator OR MODERATOR+ can modify</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final EncounterAdversaryRepository encounterAdversaryRepository;
    private final AdversaryRepository adversaryRepository;
    private final CampaignRepository campaignRepository;
    private final RoleHierarchyService roleHierarchyService;

    /**
     * Retrieves a paginated list of encounters accessible to the authenticated user.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted encounters (ADMIN+ only)
     * @param campaignId Optional filter for campaign ID
     * @param tier Optional filter for tier (1-4)
     * @param isOfficial Optional filter for official status
     * @param name Optional filter for name (partial match)
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return Paginated response containing encounters
     */
    @Transactional(readOnly = true)
    public PagedResponse<EncounterResponse> getAllEncounters(
            int page,
            int size,
            boolean includeDeleted,
            Long campaignId,
            Integer tier,
            Boolean isOfficial,
            String name,
            String expand,
            Authentication auth) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Encounter> encounterPage;

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (includeDeleted && roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            encounterPage = encounterRepository.findAllWithFilters(
                    campaignId, tier, isOfficial, name, true, pageable);
        } else {
            encounterPage = encounterRepository.findAccessibleWithFilters(
                    user.getId(), campaignId, tier, isOfficial, name, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<EncounterResponse>builder()
                .content(encounterPage.getContent().stream()
                        .map(encounter -> toResponse(encounter, expandSet))
                        .toList())
                .totalElements(encounterPage.getTotalElements())
                .totalPages(encounterPage.getTotalPages())
                .currentPage(encounterPage.getNumber())
                .pageSize(encounterPage.getSize())
                .build();
    }

    /**
     * Retrieves a single encounter by ID.
     *
     * @param id The encounter ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth Authentication context
     * @return EncounterResponse containing the encounter details
     * @throws EntityNotFoundException if the encounter is not found or not accessible
     */
    @Transactional(readOnly = true)
    public EncounterResponse getEncounterById(Long id, String expand, Authentication auth) {
        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        validateViewPermission(encounter, auth);

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(encounter, expandSet);
    }

    /**
     * Creates a new encounter.
     *
     * @param request The creation request containing encounter details
     * @param auth Authentication context
     * @return EncounterResponse containing the created encounter
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public EncounterResponse createEncounter(CreateEncounterRequest request, Authentication auth) {
        log.info("Creating new encounter with name: {}", request.getName());

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        Encounter encounter = Encounter.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tier(request.getTier())
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .encounterAdversaries(new ArrayList<>())
                .build();

        // Set optional campaign
        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findActiveById(request.getCampaignId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Campaign not found with id: " + request.getCampaignId()));
            encounter.setCampaign(campaign);
        }

        // Add adversaries if provided
        if (request.getAdversaries() != null && !request.getAdversaries().isEmpty()) {
            for (CreateEncounterRequest.EncounterAdversaryRequest advReq : request.getAdversaries()) {
                Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(advReq.getAdversaryId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Adversary not found with id: " + advReq.getAdversaryId()));

                EncounterAdversary encounterAdversary = EncounterAdversary.builder()
                        .encounter(encounter)
                        .adversary(adversary)
                        .count(advReq.getCount())
                        .build();

                encounter.getEncounterAdversaries().add(encounterAdversary);
            }
        }

        Encounter savedEncounter = encounterRepository.save(encounter);
        log.info("Created encounter with id: {}", savedEncounter.getId());

        return toResponse(savedEncounter, Set.of());
    }

    /**
     * Updates an existing encounter.
     *
     * @param id The encounter ID to update
     * @param request The update request containing new encounter details
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     * @throws EntityNotFoundException if the encounter or referenced entities are not found
     * @throws InsufficientPermissionsException if user lacks permission to modify
     */
    @Transactional
    public EncounterResponse updateEncounter(Long id, UpdateEncounterRequest request, Authentication auth) {
        log.info("Updating encounter with id: {}", id);

        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        validateModifyPermission(encounter, auth);

        // Partial updates - only update non-null fields
        if (request.getName() != null) {
            encounter.setName(request.getName());
        }
        if (request.getDescription() != null) {
            encounter.setDescription(request.getDescription());
        }
        if (request.getTier() != null) {
            encounter.setTier(request.getTier());
        }
        if (request.getIsPublic() != null) {
            encounter.setIsPublic(request.getIsPublic());
        }

        // Update campaign if provided
        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findActiveById(request.getCampaignId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Campaign not found with id: " + request.getCampaignId()));
            encounter.setCampaign(campaign);
        }

        // Replace adversaries if provided
        if (request.getAdversaries() != null) {
            encounter.getEncounterAdversaries().clear();

            for (CreateEncounterRequest.EncounterAdversaryRequest advReq : request.getAdversaries()) {
                Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(advReq.getAdversaryId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Adversary not found with id: " + advReq.getAdversaryId()));

                EncounterAdversary encounterAdversary = EncounterAdversary.builder()
                        .encounter(encounter)
                        .adversary(adversary)
                        .count(advReq.getCount())
                        .build();

                encounter.getEncounterAdversaries().add(encounterAdversary);
            }
        }

        Encounter updatedEncounter = encounterRepository.save(encounter);
        log.info("Updated encounter with id: {}", updatedEncounter.getId());

        return toResponse(updatedEncounter, Set.of());
    }

    /**
     * Soft deletes an encounter.
     *
     * @param id The encounter ID to delete
     * @param auth Authentication context
     * @throws EntityNotFoundException if the encounter is not found
     * @throws InsufficientPermissionsException if user lacks permission to delete
     */
    @Transactional
    public void deleteEncounter(Long id, Authentication auth) {
        log.info("Soft deleting encounter with id: {}", id);

        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        validateModifyPermission(encounter, auth);

        encounter.softDelete();
        encounterRepository.save(encounter);

        log.info("Soft deleted encounter with id: {}", id);
    }

    /**
     * Restores a soft-deleted encounter.
     * Only ADMIN or OWNER can restore encounters.
     *
     * @param id The encounter ID to restore
     * @param auth Authentication context
     * @return EncounterResponse containing the restored encounter
     * @throws EntityNotFoundException if the encounter is not found
     * @throws IllegalStateException if the encounter is not deleted
     * @throws InsufficientPermissionsException if user lacks ADMIN+ role
     */
    @Transactional
    public EncounterResponse restoreEncounter(Long id, Authentication auth) {
        log.info("Restoring encounter with id: {}", id);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (!roleHierarchyService.hasRoleOrHigher(user, Role.ADMIN)) {
            throw new InsufficientPermissionsException("Only admins can restore encounters");
        }

        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        if (!encounter.isDeleted()) {
            throw new IllegalStateException("Encounter with id " + id + " is not deleted");
        }

        encounter.restore();
        Encounter restoredEncounter = encounterRepository.save(encounter);

        log.info("Restored encounter with id: {}", id);

        return toResponse(restoredEncounter, Set.of());
    }

    /**
     * Creates a copy of an existing encounter for the authenticated user.
     *
     * @param id The ID of the encounter to copy
     * @param auth Authentication context
     * @return EncounterResponse containing the new copy
     * @throws EntityNotFoundException if the original encounter is not found
     */
    @Transactional
    public EncounterResponse copyEncounter(Long id, Authentication auth) {
        log.info("Copying encounter with id: {}", id);

        Encounter original = encounterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        validateViewPermission(original, auth);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        Encounter copy = Encounter.builder()
                .name(original.getName() + " (Copy)")
                .description(original.getDescription())
                .tier(original.getTier())
                .createdBy(creator)
                .originalEncounter(original)
                .isOfficial(false)
                .isPublic(false)
                .campaign(null) // Don't copy campaign association
                .encounterAdversaries(new ArrayList<>())
                .build();

        // Copy adversaries
        for (EncounterAdversary originalEA : original.getEncounterAdversaries()) {
            EncounterAdversary copyEA = EncounterAdversary.builder()
                    .encounter(copy)
                    .adversary(originalEA.getAdversary())
                    .count(originalEA.getCount())
                    .build();
            copy.getEncounterAdversaries().add(copyEA);
        }

        Encounter savedCopy = encounterRepository.save(copy);
        log.info("Created copy of encounter {} with new id: {}", id, savedCopy.getId());

        return toResponse(savedCopy, Set.of());
    }

    /**
     * Adds an adversary to an encounter or updates the count if already present.
     *
     * @param encounterId The encounter ID
     * @param adversaryId The adversary ID to add
     * @param count The count of this adversary type
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @Transactional
    public EncounterResponse addAdversaryToEncounter(Long encounterId, Long adversaryId, Integer count, Authentication auth) {
        log.info("Adding adversary {} to encounter {} with count {}", adversaryId, encounterId, count);

        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));

        validateModifyPermission(encounter, auth);

        Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(adversaryId)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + adversaryId));

        // Check if adversary already in encounter
        EncounterAdversary encounterAdversary = encounterAdversaryRepository
                .findByEncounterIdAndAdversaryId(encounterId, adversaryId)
                .orElse(null);

        if (encounterAdversary != null) {
            // Update count
            encounterAdversary.setCount(count);
            encounterAdversaryRepository.save(encounterAdversary);
        } else {
            // Add new
            encounterAdversary = EncounterAdversary.builder()
                    .encounter(encounter)
                    .adversary(adversary)
                    .count(count)
                    .build();
            encounter.getEncounterAdversaries().add(encounterAdversary);
            encounterRepository.save(encounter);
        }

        log.info("Added/updated adversary {} in encounter {}", adversaryId, encounterId);

        return toResponse(encounter, Set.of());
    }

    /**
     * Removes an adversary from an encounter.
     *
     * @param encounterId The encounter ID
     * @param adversaryId The adversary ID to remove
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @Transactional
    public EncounterResponse removeAdversaryFromEncounter(Long encounterId, Long adversaryId, Authentication auth) {
        log.info("Removing adversary {} from encounter {}", adversaryId, encounterId);

        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));

        validateModifyPermission(encounter, auth);

        EncounterAdversary encounterAdversary = encounterAdversaryRepository
                .findByEncounterIdAndAdversaryId(encounterId, adversaryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Adversary " + adversaryId + " not found in encounter " + encounterId));

        encounter.getEncounterAdversaries().remove(encounterAdversary);
        encounterAdversaryRepository.delete(encounterAdversary);

        log.info("Removed adversary {} from encounter {}", adversaryId, encounterId);

        return toResponse(encounter, Set.of());
    }

    /**
     * Updates the count of an adversary in an encounter.
     *
     * @param encounterId The encounter ID
     * @param adversaryId The adversary ID
     * @param count The new count
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @Transactional
    public EncounterResponse updateAdversaryCount(Long encounterId, Long adversaryId, Integer count, Authentication auth) {
        log.info("Updating adversary {} count in encounter {} to {}", adversaryId, encounterId, count);

        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));

        validateModifyPermission(encounter, auth);

        EncounterAdversary encounterAdversary = encounterAdversaryRepository
                .findByEncounterIdAndAdversaryId(encounterId, adversaryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Adversary " + adversaryId + " not found in encounter " + encounterId));

        encounterAdversary.setCount(count);
        encounterAdversaryRepository.save(encounterAdversary);

        log.info("Updated adversary {} count in encounter {}", adversaryId, encounterId);

        return toResponse(encounter, Set.of());
    }

    /**
     * Validates that the authenticated user has permission to modify the encounter.
     * <ul>
     *   <li>Official encounters: Only OWNER role can modify</li>
     *   <li>Non-official encounters: Creator OR MODERATOR+ can modify</li>
     * </ul>
     *
     * @param encounter The encounter being modified
     * @param auth Authentication context
     * @throws InsufficientPermissionsException if user lacks permission
     */
    private void validateModifyPermission(Encounter encounter, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (encounter.getIsOfficial()) {
            // Official encounters: Only OWNER can modify
            if (user.getRole() != Role.OWNER) {
                throw new InsufficientPermissionsException(
                        "Only owners can modify official encounters");
            }
        } else {
            // Non-official: Creator OR Moderator+ can modify
            boolean isCreator = encounter.getCreatedBy().getId().equals(user.getId());
            boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

            if (!isCreator && !isModeratorPlus) {
                throw new InsufficientPermissionsException(
                        "You do not have permission to modify this encounter");
            }
        }
    }

    /**
     * Validates that the authenticated user has permission to view the encounter.
     * Private non-official encounters are only visible to creator and moderators+.
     *
     * @param encounter The encounter being viewed
     * @param auth Authentication context
     * @throws EntityNotFoundException if user cannot view the encounter
     */
    private void validateViewPermission(Encounter encounter, Authentication auth) {
        if (encounter.getIsOfficial() || encounter.getIsPublic()) {
            return; // Anyone can view official or public encounters
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        boolean isCreator = encounter.getCreatedBy().getId().equals(user.getId());
        boolean isModeratorPlus = roleHierarchyService.hasRoleOrHigher(user, Role.MODERATOR);

        if (!isCreator && !isModeratorPlus) {
            throw new EntityNotFoundException("Encounter not found with id: " + encounter.getId());
        }
    }

    /**
     * Converts an Encounter entity to EncounterResponse DTO.
     */
    private EncounterResponse toResponse(Encounter encounter, Set<String> expand) {
        EncounterResponse.EncounterResponseBuilder builder = EncounterResponse.builder()
                .id(encounter.getId())
                .name(encounter.getName())
                .description(encounter.getDescription())
                .tier(encounter.getTier())
                .isOfficial(encounter.getIsOfficial())
                .isPublic(encounter.getIsPublic())
                .creatorId(encounter.getCreatedBy().getId())
                .totalBattlePoints(encounter.calculateTotalBattlePoints())
                .createdAt(encounter.getCreatedAt())
                .lastModifiedAt(encounter.getLastModifiedAt())
                .deletedAt(encounter.getDeletedAt());

        // Campaign ID
        if (encounter.getCampaign() != null) {
            builder.campaignId(encounter.getCampaign().getId());
        }

        // Original encounter ID
        if (encounter.getOriginalEncounter() != null) {
            builder.originalEncounterId(encounter.getOriginalEncounter().getId());
        }

        // Adversaries list
        if (encounter.getEncounterAdversaries() != null) {
            List<EncounterResponse.EncounterAdversaryResponse> adversariesList =
                    encounter.getEncounterAdversaries().stream()
                    .map(ea -> toEncounterAdversaryResponse(ea, expand.contains("adversaryDetails")))
                    .collect(Collectors.toList());
            builder.adversaries(adversariesList);
        }

        // Expanded relationships
        if (expand.contains("creator")) {
            User creator = encounter.getCreatedBy();
            builder.creator(UserResponse.builder()
                    .id(creator.getId())
                    .username(creator.getUsername())
                    .avatarUrl(creator.getAvatarUrl())
                    .build());
        }

        if (expand.contains("campaign") && encounter.getCampaign() != null) {
            Campaign campaign = encounter.getCampaign();
            builder.campaign(CampaignResponse.builder()
                    .id(campaign.getId())
                    .name(campaign.getName())
                    .description(campaign.getDescription())
                    .creatorId(campaign.getCreator().getId())
                    .createdAt(campaign.getCreatedAt())
                    .lastModifiedAt(campaign.getLastModifiedAt())
                    .deletedAt(campaign.getDeletedAt())
                    .build());
        }

        if (expand.contains("originalEncounter") && encounter.getOriginalEncounter() != null) {
            builder.originalEncounter(toResponse(encounter.getOriginalEncounter(), Set.of()));
        }

        return builder.build();
    }

    /**
     * Converts an EncounterAdversary entity to EncounterAdversaryResponse DTO.
     */
    private EncounterResponse.EncounterAdversaryResponse toEncounterAdversaryResponse(
            EncounterAdversary ea, boolean expandAdversary) {

        EncounterResponse.EncounterAdversaryResponse.EncounterAdversaryResponseBuilder builder =
                EncounterResponse.EncounterAdversaryResponse.builder()
                .id(ea.getId())
                .adversaryId(ea.getAdversary().getId())
                .count(ea.getCount())
                .battlePoints(ea.calculateBattlePoints());

        // Expand full adversary if requested
        if (expandAdversary) {
            Adversary adversary = ea.getAdversary();
            builder.adversary(AdversaryResponse.builder()
                    .id(adversary.getId())
                    .name(adversary.getName())
                    .tier(adversary.getTier())
                    .adversaryType(adversary.getAdversaryType())
                    .difficulty(adversary.getDifficulty())
                    .hitPointMax(adversary.getHitPointMax())
                    .build());
        }

        return builder.build();
    }
}
