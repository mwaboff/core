package com.aboff.core.security;

import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Filter that authenticates users via JWT tokens in HTTP-only cookies.
 * Validates the token against the database and sets the security context.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ActiveTokenRepository activeTokenRepository;
    private final String cookieName;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            ActiveTokenRepository activeTokenRepository,
            @Value("${jwt.cookie.name}") String cookieName) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.activeTokenRepository = activeTokenRepository;
        this.cookieName = cookieName;
    }

    /**
     * Filters incoming requests to check for a valid JWT authentication cookie.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.trace("Processing authentication for request: {} {}", request.getMethod(), requestPath);

        try {
            String jwt = extractTokenFromCookie(request);

            if (jwt == null) {
                log.trace("No auth cookie found for request: {}", requestPath);
            } else {
                log.debug("Found auth cookie, validating token for request: {}", requestPath);

                // Validate JWT structure and signature
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                log.debug("Token validated for userId: {}", userId);

                // Hash the token to check against database
                String tokenHash = jwtTokenProvider.hashToken(jwt);

                // Verify token is active and not revoked in database
                Optional<ActiveToken> activeToken = activeTokenRepository.findValidToken(
                        tokenHash,
                        LocalDateTime.now());

                if (activeToken.isEmpty()) {
                    log.debug("Token not found or expired in database for userId: {}", userId);
                } else if (!activeToken.get().isValid()) {
                    log.debug("Token is revoked for userId: {}", userId);
                } else {
                    // Load user from database
                    Optional<User> userOptional = userRepository.findById(userId);

                    if (userOptional.isEmpty()) {
                        log.warn("User not found for valid token, userId: {}", userId);
                    } else {
                        User user = userOptional.get();

                        // Check if user is not soft-deleted, account is not locked, and user is not
                        // banned
                        if (user.isDeleted()) {
                            log.debug("User account is deleted, userId: {}", userId);
                        } else if (user.isAccountLocked()) {
                            log.debug("User account is locked, userId: {}", userId);
                        } else if (user.isBanned()) {
                            log.debug("User account is banned, userId: {}", userId);
                        } else {
                            CustomUserDetails userDetails = new CustomUserDetails(user);

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Successfully authenticated user: {} for request: {}", user.getUsername(),
                                    requestPath);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Log but don't block the filter chain
            // Token validation failures should result in 401 from entry point
            log.debug("Could not set user authentication in security context: {}", ex.getMessage());
            log.trace("Authentication failure details", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the AUTH_TOKEN cookie.
     *
     * @param request the HTTP request
     * @return the token string, or null if not found
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
