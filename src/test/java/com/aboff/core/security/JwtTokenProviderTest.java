package com.aboff.core.security;

import com.aboff.core.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-algorithm";
    private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION_MS);
    }

    // ==================== TOKEN GENERATION TESTS ====================

    @Test
    void generateToken_ValidUser_ReturnsValidJWT() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        // Act
        String token = jwtTokenProvider.generateToken(user);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateToken_DifferentUsers_GeneratesDifferentTokens() {
        // Arrange
        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .build();

        // Act
        String token1 = jwtTokenProvider.generateToken(user1);
        String token2 = jwtTokenProvider.generateToken(user2);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Test
    void validateToken_ValidToken_ReturnsClaimsSuccessfully() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);

        // Act
        Claims claims = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("username")).isEqualTo("testuser");
    }

    @Test
    void validateToken_InvalidSignature_ThrowsJwtException() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tamperedToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT signature");
    }

    @Test
    void validateToken_ExpiredToken_ThrowsJwtException() {
        // Arrange
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1); // 1ms expiration

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = shortLivedProvider.generateToken(user);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert
        assertThatThrownBy(() -> shortLivedProvider.validateToken(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Expired JWT token");
    }

    @Test
    void validateToken_MalformedToken_ThrowsJwtException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("not-a-valid-jwt-token"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void validateToken_NullToken_ThrowsJwtException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(null))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("JWT claims string is empty");
    }

    @Test
    void validateToken_EmptyToken_ThrowsJwtException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(""))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("JWT claims string is empty");
    }

    // ==================== GET USER ID FROM TOKEN TESTS ====================

    @Test
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        // Arrange
        User user = User.builder()
                .id(123L)
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);

        // Act
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertThat(userId).isEqualTo(123L);
    }

    @Test
    void getUserIdFromToken_InvalidToken_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken("invalid-token"))
                .isInstanceOf(JwtException.class);
    }

    // ==================== TOKEN HASHING TESTS ====================

    @Test
    void hashToken_SameToken_ReturnsSameHash() {
        // Arrange
        String token = "test-jwt-token-12345";

        // Act
        String hash1 = jwtTokenProvider.hashToken(token);
        String hash2 = jwtTokenProvider.hashToken(token);

        // Assert
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 64-character hex string
    }

    @Test
    void hashToken_DifferentTokens_ReturnsDifferentHashes() {
        // Arrange
        String token1 = "test-jwt-token-12345";
        String token2 = "test-jwt-token-67890";

        // Act
        String hash1 = jwtTokenProvider.hashToken(token1);
        String hash2 = jwtTokenProvider.hashToken(token2);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash2).hasSize(64);
    }

    @Test
    void hashToken_RealJWT_ProducesValidHash() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = jwtTokenProvider.generateToken(user);

        // Act
        String hash = jwtTokenProvider.hashToken(token);

        // Assert
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[a-f0-9]{64}$"); // Hex string
    }

    // ==================== GET EXPIRATION MS TESTS ====================

    @Test
    void getExpirationMs_ReturnsConfiguredValue() {
        // Act
        long expirationMs = jwtTokenProvider.getExpirationMs();

        // Assert
        assertThat(expirationMs).isEqualTo(TEST_EXPIRATION_MS);
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    void fullLifecycle_GenerateValidateHashToken_WorksCorrectly() {
        // Arrange
        User user = User.builder()
                .id(42L)
                .username("lifecycletest")
                .email("lifecycle@example.com")
                .build();

        // Act - Generate token
        String token = jwtTokenProvider.generateToken(user);

        // Act - Validate token
        Claims claims = jwtTokenProvider.validateToken(token);

        // Act - Extract user ID
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Act - Hash token
        String hash = jwtTokenProvider.hashToken(token);

        // Assert
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("username")).isEqualTo("lifecycletest");
        assertThat(userId).isEqualTo(42L);
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[a-f0-9]{64}$");
    }
}
