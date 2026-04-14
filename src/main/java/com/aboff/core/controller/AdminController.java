package com.aboff.core.controller;

import com.aboff.core.exception.UserNotFoundException;
import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.request.ChangeRoleRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.RoleHierarchyService;
import com.aboff.core.service.SearchIndexService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for admin operations.
 * Handles role-based administrative tasks such as banning users,
 * changing user roles, and triggering search index rebuilds.
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

        private final UserRepository userRepository;
        private final RoleHierarchyService roleHierarchyService;
        private final AuthenticationService authenticationService;
        private final SearchIndexService searchIndexService;
        private final AuditLogger auditLogger;

        /**
         * Constructs a new AdminController with required dependencies.
         *
         * @param userRepository        the user repository
         * @param roleHierarchyService  the role hierarchy service
         * @param authenticationService the authentication service
         * @param searchIndexService    the search index service
         * @param auditLogger           the audit logger
         */
        public AdminController(
                        UserRepository userRepository,
                        RoleHierarchyService roleHierarchyService,
                        AuthenticationService authenticationService,
                        SearchIndexService searchIndexService,
                        AuditLogger auditLogger) {
                this.userRepository = userRepository;
                this.roleHierarchyService = roleHierarchyService;
                this.authenticationService = authenticationService;
                this.searchIndexService = searchIndexService;
                this.auditLogger = auditLogger;
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
                        @AuthenticationPrincipal CustomUserDetails currentUser,
                        Authentication authentication,
                        HttpServletRequest httpRequest) {

                long startTime = System.nanoTime();
                AuditContext ctx = AuditContext.forUser(authentication)
                                .withIp(httpRequest.getRemoteAddr())
                                .withTargetUserId(userId)
                                .build();
                auditLogger.requestReceived(ctx, "POST", "/api/admin/users/" + userId + "/ban");

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

                auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/ban", startTime);
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
                        @AuthenticationPrincipal CustomUserDetails currentUser,
                        Authentication authentication,
                        HttpServletRequest httpRequest) {

                long startTime = System.nanoTime();
                AuditContext ctx = AuditContext.forUser(authentication)
                                .withIp(httpRequest.getRemoteAddr())
                                .withTargetUserId(userId)
                                .build();
                auditLogger.requestReceived(ctx, "POST", "/api/admin/users/" + userId + "/unban");

                // Find the target user
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Validate that the current user can modify the target user
                roleHierarchyService.canModifyUser(currentUser.getUser(), targetUser);

                // Unban the user
                targetUser.unban();
                userRepository.save(targetUser);

                auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/unban", startTime);
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
                        @Valid @RequestBody ChangeRoleRequest request,
                        Authentication authentication,
                        HttpServletRequest httpRequest) {

                long startTime = System.nanoTime();
                AuditContext ctx = AuditContext.forUser(authentication)
                                .withIp(httpRequest.getRemoteAddr())
                                .withTargetUserId(userId)
                                .build();
                auditLogger.requestReceived(ctx, "POST", "/api/admin/users/" + userId + "/change-role");

                // Find the target user
                User targetUser = userRepository.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("User not found"));

                // Update the role
                targetUser.setRole(request.getNewRole());
                userRepository.save(targetUser);

                // Invalidate all user's tokens to force re-authentication with new role
                authenticationService.invalidateAllUserTokens(userId);

                auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/change-role", startTime);
                return mapToUserResponse(targetUser);
        }

        /**
         * Rebuild the full-text search index by clearing all entries and re-indexing every
         * active entity from the source repositories.
         *
         * <p>If a {@code type} query parameter is provided, only that entity type is rebuilt;
         * otherwise, every supported {@link SearchableEntityType} is rebuilt.
         *
         * <p>This is an expensive operation intended for admin-triggered recovery scenarios
         * (e.g., after a bulk SQL data fix that bypassed JPA events, or to repair a corrupted
         * index). Only accessible by the OWNER role.
         *
         * <p>POST /api/admin/search/reindex
         * <br>POST /api/admin/search/reindex?type=WEAPON
         *
         * @param type optional entity type to reindex; if omitted, all types are rebuilt
         * @return a map containing the total number of entities indexed and the type processed
         */
        @PostMapping("/search/reindex")
        @PreAuthorize("hasRole('OWNER')")
        public Map<String, Object> reindexSearch(
                        @RequestParam(required = false) SearchableEntityType type,
                        Authentication authentication,
                        HttpServletRequest httpRequest) {

                long startTime = System.nanoTime();
                AuditContext ctx = AuditContext.forUser(authentication)
                                .withIp(httpRequest.getRemoteAddr())
                                .build();
                auditLogger.requestReceived(ctx, "POST", "/api/admin/search/reindex");

                int indexed;
                String scope;
                if (type == null) {
                        indexed = searchIndexService.reindexAll();
                        scope = "ALL";
                } else {
                        indexed = searchIndexService.reindexAll(type);
                        scope = type.name();
                }

                auditLogger.requestCompleted(ctx, "POST", "/api/admin/search/reindex", startTime);
                return Map.of(
                                "scope", scope,
                                "indexed", indexed);
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
                                .usernameChosen(user.getUsernameChosen())
                                .build();
        }
}
