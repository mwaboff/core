package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.AdvancementChoice;
import com.aboff.core.model.dto.dh.request.LevelUpRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.enums.*;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for LevelUp endpoints on CharacterSheetController.
 * Tests level-up options, level-up execution, undo, and access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LevelUpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private DomainCardRepository domainCardRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private SubclassCardRepository subclassCardRepository;

    @Autowired
    private SubclassPathRepository subclassPathRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CharacterSheetDomainCardRepository characterSheetDomainCardRepository;

    @Autowired
    private CharacterAdvancementLogRepository characterAdvancementLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User player1;
    private User player2;
    private User moderator;
    private String player1Token;
    private String player2Token;
    private String moderatorToken;
    private CharacterSheet testSheet;
    private Expansion testExpansion;
    private Domain testDomain;
    private Class testClass;
    private SubclassPath testPath;

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

        // Set up game content
        testExpansion = expansionRepository.save(Expansion.builder()
                .name("Core Rules").isPublished(true).build());
        testDomain = domainRepository.save(Domain.builder()
                .name("Arcana").expansion(testExpansion).build());
        testClass = classRepository.save(Class.builder()
                .name("Wizard").expansion(testExpansion).startingEvasion(8).startingHitPoints(6).build());
        testPath = subclassPathRepository.save(SubclassPath.builder()
                .name("Pyromancer").associatedClass(testClass).expansion(testExpansion)
                .associatedDomains(Set.of(testDomain)).build());

        SubclassCard foundationCard = subclassCardRepository.save(SubclassCard.builder()
                .name("Pyromancer Foundation").level(SubclassLevel.FOUNDATION)
                .subclassPath(testPath).expansion(testExpansion).isOfficial(true).build());

        testSheet = createCharacterSheet("Gandalf", "he/him", 1, player1);
        testSheet.getSubclassCards().add(foundationCard);
        testSheet = characterSheetRepository.save(testSheet);
    }

    // ==================== GET LEVEL-UP OPTIONS TESTS ====================

    @Test
    void getLevelUpOptions_Level1Character_ReturnsCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets/{id}/level-up-options", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLevel").value(1))
                .andExpect(jsonPath("$.nextLevel").value(2))
                .andExpect(jsonPath("$.currentTier").value(1))
                .andExpect(jsonPath("$.nextTier").value(2))
                .andExpect(jsonPath("$.tierTransition").value(true))
                .andExpect(jsonPath("$.availableAdvancements").isArray())
                .andExpect(jsonPath("$.maxEquippedDomainCards").value(5));
    }

    // ==================== LEVEL-UP TESTS ====================

    @Test
    void levelUp_NonTierBoundary_ReturnsUpdatedSheetAtLevel3() throws Exception {
        // First level up to 2 (tier boundary)
        testSheet.setLevel(2);
        testSheet = characterSheetRepository.save(testSheet);

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSheet.level").value(3))
                .andExpect(jsonPath("$.advancementLogId").isNumber())
                .andExpect(jsonPath("$.appliedChanges").isArray());
    }

    @Test
    void levelUp_TierBoundary_CreatesExperienceAndIncrementsProficiency() throws Exception {
        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .newExperienceDescription("Survived the dragon attack")
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSheet.level").value(2));
    }

    @Test
    void levelUp_WithInvalidAdvancements_Returns400() throws Exception {
        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                        // Only 1 advancement - should fail
                ))
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void levelUp_AtMaxLevel_Returns400() throws Exception {
        testSheet.setLevel(10);
        testSheet = characterSheetRepository.save(testSheet);

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    // ==================== UNDO LEVEL-UP TESTS ====================

    @Test
    void undoLevelUp_SucceedsAndReturnsSheetAtPreviousLevel() throws Exception {
        // First do a level up
        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .newExperienceDescription("Test experience")
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk());

        // Now undo
        mockMvc.perform(delete("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void undoLevelUp_WithNoHistory_Returns400() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    // ==================== ACCESS CONTROL TESTS ====================

    @Test
    void levelUp_AsNonOwnerNonModerator_Returns403() throws Exception {
        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .newExperienceDescription("Test")
                .build();

        mockMvc.perform(post("/api/dh/character-sheets/{id}/level-up", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLevelUpOptions_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets/{id}/level-up-options", testSheet.getId()))
                .andExpect(status().isUnauthorized());
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

    private CharacterSheet createCharacterSheet(String name, String pronouns, Integer level, User owner) {
        CharacterSheet sheet = CharacterSheet.builder()
                .name(name)
                .pronouns(pronouns)
                .level(level)
                .proficiency(0)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(10).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0)
                .gold(50)
                .owner(owner)
                .build();
        return characterSheetRepository.save(sheet);
    }
}
