package com.aboff.core.controller;

import com.aboff.core.model.dto.request.LoginRequest;
import com.aboff.core.model.dto.request.RegisterRequest;
import com.aboff.core.model.dto.response.UserResponse;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.LoginAttempt;
import com.aboff.core.model.entity.User;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.LoginAttemptRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthControllerIntegrationTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext context;

        private ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ActiveTokenRepository activeTokenRepository;

        @Autowired
        private LoginAttemptRepository loginAttemptRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @BeforeEach
        void setUp() {
                // Configure MockMvc with Spring Security
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                // Clean up database before each test
                activeTokenRepository.deleteAll();
                loginAttemptRepository.deleteAll();
                userRepository.deleteAll();
        }

        // ==================== REGISTER TESTS ====================

        @Test
        void register_ValidRequest_Returns201AndCreatesUser() throws Exception {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("Password123!")
                                .build();

                // Act & Assert
                MvcResult result = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.passwordHash").doesNotExist()) // Should not expose password hash
                                .andReturn();

                // Verify user was created in database
                User user = userRepository.findByUsernameIgnoreCase("testuser").orElseThrow();
                assertThat(user.getEmail()).isEqualTo("test@example.com");
                assertThat(user.getPasswordHash()).isNotNull();
                assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
        }

        @Test
        void register_DuplicateUsername_Returns409() throws Exception {
                // Arrange - Create existing user
                User existingUser = User.builder()
                                .username("testuser")
                                .email("existing@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .build();
                userRepository.save(existingUser);

                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser") // Same username
                                .email("new@example.com")
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Username already taken"));
        }

        @Test
        void register_DuplicateEmail_Returns409() throws Exception {
                // Arrange - Create existing user
                User existingUser = User.builder()
                                .username("existing")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .build();
                userRepository.save(existingUser);

                RegisterRequest request = RegisterRequest.builder()
                                .username("newuser")
                                .email("test@example.com") // Same email
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Email already registered"));
        }

        @Test
        void register_InvalidPassword_Returns400() throws Exception {
                // Arrange
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password("weak") // Too short, missing requirements
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isBadRequest());
        }

        // ==================== LOGIN TESTS ====================

        @Test
        void login_ValidCredentials_Returns200AndSetsCookie() throws Exception {
                // Arrange - Create user
                User user = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();
                user = userRepository.save(user);

                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("Password123!")
                                .build();

                // Act
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(cookie().exists("AUTH_TOKEN"))
                                .andExpect(cookie().httpOnly("AUTH_TOKEN", true))
                                .andReturn();

                // Verify cookie properties
                Cookie authCookie = result.getResponse().getCookie("AUTH_TOKEN");
                assertThat(authCookie).isNotNull();
                assertThat(authCookie.getValue()).isNotEmpty();
                assertThat(authCookie.getPath()).isEqualTo("/");

                // Verify token was stored in database
                String tokenHash = jwtTokenProvider.hashToken(authCookie.getValue());
                ActiveToken activeToken = activeTokenRepository.findByTokenHash(tokenHash).orElseThrow();
                assertThat(activeToken.getUserId()).isEqualTo(user.getId());
                assertThat(activeToken.getRevokedAt()).isNull();

                // Verify login attempt was recorded
                List<LoginAttempt> attempts = loginAttemptRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
                assertThat(attempts).hasSize(1);
                assertThat(attempts.get(0).getSuccess()).isTrue();
        }

        @Test
        void login_LoginWithEmail_Returns200() throws Exception {
                // Arrange - Create user
                User user = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .avatarUrl("https://avatar.url")
                                .timezone("UTC")
                                .build();
                userRepository.save(user);

                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("test@example.com") // Use email instead of username
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(cookie().exists("AUTH_TOKEN"));
        }

        @Test
        void login_InvalidPassword_Returns401AndRecordsAttempt() throws Exception {
                // Arrange - Create user
                User user = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .build();
                user = userRepository.save(user);

                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("testuser")
                                .password("WrongPassword!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid username or password"));

                // Verify failed login attempt was recorded
                List<LoginAttempt> attempts = loginAttemptRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
                assertThat(attempts).hasSize(1);
                assertThat(attempts.get(0).getSuccess()).isFalse();
                assertThat(attempts.get(0).getFailureReason()).isEqualTo("INVALID_CREDENTIALS");

                // Verify user's failed attempts counter was incremented
                User updatedUser = userRepository.findById(user.getId()).orElseThrow();
                assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        void login_UserNotFound_Returns401() throws Exception {
                // Arrange
                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("nonexistent")
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        void login_AccountLocked_Returns403() throws Exception {
                // Arrange - Create locked user
                User user = User.builder()
                                .username("lockeduser")
                                .email("locked@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .accountLockedUntil(LocalDateTime.now().plusMinutes(30))
                                .build();
                userRepository.save(user);

                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("lockeduser")
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                                                "Account is temporarily locked due to multiple failed login attempts")));
        }

        @Test
        void login_SoftDeletedUser_Returns401() throws Exception {
                // Arrange - Create soft-deleted user
                User user = User.builder()
                                .username("deleteduser")
                                .email("deleted@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
                                .deletedAt(LocalDateTime.now())
                                .build();
                userRepository.save(user);

                LoginRequest request = LoginRequest.builder()
                                .usernameOrEmail("deleteduser")
                                .password("Password123!")
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                )
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        // ==================== LOGOUT TESTS ====================

        @Test
        void logout_ValidToken_Returns204AndRevokesToken() throws Exception {
                // Arrange - Create user and token
                User user = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .passwordHash(passwordEncoder.encode("Password123!"))
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
                                .cookie(new Cookie("AUTH_TOKEN", token))
                                )
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("AUTH_TOKEN", 0)); // Cookie should be cleared

                // Verify token was revoked in database
                ActiveToken revokedToken = activeTokenRepository.findByTokenHash(tokenHash).orElseThrow();
                assertThat(revokedToken.getRevokedAt()).isNotNull();
        }

        @Test
        void logout_NoToken_Returns401() throws Exception {
                // Act & Assert - Should return 401 when not authenticated
                mockMvc.perform(post("/api/auth/logout")
                                )
                                .andExpect(status().isUnauthorized());
        }
}
