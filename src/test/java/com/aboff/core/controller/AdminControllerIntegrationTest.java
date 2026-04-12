package com.aboff.core.controller;

import com.aboff.core.model.dto.request.ChangeRoleRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AdminControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ActiveTokenRepository activeTokenRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        private User ownerUser;
        private User adminUser;
        private User moderatorUser;
        private User regularUser;

        private String ownerToken;
        private String adminToken;
        private String moderatorToken;
        private String regularToken;

        @BeforeEach
        void setUp() {
                // Create test users with different roles
                ownerUser = createUserWithRole("owner", "owner@example.com", Role.OWNER);
                adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
                moderatorUser = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);
                regularUser = createUserWithRole("user", "user@example.com", Role.USER);

                // Generate JWT tokens for each user
                ownerToken = jwtTokenProvider.generateToken(ownerUser);
                adminToken = jwtTokenProvider.generateToken(adminUser);
                moderatorToken = jwtTokenProvider.generateToken(moderatorUser);
                regularToken = jwtTokenProvider.generateToken(regularUser);

                // Store token hashes in database for validation
                storeTokenInDatabase(ownerUser.getId(), ownerToken);
                storeTokenInDatabase(adminUser.getId(), adminToken);
                storeTokenInDatabase(moderatorUser.getId(), moderatorToken);
                storeTokenInDatabase(regularUser.getId(), regularToken);
        }

        // ==================== BAN USER TESTS ====================

        @Test
        void banUser_AdminBansModerator_Returns200() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", moderatorUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(moderatorUser.getId()))
                                .andExpect(jsonPath("$.username").value("moderator"));

                // Verify user was banned
                User banned = userRepository.findById(moderatorUser.getId()).orElseThrow();
                assertThat(banned.isBanned()).isTrue();
        }

        @Test
        void banUser_OwnerBansAdmin_Returns200() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", adminUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", ownerToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(adminUser.getId()));

                // Verify user was banned
                User banned = userRepository.findById(adminUser.getId()).orElseThrow();
                assertThat(banned.isBanned()).isTrue();
        }

        @Test
        void banUser_ModeratorBansModerator_Returns403() throws Exception {
                // Arrange - Create another moderator
                User moderator2 = createUserWithRole("moderator2", "moderator2@example.com", Role.MODERATOR);

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", moderator2.getId())
                                .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.error").value("Insufficient Permissions"));

                // Verify user was NOT banned
                User notBanned = userRepository.findById(moderator2.getId()).orElseThrow();
                assertThat(notBanned.isBanned()).isFalse();
        }

        @Test
        void banUser_ModeratorBansAdmin_Returns403() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", adminUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.error").value("Insufficient Permissions"));

                // Verify user was NOT banned
                User notBanned = userRepository.findById(adminUser.getId()).orElseThrow();
                assertThat(notBanned.isBanned()).isFalse();
        }

        @Test
        void banUser_UserTriesToBan_Returns403() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", moderatorUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", regularToken)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void banUser_NonexistentUser_Returns404() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/ban", 99999L)
                                .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("User Not Found"));
        }

        @Test
        void banUser_InvalidatesAllUserTokens() throws Exception {
                // Arrange - Verify the user has active (non-revoked) tokens
                assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regularUser.getId())).isNotEmpty();

                // Act - Ban the user
                mockMvc.perform(post("/api/admin/users/{userId}/ban", regularUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                                .andExpect(status().isOk());

                // Assert - Verify all tokens for the user have been revoked (no active tokens
                // remain)
                assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regularUser.getId())).isEmpty();
        }

        // ==================== UNBAN USER TESTS ====================

        @Test
        void unbanUser_AdminUnbansModerator_Returns200() throws Exception {
                // Arrange - Ban the user first
                moderatorUser.ban();
                userRepository.save(moderatorUser);

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/unban", moderatorUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(moderatorUser.getId()));

                // Verify user was unbanned
                User unbanned = userRepository.findById(moderatorUser.getId()).orElseThrow();
                assertThat(unbanned.isBanned()).isFalse();
        }

        @Test
        void unbanUser_OwnerUnbansAdmin_Returns200() throws Exception {
                // Arrange - Ban the user first
                adminUser.ban();
                userRepository.save(adminUser);

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/unban", adminUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", ownerToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(adminUser.getId()));

                // Verify user was unbanned
                User unbanned = userRepository.findById(adminUser.getId()).orElseThrow();
                assertThat(unbanned.isBanned()).isFalse();
        }

        @Test
        void unbanUser_ModeratorUnbansAdmin_Returns403() throws Exception {
                // Arrange - Ban the user first
                adminUser.ban();
                userRepository.save(adminUser);

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/unban", adminUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.error").value("Insufficient Permissions"));
        }

        @Test
        void unbanUser_UserTriesToUnban_Returns403() throws Exception {
                // Arrange - Ban the user first (although user can't usually ban, we set state
                // manually)
                User anotherUser = createUserWithRole("another", "another@example.com", Role.USER);
                anotherUser.ban();
                userRepository.save(anotherUser);

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/unban", anotherUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", regularToken)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void unbanUser_NonexistentUser_Returns404() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/unban", 99999L)
                                .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("User Not Found"));
        }

        // ==================== CHANGE ROLE TESTS ====================

        @Test
        void changeRole_AsOwner_Returns200() throws Exception {
                // Arrange
                ChangeRoleRequest request = ChangeRoleRequest.builder()
                                .newRole(Role.ADMIN)
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/change-role", regularUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(regularUser.getId()));

                // Verify role was changed
                User updated = userRepository.findById(regularUser.getId()).orElseThrow();
                assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        void changeRole_AsAdmin_Returns403() throws Exception {
                // Arrange
                ChangeRoleRequest request = ChangeRoleRequest.builder()
                                .newRole(Role.ADMIN)
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/change-role", regularUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());

                // Verify role was NOT changed
                User unchanged = userRepository.findById(regularUser.getId()).orElseThrow();
                assertThat(unchanged.getRole()).isEqualTo(Role.USER);
        }

        @Test
        void changeRole_AsModerator_Returns403() throws Exception {
                // Arrange
                ChangeRoleRequest request = ChangeRoleRequest.builder()
                                .newRole(Role.ADMIN)
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/change-role", regularUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void changeRole_InvalidatesAllUserTokens() throws Exception {
                // Arrange
                ChangeRoleRequest request = ChangeRoleRequest.builder()
                                .newRole(Role.ADMIN)
                                .build();
                // Verify the user has active (non-revoked) tokens
                assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regularUser.getId())).isNotEmpty();

                // Act - Change the user's role
                mockMvc.perform(post("/api/admin/users/{userId}/change-role", regularUser.getId())
                                .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Assert - Verify all tokens for the user have been revoked (no active tokens
                // remain)
                assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regularUser.getId())).isEmpty();
        }

        @Test
        void changeRole_NonexistentUser_Returns404() throws Exception {
                // Arrange
                ChangeRoleRequest request = ChangeRoleRequest.builder()
                                .newRole(Role.ADMIN)
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/admin/users/{userId}/change-role", 99999L)
                                .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("User Not Found"));
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
