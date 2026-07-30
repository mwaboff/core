package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateEnvironmentRequest;
import com.aboff.core.model.dto.dh.request.UpdateEnvironmentRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.EnvironmentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for EnvironmentController.
 * Tests all CRUD + bulk endpoints for Environment resources with proper authentication
 * and authorization, and the difficulty/difficultySpecial mutual-exclusivity rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EnvironmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User ownerUser;
    private User adminUser;
    private User moderatorUser;
    private User regularUser;
    private String ownerToken;
    private String adminToken;
    private String moderatorToken;
    private String userToken;
    private Expansion testExpansion;

    @BeforeEach
    void setUp() {
        ownerUser = createUserWithRole("owner", "owner@example.com", Role.OWNER);
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        moderatorUser = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        ownerToken = jwtTokenProvider.generateToken(ownerUser);
        adminToken = jwtTokenProvider.generateToken(adminUser);
        moderatorToken = jwtTokenProvider.generateToken(moderatorUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(ownerUser.getId(), ownerToken);
        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(moderatorUser.getId(), moderatorToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL ENVIRONMENTS TESTS ====================

    @Test
    void getAllEnvironments_AsAuthenticatedUser_Returns200() throws Exception {
        createEnvironment("Abandoned Grove", testExpansion, true, true, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);
        createEnvironment("Bustling Marketplace", testExpansion, true, true, regularUser, EnvironmentType.SOCIAL, 1, 10, null);

        mockMvc.perform(get("/api/dh/environments")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllEnvironments_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/environments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllEnvironments_FilterByType_ReturnsFiltered() throws Exception {
        createEnvironment("Abandoned Grove", testExpansion, true, true, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);
        createEnvironment("Ambushed", testExpansion, true, true, regularUser, EnvironmentType.EVENT, 1, null, "Special (see \"Relative Strength\")");

        mockMvc.perform(get("/api/dh/environments")
                        .param("environmentType", "EVENT")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Ambushed"));
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void getEnvironmentById_Existing_Returns200() throws Exception {
        Environment environment = createEnvironment("Abandoned Grove", testExpansion, true, true, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);

        mockMvc.perform(get("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Abandoned Grove"))
                .andExpect(jsonPath("$.difficulty").value(11));
    }

    @Test
    void getEnvironmentById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/environments/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEnvironmentById_SpecialDifficulty_RoundTripsWithoutSchemaFailure() throws Exception {
        // The "Difficulty: Special" round-trip is the acceptance-critical case for this
        // packet: a create followed by a get, with no numeric difficulty at all.
        Environment environment = createEnvironment("Ambushed", testExpansion, true, true, regularUser,
                EnvironmentType.EVENT, 1, null, "Special (see \"Relative Strength\")");

        mockMvc.perform(get("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ambushed"))
                .andExpect(jsonPath("$.difficultySpecial").value("Special (see \"Relative Strength\")"))
                .andExpect(jsonPath("$.difficulty").doesNotExist());
    }

    // ==================== CREATE ENVIRONMENT TESTS ====================

    @Test
    void createEnvironment_WithNumericDifficulty_Returns201() throws Exception {
        CreateEnvironmentRequest request = CreateEnvironmentRequest.builder()
                .name("Abandoned Grove")
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .description("A former druidic grove.")
                .impulses("Draw in the curious, echo the past")
                .difficulty(11)
                .potentialAdversaries("Beasts (Bear, Dire Wolf)")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .isPublic(true)
                .build();

        mockMvc.perform(post("/api/dh/environments")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Abandoned Grove"))
                .andExpect(jsonPath("$.environmentType").value("EXPLORATION"))
                .andExpect(jsonPath("$.difficulty").value(11));

        assertThat(environmentRepository.findAll()).hasSize(1);
    }

    @Test
    void createEnvironment_WithSpecialDifficulty_Returns201() throws Exception {
        // Acceptance-critical case: a create with no numeric difficulty at all must not
        // fail as a schema violation -- it's the whole point of this packet.
        CreateEnvironmentRequest request = CreateEnvironmentRequest.builder()
                .name("Ambushed")
                .tier(1)
                .environmentType(EnvironmentType.EVENT)
                .description("An ambush is set to catch an unsuspecting party off-guard.")
                .impulses("Overwhelm, scatter, surround")
                .difficultySpecial("Special (see \"Relative Strength\")")
                .potentialAdversaries("Any")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .isPublic(true)
                .build();

        mockMvc.perform(post("/api/dh/environments")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ambushed"))
                .andExpect(jsonPath("$.difficultySpecial").value("Special (see \"Relative Strength\")"))
                .andExpect(jsonPath("$.difficulty").doesNotExist());

        Environment saved = environmentRepository.findAll().get(0);
        assertThat(saved.getDifficulty()).isNull();
        assertThat(saved.getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
    }

    @Test
    void createEnvironment_BothDifficultyFieldsProvided_Returns500() throws Exception {
        // IllegalArgumentException has no dedicated handler in GlobalExceptionHandler and
        // maps to 500, matching AdversaryController's equivalent threshold-validation test.
        CreateEnvironmentRequest request = CreateEnvironmentRequest.builder()
                .name("Ambiguous Environment")
                .tier(1)
                .environmentType(EnvironmentType.EVENT)
                .difficulty(10)
                .difficultySpecial("Special (see \"Relative Strength\")")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        mockMvc.perform(post("/api/dh/environments")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        assertThat(environmentRepository.findAll()).isEmpty();
    }

    @Test
    void createEnvironment_NeitherDifficultyFieldProvided_Returns500() throws Exception {
        CreateEnvironmentRequest request = CreateEnvironmentRequest.builder()
                .name("Incomplete Environment")
                .tier(1)
                .environmentType(EnvironmentType.EVENT)
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        mockMvc.perform(post("/api/dh/environments")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        assertThat(environmentRepository.findAll()).isEmpty();
    }

    // ==================== BULK CREATE TESTS ====================

    @Test
    void createEnvironmentsBulk_AsModerator_Returns201() throws Exception {
        String bulkRequest = """
            [
                {
                    "name": "Abandoned Grove",
                    "tier": 1,
                    "environmentType": "EXPLORATION",
                    "difficulty": 11,
                    "expansionId": %d,
                    "isOfficial": true
                },
                {
                    "name": "Ambushed",
                    "tier": 1,
                    "environmentType": "EVENT",
                    "difficultySpecial": "Special (see \\"Relative Strength\\")",
                    "expansionId": %d,
                    "isOfficial": true
                }
            ]
            """.formatted(testExpansion.getId(), testExpansion.getId());

        mockMvc.perform(post("/api/dh/environments/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Abandoned Grove"))
                .andExpect(jsonPath("$[1].difficultySpecial").value("Special (see \"Relative Strength\")"));

        assertThat(environmentRepository.findAll()).hasSize(2);
    }

    @Test
    void createEnvironmentsBulk_AsUser_Returns403() throws Exception {
        String bulkRequest = """
            [
                {
                    "name": "Abandoned Grove",
                    "tier": 1,
                    "environmentType": "EXPLORATION",
                    "difficulty": 11,
                    "expansionId": %d,
                    "isOfficial": true
                }
            ]
            """.formatted(testExpansion.getId());

        mockMvc.perform(post("/api/dh/environments/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE ENVIRONMENT TESTS ====================

    @Test
    void updateEnvironment_AsCreator_Returns200() throws Exception {
        Environment environment = createEnvironment("Abandoned Grove", testExpansion, false, false, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .name("Reclaimed Grove")
                .tier(2)
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reclaimed Grove"))
                .andExpect(jsonPath("$.tier").value(2));
    }

    @Test
    void updateEnvironment_SwitchNumericToSpecial_ClearsOtherField() throws Exception {
        // Exercises the clearDifficulty flag: switching a numeric-difficulty
        // environment to "Special" must null out the old numeric value, not just
        // add difficultySpecial alongside it.
        Environment environment = createEnvironment("Ambushers", testExpansion, false, false, regularUser, EnvironmentType.EVENT, 1, 12, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .difficultySpecial("Special (see \"Relative Strength\")")
                .clearDifficulty(true)
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultySpecial").value("Special (see \"Relative Strength\")"))
                .andExpect(jsonPath("$.difficulty").doesNotExist());

        Environment updated = environmentRepository.findById(environment.getId()).orElseThrow();
        assertThat(updated.getDifficulty()).isNull();
        assertThat(updated.getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
    }

    @Test
    void updateEnvironment_NonCreatorNonModerator_Returns403() throws Exception {
        Environment environment = createEnvironment("Admin Grove", testExpansion, false, false, adminUser, EnvironmentType.EXPLORATION, 1, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .name("Hacked Grove")
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEnvironment_OfficialAsNonOwner_Returns403() throws Exception {
        Environment environment = createEnvironment("Official Grove", testExpansion, true, true, ownerUser, EnvironmentType.EXPLORATION, 1, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .name("Unofficial Grove")
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEnvironment_OfficialAsOwner_Returns200() throws Exception {
        Environment environment = createEnvironment("Official Grove", testExpansion, true, true, ownerUser, EnvironmentType.EXPLORATION, 1, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .name("Updated Official Grove")
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Official Grove"));
    }

    @Test
    void updateEnvironment_NotFound_Returns404() throws Exception {
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .name("Ghost Grove")
                .build();

        mockMvc.perform(put("/api/dh/environments/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ENVIRONMENT TESTS ====================

    @Test
    void deleteEnvironment_AsCreator_Returns204() throws Exception {
        Environment environment = createEnvironment("Abandoned Grove", testExpansion, false, false, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);

        mockMvc.perform(delete("/api/dh/environments/{id}", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNoContent());

        Environment deleted = environmentRepository.findById(environment.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteEnvironment_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/environments/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE ENVIRONMENT TESTS ====================

    @Test
    void restoreEnvironment_AsAdmin_Returns200() throws Exception {
        Environment environment = createEnvironment("Abandoned Grove", testExpansion, false, false, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);
        environment.setDeletedAt(LocalDateTime.now());
        environmentRepository.save(environment);

        mockMvc.perform(post("/api/dh/environments/{id}/restore", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(environment.getId()));

        Environment restored = environmentRepository.findById(environment.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreEnvironment_AsUser_Returns403() throws Exception {
        Environment environment = createEnvironment("Abandoned Grove", testExpansion, false, false, regularUser, EnvironmentType.EXPLORATION, 1, 11, null);
        environment.setDeletedAt(LocalDateTime.now());
        environmentRepository.save(environment);

        mockMvc.perform(post("/api/dh/environments/{id}/restore", environment.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreEnvironment_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/environments/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== TEST HELPERS ====================

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

    private Environment createEnvironment(String name, Expansion expansion, Boolean isOfficial, Boolean isPublic,
                                           User createdBy, EnvironmentType environmentType, Integer tier,
                                           Integer difficulty, String difficultySpecial) {
        Environment environment = Environment.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .isPublic(isPublic)
                .createdBy(createdBy)
                .environmentType(environmentType)
                .tier(tier)
                .difficulty(difficulty)
                .difficultySpecial(difficultySpecial)
                .build();
        return environmentRepository.save(environment);
    }
}
