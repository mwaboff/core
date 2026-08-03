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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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
    void getAllAdversaries_FilterByMultipleTiers_ReturnsAdversariesFromAnyListedTier() throws Exception {
        // Arrange - browsing several tiers at once via repeated ?tier= params. Production data
        // has dozens of adversaries per tier (Core + Hope & Fear: T1 86, T2 78, T3 55, T4 45),
        // so this seeds several per tier rather than one, to catch an IN-clause bug that only
        // returns a single match per tier value instead of all of them.
        createAdversary("Goblin", testExpansion, true, true, regularUser, AdversaryType.MINION, 1);
        createAdversary("Goblin Scout", testExpansion, true, true, regularUser, AdversaryType.SKULK, 1);
        createAdversary("Goblin Archer", testExpansion, true, true, regularUser, AdversaryType.RANGED, 1);
        createAdversary("Orc", testExpansion, true, true, regularUser, AdversaryType.STANDARD, 2);
        createAdversary("Orc Chief", testExpansion, true, true, regularUser, AdversaryType.LEADER, 2);
        createAdversary("Troll", testExpansion, true, true, regularUser, AdversaryType.BRUISER, 3);
        createAdversary("Dragon", testExpansion, true, true, regularUser, AdversaryType.SOLO, 4);

        // Act & Assert
        mockMvc.perform(get("/api/dh/adversaries")
                        .param("tier", "1", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[*].tier", everyItem(anyOf(is(1), is(2)))));
    }

    @Test
    void getAllAdversaries_FilterByMultipleTiers_PaginatesCorrectlyAtProductionScale() throws Exception {
        // Arrange - mirrors the real local DB's tier spread order of magnitude (dozens per
        // tier) closely enough to catch a pagination bug that only surfaces once a tier filter
        // matches more rows than fit on one page (default page size 20)
        for (int i = 0; i < 15; i++) {
            createAdversary("Tier1-" + i, testExpansion, true, true, regularUser, AdversaryType.MINION, 1);
        }
        for (int i = 0; i < 12; i++) {
            createAdversary("Tier2-" + i, testExpansion, true, true, regularUser, AdversaryType.STANDARD, 2);
        }
        createAdversary("Tier3-excluded", testExpansion, true, true, regularUser, AdversaryType.BRUISER, 3);

        // Act & Assert - 27 total across tiers 1 and 2, default page size 20 -> 2 pages
        mockMvc.perform(get("/api/dh/adversaries")
                        .param("tier", "1", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(27))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/dh/adversaries")
                        .param("tier", "1", "2")
                        .param("page", "1")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7));
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

    // ==================== BULK CREATE TESTS ====================

    @Test
    void createAdversariesBulk_AsModerator_Returns201() throws Exception {
        // Arrange - realistic full-shape payload matching the real bulk-import JSON
        // (hope_and_fear-import/json/10-adversaries.json's "Bugboar"/"Atototl" entries),
        // including a nested "features" array with real multi-feature cardinality
        // (Young Ice Dragon/Adult Flickerfly carry 7 features in production).
        // Deliberately omits hitPointMax/stressMax/isPublic on the second adversary so the
        // response assertions below can verify AdversaryService's defensive null-coalescing
        // (hitPointMax/stressMax -> 0, isPublic -> false) actually fires on the real bulk-JSON
        // deserialization path, not just via the Lombok builder's @Builder.Default.
        String bulkRequest = """
            [
                {
                    "name": "Bugboar",
                    "tier": 1,
                    "adversaryType": "BRUISER",
                    "description": "A large bipedal creature that has a tusked snout and coarse fur. | Experience: Traps +3",
                    "motivesAndTactics": "Ambush, bully, seek carnage and shiny things",
                    "difficulty": 13,
                    "majorThreshold": 8,
                    "severeThreshold": 15,
                    "hitPointMax": 5,
                    "stressMax": 3,
                    "attackModifier": 1,
                    "weaponName": "Tusks",
                    "attackRange": "MELEE",
                    "damage": { "diceCount": 1, "diceType": "D8", "modifier": 1, "damageType": "PHYSICAL" },
                    "isPublic": true,
                    "expansionId": %d,
                    "features": [
                        { "name": "Surprise! - Passive [Bugboar]", "description": "The first time the Bugboar attacks a target, it deals extra damage equal to its tier.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Tusked Charge - Action [Bugboar]", "description": "Mark a Stress to move up to Far range and make an attack against a target in melee range.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Bristling Hide - Passive [Bugboar]", "description": "The Bugboar's coarse fur grants it resistance to physical damage.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Territorial - Passive [Bugboar]", "description": "The Bugboar gains advantage on attacks against creatures within its territory.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Frenzy - Reaction [Bugboar]", "description": "When the Bugboar is damaged, it can mark a Stress to immediately attack the source.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Relentless (3) - Passive [Bugboar]", "description": "The Bugboar can be spotlighted up to three times per GM turn.", "featureType": "OTHER", "expansionId": %d }
                    ]
                },
                {
                    "name": "Atototl",
                    "tier": 1,
                    "adversaryType": "STANDARD",
                    "description": "A majestic green water bird that has a ten-foot wingspan and is hunted for the fortune-telling stone inside their stomach. | Experience: Flight +3, Jungles +3",
                    "motivesAndTactics": "Avoid, escape, misdirect",
                    "difficulty": 12,
                    "majorThreshold": 8,
                    "severeThreshold": 12,
                    "attackModifier": 1,
                    "weaponName": "Talons",
                    "attackRange": "MELEE",
                    "damage": { "diceCount": 1, "diceType": "D8", "modifier": 1, "damageType": "PHYSICAL" },
                    "expansionId": %d,
                    "features": [
                        { "name": "Wind Lord - Passive [Atototl]", "description": "While the Atototl is flying, attacks against it is made with disadvantage.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Stone of Omens - Passive [Atototl]", "description": "Inside the Atototl is a stone that foretells good or ill fortune.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Archer's Bane - Reaction [Atototl]", "description": "When a creature beyond Melee range targets the Atototl with an attack, it can move to Melee range of that creature.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Evasive Flight - Passive [Atototl]", "description": "The Atototl is difficult to pin down while airborne.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Shrieking Cry - Action [Atototl]", "description": "The Atototl lets out a piercing cry that can be heard for miles.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Nimble Retreat - Reaction [Atototl]", "description": "When targeted by an attack, the Atototl can mark a Stress to move away from the attacker.", "featureType": "OTHER", "expansionId": %d },
                        { "name": "Fortune's Guardian - Passive [Atototl]", "description": "Creatures that harm the Atototl gain Fear equal to half the damage dealt, rounded down.", "featureType": "OTHER", "expansionId": %d }
                    ]
                }
            ]
            """.formatted(
                    testExpansion.getId(), testExpansion.getId(), testExpansion.getId(), testExpansion.getId(),
                    testExpansion.getId(), testExpansion.getId(), testExpansion.getId(),
                    testExpansion.getId(), testExpansion.getId(), testExpansion.getId(), testExpansion.getId(),
                    testExpansion.getId(), testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Bugboar"))
                .andExpect(jsonPath("$[0].hitPointMax").value(5))
                .andExpect(jsonPath("$[0].stressMax").value(3))
                .andExpect(jsonPath("$[0].isPublic").value(true))
                .andExpect(jsonPath("$[0].featureIds").isArray())
                .andExpect(jsonPath("$[0].featureIds.length()").value(6))
                .andExpect(jsonPath("$[1].name").value("Atototl"))
                // hitPointMax/stressMax/isPublic are deliberately omitted from Atototl's JSON above;
                // this asserts AdversaryService's null-coalescing defaults actually fire on the real
                // deserialization path (not just via the builder's @Builder.Default, which Jackson bypasses)
                .andExpect(jsonPath("$[1].hitPointMax").value(0))
                .andExpect(jsonPath("$[1].stressMax").value(0))
                .andExpect(jsonPath("$[1].isPublic").value(false))
                .andExpect(jsonPath("$[1].featureIds").isArray())
                .andExpect(jsonPath("$[1].featureIds.length()").value(7));

        assertThat(adversaryRepository.findAll()).hasSize(2);
        assertThat(featureRepository.findAll()).hasSize(13);
    }

    @Test
    void createAdversariesBulk_WithRawJsonIsOfficial_PersistsOfficialFlag() throws Exception {
        // Arrange - raw JSON string (not builder+serialize) matching the real bulk-import
        // payload shape in hope_and_fear-import/json/10-adversaries.json. The first entry sends
        // "isOfficial": true (official content import); the second omits it entirely.
        // Builder-based tests can't catch a missing isOfficial field on CreateAdversaryRequest,
        // because the builder simply wouldn't compile - only a real client's JSON exercises the
        // Jackson deserialization path where an unmapped property is silently dropped.
        String bulkRequest = """
            [
                {
                    "name": "Bugboar",
                    "tier": 1,
                    "adversaryType": "BRUISER",
                    "difficulty": 13,
                    "majorThreshold": 8,
                    "severeThreshold": 15,
                    "isOfficial": true,
                    "expansionId": %d
                },
                {
                    "name": "Atototl",
                    "tier": 1,
                    "adversaryType": "STANDARD",
                    "difficulty": 12,
                    "majorThreshold": 8,
                    "severeThreshold": 12,
                    "expansionId": %d
                }
            ]
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Bugboar"))
                .andExpect(jsonPath("$[0].isOfficial").value(true))
                // isOfficial is deliberately omitted from Atototl's JSON above, so this asserts
                // AdversaryService's null-coalescing default fires on the real deserialization path
                .andExpect(jsonPath("$[1].name").value("Atototl"))
                .andExpect(jsonPath("$[1].isOfficial").value(false));

        assertThat(adversaryRepository.findAll())
                .filteredOn(adversary -> "Bugboar".equals(adversary.getName()))
                .singleElement()
                .satisfies(adversary -> assertThat(adversary.getIsOfficial()).isTrue());
    }

    @Test
    void updateAdversary_WithRawJsonIsOfficial_PersistsOfficialFlag() throws Exception {
        // Arrange - raw JSON for the same reason as the bulk create test above: only real client
        // JSON proves UpdateAdversaryRequest actually maps the isOfficial property.
        Adversary adversary = createAdversary("Goblin", testExpansion, false, true, ownerUser, AdversaryType.MINION, 1);
        String requestJson = """
            {
                "isOfficial": true
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(true));

        assertThat(adversaryRepository.findById(adversary.getId()))
                .get()
                .satisfies(updated -> assertThat(updated.getIsOfficial()).isTrue());
    }

    @Test
    void createAdversary_AsUserRequestingIsOfficial_PersistsNonOfficial() throws Exception {
        // Arrange - security regression test: single create has no role gate, so an ordinary
        // homebrew author must not be able to mint content that presents as official app-wide
        String requestJson = """
            {
                "name": "Fake Official Goblin",
                "tier": 1,
                "adversaryType": "MINION",
                "difficulty": 5,
                "majorThreshold": 3,
                "severeThreshold": 6,
                "isOfficial": true,
                "expansionId": %d
            }
            """.formatted(testExpansion.getId());

        // Act & Assert - the request succeeds, but the official flag is silently coerced to false
        mockMvc.perform(post("/api/dh/adversaries")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOfficial").value(false));

        assertThat(adversaryRepository.findAll())
                .singleElement()
                .satisfies(adversary -> assertThat(adversary.getIsOfficial()).isFalse());
    }

    @Test
    void updateAdversary_AsCreatorRequestingIsOfficial_PersistsNonOfficial() throws Exception {
        // Arrange - security regression test: owning an adversary must not let a non-moderator
        // escalate it to official content
        Adversary adversary = createAdversary("Homebrew Goblin", testExpansion, false, false, regularUser,
                AdversaryType.MINION, 1);
        String requestJson = """
            {
                "isOfficial": true
            }
            """;

        // Act & Assert
        mockMvc.perform(put("/api/dh/adversaries/{id}", adversary.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(false));

        assertThat(adversaryRepository.findById(adversary.getId()))
                .get()
                .satisfies(updated -> assertThat(updated.getIsOfficial()).isFalse());
    }

    @Test
    void createAdversariesBulk_AsUser_Returns403() throws Exception {
        // Arrange
        String bulkRequest = """
            [
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
            """.formatted(testExpansion.getId());

        // Act & Assert - regular user cannot bulk create
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
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
    void createAdversariesBulk_WithInlineFeatures_Returns201() throws Exception {
        // Arrange
        String bulkRequest = """
            [
                {
                    "name": "Bulk Goblin 1",
                    "tier": 1,
                    "adversaryType": "MINION",
                    "difficulty": 5,
                    "majorThreshold": 3,
                    "severeThreshold": 6,
                    "expansionId": %d,
                    "features": [
                        {
                            "name": "Bulk Feature",
                            "description": "Feature from bulk",
                            "featureType": "OTHER",
                            "expansionId": %d
                        }
                    ]
                },
                {
                    "name": "Bulk Goblin 2",
                    "tier": 1,
                    "adversaryType": "MINION",
                    "difficulty": 5,
                    "majorThreshold": 3,
                    "severeThreshold": 6,
                    "expansionId": %d
                }
            ]
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/adversaries/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

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
