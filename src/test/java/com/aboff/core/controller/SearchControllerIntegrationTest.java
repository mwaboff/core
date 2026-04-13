package com.aboff.core.controller;

import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link SearchController}.
 *
 * <p>Verifies authentication boundaries and request parameter validation for the
 * {@code GET /api/search} endpoint.
 *
 * <p>Note: These tests operate against H2 in PostgreSQL-compatibility mode. Tests that
 * would exercise the PostgreSQL-specific full-text search query (plainto_tsquery, ts_rank)
 * are not included here, as H2 does not support those functions. The full FTS query
 * path is covered by the {@link com.aboff.core.service.SearchServiceTest} unit tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String userToken;

    @BeforeEach
    void setUp() {
        User regularUser = createUserWithRole("searchUser", "searchuser@example.com", Role.USER);
        userToken = jwtTokenProvider.generateToken(regularUser);
        storeTokenInDatabase(regularUser.getId(), userToken);
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    void search_WithoutAuthentication_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/search").param("q", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_WithMissingQueryParam_ReturnsNon2xx() throws Exception {
        // Act & Assert — Spring cannot bind the required 'q' parameter, resulting in an error
        // The GlobalExceptionHandler catch-all maps unhandled exceptions to 500.
        mockMvc.perform(get("/api/search")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void search_WithEmptyQueryParam_ReturnsServerError() throws Exception {
        // Arrange — valid auth but blank query triggers IllegalArgumentException in the service
        // which is caught by the GlobalExceptionHandler catch-all (500).
        mockMvc.perform(get("/api/search")
                        .param("q", "")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void search_WithExpiredOrInvalidToken_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/search")
                        .param("q", "test")
                        .cookie(new Cookie("AUTH_TOKEN", "invalid.token.value")))
                .andExpect(status().isUnauthorized());
    }

    // ==================== HELPER METHODS ====================

    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private void storeTokenInDatabase(Long userId, String token) {
        String tokenHash = jwtTokenProvider.hashToken(token);
        ActiveToken activeToken = ActiveToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo("Test Device")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        activeTokenRepository.save(activeToken);
    }
}
