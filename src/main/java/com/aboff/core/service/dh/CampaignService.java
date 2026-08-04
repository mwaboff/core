package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionAccessRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignFearRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignGmNotesRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationAccessRequest;
import com.aboff.core.model.dto.dh.response.CampaignCharacterSummaryResponse;
import com.aboff.core.model.dto.dh.response.CampaignInviteResponse;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.JoinCampaignResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CampaignInvite;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.CampaignInviteRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.util.ExpandUtil;
import com.aboff.core.util.MarkdownSanitizerUtil;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final CampaignInviteRepository campaignInviteRepository;
    private final UserRepository userRepository;
    private final CharacterSheetRepository characterSheetRepository;
    private final TransformationCardRepository transformationCardRepository;
    private final CharacterSheetService characterSheetService;
    private final RoleHierarchyService roleHierarchyService;
    private final AuditLogger auditLogger;

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
     * @param auth The authentication object containing the current user
     * @return Paginated response containing campaigns
     */
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> getAllCampaigns(
            int page,
            int size,
            Long creatorId,
            String name,
            String expand,
            Authentication auth) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Campaign> campaignPage = campaignRepository.findActiveWithFilters(creatorId, name, pageable);

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<CampaignResponse>builder()
                .content(campaignPage.getContent().stream()
                        .map(campaign -> toResponse(campaign, expandSet, auth))
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
        return toResponse(campaign, expandSet, auth);
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

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(savedCampaign.getId()).build();
        auditLogger.log(AuditAction.CAMPAIGN_CREATED, ctx,
                String.format("\"%s\" (campaign_id: %d)", savedCampaign.getName(), savedCampaign.getId()));

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
        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validateCreatorAccess(campaign, auth, "update");
        validateNotEnded(campaign, "update");

        if (request.getName() != null && !request.getName().isBlank()) {
            campaign.setName(request.getName());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }

        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(updatedCampaign.getId()).build();
        auditLogger.log(AuditAction.CAMPAIGN_UPDATED, ctx,
                String.format("\"%s\" (campaign_id: %d)", updatedCampaign.getName(), updatedCampaign.getId()));

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
        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validateCreatorAccess(campaign, auth, "delete");

        campaign.softDelete();
        campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(id).build();
        auditLogger.log(AuditAction.CAMPAIGN_DELETED, ctx,
                String.format("\"%s\" (campaign_id: %d)", campaign.getName(), id));
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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateCreatorAccess(campaign, auth, "add game master to");
        validateNotEnded(campaign, "add game master to");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        campaign.getGameMasters().add(user);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withTargetUserId(userId).build();
        auditLogger.log(AuditAction.CAMPAIGN_GM_ADDED, ctx,
                String.format("user_id: %d to \"%s\" (campaign_id: %d)", userId, campaign.getName(), campaignId));

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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateCreatorAccess(campaign, auth, "remove game master from");

        // Cannot remove the creator from game masters
        if (campaign.isCreator(userId)) {
            throw new IllegalStateException("Cannot remove the campaign creator from game masters");
        }

        campaign.getGameMasters().removeIf(gm -> gm.getId().equals(userId));
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withTargetUserId(userId).build();
        auditLogger.log(AuditAction.CAMPAIGN_GM_REMOVED, ctx,
                String.format("user_id: %d from \"%s\" (campaign_id: %d)", userId, campaign.getName(), campaignId));

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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "add player to");
        validateNotEnded(campaign, "add player to");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        campaign.getPlayers().add(user);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withTargetUserId(userId).build();
        auditLogger.log(AuditAction.CAMPAIGN_PLAYER_ADDED, ctx,
                String.format("user_id: %d to \"%s\" (campaign_id: %d)", userId, campaign.getName(), campaignId));

        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Kicks a player from the campaign, removing them and all their character sheets.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role
     * can kick players. Cascades to remove all character sheets owned by the
     * kicked player from all three collections (pending, PC, NPC).
     * </p>
     *
     * @param campaignId The campaign ID
     * @param userId The user ID to kick
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     */
    @Transactional
    public CampaignResponse kickPlayer(Long campaignId, Long userId, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "kick player from");

        // Remove the player
        campaign.getPlayers().removeIf(player -> player.getId().equals(userId));

        // Cascade: remove all character sheets owned by the kicked player
        campaign.getPendingCharacterSheets().removeIf(cs -> cs.getOwner().getId().equals(userId));
        campaign.getPlayerCharacters().removeIf(cs -> cs.getOwner().getId().equals(userId));
        campaign.getNonPlayerCharacters().removeIf(cs -> cs.getOwner().getId().equals(userId));

        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withTargetUserId(userId).build();
        auditLogger.log(AuditAction.CAMPAIGN_PLAYER_KICKED, ctx,
                String.format("user_id: %d from \"%s\" (campaign_id: %d)", userId, campaign.getName(), campaignId));

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
        if (!campaign.isPlayer(userId) && !roleHierarchyService.hasModeratorOrHigher(userDetails)) {
            throw new InsufficientPermissionsException(
                    "You must be a player in this campaign to submit a character sheet");
        }

        validateNotEnded(campaign, "submit character sheet to");

        // One-campaign constraint: character sheet can only be in one active campaign
        if (campaignRepository.isCharacterSheetInActiveCampaign(characterSheetId)) {
            throw new IllegalStateException(
                    "Character sheet is already in an active campaign");
        }

        campaign.getPendingCharacterSheets().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_CHARACTER_SUBMITTED, ctx,
                String.format("\"%s\" (character_sheet_id: %d) to \"%s\" (campaign_id: %d)",
                        characterSheet.getName(), characterSheetId, campaign.getName(), campaignId));

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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "approve character sheets in");
        validateNotEnded(campaign, "approve character sheets in");

        CharacterSheet characterSheet = campaign.getPendingCharacterSheets().stream()
                .filter(cs -> cs.getId().equals(characterSheetId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CharacterSheet with id " + characterSheetId + " is not in pending list"));

        // Move from pending to player characters
        campaign.getPendingCharacterSheets().remove(characterSheet);
        campaign.getPlayerCharacters().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_CHARACTER_APPROVED, ctx,
                String.format("\"%s\" (character_sheet_id: %d) in \"%s\" (campaign_id: %d)",
                        characterSheet.getName(), characterSheetId, campaign.getName(), campaignId));

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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "reject character sheets in");

        boolean removed = campaign.getPendingCharacterSheets().removeIf(cs -> cs.getId().equals(characterSheetId));
        if (!removed) {
            throw new IllegalStateException(
                    "CharacterSheet with id " + characterSheetId + " is not in pending list");
        }

        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_CHARACTER_REJECTED, ctx,
                String.format("character_sheet_id: %d in \"%s\" (campaign_id: %d)",
                        characterSheetId, campaign.getName(), campaignId));

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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "add NPCs to");
        validateNotEnded(campaign, "add NPCs to");

        CharacterSheet characterSheet = characterSheetRepository.findActiveById(characterSheetId)
                .orElseThrow(() -> new EntityNotFoundException("CharacterSheet not found with id: " + characterSheetId));

        // One-campaign constraint
        if (campaignRepository.isCharacterSheetInActiveCampaign(characterSheetId)) {
            throw new IllegalStateException(
                    "Character sheet is already in an active campaign");
        }

        campaign.getNonPlayerCharacters().add(characterSheet);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_NPC_ADDED, ctx,
                String.format("\"%s\" (character_sheet_id: %d) to \"%s\" (campaign_id: %d)",
                        characterSheet.getName(), characterSheetId, campaign.getName(), campaignId));

        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Removes a character sheet from the campaign (from any collection).
     * <p>
     * Allowed by campaign creator/GM, the character sheet owner,
     * or users with MODERATOR/ADMIN/OWNER role. Works on ended campaigns too
     * (unlinking is always allowed).
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
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        // Allow GM/creator access OR character sheet owner
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();
        boolean isGmOrCreator = campaign.isCreator(userId) || campaign.isGameMaster(userId);
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);
        boolean isSheetOwner = isCharacterSheetOwner(campaign, characterSheetId, userId);

        if (!isGmOrCreator && !isModerator && !isSheetOwner) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to remove character sheets from this campaign");
        }

        // Remove from all collections
        campaign.getPendingCharacterSheets().removeIf(cs -> cs.getId().equals(characterSheetId));
        campaign.getPlayerCharacters().removeIf(cs -> cs.getId().equals(characterSheetId));
        campaign.getNonPlayerCharacters().removeIf(cs -> cs.getId().equals(characterSheetId));

        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(characterSheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_CHARACTER_REMOVED, ctx,
                String.format("character_sheet_id: %d from \"%s\" (campaign_id: %d)",
                        characterSheetId, campaign.getName(), campaignId));

        return toResponse(updatedCampaign, Set.of());
    }

    // ==================== CAMPAIGN LIFECYCLE ====================

    /**
     * Retrieves a paginated list of campaigns where the current user is involved.
     *
     * @param page Zero-based page number
     * @param size Number of items per page (max 100)
     * @param expand Comma-separated list of relationships to expand
     * @param auth The authentication object containing the current user
     * @return Paginated response containing the user's campaigns
     */
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> getMyCampaigns(int page, int size, String expand, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Campaign> campaignPage = campaignRepository.findActiveByUserInvolvement(userId, pageable);

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<CampaignResponse>builder()
                .content(campaignPage.getContent().stream()
                        .map(campaign -> toResponse(campaign, expandSet, auth))
                        .toList())
                .totalElements(campaignPage.getTotalElements())
                .totalPages(campaignPage.getTotalPages())
                .currentPage(campaignPage.getNumber())
                .pageSize(campaignPage.getSize())
                .build();
    }

    /**
     * Retrieves a paginated list of campaigns for a specific user.
     * <p>
     * Access is restricted to the target user themselves or users with MODERATOR/ADMIN/OWNER role.
     * Permission checks are performed before user existence checks so that regular users
     * receive 403 (not 404) for non-existent user IDs.
     * </p>
     *
     * @param userId The ID of the user whose campaigns to retrieve
     * @param page The page number (zero-based)
     * @param size The page size (capped at 100)
     * @param expand Comma-separated list of relationships to expand
     * @param auth The authentication object containing the current user
     * @return Paginated response containing the user's campaigns
     * @throws InsufficientPermissionsException if the authenticated user is not the target user and lacks MODERATOR+ role
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> getUserCampaigns(Long userId, int page, int size, String expand, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long authenticatedUserId = userDetails.getUserId();

        if (!authenticatedUserId.equals(userId) && !roleHierarchyService.hasModeratorOrHigher(userDetails)) {
            throw new InsufficientPermissionsException("You do not have permission to view this user's campaigns");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Campaign> campaignPage = campaignRepository.findActiveByUserInvolvement(userId, pageable);

        Set<String> expandSet = parseExpand(expand);

        return PagedResponse.<CampaignResponse>builder()
                .content(campaignPage.getContent().stream()
                        .map(campaign -> toResponse(campaign, expandSet, auth))
                        .toList())
                .totalElements(campaignPage.getTotalElements())
                .totalPages(campaignPage.getTotalPages())
                .currentPage(campaignPage.getNumber())
                .pageSize(campaignPage.getSize())
                .build();
    }

    /**
     * Ends a campaign, locking it from further modifications.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role can end a campaign.
     * An ended campaign remains visible but is locked for most operations.
     * </p>
     *
     * @param id The campaign ID
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the campaign is already ended
     */
    @Transactional
    public CampaignResponse endCampaign(Long id, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + id));

        validateCreatorAccess(campaign, auth, "end");

        if (campaign.isEnded()) {
            throw new IllegalStateException("Campaign is already ended");
        }

        campaign.endCampaign();
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(id).build();
        auditLogger.log(AuditAction.CAMPAIGN_ENDED, ctx,
                String.format("\"%s\" (campaign_id: %d)", campaign.getName(), id));

        return toResponse(updatedCampaign, Set.of());
    }

    /**
     * Generates an invite link for the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can generate invites.
     * The invite is valid for 24 hours and single-use.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param auth The authentication object containing the current user
     * @return CampaignInviteResponse containing the invite details
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the campaign is ended
     */
    @Transactional
    public CampaignInviteResponse generateInvite(Long campaignId, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "generate invite for");
        validateNotEnded(campaign, "generate invite for");

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        CampaignInvite invite = CampaignInvite.builder()
                .campaign(campaign)
                .token(UUID.randomUUID().toString())
                .createdBy(userDetails.getUserId())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        CampaignInvite savedInvite = campaignInviteRepository.save(invite);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).build();
        auditLogger.log(AuditAction.CAMPAIGN_INVITE_GENERATED, ctx,
                String.format("\"%s\" (campaign_id: %d)", campaign.getName(), campaignId));

        return CampaignInviteResponse.builder()
                .id(savedInvite.getId())
                .campaignId(campaignId)
                .token(savedInvite.getToken())
                .expiresAt(savedInvite.getExpiresAt())
                .createdAt(savedInvite.getCreatedAt())
                .build();
    }

    /**
     * Joins a campaign via invite token.
     * <p>
     * Validates that the token is not expired, not already used, and the campaign
     * is not ended or deleted. Adds the user as a player and marks the invite as used.
     * </p>
     *
     * @param token The invite token
     * @param auth The authentication object containing the current user
     * @return JoinCampaignResponse with campaign details
     * @throws EntityNotFoundException if the invite token is not found
     * @throws IllegalStateException if the token is invalid, expired, or campaign is ended
     */
    @Transactional
    public JoinCampaignResponse joinViaInvite(String token, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        CampaignInvite invite = campaignInviteRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invite not found"));

        if (!invite.isValid()) {
            throw new IllegalStateException("Invite is expired or already used");
        }

        Campaign campaign = invite.getCampaign();
        if (campaign.isDeleted()) {
            throw new EntityNotFoundException("Campaign not found");
        }
        if (campaign.isEnded()) {
            throw new IllegalStateException("Cannot join an ended campaign");
        }

        // Check if already involved
        if (campaign.isInvolved(userId)) {
            throw new IllegalStateException("You are already a member of this campaign");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        campaign.getPlayers().add(user);
        invite.markUsed(userId);

        campaignRepository.save(campaign);
        campaignInviteRepository.save(invite);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaign.getId()).build();
        auditLogger.log(AuditAction.CAMPAIGN_JOINED_VIA_INVITE, ctx,
                String.format("\"%s\" (campaign_id: %d)", campaign.getName(), campaign.getId()));

        return JoinCampaignResponse.builder()
                .campaignId(campaign.getId())
                .campaignName(campaign.getName())
                .message("Successfully joined campaign: " + campaign.getName())
                .build();
    }

    /**
     * Allows a player to leave a campaign voluntarily.
     * <p>
     * Only players can leave. Does NOT cascade-unlink character sheets (voluntary departure).
     * Works on ended campaigns too.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse
     * @throws EntityNotFoundException if the campaign is not found
     * @throws IllegalStateException if the user is not a player
     */
    @Transactional
    public CampaignResponse leaveCampaign(Long campaignId, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        if (!campaign.isPlayer(userId)) {
            throw new IllegalStateException("You are not a player in this campaign");
        }

        campaign.getPlayers().removeIf(player -> player.getId().equals(userId));
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).build();
        auditLogger.log(AuditAction.CAMPAIGN_PLAYER_LEFT, ctx,
                String.format("\"%s\" (campaign_id: %d)", campaign.getName(), campaignId));

        return toResponse(updatedCampaign, Set.of());
    }

    // ==================== GM SCREEN ====================

    /**
     * Updates the campaign's Fear counter.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can change Fear.
     * The value is absolute, not a delta, and its 0-12 range is enforced by request validation.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param request The request containing the new Fear value
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse including GM-only fields
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the campaign is ended
     */
    @Transactional
    public CampaignResponse updateFear(Long campaignId, UpdateCampaignFearRequest request, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "update fear for");
        validateNotEnded(campaign, "update fear for");

        campaign.setFear(request.getFear());
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).build();
        auditLogger.log(AuditAction.CAMPAIGN_FEAR_UPDATED, ctx,
                String.format("fear: %d in \"%s\" (campaign_id: %d)",
                        request.getFear(), campaign.getName(), campaignId));

        return toResponse(updatedCampaign, Set.of(), auth);
    }

    /**
     * Updates the campaign's game master notes.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can change the notes.
     * The submitted text is sanitized before persistence, so the stored value may differ from
     * the request. An empty string clears the notes.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param request The request containing the new notes
     * @param auth The authentication object containing the current user
     * @return Updated CampaignResponse including GM-only fields
     * @throws EntityNotFoundException if the campaign is not found
     * @throws InsufficientPermissionsException if the user lacks permission
     * @throws IllegalStateException if the campaign is ended
     */
    @Transactional
    public CampaignResponse updateGmNotes(Long campaignId, UpdateCampaignGmNotesRequest request, Authentication auth) {
        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "update GM notes for");
        validateNotEnded(campaign, "update GM notes for");

        campaign.setGmNotes(MarkdownSanitizerUtil.sanitize(request.getGmNotes()));
        Campaign updatedCampaign = campaignRepository.save(campaign);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).build();
        auditLogger.log(AuditAction.CAMPAIGN_GM_NOTES_UPDATED, ctx,
                String.format("\"%s\" (campaign_id: %d, length: %d)",
                        campaign.getName(), campaignId, campaign.getGmNotes().length()));

        return toResponse(updatedCampaign, Set.of(), auth);
    }

    /**
     * Grants or revokes a character's access to transformations, optionally assigning or clearing
     * the character's transformation card in the same call.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can change
     * transformation access; transformations are something a GM grants, which is why the
     * player-facing character sheet update endpoint has no equivalent field.
     * </p>
     * <p>
     * Disabling access deliberately <strong>preserves</strong> the character's
     * {@code transformationCardId}, {@code transformationTokens}, and {@code wolfFormActive}.
     * Turning transformations off only hides the panel; it must never destroy the player's
     * selection, so a GM can re-enable it later without the character losing state.
     * </p>
     * <p>
     * A transformation card may be assigned while access is disabled so a GM can pre-load a
     * transformation before revealing it. {@code clearTransformationCard} takes precedence over
     * {@code transformationCardId}, mirroring the tri-state convention used by the character
     * sheet update path.
     * </p>
     *
     * @param campaignId The campaign ID
     * @param sheetId The character sheet ID, which must belong to the campaign
     * @param request The request containing the new access flag and optional card assignment
     * @param auth The authentication object containing the current user
     * @return The updated character sheet
     * @throws EntityNotFoundException if the campaign, the sheet within that campaign, or a
     *                                 referenced transformation card is not found
     * @throws InsufficientPermissionsException if the user lacks game master access
     * @throws IllegalStateException if the campaign has ended
     */
    @Transactional
    public CharacterSheetResponse updateTransformationAccess(
            Long campaignId, Long sheetId, UpdateTransformationAccessRequest request, Authentication auth) {

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "update transformation access for");
        // Granting a transformation is a live-play act, so it follows the other GM mutations
        // (fear, GM notes, approvals) in refusing an ended campaign. removeCharacterSheet is the
        // deliberate exception to that rule because it is cleanup, not play.
        validateNotEnded(campaign, "update transformation access for");

        CharacterSheet sheet = findCharacterSheetInCampaign(campaign, sheetId);

        sheet.setTransformationEnabled(Boolean.TRUE.equals(request.getEnabled()));

        if (Boolean.TRUE.equals(request.getClearTransformationCard())) {
            sheet.setTransformationCard(null);
            sheet.setTransformationTokens(null);
            sheet.setWolfFormActive(false);
        } else if (request.getTransformationCardId() != null) {
            TransformationCard card = transformationCardRepository
                    .findByIdAndDeletedAtIsNull(request.getTransformationCardId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "TransformationCard not found with id: " + request.getTransformationCardId()));
            sheet.setTransformationCard(card);
        }

        CharacterSheet updatedSheet = characterSheetRepository.save(sheet);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(sheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_TRANSFORMATION_ACCESS_UPDATED, ctx,
                String.format("transformations %s for \"%s\" (character_sheet_id: %d, campaign_id: %d)",
                        updatedSheet.isTransformationEnabled() ? "enabled" : "disabled",
                        updatedSheet.getName(), sheetId, campaignId));

        return characterSheetService.toResponse(updatedSheet, Set.of());
    }

    /**
     * Grants or revokes a character's access to <strong>creating new</strong> companions.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can change
     * companion access. Follows the same authorization and ended-campaign checks as
     * {@link #updateTransformationAccess}, but is otherwise much simpler: there is no card to
     * assign or clear, and -- deliberately, unlike transformations -- no player-side write gate
     * to enforce. Disabling this flag only stops a new companion from being created; it must
     * never hide, disable, or orphan a companion the character already has (see the companions
     * implementation plan, section 3.4).
     * </p>
     *
     * @param campaignId The campaign ID
     * @param sheetId The character sheet ID, which must belong to the campaign
     * @param request The request containing the new access flag
     * @param auth The authentication object containing the current user
     * @return The updated character sheet
     * @throws EntityNotFoundException if the campaign or the sheet within that campaign is not found
     * @throws InsufficientPermissionsException if the user lacks game master access
     * @throws IllegalStateException if the campaign has ended
     */
    @Transactional
    public CharacterSheetResponse updateCompanionAccess(
            Long campaignId, Long sheetId, UpdateCompanionAccessRequest request, Authentication auth) {

        Campaign campaign = campaignRepository.findActiveById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        validateGameMasterAccess(campaign, auth, "update companion access for");
        validateNotEnded(campaign, "update companion access for");

        CharacterSheet sheet = findCharacterSheetInCampaign(campaign, sheetId);

        sheet.setCompanionsEnabled(Boolean.TRUE.equals(request.getEnabled()));

        CharacterSheet updatedSheet = characterSheetRepository.save(sheet);

        AuditContext ctx = AuditContext.forUser(auth).withCampaignId(campaignId).withCharacterSheetId(sheetId).build();
        auditLogger.log(AuditAction.CAMPAIGN_COMPANION_ACCESS_UPDATED, ctx,
                String.format("companions %s for \"%s\" (character_sheet_id: %d, campaign_id: %d)",
                        updatedSheet.isCompanionsEnabled() ? "enabled" : "disabled",
                        updatedSheet.getName(), sheetId, campaignId));

        return characterSheetService.toResponse(updatedSheet, Set.of());
    }

    /**
     * Finds a character sheet that belongs to the campaign, in any of its roster collections.
     *
     * @param campaign The campaign to search
     * @param sheetId The character sheet ID
     * @return The character sheet belonging to the campaign
     * @throws EntityNotFoundException if the sheet is not part of the campaign
     */
    private CharacterSheet findCharacterSheetInCampaign(Campaign campaign, Long sheetId) {
        return Stream.of(campaign.getPlayerCharacters(),
                        campaign.getNonPlayerCharacters(),
                        campaign.getPendingCharacterSheets())
                .flatMap(Set::stream)
                .filter(sheet -> sheet.getId().equals(sheetId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "CharacterSheet not found with id: " + sheetId + " in campaign with id: " + campaign.getId()));
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
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

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
    public void validateGameMasterAccess(Campaign campaign, Authentication auth, String operation) {
        if (!hasGameMasterAccess(campaign, auth)) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this campaign");
        }
    }

    /**
     * Determines, without throwing, whether the current user has game master-level access.
     * <p>
     * This is the single definition of "is a GM" for the application: {@link #validateGameMasterAccess}
     * delegates to it, {@link #toResponse(Campaign, Set, Authentication)} uses it to decide
     * whether GM-only fields may be serialized, and other campaign-scoped services
     * (e.g. {@code CountdownService}) delegate here rather than reimplementing the rule.
     * Fails closed for an absent or unrecognized principal.
     * </p>
     *
     * @param campaign The campaign to check access against
     * @param auth The authentication object containing the current user, may be null
     * @return true if the user is the creator, a game master, or a MODERATOR/ADMIN/OWNER
     */
    public boolean hasGameMasterAccess(Campaign campaign, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        Long userId = userDetails.getUserId();
        return campaign.isCreator(userId)
                || campaign.isGameMaster(userId)
                || roleHierarchyService.hasModeratorOrHigher(userDetails);
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
        boolean isModerator = roleHierarchyService.hasModeratorOrHigher(userDetails);

        if (!isInvolved && !isModerator) {
            throw new InsufficientPermissionsException(
                    "You do not have permission to " + operation + " this campaign");
        }
    }

    /**
     * Validates that the campaign has not been ended.
     *
     * @param campaign The campaign to check
     * @param operation The operation being attempted (for error message)
     * @throws IllegalStateException if the campaign is ended
     */
    public void validateNotEnded(Campaign campaign, String operation) {
        if (campaign.isEnded()) {
            throw new IllegalStateException("Cannot " + operation + " an ended campaign");
        }
    }

    /**
     * Checks if a character sheet in the campaign is owned by the specified user.
     *
     * @param campaign The campaign containing the character sheets
     * @param characterSheetId The character sheet ID to check
     * @param userId The user ID to check ownership for
     * @return true if the user owns the character sheet
     */
    private boolean isCharacterSheetOwner(Campaign campaign, Long characterSheetId, Long userId) {
        return campaign.getPendingCharacterSheets().stream()
                .anyMatch(cs -> cs.getId().equals(characterSheetId) && cs.getOwner().getId().equals(userId))
            || campaign.getPlayerCharacters().stream()
                .anyMatch(cs -> cs.getId().equals(characterSheetId) && cs.getOwner().getId().equals(userId))
            || campaign.getNonPlayerCharacters().stream()
                .anyMatch(cs -> cs.getId().equals(characterSheetId) && cs.getOwner().getId().equals(userId));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Parses the expand parameter into a set of relationship names.
     * <p>
     * Supports "all" expansion which includes all available relationships:
     * creator, gameMasters, players, pendingCharacterSheets, playerCharacters, nonPlayerCharacters.
     * </p>
     *
     * @param expand Comma-separated list of relationships to expand
     * @return Set of relationship names
     */
    private Set<String> parseExpand(String expand) {
        Set<String> expandSet = new HashSet<>(ExpandUtil.parseExpand(expand));
        // Handle "all" expansion
        if (expandSet.contains("all")) {
            expandSet.add("creator");
            expandSet.add("gameMasters");
            expandSet.add("players");
            expandSet.add("pendingCharacterSheets");
            expandSet.add("playerCharacters");
            expandSet.add("nonPlayerCharacters");
            expandSet.add("characterSummaries");
        }
        return expandSet;
    }

    /**
     * Converts a Campaign entity to CampaignResponse DTO, omitting all GM-only fields.
     * <p>
     * Always includes IDs for relationships. Optionally expands full relationship
     * objects based on the expand set.
     * </p>
     * <p>
     * This overload is the fail-closed default: it never emits {@code gmNotes}, so any
     * response built through it is safe to hand to a player. Use
     * {@link #toResponse(Campaign, Set, Authentication)} on paths where the caller has
     * been identified and GM-only fields should be included when permitted.
     * </p>
     *
     * @param campaign The campaign entity
     * @param expand Set of relationships to expand
     * @return CampaignResponse DTO without GM-only fields
     */
    private CampaignResponse toResponse(Campaign campaign, Set<String> expand) {
        return toResponse(campaign, expand, null);
    }

    /**
     * Converts a Campaign entity to CampaignResponse DTO, including GM-only fields when
     * the supplied authentication has game master-level access to the campaign.
     * <p>
     * {@code gmNotes} is populated only for the campaign creator, its game masters, and
     * MODERATOR/ADMIN/OWNER users. For anyone else it is left null and therefore omitted
     * from the serialized JSON, since CampaignResponse is {@code @JsonInclude(NON_NULL)}.
     * {@code fear} is always included: it is a table-visible shared resource.
     * </p>
     *
     * @param campaign The campaign entity
     * @param expand Set of relationships to expand
     * @param auth The authentication object for the requesting user, or null to omit GM-only fields
     * @return CampaignResponse DTO
     */
    private CampaignResponse toResponse(Campaign campaign, Set<String> expand, Authentication auth) {
        CampaignResponse.CampaignResponseBuilder builder = CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .fear(campaign.getFear())
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
                .endedAt(campaign.getEndedAt())
                .isEnded(campaign.isEnded())
                .createdAt(campaign.getCreatedAt())
                .lastModifiedAt(campaign.getLastModifiedAt())
                .deletedAt(campaign.getDeletedAt());

        // GM-only: never expose prep notes to players
        if (hasGameMasterAccess(campaign, auth)) {
            builder.gmNotes(campaign.getGmNotes());
        }

        // Expand creator if requested
        if (ExpandUtil.shouldExpand(expand, "creator")) {
            User creator = campaign.getCreator();
            builder.creator(toUserResponse(creator));
        }

        // Expand game masters if requested
        if (ExpandUtil.shouldExpand(expand, "gameMasters")) {
            builder.gameMasters(campaign.getGameMasters().stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList()));
        }

        // Expand players if requested
        if (ExpandUtil.shouldExpand(expand, "players")) {
            builder.players(campaign.getPlayers().stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList()));
        }

        // Expand pending character sheets if requested
        if (ExpandUtil.shouldExpand(expand, "pendingCharacterSheets")) {
            builder.pendingCharacterSheets(campaign.getPendingCharacterSheets().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        // Expand player characters if requested
        if (ExpandUtil.shouldExpand(expand, "playerCharacters")) {
            builder.playerCharacters(campaign.getPlayerCharacters().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        // Expand non-player characters if requested
        if (ExpandUtil.shouldExpand(expand, "nonPlayerCharacters")) {
            builder.nonPlayerCharacters(campaign.getNonPlayerCharacters().stream()
                    .map(this::toCharacterSheetResponse)
                    .collect(Collectors.toList()));
        }

        // Expand character summaries if requested
        if (ExpandUtil.shouldExpand(expand, "characterSummaries")) {
            prefetchTransformationCards(campaign);
            List<CampaignCharacterSummaryResponse> summaries = new ArrayList<>();
            for (CharacterSheet sheet : campaign.getPlayerCharacters()) {
                summaries.add(toCampaignCharacterSummary(sheet));
            }
            for (CharacterSheet sheet : campaign.getNonPlayerCharacters()) {
                summaries.add(toCampaignCharacterSummary(sheet));
            }
            for (CharacterSheet sheet : campaign.getPendingCharacterSheets()) {
                summaries.add(toCampaignCharacterSummary(sheet));
            }
            builder.characterSummaries(summaries);
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
     * Builds a lightweight character summary for campaign context.
     *
     * @param sheet The character sheet entity
     * @return CampaignCharacterSummaryResponse with enriched data
     */
    private CampaignCharacterSummaryResponse toCampaignCharacterSummary(CharacterSheet sheet) {
        TransformationCard transformationCard = sheet.getTransformationCard();

        List<String> ancestryNames = sheet.getAncestryCards().stream()
                .map(card -> card.getName())
                .collect(Collectors.toList());

        List<String> subclassNames = sheet.getSubclassCards().stream()
                .map(card -> card.getName())
                .collect(Collectors.toList());

        List<String> classNames = sheet.getSubclassCards().stream()
                .map(card -> card.getSubclassPath().getAssociatedClass().getName())
                .distinct()
                .collect(Collectors.toList());

        return CampaignCharacterSummaryResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .level(sheet.getLevel())
                .ownerId(sheet.getOwner().getId())
                .ownerUsername(sheet.getOwner().getUsername())
                .ancestryNames(ancestryNames)
                .subclassNames(subclassNames)
                .classNames(classNames)
                .transformationEnabled(sheet.isTransformationEnabled())
                .transformationCardId(transformationCard != null ? transformationCard.getId() : null)
                .transformationCardName(transformationCard != null ? transformationCard.getName() : null)
                .companionsEnabled(sheet.isCompanionsEnabled())
                .build();
    }

    /**
     * Initializes the lazy transformation card association for every character sheet in the
     * campaign with a single query.
     * <p>
     * The character summaries expose the assigned transformation card's name; without this
     * pre-fetch Hibernate would issue one additional select per sheet while building the roster.
     * </p>
     *
     * @param campaign The campaign whose character sheets are being summarized
     */
    private void prefetchTransformationCards(Campaign campaign) {
        Set<Long> sheetIds = new HashSet<>();
        campaign.getPlayerCharacters().forEach(sheet -> sheetIds.add(sheet.getId()));
        campaign.getNonPlayerCharacters().forEach(sheet -> sheetIds.add(sheet.getId()));
        campaign.getPendingCharacterSheets().forEach(sheet -> sheetIds.add(sheet.getId()));

        if (!sheetIds.isEmpty()) {
            characterSheetRepository.findAllByIdInWithTransformationCard(sheetIds);
        }
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
