package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.LootRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for LootController.
 * Tests all CRUD endpoints for Loot resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LootControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private LootRepository lootRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // ==================== GET ALL LOOT TESTS ====================

    @Test
    void getAllLoot_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createLoot("Health Potion", testExpansion, true, "Restores health");
        createLoot("Rope", testExpansion, true, "50 feet of rope");

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllLoot_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/loot"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllLoot_FilterByExpansion_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Expansion 2", true);
        createLoot("Health Potion", testExpansion, true, "Restores health");
        createLoot("Mana Potion", expansion2, true, "Restores mana");

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Health Potion"));
    }

    @Test
    void getAllLoot_FilterByIsOfficial_ReturnsFiltered() throws Exception {
        // Arrange
        createLoot("Health Potion", testExpansion, true, "Restores health");
        createLoot("Custom Potion", testExpansion, false, "A custom item");

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot")
                        .param("isOfficial", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Health Potion"));
    }

    @Test
    void getAllLoot_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createLoot("Health Potion", testExpansion, true, "Restores health");

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET LOOT BY ID TESTS ====================

    @Test
    void getLootById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot/{id}", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loot.getId()))
                .andExpect(jsonPath("$.name").value("Health Potion"))
                .andExpect(jsonPath("$.description").value("Restores health"));
    }

    @Test
    void getLootById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/loot/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE LOOT TESTS ====================

    @Test
    void createLoot_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateLootRequest request = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("Restores health when consumed")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Health Potion"))
                .andExpect(jsonPath("$.description").value("Restores health when consumed"));

        assertThat(lootRepository.findAll()).hasSize(1);
    }

    @Test
    void createLoot_AsUser_Returns403() throws Exception {
        // Arrange
        CreateLootRequest request = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("Restores health")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(lootRepository.findAll()).isEmpty();
    }

    // ==================== CREATE LOOT BULK TESTS ====================

    @Test
    void createLootBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateLootRequest request1 = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .build();
        CreateLootRequest request2 = CreateLootRequest.builder()
                .name("Rope")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isConsumable(false)
                .description("50 feet of rope")
                .build();
        List<CreateLootRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(lootRepository.findAll()).hasSize(2);
    }

    @Test
    void createLootBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateLootRequest request = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .build();
        List<CreateLootRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE LOOT TESTS ====================

    @Test
    void updateLoot_AsAdmin_Returns200() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");
        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Greater Health Potion")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isConsumable(true)
                .description("Restores more health")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/loot/{id}", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loot.getId()))
                .andExpect(jsonPath("$.name").value("Greater Health Potion"))
                .andExpect(jsonPath("$.description").value("Restores more health"));
    }

    @Test
    void updateLoot_AsUser_Returns403() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");
        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Greater Health Potion")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isConsumable(true)
                .description("Restores more health")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/loot/{id}", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateLoot_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Greater Health Potion")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isConsumable(false)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/loot/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE LOOT TESTS ====================

    @Test
    void deleteLoot_AsAdmin_Returns204() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");

        // Act & Assert
        mockMvc.perform(delete("/api/dh/loot/{id}", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Loot deleted = lootRepository.findById(loot.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteLoot_AsUser_Returns403() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");

        // Act & Assert
        mockMvc.perform(delete("/api/dh/loot/{id}", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Loot notDeleted = lootRepository.findById(loot.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteLoot_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/loot/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE LOOT TESTS ====================

    @Test
    void restoreLoot_AsAdmin_Returns200() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");
        loot.setDeletedAt(LocalDateTime.now());
        lootRepository.save(loot);

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot/{id}/restore", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loot.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Loot restored = lootRepository.findById(loot.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreLoot_AsUser_Returns403() throws Exception {
        // Arrange
        Loot loot = createLoot("Health Potion", testExpansion, true, "Restores health");
        loot.setDeletedAt(LocalDateTime.now());
        lootRepository.save(loot);

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot/{id}/restore", loot.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreLoot_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/loot/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== FEATURE TESTS ====================

    @Test
    void createLoot_WithFeatureIds_Returns201WithFeatures() throws Exception {
        // Arrange
        Feature feature = Feature.builder()
                .name("Healing Aura")
                .description("Grants healing over time")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .build();
        feature = featureRepository.save(feature);

        CreateLootRequest request = CreateLootRequest.builder()
                .name("Healing Crystal")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isConsumable(false)
                .description("A crystal that heals")
                .featureIds(List.of(feature.getId()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Healing Crystal"))
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1))
                .andExpect(jsonPath("$.featureIds[0]").value(feature.getId()));
    }

    @Test
    void createLoot_WithInlineFeature_Returns201AndCreatesFeature() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Magic Ring",
                "expansionId": %d,
                "tier": 2,
                "isOfficial": true,
                "isConsumable": false,
                "description": "A ring with magical properties",
                "features": [
                    {
                        "name": "Protection Aura",
                        "description": "Grants minor protection",
                        "featureType": "ITEM",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/loot")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(1);
        assertThat(features.get(0).getName()).isEqualTo("Protection Aura");
    }

    @Test
    void getLootById_WithExpandFeatures_ReturnsFullFeatures() throws Exception {
        // Arrange
        Feature feature = Feature.builder()
                .name("Lucky Charm")
                .description("Grants luck")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .build();
        feature = featureRepository.save(feature);

        Loot loot = Loot.builder()
                .name("Lucky Coin")
                .expansion(testExpansion)
                .tier(1)
                .isOfficial(true)
                .isConsumable(false)
                .description("A coin that brings luck")
                .features(new java.util.HashSet<>(java.util.Set.of(feature)))
                .build();
        loot = lootRepository.save(loot);

        // Act & Assert
        mockMvc.perform(get("/api/dh/loot/{id}", loot.getId())
                        .param("expand", "features")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features.length()").value(1))
                .andExpect(jsonPath("$.features[0].name").value("Lucky Charm"));
    }

    // ==================== HELPER METHODS ====================

    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
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

    private Loot createLoot(String name, Expansion expansion, Boolean isOfficial, String description) {
        return createLoot(name, expansion, isOfficial, description, 1);
    }

    private Loot createLoot(String name, Expansion expansion, Boolean isOfficial, String description, Integer tier) {
        return createLoot(name, expansion, isOfficial, description, tier, false);
    }

    private Loot createLoot(String name, Expansion expansion, Boolean isOfficial, String description, Integer tier, Boolean isConsumable) {
        Loot loot = Loot.builder()
                .name(name)
                .expansion(expansion)
                .tier(tier)
                .isOfficial(isOfficial)
                .isConsumable(isConsumable)
                .description(description)
                .build();
        return lootRepository.save(loot);
    }
}
