package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateExpansionRequest;
import com.aboff.core.model.dto.dh.request.UpdateExpansionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ExpansionController.
 * Tests all CRUD endpoints for Expansion resources with proper authentication and authorization.
 * <p>
 * Follows the AAA (Arrange-Act-Assert) testing pattern and verifies:
 * - GET endpoints work for authenticated users
 * - POST/PUT/DELETE endpoints require ADMIN or OWNER role
 * - Proper pagination and filtering
 * - Error handling for invalid requests
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ExpansionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

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

    @BeforeEach
    void setUp() {
        // Create test users with different roles
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        // Generate JWT tokens for each user
        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        // Store token hashes in database for validation
        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);
    }

    // ==================== GET ALL EXPANSIONS TESTS ====================

    /**
     * Tests retrieving all expansions as an authenticated user.
     * Verifies pagination works correctly and returns expected data.
     */
    @Test
    void getAllExpansions_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Expansion expansion1 = createExpansion("Core Rulebook", true);
        Expansion expansion2 = createExpansion("Twilight Mirage", false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(expansion1.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Core Rulebook"))
                .andExpect(jsonPath("$.content[1].id").value(expansion2.getId()));
    }

    /**
     * Tests retrieving expansions without authentication.
     * Should return 401 Unauthorized.
     */
    @Test
    void getAllExpansions_Unauthenticated_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests pagination parameters work correctly.
     */
    @Test
    void getAllExpansions_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange - Create 5 expansions
        for (int i = 1; i <= 5; i++) {
            createExpansion("Expansion " + i, true);
        }

        // Act & Assert - Request page 1 with size 2
        mockMvc.perform(get("/api/dh/expansions")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.currentPage").value(1));
    }

    /**
     * Tests filtering by published status.
     */
    @Test
    void getAllExpansions_FilterByPublished_ReturnsOnlyPublished() throws Exception {
        // Arrange
        createExpansion("Published Expansion", true);
        createExpansion("Unpublished Expansion", false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions")
                        .param("published", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Published Expansion"))
                .andExpect(jsonPath("$.content[0].isPublished").value(true));
    }

    /**
     * Tests that soft-deleted expansions are excluded by default.
     */
    @Test
    void getAllExpansions_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createExpansion("Active Expansion", true);
        Expansion deletedExpansion = createExpansion("Deleted Expansion", true);
        deletedExpansion.setDeletedAt(LocalDateTime.now());
        expansionRepository.save(deletedExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Active Expansion"));
    }

    /**
     * Tests that admins can include deleted expansions.
     */
    @Test
    void getAllExpansions_IncludeDeleted_AsAdmin_ReturnsAll() throws Exception {
        // Arrange
        createExpansion("Active Expansion", true);
        Expansion deletedExpansion = createExpansion("Deleted Expansion", true);
        deletedExpansion.setDeletedAt(LocalDateTime.now());
        expansionRepository.save(deletedExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions")
                        .param("includeDeleted", "true")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    // ==================== GET EXPANSION BY ID TESTS ====================

    /**
     * Tests retrieving a single expansion by ID as authenticated user.
     */
    @Test
    void getExpansionById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Core Rulebook", true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions/{id}", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expansion.getId()))
                .andExpect(jsonPath("$.name").value("Core Rulebook"))
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    /**
     * Tests retrieving non-existent expansion returns 404.
     */
    @Test
    void getExpansionById_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests retrieving expansion without authentication.
     */
    @Test
    void getExpansionById_Unauthenticated_Returns401() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Core Rulebook", true);

        // Act & Assert
        mockMvc.perform(get("/api/dh/expansions/{id}", expansion.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CREATE EXPANSION TESTS ====================

    /**
     * Tests creating a new expansion as admin.
     */
    @Test
    void createExpansion_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateExpansionRequest request = CreateExpansionRequest.builder()
                .name("New Expansion")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Expansion"))
                .andExpect(jsonPath("$.isPublished").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify expansion was created in database
        assertThat(expansionRepository.findAll()).hasSize(1);
        Expansion saved = expansionRepository.findAll().get(0);
        assertThat(saved.getName()).isEqualTo("New Expansion");
        assertThat(saved.getIsPublished()).isTrue();
    }

    /**
     * Tests creating expansion as regular user returns 403.
     */
    @Test
    void createExpansion_AsUser_Returns403() throws Exception {
        // Arrange
        CreateExpansionRequest request = CreateExpansionRequest.builder()
                .name("New Expansion")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify expansion was NOT created
        assertThat(expansionRepository.findAll()).isEmpty();
    }

    /**
     * Tests creating expansion without authentication returns 401.
     */
    @Test
    void createExpansion_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateExpansionRequest request = CreateExpansionRequest.builder()
                .name("New Expansion")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests creating expansion with invalid data returns 400.
     */
    @Test
    void createExpansion_InvalidData_Returns400() throws Exception {
        // Arrange - Missing required name field
        CreateExpansionRequest request = CreateExpansionRequest.builder()
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE EXPANSION TESTS ====================

    /**
     * Tests updating an expansion as admin.
     */
    @Test
    void updateExpansion_AsAdmin_Returns200() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Original Name", false);
        UpdateExpansionRequest request = UpdateExpansionRequest.builder()
                .name("Updated Name")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/expansions/{id}", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expansion.getId()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.isPublished").value(true));

        // Verify expansion was updated in database
        Expansion updated = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getIsPublished()).isTrue();
    }

    /**
     * Tests updating expansion as regular user returns 403.
     */
    @Test
    void updateExpansion_AsUser_Returns403() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Original Name", false);
        UpdateExpansionRequest request = UpdateExpansionRequest.builder()
                .name("Updated Name")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/expansions/{id}", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify expansion was NOT updated
        Expansion unchanged = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Original Name");
    }

    /**
     * Tests updating non-existent expansion returns 404.
     */
    @Test
    void updateExpansion_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateExpansionRequest request = UpdateExpansionRequest.builder()
                .name("Updated Name")
                .isPublished(true)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/expansions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE EXPANSION TESTS ====================

    /**
     * Tests soft deleting an expansion as admin.
     */
    @Test
    void deleteExpansion_AsAdmin_Returns204() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("To Delete", true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/expansions/{id}", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        // Verify expansion was soft-deleted
        Expansion deleted = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests deleting expansion as regular user returns 403.
     */
    @Test
    void deleteExpansion_AsUser_Returns403() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("To Delete", true);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/expansions/{id}", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify expansion was NOT deleted
        Expansion notDeleted = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    /**
     * Tests deleting non-existent expansion returns 404.
     */
    @Test
    void deleteExpansion_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/dh/expansions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE EXPANSION TESTS ====================

    /**
     * Tests restoring a soft-deleted expansion as admin.
     */
    @Test
    void restoreExpansion_AsAdmin_Returns200() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Deleted Expansion", true);
        expansion.setDeletedAt(LocalDateTime.now());
        expansionRepository.save(expansion);

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions/{id}/restore", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expansion.getId()))
                .andExpect(jsonPath("$.name").value("Deleted Expansion"))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        // Verify expansion was restored
        Expansion restored = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * Tests restoring expansion as regular user returns 403.
     */
    @Test
    void restoreExpansion_AsUser_Returns403() throws Exception {
        // Arrange
        Expansion expansion = createExpansion("Deleted Expansion", true);
        expansion.setDeletedAt(LocalDateTime.now());
        expansionRepository.save(expansion);

        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions/{id}/restore", expansion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify expansion was NOT restored
        Expansion stillDeleted = expansionRepository.findById(expansion.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests restoring non-existent expansion returns 404.
     */
    @Test
    void restoreExpansion_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/dh/expansions/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a test user with the specified role.
     *
     * @param username The username
     * @param email The email address
     * @param role The user role
     * @return The created user
     */
    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Stores a JWT token in the database for authentication.
     *
     * @param userId The user ID
     * @param token The JWT token
     */
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

    /**
     * Creates a test expansion in the database.
     *
     * @param name The expansion name
     * @param isPublished Whether the expansion is published
     * @return The created expansion
     */
    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }
}
