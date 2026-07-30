package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateConditionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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
 * Integration tests for ConditionController.
 * Tests all CRUD endpoints for Condition resources with proper authentication and authorization,
 * and proves all 6 rulebook conditions round-trip via bulk import.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ConditionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private ConditionRepository conditionRepository;

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

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL CONDITIONS TESTS ====================

    @Test
    void getAllConditions_AsAuthenticatedUser_Returns200() throws Exception {
        createCondition("Restrained", testExpansion, true);
        createCondition("Vulnerable", testExpansion, true);

        mockMvc.perform(get("/api/dh/conditions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllConditions_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/conditions"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET CONDITION BY ID TESTS ====================

    @Test
    void getConditionById_ValidId_Returns200() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);

        mockMvc.perform(get("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(condition.getId()))
                .andExpect(jsonPath("$.name").value("Restrained"));
    }

    @Test
    void getConditionById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/conditions/999999")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE CONDITION TESTS ====================

    @Test
    void createCondition_AsAdmin_Returns201AndRoundTripsOnGet() throws Exception {
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Restrained")
                .description("You cannot move or evade.")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        String createResponse = mockMvc.perform(post("/api/dh/conditions")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Restrained"))
                .andExpect(jsonPath("$.isOfficial").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(conditionRepository.findAll()).hasSize(1);

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/dh/conditions/" + createdId)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.name").value("Restrained"));
    }

    @Test
    void createCondition_AsUser_Returns403() throws Exception {
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Restrained")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        mockMvc.perform(post("/api/dh/conditions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(conditionRepository.findAll()).isEmpty();
    }

    // ==================== CREATE CONDITIONS BULK TESTS — all 6 rulebook conditions ====================

    @Test
    void createConditionsBulk_AsAdmin_CreatesAllSixConditions() throws Exception {
        List<CreateConditionRequest> requests = List.of(
                CreateConditionRequest.builder().name("Restrained").expansionId(testExpansion.getId()).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Vulnerable").expansionId(testExpansion.getId()).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Drained").expansionId(testExpansion.getId()).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Hexed").expansionId(testExpansion.getId()).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Chained").expansionId(testExpansion.getId()).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Ignited").expansionId(testExpansion.getId()).isOfficial(true).build());

        mockMvc.perform(post("/api/dh/conditions/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));

        assertThat(conditionRepository.findAll()).hasSize(6);

        // All 6 must be listable
        mockMvc.perform(get("/api/dh/conditions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void createConditionsBulk_AsUser_Returns403() throws Exception {
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Restrained")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        mockMvc.perform(post("/api/dh/conditions/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isForbidden());

        assertThat(conditionRepository.findAll()).isEmpty();
    }

    // ==================== UPDATE CONDITION TESTS ====================

    @Test
    void updateCondition_AsAdmin_Returns200() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);

        UpdateConditionRequest request = UpdateConditionRequest.builder()
                .description("Updated rules text")
                .build();

        mockMvc.perform(put("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated rules text"));
    }

    @Test
    void updateCondition_AsUser_Returns403() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);

        UpdateConditionRequest request = UpdateConditionRequest.builder().description("Updated").build();

        mockMvc.perform(put("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE / RESTORE CONDITION TESTS ====================

    @Test
    void deleteCondition_AsAdmin_Returns204AndSoftDeletes() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);

        mockMvc.perform(delete("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        assertThat(conditionRepository.findByIdAndDeletedAtIsNull(condition.getId())).isEmpty();

        mockMvc.perform(get("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCondition_AsUser_Returns403() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);

        mockMvc.perform(delete("/api/dh/conditions/" + condition.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreCondition_AsAdmin_Returns200AndRestores() throws Exception {
        Condition condition = createCondition("Restrained", testExpansion, true);
        condition.softDelete();
        conditionRepository.save(condition);

        mockMvc.perform(post("/api/dh/conditions/" + condition.getId() + "/restore")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        assertThat(conditionRepository.findByIdAndDeletedAtIsNull(condition.getId())).isPresent();
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

    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    private Condition createCondition(String name, Expansion expansion, boolean isOfficial) {
        Condition condition = Condition.builder()
                .name(name)
                .description("Test rules text for " + name)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .build();
        return conditionRepository.save(condition);
    }
}
