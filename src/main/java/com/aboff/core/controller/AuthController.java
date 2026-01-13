package com.aboff.core.controller;

import com.aboff.core.model.dto.request.LoginRequest;
import com.aboff.core.model.dto.request.RegisterRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.service.AuthenticationService;
import com.aboff.core.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles registration, login, and logout.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final CookieUtil cookieUtil;

    public AuthController(
            AuthenticationService authenticationService,
            CookieUtil cookieUtil) {
        this.authenticationService = authenticationService;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Register a new user.
     * POST /api/auth/register
     *
     * @param request the registration request containing user details
     * @return the registered user response
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    /**
     * Login user and set httpOnly authentication cookie.
     * POST /api/auth/login
     *
     * @param request      the login request containing username and password
     * @param httpRequest  the HTTP servlet request
     * @param httpResponse the HTTP servlet response to set the cookie
     * @return the logged-in user response
     */
    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        // Authenticate user and get login result
        AuthenticationService.LoginResult result = authenticationService.login(request, httpRequest);

        // Set httpOnly cookie with JWT token
        cookieUtil.setAuthCookie(httpResponse, result.getToken());

        // Return user information (token is in cookie, not in response body)
        return result.getUserResponse();
    }

    /**
     * Logout user by revoking token and clearing cookie.
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

        // Extract token from cookie
        String token = extractTokenFromCookie(httpRequest);

        if (token != null) {
            // Revoke the token in database
            authenticationService.logout(token);
        }

        // Clear the AUTH_TOKEN cookie
        cookieUtil.clearAuthCookie(httpResponse);
    }

    /**
     * Extracts JWT token from AUTH_TOKEN cookie.
     *
     * @param request the HTTP servlet request
     * @return the token string, or null if not found
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
