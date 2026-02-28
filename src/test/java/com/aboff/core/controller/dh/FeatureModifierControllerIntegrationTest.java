package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureModifierRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.FeatureModifierRepository;
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
 * Integration tests for FeatureModifierController.
 * Tests all CRUD endpoints for FeatureModifier resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class FeatureModifierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private FeatureModifierRepository featureModifierRepository;

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
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);
    }

    // ==================== GET ALL MODIFIERS TESTS ====================

    @Test
    void getAllModifiers_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        createModifier(ModifierTarget.EVASION, ModifierOperation.ADD, -1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllModifiers_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/feature-modifiers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllModifiers_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 2);
        createModifier(ModifierTarget.EVASION, ModifierOperation.ADD, -1);
        createModifier(ModifierTarget.HIT_POINT_MAX, ModifierOperation.ADD, 5);
        createModifier(ModifierTarget.HOPE_MAX, ModifierOperation.ADD, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getAllModifiers_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        FeatureModifier deletedModifier = createModifier(ModifierTarget.EVASION, ModifierOperation.ADD, -1);
        deletedModifier.setDeletedAt(LocalDateTime.now());
        featureModifierRepository.save(deletedModifier);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].target").value("STRENGTH"));
    }

    @Test
    void getAllModifiers_IncludeDeleted_ReturnsAll() throws Exception {
        // Arrange
        createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        FeatureModifier deletedModifier = createModifier(ModifierTarget.EVASION, ModifierOperation.ADD, -1);
        deletedModifier.setDeletedAt(LocalDateTime.now());
        featureModifierRepository.save(deletedModifier);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers")
                        .param("includeDeleted", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ==================== GET MODIFIER BY ID TESTS ====================

    @Test
    void getModifierById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers/{id}", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modifier.getId()))
                .andExpect(jsonPath("$.target").value("STRENGTH"))
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.value").value(1));
    }

    @Test
    void getModifierById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/feature-modifiers/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getModifierById_DeletedModifier_Returns404() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        modifier.setDeletedAt(LocalDateTime.now());
        featureModifierRepository.save(modifier);

        // Act & Assert
        mockMvc.perform(get("/api/dh/feature-modifiers/{id}", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE MODIFIER TESTS ====================

    @Test
    void createModifier_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.target").value("STRENGTH"))
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.value").value(1));

        assertThat(featureModifierRepository.findAll()).hasSize(1);
    }

    @Test
    void createModifier_AsUser_Returns403() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(featureModifierRepository.findAll()).isEmpty();
    }

    @Test
    void createModifier_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createModifier_NullTarget_Returns400() throws Exception {
        // Arrange
        String json = "{\"target\":null,\"operation\":\"ADD\",\"value\":1}";

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createModifier_NullOperation_Returns400() throws Exception {
        // Arrange
        String json = "{\"target\":\"STRENGTH\",\"operation\":null,\"value\":1}";

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createModifier_NullValue_Returns400() throws Exception {
        // Arrange
        String json = "{\"target\":\"STRENGTH\",\"operation\":\"ADD\",\"value\":null}";

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createModifier_NegativeValue_Returns201() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(-1));
    }

    @Test
    void createModifier_WithSetOperation_Returns201() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.HIT_POINT_MAX)
                .operation(ModifierOperation.SET)
                .value(15)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target").value("HIT_POINT_MAX"))
                .andExpect(jsonPath("$.operation").value("SET"))
                .andExpect(jsonPath("$.value").value(15));
    }

    @Test
    void createModifier_WithMultiplyOperation_Returns201() throws Exception {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.GOLD)
                .operation(ModifierOperation.MULTIPLY)
                .value(2)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target").value("GOLD"))
                .andExpect(jsonPath("$.operation").value("MULTIPLY"))
                .andExpect(jsonPath("$.value").value(2));
    }

    // ==================== DELETE MODIFIER TESTS ====================

    @Test
    void deleteModifier_AsAdmin_Returns204() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/feature-modifiers/{id}", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        FeatureModifier deleted = featureModifierRepository.findById(modifier.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteModifier_AsUser_Returns403() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/feature-modifiers/{id}", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        FeatureModifier notDeleted = featureModifierRepository.findById(modifier.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteModifier_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/feature-modifiers/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE MODIFIER TESTS ====================

    @Test
    void restoreModifier_AsAdmin_Returns200() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        modifier.setDeletedAt(LocalDateTime.now());
        featureModifierRepository.save(modifier);

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers/{id}/restore", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(modifier.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        FeatureModifier restored = featureModifierRepository.findById(modifier.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreModifier_AsUser_Returns403() throws Exception {
        // Arrange
        FeatureModifier modifier = createModifier(ModifierTarget.STRENGTH, ModifierOperation.ADD, 1);
        modifier.setDeletedAt(LocalDateTime.now());
        featureModifierRepository.save(modifier);

        // Act & Assert
        mockMvc.perform(post("/api/dh/feature-modifiers/{id}/restore", modifier.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        FeatureModifier stillDeleted = featureModifierRepository.findById(modifier.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    @Test
    void restoreModifier_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/feature-modifiers/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates and persists a user with the given role.
     *
     * @param username The username for the user
     * @param email The email for the user
     * @param role The role to assign
     * @return The persisted User entity
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
     * @param userId The user ID to associate with the token
     * @param token The JWT token string
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
     * Creates and persists a FeatureModifier with the given attributes.
     *
     * @param target The modifier target
     * @param operation The modifier operation
     * @param value The modifier value
     * @return The persisted FeatureModifier entity
     */
    private FeatureModifier createModifier(ModifierTarget target, ModifierOperation operation, Integer value) {
        FeatureModifier modifier = FeatureModifier.builder()
                .target(target)
                .operation(operation)
                .value(value)
                .build();
        return featureModifierRepository.save(modifier);
    }
}
