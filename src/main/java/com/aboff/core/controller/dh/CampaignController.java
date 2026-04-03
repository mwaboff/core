package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignRequest;
import com.aboff.core.model.dto.dh.response.CampaignInviteResponse;
import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.dh.response.JoinCampaignResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.service.dh.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Campaign entities.
 * <p>
 * Provides endpoints for CRUD operations on campaigns, user management (game masters
 * and players), and character sheet management (pending, PC, NPC) in the Daggerheart
 * TTRPG system.
 * </p>
 * <p>
 * Access control:
 * </p>
 * <ul>
 *   <li>List all: Requires MODERATOR/ADMIN/OWNER role</li>
 *   <li>View single: Campaign participants OR MODERATOR/ADMIN/OWNER role</li>
 *   <li>Create: Any authenticated user</li>
 *   <li>Update/Delete: Campaign creator OR MODERATOR/ADMIN/OWNER role</li>
 *   <li>Add/Remove GMs: Campaign creator OR MODERATOR/ADMIN/OWNER role</li>
 *   <li>Add/Remove Players: Campaign creator/GM OR MODERATOR/ADMIN/OWNER role</li>
 *   <li>Submit Character: Character sheet owner who is a player</li>
 *   <li>Approve/Reject/Add NPC/Remove: Campaign creator/GM OR MODERATOR/ADMIN/OWNER role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/dh/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    // ==================== MY CAMPAIGNS & INVITE ENDPOINTS ====================

    /**
     * Retrieves a paginated list of campaigns where the current user is involved.
     * <p>
     * Any authenticated user can access this endpoint to see their own campaigns.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The authentication object containing the current user
     * @return Paginated response containing the user's campaigns with 200 OK status
     */
    @GetMapping("/mine")
    public ResponseEntity<PagedResponse<CampaignResponse>> getMyCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        PagedResponse<CampaignResponse> response =
                campaignService.getMyCampaigns(page, size, expand, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Joins a campaign via invite token.
     * <p>
     * Any authenticated user can join a campaign using a valid invite link.
     * </p>
     *
     * @param token The invite token
     * @param authentication The authentication object containing the current user
     * @return JoinCampaignResponse with 200 OK status
     */
    @PostMapping("/join/{token}")
    public ResponseEntity<JoinCampaignResponse> joinCampaign(
            @PathVariable String token,
            Authentication authentication) {

        JoinCampaignResponse response = campaignService.joinViaInvite(token, authentication);
        return ResponseEntity.ok(response);
    }

    // ==================== CRUD ENDPOINTS ====================

    /**
     * Retrieves a paginated list of campaigns.
     * <p>
     * Supports optional filtering by creator ID and name.
     * Only users with MODERATOR/ADMIN/OWNER role can access this endpoint.
     * </p>
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 20, max: 100)
     * @param creatorId Optional filter for campaign creator ID
     * @param name Optional filter for campaign name (case-insensitive partial match)
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing campaigns with 200 OK status
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'OWNER')")
    public ResponseEntity<PagedResponse<CampaignResponse>> getAllCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String expand) {

        PagedResponse<CampaignResponse> response =
                campaignService.getAllCampaigns(page, size, creatorId, name, expand);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single campaign by ID.
     * <p>
     * Campaign participants or users with MODERATOR/ADMIN/OWNER role can view.
     * </p>
     *
     * @param id The campaign ID
     * @param expand Comma-separated list of relationships to expand
     * @param authentication The authentication object containing the current user
     * @return Campaign response with 200 OK status
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(
            @PathVariable Long id,
            @RequestParam(required = false) String expand,
            Authentication authentication) {

        CampaignResponse response = campaignService.getCampaignById(id, expand, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new campaign.
     * <p>
     * All authenticated users can create a campaign. The authenticated user
     * becomes the creator and is automatically added as a game master.
     * </p>
     *
     * @param request The campaign creation request
     * @param authentication The authentication object containing the current user
     * @return Created campaign response with 201 Created status
     */
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            Authentication authentication) {

        CampaignResponse response = campaignService.createCampaign(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request,
            Authentication authentication) {

        CampaignResponse response = campaignService.updateCampaign(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a campaign (soft delete).
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role
     * can delete a campaign.
     * </p>
     *
     * @param id The campaign ID to delete
     * @param authentication The authentication object containing the current user
     * @return Empty response with 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable Long id,
            Authentication authentication) {

        campaignService.deleteCampaign(id, authentication);
        return ResponseEntity.noContent().build();
    }

    // ==================== CAMPAIGN LIFECYCLE ENDPOINTS ====================

    /**
     * Generates an invite link for the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can generate invites.
     * The invite is valid for 24 hours and single-use.
     * </p>
     *
     * @param id The campaign ID
     * @param authentication The authentication object containing the current user
     * @return CampaignInviteResponse with 201 Created status
     */
    @PostMapping("/{id}/invites")
    public ResponseEntity<CampaignInviteResponse> generateInvite(
            @PathVariable Long id,
            Authentication authentication) {

        CampaignInviteResponse response = campaignService.generateInvite(id, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ends a campaign, locking it from further modifications.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role can end a campaign.
     * </p>
     *
     * @param id The campaign ID
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<CampaignResponse> endCampaign(
            @PathVariable Long id,
            Authentication authentication) {

        CampaignResponse response = campaignService.endCampaign(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Allows a player to leave a campaign.
     * <p>
     * Only players can leave. Does not cascade-unlink character sheets.
     * Works on ended campaigns too.
     * </p>
     *
     * @param id The campaign ID
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<CampaignResponse> leaveCampaign(
            @PathVariable Long id,
            Authentication authentication) {

        CampaignResponse response = campaignService.leaveCampaign(id, authentication);
        return ResponseEntity.ok(response);
    }

    // ==================== USER MANAGEMENT ENDPOINTS ====================

    /**
     * Adds a user as a game master to the campaign.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role can add GMs.
     * </p>
     *
     * @param id The campaign ID
     * @param userId The user ID to add as game master
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/game-masters/{userId}")
    public ResponseEntity<CampaignResponse> addGameMaster(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {

        CampaignResponse response = campaignService.addGameMaster(id, userId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a user from the game masters of the campaign.
     * <p>
     * Only the campaign creator or users with MODERATOR/ADMIN/OWNER role can remove GMs.
     * The campaign creator cannot be removed.
     * </p>
     *
     * @param id The campaign ID
     * @param userId The user ID to remove from game masters
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @DeleteMapping("/{id}/game-masters/{userId}")
    public ResponseEntity<CampaignResponse> removeGameMaster(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {

        CampaignResponse response = campaignService.removeGameMaster(id, userId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a user as a player to the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can add players.
     * </p>
     *
     * @param id The campaign ID
     * @param userId The user ID to add as player
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/players/{userId}")
    public ResponseEntity<CampaignResponse> addPlayer(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {

        CampaignResponse response = campaignService.addPlayer(id, userId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Kicks a player from the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can kick players.
     * Cascades to remove all character sheets owned by the kicked player.
     * </p>
     *
     * @param id The campaign ID
     * @param userId The user ID to kick
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @DeleteMapping("/{id}/players/{userId}")
    public ResponseEntity<CampaignResponse> kickPlayer(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication authentication) {

        CampaignResponse response = campaignService.kickPlayer(id, userId, authentication);
        return ResponseEntity.ok(response);
    }

    // ==================== CHARACTER SHEET MANAGEMENT ENDPOINTS ====================

    /**
     * Submits a character sheet to the campaign for approval.
     * <p>
     * Only the character sheet owner who is a player in the campaign can submit.
     * </p>
     *
     * @param id The campaign ID
     * @param sheetId The character sheet ID to submit
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/character-sheets/{sheetId}/submit")
    public ResponseEntity<CampaignResponse> submitCharacterSheet(
            @PathVariable Long id,
            @PathVariable Long sheetId,
            Authentication authentication) {

        CampaignResponse response = campaignService.submitCharacterSheet(id, sheetId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Approves a pending character sheet, moving it to player characters.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can approve.
     * </p>
     *
     * @param id The campaign ID
     * @param sheetId The character sheet ID to approve
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/character-sheets/{sheetId}/approve")
    public ResponseEntity<CampaignResponse> approveCharacterSheet(
            @PathVariable Long id,
            @PathVariable Long sheetId,
            Authentication authentication) {

        CampaignResponse response = campaignService.approveCharacterSheet(id, sheetId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a pending character sheet, removing it from the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can reject.
     * </p>
     *
     * @param id The campaign ID
     * @param sheetId The character sheet ID to reject
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/character-sheets/{sheetId}/reject")
    public ResponseEntity<CampaignResponse> rejectCharacterSheet(
            @PathVariable Long id,
            @PathVariable Long sheetId,
            Authentication authentication) {

        CampaignResponse response = campaignService.rejectCharacterSheet(id, sheetId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a character sheet as an NPC to the campaign.
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can add NPCs.
     * </p>
     *
     * @param id The campaign ID
     * @param sheetId The character sheet ID to add as NPC
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @PostMapping("/{id}/npcs/{sheetId}")
    public ResponseEntity<CampaignResponse> addNonPlayerCharacter(
            @PathVariable Long id,
            @PathVariable Long sheetId,
            Authentication authentication) {

        CampaignResponse response = campaignService.addNonPlayerCharacter(id, sheetId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a character sheet from the campaign (from any collection).
     * <p>
     * Only the campaign creator/GM or users with MODERATOR/ADMIN/OWNER role can remove.
     * </p>
     *
     * @param id The campaign ID
     * @param sheetId The character sheet ID to remove
     * @param authentication The authentication object containing the current user
     * @return Updated campaign response with 200 OK status
     */
    @DeleteMapping("/{id}/character-sheets/{sheetId}")
    public ResponseEntity<CampaignResponse> removeCharacterSheet(
            @PathVariable Long id,
            @PathVariable Long sheetId,
            Authentication authentication) {

        CampaignResponse response = campaignService.removeCharacterSheet(id, sheetId, authentication);
        return ResponseEntity.ok(response);
    }
}
