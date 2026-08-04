package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.UpdateCompanionAccessRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the GM companion access endpoint on CampaignController:
 * {@code PUT /api/dh/campaigns/{id}/character-sheets/{sheetId}/companions}.
 * <p>
 * Mirrors {@link CampaignTransformationAccessIntegrationTest} for authorization, campaign
 * membership of the target sheet, and the ended-campaign gate. Deliberately does <strong>not</strong>
 * mirror the transformation suite's "preserve on disable" or player-write-gate sections: a
 * companion has no card/token/form state to preserve, and disabling this flag has no player-side
 * write gate to enforce (see the companions implementation plan, section 3.4) -- an existing
 * companion is never hidden, disabled, or orphaned by this endpoint.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CampaignCompanionAccessIntegrationTest {

    private static final String COMPANIONS_PATH =
            "/api/dh/campaigns/{id}/character-sheets/{sheetId}/companions";

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
    private CharacterSheetRepository characterSheetRepository;

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
    private CharacterSheet playerSheet;
    private CharacterSheet unrelatedSheet;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("companion-creator", "companion-creator@example.com", Role.USER);
        gm2 = createUserWithRole("companion-gm2", "companion-gm2@example.com", Role.USER);
        player1 = createUserWithRole("companion-player1", "companion-player1@example.com", Role.USER);
        outsider = createUserWithRole("companion-outsider", "companion-outsider@example.com", Role.USER);
        moderator = createUserWithRole("companion-moderator", "companion-moderator@example.com", Role.MODERATOR);

        creatorToken = issueToken(creator);
        gm2Token = issueToken(gm2);
        player1Token = issueToken(player1);
        outsiderToken = issueToken(outsider);
        moderatorToken = issueToken(moderator);

        playerSheet = createCharacterSheet("Roster Hero", player1);
        unrelatedSheet = createCharacterSheet("Unrelated Hero", outsider);

        testCampaign = createCampaign("Companion Campaign", creator);
        testCampaign.getGameMasters().add(gm2);
        testCampaign.getPlayers().add(player1);
        testCampaign.getPlayerCharacters().add(playerSheet);
        testCampaign = campaignRepository.save(testCampaign);
    }

    // ==================== AUTHORIZATION ====================

    @Test
    void updateCompanionAccess_AsCreator_Returns200AndPersists() throws Exception {
        mockMvc.perform(putCompanionAccess(enableRequest(), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerSheet.getId()))
                .andExpect(jsonPath("$.companionsEnabled").value(true));

        assertThat(reloadSheet().isCompanionsEnabled()).isTrue();
    }

    @Test
    void updateCompanionAccess_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(putCompanionAccess(enableRequest(), gm2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companionsEnabled").value(true));
    }

    @Test
    void updateCompanionAccess_AsModerator_Returns200() throws Exception {
        mockMvc.perform(putCompanionAccess(enableRequest(), moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companionsEnabled").value(true));
    }

    @Test
    void updateCompanionAccess_AsSheetOwningPlayer_Returns403() throws Exception {
        // The one thing this endpoint exists to prove: a non-GM (even the sheet's own owner)
        // cannot flip the flag.
        mockMvc.perform(putCompanionAccess(enableRequest(), player1Token))
                .andExpect(status().isForbidden());

        assertThat(reloadSheet().isCompanionsEnabled()).isFalse();
    }

    @Test
    void updateCompanionAccess_AsNonParticipant_Returns403() throws Exception {
        mockMvc.perform(putCompanionAccess(enableRequest(), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCompanionAccess_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(put(COMPANIONS_PATH, testCampaign.getId(), playerSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ==================== SHEET MEMBERSHIP ====================

    @Test
    void updateCompanionAccess_SheetNotInCampaign_Returns404() throws Exception {
        mockMvc.perform(put(COMPANIONS_PATH, testCampaign.getId(), unrelatedSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCompanionAccess_CampaignNotFound_Returns404() throws Exception {
        mockMvc.perform(put(COMPANIONS_PATH, 999999L, playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isNotFound());
    }

    // ==================== VALIDATION ====================

    @Test
    void updateCompanionAccess_MissingEnabled_Returns400() throws Exception {
        mockMvc.perform(put(COMPANIONS_PATH, testCampaign.getId(), playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ENDED CAMPAIGN ====================

    @Test
    void updateCompanionAccess_EndedCampaign_Returns400() throws Exception {
        Campaign campaign = campaignRepository.findById(testCampaign.getId()).orElseThrow();
        campaign.endCampaign();
        campaignRepository.save(campaign);

        mockMvc.perform(putCompanionAccess(enableRequest(), creatorToken))
                .andExpect(status().isBadRequest());

        assertThat(reloadSheet().isCompanionsEnabled()).isFalse();
    }

    // ==================== DISABLE NEVER ORPHANS AN EXISTING COMPANION ====================

    @Test
    void updateCompanionAccess_Disable_OnlyTouchesTheFlag() throws Exception {
        // Enable, then disable -- the endpoint has no card/token state, so disabling must be a
        // clean no-op beyond the flag itself. There is nothing else on the sheet for it to touch.
        mockMvc.perform(putCompanionAccess(enableRequest(), creatorToken))
                .andExpect(status().isOk());
        assertThat(reloadSheet().isCompanionsEnabled()).isTrue();

        mockMvc.perform(putCompanionAccess(disableRequest(), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companionsEnabled").value(false));

        assertThat(reloadSheet().isCompanionsEnabled()).isFalse();
    }

    // ==================== CAMPAIGN CHARACTER SUMMARIES ====================

    @Test
    void getCampaignById_CharacterSummaries_IncludeCompanionsEnabled() throws Exception {
        mockMvc.perform(putCompanionAccess(enableRequest(), creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .param("expand", "characterSummaries")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSummaries[0].companionsEnabled").value(true));
    }

    @Test
    void getCampaignById_CharacterSummaries_DefaultsToFalse() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .param("expand", "characterSummaries")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSummaries[0].companionsEnabled").value(false));
    }

    // ==================== HELPER METHODS ====================

    private MockHttpServletRequestBuilder putCompanionAccess(
            UpdateCompanionAccessRequest request, String token) throws Exception {
        return put(COMPANIONS_PATH, testCampaign.getId(), playerSheet.getId())
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private UpdateCompanionAccessRequest enableRequest() {
        return UpdateCompanionAccessRequest.builder().enabled(true).build();
    }

    private UpdateCompanionAccessRequest disableRequest() {
        return UpdateCompanionAccessRequest.builder().enabled(false).build();
    }

    private CharacterSheet reloadSheet() {
        return characterSheetRepository.findById(playerSheet.getId()).orElseThrow();
    }

    private User createUserWithRole(String username, String email, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .role(role)
                .build());
    }

    private String issueToken(User user) {
        String token = jwtTokenProvider.generateToken(user);
        activeTokenRepository.save(ActiveToken.builder()
                .userId(user.getId())
                .tokenHash(jwtTokenProvider.hashToken(token))
                .deviceInfo("Test Device")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
        return token;
    }

    private CharacterSheet createCharacterSheet(String name, User owner) {
        return characterSheetRepository.save(CharacterSheet.builder()
                .name(name)
                .pronouns("they/them")
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
                .gold(50)
                .owner(owner)
                .build());
    }

    private Campaign createCampaign(String name, User creatorUser) {
        Campaign campaign = Campaign.builder()
                .name(name)
                .description("Companion access tests")
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
}
