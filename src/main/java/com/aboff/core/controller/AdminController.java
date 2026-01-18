package com.aboff.core.controller;

import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.dto.request.ChangeRoleRequest;
import com.aboff.core.model.dto.response.LoginAttemptResponse;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.LoginAttempt;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.LoginAttemptRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for admin operations.
 * Handles role-based administrative tasks such as viewing login history,
 * banning users, and changing user roles.
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

        private final LoginAttemptRepository loginAttemptRepository;
        private final UserRepository userRepository;
        private final RoleHierarchyService roleHierarchyService;
        private final AuthenticationService authenticationService;

        /**
         * Constructs a new AdminController with required dependencies.
         *
         * @param loginAttemptRepository the login attempt repository
         * @param userRepository         the user repository
         * @param roleHierarchyService   the role hierarchy service
         * @param authenticationService  the authentication service
         */
        public AdminController(
                        LoginAttemptRepository loginAttemptRepository,
                        UserRepository userRepository,
                        RoleHierarchyService roleHierarchyService,
                        AuthenticationService authenticationService) {
                this.loginAttemptRepository = loginAttemptRepository;
                this.userRepository = userRepository;
                this.roleHierarchyService = roleHierarchyService;
                this.authenticationService = authenticationService;
        }

        /**
         * Get all login attempts for security monitoring.
         * Only accessible by OWNER and ADMIN roles.
         * GET /api/admin/login-history?limit=100
         *
         * @param limit optional maximum number of login attempts to return
         * @return list of all login attempts (limited if specified)
         */
        @GetMapping("/login-history")
        @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
        public List<LoginAttemptResponse> getLoginHistory(
                        @RequestParam(required = false) Integer limit) {
                List<LoginAttempt> attempts = loginAttemptRepository.findAll();
                return attempts.stream()
                                .limit(limit != null ? limit : Long.MAX_VALUE)
                                .map(this::mapToLoginAttemptResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get login attempts for a specific user.
         * Only accessible by OWNER and ADMIN roles.
         * GET /api/admin/login-history/user/{userId}?limit=100
         *
         * @param userId the ID of the user to get login history for
         * @param limit  optional maximum number of login attempts to return
         * @return list of login attempts for the specified user (limited if specified)
         */
        @GetMapping("/login-history/user/{userId}")
        @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
        public List<LoginAttemptResponse> getLoginHistoryByUserId(
                        @PathVariable Long userId,
                        @RequestParam(required = false) Integer limit) {
                List<LoginAttempt> attempts = loginAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
                return attempts.stream()
                                .limit(limit != null ? limit : Long.MAX_VALUE)
                                .map(this::mapToLoginAttemptResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get login attempts for a specific IP address.
         * Only accessible by OWNER and ADMIN roles.
         * GET /api/admin/login-history/ip/{ipAddress}?limit=100
         *
         * @param ipAddress the IP address to get login history for
         * @param limit     optional maximum number of login attempts to return
         * @return list of login attempts for the specified IP address (limited if
         *         specified)
         */
        @GetMapping("/login-history/ip/{ipAddress}")
        @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
        public List<LoginAttemptResponse> getLoginHistoryByIpAddress(
                        @PathVariable String ipAddress,
                        @RequestParam(required = false) Integer limit) {
                List<LoginAttempt> attempts = loginAttemptRepository.findByIpAddressOrderByAttemptedAtDesc(ipAddress);
                return attempts.stream()
                                .limit(limit != null ? limit : Long.MAX_VALUE)
                                .map(this::mapToLoginAttemptResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Ban a user by setting their banned flag.
         * Only users with a higher role can ban others.
         * Accessible by OWNER, ADMIN, and MODERATOR roles.
         * POST /api/admin/users/{userId}/ban
         *
         * @param userId      the ID of the user to ban
         * @param currentUser the currently authenticated user
         * @return the banned user's response
         * @throws UserNotFoundException                                     if the
         *                                                                   target user
         *                                                                   is not
         *                                                                   found
         * @throws com.aboff.core.exception.InsufficientPermissionsException if the
         *                                                                   current
         *                                                                   user's role
         *                                                                   is not
         *                                                                   higher
         *                                                                   than the
         *                                                                   target
         *                                                                   user's role
         */
        @PostMapping("/users/{userId}/ban")
        @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
        public UserResponse banUser(
                        @PathVariable Long userId,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                // Find the target user
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Validate that the current user can modify the target user
                roleHierarchyService.canModifyUser(currentUser.getUser(), targetUser);

                // Ban the user
                targetUser.ban();
                userRepository.save(targetUser);

                // Invalidate all the user's tokens
                authenticationService.invalidateAllUserTokens(userId);

                return mapToUserResponse(targetUser);
        }

        /**
         * Unban a user by clearing their banned flag.
         * Only users with a higher role can unban others.
         * Accessible by OWNER, ADMIN, and MODERATOR roles.
         * POST /api/admin/users/{userId}/unban
         *
         * @param userId      the ID of the user to unban
         * @param currentUser the currently authenticated user
         * @return the unbanned user's response
         * @throws UserNotFoundException                                     if the
         *                                                                   target user
         *                                                                   is not
         *                                                                   found
         * @throws com.aboff.core.exception.InsufficientPermissionsException if the
         *                                                                   current
         *                                                                   user's role
         *                                                                   is not
         *                                                                   higher
         *                                                                   than the
         *                                                                   target
         *                                                                   user's role
         */
        @PostMapping("/users/{userId}/unban")
        @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
        public UserResponse unbanUser(
                        @PathVariable Long userId,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                // Find the target user
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Validate that the current user can modify the target user
                roleHierarchyService.canModifyUser(currentUser.getUser(), targetUser);

                // Unban the user
                targetUser.unban();
                userRepository.save(targetUser);

                return mapToUserResponse(targetUser);
        }

        /**
         * Change a user's role.
         * Only accessible by OWNER role.
         * POST /api/admin/users/{userId}/change-role
         *
         * @param userId  the ID of the user whose role should be changed
         * @param request the request containing the new role
         * @return the updated user's response
         * @throws UserNotFoundException if the target user is not found
         */
        @PostMapping("/users/{userId}/change-role")
        @PreAuthorize("hasRole('OWNER')")
        @ResponseStatus(HttpStatus.OK)
        public UserResponse changeUserRole(
                        @PathVariable Long userId,
                        @Valid @RequestBody ChangeRoleRequest request) {

                // Find the target user
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Update the role
                targetUser.setRole(request.getNewRole());
                userRepository.save(targetUser);

                // Invalidate all user's tokens to force re-authentication with new role
                authenticationService.invalidateAllUserTokens(userId);

                return mapToUserResponse(targetUser);
        }

        /**
         * Maps LoginAttempt entity to LoginAttemptResponse DTO.
         *
         * @param attempt the login attempt entity
         * @return the login attempt response DTO
         */
        private LoginAttemptResponse mapToLoginAttemptResponse(LoginAttempt attempt) {
                return LoginAttemptResponse.builder()
                                .id(attempt.getId())
                                .userId(attempt.getUserId())
                                .usernameAttempted(attempt.getUsernameAttempted())
                                .success(attempt.getSuccess())
                                .failureReason(attempt.getFailureReason())
                                .ipAddress(attempt.getIpAddress())
                                .userAgent(attempt.getUserAgent())
                                .attemptedAt(attempt.getAttemptedAt())
                                .build();
        }

        /**
         * Maps User entity to UserResponse DTO.
         *
         * @param user the user entity
         * @return the user response DTO
         */
        private UserResponse mapToUserResponse(User user) {
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
}
