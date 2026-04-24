package com.aboff.core.controller;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.request.BanUserRequest;
import com.aboff.core.model.dto.request.ChangeRoleRequest;
import com.aboff.core.model.dto.request.UpdateAdminUserRequest;
import com.aboff.core.model.dto.response.AdminUserDetailResponse;
import com.aboff.core.model.dto.response.AdminUserSummaryResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AdminUserService;
import com.aboff.core.service.AuditLogger;
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
 * <p>
 * All mutation endpoints go through {@link AdminUserService} so the audit row
 * and the state change share a single transaction. This controller is a thin
 * HTTP adapter: request parsing, auth context, delegate.
 * </p>
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminUserService adminUserService;
    private final SearchIndexService searchIndexService;
    private final AuditLogger auditLogger;

    /**
     * Constructs a new {@code AdminController}.
     *
     * @param adminUserService   admin user service
     * @param searchIndexService search index service
     * @param auditLogger        SLF4J audit logger (kept in parallel with
     *                           the persistent {@code admin_action_log})
     */
    public AdminController(
            AdminUserService adminUserService,
            SearchIndexService searchIndexService,
            AuditLogger auditLogger) {
        this.adminUserService = adminUserService;
        this.searchIndexService = searchIndexService;
        this.auditLogger = auditLogger;
    }

    // ==================== USER MANAGER ENDPOINTS ====================

    /**
     * Returns a paged summary of users visible to the admin UI.
     *
     * <p>GET /api/admin/users</p>
     *
     * @param isBanned optional banned-state filter
     * @param role     optional role filter
     * @param username optional case-insensitive substring match on username
     * @param email    optional case-insensitive substring match on email
     * @param page     zero-based page (default 0)
     * @param size     page size (default 50, clamped to 100)
     * @param sort     sort property; one of {@code id|createdAt|lastSeenAt|username}
     * @param ascending direction flag (default false)
     * @return paged summary
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
    public PagedResponse<AdminUserSummaryResponse> listUsers(
            @RequestParam(required = false) Boolean isBanned,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "false") boolean ascending,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr())
                .build();
        auditLogger.requestReceived(ctx, "GET", "/api/admin/users");

        PagedResponse<AdminUserSummaryResponse> result = adminUserService.listUsers(
                isBanned, role, username, email, page, size, sort, ascending);

        auditLogger.requestCompleted(ctx, "GET", "/api/admin/users", startTime);
        return result;
    }

    /**
     * Returns the admin detail view of a single user.
     *
     * <p>GET /api/admin/users/{userId}</p>
     *
     * @param userId the user id
     * @param expand comma-separated expand list
     * @return the detail response
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
    public AdminUserDetailResponse getUserDetail(
            @PathVariable Long userId,
            @RequestParam(required = false) String expand,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr())
                .withTargetUserId(userId)
                .build();
        auditLogger.requestReceived(ctx, "GET", "/api/admin/users/" + userId);

        AdminUserDetailResponse result = adminUserService.getUserDetail(userId, expand);

        auditLogger.requestCompleted(ctx, "GET", "/api/admin/users/" + userId, startTime);
        return result;
    }

    /**
     * Applies a partial update to a user's admin-editable fields.
     *
     * <p>PATCH /api/admin/users/{userId}</p>
     *
     * @param userId      the target user id
     * @param request     partial update body
     * @param currentUser the authenticated admin
     * @return the refreshed detail response
     */
    @PatchMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
    public AdminUserDetailResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAdminUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr())
                .withTargetUserId(userId)
                .build();
        auditLogger.requestReceived(ctx, "PATCH", "/api/admin/users/" + userId);

        AdminUserDetailResponse result = adminUserService.updateUser(
                currentUser.getUser(), userId, request, httpRequest.getRemoteAddr());

        auditLogger.requestCompleted(ctx, "PATCH", "/api/admin/users/" + userId, startTime);
        return result;
    }

    // ==================== BAN / UNBAN ====================

    /**
     * Bans a user. Body is optional; if supplied, {@code reason} is stored on
     * the user record and included in the audit entry.
     *
     * <p>POST /api/admin/users/{userId}/ban</p>
     *
     * @param userId      the target user id
     * @param request     optional ban body
     * @param currentUser the authenticated admin
     * @return the refreshed detail response
     */
    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
    public AdminUserDetailResponse banUser(
            @PathVariable Long userId,
            @Valid @RequestBody(required = false) BanUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr())
                .withTargetUserId(userId)
                .build();
        auditLogger.requestReceived(ctx, "POST", "/api/admin/users/" + userId + "/ban");

        AdminUserDetailResponse result = adminUserService.banUser(
                currentUser.getUser(), userId, request, httpRequest.getRemoteAddr());

        auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/ban", startTime);
        return result;
    }

    /**
     * Unbans a user.
     *
     * <p>POST /api/admin/users/{userId}/unban</p>
     */
    @PostMapping("/users/{userId}/unban")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MODERATOR')")
    public AdminUserDetailResponse unbanUser(
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

        AdminUserDetailResponse result = adminUserService.unbanUser(
                currentUser.getUser(), userId, httpRequest.getRemoteAddr());

        auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/unban", startTime);
        return result;
    }

    /**
     * Legacy role-change endpoint. Retained for backwards compatibility with
     * existing callers; new work should use {@code PATCH /api/admin/users/{id}}
     * with {@code role}. Will be removed once the frontend migrates.
     *
     * <p>POST /api/admin/users/{userId}/change-role</p>
     */
    @Deprecated
    @PostMapping("/users/{userId}/change-role")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.OK)
    public AdminUserDetailResponse changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr())
                .withTargetUserId(userId)
                .build();
        auditLogger.requestReceived(ctx, "POST", "/api/admin/users/" + userId + "/change-role");

        AdminUserDetailResponse result = adminUserService.changeRole(
                currentUser.getUser(), userId, request.getNewRole(), httpRequest.getRemoteAddr());

        auditLogger.requestCompleted(ctx, "POST", "/api/admin/users/" + userId + "/change-role", startTime);
        return result;
    }

    // ==================== SEARCH REINDEX ====================

    /**
     * Rebuild the full-text search index.
     *
     * <p>POST /api/admin/search/reindex</p>
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
        return Map.of("scope", scope, "indexed", indexed);
    }
}
