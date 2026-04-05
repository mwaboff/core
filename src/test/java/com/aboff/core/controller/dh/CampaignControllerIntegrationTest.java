package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCampaignRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.dh.CampaignInviteRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.UserRepository;
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
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CampaignController.
 * Tests all CRUD endpoints, user management, and character sheet management
 * with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignInviteRepository campaignInviteRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User creator;
    private User gm2;
    private User player1;
    private User player2;
    private User moderator;
    private String creatorToken;
    private String gm2Token;
    private String player1Token;
    private String player2Token;
    private String moderatorToken;
    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("creator", "creator@example.com", Role.USER);
        gm2 = createUserWithRole("gm2", "gm2@example.com", Role.USER);
        player1 = createUserWithRole("player1", "player1@example.com", Role.USER);
        player2 = createUserWithRole("player2", "player2@example.com", Role.USER);
        moderator = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);

        creatorToken = jwtTokenProvider.generateToken(creator);
        gm2Token = jwtTokenProvider.generateToken(gm2);
        player1Token = jwtTokenProvider.generateToken(player1);
        player2Token = jwtTokenProvider.generateToken(player2);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(creator.getId(), creatorToken);
        storeTokenInDatabase(gm2.getId(), gm2Token);
        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(player2.getId(), player2Token);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        testCampaign = createCampaign("Test Campaign", "A test campaign", creator);
    }

    // ==================== GET ALL CAMPAIGNS TESTS ====================

    @Test
    void getAllCampaigns_AsModerator_Returns200() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllCampaigns_AsRegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCampaigns_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCampaigns_FilterByCreatorId_ReturnsFiltered() throws Exception {
        createCampaign("Other Campaign", "Another one", gm2);

        mockMvc.perform(get("/api/dh/campaigns")
                        .param("creatorId", creator.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].creatorId").value(creator.getId()));
    }

    @Test
    void getAllCampaigns_FilterByName_ReturnsFiltered() throws Exception {
        createCampaign("Dragon Hunt", "Hunt the dragon", gm2);

        mockMvc.perform(get("/api/dh/campaigns")
                        .param("name", "Dragon")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dragon Hunt"));
    }

    // ==================== GET CAMPAIGN BY ID TESTS ====================

    @Test
    void getCampaignById_AsParticipant_Returns200() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCampaign.getId()))
                .andExpect(jsonPath("$.name").value("Test Campaign"));
    }

    @Test
    void getCampaignById_AsNonParticipant_Returns403() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCampaignById_AsModerator_Returns200() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Campaign"));
    }

    @Test
    void getCampaignById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCampaignById_WithExpansion_IncludesExpandedData() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .param("expand", "creator")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creator").exists())
                .andExpect(jsonPath("$.creator.username").value("creator"));
    }

    // ==================== CREATE CAMPAIGN TESTS ====================

    @Test
    void createCampaign_AsAuthenticatedUser_Returns201() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("New Campaign")
                .description("A new adventure")
                .build();

        mockMvc.perform(post("/api/dh/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Campaign"))
                .andExpect(jsonPath("$.creatorId").value(player1.getId()))
                .andExpect(jsonPath("$.gameMasterIds[0]").value(player1.getId()));
    }

    @Test
    void createCampaign_Unauthenticated_Returns401() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("New Campaign")
                .build();

        mockMvc.perform(post("/api/dh/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCampaign_WithMissingName_Returns400() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .description("No name provided")
                .build();

        mockMvc.perform(post("/api/dh/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCampaign_WithAdditionalGMsAndPlayers_AddsUsers() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("Full Campaign")
                .gameMasterIds(List.of(gm2.getId()))
                .playerIds(List.of(player1.getId(), player2.getId()))
                .build();

        mockMvc.perform(post("/api/dh/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameMasterIds.length()").value(2))
                .andExpect(jsonPath("$.playerIds.length()").value(2));
    }

    // ==================== UPDATE CAMPAIGN TESTS ====================

    @Test
    void updateCampaign_AsCreator_Returns200() throws Exception {
        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("Updated Campaign Name")
                .build();

        mockMvc.perform(put("/api/dh/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Campaign Name"));
    }

    @Test
    void updateCampaign_AsModerator_Returns200() throws Exception {
        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("Moderator Update")
                .build();

        mockMvc.perform(put("/api/dh/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moderator Update"));
    }

    @Test
    void updateCampaign_AsNonCreator_Returns403() throws Exception {
        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("Hacked Name")
                .build();

        mockMvc.perform(put("/api/dh/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCampaign_NotFound_Returns404() throws Exception {
        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name")
                .build();

        mockMvc.perform(put("/api/dh/campaigns/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE CAMPAIGN TESTS ====================

    @Test
    void deleteCampaign_AsCreator_Returns204() throws Exception {
        mockMvc.perform(delete("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNoContent());

        Campaign deletedCampaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(deletedCampaign.isDeleted()).isTrue();
    }

    @Test
    void deleteCampaign_AsModerator_Returns204() throws Exception {
        mockMvc.perform(delete("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());

        Campaign deletedCampaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(deletedCampaign.isDeleted()).isTrue();
    }

    @Test
    void deleteCampaign_AsNonCreator_Returns403() throws Exception {
        mockMvc.perform(delete("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());

        Campaign campaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(campaign.isDeleted()).isFalse();
    }

    // ==================== USER MANAGEMENT TESTS ====================

    @Test
    void addGameMaster_AsCreator_Returns200() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/{id}/game-masters/{userId}", testCampaign.getId(), gm2.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameMasterIds").isArray());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getGameMasters()).anyMatch(gm -> gm.getId().equals(gm2.getId()));
    }

    @Test
    void addGameMaster_AsNonCreator_Returns403() throws Exception {
        addGMToCampaign(testCampaign, gm2);

        mockMvc.perform(post("/api/dh/campaigns/{id}/game-masters/{userId}", testCampaign.getId(), player1.getId())
                        .cookie(new Cookie("AUTH_TOKEN", gm2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeGameMaster_AsCreator_Returns200() throws Exception {
        addGMToCampaign(testCampaign, gm2);

        mockMvc.perform(delete("/api/dh/campaigns/{id}/game-masters/{userId}", testCampaign.getId(), gm2.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getGameMasters()).noneMatch(gm -> gm.getId().equals(gm2.getId()));
    }

    @Test
    void removeGameMaster_RemoveCreator_Returns400() throws Exception {
        mockMvc.perform(delete("/api/dh/campaigns/{id}/game-masters/{userId}", testCampaign.getId(), creator.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPlayer_AsGM_Returns200() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/{id}/players/{userId}", testCampaign.getId(), player1.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerIds").isArray());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayers()).anyMatch(p -> p.getId().equals(player1.getId()));
    }

    @Test
    void addPlayer_AsNonGM_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/players/{userId}", testCampaign.getId(), player2.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removePlayer_AsGM_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);

        mockMvc.perform(delete("/api/dh/campaigns/{id}/players/{userId}", testCampaign.getId(), player1.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayers()).noneMatch(p -> p.getId().equals(player1.getId()));
    }

    // ==================== CHARACTER SHEET MANAGEMENT TESTS ====================

    @Test
    void submitCharacterSheet_AsOwnerAndPlayer_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/submit",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCharacterSheetIds").isArray());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPendingCharacterSheets()).anyMatch(cs -> cs.getId().equals(sheet.getId()));
    }

    @Test
    void submitCharacterSheet_AsNonOwner_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player2);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/submit",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitCharacterSheet_AsNonPlayer_Returns403() throws Exception {
        CharacterSheet sheet = createCharacterSheet("Hero", player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/submit",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveCharacterSheet_AsGM_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPendingCharacterSheet(testCampaign, sheet);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/approve",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCharacterIds").isArray());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPendingCharacterSheets()).isEmpty();
        assertThat(updated.getPlayerCharacters()).anyMatch(cs -> cs.getId().equals(sheet.getId()));
    }

    @Test
    void approveCharacterSheet_AsNonGM_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPendingCharacterSheet(testCampaign, sheet);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/approve",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveCharacterSheet_NotInPending_Returns400() throws Exception {
        CharacterSheet sheet = createCharacterSheet("Hero", player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/approve",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectCharacterSheet_AsGM_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPendingCharacterSheet(testCampaign, sheet);

        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/reject",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPendingCharacterSheets()).isEmpty();
    }

    @Test
    void addNonPlayerCharacter_AsGM_Returns200() throws Exception {
        CharacterSheet sheet = createCharacterSheet("Villain", creator);

        mockMvc.perform(post("/api/dh/campaigns/{id}/npcs/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonPlayerCharacterIds").isArray());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getNonPlayerCharacters()).anyMatch(cs -> cs.getId().equals(sheet.getId()));
    }

    @Test
    void addNonPlayerCharacter_AsNonGM_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Villain", player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/npcs/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeCharacterSheet_AsGM_Returns200() throws Exception {
        CharacterSheet sheet = createCharacterSheet("Hero", creator);
        addPlayerCharacter(testCampaign, sheet);

        mockMvc.perform(delete("/api/dh/campaigns/{id}/character-sheets/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayerCharacters()).isEmpty();
    }

    @Test
    void removeCharacterSheet_AsSheetOwner_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPlayerCharacter(testCampaign, sheet);

        mockMvc.perform(delete("/api/dh/campaigns/{id}/character-sheets/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayerCharacters()).isEmpty();
    }

    @Test
    void removeCharacterSheet_AsNonGMNonOwner_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        addPlayerToCampaign(testCampaign, player2);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPlayerCharacter(testCampaign, sheet);

        // player2 is not the sheet owner and not a GM
        mockMvc.perform(delete("/api/dh/campaigns/{id}/character-sheets/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    // ==================== GET MY CAMPAIGNS TESTS ====================

    @Test
    void getMyCampaigns_AsAuthenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/mine")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Campaign"));
    }

    @Test
    void getMyCampaigns_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyCampaigns_AsNonMember_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/mine")
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ==================== INVITE & JOIN TESTS ====================

    @Test
    void generateInvite_AsGM_Returns201() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/{id}/invites", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.campaignId").value(testCampaign.getId()));
    }

    @Test
    void generateInvite_AsNonGM_Returns403() throws Exception {
        addPlayerToCampaign(testCampaign, player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/invites", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void joinCampaign_ValidToken_Returns200() throws Exception {
        // Generate invite via endpoint
        String inviteResponse = mockMvc.perform(post("/api/dh/campaigns/{id}/invites", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String inviteToken = objectMapper.readTree(inviteResponse).get("token").asText();

        mockMvc.perform(post("/api/dh/campaigns/join/{token}", inviteToken)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(testCampaign.getId()))
                .andExpect(jsonPath("$.campaignName").value("Test Campaign"));

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayers()).anyMatch(p -> p.getId().equals(player1.getId()));
    }

    @Test
    void joinCampaign_InvalidToken_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/join/{token}", "nonexistent-token")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    // ==================== CAMPAIGN LIFECYCLE TESTS ====================

    @Test
    void endCampaign_AsCreator_Returns200() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/{id}/end", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCampaign.getId()));

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.isEnded()).isTrue();
    }

    @Test
    void endCampaign_EndedCampaign_Returns400() throws Exception {
        // End the campaign first
        mockMvc.perform(post("/api/dh/campaigns/{id}/end", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        // Try to end again
        mockMvc.perform(post("/api/dh/campaigns/{id}/end", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaveCampaign_AsPlayer_Returns200() throws Exception {
        addPlayerToCampaign(testCampaign, player1);

        mockMvc.perform(post("/api/dh/campaigns/{id}/leave", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayers()).noneMatch(p -> p.getId().equals(player1.getId()));
    }

    @Test
    void leaveCampaign_AsNonPlayer_Returns400() throws Exception {
        mockMvc.perform(post("/api/dh/campaigns/{id}/leave", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kickPlayer_CascadesCharacterSheetRemoval() throws Exception {
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPlayerCharacter(testCampaign, sheet);

        mockMvc.perform(delete("/api/dh/campaigns/{id}/players/{userId}", testCampaign.getId(), player1.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayers()).noneMatch(p -> p.getId().equals(player1.getId()));
        assertThat(updated.getPlayerCharacters()).noneMatch(cs -> cs.getId().equals(sheet.getId()));
    }

    @Test
    void submitCharacterSheet_AlreadyInCampaign_Returns400() throws Exception {
        // Add player1 to testCampaign and submit a sheet
        addPlayerToCampaign(testCampaign, player1);
        CharacterSheet sheet = createCharacterSheet("Hero", player1);
        addPlayerCharacter(testCampaign, sheet);

        // Create a second campaign with player1
        Campaign secondCampaign = createCampaign("Second Campaign", "Another campaign", gm2);
        addPlayerToCampaign(secondCampaign, player1);

        // Try to submit the same sheet to the second campaign
        mockMvc.perform(post("/api/dh/campaigns/{id}/character-sheets/{sheetId}/submit",
                        secondCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endCampaign_BlocksUpdate_Returns400() throws Exception {
        // End the campaign
        mockMvc.perform(post("/api/dh/campaigns/{id}/end", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        // Try to update the ended campaign
        UpdateCampaignRequest request = UpdateCampaignRequest.builder()
                .name("New Name After End")
                .build();

        mockMvc.perform(put("/api/dh/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endCampaign_AllowsUnlink_Returns200() throws Exception {
        CharacterSheet sheet = createCharacterSheet("Hero", creator);
        addPlayerCharacter(testCampaign, sheet);

        // End the campaign
        mockMvc.perform(post("/api/dh/campaigns/{id}/end", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        // Removing a character sheet should still be allowed
        mockMvc.perform(delete("/api/dh/campaigns/{id}/character-sheets/{sheetId}",
                        testCampaign.getId(), sheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk());

        Campaign updated = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        assertThat(updated.getPlayerCharacters()).isEmpty();
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

    private Campaign createCampaign(String name, String description, User creatorUser) {
        Campaign campaign = Campaign.builder()
                .name(name)
                .description(description)
                .creator(creatorUser)
                .gameMasters(new HashSet<>())
                .players(new HashSet<>())
                .pendingCharacterSheets(new HashSet<>())
                .playerCharacters(new HashSet<>())
                .nonPlayerCharacters(new HashSet<>())
                .build();
        campaign.getGameMasters().add(creatorUser);
        return campaignRepository.save(campaign);
    }

    private void addGMToCampaign(Campaign campaign, User gm) {
        campaign.getGameMasters().add(gm);
        campaignRepository.save(campaign);
    }

    private void addPlayerToCampaign(Campaign campaign, User player) {
        campaign.getPlayers().add(player);
        campaignRepository.save(campaign);
    }

    private CharacterSheet createCharacterSheet(String name, User owner) {
        CharacterSheet sheet = CharacterSheet.builder()
                .name(name)
                .level(1)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(3)
                .hopeMarked(0)
                .gold(0)
                .owner(owner)
                .build();
        return characterSheetRepository.save(sheet);
    }

    private void addPendingCharacterSheet(Campaign campaign, CharacterSheet sheet) {
        campaign.getPendingCharacterSheets().add(sheet);
        campaignRepository.save(campaign);
    }

    private void addPlayerCharacter(Campaign campaign, CharacterSheet sheet) {
        campaign.getPlayerCharacters().add(sheet);
        campaignRepository.save(campaign);
    }
}
