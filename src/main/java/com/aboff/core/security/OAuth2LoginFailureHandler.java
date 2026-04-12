package com.aboff.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles failed OAuth2 login events.
 * <p>
 * Logs the failure reason and redirects the browser to the frontend login page
 * with an error query parameter so the UI can display an appropriate message.
 * </p>
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String frontendBaseUrl;

    /**
     * Constructs a new OAuth2LoginFailureHandler.
     *
     * @param frontendBaseUrl the base URL of the frontend application
     */
    public OAuth2LoginFailureHandler(@Value("${application.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Logs the OAuth2 failure and redirects to the frontend login error page.
     *
     * @param request   the HTTP request
     * @param response  the HTTP response
     * @param exception the authentication exception describing the failure
     * @throws IOException if a redirect error occurs
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        log.error("OAuth2 login failed: {}", exception.getMessage());
        getRedirectStrategy().sendRedirect(request, response,
                frontendBaseUrl + "/login?error=oauth_failed");
    }
}
