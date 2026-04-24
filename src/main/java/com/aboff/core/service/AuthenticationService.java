package com.aboff.core.service;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.LoginEvent;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.LoginEventRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling user authentication.
 * <p>
 * Manages token lifecycle (issuance and revocation) for authenticated users.
 * The registration and credential-based login methods from the previous
 * password-auth flow have been removed; authentication is now handled
 * exclusively via OAuth — see Phase 3 for the OAuth service implementation.
 * </p>
 */
@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ActiveTokenRepository activeTokenRepository;
    private final LoginEventRepository loginEventRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogger auditLogger;

    /**
     * Constructs a new AuthenticationService with required dependencies.
     *
     * @param userRepository        the user repository
     * @param activeTokenRepository the active token repository
     * @param loginEventRepository  repository for persistent login audit rows
     * @param jwtTokenProvider      the JWT token provider
     * @param auditLogger           the audit logger
     */
    public AuthenticationService(
            UserRepository userRepository,
            ActiveTokenRepository activeTokenRepository,
            LoginEventRepository loginEventRepository,
            JwtTokenProvider jwtTokenProvider,
            AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.activeTokenRepository = activeTokenRepository;
        this.loginEventRepository = loginEventRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogger = auditLogger;
    }

    /**
     * Issues a JWT for the given user and persists an {@link ActiveToken}
     * record plus a durable {@link LoginEvent} row.
     *
     * @param user       the authenticated user
     * @param provider   the authentication provider identifier (e.g.
     *                   {@code "google"}, {@code "dev"}); may be {@code null}
     * @param deviceInfo device info string (e.g. trimmed User-Agent)
     * @param ipAddress  the client's IP address
     * @return a {@link LoginResult} containing the user response and raw JWT
     */
    @Transactional
    public LoginResult issueToken(User user, String provider, String deviceInfo, String ipAddress) {
        String jwt = jwtTokenProvider.generateToken(user);
        String tokenHash = jwtTokenProvider.hashToken(jwt);

        ActiveToken activeToken = ActiveToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000))
                .build();

        activeTokenRepository.save(activeToken);

        LoginEvent loginEvent = LoginEvent.builder()
                .userId(user.getId())
                .provider(provider)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();
        loginEventRepository.save(loginEvent);

        AuditContext ctx = AuditContext.forIp(ipAddress)
                .build();
        auditLogger.log(AuditAction.USER_LOGIN, ctx,
                String.format("user_id: %d, username: %s, provider: %s",
                        user.getId(), user.getUsername(), provider));

        return LoginResult.builder()
                .userResponse(mapToUserResponse(user))
                .token(jwt)
                .build();
    }

    /**
     * Logs out the user by revoking the given token.
     *
     * @param token the raw JWT token to revoke
     */
    @Transactional
    public void logout(String token) {
        String tokenHash = jwtTokenProvider.hashToken(token);
        activeTokenRepository.findByTokenHash(tokenHash).ifPresent(activeToken -> {
            activeToken.revoke();
            activeTokenRepository.save(activeToken);

            AuditContext ctx = AuditContext.forIp(activeToken.getIpAddress())
                    .build();
            auditLogger.log(AuditAction.USER_LOGOUT, ctx,
                    String.format("user_id: %d", activeToken.getUserId()));
        });
    }

    /**
     * Invalidates all active tokens for a user, forcing re-authentication on all devices.
     *
     * @param userId the user ID whose tokens should be invalidated
     */
    @Transactional
    public void invalidateAllUserTokens(Long userId) {
        activeTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());

        AuditContext ctx = AuditContext.forIp(null)
                .withTargetUserId(userId)
                .build();
        auditLogger.log(AuditAction.USER_TOKENS_INVALIDATED, ctx,
                String.format("user_id: %d", userId));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
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
    }

    /**
     * Result object containing both user response and JWT token.
     */
    @Data
    @Builder
    public static class LoginResult {
        /**
         * The user profile response.
         */
        private UserResponse userResponse;

        /**
         * The ephemeral JWT token.
         */
        private String token;
    }
}
