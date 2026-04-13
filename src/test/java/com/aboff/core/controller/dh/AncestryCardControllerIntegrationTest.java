package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.CreateMixedAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AncestryCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AncestryCardController.
 * Tests all CRUD endpoints for AncestryCard resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AncestryCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private AncestryCardRepository ancestryCardRepository;

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

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL ANCESTRY CARDS TESTS ====================

    @Test
    void getAllAncestryCards_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createAncestryCard("Elf", "Elven ancestry", testExpansion, true);
        createAncestryCard("Dwarf", "Dwarven ancestry", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllAncestryCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/cards/ancestry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllAncestryCards_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createAncestryCard("Card " + i, "Description " + i, testExpansion, true);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void getAllAncestryCards_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createAncestryCard("Card 1", "Desc 1", testExpansion, true);
        createAncestryCard("Card 2", "Desc 2", expansion2, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllAncestryCards_FilterByIsOfficial_ReturnsFiltered() throws Exception {
        // Arrange
        createAncestryCard("Official Card", "Official", testExpansion, true);
        createAncestryCard("Unofficial Card", "Unofficial", testExpansion, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .param("isOfficial", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Official Card"));
    }

    @Test
    void getAllAncestryCards_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createAncestryCard("Elf", "Elven ancestry", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET ANCESTRY CARD BY ID TESTS ====================

    @Test
    void getAncestryCardById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "Elven ancestry", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Elf"))
                .andExpect(jsonPath("$.isOfficial").value(true));
    }

    @Test
    void getAncestryCardById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/cards/ancestry/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE ANCESTRY CARD TESTS ====================

    @Test
    void createAncestryCard_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Elf")
                .description("Elven ancestry")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Elf"))
                .andExpect(jsonPath("$.isOfficial").value(true));

        assertThat(ancestryCardRepository.findAll()).hasSize(1);
    }

    @Test
    void createAncestryCard_AsUser_Returns403() throws Exception {
        // Arrange
        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Elf")
                .description("Elven ancestry")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(ancestryCardRepository.findAll()).isEmpty();
    }

    // ==================== CREATE ANCESTRY CARDS BULK TESTS ====================

    @Test
    void createAncestryCardsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateAncestryCardRequest request1 = CreateAncestryCardRequest.builder()
                .name("Elf")
                .description("Elven ancestry")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        CreateAncestryCardRequest request2 = CreateAncestryCardRequest.builder()
                .name("Dwarf")
                .description("Dwarven ancestry")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        List<CreateAncestryCardRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(ancestryCardRepository.findAll()).hasSize(2);
    }

    @Test
    void createAncestryCardsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Elf")
                .description("Elven ancestry")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        List<CreateAncestryCardRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE ANCESTRY CARD TESTS ====================

    @Test
    void updateAncestryCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "Original description", testExpansion, true);
        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(false)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("High Elf"))
                .andExpect(jsonPath("$.isOfficial").value(false));
    }

    @Test
    void updateAncestryCard_AsUser_Returns403() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "Original description", testExpansion, true);
        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAncestryCard_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/ancestry/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ANCESTRY CARD TESTS ====================

    @Test
    void deleteAncestryCard_AsAdmin_Returns204() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "To delete", testExpansion, true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        AncestryCard deleted = ancestryCardRepository.findById(card.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteAncestryCard_AsUser_Returns403() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "To delete", testExpansion, true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        AncestryCard notDeleted = ancestryCardRepository.findById(card.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteAncestryCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/cards/ancestry/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE ANCESTRY CARD TESTS ====================

    @Test
    void restoreAncestryCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "Deleted card", testExpansion, true);
        card.setDeletedAt(LocalDateTime.now());
        ancestryCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        AncestryCard restored = ancestryCardRepository.findById(card.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreAncestryCard_AsUser_Returns403() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Elf", "Deleted card", testExpansion, true);
        card.setDeletedAt(LocalDateTime.now());
        ancestryCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreAncestryCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/cards/ancestry/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== INLINE FEATURE TESTS ====================

    @Test
    void createAncestryCard_WithInlineFeatures_Returns201AndCreatesFeatures() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Ribbet Ancestry",
                "description": "Frog-like humanoids",
                "expansionId": %d,
                "isOfficial": true,
                "features": [
                    {
                        "name": "Mighty Leap",
                        "description": "Jump great distances",
                        "featureType": "ANCESTRY",
                        "expansionId": %d
                    },
                    {
                        "name": "Amphibious",
                        "description": "Breathe underwater",
                        "featureType": "ANCESTRY",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(2));

        // Verify features were actually created in the database
        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(2);
        assertThat(features).extracting(Feature::getName)
                .containsExactlyInAnyOrder("Mighty Leap", "Amphibious");
    }

    @Test
    void createAncestryCard_WithMixedFeatures_Returns201AndMerges() throws Exception {
        // Arrange - create an existing feature first
        Feature existingFeature = Feature.builder()
                .name("Existing Feature")
                .description("Pre-existing feature")
                .featureType(com.aboff.core.model.enums.FeatureType.ANCESTRY)
                .expansion(testExpansion)
                .build();
        existingFeature = featureRepository.save(existingFeature);

        String requestJson = """
            {
                "name": "Mixed Ancestry",
                "description": "Mixed feature ancestry",
                "expansionId": %d,
                "isOfficial": true,
                "featureIds": [%d],
                "features": [
                    {
                        "name": "New Inline Feature",
                        "description": "A brand new feature",
                        "featureType": "ANCESTRY",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), existingFeature.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(2));

        // Verify both features exist
        assertThat(featureRepository.findAll()).hasSize(2);
    }

    @Test
    void createAncestryCard_WithInlineFeaturesHavingCostTags_Returns201() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Cost Tag Ancestry",
                "description": "Ancestry with cost tags on features",
                "expansionId": %d,
                "isOfficial": true,
                "features": [
                    {
                        "name": "Costly Feature",
                        "description": "A feature with cost tags",
                        "featureType": "ANCESTRY",
                        "expansionId": %d,
                        "costTags": [
                            { "label": "1/session", "category": "LIMITATION" }
                        ]
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        // Verify feature and cost tag were created
        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(1);
        assertThat(features.get(0).getName()).isEqualTo("Costly Feature");
        assertThat(features.get(0).getCostTags()).hasSize(1);
    }

    @Test
    void createAncestryCard_WithInvalidFeatureInput_Returns400() throws Exception {
        // Arrange - missing required featureType
        String requestJson = """
            {
                "name": "Invalid Feature Card",
                "description": "Card with invalid feature",
                "expansionId": %d,
                "isOfficial": true,
                "features": [
                    {
                        "name": "Missing Type Feature",
                        "description": "This feature has no type"
                    }
                ]
            }
            """.formatted(testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAncestryCard_WithInlineFeatureInvalidExpansion_Returns404() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Bad Expansion Card",
                "description": "Card with feature referencing bad expansion",
                "expansionId": %d,
                "isOfficial": true,
                "features": [
                    {
                        "name": "Bad Expansion Feature",
                        "description": "References non-existent expansion",
                        "featureType": "ANCESTRY",
                        "expansionId": 99999
                    }
                ]
            }
            """.formatted(testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAncestryCard_WithInlineFeatures_Returns200() throws Exception {
        // Arrange
        AncestryCard card = createAncestryCard("Update Target", "Original", testExpansion, true);
        String requestJson = """
            {
                "name": "Updated Card",
                "description": "Updated description",
                "expansionId": %d,
                "isOfficial": true,
                "features": [
                    {
                        "name": "Update Feature",
                        "description": "Added via update",
                        "featureType": "ANCESTRY",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/ancestry/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        assertThat(featureRepository.findAll()).hasSize(1);
    }

    @Test
    void createAncestryCardBulk_WithInlineFeatures_Returns201() throws Exception {
        // Arrange
        String requestJson = """
            [
                {
                    "name": "Bulk Card 1",
                    "description": "First bulk card",
                    "expansionId": %d,
                    "isOfficial": true,
                    "features": [
                        {
                            "name": "Bulk Feature",
                            "description": "Feature from bulk",
                            "featureType": "ANCESTRY",
                            "expansionId": %d
                        }
                    ]
                },
                {
                    "name": "Bulk Card 2",
                    "description": "Second bulk card with existing IDs only",
                    "expansionId": %d,
                    "isOfficial": true
                }
            ]
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].featureIds.length()").value(1));

        assertThat(featureRepository.findAll()).hasSize(1);
    }

    @Test
    void createAncestryCard_SameInlineFeatureTwice_ReusesSameFeature() throws Exception {
        // Arrange - same inline feature in two separate requests
        String featureJson = """
            { "name": "Mighty Leap", "featureType": "ANCESTRY", "expansionId": %d }
            """.formatted(testExpansion.getId());
        String request1 = """
            { "name": "Card A", "expansionId": %d, "isOfficial": true, "features": [%s] }
            """.formatted(testExpansion.getId(), featureJson);
        String request2 = """
            { "name": "Card B", "expansionId": %d, "isOfficial": true, "features": [%s] }
            """.formatted(testExpansion.getId(), featureJson);

        // Act - create two cards
        MvcResult result1 = mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert - only one feature created, shared by both cards
        assertThat(featureRepository.findAll()).hasSize(1);

        JsonNode card1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        JsonNode card2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        assertThat(card1.get("featureIds").get(0).asLong())
                .isEqualTo(card2.get("featureIds").get(0).asLong());
    }

    // ==================== MIXED ANCESTRY CARD TESTS ====================

    @Test
    void createMixedAncestryCard_AsAuthenticatedUser_Returns201() throws Exception {
        // Arrange
        Feature feature1 = createFeature("Darkvision", "See in the dark");
        Feature feature2 = createFeature("Nimble", "Quick on your feet");

        CreateMixedAncestryCardRequest request = CreateMixedAncestryCardRequest.builder()
                .name("Half-Elf")
                .description("Mixed elf and human ancestry")
                .expansionId(testExpansion.getId())
                .featureIds(List.of(feature1.getId(), feature2.getId()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/mixed")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Half-Elf"))
                .andExpect(jsonPath("$.isOfficial").value(false))
                .andExpect(jsonPath("$.featureIds.length()").value(2));
    }

    @Test
    void createMixedAncestryCard_WithInvalidFeatureCount_Returns400() throws Exception {
        // Arrange - only 1 feature ID (needs exactly 2)
        Feature feature1 = createFeature("Darkvision", "See in the dark");

        CreateMixedAncestryCardRequest requestTooFew = CreateMixedAncestryCardRequest.builder()
                .name("Bad Mixed")
                .description("Too few features")
                .expansionId(testExpansion.getId())
                .featureIds(List.of(feature1.getId()))
                .build();

        // Act & Assert - too few
        mockMvc.perform(post("/api/dh/cards/ancestry/mixed")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTooFew)))
                .andExpect(status().isBadRequest());

        // Arrange - 3 feature IDs (needs exactly 2)
        Feature feature2 = createFeature("Nimble", "Quick on your feet");
        Feature feature3 = createFeature("Tough", "Hardy constitution");

        CreateMixedAncestryCardRequest requestTooMany = CreateMixedAncestryCardRequest.builder()
                .name("Bad Mixed")
                .description("Too many features")
                .expansionId(testExpansion.getId())
                .featureIds(List.of(feature1.getId(), feature2.getId(), feature3.getId()))
                .build();

        // Act & Assert - too many
        mockMvc.perform(post("/api/dh/cards/ancestry/mixed")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTooMany)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAncestryCards_ExcludesMixedByDefault() throws Exception {
        // Arrange
        createAncestryCard("Standard Elf", "Normal ancestry", testExpansion, true);
        AncestryCard mixedCard = AncestryCard.builder()
                .name("Mixed Heritage")
                .description("A mixed ancestry")
                .expansion(testExpansion)
                .isOfficial(false)
                .isMixed(true)
                .build();
        ancestryCardRepository.save(mixedCard);

        // Act & Assert - default (no isMixed param) should exclude mixed
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Standard Elf"));
    }

    @Test
    void getAllAncestryCards_WithIsMixedTrue_ReturnsOnlyMixed() throws Exception {
        // Arrange
        createAncestryCard("Standard Elf", "Normal ancestry", testExpansion, true);
        AncestryCard mixedCard = AncestryCard.builder()
                .name("Mixed Heritage")
                .description("A mixed ancestry")
                .expansion(testExpansion)
                .isOfficial(false)
                .isMixed(true)
                .build();
        ancestryCardRepository.save(mixedCard);

        // Act & Assert - isMixed=true should return only mixed cards
        mockMvc.perform(get("/api/dh/cards/ancestry")
                        .param("isMixed", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Mixed Heritage"))
                .andExpect(jsonPath("$.content[0].isMixed").value(true));
    }

    @Test
    void createMixedAncestryCard_ResponseIncludesIsMixed() throws Exception {
        // Arrange
        Feature feature1 = createFeature("Darkvision", "See in the dark");
        Feature feature2 = createFeature("Nimble", "Quick on your feet");

        CreateMixedAncestryCardRequest request = CreateMixedAncestryCardRequest.builder()
                .name("Half-Dwarf")
                .description("Mixed dwarf ancestry")
                .expansionId(testExpansion.getId())
                .featureIds(List.of(feature1.getId(), feature2.getId()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/ancestry/mixed")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isMixed").value(true));
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

    private AncestryCard createAncestryCard(String name, String description, Expansion expansion, Boolean isOfficial) {
        AncestryCard card = AncestryCard.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .build();
        return ancestryCardRepository.save(card);
    }

    private Feature createFeature(String name, String description) {
        Feature feature = Feature.builder()
                .name(name)
                .description(description)
                .featureType(com.aboff.core.model.enums.FeatureType.ANCESTRY)
                .expansion(testExpansion)
                .build();
        return featureRepository.save(feature);
    }
}
