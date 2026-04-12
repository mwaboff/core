package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateExperienceRequest;
import com.aboff.core.model.dto.dh.request.UpdateExperienceRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ExperienceRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ExperienceController.
 * Tests all CRUD endpoints for Experience resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ExperienceControllerIntegrationTest {

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

    // ==================== GET ALL EXPERIENCES TESTS ====================

    @Test
    void getAllExperiences_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createExperience("Survived dragon attack", 2, testSheet, player1);
        createExperience("Negotiated peace treaty", 3, testSheet, player1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllExperiences_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/experiences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllExperiences_FilterByCharacterSheetId_ReturnsFiltered() throws Exception {
        // Arrange
        CharacterSheet sheet2 = createCharacterSheet("Legolas", player2, 4);
        createExperience("Survived dragon attack", 2, testSheet, player1);
        createExperience("Won archery contest", 2, sheet2, player2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/experiences")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].characterSheetId").value(testSheet.getId()));
    }

    @Test
    void getAllExperiences_WithExpansion_IncludesExpandedEntities() throws Exception {
        // Arrange
        createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/experiences")
                        .param("expand", "characterSheet,createdBy")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].characterSheet").exists())
                .andExpect(jsonPath("$.content[0].characterSheet.name").value("Aragorn"))
                .andExpect(jsonPath("$.content[0].createdBy").exists())
                .andExpect(jsonPath("$.content[0].createdBy.username").value("player1"));
    }

    @Test
    void getAllExperiences_WithInvalidCharacterSheetFilter_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/experiences")
                        .param("characterSheetId", "99999")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET EXPERIENCE BY ID TESTS ====================

    @Test
    void getExperienceById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exp.getId()))
                .andExpect(jsonPath("$.description").value("Survived dragon attack"))
                .andExpect(jsonPath("$.modifier").value(2))
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()))
                .andExpect(jsonPath("$.createdById").value(player1.getId()));
    }

    @Test
    void getExperienceById_WithExpansion_IncludesExpandedEntities() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/experiences/{id}", exp.getId())
                        .param("expand", "characterSheet,createdBy")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSheet").exists())
                .andExpect(jsonPath("$.characterSheet.name").value("Aragorn"))
                .andExpect(jsonPath("$.createdBy").exists())
                .andExpect(jsonPath("$.createdBy.username").value("player1"));
    }

    @Test
    void getExperienceById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/experiences/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExperienceById_Unauthenticated_Returns401() throws Exception {
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        mockMvc.perform(get("/api/dh/experiences/{id}", exp.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CREATE EXPERIENCE TESTS ====================

    @Test
    void createExperience_AsAuthenticatedUser_Returns201() throws Exception {
        // Arrange
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(testSheet.getId())
                .description("Survived dragon attack on Redstone Village")
                .modifier(2)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Survived dragon attack on Redstone Village"))
                .andExpect(jsonPath("$.modifier").value(2))
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()))
                .andExpect(jsonPath("$.createdById").value(player1.getId()));

        assertThat(experienceRepository.findAll()).hasSize(1);
    }

    @Test
    void createExperience_WithDefaultModifier_UsesDefaultValue() throws Exception {
        // Arrange
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(testSheet.getId())
                .description("Survived dragon attack")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modifier").value(2));
    }

    @Test
    void createExperience_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(testSheet.getId())
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(experienceRepository.findAll()).isEmpty();
    }

    @Test
    void createExperience_WithInvalidCharacterSheetId_Returns404() throws Exception {
        // Arrange
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(99999L)
                .description("Survived dragon attack")
                .modifier(2)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createExperience_WithMissingRequiredFields_Returns400() throws Exception {
        // Arrange
        CreateExperienceRequest request = CreateExperienceRequest.builder()
                .characterSheetId(testSheet.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE EXPERIENCE TESTS ====================

    @Test
    void updateExperience_AsOwner_Returns200() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player2);
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .modifier(3)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exp.getId()))
                .andExpect(jsonPath("$.description").value("Survived dragon attack on Redstone Village"))
                .andExpect(jsonPath("$.modifier").value(3));

        Experience updated = experienceRepository.findById(exp.getId()).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("Survived dragon attack on Redstone Village");
        assertThat(updated.getModifier()).isEqualTo(3);
    }

    @Test
    void updateExperience_AsModerator_Returns200() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Survived dragon attack on Redstone Village"));
    }

    @Test
    void updateExperience_AsOtherUser_Returns403() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Experience unchanged = experienceRepository.findById(exp.getId()).orElseThrow();
        assertThat(unchanged.getDescription()).isEqualTo("Survived dragon attack");
    }

    @Test
    void updateExperience_WithPartialUpdate_OnlyUpdatesProvidedFields() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Survived dragon attack on Redstone Village"))
                .andExpect(jsonPath("$.modifier").value(2));
    }

    @Test
    void updateExperience_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", exp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateExperience_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateExperienceRequest request = UpdateExperienceRequest.builder()
                .description("Survived dragon attack on Redstone Village")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/experiences/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE EXPERIENCE TESTS ====================

    @Test
    void deleteExperience_AsOwner_Returns204() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player2);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        assertThat(experienceRepository.findById(exp.getId())).isEmpty();
    }

    @Test
    void deleteExperience_AsModerator_Returns204() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());

        assertThat(experienceRepository.findById(exp.getId())).isEmpty();
    }

    @Test
    void deleteExperience_AsOtherUser_Returns403() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/experiences/{id}", exp.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        assertThat(experienceRepository.findById(exp.getId())).isPresent();
    }

    @Test
    void deleteExperience_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/experiences/{id}", exp.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(experienceRepository.findById(exp.getId())).isPresent();
    }

    @Test
    void deleteExperience_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/experiences/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
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

    private Experience createExperience(String description, Integer modifier, CharacterSheet sheet, User createdBy) {
        Experience exp = Experience.builder()
                .description(description)
                .modifier(modifier)
                .characterSheet(sheet)
                .createdBy(createdBy)
                .build();
        return experienceRepository.save(exp);
    }
}
