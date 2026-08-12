package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.UpdateTransformationAccessRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
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
 * Integration tests for the GM transformation access endpoint on CampaignController:
 * {@code PUT /api/dh/campaigns/{id}/character-sheets/{sheetId}/transformation}.
 * <p>
 * Covers authorization, campaign membership of the target sheet, the preserve-on-disable
 * contract, {@code clearTransformationCard} precedence, and the transformation fields exposed
 * on the campaign character summaries used by the GM roster.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CampaignTransformationAccessIntegrationTest {

    private static final String TRANSFORMATION_PATH =
            "/api/dh/campaigns/{id}/character-sheets/{sheetId}/transformation";

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
    private TransformationCardRepository transformationCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

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
    private TransformationCard werewolf;
    private TransformationCard vampire;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("xform-creator", "xform-creator@example.com", Role.USER);
        gm2 = createUserWithRole("xform-gm2", "xform-gm2@example.com", Role.USER);
        player1 = createUserWithRole("xform-player1", "xform-player1@example.com", Role.USER);
        outsider = createUserWithRole("xform-outsider", "xform-outsider@example.com", Role.USER);
        moderator = createUserWithRole("xform-moderator", "xform-moderator@example.com", Role.MODERATOR);

        creatorToken = issueToken(creator);
        gm2Token = issueToken(gm2);
        player1Token = issueToken(player1);
        outsiderToken = issueToken(outsider);
        moderatorToken = issueToken(moderator);

        Expansion expansion = expansionRepository.save(
                Expansion.builder().name("Transformation Test Expansion").isPublished(true).build());
        werewolf = createTransformationCard("Werewolf", expansion);
        vampire = createTransformationCard("Vampire", expansion);

        playerSheet = createCharacterSheet("Roster Hero", player1);
        unrelatedSheet = createCharacterSheet("Unrelated Hero", outsider);

        testCampaign = createCampaign("Transformation Campaign", creator);
        testCampaign.getGameMasters().add(gm2);
        testCampaign.getPlayers().add(player1);
        testCampaign.getPlayerCharacters().add(playerSheet);
        testCampaign = campaignRepository.save(testCampaign);
    }

    // ==================== AUTHORIZATION ====================

    @Test
    void updateTransformationAccess_AsCreator_Returns200AndPersists() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerSheet.getId()))
                .andExpect(jsonPath("$.transformationEnabled").value(true));

        assertThat(reloadSheet().isTransformationEnabled()).isTrue();
    }

    @Test
    void updateTransformationAccess_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), gm2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationEnabled").value(true));
    }

    @Test
    void updateTransformationAccess_AsModerator_Returns200() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationEnabled").value(true));
    }

    @Test
    void updateTransformationAccess_AsSheetOwningPlayer_Returns403() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), player1Token))
                .andExpect(status().isForbidden());

        assertThat(reloadSheet().isTransformationEnabled()).isFalse();
    }

    @Test
    void updateTransformationAccess_AsNonParticipant_Returns403() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTransformationAccess_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(put(TRANSFORMATION_PATH, testCampaign.getId(), playerSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ==================== SHEET MEMBERSHIP ====================

    @Test
    void updateTransformationAccess_SheetNotInCampaign_Returns404() throws Exception {
        mockMvc.perform(put(TRANSFORMATION_PATH, testCampaign.getId(), unrelatedSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransformationAccess_CampaignNotFound_Returns404() throws Exception {
        mockMvc.perform(put(TRANSFORMATION_PATH, 999999L, playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableRequest())))
                .andExpect(status().isNotFound());
    }

    // ==================== VALIDATION ====================

    @Test
    void updateTransformationAccess_MissingEnabled_Returns400() throws Exception {
        mockMvc.perform(put(TRANSFORMATION_PATH, testCampaign.getId(), playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransformationAccess_UnknownCardId_Returns404() throws Exception {
        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .transformationCardId(999999L)
                .build();

        mockMvc.perform(putTransformation(request, creatorToken))
                .andExpect(status().isNotFound());
    }

    // ==================== SEMANTICS ====================

    @Test
    void updateTransformationAccess_AssignsCard() throws Exception {
        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .transformationCardId(werewolf.getId())
                .build();

        mockMvc.perform(putTransformation(request, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationEnabled").value(true))
                .andExpect(jsonPath("$.transformationCardId").value(werewolf.getId()));
    }

    @Test
    void updateTransformationAccess_AssignsCardWhileDisabled() throws Exception {
        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(false)
                .transformationCardId(vampire.getId())
                .build();

        mockMvc.perform(putTransformation(request, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationEnabled").value(false))
                .andExpect(jsonPath("$.transformationCardId").value(vampire.getId()));
    }

    @Test
    void updateTransformationAccess_Disable_PreservesCardTokensAndWolfForm() throws Exception {
        seedTransformationState();

        mockMvc.perform(putTransformation(disableRequest(), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationEnabled").value(false))
                .andExpect(jsonPath("$.transformationCardId").value(werewolf.getId()))
                .andExpect(jsonPath("$.transformationTokens").value(4))
                .andExpect(jsonPath("$.wolfFormActive").value(true));

        CharacterSheet reloaded = reloadSheet();
        assertThat(reloaded.isTransformationEnabled()).isFalse();
        assertThat(reloaded.getTransformationCard().getId()).isEqualTo(werewolf.getId());
        assertThat(reloaded.getTransformationTokens()).isEqualTo(4);
        assertThat(reloaded.getWolfFormActive()).isTrue();
    }

    @Test
    void updateTransformationAccess_ClearFlag_TakesPrecedenceOverCardId() throws Exception {
        seedTransformationState();

        UpdateTransformationAccessRequest request = UpdateTransformationAccessRequest.builder()
                .enabled(true)
                .transformationCardId(vampire.getId())
                .clearTransformationCard(true)
                .build();

        mockMvc.perform(putTransformation(request, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationCardId").doesNotExist())
                .andExpect(jsonPath("$.transformationTokens").doesNotExist())
                .andExpect(jsonPath("$.wolfFormActive").value(false));

        CharacterSheet reloaded = reloadSheet();
        assertThat(reloaded.getTransformationCard()).isNull();
        assertThat(reloaded.getTransformationTokens()).isNull();
        assertThat(reloaded.getWolfFormActive()).isFalse();
    }

    // ==================== PLAYER UPDATE PATH GATE ====================

    @Test
    void updateCharacterSheet_TransformationFields_WhenDisabled_Returns400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/dh/character-sheets/{id}", playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transformationCardId\":" + werewolf.getId() + "}"))
                .andExpect(status().isBadRequest());

        assertThat(reloadSheet().getTransformationCard()).isNull();
    }

    @Test
    void updateCharacterSheet_TransformationFields_AfterGmEnables_Returns200() throws Exception {
        mockMvc.perform(putTransformation(enableRequest(), creatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/dh/character-sheets/{id}", playerSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transformationCardId\":" + werewolf.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transformationCardId").value(werewolf.getId()));
    }

    // ==================== CAMPAIGN CHARACTER SUMMARIES ====================

    @Test
    void getCampaignById_CharacterSummaries_IncludeTransformationFields() throws Exception {
        seedTransformationState();

        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .param("expand", "characterSummaries")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSummaries[0].transformationEnabled").value(true))
                .andExpect(jsonPath("$.characterSummaries[0].transformationCardId").value(werewolf.getId()))
                .andExpect(jsonPath("$.characterSummaries[0].transformationCardName").value("Werewolf"));
    }

    @Test
    void getCampaignById_CharacterSummaries_WithoutTransformation_OmitCardFields() throws Exception {
        mockMvc.perform(get("/api/dh/campaigns/{id}", testCampaign.getId())
                        .param("expand", "characterSummaries")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSummaries[0].transformationEnabled").value(false))
                .andExpect(jsonPath("$.characterSummaries[0].transformationCardId").doesNotExist())
                .andExpect(jsonPath("$.characterSummaries[0].transformationCardName").doesNotExist());
    }

    // ==================== HELPER METHODS ====================

    private MockHttpServletRequestBuilder putTransformation(
            UpdateTransformationAccessRequest request, String token) throws Exception {
        return put(TRANSFORMATION_PATH, testCampaign.getId(), playerSheet.getId())
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private UpdateTransformationAccessRequest enableRequest() {
        return UpdateTransformationAccessRequest.builder().enabled(true).build();
    }

    private UpdateTransformationAccessRequest disableRequest() {
        return UpdateTransformationAccessRequest.builder().enabled(false).build();
    }

    /**
     * Puts the roster character into a fully transformed state so the preserve-on-disable
     * assertions do not depend on the endpoint under test.
     */
    private void seedTransformationState() {
        CharacterSheet sheet = characterSheetRepository.findById(playerSheet.getId()).orElseThrow();
        sheet.setTransformationEnabled(true);
        sheet.setTransformationCard(werewolf);
        sheet.setTransformationTokens(4);
        sheet.setWolfFormActive(true);
        characterSheetRepository.save(sheet);
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

    private TransformationCard createTransformationCard(String name, Expansion expansion) {
        return transformationCardRepository.save(TransformationCard.builder()
                .name(name)
                .description(name + " transformation")
                .expansion(expansion)
                .isOfficial(false)
                .build());
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
                .description("Transformation access tests")
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
