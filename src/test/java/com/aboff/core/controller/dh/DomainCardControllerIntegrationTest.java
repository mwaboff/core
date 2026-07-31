package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.DomainCardRepository;
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
 * Integration tests for DomainCardController.
 * Tests all CRUD endpoints for DomainCard resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class DomainCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private DomainCardRepository domainCardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private DomainRepository domainRepository;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Expansion testExpansion;
    private Domain testDomain;

    @BeforeEach
    void setUp() {
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
        testDomain = createDomain("Fire", "Fire domain", testExpansion);
    }

    // ==================== GET ALL DOMAIN CARDS TESTS ====================

    @Test
    void getAllDomainCards_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        createDomainCard("Flame Shield", "Fire defense", testExpansion, true, testDomain, 2, 2, DomainCardType.ABILITY);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllDomainCards_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/cards/domain"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllDomainCards_FilterByType_ReturnsFiltered() throws Exception {
        // Arrange
        createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        createDomainCard("Flame Shield", "Fire armor", testExpansion, true, testDomain, 2, 2, DomainCardType.ABILITY);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain")
                        .param("type", "SPELL")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("SPELL"));
    }

    @Test
    void getAllDomainCards_FilterByAssociatedDomainId_ReturnsFiltered() throws Exception {
        // Arrange
        Domain domain2 = createDomain("Ice", "Ice domain", testExpansion);
        createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        createDomainCard("Ice Lance", "Ice spell", testExpansion, true, domain2, 1, 1, DomainCardType.SPELL);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain")
                        .param("associatedDomainIds", testDomain.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Fireball"));
    }

    @Test
    void getAllDomainCards_FilterByLevel_ReturnsFiltered() throws Exception {
        // Arrange
        createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        createDomainCard("Greater Fireball", "Greater fire spell", testExpansion, true, testDomain, 3, 2, DomainCardType.SPELL);
        createDomainCard("Flame Shield", "Fire armor", testExpansion, true, testDomain, 3, 2, DomainCardType.ABILITY);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain")
                        .param("levels", "3")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].level").value(3))
                .andExpect(jsonPath("$.content[1].level").value(3));
    }

    @Test
    void getAllDomainCards_WithExpand_IncludesAssociatedDomain() throws Exception {
        // Arrange
        createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain")
                        .param("expand", "expansion,associatedDomain")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].associatedDomain").exists())
                .andExpect(jsonPath("$.content[0].associatedDomain.name").value("Fire"));
    }

    // ==================== GET DOMAIN CARD BY ID TESTS ====================

    @Test
    void getDomainCardById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "Fire spell", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);

        // Act & Assert
        mockMvc.perform(get("/api/dh/cards/domain/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Fireball"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.recallCost").value(1))
                .andExpect(jsonPath("$.type").value("SPELL"));
    }

    @Test
    void getDomainCardById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/cards/domain/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE DOMAIN CARD TESTS ====================

    @Test
    void createDomainCard_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateDomainCardRequest request = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Fire spell")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Fireball"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.recallCost").value(1))
                .andExpect(jsonPath("$.type").value("SPELL"));

        assertThat(domainCardRepository.findAll()).hasSize(1);
    }

    @Test
    void createDomainCard_AsUser_Returns403() throws Exception {
        // Arrange
        CreateDomainCardRequest request = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Fire spell")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(domainCardRepository.findAll()).isEmpty();
    }

    // ==================== CREATE DOMAIN CARDS BULK TESTS ====================

    @Test
    void createDomainCardsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateDomainCardRequest request1 = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Fire spell")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();
        CreateDomainCardRequest request2 = CreateDomainCardRequest.builder()
                .name("Flame Shield")
                .description("Fire defense")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(2)
                .recallCost(2)
                .type(DomainCardType.ABILITY)
                .build();
        List<CreateDomainCardRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(domainCardRepository.findAll()).hasSize(2);
    }

    @Test
    void createDomainCardsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateDomainCardRequest request = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Fire spell")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();
        List<CreateDomainCardRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE DOMAIN CARD TESTS ====================

    @Test
    void updateDomainCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "Original description", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        UpdateDomainCardRequest request = UpdateDomainCardRequest.builder()
                .name("Greater Fireball")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(2)
                .recallCost(3)
                .type(DomainCardType.GRIMOIRE)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/domain/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.name").value("Greater Fireball"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.recallCost").value(3))
                .andExpect(jsonPath("$.type").value("GRIMOIRE"));
    }

    @Test
    void updateDomainCard_AsUser_Returns403() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "Original description", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        UpdateDomainCardRequest request = UpdateDomainCardRequest.builder()
                .name("Greater Fireball")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/domain/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDomainCard_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateDomainCardRequest request = UpdateDomainCardRequest.builder()
                .name("Greater Fireball")
                .description("Updated description")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .associatedDomainId(testDomain.getId())
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/cards/domain/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE DOMAIN CARD TESTS ====================

    @Test
    void deleteDomainCard_AsAdmin_Returns204() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "To delete", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/domain/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        DomainCard deleted = domainCardRepository.findById(card.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteDomainCard_AsUser_Returns403() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "To delete", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/cards/domain/{id}", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        DomainCard notDeleted = domainCardRepository.findById(card.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteDomainCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/cards/domain/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE DOMAIN CARD TESTS ====================

    @Test
    void restoreDomainCard_AsAdmin_Returns200() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "Deleted card", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        card.setDeletedAt(LocalDateTime.now());
        domainCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        DomainCard restored = domainCardRepository.findById(card.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreDomainCard_AsUser_Returns403() throws Exception {
        // Arrange
        DomainCard card = createDomainCard("Fireball", "Deleted card", testExpansion, true, testDomain, 1, 1, DomainCardType.SPELL);
        card.setDeletedAt(LocalDateTime.now());
        domainCardRepository.save(card);

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain/{id}/restore", card.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreDomainCard_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/cards/domain/{id}/restore", 99999L)
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

    private Domain createDomain(String name, String description, Expansion expansion) {
        Domain domain = Domain.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .build();
        return domainRepository.save(domain);
    }

    private DomainCard createDomainCard(String name, String description, Expansion expansion, Boolean isOfficial,
                                       Domain associatedDomain, Integer level, Integer recallCost, DomainCardType type) {
        DomainCard card = DomainCard.builder()
                .name(name)
                .description(description)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .associatedDomain(associatedDomain)
                .level(level)
                .recallCost(recallCost)
                .type(type)
                .build();
        return domainCardRepository.save(card);
    }
}
