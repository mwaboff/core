package com.aboff.core.security;

import com.aboff.core.model.entity.User;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.service.OAuth2UserProvisioningService;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2 login events.
 * <p>
 * Provisions the user account via {@link OAuth2UserProvisioningService}, issues
 * a JWT session cookie via {@link AuthenticationService}, and redirects the
 * browser to the frontend. Banned or deleted users are redirected to the error
 * page instead.
 * </p>
 */
@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2UserProvisioningService provisioningService;
    private final AuthenticationService authenticationService;
    private final CookieUtil cookieUtil;
    private final String frontendBaseUrl;

    /**
     * Constructs a new OAuth2LoginSuccessHandler with required dependencies.
     *
     * @param provisioningService   the OAuth2 user provisioning service
     * @param authenticationService the authentication service
     * @param cookieUtil            the cookie utility
     * @param frontendBaseUrl       the base URL of the frontend application
     */
    public OAuth2LoginSuccessHandler(
            OAuth2UserProvisioningService provisioningService,
            AuthenticationService authenticationService,
            CookieUtil cookieUtil,
            @Value("${application.frontend.base-url}") String frontendBaseUrl) {
        this.provisioningService = provisioningService;
        this.authenticationService = authenticationService;
        this.cookieUtil = cookieUtil;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Provisions the user, issues a JWT cookie, and redirects to the frontend.
     * Banned or deleted users are redirected to the login error page.
     *
     * @param request        the HTTP request
     * @param response       the HTTP response
     * @param authentication the OAuth2 authentication token
     * @throws IOException if a redirect error occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        User user = provisioningService.findOrCreateUserFromOAuth2(provider, oAuth2User);

        if (user.isBanned() || user.isDeleted()) {
            log.warn("OAuth login rejected for banned/deleted user: {}", user.getUsername());
            getRedirectStrategy().sendRedirect(request, response,
                    frontendBaseUrl + "/auth/callback?error=banned");
            return;
        }

        // Issue JWT session
        String ipAddress = extractIpAddress(request);
        String deviceInfo = extractDeviceInfo(request);
        AuthenticationService.LoginResult result = authenticationService.issueToken(user, deviceInfo, ipAddress);

        cookieUtil.setAuthCookie(response, result.getToken());
        log.info("OAuth login successful for user '{}' via {}", user.getUsername(), provider);

        boolean needsUsername = !Boolean.TRUE.equals(user.getUsernameChosen());
        String redirectUrl = frontendBaseUrl + "/auth/callback?needsUsername=" + needsUsername;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Extracts the client's IP address from the request, respecting proxy headers.
     *
     * @param request the HTTP request
     * @return the client IP address string
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
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
}
