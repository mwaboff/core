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

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current authenticated user
     * GET /api/users/me
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        return userService.getCurrentUser(authentication);
    }

    /**
     * Update current user's profile
     * PATCH /api/users/me
     */
    @PatchMapping("/me")
    public UserResponse updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return userService.updateUser(userId, request);
    }

    /**
     * Change current user's password
     * POST /api/users/me/change-password
     * Invalidates all existing tokens and clears current session cookie
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
     * Delete (soft delete) current user's account
     * DELETE /api/users/me
     * Invalidates all tokens and clears current session cookie
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
     * Extracts user ID from Spring Security Authentication
     */
    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}
