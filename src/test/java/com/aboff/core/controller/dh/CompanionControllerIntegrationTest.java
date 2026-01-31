package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.CharacterSheetRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CompanionRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CompanionController.
 * Tests all CRUD endpoints for Companion resources with proper authentication and authorization.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CompanionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private CompanionRepository companionRepository;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        activeTokenRepository.deleteAll();
        companionRepository.deleteAll();
        characterSheetRepository.deleteAll();
        userRepository.deleteAll();

        player1 = createUserWithRole("player1", "player1@example.com", Role.USER);
        player2 = createUserWithRole("player2", "player2@example.com", Role.USER);
        moderator = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);

        player1Token = jwtTokenProvider.generateToken(player1);
        player2Token = jwtTokenProvider.generateToken(player2);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(player2.getId(), player2Token);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        testSheet = createCharacterSheet("Aragorn", player1, 5);
    }

    // ==================== GET ALL COMPANIONS TESTS ====================

    @Test
    void getAllCompanions_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createCompanion("Wolf", testSheet);
        createCompanion("Hawk", testSheet);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllCompanions_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/companions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCompanions_FilterByCharacterSheetId_ReturnsFiltered() throws Exception {
        // Arrange
        CharacterSheet sheet2 = createCharacterSheet("Legolas", player2, 4);
        createCompanion("Wolf", testSheet);
        createCompanion("Eagle", sheet2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].characterSheetId").value(testSheet.getId()));
    }

    @Test
    void getAllCompanions_WithExpansion_IncludesExpandedEntities() throws Exception {
        // Arrange
        createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions")
                        .param("expand", "characterSheet")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].characterSheet").isNotEmpty())
                .andExpect(jsonPath("$.content[0].characterSheet.name").value("Aragorn"));
    }

    // ==================== GET COMPANION BY ID TESTS ====================

    @Test
    void getCompanionById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companion.getId()))
                .andExpect(jsonPath("$.name").value("Wolf"))
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()));
    }

    @Test
    void getCompanionById_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCompanionById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/companions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCompanionById_WithExpansion_IncludesExpandedEntities() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .param("expand", "characterSheet")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSheet").isNotEmpty())
                .andExpect(jsonPath("$.characterSheet.name").value("Aragorn"));
    }

    // ==================== CREATE COMPANION TESTS ====================

    @Test
    void createCompanion_AsOwner_Returns201() throws Exception {
        // Arrange
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .description("A loyal wolf companion")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wolf"))
                .andExpect(jsonPath("$.attackName").value("Bite"))
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()));

        assertThat(companionRepository.countByCharacterSheetId(testSheet.getId())).isEqualTo(1);
    }

    @Test
    void createCompanion_AsModerator_Returns201() throws Exception {
        // Arrange
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void createCompanion_AsOtherUser_Returns403() throws Exception {
        // Arrange
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(companionRepository.countByCharacterSheetId(testSheet.getId())).isEqualTo(0);
    }

    @Test
    void createCompanion_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCompanion_WithInvalidCharacterSheetId_Returns404() throws Exception {
        // Arrange
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(99999L)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCompanion_WithMissingRequiredFields_Returns400() throws Exception {
        // Arrange - missing name
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE COMPANION TESTS ====================

    @Test
    void updateCompanion_AsOwner_Returns200() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companion.getId()))
                .andExpect(jsonPath("$.stressMarked").value(2));

        Companion updated = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(updated.getStressMarked()).isEqualTo(2);
    }

    @Test
    void updateCompanion_AsModerator_Returns200() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .name("Shadow Wolf")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shadow Wolf"));
    }

    @Test
    void updateCompanion_AsOtherUser_Returns403() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Companion unchanged = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(unchanged.getStressMarked()).isEqualTo(0);
    }

    @Test
    void updateCompanion_WithPartialUpdate_OnlyUpdatesProvidedFields() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(1)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wolf")) // Unchanged
                .andExpect(jsonPath("$.stressMarked").value(1)); // Changed

        Companion updated = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Wolf");
        assertThat(updated.getStressMarked()).isEqualTo(1);
    }

    @Test
    void updateCompanion_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCompanion_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/companions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE COMPANION TESTS ====================

    @Test
    void deleteCompanion_AsOwner_Returns204() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        assertThat(companionRepository.findById(companion.getId())).isEmpty();
    }

    @Test
    void deleteCompanion_AsModerator_Returns204() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());

        assertThat(companionRepository.findById(companion.getId())).isEmpty();
    }

    @Test
    void deleteCompanion_AsOtherUser_Returns403() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        assertThat(companionRepository.findById(companion.getId())).isPresent();
    }

    @Test
    void deleteCompanion_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Companion companion = createCompanion("Wolf", testSheet);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(companionRepository.findById(companion.getId())).isPresent();
    }

    @Test
    void deleteCompanion_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/companions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
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
                .stressMax(5)
                .stressMarked(0)
                .hopeMax(5)
                .hopeMarked(0)
                .build();
        return characterSheetRepository.save(sheet);
    }

    private Companion createCompanion(String name, CharacterSheet characterSheet) {
        Companion companion = Companion.builder()
                .characterSheet(characterSheet)
                .name(name)
                .description("A " + name + " companion")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .build();
        return companionRepository.save(companion);
    }
}
