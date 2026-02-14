package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CommunityCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CommunityCardController.
 * Tests all CRUD endpoints for CommunityCard resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CommunityCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CommunityCardRepository communityCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;

    @BeforeEach
    void setUp() {
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL ANCESTRY CARDS TESTS ====================

    @Test
    void getAllCommunityCards_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createCommunityCard("Elf", "Elven community", testExpansion, true);
        createCommunityCard("Dwarf", "Dwarven community", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllCommunityCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/cards/community"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCommunityCards_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createCommunityCard("Card " + i, "Description " + i, testExpansion, true);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void getAllCommunityCards_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createCommunityCard("Card 1", "Desc 1", testExpansion, true);
        createCommunityCard("Card 2", "Desc 2", expansion2, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllCommunityCards_FilterByIsOfficial_ReturnsFiltered() throws Exception {
        // Arrange
        createCommunityCard("Official Card", "Official", testExpansion, true);
        createCommunityCard("Unofficial Card", "Unofficial", testExpansion, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community")
                        .param("isOfficial", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Official Card"));
    }

    @Test
    void getAllCommunityCards_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createCommunityCard("Elf", "Elven community", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET ANCESTRY CARD BY ID TESTS ====================

    @Test
    void getCommunityCardById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "Elven community", testExpansion, true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/community/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Elf"))
                .andExpect(jsonPath("$.isOfficial").value(true));
    }

    @Test
    void getCommunityCardById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/cards/community/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE ANCESTRY CARD TESTS ====================

    @Test
    void createCommunityCard_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateCommunityCardRequest request = CreateCommunityCardRequest.builder()
                .name("Elf")
                .description("Elven community")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Elf"))
                .andExpect(jsonPath("$.isOfficial").value(true));

        assertThat(communityCardRepository.findAll()).hasSize(1);
    }

    @Test
    void createCommunityCard_AsUser_Returns403() throws Exception {
        // Arrange
        CreateCommunityCardRequest request = CreateCommunityCardRequest.builder()
                .name("Elf")
                .description("Elven community")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(communityCardRepository.findAll()).isEmpty();
    }

    // ==================== CREATE ANCESTRY CARDS BULK TESTS ====================

    @Test
    void createCommunityCardsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateCommunityCardRequest request1 = CreateCommunityCardRequest.builder()
                .name("Elf")
                .description("Elven community")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        CreateCommunityCardRequest request2 = CreateCommunityCardRequest.builder()
                .name("Dwarf")
                .description("Dwarven community")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        List<CreateCommunityCardRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(communityCardRepository.findAll()).hasSize(2);
    }

    @Test
    void createCommunityCardsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateCommunityCardRequest request = CreateCommunityCardRequest.builder()
                .name("Elf")
                .description("Elven community")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();
        List<CreateCommunityCardRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE ANCESTRY CARD TESTS ====================

    @Test
    void updateCommunityCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "Original description", testExpansion, true);
        UpdateCommunityCardRequest request = UpdateCommunityCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(false)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/community/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("High Elf"))
                .andExpect(jsonPath("$.isOfficial").value(false));
    }

    @Test
    void updateCommunityCard_AsUser_Returns403() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "Original description", testExpansion, true);
        UpdateCommunityCardRequest request = UpdateCommunityCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/community/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCommunityCard_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateCommunityCardRequest request = UpdateCommunityCardRequest.builder()
                .name("High Elf")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/community/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ANCESTRY CARD TESTS ====================

    @Test
    void deleteCommunityCard_AsAdmin_Returns204() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "To delete", testExpansion, true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/community/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        CommunityCard deleted = communityCardRepository.findById(card.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteCommunityCard_AsUser_Returns403() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "To delete", testExpansion, true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/community/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        CommunityCard notDeleted = communityCardRepository.findById(card.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteCommunityCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/cards/community/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE ANCESTRY CARD TESTS ====================

    @Test
    void restoreCommunityCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "Deleted card", testExpansion, true);
        card.setDeletedAt(LocalDateTime.now());
        communityCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        CommunityCard restored = communityCardRepository.findById(card.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreCommunityCard_AsUser_Returns403() throws Exception {
        // Arrange
        CommunityCard card = createCommunityCard("Elf", "Deleted card", testExpansion, true);
        card.setDeletedAt(LocalDateTime.now());
        communityCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/community/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreCommunityCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/cards/community/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
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

    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    private CommunityCard createCommunityCard(String name, String description, Expansion expansion, Boolean isOfficial) {
        CommunityCard card = CommunityCard.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .build();
        return communityCardRepository.save(card);
    }
}
