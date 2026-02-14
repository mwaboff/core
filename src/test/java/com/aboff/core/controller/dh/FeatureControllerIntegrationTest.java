package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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
 * Integration tests for FeatureController.
 * Tests all CRUD endpoints for Feature resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class FeatureControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private FeatureRepository featureRepository;

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

    // ==================== GET ALL FEATURES TESTS ====================

    @Test
    void getAllFeatures_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createFeature("Hope Feature", FeatureType.HOPE, testExpansion);
        createFeature("Class Feature", FeatureType.CLASS, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllFeatures_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/features"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllFeatures_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createFeature("Feature " + i, FeatureType.OTHER, testExpansion);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getAllFeatures_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createFeature("Feature 1", FeatureType.HOPE, testExpansion);
        createFeature("Feature 2", FeatureType.CLASS, expansion2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Feature 1"));
    }

    @Test
    void getAllFeatures_FilterByFeatureType_ReturnsFiltered() throws Exception {
        // Arrange
        createFeature("Hope Feature", FeatureType.HOPE, testExpansion);
        createFeature("Class Feature", FeatureType.CLASS, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .param("featureType", "HOPE")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].featureType").value("HOPE"));
    }

    @Test
    void getAllFeatures_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createFeature("Test Feature", FeatureType.HOPE, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    @Test
    void getAllFeatures_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createFeature("Active Feature", FeatureType.HOPE, testExpansion);
        Feature deletedFeature = createFeature("Deleted Feature", FeatureType.CLASS, testExpansion);
        deletedFeature.setDeletedAt(LocalDateTime.now());
        featureRepository.save(deletedFeature);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Active Feature"));
    }

    // ==================== GET FEATURE BY ID TESTS ====================

    @Test
    void getFeatureById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Feature feature = createFeature("Hope Feature", FeatureType.HOPE, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features/{id}", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feature.getId()))
                .andExpect(jsonPath("$.name").value("Hope Feature"))
                .andExpect(jsonPath("$.featureType").value("HOPE"));
    }

    @Test
    void getFeatureById_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        Feature feature = createFeature("Hope Feature", FeatureType.HOPE, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/features/{id}", feature.getId())
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expansion").exists())
                .andExpect(jsonPath("$.expansion.name").value("Core Rulebook"));
    }

    @Test
    void getFeatureById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/features/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE FEATURE TESTS ====================

    @Test
    void createFeature_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("New Feature")
                .description("Feature description")
                .featureType(FeatureType.HOPE)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/features")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Feature"))
                .andExpect(jsonPath("$.featureType").value("HOPE"));

        assertThat(featureRepository.findAll()).hasSize(1);
    }

    @Test
    void createFeature_AsUser_Returns403() throws Exception {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("New Feature")
                .description("Feature description")
                .featureType(FeatureType.HOPE)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/features")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(featureRepository.findAll()).isEmpty();
    }

    @Test
    void createFeature_Unauthenticated_Returns401() throws Exception {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("New Feature")
                .description("Feature description")
                .featureType(FeatureType.HOPE)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createFeature_InvalidExpansionId_Returns404() throws Exception {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("New Feature")
                .description("Feature description")
                .featureType(FeatureType.HOPE)
                .expansionId(99999L)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/features")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== UPDATE FEATURE TESTS ====================

    @Test
    void updateFeature_AsAdmin_Returns200() throws Exception {
        // Arrange
        Feature feature = createFeature("Original Name", FeatureType.HOPE, testExpansion);
        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/features/{id}", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feature.getId()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.featureType").value("CLASS"));

        Feature updated = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
    }

    @Test
    void updateFeature_AsUser_Returns403() throws Exception {
        // Arrange
        Feature feature = createFeature("Original Name", FeatureType.HOPE, testExpansion);
        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/features/{id}", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFeature_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/features/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE FEATURE TESTS ====================

    @Test
    void deleteFeature_AsAdmin_Returns204() throws Exception {
        // Arrange
        Feature feature = createFeature("To Delete", FeatureType.HOPE, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/features/{id}", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Feature deleted = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteFeature_AsUser_Returns403() throws Exception {
        // Arrange
        Feature feature = createFeature("To Delete", FeatureType.HOPE, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/features/{id}", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Feature notDeleted = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteFeature_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/features/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE FEATURE TESTS ====================

    @Test
    void restoreFeature_AsAdmin_Returns200() throws Exception {
        // Arrange
        Feature feature = createFeature("Deleted Feature", FeatureType.HOPE, testExpansion);
        feature.setDeletedAt(LocalDateTime.now());
        featureRepository.save(feature);

        // Act & Assert
        mockMvc.perform(post("/api/dh/features/{id}/restore", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feature.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Feature restored = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreFeature_AsUser_Returns403() throws Exception {
        // Arrange
        Feature feature = createFeature("Deleted Feature", FeatureType.HOPE, testExpansion);
        feature.setDeletedAt(LocalDateTime.now());
        featureRepository.save(feature);

        // Act & Assert
        mockMvc.perform(post("/api/dh/features/{id}/restore", feature.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Feature stillDeleted = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    @Test
    void restoreFeature_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/features/{id}/restore", 99999L)
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

    private Feature createFeature(String name, FeatureType featureType, Expansion expansion) {
        Feature feature = Feature.builder()
                .name(name)
                .description("Description for " + name)
                .featureType(featureType)
                .expansion(expansion)
                .build();
        return featureRepository.save(feature);
    }
}
