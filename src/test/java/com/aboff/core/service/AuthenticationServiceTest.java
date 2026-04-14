package com.aboff.core.service;

import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationService}.
 * <p>
 * Tests cover token issuance, logout, and bulk token invalidation.
 * The password-based register/login methods were removed in the OAuth migration;
 * OAuth flow tests will be added in Phase 3.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private ActiveTokenRepository activeTokenRepository;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @Mock
        private AuditLogger auditLogger;

        @InjectMocks
        private AuthenticationService authenticationService;

        private User testUser;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .createdAt(LocalDateTime.now())
                                .lastModifiedAt(LocalDateTime.now())
                                .build();
        }

        // ==================== ISSUE TOKEN TESTS ====================

        @Test
        void issueToken_ValidUser_ReturnsLoginResultWithToken() {
                // Arrange
                when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt-token");
                when(jwtTokenProvider.hashToken("jwt-token")).thenReturn("token-hash");
                when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
                when(activeTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                AuthenticationService.LoginResult result = authenticationService.issueToken(
                                testUser, "Mozilla/5.0", "127.0.0.1");

                // Assert
                assertThat(result.getToken()).isEqualTo("jwt-token");
                assertThat(result.getUserResponse()).isNotNull();
                assertThat(result.getUserResponse().getUsername()).isEqualTo("testuser");
                assertThat(result.getUserResponse().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        void issueToken_PersistsActiveToken() {
                // Arrange
                when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt-token");
                when(jwtTokenProvider.hashToken("jwt-token")).thenReturn("token-hash");
                when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);
                when(activeTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                authenticationService.issueToken(testUser, "device-info", "10.0.0.1");

                // Assert - verify token was persisted with correct fields
                ArgumentCaptor<ActiveToken> captor = ArgumentCaptor.forClass(ActiveToken.class);
                verify(activeTokenRepository).save(captor.capture());
                ActiveToken persisted = captor.getValue();
                assertThat(persisted.getUserId()).isEqualTo(1L);
                assertThat(persisted.getTokenHash()).isEqualTo("token-hash");
                assertThat(persisted.getDeviceInfo()).isEqualTo("device-info");
                assertThat(persisted.getIpAddress()).isEqualTo("10.0.0.1");
                assertThat(persisted.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        // ==================== LOGOUT TESTS ====================

        @Test
        void logout_ValidToken_RevokesToken() {
                // Arrange
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(1L)
                                .tokenHash("token-hash")
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();

                when(jwtTokenProvider.hashToken("raw-token")).thenReturn("token-hash");
                when(activeTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(activeToken));
                when(activeTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                // Act
                authenticationService.logout("raw-token");

                // Assert
                verify(activeTokenRepository).save(argThat(t -> t.getRevokedAt() != null));
        }

        @Test
        void logout_TokenNotFound_DoesNothing() {
                // Arrange
                when(jwtTokenProvider.hashToken("unknown-token")).thenReturn("unknown-hash");
                when(activeTokenRepository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());

                // Act
                authenticationService.logout("unknown-token");

                // Assert - no save call made
                verify(activeTokenRepository, never()).save(any());
        }

        // ==================== INVALIDATE ALL USER TOKENS TESTS ====================

        @Test
        void invalidateAllUserTokens_CallsRevokeAll() {
                // Act
                authenticationService.invalidateAllUserTokens(1L);

                // Assert
                verify(activeTokenRepository).revokeAllUserTokens(eq(1L), any(LocalDateTime.class));
        }
}
