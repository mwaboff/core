package com.aboff.core.controller;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.OAuth2UserProvisioningService;
import com.aboff.core.util.CookieUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * Development-only authentication controller.
 * <p>
 * Provides the {@code /api/auth/dev-login} endpoint for local development and
 * testing without requiring a real OAuth provider. This controller is activated
 * only when the {@code dev} Spring profile is active.
 * </p>
 *
 * <p><strong>WARNING:</strong> This controller MUST NOT be deployed in
 * production. Verify the {@code dev} profile is not active in any
 * production environment.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Profile("dev")
public class DevAuthController {

    private final OAuth2UserProvisioningService provisioningService;
    private final AuthenticationService authenticationService;
    private final CookieUtil cookieUtil;
    private final AuditLogger auditLogger;

    /**
     * Constructs a new DevAuthController with required dependencies.
     *
     * @param provisioningService   the OAuth2 user provisioning service
     * @param authenticationService the authentication service
     * @param cookieUtil            the cookie utility
     * @param auditLogger           the audit logger
     */
    public DevAuthController(
            OAuth2UserProvisioningService provisioningService,
            AuthenticationService authenticationService,
            CookieUtil cookieUtil,
            AuditLogger auditLogger) {
        this.provisioningService = provisioningService;
        this.authenticationService = authenticationService;
        this.cookieUtil = cookieUtil;
        this.auditLogger = auditLogger;
    }

    /**
     * Logs a startup warning when the dev auth endpoint is active.
     */
    @PostConstruct
    void warnDevEndpointActive() {
        log.warn("DEV AUTH ENDPOINT IS ACTIVE — /api/auth/dev-login is available. "
                + "Do NOT deploy with the 'dev' profile in production!");
    }

    /**
     * Dev-only login endpoint. Creates or retrieves a user and issues a JWT session.
     * Only available when the {@code dev} profile is active.
     * POST /api/auth/dev-login
     *
     * @param request      the dev login request containing email and optional role
     * @param httpRequest  the HTTP servlet request
     * @param httpResponse the HTTP servlet response for setting the auth cookie
     * @return the user profile response
     */
    @PostMapping("/dev-login")
    public UserResponse devLogin(
            @RequestBody DevLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        long startTime = System.nanoTime();
        AuditContext ctx = AuditContext.forIp(httpRequest.getRemoteAddr()).build();
        auditLogger.requestReceived(ctx, "POST", "/api/auth/dev-login");

        log.warn("Dev login used for email: {} — this endpoint must NOT be available in production",
                request.getEmail());

        User user = provisioningService.findOrCreateDevUser(request.getEmail(), request.getRole(), request.getUsername());

        String ipAddress = extractIpAddress(httpRequest);
        String deviceInfo = extractDeviceInfo(httpRequest);
        AuthenticationService.LoginResult result = authenticationService.issueToken(user, "dev", deviceInfo, ipAddress);

        cookieUtil.setAuthCookie(httpResponse, result.getToken());

        auditLogger.requestCompleted(ctx, "POST", "/api/auth/dev-login", startTime);
        return result.getUserResponse();
    }

    /**
     * Extracts the client's IP address from the request, respecting proxy headers.
     *
     * @param request the HTTP request
     * @return the client IP address string
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    /**
     * Extracts a truncated User-Agent string for device tracking.
     *
     * @param request the HTTP request
     * @return the User-Agent string, truncated to 500 characters, or {@code null} if absent
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    /**
     * Request body for the dev login endpoint.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevLoginRequest {
        /**
         * The email address for the dev user.
         */
        private String email;

        /**
         * The desired role for the dev user; defaults to USER if null.
         */
        private Role role;

        /**
         * Optional explicit username for the dev user. If omitted or blank,
         * a username is generated from the email address.
         */
        private String username;
    }
}
