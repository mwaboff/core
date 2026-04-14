package com.aboff.core.controller;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.UserService;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * <p>
 * Registration and credential-based login have been removed as part of the
 * migration to OAuth-only authentication. The OAuth callback endpoints will be
 * added in Phase 3. This controller retains the logout endpoint, which is
 * provider-agnostic.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final AuditLogger auditLogger;

    /**
     * Constructs a new AuthController with required dependencies.
     *
     * @param authenticationService the authentication service
     * @param userService           the user service
     * @param cookieUtil            the cookie utility
     * @param auditLogger           the audit logger
     */
    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            CookieUtil cookieUtil,
            AuditLogger auditLogger) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.cookieUtil = cookieUtil;
        this.auditLogger = auditLogger;
    }

    /**
     * Logout user by revoking token and clearing the auth cookie.
     * POST /api/auth/logout
     *
     * @param httpRequest    the HTTP servlet request
     * @param httpResponse   the HTTP servlet response to clear the cookie
     * @param authentication the current authentication object
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/auth/logout");

        String token = extractTokenFromCookie(httpRequest);

        if (token != null) {
            authenticationService.logout(token);
        }

        cookieUtil.clearAuthCookie(httpResponse);

        auditLogger.requestCompleted(ctx, "POST", "/api/auth/logout", startTime);
    }

    /**
     * Get the current authenticated user's profile.
     * GET /api/auth/me
     *
     * @param authentication the current authentication object
     * @return the current user's profile response
     */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication, HttpServletRequest httpRequest) {
        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "GET", "/api/auth/me");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserResponse result = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .createdAt(user.getCreatedAt())
                .lastModifiedAt(user.getLastModifiedAt())
                .usernameChosen(user.getUsernameChosen())
                .build();

        auditLogger.requestCompleted(ctx, "GET", "/api/auth/me", startTime);
        return result;
    }

    /**
     * Choose a username for the current user.
     * <p>
     * Only permitted when the user has not yet chosen a username (i.e.,
     * {@code usernameChosen} is {@code false}). This endpoint is called by
     * first-time OAuth users after being redirected to the choose-username page.
     * The user is already authenticated via the JWT cookie set during the OAuth
     * callback — no re-login is required.
     * </p>
     * POST /api/auth/choose-username
     *
     * @param request        the request containing the desired username
     * @param authentication the current authentication object
     * @return the updated user profile response
     * @throws IllegalStateException      if the user has already chosen a username
     * @throws com.aboff.core.exception.UserAlreadyExistsException if the username is taken
     */
    @PostMapping("/choose-username")
    public UserResponse chooseUsername(
            @Valid @RequestBody ChooseUsernameRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forUser(authentication)
                .withIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/auth/choose-username");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserResponse result = userService.chooseUsername(user.getId(), request.getUsername(), authentication);

        auditLogger.requestCompleted(ctx, "POST", "/api/auth/choose-username", startTime);
        return result;
    }

    /**
     * Request body for the choose-username endpoint.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChooseUsernameRequest {

        /**
         * The desired username. Must be 3–30 characters and contain only
         * letters, numbers, underscores, and hyphens.
         */
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username can only contain letters, numbers, underscores, and hyphens")
        private String username;
    }

    /**
     * Extracts the JWT token from the auth cookie.
     *
     * @param request the HTTP servlet request
     * @return the token string, or null if not present
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookieUtil.getCookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
