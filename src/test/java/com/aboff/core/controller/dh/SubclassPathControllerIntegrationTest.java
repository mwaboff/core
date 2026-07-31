package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassPathRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassPathRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SubclassPathController.
 * Tests all CRUD endpoints for SubclassPath resources with proper authentication and authorization.
 * <p>
 * Follows the AAA (Arrange-Act-Assert) testing pattern and verifies:
 * - GET endpoints work for authenticated users
 * - POST/PUT/DELETE endpoints require ADMIN or OWNER role
 * - Proper pagination, filtering, and expand functionality
 * - Error handling for invalid requests
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SubclassPathControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private SubclassPathRepository subclassPathRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private DomainRepository domainRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;
    private Class testClass;

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

        // Create test expansion and class
        testExpansion = createExpansion("Core Rulebook", true);
        testClass = createClass("Druid", testExpansion);
    }

    // ==================== GET ALL SUBCLASS PATHS TESTS ====================

    /**
     * Tests retrieving all subclass paths as an authenticated user.
     */
    @Test
    void getAllSubclassPaths_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createSubclassPath("Warden of Renewal", testClass, testExpansion);
        createSubclassPath("Warden of the Elements", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Warden of Renewal"))
                .andExpect(jsonPath("$.content[1].name").value("Warden of the Elements"));
    }

    /**
     * Tests retrieving subclass paths without authentication returns 401.
     */
    @Test
    void getAllSubclassPaths_Unauthenticated_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests pagination works correctly.
     */
    @Test
    void getAllSubclassPaths_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange - Create 5 paths
        for (int i = 1; i <= 5; i++) {
            createSubclassPath("Path " + i, testClass, testExpansion);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    /**
     * Tests filtering by class ID.
     */
    @Test
    void getAllSubclassPaths_FilterByClassId_ReturnsFiltered() throws Exception {
        // Arrange
        Class otherClass = createClass("Warrior", testExpansion);
        createSubclassPath("Warden of Renewal", testClass, testExpansion);
        createSubclassPath("Path of the Blade", otherClass, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths")
                        .param("classId", testClass.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Warden of Renewal"));
    }

    /**
     * Tests expand parameter includes expansion details.
     */
    @Test
    void getAllSubclassPaths_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createSubclassPath("Warden of Renewal", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.id").value(testExpansion.getId()))
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    /**
     * Tests that soft-deleted paths are excluded by default.
     */
    @Test
    void getAllSubclassPaths_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createSubclassPath("Active Path", testClass, testExpansion);
        SubclassPath deletedPath = createSubclassPath("Deleted Path", testClass, testExpansion);
        deletedPath.setDeletedAt(LocalDateTime.now());
        subclassPathRepository.save(deletedPath);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Active Path"));
    }

    // ==================== GET SUBCLASS PATH BY ID TESTS ====================

    /**
     * Tests retrieving a single subclass path by ID.
     */
    @Test
    void getSubclassPathById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths/{id}", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(path.getId()))
                .andExpect(jsonPath("$.name").value("Warden of Renewal"));
    }

    /**
     * Tests retrieving path with expand parameter includes associated class.
     */
    @Test
    void getSubclassPathById_WithExpand_IncludesAssociatedClass() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths/{id}", path.getId())
                        .param("expand", "associatedClass")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.associatedClass").exists())
                .andExpect(jsonPath("$.associatedClass.name").value("Druid"));
    }

    /**
     * Tests retrieving non-existent path returns 404.
     */
    @Test
    void getSubclassPathById_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/subclass-paths/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE SUBCLASS PATH TESTS ====================

    /**
     * Tests creating a new subclass path as admin.
     */
    @Test
    void createSubclassPath_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Warden of Renewal"))
                .andExpect(jsonPath("$.associatedClassId").value(testClass.getId()));

        // Verify path was created
        assertThat(subclassPathRepository.findAll()).hasSize(1);
    }

    /**
     * Tests creating path as regular user returns 403.
     */
    @Test
    void createSubclassPath_AsUser_Returns403() throws Exception {
        // Arrange
        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify path was NOT created
        assertThat(subclassPathRepository.findAll()).isEmpty();
    }

    /**
     * Tests creating path with invalid class ID returns 404.
     */
    @Test
    void createSubclassPath_InvalidClassId_Returns404() throws Exception {
        // Arrange
        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(99999L)
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== BULK CREATE SUBCLASS PATHS TESTS ====================

    /**
     * Tests creating multiple subclass paths in bulk as admin.
     */
    @Test
    void createSubclassPathsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateSubclassPathRequest request1 = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();
        CreateSubclassPathRequest request2 = CreateSubclassPathRequest.builder()
                .name("Warden of the Elements")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();
        List<CreateSubclassPathRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Warden of Renewal"))
                .andExpect(jsonPath("$[1].name").value("Warden of the Elements"));

        // Verify paths were created
        assertThat(subclassPathRepository.findAll()).hasSize(2);
    }

    /**
     * Tests bulk create as regular user returns 403.
     */
    @Test
    void createSubclassPathsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();
        List<CreateSubclassPathRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE SUBCLASS PATH TESTS ====================

    /**
     * Tests updating a subclass path as admin.
     */
    @Test
    void updateSubclassPath_AsAdmin_Returns200() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Old Name", testClass, testExpansion);
        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/subclass-paths/{id}", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(path.getId()))
                .andExpect(jsonPath("$.name").value("Warden of Renewal"));

        // Verify path was updated
        SubclassPath updated = subclassPathRepository.findById(path.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Warden of Renewal");
    }

    /**
     * Tests updating path as regular user returns 403.
     */
    @Test
    void updateSubclassPath_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Old Name", testClass, testExpansion);
        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/subclass-paths/{id}", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests updating non-existent path returns 404.
     */
    @Test
    void updateSubclassPath_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(testClass.getId())
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/subclass-paths/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE SUBCLASS PATH TESTS ====================

    /**
     * Tests soft deleting a subclass path as admin.
     */
    @Test
    void deleteSubclassPath_AsAdmin_Returns204() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/subclass-paths/{id}", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        // Verify path was soft-deleted
        SubclassPath deleted = subclassPathRepository.findById(path.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests deleting path as regular user returns 403.
     */
    @Test
    void deleteSubclassPath_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/subclass-paths/{id}", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify path was NOT deleted
        SubclassPath notDeleted = subclassPathRepository.findById(path.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    /**
     * Tests deleting non-existent path returns 404.
     */
    @Test
    void deleteSubclassPath_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/dh/subclass-paths/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE SUBCLASS PATH TESTS ====================

    /**
     * Tests restoring a soft-deleted subclass path as admin.
     */
    @Test
    void restoreSubclassPath_AsAdmin_Returns200() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);
        path.setDeletedAt(LocalDateTime.now());
        subclassPathRepository.save(path);

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths/{id}/restore", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(path.getId()))
                .andExpect(jsonPath("$.name").value("Warden of Renewal"))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        // Verify path was restored
        SubclassPath restored = subclassPathRepository.findById(path.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * Tests restoring path as regular user returns 403.
     */
    @Test
    void restoreSubclassPath_AsUser_Returns403() throws Exception {
        // Arrange
        SubclassPath path = createSubclassPath("Warden of Renewal", testClass, testExpansion);
        path.setDeletedAt(LocalDateTime.now());
        subclassPathRepository.save(path);

        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths/{id}/restore", path.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify path was NOT restored
        SubclassPath stillDeleted = subclassPathRepository.findById(path.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests restoring non-existent path returns 404.
     */
    @Test
    void restoreSubclassPath_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/dh/subclass-paths/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a test user with the specified role.
     */
    private User createUserWithRole(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Stores a JWT token in the database for authentication.
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
     */
    private Expansion createExpansion(String name, Boolean isPublished) {
        Expansion expansion = Expansion.builder()
                .name(name)
                .isPublished(isPublished)
                .build();
        return expansionRepository.save(expansion);
    }

    /**
     * Creates a test class in the database.
     */
    private Class createClass(String name, Expansion expansion) {
        Class clazz = Class.builder()
                .name(name)
                .expansion(expansion)
                .startingEvasion(9)
                .startingHitPoints(16)
                .isOfficial(true)
                .build();
        return classRepository.save(clazz);
    }

    /**
     * Creates a test subclass path in the database.
     */
    private SubclassPath createSubclassPath(String name, Class clazz, Expansion expansion) {
        SubclassPath path = SubclassPath.builder()
                .name(name)
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .build();
        return subclassPathRepository.save(path);
    }

    /**
     * Creates a test domain in the database.
     */
    private Domain createDomain(String name, Expansion expansion) {
        Domain domain = Domain.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(true)
                .build();
        return domainRepository.save(domain);
    }
}
