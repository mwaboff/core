package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.SubclassCardRepository;
import com.aboff.core.repository.dh.SubclassPathRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SubclassCardController.
 * Tests all CRUD endpoints for SubclassCard resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SubclassCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private SubclassCardRepository subclassCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private SubclassPathRepository subclassPathRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;
    private Class testClass;
    private SubclassPath testPath;

    @BeforeEach
    void setUp() {
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
        testClass = createClass("Warrior", testExpansion);
        testPath = createSubclassPath("Warden of Renewal", testClass, testExpansion);
    }

    // ==================== GET ALL SUBCLASS CARDS TESTS ====================

    @Test
    void getAllSubclassCards_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createSubclassCard("Berserker", "Berserker path", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        createSubclassCard("Paladin", "Paladin path", testExpansion, true, testPath, SubclassLevel.SPECIALIZATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/subclass")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllSubclassCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/cards/subclass"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllSubclassCards_FilterByLevel_ReturnsFiltered() throws Exception {
        // Arrange
        createSubclassCard("Berserker", "Level 2 subclass", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        createSubclassCard("Paladin", "Level 5 subclass", testExpansion, true, testPath, SubclassLevel.SPECIALIZATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/subclass")
                        .param("level", "FOUNDATION")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].level").value("FOUNDATION"));
    }

    @Test
    void getAllSubclassCards_FilterByAssociatedClassId_ReturnsFiltered() throws Exception {
        // Arrange
        Class class2 = createClass("Mage", testExpansion);
        SubclassPath path2 = createSubclassPath("Arcane Scholar", class2, testExpansion);
        createSubclassCard("Berserker", "Warrior subclass", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        createSubclassCard("Wizard", "Mage subclass", testExpansion, true, path2, SubclassLevel.FOUNDATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/subclass")
                        .param("associatedClassId", testClass.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Berserker"));
    }

    // ==================== GET SUBCLASS CARD BY ID TESTS ====================

    @Test
    void getSubclassCardById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Berserker path", testExpansion, true, testPath, SubclassLevel.FOUNDATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/subclass/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Berserker"))
                .andExpect(jsonPath("$.level").value("FOUNDATION"))
                .andExpect(jsonPath("$.subclassPathId").value(testPath.getId()));
    }

    @Test
    void getSubclassCardById_WithExpand_IncludesSubclassPath() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Berserker path", testExpansion, true, testPath, SubclassLevel.FOUNDATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/subclass/{id}", card.getId())
                        .param("expand", "expansion,subclassPath")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expansion").exists())
                .andExpect(jsonPath("$.subclassPath").exists())
                .andExpect(jsonPath("$.subclassPath.name").value("Warden of Renewal"));
    }

    @Test
    void getSubclassCardById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/cards/subclass/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE SUBCLASS CARD TESTS ====================

    @Test
    void createSubclassCard_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Berserker path")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Berserker"))
                .andExpect(jsonPath("$.level").value("FOUNDATION"))
                .andExpect(jsonPath("$.subclassPathId").value(testPath.getId()));

        assertThat(subclassCardRepository.findAll()).hasSize(1);
    }

    @Test
    void createSubclassCard_AsUser_Returns403() throws Exception {
        // Arrange
        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Berserker path")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(subclassCardRepository.findAll()).isEmpty();
    }

    // ==================== CREATE SUBCLASS CARDS BULK TESTS ====================

    @Test
    void createSubclassCardsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateSubclassCardRequest request1 = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Berserker path")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();
        CreateSubclassCardRequest request2 = CreateSubclassCardRequest.builder()
                .name("Paladin")
                .description("Paladin path")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.SPECIALIZATION)
                .build();
        List<CreateSubclassCardRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(subclassCardRepository.findAll()).hasSize(2);
    }

    @Test
    void createSubclassCardsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Berserker path")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();
        List<CreateSubclassCardRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE SUBCLASS CARD TESTS ====================

    @Test
    void updateSubclassCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Original description", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Warlord")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.SPECIALIZATION)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/subclass/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Warlord"))
                .andExpect(jsonPath("$.level").value("SPECIALIZATION"));
    }

    @Test
    void updateSubclassCard_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Original description", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Warlord")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/subclass/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSubclassCard_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Warlord")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .subclassPathId(testPath.getId())
                .level(SubclassLevel.FOUNDATION)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/subclass/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE SUBCLASS CARD TESTS ====================

    @Test
    void deleteSubclassCard_AsAdmin_Returns204() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "To delete", testExpansion, true, testPath, SubclassLevel.FOUNDATION);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/subclass/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        SubclassCard deleted = subclassCardRepository.findById(card.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteSubclassCard_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "To delete", testExpansion, true, testPath, SubclassLevel.FOUNDATION);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/subclass/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        SubclassCard notDeleted = subclassCardRepository.findById(card.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteSubclassCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/cards/subclass/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE SUBCLASS CARD TESTS ====================

    @Test
    void restoreSubclassCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Deleted card", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        card.setDeletedAt(LocalDateTime.now());
        subclassCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        SubclassCard restored = subclassCardRepository.findById(card.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreSubclassCard_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassCard card = createSubclassCard("Berserker", "Deleted card", testExpansion, true, testPath, SubclassLevel.FOUNDATION);
        card.setDeletedAt(LocalDateTime.now());
        subclassCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/subclass/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreSubclassCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/cards/subclass/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
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

    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    private Class createClass(String name, Expansion expansion) {
        Class clazz = Class.builder()
                .name(name)
                .expansion(expansion)
                .startingEvasion(10)
                .startingHitPoints(20)
                .isOfficial(true)
                .build();
        return classRepository.save(clazz);
    }

    private SubclassPath createSubclassPath(String name, Class clazz, Expansion expansion) {
        SubclassPath path = SubclassPath.builder()
                .name(name)
                .associatedClass(clazz)
                .expansion(expansion)
                .build();
        return subclassPathRepository.save(path);
    }

    private SubclassCard createSubclassCard(String name, String description, Expansion expansion, Boolean isOfficial, SubclassPath path, SubclassLevel level) {
        SubclassCard card = SubclassCard.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .subclassPath(path)
                .level(level)
                .build();
        return subclassCardRepository.save(card);
    }
}
