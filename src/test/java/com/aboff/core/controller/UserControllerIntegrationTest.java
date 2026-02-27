package com.aboff.core.controller;

import com.aboff.core.model.dto.request.ChangePasswordRequest;
import com.aboff.core.model.dto.request.UpdateUserRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ActiveTokenRepository activeTokenRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        private User testUser;
        private String authToken;
        private Cookie authCookie;

        @BeforeEach
        void setUp() {
                // Create test user
                testUser = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();
                testUser = userRepository.save(testUser);

                // Generate auth token
                authToken = jwtTokenProvider.generateToken(testUser);
                String tokenHash = jwtTokenProvider.hashToken(authToken);

                // Store token in database
                ActiveToken activeToken = ActiveToken.builder()
                                .userId(testUser.getId())
                                .tokenHash(tokenHash)
                                .issuedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusDays(30))
                                .build();
                activeTokenRepository.save(activeToken);

                // Create auth cookie
                authCookie = new Cookie("AUTH_TOKEN", authToken);
        }

        // ==================== GET CURRENT USER TESTS ====================

        @Test
        void getCurrentUser_Me_ReturnsFullInfo() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/users/me")
                                .cookie(authCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url"))
                                .andExpect(jsonPath("$.timezone").value("UTC"))
                                .andExpect(jsonPath("$.createdAt").exists())
                                .andExpect(jsonPath("$.lastModifiedAt").exists())
                                // Admin fields should be absent for regular users
                                .andExpect(jsonPath("$.accountLockedUntil").doesNotExist())
                                .andExpect(jsonPath("$.failedLoginAttempts").doesNotExist())
                                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                                .andExpect(jsonPath("$.bannedAt").doesNotExist());
        }

        @Test
        void getUser_OwnId_ReturnsFullInfo() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/users/" + testUser.getId())
                                .cookie(authCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url"))
                                .andExpect(jsonPath("$.timezone").value("UTC"));
        }

        @Test
        void getUser_OtherId_ReturnsLimitedInfo() throws Exception {
                // Arrange - Create another user
                User otherUser = User.builder()
                                .username("otheruser")
                                .email("other@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .avatarUrl("https://other.avatar.url")
                                .timezone("Europe/London")
                                .build();
                otherUser = userRepository.save(otherUser);

                // Act & Assert
                mockMvc.perform(get("/api/users/" + otherUser.getId())
                                .cookie(authCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("otheruser"))
                                .andExpect(jsonPath("$.avatarUrl").value("https://other.avatar.url"))
                                .andExpect(jsonPath("$.createdAt").exists())
                                // Restricted fields should be missing
                                .andExpect(jsonPath("$.email").doesNotExist())
                                .andExpect(jsonPath("$.timezone").doesNotExist())
                                .andExpect(jsonPath("$.lastModifiedAt").doesNotExist())
                                // Admin fields should be absent for regular users
                                .andExpect(jsonPath("$.accountLockedUntil").doesNotExist())
                                .andExpect(jsonPath("$.failedLoginAttempts").doesNotExist())
                                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                                .andExpect(jsonPath("$.bannedAt").doesNotExist());
        }

        @Test
        void getUser_NonExistent_Returns404() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/users/999999")
                                .cookie(authCookie))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getUser_InvalidId_Returns404() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/users/not-a-number")
                                .cookie(authCookie))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getUser_ModeratorFetchingOther_ReturnsFullInfo() throws Exception {
                // Arrange - Create a moderator and a target user
                User moderator = createPrivilegedUser("moderator", "mod@example.com", Role.MODERATOR);
                String modToken = jwtTokenProvider.generateToken(moderator);
                Cookie modCookie = new Cookie("AUTH_TOKEN", modToken);

                User targetUser = User.builder()
                                .username("targetuser")
                                .email("target@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .avatarUrl("https://target.avatar.url")
                                .timezone("America/Chicago")
                                .build();
                targetUser = userRepository.save(targetUser);

                // Act & Assert
                mockMvc.perform(get("/api/users/" + targetUser.getId())
                                .cookie(modCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("targetuser"))
                                .andExpect(jsonPath("$.email").value("target@example.com"))
                                .andExpect(jsonPath("$.avatarUrl").value("https://target.avatar.url"))
                                .andExpect(jsonPath("$.timezone").value("America/Chicago"))
                                .andExpect(jsonPath("$.lastModifiedAt").exists())
                                // Admin fields should be present for moderators
                                .andExpect(jsonPath("$.accountLockedUntil").doesNotExist())
                                .andExpect(jsonPath("$.failedLoginAttempts").value(0))
                                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                                .andExpect(jsonPath("$.bannedAt").doesNotExist());
        }

        @Test
        void getUser_AdminFetchingOther_ReturnsAdminInfo() throws Exception {
                // Arrange - Create an admin and a target user with some admin data
                User admin = createPrivilegedUser("admin", "admin@example.com", Role.ADMIN);
                String adminToken = jwtTokenProvider.generateToken(admin);
                Cookie adminCookie = new Cookie("AUTH_TOKEN", adminToken);

                LocalDateTime lockUntil = LocalDateTime.now().plusHours(1);
                User targetUser = User.builder()
                                .username("lockeduser")
                                .email("locked@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .accountLockedUntil(lockUntil)
                                .failedLoginAttempts(5)
                                .build();
                targetUser = userRepository.save(targetUser);

                // Act & Assert
                mockMvc.perform(get("/api/users/" + targetUser.getId())
                                .cookie(adminCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("lockeduser"))
                                .andExpect(jsonPath("$.accountLockedUntil").exists())
                                .andExpect(jsonPath("$.failedLoginAttempts").value(5))
                                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                                .andExpect(jsonPath("$.bannedAt").doesNotExist());
        }

        private User createPrivilegedUser(String username, String email, Role role) {
                User user = User.builder()
                                .username(username)
                                .email(email)
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .role(role)
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

                return user;
        }

        @Test
        void getCurrentUser_NoToken_Returns401() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void getUser_BannedUser_Returns401() throws Exception {
                // Arrange - Create a user and ban them
                User user = User.builder()
                                .username("banneduser")
                                .email("banned@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .bannedAt(LocalDateTime.now())
                                .build();
                user = userRepository.save(user);

                String token = jwtTokenProvider.generateToken(user);
                Cookie authCookie = new Cookie("AUTH_TOKEN", token);

                // Act & Assert
                mockMvc.perform(get("/api/users/me")
                                .cookie(authCookie))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void getCurrentUser_RevokedToken_Returns401() throws Exception {
                // Arrange - Revoke the token
                String tokenHash = jwtTokenProvider.hashToken(authToken);
                ActiveToken token = activeTokenRepository.findByTokenHash(tokenHash).orElseThrow();
                token.revoke();
                activeTokenRepository.save(token);

                // Act & Assert
                mockMvc.perform(get("/api/users/me")
                                .cookie(authCookie))
                                .andExpect(status().isUnauthorized());
        }

        // ==================== UPDATE USER TESTS ====================

        @Test
        void updateCurrentUser_ValidData_Returns200() throws Exception {
                // Arrange
                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("newemail@example.com")
                                .avatarUrl("https://new.avatar.url")
                                .timezone("America/New_York")
                                .build();

                // Act & Assert
                mockMvc.perform(patch("/api/users/me")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("newemail@example.com"))
                                .andExpect(jsonPath("$.avatarUrl").value("https://new.avatar.url"))
                                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                                .andExpect(jsonPath("$.username").value("testuser")); // Username unchanged

                // Verify in database
                User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
                assertThat(updatedUser.getAvatarUrl()).isEqualTo("https://new.avatar.url");
                assertThat(updatedUser.getTimezone()).isEqualTo("America/New_York");
        }

        @Test
        void updateCurrentUser_PartialUpdate_Returns200() throws Exception {
                // Arrange - Only update timezone
                UpdateUserRequest request = UpdateUserRequest.builder()
                                .timezone("America/Los_Angeles")
                                .build();

                // Act & Assert
                mockMvc.perform(patch("/api/users/me")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.timezone").value("America/Los_Angeles"))
                                .andExpect(jsonPath("$.email").value("test@example.com")) // Email unchanged
                                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url")); // Avatar unchanged
        }

        @Test
        void updateCurrentUser_DuplicateEmail_Returns409() throws Exception {
                // Arrange - Create another user with the email we want to use
                User otherUser = User.builder()
                                .username("otheruser")
                                .email("taken@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .build();
                userRepository.save(otherUser);

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("taken@example.com")
                                .build();

                // Act & Assert
                mockMvc.perform(patch("/api/users/me")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Email already registered"));
        }

        @Test
        void updateCurrentUser_NoToken_Returns401() throws Exception {
                // Arrange
                UpdateUserRequest request = UpdateUserRequest.builder()
                                .email("newemail@example.com")
                                .build();

                // Act & Assert
                mockMvc.perform(patch("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        // ==================== CHANGE PASSWORD TESTS ====================

        @Test
        void changePassword_ValidRequest_Returns204AndInvalidatesAllTokens() throws Exception {
                // Arrange
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("Password123!")
                                .newPassword("NewPassword456!")
                                .build();

                // Verify we have one active token before password change
                List<ActiveToken> tokensBefore = activeTokenRepository.findByUserIdAndRevokedAtIsNull(testUser.getId());
                assertThat(tokensBefore).hasSize(1);

                // Act
                mockMvc.perform(post("/api/users/me/change-password")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("AUTH_TOKEN", 0)); // Cookie should be cleared

                // Verify password was changed
                User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(passwordEncoder.matches("NewPassword456!", updatedUser.getPasswordHash())).isTrue();

                // Verify ALL tokens were revoked (including the one from setUp)
                List<ActiveToken> allTokens = activeTokenRepository.findByUserIdAndRevokedAtIsNull(testUser.getId());
                assertThat(allTokens).isEmpty(); // All should be revoked
        }

        @Test
        void changePassword_InvalidCurrentPassword_Returns400() throws Exception {
                // Arrange
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("WrongPassword!")
                                .newPassword("NewPassword456!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/users/me/change-password")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

                // Verify password was NOT changed
                User unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(passwordEncoder.matches("Password123!", unchangedUser.getPasswordHash())).isTrue();
        }

        @Test
        void changePassword_WeakNewPassword_Returns400() throws Exception {
                // Arrange
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                                .currentPassword("Password123!")
                                .newPassword("weak")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/users/me/change-password")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ==================== DELETE USER TESTS ====================

        @Test
        void deleteCurrentUser_Success_Returns204AndSoftDeletes() throws Exception {
                // Act
                mockMvc.perform(delete("/api/users/me")

                                .cookie(authCookie))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("AUTH_TOKEN", 0)); // Cookie should be cleared

                // Verify user was soft-deleted
                User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(deletedUser.getDeletedAt()).isNotNull();
                assertThat(deletedUser.isDeleted()).isTrue();

                // Verify tokens were revoked
                List<ActiveToken> allTokens = activeTokenRepository.findByUserIdAndRevokedAtIsNull(testUser.getId());
                assertThat(allTokens).isEmpty();
        }

        @Test
        void deleteCurrentUser_NoToken_Returns401() throws Exception {
                // Act & Assert
                mockMvc.perform(delete("/api/users/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteCurrentUser_AfterDeletion_CannotLogin() throws Exception {
                // Arrange - Delete the user
                mockMvc.perform(delete("/api/users/me")

                                .cookie(authCookie))
                                .andExpect(status().isNoContent());

                // Act - Try to access with same token
                mockMvc.perform(get("/api/users/me")
                                .cookie(authCookie))
                                .andExpect(status().isUnauthorized());
        }
}
