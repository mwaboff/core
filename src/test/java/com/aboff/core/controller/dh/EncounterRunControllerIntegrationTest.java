package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateEncounterRunRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRunAdversaryRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.EncounterRunStatus;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.EncounterAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EncounterRunAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRunRepository;
import com.aboff.core.repository.dh.EnvironmentRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code /api/dh/encounters/{id}/runs} and {@code /api/dh/encounter-runs}.
 * <p>
 * Exercises the full authorization matrix (owner / campaign GM / unrelated user / MODERATOR+,
 * for both a standalone and a campaign-tagged run) against a real Postgres, so the new CHECK
 * constraints and FKs are actually validated, not just mocked. Also proves the two properties
 * the whole feature exists for: a user with <strong>no campaign membership at all</strong> can
 * start and play a standalone run, and editing the source encounter mid-run cannot corrupt a
 * run already in progress.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EncounterRunControllerIntegrationTest {

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
    private EncounterAdversaryRepository encounterAdversaryRepository;

    @Autowired
    private EncounterRunRepository encounterRunRepository;

    @Autowired
    private EncounterRunAdversaryRepository encounterRunAdversaryRepository;

    @Autowired
    private AdversaryRepository adversaryRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User creator;
    private User gm;
    private User player;
    private User outsider;
    private User moderator;
    private String creatorToken;
    private String gmToken;
    private String playerToken;
    private String outsiderToken;
    private String moderatorToken;
    private Expansion testExpansion;
    private Campaign testCampaign;
    private Encounter testEncounter;
    private Adversary testAdversary;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("run-creator", "run-creator@example.com", Role.USER);
        gm = createUserWithRole("run-gm", "run-gm@example.com", Role.USER);
        player = createUserWithRole("run-player", "run-player@example.com", Role.USER);
        outsider = createUserWithRole("run-outsider", "run-outsider@example.com", Role.USER);
        moderator = createUserWithRole("run-moderator", "run-moderator@example.com", Role.MODERATOR);

        creatorToken = tokenFor(creator);
        gmToken = tokenFor(gm);
        playerToken = tokenFor(player);
        outsiderToken = tokenFor(outsider);
        moderatorToken = tokenFor(moderator);

        testExpansion = expansionRepository.save(Expansion.builder().name("Core Rulebook").isPublished(true).build());

        testCampaign = campaignRepository.save(Campaign.builder()
                .name("Encounter Run Campaign")
                .creator(creator)
                .gameMasters(new HashSet<>())
                .players(new HashSet<>())
                .pendingCharacterSheets(new HashSet<>())
                .playerCharacters(new HashSet<>())
                .nonPlayerCharacters(new HashSet<>())
                .build());
        testCampaign.getGameMasters().add(gm);
        testCampaign.getPlayers().add(player);
        campaignRepository.save(testCampaign);

        testAdversary = adversaryRepository.save(Adversary.builder()
                .name("Goblin Scout")
                .expansion(testExpansion)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(creator)
                .adversaryType(AdversaryType.STANDARD)
                .tier(1)
                .difficulty(11)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(6)
                .stressMax(3)
                .build());

        testEncounter = saveEncounterWithAdversary("Goblin Ambush", creator, testAdversary, "Archer A");
    }

    /**
     * Builds and saves an encounter with a single adversary instance, populating the parent's
     * in-memory collection before saving rather than saving the child through a separate
     * repository call. Within a {@code @Transactional} test, MockMvc requests share the test
     * method's persistence context, so an entity already managed there (like a field built in
     * {@code setUp}) is returned as-is from a later {@code findBy...} rather than re-queried --
     * populating the collection up front avoids that staleness entirely.
     */
    private Encounter saveEncounterWithAdversary(String name, User creator, Adversary adversary, String label) {
        EncounterAdversary instance = EncounterAdversary.builder()
                .adversary(adversary)
                .label(label)
                .displayOrder(0)
                .build();

        Encounter encounter = Encounter.builder()
                .name(name)
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(false)
                .encounterAdversaries(new java.util.ArrayList<>(java.util.List.of(instance)))
                .build();
        instance.setEncounter(encounter);

        return encounterRepository.save(encounter);
    }

    private Feature createFeature(String name) {
        return featureRepository.save(Feature.builder()
                .name(name)
                .description("Test feature description")
                .featureType(FeatureType.ADVERSARY)
                .expansion(testExpansion)
                .build());
    }

    private Experience createExperience(String description, int modifier) {
        return experienceRepository.save(Experience.builder()
                .description(description)
                .modifier(modifier)
                .createdBy(creator)
                .build());
    }

    private Environment createEnvironment(String name) {
        return environmentRepository.save(Environment.builder()
                .name(name)
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .difficulty(11)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(creator)
                .expansion(testExpansion)
                .build());
    }

    // ==================== START RUN ====================

    @Test
    void startRun_AsEncounterCreator_Returns201AndPersists() throws Exception {
        mockMvc.perform(startRun(testEncounter.getId(), null, creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.adversaries.length()").value(1))
                .andExpect(jsonPath("$.adversaries[0].label").value("Archer A"))
                .andExpect(jsonPath("$.adversaries[0].hitPointsMarked").value(0));

        assertThat(encounterRunRepository.findAll()).hasSize(1);
    }

    @Test
    void startRun_NoCampaignId_StartsStandaloneRun() throws Exception {
        mockMvc.perform(startRun(testEncounter.getId(), null, creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").doesNotExist());
    }

    @Test
    void startRun_WithCampaignId_TagsRun() throws Exception {
        mockMvc.perform(startRun(testEncounter.getId(), testCampaign.getId(), creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").value(testCampaign.getId()));
    }

    @Test
    void startRun_UserWithNoCampaignMembershipAtAll_CanStartAndPlayAStandaloneRun() throws Exception {
        // The regression this whole design exists to prevent: no campaign, no GM role, still works.
        User loneUser = createUserWithRole("run-lone-wolf", "run-lone-wolf@example.com", Role.USER);
        String loneToken = tokenFor(loneUser);

        Encounter loneEncounter = saveEncounterWithAdversary("Solo Bandit Fight", loneUser, testAdversary, null);

        String runId = mockMvc.perform(startRun(loneEncounter.getId(), null, loneToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(runId).get("id").asLong();

        mockMvc.perform(patchAdversary(id, findInstanceId(id), 3, null, null, loneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].hitPointsMarked").value(3));
    }

    @Test
    void startRun_PrivateEncounterNotVisibleToCaller_Returns404() throws Exception {
        mockMvc.perform(startRun(testEncounter.getId(), null, outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void startRun_UnknownEncounter_Returns404() throws Exception {
        mockMvc.perform(startRun(999999L, null, creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void startRun_UnknownCampaign_Returns404() throws Exception {
        mockMvc.perform(startRun(testEncounter.getId(), 999999L, creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void startRun_TaggedToEndedCampaign_Returns400() throws Exception {
        testCampaign.endCampaign();
        campaignRepository.save(testCampaign);

        mockMvc.perform(startRun(testEncounter.getId(), testCampaign.getId(), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startRun_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/dh/encounters/{id}/runs", testEncounter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startRun_ThenEditingSourceEncounter_LeavesTheRunsSnapshotUnchanged() throws Exception {
        String body = mockMvc.perform(startRun(testEncounter.getId(), null, creatorToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long runId = objectMapper.readTree(body).get("id").asLong();

        // Edit the source encounter's adversary instance after the run has started.
        EncounterAdversary template = encounterAdversaryRepository.findByEncounterId(testEncounter.getId()).get(0);
        template.setLabel("Renamed After Run Started");
        encounterAdversaryRepository.save(template);

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].label").value("Archer A"));
    }

    // ==================== GET RUN — AUTHORIZATION MATRIX ====================

    @Test
    void getRun_AsOwner_Standalone_Returns200() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRun_AsUnrelatedUser_Standalone_Returns403() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRun_AsModerator_Standalone_Returns200() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRun_AsOwner_CampaignTagged_Returns200() throws Exception {
        Long runId = startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRun_AsCampaignGameMaster_CampaignTagged_Returns200() throws Exception {
        Long runId = startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", gmToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRun_AsPlayer_CampaignTagged_Returns403() throws Exception {
        // Only GM-level access counts -- read-only sharing with players is explicitly out of
        // scope for this phase.
        Long runId = startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", playerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRun_AsUnrelatedUser_CampaignTagged_Returns403() throws Exception {
        Long runId = startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRun_AsModerator_CampaignTagged_Returns200() throws Exception {
        Long runId = startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void getRun_UnknownRun_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", 999999L)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRun_ExpandsAdversaryStatBlock() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].adversary.name").value("Goblin Scout"));
    }

    @Test
    void getRun_AdversaryWithNoFeaturesOrExperiences_OmitsThemFromStatBlock() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].adversary.features").doesNotExist())
                .andExpect(jsonPath("$.adversaries[0].adversary.featureIds").doesNotExist())
                .andExpect(jsonPath("$.adversaries[0].adversary.experiences").doesNotExist())
                .andExpect(jsonPath("$.adversaries[0].adversary.experienceIds").doesNotExist());
    }

    @Test
    void getRun_IncludesAdversaryFeaturesAndExperiences_Returns200() throws Exception {
        // The whole reason this endpoint expands features/experiences: without them the GM has
        // thresholds and a weapon line but none of the abilities that let the adversary play
        // differently in a fight.
        Feature feature = createFeature("Relentless (3) - Passive");
        Experience experience = createExperience("Combat Expert", 2);
        testAdversary.setFeatures(new HashSet<>(Set.of(feature)));
        testAdversary.setExperiences(new HashSet<>(Set.of(experience)));
        adversaryRepository.save(testAdversary);

        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].adversary.features[0].name").value("Relentless (3) - Passive"))
                .andExpect(jsonPath("$.adversaries[0].adversary.featureIds[0]").value(feature.getId()))
                .andExpect(jsonPath("$.adversaries[0].adversary.experiences[0].description").value("Combat Expert"))
                .andExpect(jsonPath("$.adversaries[0].adversary.experienceIds[0]").value(experience.getId()));
    }

    @Test
    void getRun_EncounterWithoutEnvironment_EnvironmentIdDoesNotExist() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environmentId").doesNotExist());
    }

    @Test
    void getRun_EncounterWithEnvironment_ReturnsEnvironmentId() throws Exception {
        Environment environment = createEnvironment("Sunken Ruins");
        testEncounter.setEnvironment(environment);
        encounterRepository.save(testEncounter);

        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environmentId").value(environment.getId()));
    }

    // ==================== LIST ====================

    @Test
    void listRuns_NoCampaignId_ReturnsCallersOwnRuns() throws Exception {
        startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].adversaries[0].adversary").doesNotExist());
    }

    @Test
    void listRuns_IncludesEnvironmentIdEvenThoughAdversariesStayUnexpanded() throws Exception {
        // environmentId is a cheap scalar (eagerly joined by the repository, not a per-instance
        // lookup), so unlike the adversary stat block it stays on the list endpoint too.
        Environment environment = createEnvironment("Sunken Ruins");
        testEncounter.setEnvironment(environment);
        encounterRepository.save(testEncounter);
        startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].environmentId").value(environment.getId()));
    }

    @Test
    void listRuns_WithCampaignId_AsGameMaster_Returns200() throws Exception {
        startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", gmToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listRuns_WithCampaignId_AsUnrelatedUser_Returns403() throws Exception {
        startCampaignRun();

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRuns_StatusFilter_ExcludesNonMatchingRuns() throws Exception {
        Long runId = startStandaloneRun();
        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .param("status", "ACTIVE")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listRuns_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/encounter-runs"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== UPDATE ADVERSARY ====================

    @Test
    void updateRunAdversary_AsOwner_Returns200AndPersists() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversary(runId, instanceId, 4, 2, true, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].hitPointsMarked").value(4))
                .andExpect(jsonPath("$.adversaries[0].stressMarked").value(2))
                .andExpect(jsonPath("$.adversaries[0].isDefeated").value(true));

        assertThat(encounterRunAdversaryRepository.findById(instanceId).orElseThrow().getHitPointsMarked())
                .isEqualTo(4);
    }

    @Test
    void updateRunAdversary_ClampsHitPointsMarkedToAdversaryMax() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversary(runId, instanceId, 99, null, null, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].hitPointsMarked").value(6)); // testAdversary hitPointMax = 6
    }

    @Test
    void updateRunAdversary_AsUnrelatedUser_Returns403() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversary(runId, instanceId, 4, null, null, outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRunAdversary_UnknownInstance_Returns404() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(patchAdversary(runId, 999999L, 4, null, null, creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRunAdversary_OnCompletedRun_Returns400() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);
        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patchAdversary(runId, instanceId, 4, null, null, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRunAdversary_NegativeHitPointsMarked_Returns400() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversary(runId, instanceId, -1, null, null, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRunAdversary_NewInstance_TokensDefaultToZero() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(get("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].tokens").value(0));
    }

    @Test
    void updateRunAdversary_SetsTokens() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 5, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].tokens").value(5));
    }

    @Test
    void updateRunAdversary_TokensBackToZero() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);
        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 5, creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 0, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].tokens").value(0));
    }

    @Test
    void updateRunAdversary_TokensNotClampedToAnyMax() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 99, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].tokens").value(99));
    }

    @Test
    void updateRunAdversary_NegativeTokens_Returns400() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);

        mockMvc.perform(patchAdversaryTokens(runId, instanceId, -1, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRunAdversary_OmittedTokens_LeavesExistingTokensUnchanged() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);
        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 5, creatorToken))
                .andExpect(status().isOk());

        // hitPointsMarked-only patch, tokens omitted entirely.
        mockMvc.perform(patchAdversary(runId, instanceId, 4, null, null, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversaries[0].tokens").value(5));
    }

    @Test
    void listRuns_IncludesTokensEvenThoughAdversariesStayUnexpanded() throws Exception {
        Long runId = startStandaloneRun();
        Long instanceId = findInstanceId(runId);
        mockMvc.perform(patchAdversaryTokens(runId, instanceId, 5, creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dh/encounter-runs")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adversaries[0].tokens").value(5));
    }

    // ==================== COMPLETE ====================

    @Test
    void completeRun_AsOwner_Returns200AndSetsCompleted() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endedAt").exists());

        assertThat(encounterRunRepository.findById(runId).orElseThrow().getStatus())
                .isEqualTo(EncounterRunStatus.COMPLETED);
    }

    @Test
    void completeRun_AlreadyCompleted_Returns400() throws Exception {
        Long runId = startStandaloneRun();
        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeRun_AsUnrelatedUser_Returns403() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(post("/api/dh/encounter-runs/{runId}/complete", runId)
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE ====================

    @Test
    void deleteRun_AsOwner_Returns204AndRemoves() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(delete("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNoContent());

        assertThat(encounterRunRepository.findById(runId)).isEmpty();
    }

    @Test
    void deleteRun_AsModerator_Returns204() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(delete("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRun_AsUnrelatedUser_Returns403() throws Exception {
        Long runId = startStandaloneRun();

        mockMvc.perform(delete("/api/dh/encounter-runs/{runId}", runId)
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRun_UnknownRun_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/encounter-runs/{runId}", 999999L)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    private MockHttpServletRequestBuilder startRun(Long encounterId, Long campaignId, String token) throws Exception {
        CreateEncounterRunRequest request = CreateEncounterRunRequest.builder().campaignId(campaignId).build();
        return post("/api/dh/encounters/{id}/runs", encounterId)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder patchAdversary(
            Long runId, Long instanceId, Integer hitPoints, Integer stress, Boolean defeated, String token)
            throws Exception {
        UpdateEncounterRunAdversaryRequest request = UpdateEncounterRunAdversaryRequest.builder()
                .hitPointsMarked(hitPoints)
                .stressMarked(stress)
                .isDefeated(defeated)
                .build();
        return patch("/api/dh/encounter-runs/{runId}/adversaries/{instanceId}", runId, instanceId)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder patchAdversaryTokens(
            Long runId, Long instanceId, Integer tokens, String token) throws Exception {
        UpdateEncounterRunAdversaryRequest request = UpdateEncounterRunAdversaryRequest.builder()
                .tokens(tokens)
                .build();
        return patch("/api/dh/encounter-runs/{runId}/adversaries/{instanceId}", runId, instanceId)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private Long startStandaloneRun() throws Exception {
        String body = mockMvc.perform(startRun(testEncounter.getId(), null, creatorToken))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long startCampaignRun() throws Exception {
        String body = mockMvc.perform(startRun(testEncounter.getId(), testCampaign.getId(), creatorToken))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long findInstanceId(Long runId) {
        EncounterRun run = encounterRunRepository.findById(runId).orElseThrow();
        return run.getEncounterRunAdversaries().get(0).getId();
    }

    private User createUserWithRole(String username, String email, Role role) {
        return userRepository.save(User.builder().username(username).email(email).role(role).build());
    }

    private String tokenFor(User user) {
        String token = jwtTokenProvider.generateToken(user);
        storeTokenInDatabase(user.getId(), token);
        return token;
    }

    private void storeTokenInDatabase(Long userId, String token) {
        activeTokenRepository.save(ActiveToken.builder()
                .userId(userId)
                .tokenHash(jwtTokenProvider.hashToken(token))
                .deviceInfo("Test Device")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }
}
