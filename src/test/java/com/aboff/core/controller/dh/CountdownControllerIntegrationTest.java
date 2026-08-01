package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownValueRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Countdown;
import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.model.enums.CountdownType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CountdownRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code /api/dh/countdowns}.
 * <p>
 * Countdowns are GM-only state, so every endpoint — reads included — must reject players and
 * outsiders while admitting the creator, any game master, and any moderator.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CountdownControllerIntegrationTest {

    private static final String BASE_PATH = "/api/dh/countdowns";
    private static final String BY_ID_PATH = BASE_PATH + "/{id}";
    private static final String VALUE_PATH = BASE_PATH + "/{id}/value";

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
    private CountdownRepository countdownRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User creator;
    private String creatorToken;
    private String gm2Token;
    private String player1Token;
    private String outsiderToken;
    private String moderatorToken;
    private Campaign testCampaign;
    private Countdown testCountdown;

    @BeforeEach
    void setUp() {
        creator = createUserWithRole("cd-creator", "cd-creator@example.com", Role.USER);
        User gm2 = createUserWithRole("cd-gm2", "cd-gm2@example.com", Role.USER);
        User player1 = createUserWithRole("cd-player1", "cd-player1@example.com", Role.USER);
        User outsider = createUserWithRole("cd-outsider", "cd-outsider@example.com", Role.USER);
        User moderator = createUserWithRole("cd-moderator", "cd-moderator@example.com", Role.MODERATOR);

        creatorToken = tokenFor(creator);
        gm2Token = tokenFor(gm2);
        player1Token = tokenFor(player1);
        outsiderToken = tokenFor(outsider);
        moderatorToken = tokenFor(moderator);

        testCampaign = createCampaign("Countdown Campaign", creator);
        testCampaign.getGameMasters().add(gm2);
        testCampaign.getPlayers().add(player1);
        campaignRepository.save(testCampaign);

        testCountdown = countdownRepository.save(Countdown.builder()
                .campaign(testCampaign)
                .name("The ritual completes")
                .type(CountdownType.CONSEQUENCE)
                .loopBehavior(CountdownLoop.NONE)
                .startingValue(8)
                .currentValue(8)
                .displayOrder(0)
                .build());
    }

    // ==================== LIST — AUTHORIZATION ====================

    @Test
    void listCountdowns_AsCreator_Returns200() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("The ritual completes"));
    }

    @Test
    void listCountdowns_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", gm2Token)))
                .andExpect(status().isOk());
    }

    @Test
    void listCountdowns_AsModerator_Returns200() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void listCountdowns_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCountdowns_AsOutsider_Returns403() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCountdowns_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", testCampaign.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCountdowns_UnknownCampaign_Returns404() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("campaignId", "999999")
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE ====================

    @Test
    void createCountdown_AsCreator_Returns201AndPersists() throws Exception {
        mockMvc.perform(postCountdown(createRequest("Reinforcements arrive", 4), creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentValue").value(4));

        assertThat(countdownRepository.findByCampaignId(testCampaign.getId())).hasSize(2);
    }

    @Test
    void createCountdown_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(postCountdown(createRequest("Reinforcements arrive", 4), player1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCountdown_AsOutsider_Returns403() throws Exception {
        mockMvc.perform(postCountdown(createRequest("Reinforcements arrive", 4), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCountdown_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest("X", 4))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCountdown_StartingValueBelowMinimum_Returns400() throws Exception {
        mockMvc.perform(postCountdown(createRequest("Too small", 0), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCountdown_StartingValueAboveMaximum_Returns400() throws Exception {
        mockMvc.perform(postCountdown(createRequest("Too big", 100), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCountdown_BlankName_Returns400() throws Exception {
        mockMvc.perform(postCountdown(createRequest("   ", 4), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCountdown_EndedCampaign_Returns400() throws Exception {
        testCampaign.endCampaign();
        campaignRepository.save(testCampaign);

        mockMvc.perform(postCountdown(createRequest("Too late", 4), creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCountdown_StripsScriptFromNote() throws Exception {
        CreateCountdownRequest request = createRequest("With note", 4);
        request.setNote("<script>alert('xss')</script>The gate opens");

        mockMvc.perform(postCountdown(request, creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value(not(containsString("<script>"))));
    }

    // ==================== TICK ====================

    @Test
    void updateValue_AsCreator_Returns200AndPersists() throws Exception {
        mockMvc.perform(patchValue(testCountdown.getId(), 5, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(5));

        assertThat(countdownRepository.findById(testCountdown.getId()).orElseThrow().getCurrentValue())
                .isEqualTo(5);
    }

    @Test
    void updateValue_AsGameMaster_Returns200() throws Exception {
        mockMvc.perform(patchValue(testCountdown.getId(), 5, gm2Token))
                .andExpect(status().isOk());
    }

    @Test
    void updateValue_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(patchValue(testCountdown.getId(), 5, player1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateValue_NegativeValue_Returns400() throws Exception {
        mockMvc.perform(patchValue(testCountdown.getId(), -1, creatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateValue_UnknownCountdown_Returns404() throws Exception {
        mockMvc.perform(patchValue(999999L, 3, creatorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateValue_ToZeroOnLoopingCountdown_ResetsToStartingValue() throws Exception {
        Countdown looping = countdownRepository.save(Countdown.builder()
                .campaign(testCampaign).name("Patrol returns").type(CountdownType.STANDARD)
                .loopBehavior(CountdownLoop.LOOP).startingValue(3).currentValue(1).displayOrder(1).build());

        mockMvc.perform(patchValue(looping.getId(), 0, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(3));
    }

    @Test
    void updateValue_ToZeroOnNonLoopingCountdown_StaysAtZero() throws Exception {
        mockMvc.perform(patchValue(testCountdown.getId(), 0, creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(0));
    }

    @Test
    void updateValue_OnCountdownInAnotherCampaign_Returns403() throws Exception {
        User otherGm = createUserWithRole("cd-other-gm", "cd-other-gm@example.com", Role.USER);
        Campaign otherCampaign = createCampaign("Someone else's campaign", otherGm);
        Countdown otherCountdown = countdownRepository.save(Countdown.builder()
                .campaign(otherCampaign).name("Not yours").type(CountdownType.STANDARD)
                .loopBehavior(CountdownLoop.NONE).startingValue(4).currentValue(4).displayOrder(0).build());

        mockMvc.perform(patchValue(otherCountdown.getId(), 1, creatorToken))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE DEFINITION ====================

    @Test
    void updateCountdown_AsCreator_Returns200() throws Exception {
        mockMvc.perform(putCountdown(testCountdown.getId(), updateRequest("Renamed", 6), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void updateCountdown_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(putCountdown(testCountdown.getId(), updateRequest("Renamed", 6), player1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCountdown_LoweringStartingValueClampsCurrentValue() throws Exception {
        mockMvc.perform(putCountdown(testCountdown.getId(), updateRequest("Shorter", 3), creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(3));
    }

    @Test
    void updateCountdown_UnknownCountdown_Returns404() throws Exception {
        mockMvc.perform(putCountdown(999999L, updateRequest("Renamed", 6), creatorToken))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ====================

    @Test
    void deleteCountdown_AsCreator_Returns204AndRemoves() throws Exception {
        mockMvc.perform(delete(BY_ID_PATH, testCountdown.getId())
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNoContent());

        assertThat(countdownRepository.findById(testCountdown.getId())).isEmpty();
    }

    @Test
    void deleteCountdown_AsModerator_Returns204() throws Exception {
        mockMvc.perform(delete(BY_ID_PATH, testCountdown.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCountdown_AsPlayer_Returns403() throws Exception {
        mockMvc.perform(delete(BY_ID_PATH, testCountdown.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCountdown_UnknownCountdown_Returns404() throws Exception {
        mockMvc.perform(delete(BY_ID_PATH, 999999L)
                        .cookie(new Cookie("AUTH_TOKEN", creatorToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    private CreateCountdownRequest createRequest(String name, int startingValue) {
        return CreateCountdownRequest.builder()
                .campaignId(testCampaign.getId())
                .name(name)
                .type(CountdownType.PROGRESS)
                .startingValue(startingValue)
                .build();
    }

    private UpdateCountdownRequest updateRequest(String name, int startingValue) {
        return UpdateCountdownRequest.builder()
                .name(name)
                .type(CountdownType.PROGRESS)
                .loopBehavior(CountdownLoop.NONE)
                .startingValue(startingValue)
                .build();
    }

    private MockHttpServletRequestBuilder postCountdown(CreateCountdownRequest request, String token)
            throws Exception {
        return post(BASE_PATH)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder putCountdown(Long id, UpdateCountdownRequest request, String token)
            throws Exception {
        return put(BY_ID_PATH, id)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder patchValue(Long id, int value, String token) throws Exception {
        return patch(VALUE_PATH, id)
                .cookie(new Cookie("AUTH_TOKEN", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UpdateCountdownValueRequest.builder().currentValue(value).build()));
    }

    private String tokenFor(User user) {
        String token = jwtTokenProvider.generateToken(user);
        storeTokenInDatabase(user.getId(), token);
        return token;
    }

    private User createUserWithRole(String username, String email, Role role) {
        return userRepository.save(User.builder().username(username).email(email).role(role).build());
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

    private Campaign createCampaign(String name, User creatorUser) {
        Campaign campaign = Campaign.builder()
                .name(name)
                .description("For countdown tests")
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
