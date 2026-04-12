package com.aboff.core.controller;

import com.aboff.core.model.dto.dh.response.CampaignResponse;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.UserService;
import com.aboff.core.service.dh.CampaignService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-related operations.
 * Handles profile retrieval, updates, password changes, and account deletion.
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;
    private final CampaignService campaignService;

    /**
     * Constructs a new UserController with required dependencies.
     *
     * @param userService the user service
     * @param campaignService the campaign service
     */
    public UserController(UserService userService, CampaignService campaignService) {
        this.userService = userService;
        this.campaignService = campaignService;
    }

    /**
     * Get user profile by ID or for the current authenticated user.
     * GET /api/users/me
     * GET /api/users/{userId}
     *
     * @param userId         optional user ID (numeric or "me")
     * @param authentication the current authentication object
     * @return the user response containing profile details
     */
    @GetMapping({ "/me", "/{userId}" })
    public UserResponse getCurrentUser(
            @PathVariable(required = false) String userId,
            Authentication authentication) {
        String targetId = (userId != null) ? userId : "me";
        return userService.getUserProfile(targetId, authentication);
    }

    /**
     * Get campaigns for a specific user.
     * GET /api/users/{userId}/campaigns
     * <p>
     * Accessible by the target user themselves or users with MODERATOR+ role.
     * Supports pagination and response expansion.
     * </p>
     *
     * @param userId         the target user's ID
     * @param page           the page number (zero-based, default 0)
     * @param size           the page size (default 20, max 100)
     * @param expand         optional comma-separated list of fields to expand
     * @param authentication the current authentication object
     * @return paginated list of campaigns the user is involved in
     */
    @GetMapping("/{userId}/campaigns")
    public ResponseEntity<PagedResponse<CampaignResponse>> getUserCampaigns(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String expand,
            Authentication authentication) {
        return ResponseEntity.ok(campaignService.getUserCampaigns(userId, page, size, expand, authentication));
    }

    /**
     * Update current user's profile.
     * PATCH /api/users/me
     *
     * @param request        the update request containing new details
     * @param authentication the current authentication object
     * @return the updated user response
     */
    @PatchMapping("/me")
    public UserResponse updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return userService.updateUser(userId, request);
    }

    /**
     * Delete (soft delete) current user's account.
     * DELETE /api/users/me
     * Invalidates all tokens and clears current session cookie.
     *
     * @param authentication the current authentication object
     * @param response       the HTTP servlet response to clear the cookie
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser(
            Authentication authentication,
            HttpServletResponse response) {

        Long userId = extractUserId(authentication);
        userService.deleteUser(userId, response);
    }

    /**
     * Extracts user ID from Spring Security Authentication.
     *
     * @param authentication the authentication object
     * @return the user ID
     */
    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}
