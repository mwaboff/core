package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CardCostTagRepository;
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
 * Integration tests for CardCostTagController.
 * Tests all CRUD endpoints for CardCostTag resources with proper authentication and authorization.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CardCostTagControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CardCostTagRepository cardCostTagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        activeTokenRepository.deleteAll();
        cardCostTagRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);
    }

    // ==================== GET ALL COST TAGS TESTS ====================

    @Test
    void getAllCostTags_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createCostTag("3 Hope", CostTagCategory.COST);
        createCostTag("1/session", CostTagCategory.TIMING);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllCostTags_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/cost-tags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCostTags_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createCostTag("Tag " + i, CostTagCategory.COST);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/cost-tags")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getAllCostTags_FilterByCategory_ReturnsFiltered() throws Exception {
        // Arrange
        createCostTag("3 Hope", CostTagCategory.COST);
        createCostTag("1/session", CostTagCategory.TIMING);
        createCostTag("Close range", CostTagCategory.LIMITATION);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cost-tags")
                        .param("category", "COST")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].category").value("COST"));
    }

    @Test
    void getAllCostTags_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createCostTag("Active Tag", CostTagCategory.COST);
        CardCostTag deletedTag = createCostTag("Deleted Tag", CostTagCategory.TIMING);
        deletedTag.setDeletedAt(LocalDateTime.now());
        cardCostTagRepository.save(deletedTag);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].label").value("Active Tag"));
    }

    // ==================== GET COST TAG BY ID TESTS ====================

    @Test
    void getCostTagById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("3 Hope", CostTagCategory.COST);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cost-tags/{id}", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag.getId()))
                .andExpect(jsonPath("$.label").value("3 Hope"))
                .andExpect(jsonPath("$.category").value("COST"));
    }

    @Test
    void getCostTagById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/cost-tags/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE COST TAG TESTS ====================

    @Test
    void createCostTag_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateCardCostTagRequest request = CreateCardCostTagRequest.builder()
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.label").value("3 Hope"))
                .andExpect(jsonPath("$.category").value("COST"));

        assertThat(cardCostTagRepository.findAll()).hasSize(1);
    }

    @Test
    void createCostTag_AsUser_Returns403() throws Exception {
        // Arrange
        CreateCardCostTagRequest request = CreateCardCostTagRequest.builder()
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(cardCostTagRepository.findAll()).isEmpty();
    }

    @Test
    void createCostTag_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateCardCostTagRequest request = CreateCardCostTagRequest.builder()
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCostTag_BlankLabel_Returns400() throws Exception {
        // Arrange
        CreateCardCostTagRequest request = CreateCardCostTagRequest.builder()
                .label("")
                .category(CostTagCategory.COST)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCostTag_NullCategory_Returns400() throws Exception {
        // Arrange - send JSON with null category
        String json = "{\"label\":\"3 Hope\",\"category\":null}";

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE COST TAG TESTS ====================

    @Test
    void updateCostTag_AsAdmin_Returns200() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("Original Label", CostTagCategory.COST);
        UpdateCardCostTagRequest request = UpdateCardCostTagRequest.builder()
                .label("Updated Label")
                .category(CostTagCategory.TIMING)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cost-tags/{id}", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag.getId()))
                .andExpect(jsonPath("$.label").value("Updated Label"))
                .andExpect(jsonPath("$.category").value("TIMING"));

        CardCostTag updated = cardCostTagRepository.findById(tag.getId()).orElseThrow();
        assertThat(updated.getLabel()).isEqualTo("Updated Label");
    }

    @Test
    void updateCostTag_AsUser_Returns403() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("Original Label", CostTagCategory.COST);
        UpdateCardCostTagRequest request = UpdateCardCostTagRequest.builder()
                .label("Updated Label")
                .category(CostTagCategory.TIMING)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cost-tags/{id}", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCostTag_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateCardCostTagRequest request = UpdateCardCostTagRequest.builder()
                .label("Updated Label")
                .category(CostTagCategory.TIMING)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cost-tags/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE COST TAG TESTS ====================

    @Test
    void deleteCostTag_AsAdmin_Returns204() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("To Delete", CostTagCategory.COST);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cost-tags/{id}", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        CardCostTag deleted = cardCostTagRepository.findById(tag.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteCostTag_AsUser_Returns403() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("To Delete", CostTagCategory.COST);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cost-tags/{id}", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        CardCostTag notDeleted = cardCostTagRepository.findById(tag.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteCostTag_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/cost-tags/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE COST TAG TESTS ====================

    @Test
    void restoreCostTag_AsAdmin_Returns200() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("Deleted Tag", CostTagCategory.COST);
        tag.setDeletedAt(LocalDateTime.now());
        cardCostTagRepository.save(tag);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags/{id}/restore", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tag.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        CardCostTag restored = cardCostTagRepository.findById(tag.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreCostTag_AsUser_Returns403() throws Exception {
        // Arrange
        CardCostTag tag = createCostTag("Deleted Tag", CostTagCategory.COST);
        tag.setDeletedAt(LocalDateTime.now());
        cardCostTagRepository.save(tag);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cost-tags/{id}/restore", tag.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        CardCostTag stillDeleted = cardCostTagRepository.findById(tag.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    @Test
    void restoreCostTag_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/cost-tags/{id}/restore", 99999L)
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

    private CardCostTag createCostTag(String label, CostTagCategory category) {
        CardCostTag tag = CardCostTag.builder()
                .label(label)
                .category(category)
                .build();
        return cardCostTagRepository.save(tag);
    }
}
