package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
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
 * Integration tests for DomainController.
 * Tests all CRUD endpoints for Domain resources with proper authentication and authorization.
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
class DomainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ExpansionRepository expansionRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;

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

        // Create test expansion
        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL DOMAINS TESTS ====================

    /**
     * Tests retrieving all domains as an authenticated user.
     */
    @Test
    void getAllDomains_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Domain domain1 = createDomain("Fire", "Fire magic domain", testExpansion);
        Domain domain2 = createDomain("Ice", "Ice magic domain", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Fire"))
                .andExpect(jsonPath("$.content[1].name").value("Ice"));
    }

    /**
     * Tests retrieving domains without authentication returns 401.
     */
    @Test
    void getAllDomains_Unauthenticated_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/domains"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests pagination works correctly.
     */
    @Test
    void getAllDomains_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange - Create 5 domains
        for (int i = 1; i <= 5; i++) {
            createDomain("Domain " + i, "Description " + i, testExpansion);
        }

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .param("page", "1")
                        .param("size", "2")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    /**
     * Tests filtering by expansion ID.
     */
    @Test
    void getAllDomains_FilterByExpansionId_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Second Expansion", true);
        createDomain("Domain 1", "Desc 1", testExpansion);
        createDomain("Domain 2", "Desc 2", expansion2);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Domain 1"));
    }

    /**
     * Tests filtering by official status narrows the result set.
     */
    @Test
    void getAllDomains_FilterByIsOfficialTrue_ReturnsOnlyOfficial() throws Exception {
        // Arrange
        createDomain("Official Domain", "Desc 1", testExpansion, true);
        createDomain("Homebrew Domain", "Desc 2", testExpansion, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .param("isOfficial", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Official Domain"))
                .andExpect(jsonPath("$.content[0].isOfficial").value(true));
    }

    /**
     * Tests filtering by non-official status narrows the result set.
     */
    @Test
    void getAllDomains_FilterByIsOfficialFalse_ReturnsOnlyNonOfficial() throws Exception {
        // Arrange
        createDomain("Official Domain", "Desc 1", testExpansion, true);
        createDomain("Homebrew Domain", "Desc 2", testExpansion, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .param("isOfficial", "false")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Homebrew Domain"))
                .andExpect(jsonPath("$.content[0].isOfficial").value(false));
    }

    /**
     * Tests omitting the official filter returns both official and non-official domains.
     */
    @Test
    void getAllDomains_WithoutIsOfficialFilter_ReturnsBoth() throws Exception {
        // Arrange
        createDomain("Official Domain", "Desc 1", testExpansion, true);
        createDomain("Homebrew Domain", "Desc 2", testExpansion, false);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * Tests that a raw JSON create payload is deserialized into the isOfficial field.
     */
    @Test
    void createDomain_WithRawJsonIsOfficialFalse_PersistsNonOfficial() throws Exception {
        // Arrange - raw JSON string rather than builder+serialize: a builder-based fixture
        // cannot catch a missing isOfficial field on CreateDomainRequest, because the builder
        // would simply not compile. Only a real client's JSON exercises the Jackson path
        // where an unmapped property is silently dropped.
        String requestJson = """
            {
                "name": "Homebrew Domain",
                "description": "Not official content",
                "isOfficial": false,
                "expansionId": %d
            }
            """.formatted(testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOfficial").value(false));

        assertThat(domainRepository.findAll())
                .singleElement()
                .satisfies(domain -> assertThat(domain.getIsOfficial()).isFalse());
    }

    /**
     * Tests that omitting isOfficial from a raw JSON create payload defaults it to true.
     */
    @Test
    void createDomain_WithRawJsonOmittingIsOfficial_DefaultsToTrue() throws Exception {
        // Arrange - existing clients do not send isOfficial and must keep getting official content
        String requestJson = """
            {
                "name": "Dread",
                "description": "The domain of fear",
                "expansionId": %d
            }
            """.formatted(testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOfficial").value(true));

        assertThat(domainRepository.findAll())
                .singleElement()
                .satisfies(domain -> assertThat(domain.getIsOfficial()).isTrue());
    }

    /**
     * Tests expand parameter includes expansion details.
     */
    @Test
    void getAllDomains_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createDomain("Fire", "Fire domain", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.id").value(testExpansion.getId()))
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    /**
     * Tests that soft-deleted domains are excluded by default.
     */
    @Test
    void getAllDomains_ExcludesDeletedByDefault_ReturnsOnlyActive() throws Exception {
        // Arrange
        createDomain("Active Domain", "Active", testExpansion);
        Domain deletedDomain = createDomain("Deleted Domain", "Deleted", testExpansion);
        deletedDomain.setDeletedAt(LocalDateTime.now());
        domainRepository.save(deletedDomain);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Active Domain"));
    }

    // ==================== GET DOMAIN BY ID TESTS ====================

    /**
     * Tests retrieving a single domain by ID.
     */
    @Test
    void getDomainById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire magic domain", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains/{id}", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(domain.getId()))
                .andExpect(jsonPath("$.name").value("Fire"))
                .andExpect(jsonPath("$.description").value("Fire magic domain"));
    }

    /**
     * Tests retrieving domain with expand parameter.
     */
    @Test
    void getDomainById_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire magic domain", testExpansion);

        // Act & Assert
        mockMvc.perform(get("/api/dh/domains/{id}", domain.getId())
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expansion").exists())
                .andExpect(jsonPath("$.expansion.name").value("Core Rulebook"));
    }

    /**
     * Tests retrieving non-existent domain returns 404.
     */
    @Test
    void getDomainById_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/dh/domains/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE DOMAIN TESTS ====================

    /**
     * Tests creating a new domain as admin.
     */
    @Test
    void createDomain_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Fire")
                .description("Fire magic domain")
                .iconUrl("https://example.com/fire.png")
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Fire"))
                .andExpect(jsonPath("$.description").value("Fire magic domain"))
                .andExpect(jsonPath("$.iconUrl").value("https://example.com/fire.png"));

        // Verify domain was created
        assertThat(domainRepository.findAll()).hasSize(1);
    }

    /**
     * Tests creating domain as regular user returns 403.
     */
    @Test
    void createDomain_AsUser_Returns403() throws Exception {
        // Arrange
        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Fire")
                .description("Fire magic domain")
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify domain was NOT created
        assertThat(domainRepository.findAll()).isEmpty();
    }

    /**
     * Tests creating domain with invalid expansion ID returns 404.
     */
    @Test
    void createDomain_InvalidExpansionId_Returns404() throws Exception {
        // Arrange
        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Fire")
                .description("Fire magic domain")
                .expansionId(99999L)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE DOMAINS BULK TESTS ====================

    /**
     * Tests creating multiple domains in bulk as admin.
     */
    @Test
    void createDomainsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateDomainRequest request1 = CreateDomainRequest.builder()
                .name("Fire")
                .description("Fire domain")
                .expansionId(testExpansion.getId())
                .build();
        CreateDomainRequest request2 = CreateDomainRequest.builder()
                .name("Ice")
                .description("Ice domain")
                .expansionId(testExpansion.getId())
                .build();
        List<CreateDomainRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Fire"))
                .andExpect(jsonPath("$[1].name").value("Ice"));

        // Verify domains were created
        assertThat(domainRepository.findAll()).hasSize(2);
    }

    /**
     * Tests bulk create as regular user returns 403.
     */
    @Test
    void createDomainsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateDomainRequest request = CreateDomainRequest.builder()
                .name("Fire")
                .description("Fire domain")
                .expansionId(testExpansion.getId())
                .build();
        List<CreateDomainRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE DOMAIN TESTS ====================

    /**
     * Tests updating a domain as admin.
     */
    @Test
    void updateDomain_AsAdmin_Returns200() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Original description", testExpansion);
        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Flame")
                .description("Updated description")
                .iconUrl("https://example.com/flame.png")
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/domains/{id}", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(domain.getId()))
                .andExpect(jsonPath("$.name").value("Flame"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.iconUrl").value("https://example.com/flame.png"));

        // Verify domain was updated
        Domain updated = domainRepository.findById(domain.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Flame");
    }

    /**
     * Tests updating domain as regular user returns 403.
     */
    @Test
    void updateDomain_AsUser_Returns403() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Original description", testExpansion);
        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Flame")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/domains/{id}", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests updating non-existent domain returns 404.
     */
    @Test
    void updateDomain_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateDomainRequest request = UpdateDomainRequest.builder()
                .name("Flame")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/domains/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE DOMAIN TESTS ====================

    /**
     * Tests soft deleting a domain as admin.
     */
    @Test
    void deleteDomain_AsAdmin_Returns204() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire domain", testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/domains/{id}", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        // Verify domain was soft-deleted
        Domain deleted = domainRepository.findById(domain.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests deleting domain as regular user returns 403.
     */
    @Test
    void deleteDomain_AsUser_Returns403() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire domain", testExpansion);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/domains/{id}", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify domain was NOT deleted
        Domain notDeleted = domainRepository.findById(domain.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    /**
     * Tests deleting non-existent domain returns 404.
     */
    @Test
    void deleteDomain_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/dh/domains/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE DOMAIN TESTS ====================

    /**
     * Tests restoring a soft-deleted domain as admin.
     */
    @Test
    void restoreDomain_AsAdmin_Returns200() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire domain", testExpansion);
        domain.setDeletedAt(LocalDateTime.now());
        domainRepository.save(domain);

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains/{id}/restore", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(domain.getId()))
                .andExpect(jsonPath("$.name").value("Fire"))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        // Verify domain was restored
        Domain restored = domainRepository.findById(domain.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    /**
     * Tests restoring domain as regular user returns 403.
     */
    @Test
    void restoreDomain_AsUser_Returns403() throws Exception {
        // Arrange
        Domain domain = createDomain("Fire", "Fire domain", testExpansion);
        domain.setDeletedAt(LocalDateTime.now());
        domainRepository.save(domain);

        // Act & Assert
        mockMvc.perform(post("/api/dh/domains/{id}/restore", domain.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        // Verify domain was NOT restored
        Domain stillDeleted = domainRepository.findById(domain.getId()).orElseThrow();
        assertThat(stillDeleted.getDeletedAt()).isNotNull();
    }

    /**
     * Tests restoring non-existent domain returns 404.
     */
    @Test
    void restoreDomain_NotFound_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/dh/domains/{id}/restore", 99999L)
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
     * Creates an official test domain in the database.
     */
    private Domain createDomain(String name, String description, Expansion expansion) {
        return createDomain(name, description, expansion, true);
    }

    /**
     * Creates a test domain in the database with an explicit official status.
     */
    private Domain createDomain(String name, String description, Expansion expansion, Boolean isOfficial) {
        Domain domain = Domain.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .build();
        return domainRepository.save(domain);
    }
}
