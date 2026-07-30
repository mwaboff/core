package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateTransformationCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for TransformationCardController.
 * Tests all CRUD endpoints for TransformationCard resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class TransformationCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private TransformationCardRepository transformationCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;

    @BeforeEach
    void setUp() {
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Hope & Fear", true);
    }

    // ==================== GET ALL TESTS ====================

    @Test
    void getAllTransformationCards_AsAuthenticatedUser_Returns200() throws Exception {
        createTransformationCard("Feral Transformation", "Becomes a beast", testExpansion);
        createTransformationCard("Elemental Transformation", "Becomes an element", testExpansion);

        mockMvc.perform(get("/api/dh/transformation-cards")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllTransformationCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/transformation-cards"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void getTransformationCardById_Existing_ReturnsCard() throws Exception {
        TransformationCard card = createTransformationCard("Feral Transformation", "Becomes a beast", testExpansion);

        mockMvc.perform(get("/api/dh/transformation-cards/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Feral Transformation"));
    }

    @Test
    void getTransformationCardById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/transformation-cards/{id}", 999999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createTransformationCard_AsAdmin_Returns201() throws Exception {
        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansionId(testExpansion.getId())
                .build();

        mockMvc.perform(post("/api/dh/transformation-cards")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Feral Transformation"));

        assertThat(transformationCardRepository.findAll()).hasSize(1);
    }

    @Test
    void createTransformationCard_AsUser_Returns403() throws Exception {
        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansionId(testExpansion.getId())
                .build();

        mockMvc.perform(post("/api/dh/transformation-cards")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(transformationCardRepository.findAll()).isEmpty();
    }

    @Test
    void createTransformationCard_MissingName_Returns400() throws Exception {
        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .description("Becomes a beast")
                .expansionId(testExpansion.getId())
                .build();

        mockMvc.perform(post("/api/dh/transformation-cards")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== CREATE BULK TESTS ====================

    @Test
    void createTransformationCardsBulk_AsAdmin_Returns201() throws Exception {
        CreateTransformationCardRequest request1 = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansionId(testExpansion.getId())
                .build();
        CreateTransformationCardRequest request2 = CreateTransformationCardRequest.builder()
                .name("Elemental Transformation")
                .description("Becomes an element")
                .expansionId(testExpansion.getId())
                .build();
        List<CreateTransformationCardRequest> requests = List.of(request1, request2);

        mockMvc.perform(post("/api/dh/transformation-cards/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(transformationCardRepository.findAll()).hasSize(2);
    }

    @Test
    void createTransformationCardsBulk_AsUser_Returns403() throws Exception {
        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansionId(testExpansion.getId())
                .build();

        mockMvc.perform(post("/api/dh/transformation-cards/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateTransformationCard_AsAdmin_Returns200() throws Exception {
        TransformationCard card = createTransformationCard("Feral Transformation", "Original", testExpansion);
        UpdateTransformationCardRequest request = UpdateTransformationCardRequest.builder()
                .name("Feral Transformation (Revised)")
                .description("Updated description")
                .build();

        mockMvc.perform(put("/api/dh/transformation-cards/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Feral Transformation (Revised)"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void updateTransformationCard_AsUser_Returns403() throws Exception {
        TransformationCard card = createTransformationCard("Feral Transformation", "Original", testExpansion);
        UpdateTransformationCardRequest request = UpdateTransformationCardRequest.builder()
                .name("Should Not Apply")
                .build();

        mockMvc.perform(put("/api/dh/transformation-cards/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE / RESTORE TESTS ====================

    @Test
    void deleteTransformationCard_AsAdmin_Returns204AndSoftDeletes() throws Exception {
        TransformationCard card = createTransformationCard("Feral Transformation", "To delete", testExpansion);

        mockMvc.perform(delete("/api/dh/transformation-cards/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        TransformationCard deleted = transformationCardRepository.findById(card.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void restoreTransformationCard_AsAdmin_Returns200AndClearsDeletedAt() throws Exception {
        TransformationCard card = createTransformationCard("Feral Transformation", "To restore", testExpansion);
        card.softDelete();
        transformationCardRepository.save(card);

        mockMvc.perform(post("/api/dh/transformation-cards/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        TransformationCard restored = transformationCardRepository.findById(card.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    // Note: search findability is deliberately NOT asserted here via a real GET /api/search call.
    // H2 (this test's database, per application-test.properties) does not support the
    // PostgreSQL-specific full-text search functions (plainto_tsquery, ts_rank) the real query
    // uses — see SearchControllerIntegrationTest's class javadoc, which documents the same
    // constraint for every other indexed entity. SearchFieldMappingTest below unit-tests the
    // buildForTransformationCard mapping in isolation; the actual end-to-end "findable via
    // /api/search" proof is done against a real Postgres boot (see packet verification).

    // ==================== HELPERS ====================

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

    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    private TransformationCard createTransformationCard(String name, String description, Expansion expansion) {
        TransformationCard card = TransformationCard.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .build();
        return transformationCardRepository.save(card);
    }
}
