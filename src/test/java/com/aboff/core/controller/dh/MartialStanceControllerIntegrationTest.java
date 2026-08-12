package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateMartialStanceRequest;
import com.aboff.core.model.dto.dh.request.UpdateMartialStanceRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.MartialStance;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.MartialStanceRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for MartialStanceController.
 * Tests all CRUD endpoints for MartialStance resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MartialStanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private MartialStanceRepository martialStanceRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private FeatureRepository featureRepository;

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

        testExpansion = createExpansion("Hope and Fear", true);
    }

    // ==================== GET ALL MARTIAL STANCES TESTS ====================

    @Test
    void getAllMartialStances_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        createMartialStance("Reliable", testExpansion, true, "Gain a +1 bonus to attack rolls.", 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllMartialStances_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/martial-stances"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllMartialStances_FilterByExpansion_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Expansion 2", true);
        createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        createMartialStance("Anchored", expansion2, true, "Gain a +2 bonus to damage thresholds.", 2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Favored"));
    }

    @Test
    void getAllMartialStances_FilterByTier_ReturnsFiltered() throws Exception {
        // Arrange
        createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        createMartialStance("Anchored", testExpansion, true, "Gain a +2 bonus to damage thresholds.", 2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances")
                        .param("tier", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Anchored"));
    }

    @Test
    void getAllMartialStances_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Hope and Fear"));
    }

    // ==================== GET MARTIAL STANCE BY ID TESTS ====================

    @Test
    void getMartialStanceById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances/{id}", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stance.getId()))
                .andExpect(jsonPath("$.name").value("Favored"))
                .andExpect(jsonPath("$.description").value("Gain a bonus to damage rolls."));
    }

    @Test
    void getMartialStanceById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/martial-stances/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE MARTIAL STANCE TESTS ====================

    @Test
    void createMartialStance_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .description("Gain a bonus to damage rolls equal to a trait of your choice.")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Favored"))
                .andExpect(jsonPath("$.description").value("Gain a bonus to damage rolls equal to a trait of your choice."));

        assertThat(martialStanceRepository.findAll()).hasSize(1);
    }

    @Test
    void createMartialStance_AsUser_Returns403() throws Exception {
        // Arrange
        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .description("Gain a bonus to damage rolls.")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(martialStanceRepository.findAll()).isEmpty();
    }

    // ==================== CREATE MARTIAL STANCE BULK TESTS ====================

    @Test
    void createMartialStanceBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateMartialStanceRequest request1 = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .build();
        CreateMartialStanceRequest request2 = CreateMartialStanceRequest.builder()
                .name("Reliable")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .description("Gain a +1 bonus to your attack rolls.")
                .build();
        List<CreateMartialStanceRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(martialStanceRepository.findAll()).hasSize(2);
    }

    @Test
    void createMartialStanceBulk_AllTwelveStances_Returns201() throws Exception {
        // Arrange - mirrors the real Hope & Fear acceptance shape: 12 stance texts, tiers 1-4
        List<CreateMartialStanceRequest> requests = List.of(
                stanceRequest("Favored", 1), stanceRequest("Invigorating", 1),
                stanceRequest("Quick", 1), stanceRequest("Reliable", 1),
                stanceRequest("Aggressive", 2), stanceRequest("Anchored", 2),
                stanceRequest("Evasive", 2), stanceRequest("Vigilant", 2),
                stanceRequest("Relentless", 3), stanceRequest("Precise", 3),
                stanceRequest("Unshakable", 4), stanceRequest("Deadly", 4)
        );

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(12));

        assertThat(martialStanceRepository.findAll()).hasSize(12);
    }

    private CreateMartialStanceRequest stanceRequest(String name, int tier) {
        return CreateMartialStanceRequest.builder()
                .name(name)
                .expansionId(testExpansion.getId())
                .tier(tier)
                .isOfficial(true)
                .description(name + " stance effect text.")
                .build();
    }

    @Test
    void createMartialStanceBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .build();
        List<CreateMartialStanceRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE MARTIAL STANCE TESTS ====================

    @Test
    void updateMartialStance_AsAdmin_Returns200() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Greater Favored")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .description("Gain a bigger bonus to damage rolls.")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/martial-stances/{id}", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stance.getId()))
                .andExpect(jsonPath("$.name").value("Greater Favored"))
                .andExpect(jsonPath("$.description").value("Gain a bigger bonus to damage rolls."));
    }

    @Test
    void updateMartialStance_AsUser_Returns403() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Greater Favored")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .description("Gain a bigger bonus to damage rolls.")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/martial-stances/{id}", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMartialStance_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Greater Favored")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/martial-stances/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE MARTIAL STANCE TESTS ====================

    @Test
    void deleteMartialStance_AsAdmin_Returns204() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/martial-stances/{id}", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        MartialStance deleted = martialStanceRepository.findById(stance.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteMartialStance_AsUser_Returns403() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/martial-stances/{id}", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        MartialStance notDeleted = martialStanceRepository.findById(stance.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteMartialStance_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/martial-stances/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE MARTIAL STANCE TESTS ====================

    @Test
    void restoreMartialStance_AsAdmin_Returns200() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        stance.setDeletedAt(LocalDateTime.now());
        martialStanceRepository.save(stance);

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances/{id}/restore", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stance.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        MartialStance restored = martialStanceRepository.findById(stance.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreMartialStance_AsUser_Returns403() throws Exception {
        // Arrange
        MartialStance stance = createMartialStance("Favored", testExpansion, true, "Gain a bonus to damage rolls.", 1);
        stance.setDeletedAt(LocalDateTime.now());
        martialStanceRepository.save(stance);

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances/{id}/restore", stance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreMartialStance_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/martial-stances/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== FEATURE TESTS ====================

    @Test
    void createMartialStance_WithFeatureIds_Returns201WithFeatures() throws Exception {
        // Arrange
        Feature feature = Feature.builder()
                .name("Aggressive Stance Bonus")
                .description("Grants extra damage while off-balance")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .isOfficial(false)
                .build();
        feature = featureRepository.save(feature);

        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Aggressive")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .description("A penalty-for-power stance")
                .featureIds(List.of(feature.getId()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/martial-stances")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Aggressive"))
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1))
                .andExpect(jsonPath("$.featureIds[0]").value(feature.getId()));
    }

    @Test
    void getMartialStanceById_WithExpandFeatures_ReturnsFullFeatures() throws Exception {
        // Arrange
        Feature feature = Feature.builder()
                .name("Lucky Stance")
                .description("Grants luck while in this stance")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .isOfficial(false)
                .build();
        feature = featureRepository.save(feature);

        MartialStance stance = MartialStance.builder()
                .name("Lucky")
                .expansion(testExpansion)
                .tier(1)
                .isOfficial(true)
                .description("A lucky stance")
                .features(new java.util.HashSet<>(java.util.Set.of(feature)))
                .build();
        stance = martialStanceRepository.save(stance);

        // Act & Assert
        mockMvc.perform(get("/api/dh/martial-stances/{id}", stance.getId())
                        .param("expand", "features")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features.length()").value(1))
                .andExpect(jsonPath("$.features[0].name").value("Lucky Stance"));
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

    private MartialStance createMartialStance(String name, Expansion expansion, Boolean isOfficial, String description, Integer tier) {
        MartialStance stance = MartialStance.builder()
                .name(name)
                .expansion(expansion)
                .tier(tier)
                .isOfficial(isOfficial)
                .description(description)
                .build();
        return martialStanceRepository.save(stance);
    }
}
