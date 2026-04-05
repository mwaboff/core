package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateClassRequest;
import com.aboff.core.model.dto.dh.request.UpdateClassRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
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
 * Integration tests for ClassController.
 * Tests all CRUD endpoints for Class resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ClassControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private DomainRepository domainRepository;

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

    // ==================== GET ALL CLASSES TESTS ====================

    @Test
    void getAllClasses_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createClass("Warrior", "Warrior description", testExpansion);
        createClass("Mage", "Mage description", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllClasses_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/classes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllClasses_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            createClass("Class " + i, "Description " + i, testExpansion);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void getAllClasses_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createClass("Class 1", "Desc 1", testExpansion);
        createClass("Class 2", "Desc 2", expansion2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllClasses_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createClass("Warrior", "Warrior description", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    @Test
    void getAllClasses_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createClass("Active Class", "Active", testExpansion);
        Class deletedClass = createClass("Deleted Class", "Deleted", testExpansion);
        deletedClass.setDeletedAt(LocalDateTime.now());
        classRepository.save(deletedClass);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Active Class"));
    }

    // ==================== GET CLASS BY ID TESTS ====================

    @Test
    void getClassById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Warrior description", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clazz.getId()))
                .andExpect(jsonPath("$.name").value("Warrior"));
    }

    @Test
    void getClassById_WithExpand_IncludesExpansionAndDomains() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Warrior description", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/classes/{id}", clazz.getId())
                        .param("expand", "expansion,associatedDomains")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expansion").exists())
                .andExpect(jsonPath("$.expansion.name").value("Core Rulebook"));
    }

    @Test
    void getClassById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/classes/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE CLASS TESTS ====================

    @Test
    void createClass_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior")
                .description("A strong fighter")
                .expansionId(testExpansion.getId())
                .startingEvasion(10)
                .startingHitPoints(25)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Warrior"))
                .andExpect(jsonPath("$.startingEvasion").value(10))
                .andExpect(jsonPath("$.startingHitPoints").value(25));

        assertThat(classRepository.findAll()).hasSize(1);
    }

    @Test
    void createClass_AsUser_Returns403() throws Exception {
        // Arrange
        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior")
                .description("A strong fighter")
                .expansionId(testExpansion.getId())
                .startingEvasion(10)
                .startingHitPoints(25)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(classRepository.findAll()).isEmpty();
    }

    // ==================== CREATE CLASSES BULK TESTS ====================

    @Test
    void createClassesBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateClassRequest request1 = CreateClassRequest.builder()
                .name("Warrior")
                .description("A strong fighter")
                .expansionId(testExpansion.getId())
                .startingEvasion(10)
                .startingHitPoints(25)
                .build();
        CreateClassRequest request2 = CreateClassRequest.builder()
                .name("Mage")
                .description("A spell caster")
                .expansionId(testExpansion.getId())
                .startingEvasion(15)
                .startingHitPoints(20)
                .build();
        List<CreateClassRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(classRepository.findAll()).hasSize(2);
    }

    @Test
    void createClassesBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior")
                .description("A strong fighter")
                .expansionId(testExpansion.getId())
                .startingEvasion(10)
                .startingHitPoints(25)
                .build();
        List<CreateClassRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE CLASS TESTS ====================

    @Test
    void updateClass_AsAdmin_Returns200() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Original description", testExpansion);
        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Guardian")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .startingEvasion(12)
                .startingHitPoints(30)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clazz.getId()))
                .andExpect(jsonPath("$.name").value("Guardian"))
                .andExpect(jsonPath("$.startingEvasion").value(12));
    }

    @Test
    void updateClass_AsUser_Returns403() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Original description", testExpansion);
        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Guardian")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .startingEvasion(12)
                .startingHitPoints(30)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateClass_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Guardian")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .startingEvasion(12)
                .startingHitPoints(30)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/classes/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE CLASS TESTS ====================

    @Test
    void deleteClass_AsAdmin_Returns204() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "To delete", testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Class deleted = classRepository.findById(clazz.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteClass_AsUser_Returns403() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "To delete", testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Class notDeleted = classRepository.findById(clazz.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteClass_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/classes/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE CLASS TESTS ====================

    @Test
    void restoreClass_AsAdmin_Returns200() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Deleted class", testExpansion);
        clazz.setDeletedAt(LocalDateTime.now());
        classRepository.save(clazz);

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes/{id}/restore", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clazz.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Class restored = classRepository.findById(clazz.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreClass_AsUser_Returns403() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Deleted class", testExpansion);
        clazz.setDeletedAt(LocalDateTime.now());
        classRepository.save(clazz);

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes/{id}/restore", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreClass_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/classes/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== INLINE CREATION TESTS ====================

    @Test
    void createClass_withInlineHopeFeatures_createsAndAssociates() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "name": "Warrior",
                    "description": "A strong fighter",
                    "expansionId": %d,
                    "startingEvasion": 10,
                    "startingHitPoints": 25,
                    "hopeFeatures": [
                        {
                            "name": "Test Hope Feature",
                            "featureType": "HOPE",
                            "expansionId": %d
                        }
                    ]
                }
                """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Warrior"))
                .andExpect(jsonPath("$.hopeFeatureIds").isArray())
                .andExpect(jsonPath("$.hopeFeatureIds.length()").value(1));
    }

    @Test
    void createClass_withInlineBackgroundQuestions_createsAndAssociates() throws Exception {
        // Arrange
        String requestJson = """
                {
                    "name": "Scholar",
                    "description": "A learned person",
                    "expansionId": %d,
                    "startingEvasion": 8,
                    "startingHitPoints": 15,
                    "backgroundQuestions": [
                        {
                            "questionText": "What is your background?",
                            "questionType": "BACKGROUND",
                            "expansionId": %d
                        }
                    ]
                }
                """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Scholar"))
                .andExpect(jsonPath("$.backgroundQuestionIds").isArray())
                .andExpect(jsonPath("$.backgroundQuestionIds.length()").value(1));
    }

    @Test
    void createClassBulk_withInlineInputs_createsAll() throws Exception {
        // Arrange
        String requestJson = """
                [
                    {
                        "name": "Warrior",
                        "description": "Fighter",
                        "expansionId": %d,
                        "startingEvasion": 10,
                        "startingHitPoints": 25,
                        "hopeFeatures": [
                            {
                                "name": "Bulk Hope Feature",
                                "featureType": "HOPE",
                                "expansionId": %d
                            }
                        ]
                    },
                    {
                        "name": "Mage",
                        "description": "Caster",
                        "expansionId": %d,
                        "startingEvasion": 15,
                        "startingHitPoints": 20,
                        "backgroundQuestions": [
                            {
                                "questionText": "Where did you study?",
                                "questionType": "BACKGROUND",
                                "expansionId": %d
                            }
                        ]
                    }
                ]
                """.formatted(testExpansion.getId(), testExpansion.getId(),
                testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/classes/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hopeFeatureIds.length()").value(1))
                .andExpect(jsonPath("$[1].backgroundQuestionIds.length()").value(1));
    }

    @Test
    void updateClass_withInlineFeatures_updatesAssociations() throws Exception {
        // Arrange
        Class clazz = createClass("Warrior", "Original description", testExpansion);

        String requestJson = """
                {
                    "name": "Warrior",
                    "description": "Updated",
                    "expansionId": %d,
                    "startingEvasion": 10,
                    "startingHitPoints": 25,
                    "hopeFeatures": [
                        {
                            "name": "Updated Hope Feature",
                            "featureType": "HOPE",
                            "expansionId": %d
                        }
                    ]
                }
                """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(put("/api/dh/classes/{id}", clazz.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Warrior"))
                .andExpect(jsonPath("$.hopeFeatureIds").isArray())
                .andExpect(jsonPath("$.hopeFeatureIds.length()").value(1));
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

    private Class createClass(String name, String description, Expansion expansion) {
        Class clazz = Class.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .startingEvasion(10)
                .startingHitPoints(20)
                .build();
        return classRepository.save(clazz);
    }
}
