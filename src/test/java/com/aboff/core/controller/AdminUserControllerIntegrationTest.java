package com.aboff.core.controller;

import com.aboff.core.model.dto.request.BanUserRequest;
import com.aboff.core.model.dto.request.UpdateAdminUserRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.AdminActionLogRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.UsernameHistoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the new User Manager admin endpoints:
 * {@code GET /api/admin/users}, {@code GET /api/admin/users/{id}},
 * {@code PATCH /api/admin/users/{id}}, and the ban-with-reason body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AdminUserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ActiveTokenRepository activeTokenRepository;
    @Autowired private UsernameHistoryRepository usernameHistoryRepository;
    @Autowired private AdminActionLogRepository adminActionLogRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User owner;
    private User admin;
    private User moderator;
    private User regular;

    private String ownerToken;
    private String adminToken;
    private String moderatorToken;
    private String regularToken;

    @BeforeEach
    void setUp() {
        owner = createUser("um-owner", "um-owner@example.com", Role.OWNER);
        admin = createUser("um-admin", "um-admin@example.com", Role.ADMIN);
        moderator = createUser("um-mod", "um-mod@example.com", Role.MODERATOR);
        regular = createUser("um-user", "um-user@example.com", Role.USER);

        ownerToken = issueToken(owner);
        adminToken = issueToken(admin);
        moderatorToken = issueToken(moderator);
        regularToken = issueToken(regular);
    }

    // -------- LIST --------

    @Test
    void listUsers_AsAdmin_Returns200WithPagedResult() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageSize").exists());
    }

    @Test
    void listUsers_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .cookie(new Cookie("AUTH_TOKEN", regularToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_SizeOver100IsClamped() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("size", "999")
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    // -------- DETAIL --------

    @Test
    void getUserDetail_WithExpandAll_PopulatesAllCollections() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", regular.getId())
                        .param("expand", "all")
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(regular.getId()))
                .andExpect(jsonPath("$.identities").isArray())
                .andExpect(jsonPath("$.loginEvents").isArray())
                .andExpect(jsonPath("$.usernameHistory").isArray())
                .andExpect(jsonPath("$.adminActions").isArray());
    }

    @Test
    void getUserDetail_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", regularToken)))
                .andExpect(status().isForbidden());
    }

    // -------- PATCH --------

    @Test
    void patchUser_OwnerUpdatesUsername_WritesHistoryAndAudit() throws Exception {
        UpdateAdminUserRequest req = UpdateAdminUserRequest.builder()
                .username("renamed-by-owner")
                .build();

        mockMvc.perform(patch("/api/admin/users/{id}", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("renamed-by-owner"));

        assertThat(usernameHistoryRepository.count()).isGreaterThan(0);
        assertThat(adminActionLogRepository.findByTargetUserIdOrderByCreatedAtDesc(
                regular.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent()).isNotEmpty();
    }

    @Test
    void patchUser_AdminTriesToGrantAdmin_Returns403() throws Exception {
        UpdateAdminUserRequest req = UpdateAdminUserRequest.builder()
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(patch("/api/admin/users/{id}", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        User unchanged = userRepository.findById(regular.getId()).orElseThrow();
        assertThat(unchanged.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void patchUser_AdminPromotesToModerator_RevokesTokens() throws Exception {
        UpdateAdminUserRequest req = UpdateAdminUserRequest.builder()
                .role(Role.MODERATOR)
                .build();

        assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regular.getId())).isNotEmpty();

        mockMvc.perform(patch("/api/admin/users/{id}", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MODERATOR"));

        assertThat(activeTokenRepository.findByUserIdAndRevokedAtIsNull(regular.getId())).isEmpty();
    }

    @Test
    void patchUser_ModeratorCannotModifyModerator() throws Exception {
        UpdateAdminUserRequest req = UpdateAdminUserRequest.builder()
                .avatarUrl("https://example.com/a.png")
                .build();

        User peer = createUser("um-mod2", "um-mod2@example.com", Role.MODERATOR);

        mockMvc.perform(patch("/api/admin/users/{id}", peer.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------- BAN WITH REASON --------

    @Test
    void banUser_WithReason_PopulatesBanReason() throws Exception {
        BanUserRequest req = BanUserRequest.builder().reason("disruptive").build();

        mockMvc.perform(post("/api/admin/users/{id}/ban", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.banReason").value("disruptive"));

        User banned = userRepository.findById(regular.getId()).orElseThrow();
        assertThat(banned.getBanReason()).isEqualTo("disruptive");
        assertThat(banned.isBanned()).isTrue();
    }

    @Test
    void banUser_NoBody_StillBans() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/ban", regular.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk());

        User banned = userRepository.findById(regular.getId()).orElseThrow();
        assertThat(banned.isBanned()).isTrue();
        assertThat(banned.getBanReason()).isNull();
    }

    // -------- HELPERS --------

    private User createUser(String username, String email, Role role) {
        User u = User.builder()
                .username(username)
                .email(email)
                .role(role)
                .build();
        return userRepository.save(u);
    }

    private String issueToken(User user) {
        String token = jwtTokenProvider.generateToken(user);
        ActiveToken active = ActiveToken.builder()
                .userId(user.getId())
                .tokenHash(jwtTokenProvider.hashToken(token))
                .deviceInfo("test")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        activeTokenRepository.save(active);
        return token;
    }
}
