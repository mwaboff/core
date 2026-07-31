package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.*;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.WeaponRepository;
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
 * Integration tests for WeaponController.
 * Tests all CRUD endpoints for Weapon resources with proper authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class WeaponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private FeatureRepository featureRepository;


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

    // ==================== GET ALL WEAPONS TESTS ====================

    @Test
    void getAllWeapons_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        createWeapon("Shortbow", testExpansion, true, true, Trait.FINESSE, Range.FAR, Burden.TWO_HANDED);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllWeapons_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/weapons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllWeapons_FilterByTrait_ReturnsFiltered() throws Exception {
        // Arrange
        createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        createWeapon("Shortbow", testExpansion, true, true, Trait.FINESSE, Range.FAR, Burden.TWO_HANDED);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .param("trait", "STRENGTH")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].trait").value("STRENGTH"));
    }

    @Test
    void getAllWeapons_FilterByRange_ReturnsFiltered() throws Exception {
        // Arrange
        createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        createWeapon("Shortbow", testExpansion, true, true, Trait.FINESSE, Range.FAR, Burden.TWO_HANDED);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .param("range", "FAR")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].range").value("FAR"));
    }

    @Test
    void getAllWeapons_FilterByDamageTypePhysical_ReturnsOnlyPhysicalWeapons() throws Exception {
        // Arrange
        createWeaponWithDamageType("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED, DamageType.PHYSICAL);
        createWeaponWithDamageType("Magic Staff", testExpansion, true, true, Trait.INSTINCT, Range.FAR, Burden.TWO_HANDED, DamageType.MAGIC);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .param("damageType", "PHYSICAL")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Longsword"))
                .andExpect(jsonPath("$.content[0].damage.damageType").value("PHYSICAL"));
    }

    @Test
    void getAllWeapons_FilterByDamageTypeMagic_ReturnsOnlyMagicWeapons() throws Exception {
        // Arrange
        createWeaponWithDamageType("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED, DamageType.PHYSICAL);
        createWeaponWithDamageType("Magic Staff", testExpansion, true, true, Trait.INSTINCT, Range.FAR, Burden.TWO_HANDED, DamageType.MAGIC);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .param("damageType", "MAGIC")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Magic Staff"))
                .andExpect(jsonPath("$.content[0].damage.damageType").value("MAGIC"));
    }

    @Test
    void getAllWeapons_WithNoDamageTypeFilter_ReturnsAllWeapons() throws Exception {
        // Arrange
        createWeaponWithDamageType("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED, DamageType.PHYSICAL);
        createWeaponWithDamageType("Magic Staff", testExpansion, true, true, Trait.INSTINCT, Range.FAR, Burden.TWO_HANDED, DamageType.MAGIC);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllWeapons_WithInvalidDamageType_ReturnsError() throws Exception {
        // Act & Assert - passing an invalid enum value triggers a type conversion error
        mockMvc.perform(get("/api/dh/weapons")
                        .param("damageType", "INVALID")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getAllWeapons_FilterByDamageTypeAndTrait_CombinesFilters() throws Exception {
        // Arrange - create weapons with different combinations of trait and damage type
        createWeaponWithDamageType("Physical Sword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED, DamageType.PHYSICAL);
        createWeaponWithDamageType("Magic Staff", testExpansion, true, true, Trait.INSTINCT, Range.FAR, Burden.TWO_HANDED, DamageType.MAGIC);
        createWeaponWithDamageType("Enchanted Blade", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED, DamageType.MAGIC);

        // Act & Assert - filter by both STRENGTH trait and MAGIC damage type
        mockMvc.perform(get("/api/dh/weapons")
                        .param("trait", "STRENGTH")
                        .param("damageType", "MAGIC")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Enchanted Blade"))
                .andExpect(jsonPath("$.content[0].trait").value("STRENGTH"))
                .andExpect(jsonPath("$.content[0].damage.damageType").value("MAGIC"));
    }

    @Test
    void getAllWeapons_WithExpand_IncludesExpansion() throws Exception {
        // Arrange
        createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons")
                        .param("expand", "expansion")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expansion").exists())
                .andExpect(jsonPath("$.content[0].expansion.name").value("Core Rulebook"));
    }

    // ==================== GET WEAPON BY ID TESTS ====================

    @Test
    void getWeaponById_AsAuthenticatedUser_Returns200() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);

        // Act & Assert
        mockMvc.perform(get("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(weapon.getId()))
                .andExpect(jsonPath("$.name").value("Longsword"))
                .andExpect(jsonPath("$.trait").value("STRENGTH"))
                .andExpect(jsonPath("$.range").value("MELEE"));
    }

    @Test
    void getWeaponById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/weapons/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE WEAPON TESTS ====================

    @Test
    void createWeapon_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(3)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Longsword"))
                .andExpect(jsonPath("$.trait").value("STRENGTH"))
                .andExpect(jsonPath("$.damage.notation").value("2d10+3 phy"));

        assertThat(weaponRepository.findAll()).hasSize(1);
    }

    @Test
    void createWeapon_PhysicalAndMagicDamageType_RoundTripsCreateThenGet() throws Exception {
        // Arrange - Shadowblade-style dual damage type weapon (Otherworldly: physical or magic, per attack)
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Shadowblade")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.PRESENCE)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D8)
                        .damageType(DamageType.PHYSICAL_AND_MAGIC)
                        .build())
                .build();

        // Act - create
        String createResponse = mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Shadowblade"))
                .andExpect(jsonPath("$.damage.damageType").value("PHYSICAL_AND_MAGIC"))
                .andExpect(jsonPath("$.damage.notation").value("d8 phy/mag"))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // Assert - get round-trips the same dual damage type
        mockMvc.perform(get("/api/dh/weapons/{id}", createdId)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shadowblade"))
                .andExpect(jsonPath("$.damage.damageType").value("PHYSICAL_AND_MAGIC"))
                .andExpect(jsonPath("$.damage.notation").value("d8 phy/mag"));
    }

    @Test
    void createWeapon_AsUser_Returns403() throws Exception {
        // Arrange
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(weaponRepository.findAll()).isEmpty();
    }

    // ==================== CREATE WEAPONS BULK TESTS ====================

    @Test
    void createWeaponsBulk_AsAdmin_Returns201() throws Exception {
        // Arrange
        CreateWeaponRequest request1 = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        CreateWeaponRequest request2 = CreateWeaponRequest.builder()
                .name("Shortbow")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.FINESSE)
                .range(Range.FAR)
                .burden(Burden.TWO_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        List<CreateWeaponRequest> requests = List.of(request1, request2);

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(weaponRepository.findAll()).hasSize(2);
    }

    @Test
    void createWeaponsBulk_WithRawJsonRealisticPayload_Returns201() throws Exception {
        // Arrange - raw JSON string (not builder+serialize) matching the real bulk-import
        // payload shape in hope_and_fear-import/json/07-weapons.json: a nested "features" array
        // (find-or-create by name) and a flat-die damage roll with diceCount/modifier genuinely
        // absent, as real weapons like "Broadsword" (d8, no count/modifier) actually send.
        // Builder-based tests elsewhere in this file always serialize a fully-populated DTO, so
        // they can't catch a Jackson deserialization regression on an omitted/null field the way
        // a real client's JSON can; this test exercises that real path for the /bulk endpoint.
        String bulkRequest = """
            [
                {
                    "name": "Katana",
                    "expansionId": %d,
                    "tier": 1,
                    "isOfficial": true,
                    "isPrimary": true,
                    "trait": "AGILITY",
                    "range": "MELEE",
                    "burden": "TWO_HANDED",
                    "damage": { "diceType": "D10", "modifier": 3, "damageType": "PHYSICAL" },
                    "features": [
                        { "name": "Quick", "description": "When you make an attack, you can mark a Stress to target another creature within range.", "featureType": "ITEM", "expansionId": %d }
                    ]
                },
                {
                    "name": "Broadsword",
                    "expansionId": %d,
                    "tier": 1,
                    "isOfficial": true,
                    "isPrimary": true,
                    "trait": "AGILITY",
                    "range": "MELEE",
                    "burden": "ONE_HANDED",
                    "damage": { "diceType": "D8", "damageType": "PHYSICAL" }
                }
            ]
            """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Katana"))
                .andExpect(jsonPath("$[0].featureIds").isArray())
                .andExpect(jsonPath("$[0].featureIds.length()").value(1))
                .andExpect(jsonPath("$[0].damage.diceCount").value((Object) null))
                .andExpect(jsonPath("$[1].name").value("Broadsword"))
                .andExpect(jsonPath("$[1].damage.diceCount").value((Object) null))
                .andExpect(jsonPath("$[1].damage.modifier").value((Object) null));

        assertThat(weaponRepository.findAll()).hasSize(2);
        assertThat(featureRepository.findAll()).hasSize(1);
    }

    @Test
    void createWeaponsBulk_AsUser_Returns403() throws Exception {
        // Arrange
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(testExpansion.getId())
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        List<CreateWeaponRequest> requests = List.of(request);

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }

    // ==================== UPDATE WEAPON TESTS ====================

    @Test
    void updateWeapon_AsAdmin_Returns200() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        UpdateWeaponRequest request = UpdateWeaponRequest.builder()
                .name("Greater Longsword")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.TWO_HANDED)
                .damage(UpdateWeaponRequest.DamageRollRequest.builder()
                        .diceCount(3)
                        .diceType(DiceType.D12)
                        .modifier(5)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(weapon.getId()))
                .andExpect(jsonPath("$.name").value("Greater Longsword"))
                .andExpect(jsonPath("$.burden").value("TWO_HANDED"))
                .andExpect(jsonPath("$.damage.notation").value("3d12+5 phy"));
    }

    @Test
    void updateWeapon_AsUser_Returns403() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        UpdateWeaponRequest request = UpdateWeaponRequest.builder()
                .name("Greater Longsword")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(UpdateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWeapon_NotFound_Returns404() throws Exception {
        // Arrange
        UpdateWeaponRequest request = UpdateWeaponRequest.builder()
                .name("Greater Longsword")
                .expansionId(testExpansion.getId())
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(UpdateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/dh/weapons/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE WEAPON TESTS ====================

    @Test
    void deleteWeapon_AsAdmin_Returns204() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        Weapon deleted = weaponRepository.findById(weapon.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteWeapon_AsUser_Returns403() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);

        // Act & Assert
        mockMvc.perform(delete("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());

        Weapon notDeleted = weaponRepository.findById(weapon.getId()).orElseThrow();
        assertThat(notDeleted.getDeletedAt()).isNull();
    }

    @Test
    void deleteWeapon_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/dh/weapons/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== RESTORE WEAPON TESTS ====================

    @Test
    void restoreWeapon_AsAdmin_Returns200() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        weapon.setDeletedAt(LocalDateTime.now());
        weaponRepository.save(weapon);

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons/{id}/restore", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(weapon.getId()))
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        Weapon restored = weaponRepository.findById(weapon.getId()).orElseThrow();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void restoreWeapon_AsUser_Returns403() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Longsword", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        weapon.setDeletedAt(LocalDateTime.now());
        weaponRepository.save(weapon);

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons/{id}/restore", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreWeapon_NotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/dh/weapons/{id}/restore", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== INLINE FEATURE TESTS ====================

    @Test
    void createWeapon_WithInlineFeature_Returns201AndCreatesFeature() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Flaming Sword",
                "description": "A sword wreathed in flame",
                "expansionId": %d,
                "tier": 1,
                "isOfficial": true,
                "isPrimary": true,
                "trait": "STRENGTH",
                "range": "MELEE",
                "burden": "ONE_HANDED",
                "damage": { "diceCount": 2, "diceType": "D10", "modifier": 3, "damageType": "PHYSICAL" },
                "features": [
                    {
                        "name": "Flame Burst",
                        "description": "Deal extra fire damage",
                        "featureType": "OTHER",
                        "expansionId": %d,
                        "costTags": [
                            { "label": "1/rest", "category": "LIMITATION" }
                        ]
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        // Verify feature and cost tag were created
        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(1);
        assertThat(features.get(0).getName()).isEqualTo("Flame Burst");
        assertThat(features.get(0).getCostTags()).hasSize(1);
    }

    @Test
    void createWeapon_WithBothIdsAndInline_MergesBothSources() throws Exception {
        // Arrange - create an existing feature
        Feature existingFeature = Feature.builder()
                .name("Existing Weapon Feature")
                .description("Pre-existing")
                .featureType(FeatureType.OTHER)
                .expansion(testExpansion)
                .build();
        existingFeature = featureRepository.save(existingFeature);

        String requestJson = """
            {
                "name": "Multi-Feature Sword",
                "expansionId": %d,
                "tier": 1,
                "isOfficial": true,
                "isPrimary": true,
                "trait": "STRENGTH",
                "range": "MELEE",
                "burden": "ONE_HANDED",
                "damage": { "diceCount": 1, "diceType": "D8", "damageType": "PHYSICAL" },
                "featureIds": [%d],
                "features": [
                    {
                        "name": "Inline Feature",
                        "featureType": "OTHER",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), existingFeature.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(2));

        // Both the existing feature and the new inline feature should be in DB
        assertThat(featureRepository.findAll()).hasSize(2);
    }

    @Test
    void createWeapon_WithInlineFeatureHavingCostTags_Returns201() throws Exception {
        // Arrange
        String requestJson = """
            {
                "name": "Tagged Weapon",
                "expansionId": %d,
                "tier": 1,
                "isOfficial": true,
                "isPrimary": true,
                "trait": "FINESSE",
                "range": "MELEE",
                "burden": "ONE_HANDED",
                "damage": { "diceCount": 1, "diceType": "D6", "damageType": "PHYSICAL" },
                "features": [
                    {
                        "name": "Quick Strike",
                        "description": "A fast attack",
                        "featureType": "OTHER",
                        "expansionId": %d,
                        "costTags": [
                            { "label": "1/session", "category": "TIMING" },
                            { "label": "Close range", "category": "LIMITATION" }
                        ]
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        List<Feature> features = featureRepository.findAll();
        assertThat(features).hasSize(1);
        assertThat(features.get(0).getCostTags()).hasSize(2);
    }

    @Test
    void updateWeapon_WithInlineFeature_Returns200() throws Exception {
        // Arrange
        Weapon weapon = createWeapon("Update Target", testExpansion, true, true, Trait.STRENGTH, Range.MELEE, Burden.ONE_HANDED);
        String requestJson = """
            {
                "name": "Updated Weapon",
                "expansionId": %d,
                "tier": 1,
                "isOfficial": true,
                "isPrimary": true,
                "trait": "STRENGTH",
                "range": "MELEE",
                "burden": "ONE_HANDED",
                "damage": { "diceCount": 2, "diceType": "D10", "modifier": 3, "damageType": "PHYSICAL" },
                "features": [
                    {
                        "name": "New Feature Via Update",
                        "description": "Added during update",
                        "featureType": "OTHER",
                        "expansionId": %d
                    }
                ]
            }
            """.formatted(testExpansion.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(put("/api/dh/weapons/{id}", weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureIds").isArray())
                .andExpect(jsonPath("$.featureIds.length()").value(1));

        assertThat(featureRepository.findAll()).hasSize(1);
        assertThat(featureRepository.findAll().get(0).getName()).isEqualTo("New Feature Via Update");
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

    private Weapon createWeapon(String name, Expansion expansion, Boolean isOfficial, Boolean isPrimary,
                                Trait trait, Range range, Burden burden) {
        return createWeapon(name, expansion, isOfficial, isPrimary, trait, range, burden, 1);
    }

    private Weapon createWeapon(String name, Expansion expansion, Boolean isOfficial, Boolean isPrimary,
                                Trait trait, Range range, Burden burden, Integer tier) {
        Weapon weapon = Weapon.builder()
                .name(name)
                .expansion(expansion)
                .tier(tier)
                .isOfficial(isOfficial)
                .isPrimary(isPrimary)
                .trait(trait)
                .range(range)
                .burden(burden)
                .damage(DamageRoll.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(3)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        return weaponRepository.save(weapon);
    }

    /**
     * Creates a weapon with a specific damage type for testing damage type filtering.
     */
    private Weapon createWeaponWithDamageType(String name, Expansion expansion, Boolean isOfficial, Boolean isPrimary,
                                              Trait trait, Range range, Burden burden, DamageType damageType) {
        Weapon weapon = Weapon.builder()
                .name(name)
                .expansion(expansion)
                .tier(1)
                .isOfficial(isOfficial)
                .isPrimary(isPrimary)
                .trait(trait)
                .range(range)
                .burden(burden)
                .damage(DamageRoll.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(3)
                        .damageType(damageType)
                        .build())
                .build();
        return weaponRepository.save(weapon);
    }
}
