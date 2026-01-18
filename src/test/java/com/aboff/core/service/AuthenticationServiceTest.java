package com.aboff.core.service;

import com.aboff.core.exception.AccountLockedException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private ActiveTokenRepository activeTokenRepository;

        @Mock
        private LoginAttemptService loginAttemptService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @Mock
        private PasswordValidator passwordValidator;

        @Mock
        private HttpServletRequest httpRequest;

        private AuthenticationService authenticationService;

        private static final String DEFAULT_AVATAR_URL = "https://api.dicebear.com/7.x/avatars/svg?seed=default";
        private static final String DEFAULT_TIMEZONE = "UTC";
        private static final int MAX_FAILED_ATTEMPTS = 5;
        private static final int LOCKOUT_DURATION_MINUTES = 30;
        private static final int FAILED_ATTEMPT_WINDOW_MINUTES = 15;

        @BeforeEach
        void setUp() {
                // Create service with configuration values using reflection
                authenticationService = new AuthenticationService(
                                userRepository,
                                activeTokenRepository,
                                loginAttemptService,
                                passwordEncoder,
                                jwtTokenProvider,
                                passwordValidator,
                                MAX_FAILED_ATTEMPTS,
                                LOCKOUT_DURATION_MINUTES,
                                FAILED_ATTEMPT_WINDOW_MINUTES,
                                DEFAULT_AVATAR_URL,
                                DEFAULT_TIMEZONE);
        }

        // ==================== REGISTER TESTS ====================

        @Test
        void register_ValidData_CreatesUserWithHashedPassword() {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("Password123!")
                                .build();

                when(userRepository.existsByUsername("testuser")).thenReturn(false);
                when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
                when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");

                User savedUser = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .avatarUrl(DEFAULT_AVATAR_URL)
                                .timezone(DEFAULT_TIMEZONE)
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();

                when(userRepository.save(any(User.class))).thenReturn(savedUser);

                // Act
                UserResponse response = authenticationService.register(request);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getUsername()).isEqualTo("testuser");
                assertThat(response.getEmail()).isEqualTo("test@example.com");

                verify(passwordValidator).validatePassword("Password123!");
                verify(passwordEncoder).encode("Password123!");

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(userCaptor.capture());
                User capturedUser = userCaptor.getValue();
                assertThat(capturedUser.getPasswordHash()).isEqualTo("hashedPassword");
                assertThat(capturedUser.getAvatarUrl()).isEqualTo(DEFAULT_AVATAR_URL);
                assertThat(capturedUser.getTimezone()).isEqualTo(DEFAULT_TIMEZONE);
        }

        @Test
        void register_ExistingUsername_ThrowsUserAlreadyExistsException() {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("Password123!")
                                .build();

                when(userRepository.existsByUsername("testuser")).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.register(request))
                                .isInstanceOf(UserAlreadyExistsException.class)
                                .hasMessage("Username already taken");

                verify(userRepository, never()).save(any());
        }

        @Test
        void register_ExistingEmail_ThrowsUserAlreadyExistsException() {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("Password123!")
                                .build();

                when(userRepository.existsByUsername("testuser")).thenReturn(false);
                when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.register(request))
                                .isInstanceOf(UserAlreadyExistsException.class)
                                .hasMessage("Email already registered");

                verify(userRepository, never()).save(any());
        }

        @Test
        void register_CustomAvatarAndTimezone_UsesProvidedValues() {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("Password123!")
                                .avatarUrl("https://custom.avatar/image.png")
                                .timezone("America/New_York")
                                .build();

                when(userRepository.existsByUsername("testuser")).thenReturn(false);
                when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
                when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");

                User savedUser = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .avatarUrl("https://custom.avatar/image.png")
                                .timezone("America/New_York")
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();

                when(userRepository.save(any(User.class))).thenReturn(savedUser);

                // Act
                UserResponse response = authenticationService.register(request);

                // Assert
                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(userCaptor.capture());
                User capturedUser = userCaptor.getValue();
                assertThat(capturedUser.getAvatarUrl()).isEqualTo("https://custom.avatar/image.png");
                assertThat(capturedUser.getTimezone()).isEqualTo("America/New_York");
        }

        // ==================== LOGIN TESTS ====================

        @Test
        void login_ValidCredentials_ReturnsTokenAndStoresHash() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("Password123!")
                                .build();

                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .avatarUrl(DEFAULT_AVATAR_URL)
                                .timezone(DEFAULT_TIMEZONE)
                                .failedLoginAttempts(0)
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
                when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");
                when(jwtTokenProvider.hashToken("jwt-token")).thenReturn("token-hash");
                when(jwtTokenProvider.getExpirationMs()).thenReturn(2592000000L);
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act
                AuthenticationService.LoginResult result = authenticationService.login(request, httpRequest);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getToken()).isEqualTo("jwt-token");
                assertThat(result.getUserResponse().getUsername()).isEqualTo("testuser");

                verify(activeTokenRepository).save(any(ActiveToken.class));
                verify(loginAttemptService).recordAttempt(any(LoginAttempt.class));
                verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 0));
        }

        @Test
        void login_UserNotFound_ThrowsBadCredentialsException() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("nonexistent")
                                .password("Password123!")
                                .build();

                when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
                when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessage("Invalid username or password");

                verify(loginAttemptService).recordAttempt(any(LoginAttempt.class));
        }

        @Test
        void login_SoftDeletedUser_ThrowsBadCredentialsException() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("deleteduser")
                                .password("Password123!")
                                .build();

                User deletedUser = User.builder()
                                .id(1L)
                                .username("deleteduser")
                                .email("deleted@example.com")
                                .passwordHash("hashedPassword")
                                .deletedAt(LocalDateTime.now())
                                .build();

                when(userRepository.findByUsername("deleteduser")).thenReturn(Optional.of(deletedUser));
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessage("Invalid username or password");

                verify(loginAttemptService).recordAttempt(any(LoginAttempt.class));
        }

        @Test
        void login_BannedUser_ThrowsBadCredentialsException() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("banneduser")
                                .password("Password123!")
                                .build();

                User bannedUser = User.builder()
                                .id(1L)
                                .username("banneduser")
                                .email("banned@example.com")
                                .passwordHash("hashedPassword")
                                .bannedAt(LocalDateTime.now())
                                .build();

                when(userRepository.findByUsername("banneduser")).thenReturn(Optional.of(bannedUser));
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessage("Invalid username or password");

                verify(loginAttemptService)
                                .recordAttempt(argThat(attempt -> "USER_BANNED".equals(attempt.getFailureReason())));
        }

        @Test
        void login_AccountLocked_ThrowsAccountLockedException() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("lockeduser")
                                .password("Password123!")
                                .build();

                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(30);
                User lockedUser = User.builder()
                                .id(1L)
                                .username("lockeduser")
                                .email("locked@example.com")
                                .passwordHash("hashedPassword")
                                .accountLockedUntil(lockedUntil)
                                .build();

                when(userRepository.findByUsername("lockeduser")).thenReturn(Optional.of(lockedUser));
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(AccountLockedException.class)
                                .hasMessageContaining("Account is temporarily locked");

                verify(loginAttemptService).recordAttempt(any(LoginAttempt.class));
        }

        @Test
        void login_InvalidPassword_IncrementsFailedAttempts() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("WrongPassword!")
                                .build();

                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .failedLoginAttempts(2)
                                .build();

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("WrongPassword!", "hashedPassword")).thenReturn(false);
                when(loginAttemptService.getRecentFailedAttempts(anyString(), anyInt()))
                                .thenReturn(new ArrayList<>());
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(BadCredentialsException.class);

                verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 3));
                verify(loginAttemptService).recordAttempt(any(LoginAttempt.class));
        }

        @Test
        void login_FifthFailedAttempt_LocksAccount() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("WrongPassword!")
                                .build();

                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .failedLoginAttempts(4)
                                .build();

                List<LoginAttempt> recentFailures = List.of(
                                LoginAttempt.builder().build(),
                                LoginAttempt.builder().build(),
                                LoginAttempt.builder().build(),
                                LoginAttempt.builder().build());

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("WrongPassword!", "hashedPassword")).thenReturn(false);
                when(loginAttemptService.getRecentFailedAttempts("testuser", FAILED_ATTEMPT_WINDOW_MINUTES))
                                .thenReturn(recentFailures);
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act & Assert
                assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                                .isInstanceOf(BadCredentialsException.class);

                verify(userRepository).save(
                                argThat(u -> u.getFailedLoginAttempts() == 5 && u.getAccountLockedUntil() != null));
        }

        @Test
        void login_SuccessAfterFailedAttempts_ResetsFailedAttempts() {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("Password123!")
                                .build();

                User user = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .failedLoginAttempts(3)
                                .avatarUrl(DEFAULT_AVATAR_URL)
                                .timezone(DEFAULT_TIMEZONE)
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
                when(jwtTokenProvider.generateToken(user)).thenReturn("jwt-token");
                when(jwtTokenProvider.hashToken("jwt-token")).thenReturn("token-hash");
                when(jwtTokenProvider.getExpirationMs()).thenReturn(2592000000L);
                when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
                when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
                when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

                // Act
                authenticationService.login(request, httpRequest);

                // Assert
                verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 0));
        }

        // ==================== LOGOUT TESTS ====================

        @Test
        void logout_ValidToken_RevokesToken() {
                // Arrange
                String token = "jwt-token";
                String tokenHash = "token-hash";

                ActiveToken activeToken = ActiveToken.builder()
                                .id(1L)
                                .userId(1L)
                                .tokenHash(tokenHash)
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();

                when(jwtTokenProvider.hashToken(token)).thenReturn(tokenHash);
                when(activeTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));

                // Act
                authenticationService.logout(token);

                // Assert
                verify(activeTokenRepository).save(argThat(t -> t.getRevokedAt() != null));
        }

        @Test
        void logout_TokenNotFound_DoesNotThrowException() {
                // Arrange
                String token = "jwt-token";
                String tokenHash = "token-hash";

                when(jwtTokenProvider.hashToken(token)).thenReturn(tokenHash);
                when(activeTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

                // Act & Assert - should not throw
                authenticationService.logout(token);

                verify(activeTokenRepository, never()).save(any());
        }

        // ==================== INVALIDATE ALL TOKENS TESTS ====================

        @Test
        void invalidateAllUserTokens_RevokesAllTokens() {
                // Arrange
                Long userId = 1L;

                // Act
                authenticationService.invalidateAllUserTokens(userId);

                // Assert
                verify(activeTokenRepository).revokeAllUserTokens(eq(userId), any(LocalDateTime.class));
        }
}
