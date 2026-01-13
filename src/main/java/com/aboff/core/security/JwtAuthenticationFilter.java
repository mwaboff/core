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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractTokenFromCookie(request);

            if (jwt != null) {
                // Validate JWT structure and signature
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // Hash the token to check against database
                String tokenHash = jwtTokenProvider.hashToken(jwt);

                // Verify token is active and not revoked in database
                Optional<ActiveToken> activeToken = activeTokenRepository.findValidToken(
                        tokenHash,
                        LocalDateTime.now()
                );

                if (activeToken.isPresent() && activeToken.get().isValid()) {
                    // Load user from database
                    Optional<User> userOptional = userRepository.findById(userId);

                    if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        // Check if user is not soft-deleted and account is not locked
                        if (!user.isDeleted() && !user.isAccountLocked()) {
                            CustomUserDetails userDetails = new CustomUserDetails(user);

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Log but don't block the filter chain
            // Token validation failures should result in 401 from entry point
            logger.debug("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the AUTH_TOKEN cookie
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
