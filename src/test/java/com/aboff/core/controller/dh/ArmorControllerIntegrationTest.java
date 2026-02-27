package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ArmorRepository;
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
 * Integration tests for ArmorController.
 * Tests all CRUD endpoints for Armor resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ArmorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private ArmorRepository armorRepository;

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

    // ==================== GET ALL ARMORS TESTS ====================

    @Test
    void getAllArmors_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        createArmor("Plate Mail", testExpansion, true, 8, 16, 3);

        // Act & Assert
        mockMvc.perform(get("/api/dh/armors")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllArmors_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/armors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllArmors_FilterByExpansion_ReturnsFiltered() throws Exception {
        // Arrange
        Expansion expansion2 = createExpansion("Expansion 2", true);
        createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        createArmor("Plate Mail", expansion2, true, 8, 16, 3);

        // Act & Assert
        mockMvc.perform(get("/api/dh/armors")
                        .param("expansionId", testExpansion.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Leather Armor"));
    }

    @Test
    void getAllArmors_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createArmor("Leather Armor", testExpansion, true, 5, 10, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/armors")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET ARMOR BY ID TESTS ====================

    @Test
    void getArmorById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);

        // Act & Assert
        mockMvc.perform(get("/api/dh/armors/{id}", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(armor.getId()))
                .andExpect(jsonPath("$.name").value("Leather Armor"))
                .andExpect(jsonPath("$.baseMajorThreshold").value(5))
                .andExpect(jsonPath("$.baseSevereThreshold").value(10))
                .andExpect(jsonPath("$.baseScore").value(1));
    }

    @Test
    void getArmorById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/armors/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE ARMOR TESTS ====================

    @Test
    void createArmor_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Leather Armor"))
                .andExpect(jsonPath("$.baseMajorThreshold").value(5))
                .andExpect(jsonPath("$.baseSevereThreshold").value(10))
                .andExpect(jsonPath("$.baseScore").value(1));

        assertThat(armorRepository.findAll()).hasSize(1);
    }

    @Test
    void createArmor_AsUser_Returns403() throws Exception {
        // Arrange
        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(armorRepository.findAll()).isEmpty();
    }

    // ==================== CREATE ARMORS BULK TESTS ====================

    @Test
    void createArmorsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateArmorRequest request1 = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();
        CreateArmorRequest request2 = CreateArmorRequest.builder()
                .name("Plate Mail")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(8)
                .baseSevereThreshold(16)
                .baseScore(3)
                .build();
        List<CreateArmorRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(armorRepository.findAll()).hasSize(2);
    }

    @Test
    void createArmorsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();
        List<CreateArmorRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE ARMOR TESTS ====================

    @Test
    void updateArmor_AsAdmin_Returns200() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        UpdateArmorRequest request = UpdateArmorRequest.builder()
                .name("Reinforced Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(6)
                .baseSevereThreshold(12)
                .baseScore(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/armors/{id}", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(armor.getId()))
                .andExpect(jsonPath("$.name").value("Reinforced Leather Armor"))
                .andExpect(jsonPath("$.baseMajorThreshold").value(6))
                .andExpect(jsonPath("$.baseSevereThreshold").value(12))
                .andExpect(jsonPath("$.baseScore").value(2));
    }

    @Test
    void updateArmor_AsUser_Returns403() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        UpdateArmorRequest request = UpdateArmorRequest.builder()
                .name("Reinforced Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(6)
                .baseSevereThreshold(12)
                .baseScore(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/armors/{id}", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateArmor_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateArmorRequest request = UpdateArmorRequest.builder()
                .name("Reinforced Leather Armor")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .baseMajorThreshold(6)
                .baseSevereThreshold(12)
                .baseScore(2)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/armors/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ARMOR TESTS ====================

    @Test
    void deleteArmor_AsAdmin_Returns204() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/armors/{id}", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Armor deleted = armorRepository.findById(armor.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteArmor_AsUser_Returns403() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/armors/{id}", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Armor notDeleted = armorRepository.findById(armor.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteArmor_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/armors/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE ARMOR TESTS ====================

    @Test
    void restoreArmor_AsAdmin_Returns200() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        armor.setDeletedAt(LocalDateTime.now());
        armorRepository.save(armor);

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors/{id}/restore", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(armor.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Armor restored = armorRepository.findById(armor.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreArmor_AsUser_Returns403() throws Exception {
        // Arrange
        Armor armor = createArmor("Leather Armor", testExpansion, true, 5, 10, 1);
        armor.setDeletedAt(LocalDateTime.now());
        armorRepository.save(armor);

        // Act & Assert
        mockMvc.perform(post("/api/dh/armors/{id}/restore", armor.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreArmor_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/armors/{id}/restore", 99999L)
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

    private Armor createArmor(String name, Expansion expansion, Boolean isOfficial,
                              Integer baseMajorThreshold, Integer baseSevereThreshold, Integer baseScore) {
        Armor armor = Armor.builder()
                .name(name)
                .expansion(expansion)
                .isOfficial(isOfficial)
                .baseMajorThreshold(baseMajorThreshold)
                .baseSevereThreshold(baseSevereThreshold)
                .baseScore(baseScore)
                .build();
        return armorRepository.save(armor);
    }
}
