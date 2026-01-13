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
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    /**
     * Login user and set httpOnly authentication cookie
     * POST /api/auth/login
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
     * Logout user by revoking token and clearing cookie
     * POST /api/auth/logout
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
     * Extracts JWT token from AUTH_TOKEN cookie
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
