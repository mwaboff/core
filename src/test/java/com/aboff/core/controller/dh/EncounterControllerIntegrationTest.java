package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateEncounterRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.EncounterRunStatus;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EncounterRunRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for EncounterController.
 * <p>
 * EncounterController had no integration test coverage before this Battle Point / encounter
 * model work. Focuses on what changed: party size and the six Battle Point adjustment flags,
 * the richer per-instance adversary entries (label / tierOverride) alongside the deprecated
 * bare {@code adversaryIds} list, and the optional environment relation -- exercised against a
 * real Postgres so the new CHECK constraints and FK are actually validated, not just mocked.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EncounterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private AdversaryRepository adversaryRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private EncounterRunRepository encounterRunRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private String userToken;
    private String otherUserToken;
    private String adminToken;
    private Expansion testExpansion;

    @BeforeEach
    void setUp() {
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);
        otherUser = createUserWithRole("other", "other@example.com", Role.USER);
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);

        userToken = jwtTokenProvider.generateToken(regularUser);
        otherUserToken = jwtTokenProvider.generateToken(otherUser);
        adminToken = jwtTokenProvider.generateToken(adminUser);

        storeTokenInDatabase(regularUser.getId(), userToken);
        storeTokenInDatabase(otherUser.getId(), otherUserToken);
        storeTokenInDatabase(adminUser.getId(), adminToken);

        testExpansion = expansionRepository.save(Expansion.builder().name("Core Rulebook").isPublished(true).build());
    }

    // ==================== CREATE: BATTLE POINTS ====================

    @Test
    void createEncounter_WithPartySizeAndAdjustments_ComputesSuggestedBudget() throws Exception {
        // Arrange - the rulebook's worked example: party of 4 -> budget 14
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Boss Fight")
                .partySize(4)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partySize").value(4))
                .andExpect(jsonPath("$.suggestedBattlePoints").value(14))
                .andExpect(jsonPath("$.spentBattlePoints").value(0));
    }

    @Test
    void createEncounter_EightMinionsPartyFour_SpentIsTwoPoints() throws Exception {
        // Arrange - manual QA script's example: 8 Minions, party 4 -> 2 points spent, not 8
        Adversary minion = createAdversary("Goblin", AdversaryType.MINION, 1);
        List<CreateEncounterRequest.AdversaryEntry> entries = java.util.Collections.nCopies(8,
                CreateEncounterRequest.AdversaryEntry.builder().adversaryId(minion.getId()).build());

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Goblin Swarm")
                .partySize(4)
                .adversaries(entries)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spentBattlePoints").value(2))
                .andExpect(jsonPath("$.adversaries.length()").value(8));
    }

    @Test
    void createEncounter_HarderThenTwoPlusSolosAdjustments_NetsBackToBaseBudget() throws Exception {
        // Arrange - manual QA script: toggle "more dangerous" -> 16, then "2+ Solos" -> 14
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Adjusted Fight")
                .partySize(4)
                .adjustmentHarder(true)
                .adjustmentTwoPlusSolos(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentHarder").value(true))
                .andExpect(jsonPath("$.adjustmentTwoPlusSolos").value(true))
                .andExpect(jsonPath("$.suggestedBattlePoints").value(14));
    }

    @Test
    void createEncounter_PartySizeOutOfRange_Returns400() throws Exception {
        // Arrange - CHECK (party_size BETWEEN 1 AND 12), also enforced at the request layer
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Too Big")
                .partySize(13)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== CREATE: RICH ADVERSARY ENTRIES + BACKWARD COMPAT ====================

    @Test
    void createEncounter_WithLabelAndTierOverride_PersistsAndReturnsRetieredStatistics() throws Exception {
        // Arrange - a Tier 1 Standard retiered to Tier 3
        Adversary standard = createAdversary("Bandit", AdversaryType.STANDARD, 1);
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Retiered Fight")
                .adversaries(List.of(CreateEncounterRequest.AdversaryEntry.builder()
                        .adversaryId(standard.getId())
                        .label("Elite Bandit")
                        .tierOverride(3)
                        .build()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adversaries[0].label").value("Elite Bandit"))
                .andExpect(jsonPath("$.adversaries[0].tierOverride").value(3))
                .andExpect(jsonPath("$.adversaries[0].retieredStatistics.difficulty").value(17))
                .andExpect(jsonPath("$.adversaries[0].retieredStatistics.majorThreshold").value(20))
                .andExpect(jsonPath("$.adversaries[0].retieredStatistics.severeThreshold").value(32))
                .andExpect(jsonPath("$.adversaries[0].retieredStatistics.attackModifier").value(3));
    }

    @Test
    void createEncounter_WithLegacyAdversaryIds_StillWorks() throws Exception {
        // Arrange - backward compatibility with the deprecated bare adversaryIds list
        Adversary goblin = createAdversary("Goblin", AdversaryType.MINION, 1);
        String requestJson = """
            {
                "name": "Legacy Ambush",
                "adversaryIds": [%d, %d]
            }
            """.formatted(goblin.getId(), goblin.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adversaries.length()").value(2));
    }

    // ==================== CREATE: ENVIRONMENT ====================

    @Test
    void createEncounter_WithEnvironmentId_SetsEnvironmentAndSupportsExpand() throws Exception {
        // Arrange
        Environment environment = createEnvironment("Collapsing Bridge");
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Bridge Fight")
                .environmentId(environment.getId())
                .build();

        // Act
        String responseJson = mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.environmentId").value(environment.getId()))
                .andReturn().getResponse().getContentAsString();

        Long encounterId = objectMapper.readTree(responseJson).get("id").asLong();

        // Assert - expand=environment includes the full object
        mockMvc.perform(get("/api/dh/encounters/{id}", encounterId)
                        .param("expand", "environment")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment.name").value("Collapsing Bridge"));
    }

    @Test
    void createEncounter_WithInvalidEnvironmentId_Returns404() throws Exception {
        // Arrange
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Bad Environment")
                .environmentId(99999L)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET ALL: creatorId FILTER ====================

    @Test
    void getAllEncounters_NoCreatorIdFilter_ReturnsOwnPlusPublicPlusOfficial() throws Exception {
        // Arrange - unchanged existing behaviour: own private, someone else's public, and official
        // all show up when no creatorId is supplied
        createEncounter("Mine", regularUser, false, false);
        createEncounter("Their Public One", otherUser, false, true);
        createEncounter("Their Private One", otherUser, false, false);
        createEncounter("Official", adminUser, true, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.containsInAnyOrder(
                        "Mine", "Their Public One", "Official")));
    }

    @Test
    void getAllEncounters_CreatorIdIsCaller_ReturnsOnlyOwnEncounters() throws Exception {
        // Arrange
        Encounter mine = createEncounter("Mine", regularUser, false, false);
        createEncounter("Their Public One", otherUser, false, true);
        createEncounter("Official", adminUser, true, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", regularUser.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mine.getId()));
    }

    @Test
    void getAllEncounters_CreatorIdIsAnotherUser_OnlyReturnsThatUsersPublicAndOfficialEncounters() throws Exception {
        // Arrange - this pins the load-bearing behaviour: filtering to another user's ID must
        // never leak their private encounters, only whatever was already visible under the
        // official/public/own rules
        createEncounter("Their Public One", otherUser, false, true);
        Encounter theirPrivate = createEncounter("Their Private One", otherUser, false, false);
        createEncounter("Mine", regularUser, false, false);

        // Act & Assert - regularUser filters to otherUser's ID
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", otherUser.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Their Public One"));

        // Sanity check the private one really exists and is otherUser's, so the omission above
        // is the visibility rule at work and not just an empty fixture
        assertThat(encounterRepository.findByIdAndDeletedAtIsNull(theirPrivate.getId())).isPresent();
    }

    @Test
    void getAllEncounters_CreatorIdOwnerFilteringToOwnPrivate_SeesItViaTheOtherToken() throws Exception {
        // Arrange - otherUser filtering to their own ID (as the authenticated caller) sees their
        // own private encounter, confirming the narrowing composes with "own" visibility too
        Encounter theirPrivate = createEncounter("Their Private One", otherUser, false, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", otherUser.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", otherUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(theirPrivate.getId()));
    }

    @Test
    void getAllEncounters_CreatorIdIsNonexistentUser_ReturnsEmptyPage() throws Exception {
        // Arrange
        createEncounter("Mine", regularUser, false, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", "999999")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getAllEncounters_CreatorIdWithPaging_AppliesBothTogether() throws Exception {
        // Arrange - three of the caller's own encounters, paged at size 2
        createEncounter("Alpha", regularUser, false, false);
        createEncounter("Bravo", regularUser, false, false);
        createEncounter("Charlie", regularUser, false, false);

        // Act & Assert - page 0
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", regularUser.getId().toString())
                        .param("page", "0")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));

        // Act & Assert - page 1 has the remainder
        mockMvc.perform(get("/api/dh/encounters")
                        .param("creatorId", regularUser.getId().toString())
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllEncounters_AdminIncludeDeletedWithCreatorId_ReturnsThatCreatorsSoftDeletedEncounters() throws Exception {
        // Arrange - a soft-deleted encounter for otherUser, plus an undeleted one for regularUser
        // as a distractor that must not show up in the creatorId-scoped result
        Encounter deleted = createEncounter("Their Deleted Fight", otherUser, false, false);
        deleted.softDelete();
        encounterRepository.save(deleted);
        createEncounter("Mine, Not Deleted", regularUser, false, false);

        // Act & Assert - admin, includeDeleted=true, creatorId=otherUser routes through the
        // admin-only findAllWithFilters query and surfaces the soft-deleted row
        mockMvc.perform(get("/api/dh/encounters")
                        .param("includeDeleted", "true")
                        .param("creatorId", otherUser.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(deleted.getId()))
                .andExpect(jsonPath("$.content[0].deletedAt").isNotEmpty());
    }

    @Test
    void getAllEncounters_NonAdminIncludeDeletedWithCreatorId_IgnoresIncludeDeletedAndStaysVisibilityScoped() throws Exception {
        // Arrange - same soft-deleted encounter, but the caller lacks ADMIN+, so includeDeleted
        // must be ignored and the request must fall through to the visibility-scoped query
        // (findAccessibleWithFilters), which excludes soft-deleted rows regardless of creatorId
        Encounter deleted = createEncounter("Their Deleted Fight", otherUser, false, false);
        deleted.softDelete();
        encounterRepository.save(deleted);

        // Act & Assert - regularUser, includeDeleted=true, creatorId=otherUser
        mockMvc.perform(get("/api/dh/encounters")
                        .param("includeDeleted", "true")
                        .param("creatorId", otherUser.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ==================== UPDATE ====================

    @Test
    void updateEncounter_PartialUpdate_OnlyChangesProvidedFields() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("Original Name", regularUser, false, false);
        UpdateEncounterRequest request = UpdateEncounterRequest.builder().partySize(6).build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original Name"))
                .andExpect(jsonPath("$.partySize").value(6));
    }

    @Test
    void updateEncounter_NonCreatorNonModerator_Returns403() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("Someone Else's", otherUser, false, false);
        UpdateEncounterRequest request = UpdateEncounterRequest.builder().name("Hacked").build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== GET: MULTI-TIER PARTY & VISIBILITY ====================

    @Test
    void getEncounterById_PrivateAsNonCreator_Returns404() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("Secret Plan", otherUser, false, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEncounterById_NoPartySizeOrAdversaries_ReturnsBaseBudgetAndZeroSpent() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("Empty Encounter", regularUser, false, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentBattlePoints").value(0))
                .andExpect(jsonPath("$.suggestedBattlePoints").value(2));
    }

    // ==================== COPY ====================

    @Test
    void copyEncounter_CarriesOverPartySizeAdjustmentsEnvironmentAndInstanceFields() throws Exception {
        // Arrange - this is the bug the design exists to fix: copying an encounter used to
        // silently drop per-instance data. Build an original with every carry-over field set.
        Environment environment = createEnvironment("Collapsing Bridge");
        Adversary standard = createAdversary("Bandit", AdversaryType.STANDARD, 1);

        CreateEncounterRequest createRequest = CreateEncounterRequest.builder()
                .name("Original Fight")
                .partySize(4)
                .adjustmentHarder(true)
                .environmentId(environment.getId())
                .adversaries(List.of(CreateEncounterRequest.AdversaryEntry.builder()
                        .adversaryId(standard.getId())
                        .label("Elite Bandit")
                        .tierOverride(3)
                        .build()))
                .build();

        String createResponseJson = mockMvc.perform(post("/api/dh/encounters")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long originalId = objectMapper.readTree(createResponseJson).get("id").asLong();

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters/{id}/copy", originalId)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Original Fight (Copy)"))
                .andExpect(jsonPath("$.originalEncounterId").value(originalId))
                .andExpect(jsonPath("$.partySize").value(4))
                .andExpect(jsonPath("$.adjustmentHarder").value(true))
                .andExpect(jsonPath("$.environmentId").value(environment.getId()))
                .andExpect(jsonPath("$.adversaries.length()").value(1))
                .andExpect(jsonPath("$.adversaries[0].label").value("Elite Bandit"))
                .andExpect(jsonPath("$.adversaries[0].tierOverride").value(3))
                .andExpect(jsonPath("$.adversaries[0].displayOrder").value(0));
    }

    @Test
    void copyEncounter_OfPublicEncounter_IsPrivateAndUnofficialForCopier() throws Exception {
        // Arrange - copying someone else's public encounter should not carry over official
        // status, and the copy should be private to the copying user by default
        Encounter original = createEncounter("Shared Plan", otherUser, false, true);

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters/{id}/copy", original.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.isOfficial").value(false))
                .andExpect(jsonPath("$.creatorId").value(regularUser.getId()));
    }

    @Test
    void copyEncounter_PrivateAsNonCreator_Returns404() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("Secret Plan", otherUser, false, false);

        // Act & Assert
        mockMvc.perform(post("/api/dh/encounters/{id}/copy", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ====================

    @Test
    void deleteEncounter_AsCreator_Returns204() throws Exception {
        // Arrange
        Encounter encounter = createEncounter("To Delete", regularUser, false, false);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNoContent());

        Encounter deleted = encounterRepository.findById(encounter.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteEncounter_WithAnActiveRun_AlsoDiscardsTheRun() throws Exception {
        // Arrange -- a live fight must not survive as an orphan once its encounter is gone.
        Encounter encounter = createEncounter("Ambush With A Live Run", regularUser, false, false);
        EncounterRun run = EncounterRun.builder()
                .encounter(encounter)
                .startedBy(regularUser)
                .status(EncounterRunStatus.ACTIVE)
                .build();
        run = encounterRunRepository.save(run);

        // Act
        mockMvc.perform(delete("/api/dh/encounters/{id}", encounter.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNoContent());

        // Assert
        assertThat(encounterRunRepository.findById(run.getId())).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder().username(username).email(email).role(role).build();
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

    private Adversary createAdversary(String name, AdversaryType type, int tier) {
        Adversary adversary = Adversary.builder()
                .name(name)
                .expansion(testExpansion)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(adminUser)
                .adversaryType(type)
                .tier(tier)
                .difficulty(10)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(5)
                .stressMarked(0)
                .build();
        return adversaryRepository.save(adversary);
    }

    private Environment createEnvironment(String name) {
        Environment environment = Environment.builder()
                .name(name)
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .difficulty(11)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(adminUser)
                .expansion(testExpansion)
                .build();
        return environmentRepository.save(environment);
    }

    private Encounter createEncounter(String name, User creator, boolean isOfficial, boolean isPublic) {
        Encounter encounter = Encounter.builder()
                .name(name)
                .createdBy(creator)
                .isOfficial(isOfficial)
                .isPublic(isPublic)
                .build();
        return encounterRepository.save(encounter);
    }
}
