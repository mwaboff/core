package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.UpdateCampaignFearRequest;
import com.aboff.core.model.dto.dh.request.UpdateCampaignGmNotesRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.UserRepository;
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
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the GM Screen endpoints on CampaignController:
 * {@code PATCH /api/dh/campaigns/{id}/fear} and {@code PATCH /api/dh/campaigns/{id}/gm-notes}.
 * <p>
 * Also covers the visibility contract for the two new response fields: {@code fear} is
 * table-visible to every participant, while {@code gmNotes} must never reach a player
 * through any campaign read endpoint.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CampaignGmScreenIntegrationTest {

    private static final String FEAR_PATH = "/api/dh/campaigns/{id}/fear";
    private static final String GM_NOTES_PATH = "/api/dh/campaigns/{id}/gm-notes";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User creator;
    private User gm2;
    private User player1;
    private User outsider;
    private User moderator;
    private String creatorToken;
    private String gm2Token;
    private String player1Token;
    private String outsiderToken;
    private String moderatorToken;
    private Campaign testCampaign;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("gmscreen-creator", "gmscreen-creator@example.com", Role.USER);
        gm2 = createUserWithRole("gmscreen-gm2", "gmscreen-gm2@example.com", Role.USER);
        player1 = createUserWithRole("gmscreen-player1", "gmscreen-player1@example.com", Role.USER);
        outsider = createUserWithRole("gmscreen-outsider", "gmscreen-outsider@example.com", Role.USER);
        moderator = createUserWithRole("gmscreen-moderator", "gmscreen-moderator@example.com", Role.MODERATOR);

        creatorToken = jwtTokenProvider.generateToken(creator);
        gm2Token = jwtTokenProvider.generateToken(gm2);
        player1Token = jwtTokenProvider.generateToken(player1);
        outsiderToken = jwtTokenProvider.generateToken(outsider);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(creator.getId(), creatorToken);
        storeTokenInDatabase(gm2.getId(), gm2Token);
        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(outsider.getId(), outsiderToken);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        testCampaign = createCampaign("GM Screen Campaign", "For GM screen tests", creator);
        addGameMaster(testCampaign, gm2);
        addPlayer(testCampaign, player1);
    }

    // ==================== PATCH FEAR — AUTHORIZATION ====================

    @Test
    void updateFear_AsCreator_Returns200AndPersists() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 5, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(5));

        assertThat(campaignRepository.findById(testCampaign.getId()).orElseThrow().getFear()).isEqualTo(5);
    }

    @Test
    void updateFear_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 3, gm2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(3));
    }

    @Test
    void updateFear_AsModerator_Returns200() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 7, moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(7));
    }

    @Test
    void updateFear_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 4, player1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFear_AsNonParticipant_Returns403() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 4, outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFear_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(patch(FEAR_PATH, testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fearBody(4)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateFear_UnknownCampaign_Returns404() throws Exception {
        mockMvc.perform(patchFear(999999L, 4, creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFear_EndedCampaign_Returns400() throws Exception {
        endCampaign(testCampaign);

        mockMvc.perform(patchFear(testCampaign.getId(), 4, creatorToken))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH FEAR — VALIDATION ====================

    @Test
    void updateFear_BelowMinimum_Returns400() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), -1, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFear_AboveMaximum_Returns400() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 13, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFear_AtBoundaries_Returns200() throws Exception {
        mockMvc.perform(patchFear(testCampaign.getId(), 0, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(0));

        mockMvc.perform(patchFear(testCampaign.getId(), 12, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(12));
    }

    @Test
    void updateFear_MissingField_Returns400() throws Exception {
        mockMvc.perform(patch(FEAR_PATH, testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH GM NOTES — AUTHORIZATION ====================

    @Test
    void updateGmNotes_AsCreator_Returns200AndPersists() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "Session 1 prep", creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value("Session 1 prep"));

        assertThat(campaignRepository.findById(testCampaign.getId()).orElseThrow().getGmNotes())
                .isEqualTo("Session 1 prep");
    }

    @Test
    void updateGmNotes_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "GM2 was here", gm2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value("GM2 was here"));
    }

    @Test
    void updateGmNotes_AsModerator_Returns200() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "Moderated", moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value("Moderated"));
    }

    @Test
    void updateGmNotes_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "sneaky", player1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateGmNotes_AsNonParticipant_Returns403() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "sneaky", outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateGmNotes_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(patch(GM_NOTES_PATH, testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gmNotesBody("anon")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateGmNotes_UnknownCampaign_Returns404() throws Exception {
        mockMvc.perform(patchGmNotes(999999L, "notes", creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateGmNotes_EndedCampaign_Returns400() throws Exception {
        endCampaign(testCampaign);

        mockMvc.perform(patchGmNotes(testCampaign.getId(), "notes", creatorToken))
                .andExpect(status().isBadRequest());
    }

    // ==================== PATCH GM NOTES — VALIDATION & SANITIZATION ====================

    @Test
    void updateGmNotes_NullNotes_Returns400() throws Exception {
        mockMvc.perform(patch(GM_NOTES_PATH, testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gmNotes\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGmNotes_AtMaxLength_Returns200() throws Exception {
        String notes = "a".repeat(50000);

        mockMvc.perform(patchGmNotes(testCampaign.getId(), notes, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value(notes));
    }

    @Test
    void updateGmNotes_ExceedsMaxLength_Returns400() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "a".repeat(50001), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGmNotes_EmptyString_ClearsNotes() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "something", creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(patchGmNotes(testCampaign.getId(), "", creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value(""));

        assertThat(campaignRepository.findById(testCampaign.getId()).orElseThrow().getGmNotes()).isEmpty();
    }

    @Test
    void updateGmNotes_ScriptTag_IsSanitized() throws Exception {
        mockMvc.perform(patchGmNotes(testCampaign.getId(), "<script>alert(1)</script>Real notes", creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes", not(containsString("<script"))))
                .andExpect(jsonPath("$.gmNotes", containsString("Real notes")));

        assertThat(campaignRepository.findById(testCampaign.getId()).orElseThrow().getGmNotes())
                .doesNotContain("<script");
    }

    // ==================== VISIBILITY: GET BY ID ====================

    @Test
    void getCampaignById_AsGameMaster_IncludesGmNotesAndFear() throws Exception {
        seedGmScreenState();

        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").value("Secret prep"))
                .andExpect(jsonPath("$.fear").value(6));
    }

    @Test
    void getCampaignById_AsPlayer_OmitsGmNotesButIncludesFear() throws Exception {
        seedGmScreenState();

        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmNotes").doesNotExist())
                .andExpect(jsonPath("$.fear").value(6));
    }

    // ==================== VISIBILITY: GET /mine ====================

    @Test
    void getMyCampaigns_AsGameMaster_IncludesGmNotes() throws Exception {
        seedGmScreenState();

        mockMvc.perform(get("/api/dh/campaigns/mine")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].gmNotes").value("Secret prep"))
                .andExpect(jsonPath("$.content[0].fear").value(6));
    }

    @Test
    void getMyCampaigns_AsPlayer_OmitsGmNotesButIncludesFear() throws Exception {
        seedGmScreenState();

        mockMvc.perform(get("/api/dh/campaigns/mine")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].gmNotes").doesNotExist())
                .andExpect(jsonPath("$.content[0].fear").value(6));
    }

    // ==================== VISIBILITY: MOD+ LIST ENDPOINT ====================

    @Test
    void getAllCampaigns_AsModerator_IncludesGmNotesAndFear() throws Exception {
        seedGmScreenState();

        mockMvc.perform(get("/api/dh/campaigns")
                        .param("creatorId", creator.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].gmNotes").value("Secret prep"))
                .andExpect(jsonPath("$.content[0].fear").value(6));
    }

    @Test
    void newCampaign_DefaultsFearToZeroAndHasNoGmNotes() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fear").value(0))
                .andExpect(jsonPath("$.gmNotes").doesNotExist());
    }

    // ==================== HELPER METHODS ====================

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder patchFear(
            Long campaignId, Integer fear, String token) throws Exception {
        return patch(FEAR_PATH, campaignId)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(fearBody(fear));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder patchGmNotes(
            Long campaignId, String notes, String token) throws Exception {
        return patch(GM_NOTES_PATH, campaignId)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(gmNotesBody(notes));
    }

    private String fearBody(Integer fear) throws Exception {
        return objectMapper.writeValueAsString(UpdateCampaignFearRequest.builder().fear(fear).build());
    }

    private String gmNotesBody(String notes) throws Exception {
        return objectMapper.writeValueAsString(UpdateCampaignGmNotesRequest.builder().gmNotes(notes).build());
    }

    /**
     * Sets a known Fear value and GM notes directly on the persisted campaign so
     * visibility assertions do not depend on the PATCH endpoints.
     */
    private void seedGmScreenState() {
        Campaign campaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        campaign.setFear(6);
        campaign.setGmNotes("Secret prep");
        campaignRepository.save(campaign);
    }

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

    private void addGameMaster(Campaign campaign, User gm) {
        campaign.getGameMasters().add(gm);
        campaignRepository.save(campaign);
    }

    private void addPlayer(Campaign campaign, User player) {
        campaign.getPlayers().add(player);
        campaignRepository.save(campaign);
    }

    private void endCampaign(Campaign campaign) {
        campaign.endCampaign();
        campaignRepository.save(campaign);
    }
}
