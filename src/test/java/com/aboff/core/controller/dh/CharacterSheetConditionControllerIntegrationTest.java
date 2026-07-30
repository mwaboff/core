package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetConditionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.CharacterSheetCondition;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CharacterSheetConditionRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ConditionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CharacterSheetConditionController.
 * Tests CRUD endpoints for a character's per-instance conditions, proving the magnitude
 * round-trips and that owner/moderator/unauthorized access control holds.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CharacterSheetConditionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private CharacterSheetConditionRepository characterSheetConditionRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User player1;
    private User player2;
    private User moderator;
    private String player1Token;
    private String player2Token;
    private String moderatorToken;
    private CharacterSheet testSheet;
    private Condition ignited;

    @BeforeEach
    void setUp() {
        player1 = createUserWithRole("player1", "player1@example.com", Role.USER);
        player2 = createUserWithRole("player2", "player2@example.com", Role.USER);
        moderator = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);

        player1Token = jwtTokenProvider.generateToken(player1);
        player2Token = jwtTokenProvider.generateToken(player2);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(player2.getId(), player2Token);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        Expansion expansion = createExpansion("Hope & Fear", true);
        testSheet = createCharacterSheet("Aragorn", player1, 5);
        ignited = createCondition("Ignited", expansion, true);
    }

    // ==================== GET TESTS ====================

    @Test
    void getConditionsForCharacterSheet_AsAuthenticatedUser_Returns200() throws Exception {
        createCharacterSheetCondition(testSheet, ignited, 2);

        mockMvc.perform(get("/api/dh/character-sheet-conditions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].magnitude").value(2));
    }

    @Test
    void getConditionsForCharacterSheet_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheet-conditions").param("characterSheetId", "1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CREATE TESTS — the magnitude round-trip ====================

    @Test
    void createCharacterSheetCondition_ValidRequest_Returns201AndRoundTripsMagnitude() throws Exception {
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(testSheet.getId())
                .conditionId(ignited.getId())
                .magnitude(3)
                .build();

        String createResponse = mockMvc.perform(post("/api/dh/character-sheet-conditions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()))
                .andExpect(jsonPath("$.conditionId").value(ignited.getId()))
                .andExpect(jsonPath("$.magnitude").value(3))
                .andReturn().getResponse().getContentAsString();

        assertThat(characterSheetConditionRepository.count()).isEqualTo(1);

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // Round-trip: create -> get returns the same magnitude
        mockMvc.perform(get("/api/dh/character-sheet-conditions/" + createdId)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.magnitude").value(3));
    }

    @Test
    void createCharacterSheetCondition_CharacterSheetNotFound_Returns404() throws Exception {
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(999999L)
                .conditionId(ignited.getId())
                .build();

        mockMvc.perform(post("/api/dh/character-sheet-conditions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== UPDATE TESTS — permission model ====================

    @Test
    void updateCharacterSheetCondition_AsOwner_Returns200() throws Exception {
        CharacterSheetCondition instance = createCharacterSheetCondition(testSheet, ignited, 1);

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(4)
                .build();

        mockMvc.perform(put("/api/dh/character-sheet-conditions/" + instance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.magnitude").value(4));
    }

    @Test
    void updateCharacterSheetCondition_AsModerator_Returns200() throws Exception {
        CharacterSheetCondition instance = createCharacterSheetCondition(testSheet, ignited, 1);

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(5)
                .build();

        mockMvc.perform(put("/api/dh/character-sheet-conditions/" + instance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.magnitude").value(5));
    }

    @Test
    void updateCharacterSheetCondition_AsUnrelatedUser_Returns403() throws Exception {
        CharacterSheetCondition instance = createCharacterSheetCondition(testSheet, ignited, 1);

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(5)
                .build();

        mockMvc.perform(put("/api/dh/character-sheet-conditions/" + instance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteCharacterSheetCondition_AsOwner_Returns204() throws Exception {
        CharacterSheetCondition instance = createCharacterSheetCondition(testSheet, ignited, 1);

        mockMvc.perform(delete("/api/dh/character-sheet-conditions/" + instance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        assertThat(characterSheetConditionRepository.findById(instance.getId())).isEmpty();
    }

    @Test
    void deleteCharacterSheetCondition_AsUnrelatedUser_Returns403() throws Exception {
        CharacterSheetCondition instance = createCharacterSheetCondition(testSheet, ignited, 1);

        mockMvc.perform(delete("/api/dh/character-sheet-conditions/" + instance.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        assertThat(characterSheetConditionRepository.findById(instance.getId())).isPresent();
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

    private Condition createCondition(String name, Expansion expansion, boolean isOfficial) {
        Condition condition = Condition.builder()
                .name(name)
                .description("Test rules text for " + name)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .build();
        return conditionRepository.save(condition);
    }

    private CharacterSheetCondition createCharacterSheetCondition(CharacterSheet sheet, Condition condition, Integer magnitude) {
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .characterSheet(sheet)
                .condition(condition)
                .magnitude(magnitude)
                .build();
        return characterSheetConditionRepository.save(instance);
    }

    private CharacterSheet createCharacterSheet(String name, User owner, Integer level) {
        CharacterSheet sheet = CharacterSheet.builder()
                .name(name)
                .owner(owner)
                .level(level)
                .evasion(0)
                .armorMax(0)
                .armorMarked(0)
                .majorDamageThreshold(5)
                .severeDamageThreshold(10)
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
                .hitPointMax(6)
                .hitPointMarked(0)
                .stressMax(6)
                .stressMarked(0)
                .hopeMax(2)
                .hopeMarked(0)
                .gold(0)
                .build();
        return characterSheetRepository.save(sheet);
    }
}
