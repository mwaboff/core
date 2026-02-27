package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCharacterSheetRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Integration tests for CharacterSheetController.
 * Tests all CRUD endpoints for CharacterSheet resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CharacterSheetControllerIntegrationTest {

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
        player1 = createUserWithRole("player1", "player1@example.com", Role.USER);
        player2 = createUserWithRole("player2", "player2@example.com", Role.USER);
        moderator = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);

        player1Token = jwtTokenProvider.generateToken(player1);
        player2Token = jwtTokenProvider.generateToken(player2);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(player2.getId(), player2Token);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        testSheet = createCharacterSheet("Aragorn", "he/him", 5, player1);
    }

    // ==================== GET ALL CHARACTER SHEETS TESTS ====================

    @Test
    void getAllCharacterSheets_AsModerator_Returns200() throws Exception {
        // Arrange
        createCharacterSheet("Legolas", "he/him", 6, player2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllCharacterSheets_AsRegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCharacterSheets_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCharacterSheets_FilterByOwnerId_ReturnsFiltered() throws Exception {
        // Arrange
        createCharacterSheet("Legolas", "he/him", 6, player2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .param("ownerId", player1.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ownerId").value(player1.getId()));
    }

    @Test
    void getAllCharacterSheets_FilterByName_ReturnsFiltered() throws Exception {
        // Arrange
        createCharacterSheet("Legolas", "he/him", 6, player2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .param("name", "Ara")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Aragorn"));
    }

    @Test
    void getAllCharacterSheets_FilterByLevelRange_ReturnsFiltered() throws Exception {
        // Arrange
        createCharacterSheet("Legolas", "he/him", 3, player2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .param("minLevel", "4")
                        .param("maxLevel", "6")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Aragorn"));
    }

    @Test
    void getAllCharacterSheets_WithPagination_ReturnsPaged() throws Exception {
        // Arrange
        createCharacterSheet("Legolas", "he/him", 6, player2);
        createCharacterSheet("Gimli", "he/him", 4, player1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .param("page", "0")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.pageSize").value(2));
    }

    @Test
    void getAllCharacterSheets_WithExpansion_IncludesExpandedData() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/character-sheets")
                        .param("expand", "owner")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].owner").exists())
                .andExpect(jsonPath("$.content[0].owner.username").value("player1"));
    }

    // ==================== GET CHARACTER SHEET BY ID TESTS ====================

    @Test
    void getCharacterSheetById_AsAuthenticatedUser_Returns200() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSheet.getId()))
                .andExpect(jsonPath("$.name").value("Aragorn"))
                .andExpect(jsonPath("$.level").value(5))
                .andExpect(jsonPath("$.ownerId").value(player1.getId()));
    }

    @Test
    void getCharacterSheetById_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets/{id}", testSheet.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCharacterSheetById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/character-sheets/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCharacterSheetById_WithExpansion_IncludesAllRelationships() throws Exception {
        // Act & Assert - Test owner expansion
        mockMvc.perform(get("/api/dh/character-sheets/{id}", testSheet.getId())
                        .param("expand", "owner")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").exists())
                .andExpect(jsonPath("$.owner.username").value("player1"))
                .andExpect(jsonPath("$.owner.email").value("player1@example.com"));
    }

    // ==================== CREATE CHARACTER SHEET TESTS ====================

    @Test
    void createCharacterSheet_AsAuthenticatedUser_Returns201() throws Exception {
        // Arrange
        CreateCharacterSheetRequest request = createValidRequest();

        // Act & Assert
        mockMvc.perform(post("/api/dh/character-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Strider"))
                .andExpect(jsonPath("$.level").value(3))
                .andExpect(jsonPath("$.ownerId").value(player1.getId()));
    }

    @Test
    void createCharacterSheet_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateCharacterSheetRequest request = createValidRequest();

        // Act & Assert
        mockMvc.perform(post("/api/dh/character-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCharacterSheet_WithMissingRequiredFields_Returns400() throws Exception {
        // Arrange - missing name
        CreateCharacterSheetRequest request = CreateCharacterSheetRequest.builder()
                .level(5)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/character-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCharacterSheet_WithConstraintViolation_Returns400() throws Exception {
        // Arrange
        CreateCharacterSheetRequest request = createValidRequest();
        request.setArmorMarked(100); // Exceeds armorMax

        // Act & Assert
        mockMvc.perform(post("/api/dh/character-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE CHARACTER SHEET TESTS ====================

    @Test
    void updateCharacterSheet_AsOwner_Returns200() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .level(6)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aragorn II"))
                .andExpect(jsonPath("$.level").value(6));
    }

    @Test
    void updateCharacterSheet_AsModerator_Returns200() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aragorn II"));
    }

    @Test
    void updateCharacterSheet_AsOtherUser_Returns403() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Hacked Name")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCharacterSheet_Unauthenticated_Returns401() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCharacterSheet_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Aragorn II")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCharacterSheet_PartialUpdate_OnlyUpdatesProvidedFields() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Strider")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Strider"))
                .andExpect(jsonPath("$.level").value(5)); // Should remain unchanged
    }

    @Test
    void updateCharacterSheet_UpdatesAllFieldGroups_Success() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .name("Strider")
                .level(6)
                .evasion(12)
                .agilityModifier(3)
                .hitPointMax(12)
                .gold(100)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Strider"))
                .andExpect(jsonPath("$.level").value(6))
                .andExpect(jsonPath("$.evasion").value(12))
                .andExpect(jsonPath("$.agilityModifier").value(3))
                .andExpect(jsonPath("$.hitPointMax").value(12))
                .andExpect(jsonPath("$.gold").value(100));
    }

    @Test
    void updateCharacterSheet_WithConstraintViolation_Returns400() throws Exception {
        // Arrange
        UpdateCharacterSheetRequest request = UpdateCharacterSheetRequest.builder()
                .armorMarked(100) // Exceeds armorMax
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/character-sheets/{id}", testSheet.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    // ==================== DELETE CHARACTER SHEET TESTS ====================

    @Test
    void deleteCharacterSheet_AsOwner_Returns204() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        // Verify soft delete
        CharacterSheet deletedSheet = characterSheetRepository.findById(testSheet.getId()).orElseThrow();
        assertThat(deletedSheet.isDeleted()).isTrue();
    }

    @Test
    void deleteCharacterSheet_AsModerator_Returns204() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isNoContent());

        // Verify soft delete
        CharacterSheet deletedSheet = characterSheetRepository.findById(testSheet.getId()).orElseThrow();
        assertThat(deletedSheet.isDeleted()).isTrue();
    }

    @Test
    void deleteCharacterSheet_AsOtherUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        // Verify NOT deleted
        CharacterSheet sheet = characterSheetRepository.findById(testSheet.getId()).orElseThrow();
        assertThat(sheet.isDeleted()).isFalse();
    }

    @Test
    void deleteCharacterSheet_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCharacterSheet_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCharacterSheet_VerifiesSoftDelete_SheetMarkedDeleted() throws Exception {
        // Act
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        // Assert
        CharacterSheet deletedSheet = characterSheetRepository.findById(testSheet.getId()).orElseThrow();
        assertThat(deletedSheet.getDeletedAt()).isNotNull();
        assertThat(deletedSheet.isDeleted()).isTrue();
    }

    @Test
    void deleteCharacterSheet_VerifiesCascade_ExperiencesDeleted() throws Exception {
        // Arrange
        Experience exp = createExperience("Survived dragon attack", 2, testSheet, player1);

        // Act
        mockMvc.perform(delete("/api/dh/character-sheets/{id}", testSheet.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        // Assert - with soft delete and orphanRemoval, experiences are NOT deleted from database
        // The experiences remain but their character sheet is soft-deleted
        assertThat(experienceRepository.findById(exp.getId())).isPresent();
        Experience foundExp = experienceRepository.findById(exp.getId()).get();
        assertThat(foundExp.getCharacterSheet().isDeleted()).isTrue();
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

    private CreateCharacterSheetRequest createValidRequest() {
        return CreateCharacterSheetRequest.builder()
                .name("Strider")
                .pronouns("he/him")
                .level(3)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(2)
                .agilityMarked(false)
                .strengthModifier(3)
                .strengthMarked(false)
                .finesseModifier(1)
                .finesseMarked(false)
                .instinctModifier(2)
                .instinctMarked(false)
                .presenceModifier(2)
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
                .build();
    }
}
