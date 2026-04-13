package com.aboff.core.controller;

import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link AuthController}.
 * <p>
 * Register and credential-based login have been removed in the OAuth migration.
 * This class tests the logout endpoint, which remains provider-agnostic.
 * Full OAuth flow tests will be added in Phase 3.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ActiveTokenRepository activeTokenRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        // ==================== LOGOUT TESTS ====================

        @Test
        void logout_ValidToken_Returns204AndRevokesToken() throws Exception {
                // Arrange - Create user and token
                User user = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                String tokenHash = jwtTokenProvider.hashToken(token);

                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(tokenHash)
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act
                mockMvc.perform(post("/api/auth/logout")
                                .cookie(new Cookie("AUTH_TOKEN", token)))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("AUTH_TOKEN", 0));

                // Verify token was revoked in database
                ActiveToken revokedToken = activeTokenRepository.findByTokenHash(tokenHash).orElseThrow();
                assertThat(revokedToken.getRevokedAt()).isNotNull();
        }

        @Test
        void logout_NoToken_Returns204() throws Exception {
                // Act & Assert - logout is permitAll and performs a no-op when no token is present
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isNoContent());
        }

        // ==================== ME TESTS ====================

        @Test
        void me_Authenticated_Returns200WithUserProfile() throws Exception {
                // Arrange - Create user and valid token
                User user = User.builder()
                                .username("meuser")
                                .email("me@example.com")
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert
                mockMvc.perform(get("/api/auth/me")
                                .cookie(new Cookie("AUTH_TOKEN", token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("meuser"))
                                .andExpect(jsonPath("$.email").value("me@example.com"))
                                .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        void me_Unauthenticated_Returns401() throws Exception {
                // Act & Assert - No cookie provided
                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isUnauthorized());
        }

        // ==================== CHOOSE-USERNAME TESTS ====================

        @Test
        void chooseUsername_ValidUsername_Returns200AndSetsUsername() throws Exception {
                // Arrange - first-time user with temp username
                User user = User.builder()
                                .username("user-tempname")
                                .email("chooseuser@example.com")
                                .usernameChosen(false)
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert
                mockMvc.perform(post("/api/auth/choose-username")
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"mynewusername\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("mynewusername"))
                                .andExpect(jsonPath("$.usernameChosen").value(true));

                // Verify changes persisted in DB
                User updated = userRepository.findById(user.getId()).orElseThrow();
                assertThat(updated.getUsername()).isEqualTo("mynewusername");
                assertThat(updated.getUsernameChosen()).isTrue();
        }

        @Test
        void chooseUsername_AlreadyChosen_Returns400() throws Exception {
                // Arrange - user who has already completed the username selection flow
                User user = User.builder()
                                .username("alreadychosen")
                                .email("alreadychosen@example.com")
                                .usernameChosen(true)
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert - IllegalStateException maps to 400
                mockMvc.perform(post("/api/auth/choose-username")
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"newname\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void chooseUsername_DuplicateUsername_Returns409() throws Exception {
                // Arrange - another user already owns the desired username
                User existingUser = User.builder()
                                .username("takenname")
                                .email("existing@example.com")
                                .usernameChosen(true)
                                .build();
                userRepository.save(existingUser);

                // New user who hasn't chosen a username yet
                User newUser = User.builder()
                                .username("user-newtemp")
                                .email("newuser@example.com")
                                .usernameChosen(false)
                                .build();
                newUser = userRepository.save(newUser);

                String token = jwtTokenProvider.generateToken(newUser);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(newUser.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert - username collision returns 409
                mockMvc.perform(post("/api/auth/choose-username")
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"takenname\"}"))
                                .andExpect(status().isConflict());
        }

        @Test
        void chooseUsername_TooShort_Returns400() throws Exception {
                // Arrange
                User user = User.builder()
                                .username("user-shorttest")
                                .email("shorttest@example.com")
                                .usernameChosen(false)
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert - "ab" is 2 chars, minimum is 3
                mockMvc.perform(post("/api/auth/choose-username")
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"ab\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void chooseUsername_InvalidChars_Returns400() throws Exception {
                // Arrange
                User user = User.builder()
                                .username("user-invalidtest")
                                .email("invalidtest@example.com")
                                .usernameChosen(false)
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(user.getId())
                                .tokenHash(jwtTokenProvider.hashToken(token))
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Act & Assert - spaces and special characters are not allowed
                mockMvc.perform(post("/api/auth/choose-username")
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"my name!\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void chooseUsername_Unauthenticated_Returns401() throws Exception {
                // Act & Assert - no AUTH_TOKEN cookie → 401
                mockMvc.perform(post("/api/auth/choose-username")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\": \"anyname\"}"))
                                .andExpect(status().isUnauthorized());
        }
}
