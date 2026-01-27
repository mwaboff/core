package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignRequest;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.repository.CampaignRepository;
import com.aboff.core.repository.CharacterSheetRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
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
import java.util.stream.Collectors;

/**
 * Service for managing Campaign entities.
 * <p>
 * Handles business logic for CRUD operations on campaigns, including user management
 * (game masters and players), character sheet management (pending, PC, NPC), and
 * access control validation.
 * </p>
 * <p>
 * Access control:
 * </p>
 * <ul>
 *   <li>View: Campaign participants OR users with MODERATOR/ADMIN/OWNER role</li>
 *   <li>Create: Any authenticated user</li>
 *   <li>Update/Delete: Campaign creator OR users with MODERATOR/ADMIN/OWNER role</li>
 *   <li>Add/Remove GMs: Campaign creator OR users with MODERATOR/ADMIN/OWNER role</li>
 *   <li>Add/Remove Players: Campaign creator/GM OR users with MODERATOR/ADMIN/OWNER role</li>
 *   <li>Submit Character: Character sheet owner who is a player in the campaign</li>
 *   <li>Approve/Reject/Add NPC/Remove Character: Campaign creator/GM OR users with MODERATOR/ADMIN/OWNER role</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CharacterSheetRepository characterSheetRepository;

    // ==================== CRUD OPERATIONS ====================

    /**
     * Retrieves a paginated list of campaigns.
     * <p>
     * Supports optional filtering by creator ID and name.
     * Only users with MODERATOR/ADMIN/OWNER role can list all campaigns.
     * </p>
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param creatorId Optional filter for creator ID
     * @param name Optional filter for name (case-insensitive partial match)
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing campaigns
     */
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> getAllCampaigns(
            int page,
            int size,
            Long creatorId,
            String name,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Campaign> campaignPage = campaignRepository.findActiveWithFilters(creatorId, name, pageable);

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<CampaignResponse>builder()
                .content(campaignPage.getContent().stream()
                        .map(campaign -> toResponse(campaign, expandSet))
                        .toList())
                .totalElements(campaignPage.getTotalElements())
                .totalPages(campaignPage.getTotalPages())
                .currentPage(campaignPage.getNumber())
                .pageSize(campaignPage.getSize())
                .build();
    }

    /**
     * Retrieves a single campaign by ID.
     * <p>
     * Only campaign participants or users with MODERATOR/ADMIN/OWNER role can view.
     * </p>
     *
     * @param id The campaign ID
     * @param expand Comma-separated list of relationships to expand
     * @param auth The authentication object containing the current user
     * @return CampaignResponse containing the campaign details
     * @throws EntityNotFoundException if the campaign is not found or is deleted
     * @throws InsufficientPermissionsException if the user lacks permission to view
     */
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id, String expand, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validatePlayerAccess(campaign, auth, "view");

        Set<String> expandSet = parseExpand(expand);
        return toResponse(campaign, expandSet);
    }

    /**
     * Creates a new campaign.
     * <p>
     * Any authenticated user can create a campaign. The creating user becomes
     * the creator and is automatically added as a game master.
     * </p>
     *
     * @param request The creation request containing campaign details
     * @param auth The authentication object containing the current user
     * @return CampaignResponse containing the created campaign
     */
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        log.info("Creating new campaign '{}' for user {}", request.getName(), userId);

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .creator(creator)
                .gameMasters(new HashSet<>())
                .players(new HashSet<>())
                .pendingCharacterSheets(new HashSet<>())
                .playerCharacters(new HashSet<>())
                .nonPlayerCharacters(new HashSet<>())
                .build();

        // Creator is automatically a game master
        campaign.getGameMasters().add(creator);

        // Add additional game masters if provided
        if (request.getGameMasterIds() != null) {
            for (Long gmId : request.getGameMasterIds()) {
                if (!gmId.equals(userId)) { // Don't duplicate creator
                    User gm = userRepository.findById(gmId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + gmId));
                    campaign.getGameMasters().add(gm);
                }
            }
        }

        // Add players if provided
        if (request.getPlayerIds() != null) {
            for (Long playerId : request.getPlayerIds()) {
                User player = userRepository.findById(playerId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + playerId));
                campaign.getPlayers().add(player);
            }
        }

        Campaign savedCampaign = campaignRepository.save(campaign);
        log.info("Created campaign with id: {} for user {}", savedCampaign.getId(), userId);

        return toResponse(savedCampaign, Set.of());
    }

    /**
     * Updates an existing campaign.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role
     * can update a campaign. Supports partial updates.
     * </p>
     *
     * @param id The campaign ID to update
     * @param request The update request containing new campaign details
     * @param auth The authentication object containing the current user
     * @return CampaignResponse containing the updated campaign
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission to update
     */
    @Transactional
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request, Authentication auth) {
        log.info("Updating campaign with id: {}", id);

        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validateCreatorAccess(campaign, auth, "update");

        if (request.getName() != null && !request.getName().isBlank()) {
            campaign.setName(request.getName());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }

        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("Updated campaign with id: {}", updatedCampaign.getId());

        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Deletes a campaign (soft delete).
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role
     * can delete a campaign.
     * </p>
     *
     * @param id The campaign ID to delete
     * @param auth The authentication object containing the current user
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission to delete
     */
    @Transactional
    public void deleteCampaign(Long id, Authentication auth) {
        log.info("Deleting campaign with id: {}", id);

        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validateCreatorAccess(campaign, auth, "delete");

        campaign.softDelete();
        campaignRepository.save(campaign);

        log.info("Soft deleted campaign with id: {}", id);
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Adds a user as a game master to the campaign.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role
     * can add game masters.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to add as GM
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign or user is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse addGameMaster(Long campaignId, Long userId, Authentication auth) {
        log.info("Adding game master {} to campaign {}", userId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateCreatorAccess(campaign, auth, "add game master to");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        campaign.getGameMasters().add(user);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Added game master {} to campaign {}", userId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Removes a user from the game masters of the campaign.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role
     * can remove game masters. The campaign creator cannot be removed.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to remove from GMs
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if attempting to remove the creator
     */
    @Transactional
    public CampaignResponse removeGameMaster(Long campaignId, Long userId, Authentication auth) {
        log.info("Removing game master {} from campaign {}", userId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateCreatorAccess(campaign, auth, "remove game master from");

        // Cannot remove the creator from game masters
        if (campaign.isCreator(userId)) {
            throw new IllegalStateException("Cannot remove the campaign creator from game masters");
        }

        campaign.getGameMasters().removeIf(gm -> gm.getId().equals(userId));
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Removed game master {} from campaign {}", userId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Adds a user as a player to the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role
     * can add players.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to add as player
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign or user is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse addPlayer(Long campaignId, Long userId, Authentication auth) {
        log.info("Adding player {} to campaign {}", userId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "add player to");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        campaign.getPlayers().add(user);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Added player {} to campaign {}", userId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Removes a user from the players of the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role
     * can remove players.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to remove from players
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse removePlayer(Long campaignId, Long userId, Authentication auth) {
        log.info("Removing player {} from campaign {}", userId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "remove player from");

        campaign.getPlayers().removeIf(player -> player.getId().equals(userId));
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Removed player {} from campaign {}", userId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    // ==================== CHARACTER SHEET MANAGEMENT ====================

    /**
     * Submits a character sheet to the campaign for approval.
     * <p>
     * Only the character sheet owner who is a player in the campaign can submit.
     * The character sheet is added to pendingCharacterSheets.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param characterSheetId The character sheet ID to submit
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign or character sheet is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse submitCharacterSheet(Long campaignId, Long characterSheetId, Authentication auth) {
        log.info("Submitting character sheet {} to campaign {}", characterSheetId, campaignId);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        CharacterSheet characterSheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        // Must be the character sheet owner
        if (!characterSheet.getOwner().getId().equals(userId)) {
            throw new InsufficientPermissionsException(
                    "You must be the owner of the character sheet to submit it");
        }

        // Must be a player in the campaign (or moderator)
        if (!campaign.isPlayer(userId) && !hasModeratorRole(userDetails)) {
            throw new InsufficientPermissionsException(
                    "You must be a player in this campaign to submit a character sheet");
        }

        campaign.getPendingCharacterSheets().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Submitted character sheet {} to campaign {}", characterSheetId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Approves a pending character sheet, moving it to player characters.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can approve.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param characterSheetId The character sheet ID to approve
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign or character sheet is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the character sheet is not in pending
     */
    @Transactional
    public CampaignResponse approveCharacterSheet(Long campaignId, Long characterSheetId, Authentication auth) {
        log.info("Approving character sheet {} in campaign {}", characterSheetId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "approve character sheets in");

        CharacterSheet characterSheet = campaign.getPendingCharacterSheets().stream()
                .filter(cs -> cs.getId().equals(characterSheetId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CharacterSheet with id " + characterSheetId + " is not in pending list"));

        // Move from pending to player characters
        campaign.getPendingCharacterSheets().remove(characterSheet);
        campaign.getPlayerCharacters().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Approved character sheet {} in campaign {}", characterSheetId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Rejects a pending character sheet, removing it from the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can reject.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param characterSheetId The character sheet ID to reject
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the character sheet is not in pending
     */
    @Transactional
    public CampaignResponse rejectCharacterSheet(Long campaignId, Long characterSheetId, Authentication auth) {
        log.info("Rejecting character sheet {} in campaign {}", characterSheetId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "reject character sheets in");

        boolean removed = campaign.getPendingCharacterSheets().removeIf(cs -> cs.getId().equals(characterSheetId));
        if (!removed) {
            throw new IllegalStateException(
                    "CharacterSheet with id " + characterSheetId + " is not in pending list");
        }

        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Rejected character sheet {} in campaign {}", characterSheetId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Adds a character sheet as an NPC to the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can add NPCs.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param characterSheetId The character sheet ID to add as NPC
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign or character sheet is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse addNonPlayerCharacter(Long campaignId, Long characterSheetId, Authentication auth) {
        log.info("Adding NPC {} to campaign {}", characterSheetId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "add NPCs to");

        CharacterSheet characterSheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        campaign.getNonPlayerCharacters().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Added NPC {} to campaign {}", characterSheetId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Removes a character sheet from the campaign (from any collection).
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can remove.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param characterSheetId The character sheet ID to remove
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse removeCharacterSheet(Long campaignId, Long characterSheetId, Authentication auth) {
        log.info("Removing character sheet {} from campaign {}", characterSheetId, campaignId);

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "remove character sheets from");

        // Remove from all collections
        campaign.getPendingCharacterSheets().removeIf(cs -> cs.getId().equals(characterSheetId));
        campaign.getPlayerCharacters().removeIf(cs -> cs.getId().equals(characterSheetId));
        campaign.getNonPlayerCharacters().removeIf(cs -> cs.getId().equals(characterSheetId));

        Campaign updatedCampaign = campaignRepository.save(campaign);

        log.info("Removed character sheet {} from campaign {}", characterSheetId, campaignId);
        return toResponse(updatedCampaign, Set.of());
    }

    // ==================== ACCESS CONTROL HELPERS ====================

    /**
     * Validates that the current user has creator-level access to the campaign.
     * <p>
     * Access is granted if the user is the campaign creator OR has a
     * MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param campaign The campaign to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateCreatorAccess(Campaign campaign, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        boolean isCreator = campaign.isCreator(userId);
        boolean isModerator = hasModeratorRole(userDetails);

        if (!isCreator && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this campaign");
        }
    }

    /**
     * Validates that the current user has game master-level access to the campaign.
     * <p>
     * Access is granted if the user is the campaign creator, a game master,
     * OR has a MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param campaign The campaign to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validateGameMasterAccess(Campaign campaign, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        boolean isCreator = campaign.isCreator(userId);
        boolean isGameMaster = campaign.isGameMaster(userId);
        boolean isModerator = hasModeratorRole(userDetails);

        if (!isCreator && !isGameMaster && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this campaign");
        }
    }

    /**
     * Validates that the current user has player-level access to the campaign.
     * <p>
     * Access is granted if the user is involved in the campaign (creator, GM, or player)
     * OR has a MODERATOR/ADMIN/OWNER role.
     * </p>
     *
     * @param campaign The campaign to validate access for
     * @param auth The authentication object containing the current user
     * @param operation The operation being performed (for error message)
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    private void validatePlayerAccess(Campaign campaign, Authentication auth, String operation) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        boolean isInvolved = campaign.isInvolved(userId);
        boolean isModerator = hasModeratorRole(userDetails);

        if (!isInvolved && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this campaign");
        }
    }

    /**
     * Checks if the user has MODERATOR, ADMIN, or OWNER role.
     *
     * @param userDetails The user details to check
     * @return true if the user has a moderator-level role, false otherwise
     */
    private boolean hasModeratorRole(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().matches("ROLE_(MODERATOR|ADMIN|OWNER)"));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Parses the expand parameter into a set of relationship names.
     *
     * @param expand Comma-separated list of relationships to expand
     * @return Set of relationship names
     */
    private Set<String> parseExpand(String expand) {
        if (expand == null || expand.trim().isEmpty()) {
            return Set.of();
        }
        Set<String> expandSet = new HashSet<>(List.of(expand.split(",")));
        // Handle "all" expansion
        if (expandSet.contains("all")) {
            expandSet.add("creator");
            expandSet.add("gameMasters");
            expandSet.add("players");
            expandSet.add("pendingCharacterSheets");
            expandSet.add("playerCharacters");
            expandSet.add("nonPlayerCharacters");
        }
        return expandSet;
    }

    /**
     * Converts a Campaign entity to CampaignResponse DTO.
     * <p>
     * Always includes IDs for relationships. Optionally expands full relationship
     * objects based on the expand set.
     * </p>
     *
     * @param campaign The campaign entity
     * @param expand Set of relationships to expand
     * @return CampaignResponse DTO
     */
    private CampaignResponse toResponse(Campaign campaign, Set<String> expand) {
        CampaignResponse.CampaignResponseBuilder builder = CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .creatorId(campaign.getCreator().getId())
                .gameMasterIds(campaign.getGameMasters().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .playerIds(campaign.getPlayers().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .pendingCharacterSheetIds(campaign.getPendingCharacterSheets().stream()
                        .map(CharacterSheet::getId)
                        .collect(Collectors.toList()))
                .playerCharacterIds(campaign.getPlayerCharacters().stream()
                        .map(CharacterSheet::getId)
                        .collect(Collectors.toList()))
                .nonPlayerCharacterIds(campaign.getNonPlayerCharacters().stream()
                        .map(CharacterSheet::getId)
                        .collect(Collectors.toList()))
                .createdAt(campaign.getCreatedAt())
                .lastModifiedAt(campaign.getLastModifiedAt())
                .deletedAt(campaign.getDeletedAt());

        // Expand creator if requested
        if (expand.contains("creator")) {
            User creator = campaign.getCreator();
            builder.creator(toUserResponse(creator));
        }

        // Expand game masters if requested
        if (expand.contains("gameMasters")) {
            builder.gameMasters(campaign.getGameMasters().stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList()));
        }

        // Expand players if requested
        if (expand.contains("players")) {
            builder.players(campaign.getPlayers().stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList()));
        }

        // Expand pending character sheets if requested
        if (expand.contains("pendingCharacterSheets")) {
            builder.pendingCharacterSheets(campaign.getPendingCharacterSheets().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        // Expand player characters if requested
        if (expand.contains("playerCharacters")) {
            builder.playerCharacters(campaign.getPlayerCharacters().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        // Expand non-player characters if requested
        if (expand.contains("nonPlayerCharacters")) {
            builder.nonPlayerCharacters(campaign.getNonPlayerCharacters().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Converts a User entity to UserResponse DTO.
     *
     * @param user The user entity
     * @return UserResponse DTO
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .createdAt(user.getCreatedAt())
                .lastModifiedAt(user.getLastModifiedAt())
                .build();
    }

    /**
     * Converts a CharacterSheet entity to CharacterSheetResponse DTO with basic fields.
     *
     * @param sheet The character sheet entity
     * @return CharacterSheetResponse DTO
     */
    private CharacterSheetResponse toCharacterSheetResponse(CharacterSheet sheet) {
        return CharacterSheetResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .pronouns(sheet.getPronouns())
                .level(sheet.getLevel())
                .ownerId(sheet.getOwner().getId())
                .createdAt(sheet.getCreatedAt())
                .lastModifiedAt(sheet.getLastModifiedAt())
                .build();
    }
}
