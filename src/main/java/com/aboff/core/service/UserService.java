package com.aboff.core.service;

import com.aboff.core.exception.InvalidPasswordException;
import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.dto.request.ChangePasswordRequest;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.util.CookieUtil;
import com.aboff.core.util.PasswordValidator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user accounts.
 * Handles user profile updates, password changes, and account deletion.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final RoleHierarchyService roleHierarchyService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final CookieUtil cookieUtil;

    /**
     * Constructs a new UserService with required dependencies.
     *
     * @param userRepository        the user repository
     * @param authenticationService the authentication service
     * @param passwordEncoder       the password encoder
     * @param passwordValidator     the password validator
     * @param cookieUtil            the cookie utility
     */
    public UserService(
            UserRepository userRepository,
            AuthenticationService authenticationService,
            RoleHierarchyService roleHierarchyService,
            PasswordEncoder passwordEncoder,
            PasswordValidator passwordValidator,
            CookieUtil cookieUtil) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
        this.roleHierarchyService = roleHierarchyService;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Gets the current authenticated user.
     *
     * @param authentication the Spring Security authentication object
     * @return the current user's response
     * @throws UserNotFoundException if the user cannot be found
     */
    public UserResponse getCurrentUser(Authentication authentication) {
        return getUserProfile("me", authentication);
    }

    /**
     * Gets a user profile by ID.
     * If the ID is "me", returns the current authenticated user's profile.
     * If the ID matches the current user, returns full profile.
     * If the ID is different, returns only username and createdAt.
     *
     * @param userIdStr      the user ID to fetch (numeric or "me")
     * @param authentication the Spring Security authentication object
     * @return the user profile response
     * @throws UserNotFoundException if the user is not found
     */
    public UserResponse getUserProfile(String userIdStr, Authentication authentication) {
        User currentUser = extractUserFromAuthentication(authentication);
        User targetUser;

        if ("me".equalsIgnoreCase(userIdStr)) {
            targetUser = currentUser;
        } else {
            try {
                Long targetId = Long.parseLong(userIdStr);
                targetUser = userRepository.findById(targetId)
                        .orElseThrow(() -> new UserNotFoundException("User not found"));
            } catch (NumberFormatException e) {
                // If it's not a number and not "me", we can treat it as not found
                throw new UserNotFoundException("User not found: " + userIdStr);
            }
        }

        boolean isOwnProfile = currentUser.getId().equals(targetUser.getId());
        boolean hasPrivilegedRole = roleHierarchyService.isPrivilegedRole(currentUser.getRole());

        // Full info for self OR privileged callers.
        // Privileged info ONLY for privileged callers.
        return mapToUserResponse(targetUser, isOwnProfile || hasPrivilegedRole, hasPrivilegedRole);
    }

    /**
     * Updates user profile information (email, avatarUrl, timezone).
     *
     * @param userId  the ID of the user to update
     * @param request the update request containing new details
     * @return the updated user's response
     * @throws UserNotFoundException      if the user is not found
     * @throws UserAlreadyExistsException if the new email is already taken
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if email is being updated and is already taken by another user (case-insensitive)
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already registered");
            }
            user.setEmail(request.getEmail());
        }

        // Update avatar URL if provided
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Update timezone if provided
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Changes user password and invalidates all existing tokens (force re-login on
     * all devices).
     *
     * @param userId   the ID of the user
     * @param request  the change password request
     * @param response the HTTP response to clear cookies
     * @throws UserNotFoundException    if the user is not found
     * @throws InvalidPasswordException if the current password is incorrect or new
     *                                  password is weak
     */
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request,
            HttpServletResponse response) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Validate new password strength
        passwordValidator.validatePassword(request.getNewPassword());

        // Hash and save new password
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        // Invalidate ALL user's tokens (force re-login on all devices)
        authenticationService.invalidateAllUserTokens(userId);

        // Clear AUTH_TOKEN cookie for current session
        cookieUtil.clearAuthCookie(response);
    }

    /**
     * Soft deletes a user account.
     *
     * @param userId   the ID of the user to delete
     * @param response the HTTP response to clear cookies
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional
    public void deleteUser(Long userId, HttpServletResponse response) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete user
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all user's tokens
        authenticationService.invalidateAllUserTokens(userId);

        // Clear AUTH_TOKEN cookie
        cookieUtil.clearAuthCookie(response);
    }

    /**
     * Extracts User entity from Spring Security Authentication.
     *
     * @param authentication the authentication object
     * @return the user entity
     * @throws UserNotFoundException if the user cannot be found
     */
    private User extractUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UserNotFoundException("User not found");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Maps User entity to UserResponse DTO.
     *
     * @param user the user entity
     * @return the user response DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return mapToUserResponse(user, true, false);
    }

    /**
     * Maps User entity to UserResponse DTO with optional field restriction.
     *
     * @param user           the user entity
     * @param fullInfo       whether to include all non-sensitive fields
     * @param privilegedInfo whether to include administrative fields
     * @return the user response DTO
     */
    private UserResponse mapToUserResponse(User user, boolean fullInfo, boolean privilegedInfo) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt());

        if (fullInfo) {
            builder.email(user.getEmail())
                    .timezone(user.getTimezone())
                    .lastModifiedAt(user.getLastModifiedAt());
        }

        if (privilegedInfo) {
            builder.accountLockedUntil(user.getAccountLockedUntil())
                    .failedLoginAttempts(user.getFailedLoginAttempts())
                    .deletedAt(user.getDeletedAt())
                    .bannedAt(user.getBannedAt());
        }

        return builder.build();
    }
}
