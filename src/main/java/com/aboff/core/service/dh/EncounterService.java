package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dh.ImprovisedTierStatistics;
import com.aboff.core.model.dto.dh.request.CreateEncounterRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.EncounterResponse;
import com.aboff.core.model.dto.dh.response.EnvironmentResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.EncounterAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EnvironmentRepository;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
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
    private final EnvironmentRepository environmentRepository;
    private final RoleHierarchyService roleHierarchyService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

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
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User creator = userDetails.getUser();

        Encounter encounter = Encounter.builder()
                .name(request.getName())
                .description(request.getDescription())
                .tier(request.getTier())
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .partySize(request.getPartySize())
                .adjustmentEasier(nullToFalse(request.getAdjustmentEasier()))
                .adjustmentTwoPlusSolos(nullToFalse(request.getAdjustmentTwoPlusSolos()))
                .adjustmentBonusDamage(nullToFalse(request.getAdjustmentBonusDamage()))
                .adjustmentLowerTier(nullToFalse(request.getAdjustmentLowerTier()))
                .adjustmentNoElites(nullToFalse(request.getAdjustmentNoElites()))
                .adjustmentHarder(nullToFalse(request.getAdjustmentHarder()))
                .encounterAdversaries(new ArrayList<>())
                .build();

        // Set optional campaign
        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findActiveById(request.getCampaignId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Campaign not found with id: " + request.getCampaignId()));
            encounter.setCampaign(campaign);
        }

        // Set optional environment
        if (request.getEnvironmentId() != null) {
            encounter.setEnvironment(findActiveEnvironment(request.getEnvironmentId()));
        }

        // Add adversaries if provided (richer `adversaries` entries preferred; falls back to
        // the deprecated bare `adversaryIds` list for backward compatibility)
        List<CreateEncounterRequest.AdversaryEntry> entries = resolveAdversaryEntries(
                request.getAdversaries(), request.getAdversaryIds());
        int displayOrder = 0;
        for (CreateEncounterRequest.AdversaryEntry entry : entries) {
            Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(entry.getAdversaryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Adversary not found with id: " + entry.getAdversaryId()));

            EncounterAdversary encounterAdversary = EncounterAdversary.builder()
                    .encounter(encounter)
                    .adversary(adversary)
                    .label(entry.getLabel())
                    .tierOverride(entry.getTierOverride())
                    .displayOrder(displayOrder++)
                    .build();

            encounter.getEncounterAdversaries().add(encounterAdversary);
        }

        Encounter savedEncounter = encounterRepository.save(encounter);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedEncounter, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.ENCOUNTER_CREATED, AuditContext.forUser(auth).build(),
                "\"" + savedEncounter.getName() + "\" (encounter_id: " + savedEncounter.getId() + ")");

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
        if (request.getPartySize() != null) {
            encounter.setPartySize(request.getPartySize());
        }
        if (request.getAdjustmentEasier() != null) {
            encounter.setAdjustmentEasier(request.getAdjustmentEasier());
        }
        if (request.getAdjustmentTwoPlusSolos() != null) {
            encounter.setAdjustmentTwoPlusSolos(request.getAdjustmentTwoPlusSolos());
        }
        if (request.getAdjustmentBonusDamage() != null) {
            encounter.setAdjustmentBonusDamage(request.getAdjustmentBonusDamage());
        }
        if (request.getAdjustmentLowerTier() != null) {
            encounter.setAdjustmentLowerTier(request.getAdjustmentLowerTier());
        }
        if (request.getAdjustmentNoElites() != null) {
            encounter.setAdjustmentNoElites(request.getAdjustmentNoElites());
        }
        if (request.getAdjustmentHarder() != null) {
            encounter.setAdjustmentHarder(request.getAdjustmentHarder());
        }

        // Update campaign if provided
        if (request.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findActiveById(request.getCampaignId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Campaign not found with id: " + request.getCampaignId()));
            encounter.setCampaign(campaign);
        }

        // Update environment if provided
        if (request.getEnvironmentId() != null) {
            encounter.setEnvironment(findActiveEnvironment(request.getEnvironmentId()));
        }

        // Replace adversaries if provided (richer `adversaries` entries preferred; falls back
        // to the deprecated bare `adversaryIds` list for backward compatibility)
        List<CreateEncounterRequest.AdversaryEntry> entries = resolveAdversaryEntries(
                request.getAdversaries(), request.getAdversaryIds());
        if (!entries.isEmpty() || request.getAdversaries() != null || request.getAdversaryIds() != null) {
            encounter.getEncounterAdversaries().clear();

            int displayOrder = 0;
            for (CreateEncounterRequest.AdversaryEntry entry : entries) {
                Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(entry.getAdversaryId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Adversary not found with id: " + entry.getAdversaryId()));

                EncounterAdversary encounterAdversary = EncounterAdversary.builder()
                        .encounter(encounter)
                        .adversary(adversary)
                        .label(entry.getLabel())
                        .tierOverride(entry.getTierOverride())
                        .displayOrder(displayOrder++)
                        .build();

                encounter.getEncounterAdversaries().add(encounterAdversary);
            }
        }

        Encounter updatedEncounter = encounterRepository.save(encounter);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedEncounter, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.ENCOUNTER_UPDATED, AuditContext.forUser(auth).build(),
                "encounter_id: " + updatedEncounter.getId());

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
        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + id));

        validateModifyPermission(encounter, auth);

        encounter.softDelete();
        encounterRepository.save(encounter);
        eventPublisher.publishEvent(new EntityChangeEvent(this, encounter, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.ENCOUNTER_DELETED, AuditContext.forUser(auth).build(),
                "encounter_id: " + id);
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
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredEncounter, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.ENCOUNTER_RESTORED, AuditContext.forUser(auth).build(),
                "encounter_id: " + id);

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
                .environment(original.getEnvironment())
                .partySize(original.getPartySize())
                .adjustmentEasier(original.getAdjustmentEasier())
                .adjustmentTwoPlusSolos(original.getAdjustmentTwoPlusSolos())
                .adjustmentBonusDamage(original.getAdjustmentBonusDamage())
                .adjustmentLowerTier(original.getAdjustmentLowerTier())
                .adjustmentNoElites(original.getAdjustmentNoElites())
                .adjustmentHarder(original.getAdjustmentHarder())
                .encounterAdversaries(new ArrayList<>())
                .build();

        // Copy adversaries
        for (EncounterAdversary originalEA : original.getEncounterAdversaries()) {
            EncounterAdversary copyEA = EncounterAdversary.builder()
                    .encounter(copy)
                    .adversary(originalEA.getAdversary())
                    .label(originalEA.getLabel())
                    .tierOverride(originalEA.getTierOverride())
                    .displayOrder(originalEA.getDisplayOrder())
                    .build();
            copy.getEncounterAdversaries().add(copyEA);
        }

        Encounter savedCopy = encounterRepository.save(copy);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCopy, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.ENCOUNTER_COPIED, AuditContext.forUser(auth).build(),
                "encounter_id: " + id + " → copy_id: " + savedCopy.getId());

        return toResponse(savedCopy, Set.of());
    }

    /**
     * Adds an adversary instance to an encounter.
     *
     * @param encounterId The encounter ID
     * @param adversaryId The adversary ID to add
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @Transactional
    public EncounterResponse addAdversaryToEncounter(Long encounterId, Long adversaryId, Authentication auth) {
        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));

        validateModifyPermission(encounter, auth);

        Adversary adversary = adversaryRepository.findByIdAndDeletedAtIsNull(adversaryId)
                .orElseThrow(() -> new EntityNotFoundException("Adversary not found with id: " + adversaryId));

        EncounterAdversary encounterAdversary = EncounterAdversary.builder()
                .encounter(encounter)
                .adversary(adversary)
                .displayOrder(nextDisplayOrder(encounter))
                .build();
        encounter.getEncounterAdversaries().add(encounterAdversary);
        encounterRepository.save(encounter);
        auditLogger.log(AuditAction.ENCOUNTER_ADVERSARY_ADDED, AuditContext.forUser(auth).build(),
                "adversary_id: " + adversaryId + " → encounter_id: " + encounterId);

        return toResponse(encounter, Set.of());
    }

    /**
     * Removes an adversary instance from an encounter.
     *
     * @param encounterId The encounter ID
     * @param encounterAdversaryId The encounter adversary ID to remove
     * @param auth Authentication context
     * @return EncounterResponse containing the updated encounter
     */
    @Transactional
    public EncounterResponse removeAdversaryFromEncounter(Long encounterId, Long encounterAdversaryId, Authentication auth) {
        Encounter encounter = encounterRepository.findByIdAndDeletedAtIsNull(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with id: " + encounterId));

        validateModifyPermission(encounter, auth);

        EncounterAdversary encounterAdversary = encounterAdversaryRepository.findById(encounterAdversaryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Encounter adversary not found with id: " + encounterAdversaryId));

        if (!encounterAdversary.getEncounter().getId().equals(encounterId)) {
            throw new IllegalArgumentException(
                    "Encounter adversary " + encounterAdversaryId + " does not belong to encounter " + encounterId);
        }

        encounter.getEncounterAdversaries().remove(encounterAdversary);
        encounterAdversaryRepository.delete(encounterAdversary);
        auditLogger.log(AuditAction.ENCOUNTER_ADVERSARY_REMOVED, AuditContext.forUser(auth).build(),
                "encounter_adversary_id: " + encounterAdversaryId + " → encounter_id: " + encounterId);

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
     * <p>
     * Package-private rather than private: {@code EncounterRunService} delegates here to decide
     * whether a user may start a run from an encounter, so "can view this encounter" cannot
     * drift between the two services.
     * </p>
     *
     * @param encounter The encounter being viewed
     * @param auth Authentication context
     * @throws EntityNotFoundException if user cannot view the encounter
     */
    void validateViewPermission(Encounter encounter, Authentication auth) {
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
                .partySize(encounter.getPartySize())
                .adjustmentEasier(encounter.getAdjustmentEasier())
                .adjustmentTwoPlusSolos(encounter.getAdjustmentTwoPlusSolos())
                .adjustmentBonusDamage(encounter.getAdjustmentBonusDamage())
                .adjustmentLowerTier(encounter.getAdjustmentLowerTier())
                .adjustmentNoElites(encounter.getAdjustmentNoElites())
                .adjustmentHarder(encounter.getAdjustmentHarder())
                .suggestedBattlePoints(BattlePointCalculator.suggestedBudget(
                        encounter.getPartySize(), toAdjustments(encounter)))
                .spentBattlePoints(BattlePointCalculator.spentPoints(
                        toAdversaryTypes(encounter), encounter.getPartySize()))
                .createdAt(encounter.getCreatedAt())
                .lastModifiedAt(encounter.getLastModifiedAt())
                .deletedAt(encounter.getDeletedAt());

        // Campaign ID
        if (encounter.getCampaign() != null) {
            builder.campaignId(encounter.getCampaign().getId());
        }

        // Environment ID
        if (encounter.getEnvironment() != null) {
            builder.environmentId(encounter.getEnvironment().getId());
        }

        // Original encounter ID
        if (encounter.getOriginalEncounter() != null) {
            builder.originalEncounterId(encounter.getOriginalEncounter().getId());
        }

        // Adversaries list
        if (encounter.getEncounterAdversaries() != null) {
            List<EncounterResponse.EncounterAdversaryResponse> adversariesList =
                    encounter.getEncounterAdversaries().stream()
                    .map(ea -> toEncounterAdversaryResponse(ea, ExpandUtil.shouldExpand(expand, "adversaryDetails")))
                    .collect(Collectors.toList());
            builder.adversaries(adversariesList);
        }

        // Expanded relationships
        if (ExpandUtil.shouldExpand(expand, "creator")) {
            User creator = encounter.getCreatedBy();
            builder.creator(UserResponse.builder()
                    .id(creator.getId())
                    .username(creator.getUsername())
                    .avatarUrl(creator.getAvatarUrl())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "campaign") && encounter.getCampaign() != null) {
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

        if (ExpandUtil.shouldExpand(expand, "environment") && encounter.getEnvironment() != null) {
            builder.environment(toEnvironmentResponse(encounter.getEnvironment()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalEncounter") && encounter.getOriginalEncounter() != null) {
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
                .label(ea.getLabel())
                .tierOverride(ea.getTierOverride())
                .displayOrder(ea.getDisplayOrder());

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

        // Derived retiered statistics, computed on read rather than stored
        ImprovisedTierStatistics.forTier(ea.getTierOverride()).ifPresent(stats ->
                builder.retieredStatistics(EncounterResponse.RetieredStatisticsResponse.builder()
                        .tier(stats.tier())
                        .attackModifier(stats.attackModifier())
                        .difficulty(stats.difficulty())
                        .majorThreshold(stats.majorThreshold())
                        .severeThreshold(stats.severeThreshold())
                        .damageDiceRange(stats.damageDiceRange())
                        .build()));

        return builder.build();
    }

    /**
     * Converts an Environment entity to EnvironmentResponse DTO (unexpanded).
     */
    private EnvironmentResponse toEnvironmentResponse(Environment environment) {
        return EnvironmentResponse.builder()
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
                .deletedAt(environment.getDeletedAt())
                .build();
    }

    /**
     * Builds the {@link BattlePointCalculator.Adjustments} record from an encounter's six
     * adjustment flags.
     */
    private BattlePointCalculator.Adjustments toAdjustments(Encounter encounter) {
        return new BattlePointCalculator.Adjustments(
                Boolean.TRUE.equals(encounter.getAdjustmentEasier()),
                Boolean.TRUE.equals(encounter.getAdjustmentTwoPlusSolos()),
                Boolean.TRUE.equals(encounter.getAdjustmentBonusDamage()),
                Boolean.TRUE.equals(encounter.getAdjustmentLowerTier()),
                Boolean.TRUE.equals(encounter.getAdjustmentNoElites()),
                Boolean.TRUE.equals(encounter.getAdjustmentHarder()));
    }

    /**
     * Extracts the {@link AdversaryType} of each of an encounter's adversary instances, for
     * {@link BattlePointCalculator#spentPoints}. Instances with no adversary or no type
     * resolved (should not normally occur given the FK constraint) are skipped.
     */
    private List<AdversaryType> toAdversaryTypes(Encounter encounter) {
        if (encounter.getEncounterAdversaries() == null) {
            return List.of();
        }
        return encounter.getEncounterAdversaries().stream()
                .filter(ea -> ea.getAdversary() != null && ea.getAdversary().getAdversaryType() != null)
                .map(ea -> ea.getAdversary().getAdversaryType())
                .collect(Collectors.toList());
    }

    /**
     * Resolves the effective list of adversary entries to apply, preferring the richer
     * {@code adversaries} field over the deprecated bare {@code adversaryIds} list.
     *
     * @param entries The richer adversary entries from the request, may be null
     * @param legacyIds The deprecated bare adversary ID list from the request, may be null
     * @return The entries to apply; empty if neither field was provided
     */
    private List<CreateEncounterRequest.AdversaryEntry> resolveAdversaryEntries(
            List<CreateEncounterRequest.AdversaryEntry> entries, List<Long> legacyIds) {
        if (entries != null) {
            return entries;
        }
        if (legacyIds != null) {
            return legacyIds.stream()
                    .map(id -> CreateEncounterRequest.AdversaryEntry.builder().adversaryId(id).build())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Looks up an active (non-deleted) environment by ID.
     *
     * @param environmentId The environment ID to look up
     * @return The environment entity
     * @throws EntityNotFoundException if no active environment exists with that ID
     */
    private Environment findActiveEnvironment(Long environmentId) {
        return environmentRepository.findByIdAndDeletedAtIsNull(environmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Environment not found with id: " + environmentId));
    }

    /**
     * Returns the next display order for a new instance appended to an encounter's adversary
     * list -- one past the current highest, or 0 if the encounter has no adversaries yet.
     *
     * @param encounter The encounter a new instance is being added to
     * @return The display order to assign to the new instance
     */
    private int nextDisplayOrder(Encounter encounter) {
        if (encounter.getEncounterAdversaries() == null || encounter.getEncounterAdversaries().isEmpty()) {
            return 0;
        }
        return encounter.getEncounterAdversaries().stream()
                .mapToInt(EncounterAdversary::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
    }

    /**
     * Coalesces a nullable Boolean to {@code false}, for building entity defaults from request
     * fields that are optional and default to off.
     */
    private boolean nullToFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
