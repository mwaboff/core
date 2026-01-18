package com.aboff.core.controller;

import com.aboff.core.model.dto.request.ChangePasswordRequest;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    /**
     * Constructs a new UserController with required dependencies.
     *
     * @param userService the user service
     */
    public UserController(UserService userService) {
        this.userService = userService;
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
     * Change current user's password.
     * POST /api/users/me/change-password
     * Invalidates all existing tokens and clears current session cookie.
     *
     * @param request        the change password request
     * @param authentication the current authentication object
     * @param response       the HTTP servlet response to clear the cookie
     */
    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletResponse response) {

        Long userId = extractUserId(authentication);
        userService.changePassword(userId, request, response);
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
