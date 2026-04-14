package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.UpdateAdversaryRequest;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.*;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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
 * Integration tests for AdversaryController.
 * Tests all CRUD endpoints for Adversary resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AdversaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private AdversaryRepository adversaryRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private FeatureRepository featureRepository;


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

    // ==================== GET ALL ADVERSARIES TESTS ====================

    @Test
    void getAllAdversaries_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);
        createAdversary("Orc Warrior", testExpansion, true, true, regularUser, AdversaryType.BRUISER, 2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllAdversaries_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/adversaries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllAdversaries_FilterByTier_ReturnsFiltered() throws Exception {
        // Arrange
        createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);
        createAdversary("Dragon", testExpansion, true, true, regularUser, AdversaryType.LEADER, 4);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries")
                        .param("tier", "1")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tier").value(1));
    }

    @Test
    void getAllAdversaries_FilterByAdversaryType_ReturnsFiltered() throws Exception {
        // Arrange
        createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);
        createAdversary("Orc Chief", testExpansion, true, true, regularUser, AdversaryType.LEADER, 3);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries")
                        .param("adversaryType", "LEADER")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].adversaryType").value("LEADER"));
    }

    @Test
    void getAllAdversaries_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET ADVERSARY BY ID TESTS ====================

    @Test
    void getAdversaryById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adversary.getId()))
                .andExpect(jsonPath("$.name").value("Goblin"))
                .andExpect(jsonPath("$.tier").value(1))
                .andExpect(jsonPath("$.adversaryType").value("MINION"));
    }

    @Test
    void getAdversaryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/adversaries/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAdversaryById_PrivateAdversaryAsNonCreator_Returns404() throws Exception {
        // Arrange - create private adversary by admin
        Adversary adversary = createAdversary("Secret Goblin", testExpansion, false, false, adminUser, AdversaryType.MINION, 1);

        // Act & Assert - regular user tries to access
        mockMvc.perform(get("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAdversaryById_PrivateAdversaryAsCreator_Returns200() throws Exception {
        // Arrange - create private adversary by regular user
        Adversary adversary = createAdversary("My Secret Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);

        // Act & Assert - same user can access it
        mockMvc.perform(get("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Secret Goblin"));
    }

    // ==================== CREATE ADVERSARY TESTS ====================

    @Test
    void createAdversary_AsUser_Returns201() throws Exception {
        // Arrange
        CreateAdversaryRequest request = CreateAdversaryRequest.builder()
                .name("Goblin")
                .tier(1)
                .adversaryType(AdversaryType.MINION)
                .difficulty(5)
                .majorThreshold(3)
                .severeThreshold(6)
                .hitPointMax(10)
                .stressMax(5)
                .expansionId(testExpansion.getId())
                .isPublic(false)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Goblin"))
                .andExpect(jsonPath("$.tier").value(1))
                .andExpect(jsonPath("$.adversaryType").value("MINION"));

        assertThat(adversaryRepository.findAll()).hasSize(1);
    }

    @Test
    void createAdversary_WithDamage_Returns201() throws Exception {
        // Arrange
        CreateAdversaryRequest request = CreateAdversaryRequest.builder()
                .name("Orc Warrior")
                .tier(2)
                .adversaryType(AdversaryType.BRUISER)
                .difficulty(8)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(20)
                .stressMax(10)
                .expansionId(testExpansion.getId())
                .attackModifier(3)
                .weaponName("Greataxe")
                .attackRange(Range.MELEE)
                .damage(CreateAdversaryRequest.DamageRollRequest.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(5)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weaponName").value("Greataxe"))
                .andExpect(jsonPath("$.damage.notation").value("2d10+5 phy"));
    }

    @Test
    void createAdversary_InvalidThresholds_Returns400() throws Exception {
        // Arrange - severe threshold less than major threshold
        CreateAdversaryRequest request = CreateAdversaryRequest.builder()
                .name("Invalid Goblin")
                .tier(1)
                .adversaryType(AdversaryType.MINION)
                .difficulty(5)
                .majorThreshold(10)  // higher than severe
                .severeThreshold(5)   // lower than major
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert - IllegalArgumentException maps to 500 Internal Server Error
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== UPDATE ADVERSARY TESTS ====================

    @Test
    void updateAdversary_AsCreator_Returns200() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Elite Goblin")
                .tier(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adversary.getId()))
                .andExpect(jsonPath("$.name").value("Elite Goblin"))
                .andExpect(jsonPath("$.tier").value(2));
    }

    @Test
    void updateAdversary_NonCreatorNonModerator_Returns403() throws Exception {
        // Arrange - adversary created by admin
        Adversary adversary = createAdversary("Admin Goblin", testExpansion, false, false, adminUser, AdversaryType.MINION, 1);
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Hacked Goblin")
                .build();

        // Act & Assert - regular user tries to update
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAdversary_OfficialAsNonOwner_Returns403() throws Exception {
        // Arrange - official adversary
        Adversary adversary = createAdversary("Official Goblin", testExpansion, true, true, ownerUser, AdversaryType.MINION, 1);
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Unofficial Goblin")
                .build();

        // Act & Assert - admin (not owner) tries to update official
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAdversary_OfficialAsOwner_Returns200() throws Exception {
        // Arrange - official adversary by owner
        Adversary adversary = createAdversary("Official Goblin", testExpansion, true, true, ownerUser, AdversaryType.MINION, 1);
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Official Goblin")
                .build();

        // Act & Assert - owner can update official
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Official Goblin"));
    }

    @Test
    void updateAdversary_AsModerator_Returns200() throws Exception {
        // Arrange - non-official adversary created by regular user
        Adversary adversary = createAdversary("User Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Moderated Goblin")
                .build();

        // Act & Assert - moderator can update
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moderated Goblin"));
    }

    @Test
    void updateAdversary_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Ghost Goblin")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/adversaries/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ADVERSARY TESTS ====================

    @Test
    void deleteAdversary_AsCreator_Returns204() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNoContent());

        Adversary deleted = adversaryRepository.findById(adversary.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteAdversary_NonCreatorNonModerator_Returns403() throws Exception {
        // Arrange - adversary created by admin
        Adversary adversary = createAdversary("Admin Goblin", testExpansion, false, false, adminUser, AdversaryType.MINION, 1);

        // Act & Assert - regular user tries to delete
        mockMvc.perform(delete("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Adversary notDeleted = adversaryRepository.findById(adversary.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteAdversary_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/adversaries/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE ADVERSARY TESTS ====================

    @Test
    void restoreAdversary_AsAdmin_Returns200() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);
        adversary.setDeletedAt(LocalDateTime.now());
        adversaryRepository.save(adversary);

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/{id}/restore", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adversary.getId()));

        Adversary restored = adversaryRepository.findById(adversary.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreAdversary_AsUser_Returns403() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Goblin", testExpansion, false, false, regularUser, AdversaryType.MINION, 1);
        adversary.setDeletedAt(LocalDateTime.now());
        adversaryRepository.save(adversary);

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/{id}/restore", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreAdversary_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/adversaries/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== COPY ADVERSARY TESTS ====================

    @Test
    void copyAdversary_AsUser_Returns201() throws Exception {
        // Arrange - create a public adversary
        Adversary adversary = createAdversary("Original Goblin", testExpansion, false, true, adminUser, AdversaryType.MINION, 1);

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/{id}/copy", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Original Goblin (Copy)"))
                .andExpect(jsonPath("$.originalAdversaryId").value(adversary.getId()))
                .andExpect(jsonPath("$.isOfficial").value(false))
                .andExpect(jsonPath("$.isPublic").value(false));

        assertThat(adversaryRepository.findAll()).hasSize(2);
    }

    @Test
    void copyAdversary_PrivateNotAccessible_Returns404() throws Exception {
        // Arrange - create a private adversary by admin
        Adversary adversary = createAdversary("Secret Goblin", testExpansion, false, false, adminUser, AdversaryType.MINION, 1);

        // Act & Assert - regular user cannot copy private adversary
        mockMvc.perform(post("/api/dh/adversaries/{id}/copy", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== BATCH CREATE TESTS ====================

    @Test
    void batchCreateAdversaries_AsModerator_Returns201() throws Exception {
        // Arrange
        String batchRequest = """
            {
                "adversaries": [
                    {
                        "name": "Goblin 1",
                        "tier": 1,
                        "adversaryType": "MINION",
                        "difficulty": 5,
                        "majorThreshold": 3,
                        "severeThreshold": 6,
                        "expansionId": %d
                    },
                    {
                        "name": "Goblin 2",
                        "tier": 1,
                        "adversaryType": "MINION",
                        "difficulty": 5,
                        "majorThreshold": 3,
                        "severeThreshold": 6,
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCreated").value(2))
                .andExpect(jsonPath("$.totalFailed").value(0))
                .andExpect(jsonPath("$.created.length()").value(2));

        assertThat(adversaryRepository.findAll()).hasSize(2);
    }

    @Test
    void batchCreateAdversaries_AsUser_Returns403() throws Exception {
        // Arrange
        String batchRequest = """
            {
                "adversaries": [
                    {
                        "name": "Goblin",
                        "tier": 1,
                        "adversaryType": "MINION",
                        "difficulty": 5,
                        "majorThreshold": 3,
                        "severeThreshold": 6,
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId());

        // Act & Assert - regular user cannot batch create
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchRequest))
                .andExpect(status().isForbidden());
    }

    // ==================== INLINE FEATURE TESTS ====================

    @Test
    void createAdversary_WithInlineFeatures_Returns201AndCreatesFeatures() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Shadow Drake",
                "tier": 3,
                "adversaryType": "BRUISER",
                "difficulty": 10,
                "majorThreshold": 5,
                "severeThreshold": 10,
                "hitPointMax": 30,
                "stressMax": 10,
                "expansionId": %d,
                "features": [
                    {
                        "name": "Shadow Breath",
                        "description": "Exhales a cone of shadow energy",
                        "featureType": "OTHER",
                        "expansionId": %d
                    },
                    {
                        "name": "Dark Resilience",
                        "description": "Resistance to shadow damage",
                        "featureType": "OTHER",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(2));

        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(2);
        assertThat(features).extracting(Feature::getName)
                .containsExactlyInAnyOrder("Shadow Breath", "Dark Resilience");
    }

    @Test
    void createAdversary_WithMixedFeatures_Returns201AndMerges() throws Exception {
        // Arrange - create existing feature
        Feature existingFeature = Feature.builder()
                .name("Existing Adversary Feature")
                .description("Pre-existing")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .build();
        existingFeature = featureRepository.save(existingFeature);

        String requestJson = """
            {
                "name": "Mixed Feature Goblin",
                "tier": 1,
                "adversaryType": "MINION",
                "difficulty": 5,
                "majorThreshold": 3,
                "severeThreshold": 6,
                "expansionId": %d,
                "featureIds": [%d],
                "features": [
                    {
                        "name": "New Goblin Trick",
                        "description": "A sneaky trick",
                        "featureType": "OTHER",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), existingFeature.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(2));

        assertThat(featureRepository.findAll()).hasSize(2);
    }

    @Test
    void createAdversary_WithInlineFeaturesHavingCostTags_Returns201() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Cost Tag Adversary",
                "tier": 2,
                "adversaryType": "LEADER",
                "difficulty": 8,
                "majorThreshold": 5,
                "severeThreshold": 10,
                "expansionId": %d,
                "features": [
                    {
                        "name": "Costly Ability",
                        "description": "An expensive ability",
                        "featureType": "OTHER",
                        "expansionId": %d,
                        "costTags": [
                            { "label": "Recharge 5-6", "category": "LIMITATION" }
                        ]
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(1);
        assertThat(features.get(0).getCostTags()).hasSize(1);
    }

    @Test
    void updateAdversary_WithInlineFeatures_Returns200() throws Exception {
        // Arrange
        Adversary adversary = createAdversary("Update Target", testExpansion, false, false,
                regularUser, AdversaryType.MINION, 1);
        String requestJson = """
            {
                "features": [
                    {
                        "name": "Update Feature",
                        "description": "Added via update",
                        "featureType": "OTHER",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId());

        // Act & Assert
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        assertThat(featureRepository.findAll()).hasSize(1);
    }

    @Test
    void batchCreateAdversaries_WithInlineFeatures_Returns201() throws Exception {
        // Arrange
        String batchRequest = """
            {
                "adversaries": [
                    {
                        "name": "Batch Goblin 1",
                        "tier": 1,
                        "adversaryType": "MINION",
                        "difficulty": 5,
                        "majorThreshold": 3,
                        "severeThreshold": 6,
                        "expansionId": %d,
                        "features": [
                            {
                                "name": "Batch Feature",
                                "description": "Feature from batch",
                                "featureType": "OTHER",
                                "expansionId": %d
                            }
                        ]
                    },
                    {
                        "name": "Batch Goblin 2",
                        "tier": 1,
                        "adversaryType": "MINION",
                        "difficulty": 5,
                        "majorThreshold": 3,
                        "severeThreshold": 6,
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCreated").value(2));

        assertThat(featureRepository.findAll()).hasSize(1);
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

    private Adversary createAdversary(String name, Expansion expansion, Boolean isOfficial, Boolean isPublic,
                                       User createdBy, AdversaryType adversaryType, Integer tier) {
        Adversary adversary = Adversary.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .isPublic(isPublic)
                .createdBy(createdBy)
                .adversaryType(adversaryType)
                .tier(tier)
                .difficulty(5)
                .majorThreshold(3)
                .severeThreshold(6)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(5)
                .stressMarked(0)
                .damage(DamageRoll.builder()
                        .diceCount(1)
                        .diceType(DiceType.D6)
                        .modifier(2)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        return adversaryRepository.save(adversary);
    }
}
