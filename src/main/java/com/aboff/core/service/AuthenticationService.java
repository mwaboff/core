package com.aboff.core.service;

import com.aboff.core.exception.AccountLockedException;
import com.aboff.core.exception.InvalidPasswordException;
import com.aboff.core.exception.UserAlreadyExistsException;
import com.aboff.core.model.dto.request.LoginRequest;
import com.aboff.core.model.dto.request.RegisterRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.LoginAttempt;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.aboff.core.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling user authentication and registration.
 * Manages user lifecycle events like login, logout, and registration.
 */
@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ActiveTokenRepository activeTokenRepository;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;

    private final int maxFailedAttempts;
    private final int lockoutDurationMinutes;
    private final int failedAttemptWindowMinutes;
    private final String defaultAvatarUrl;
    private final String defaultTimezone;

    /**
     * Constructs a new AuthenticationService with required dependencies and
     * configuration.
     *
     * @param userRepository             the user repository
     * @param activeTokenRepository      the active token repository
     * @param loginAttemptService        the login attempt service
     * @param passwordEncoder            the password encoder
     * @param jwtTokenProvider           the JWT token provider
     * @param passwordValidator          the password validator
     * @param maxFailedAttempts          max failed login attempts before lockout
     * @param lockoutDurationMinutes     duration of account lockout in minutes
     * @param failedAttemptWindowMinutes time window for tracking failed attempts
     * @param defaultAvatarUrl           default avatar URL for new users
     * @param defaultTimezone            default timezone for new users
     */
    public AuthenticationService(
            UserRepository userRepository,
            ActiveTokenRepository activeTokenRepository,
            LoginAttemptService loginAttemptService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PasswordValidator passwordValidator,
            @Value("${application.security.max-failed-attempts}") int maxFailedAttempts,
            @Value("${application.security.lockout-duration-minutes}") int lockoutDurationMinutes,
            @Value("${application.security.failed-attempt-window-minutes}") int failedAttemptWindowMinutes,
            @Value("${application.user.default-avatar-url}") String defaultAvatarUrl,
            @Value("${application.user.default-timezone}") String defaultTimezone) {
        this.userRepository = userRepository;
        this.activeTokenRepository = activeTokenRepository;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordValidator = passwordValidator;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
        this.failedAttemptWindowMinutes = failedAttemptWindowMinutes;
        this.defaultAvatarUrl = defaultAvatarUrl;
        this.defaultTimezone = defaultTimezone;
    }

    /**
     * Registers a new user.
     *
     * @param request the registration request containing user details
     * @return the registered user's response
     * @throws UserAlreadyExistsException if username or email is already taken
     * @throws InvalidPasswordException   if the password does not meet requirements
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.debug("Registration attempt for username: {}, email: {}", request.getUsername(), request.getEmail());

        // Check if username already exists (case-insensitive)
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        // Check if email already exists (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Validate password strength
        passwordValidator.validatePassword(request.getPassword());

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Set default values if not provided
        String avatarUrl = request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()
                ? request.getAvatarUrl()
                : defaultAvatarUrl;

        String timezone = request.getTimezone() != null && !request.getTimezone().isBlank()
                ? request.getTimezone()
                : defaultTimezone;

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .avatarUrl(avatarUrl)
                .timezone(timezone)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getUsername());
        return mapToUserResponse(user);
    }

    /**
     * Authenticates a user and returns JWT token with user information.
     *
     * @param request     the login request containing credentials
     * @param httpRequest the raw HTTP request for metadata extraction
     * @return the login result containing user info and token
     * @throws BadCredentialsException if authentication fails
     * @throws AccountLockedException  if the account is locked
     */
    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String usernameOrEmail = request.getUsernameOrEmail();
        log.debug("Login attempt for: {}", usernameOrEmail);
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = extractUserAgent(httpRequest);

        // Find user by username or email (case-insensitive)
        User user = userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail))
                .orElse(null);

        if (user == null) {
            // Record failed attempt with no user ID
            log.warn("Login failed - user not found: {}", usernameOrEmail);
            recordLoginAttempt(usernameOrEmail, null, false, "USER_NOT_FOUND", ipAddress, userAgent);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Check if account is soft-deleted
        if (user.isDeleted()) {
            recordLoginAttempt(usernameOrEmail, user.getId(), false, "USER_DELETED", ipAddress, userAgent);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Check if account is banned
        if (user.isBanned()) {
            log.warn("Login failed - user is banned: {}", usernameOrEmail);
            recordLoginAttempt(usernameOrEmail, user.getId(), false, "USER_BANNED", ipAddress, userAgent);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login failed - account locked: {} until {}", usernameOrEmail, user.getAccountLockedUntil());
            recordLoginAttempt(usernameOrEmail, user.getId(), false, "ACCOUNT_LOCKED", ipAddress, userAgent);
            throw new AccountLockedException(
                    "Account is temporarily locked due to multiple failed login attempts",
                    user.getAccountLockedUntil());
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Increment failed attempts
            user.incrementFailedAttempts();

            // Check recent failed attempts and lock if necessary
            List<LoginAttempt> recentFailures = loginAttemptService.getRecentFailedAttempts(
                    usernameOrEmail, failedAttemptWindowMinutes);

            if (recentFailures.size() + 1 >= maxFailedAttempts) {
                log.warn("Account locked due to {} failed attempts: {}", recentFailures.size() + 1, usernameOrEmail);
                user.lockAccount(lockoutDurationMinutes);
            }

            userRepository.save(user);

            // Record failed attempt
            log.warn("Login failed - invalid credentials: {}", usernameOrEmail);
            recordLoginAttempt(usernameOrEmail, user.getId(), false, "INVALID_CREDENTIALS", ipAddress, userAgent);

            throw new BadCredentialsException("Invalid username or password");
        }

        // Successful login - reset failed attempts
        user.resetFailedAttempts();
        userRepository.save(user);

        // Generate JWT token
        String jwt = jwtTokenProvider.generateToken(user);
        String tokenHash = jwtTokenProvider.hashToken(jwt);

        // Store token in database
        ActiveToken activeToken = ActiveToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .deviceInfo(extractDeviceInfo(httpRequest))
                .ipAddress(ipAddress)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationMs() / 1000))
                .build();

        activeTokenRepository.save(activeToken);

        // Record successful login attempt
        recordLoginAttempt(usernameOrEmail, user.getId(), true, null, ipAddress, userAgent);

        log.info("User logged in successfully: {}", user.getUsername());

        // Return user response and token
        return LoginResult.builder()
                .userResponse(mapToUserResponse(user))
                .token(jwt)
                .build();
    }

    /**
     * Logs out the user by revoking the token.
     *
     * @param token the JWT token to revoke
     */
    @Transactional
    public void logout(String token) {
        log.debug("Processing logout request");
        String tokenHash = jwtTokenProvider.hashToken(token);
        activeTokenRepository.findByTokenHash(tokenHash).ifPresent(activeToken -> {
            log.info("Revoking token for userId: {}", activeToken.getUserId());
            activeToken.revoke();
            activeTokenRepository.save(activeToken);
        });
    }

    /**
     * Invalidates all tokens for a user (used on password change).
     *
     * @param userId the user ID to invalidate tokens for
     */
    @Transactional
    public void invalidateAllUserTokens(Long userId) {
        log.info("Invalidating all tokens for userId: {}", userId);
        activeTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    private void recordLoginAttempt(String usernameAttempted, Long userId, boolean success,
            String failureReason, String ipAddress, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .userId(userId)
                .usernameAttempted(usernameAttempted)
                .success(success)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        loginAttemptService.recordAttempt(attempt);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = extractUserAgent(request);
        return userAgent != null && userAgent.length() > 500
                ? userAgent.substring(0, 500)
                : userAgent;
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
