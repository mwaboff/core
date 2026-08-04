package com.aboff.core.service;

import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.request.UpdateUserRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.UsernameHistory;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.UsernameHistoryRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user accounts.
 * Handles user profile updates, password changes, and account deletion.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UsernameHistoryRepository usernameHistoryRepository;
    private final AuthenticationService authenticationService;
    private final RoleHierarchyService roleHierarchyService;
    private final CookieUtil cookieUtil;
    private final AuditLogger auditLogger;

    /**
     * Constructs a new UserService with required dependencies.
     *
     * @param userRepository            the user repository
     * @param usernameHistoryRepository repository for persistent username-change
     *                                  records
     * @param authenticationService     the authentication service
     * @param roleHierarchyService      the role hierarchy service
     * @param cookieUtil                the cookie utility
     * @param auditLogger               the audit logger
     */
    public UserService(
            UserRepository userRepository,
            UsernameHistoryRepository usernameHistoryRepository,
            AuthenticationService authenticationService,
            RoleHierarchyService roleHierarchyService,
            CookieUtil cookieUtil,
            AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.usernameHistoryRepository = usernameHistoryRepository;
        this.authenticationService = authenticationService;
        this.roleHierarchyService = roleHierarchyService;
        this.cookieUtil = cookieUtil;
        this.auditLogger = auditLogger;
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
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Authentication authentication) {
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

        AuditContext ctx = AuditContext.forUser(authentication)
                .withTargetUserId(userId)
                .build();
        auditLogger.log(AuditAction.USER_PROFILE_UPDATED, ctx,
                String.format("user_id: %d", userId));

        return mapToUserResponse(user);
    }

    /**
     * Sets the username for a first-time OAuth user who has not yet chosen a username.
     * <p>
     * Only allowed when {@code usernameChosen} is {@code false}. Once set,
     * {@code usernameChosen} is flipped to {@code true} and the user will no longer
     * be redirected to the choose-username page after OAuth login.
     * </p>
     *
     * @param userId   the ID of the user selecting a username
     * @param username the desired username
     * @return the updated user response
     * @throws UserNotFoundException      if the user is not found
     * @throws IllegalStateException      if the user has already chosen a username
     * @throws UserAlreadyExistsException if the username is already taken (case-insensitive)
     */
    @Transactional
    public UserResponse chooseUsername(Long userId, String username, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getUsernameChosen())) {
            throw new IllegalStateException("Username has already been chosen");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        String previousUsername = user.getUsername();
        user.setUsername(username);
        user.setUsernameChosen(true);
        user = userRepository.save(user);

        usernameHistoryRepository.save(UsernameHistory.builder()
                .userId(user.getId())
                .previousUsername(previousUsername)
                .newUsername(username)
                .changedByUserId(user.getId())
                .build());

        AuditContext ctx = AuditContext.forUser(authentication).build();
        auditLogger.log(AuditAction.USER_USERNAME_CHOSEN, ctx,
                String.format("username: %s (user_id: %d)", username, userId));

        return mapToUserResponse(user);
    }

    /**
     * Soft deletes a user account.
     *
     * @param userId   the ID of the user to delete
     * @param response the HTTP response to clear cookies
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional
    public void deleteUser(Long userId, HttpServletResponse response, Authentication authentication) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete user
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all user's tokens
        authenticationService.invalidateAllUserTokens(userId);

        // Clear AUTH_TOKEN cookie
        cookieUtil.clearAuthCookie(response);

        AuditContext ctx = AuditContext.forUser(authentication)
                .withTargetUserId(userId)
                .build();
        auditLogger.log(AuditAction.USER_ACCOUNT_DELETED, ctx,
                String.format("user_id: %d, username: %s", userId, user.getUsername()));
    }

    /**
     * Maps a User entity to a redacted UserResponse DTO for a given viewer.
     * <p>
     * This is the single source of truth for hiding a user's private profile fields
     * ({@code email}, {@code avatarUrl}, {@code timezone}) from other users. Callers outside
     * this service that need to embed a {@link UserResponse} in another response
     * (e.g. a character sheet's {@code expand=owner} or a campaign's
     * {@code expand=creator,gameMasters,players}) must route through this method rather than
     * building a {@code UserResponse} directly, so a non-self, non-privileged viewer never
     * receives another user's private fields.
     * </p>
     * <p>
     * Full profile fields are included when the viewer is the target user themself or holds
     * MODERATOR/ADMIN/OWNER role. A {@code null} viewer authentication (unauthenticated context)
     * is treated as neither, so the response is fully redacted.
     * </p>
     *
     * @param targetUser the user entity being mapped
     * @param viewerAuth the authentication object for the viewer; may be null
     * @return the target user's response DTO, redacted unless the viewer is self or privileged
     */
    public UserResponse mapToUserResponse(User targetUser, Authentication viewerAuth) {
        boolean isOwnProfile = false;
        boolean hasPrivilegedRole = false;

        if (viewerAuth != null && viewerAuth.getPrincipal() instanceof CustomUserDetails viewerDetails) {
            isOwnProfile = viewerDetails.getUserId().equals(targetUser.getId());
            hasPrivilegedRole = roleHierarchyService.hasModeratorOrHigher(viewerDetails);
        }

        return mapToUserResponse(targetUser, isOwnProfile || hasPrivilegedRole, hasPrivilegedRole);
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
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .usernameChosen(user.getUsernameChosen());

        if (fullInfo) {
            builder.email(user.getEmail())
                    .timezone(user.getTimezone())
                    .lastModifiedAt(user.getLastModifiedAt());
        }

        if (privilegedInfo) {
            builder.deletedAt(user.getDeletedAt())
                    .bannedAt(user.getBannedAt())
                    .banReason(user.getBanReason())
                    .lastSeenAt(user.getLastSeenAt());
        }

        return builder.build();
    }
}
