package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateBeastformRequest;
import com.aboff.core.model.dto.dh.request.UpdateBeastformRequest;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.BeastformRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for BeastformController.
 * Tests all CRUD endpoints for Beastform resources with proper authentication and authorization,
 * and proves a beastform round-trips create -&gt; get and survives bulk import.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class BeastformControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private BeastformRepository beastformRepository;

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
        adminUser = createUserWithRole("admin", "admin@example.com", Role.ADMIN);
        regularUser = createUserWithRole("user", "user@example.com", Role.USER);

        adminToken = jwtTokenProvider.generateToken(adminUser);
        userToken = jwtTokenProvider.generateToken(regularUser);

        storeTokenInDatabase(adminUser.getId(), adminToken);
        storeTokenInDatabase(regularUser.getId(), userToken);

        testExpansion = createExpansion("Core Rulebook", true);
    }

    // ==================== GET ALL BEASTFORMS TESTS ====================

    @Test
    void getAllBeastforms_AsAuthenticatedUser_Returns200() throws Exception {
        createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);
        createBeastform("Bear", testExpansion, Trait.STRENGTH, Range.MELEE);

        mockMvc.perform(get("/api/dh/beastforms")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllBeastforms_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/beastforms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllBeastforms_FilterByIsOfficial_ReturnsFiltered() throws Exception {
        createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        mockMvc.perform(get("/api/dh/beastforms")
                        .param("isOfficial", "true")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].isOfficial").value(true));
    }

    // ==================== GET BEASTFORM BY ID TESTS ====================

    @Test
    void getBeastformById_ValidId_Returns200() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        mockMvc.perform(get("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(beastform.getId()))
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void getBeastformById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/beastforms/999999")
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE BEASTFORM TESTS ====================

    @Test
    void createBeastform_AsAdmin_Returns201AndRoundTripsOnGet() throws Exception {
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .example("A lean grey wolf")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceCount(1)
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        String createResponse = mockMvc.perform(post("/api/dh/beastforms")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Wolf"))
                .andExpect(jsonPath("$.attackTrait").value("AGILITY"))
                .andExpect(jsonPath("$.damage.notation").value("1d6 phy"))
                .andReturn().getResponse().getContentAsString();

        assertThat(beastformRepository.findAll()).hasSize(1);

        Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // Round-trip: create -> get returns the same beastform
        mockMvc.perform(get("/api/dh/beastforms/" + createdId)
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void createBeastform_AsUser_Returns403() throws Exception {
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        mockMvc.perform(post("/api/dh/beastforms")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(beastformRepository.findAll()).isEmpty();
    }

    // ==================== CREATE BEASTFORMS BULK TESTS ====================

    @Test
    void createBeastformsBulk_AsAdmin_Returns201() throws Exception {
        CreateBeastformRequest request1 = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        CreateBeastformRequest request2 = CreateBeastformRequest.builder()
                .name("Bear")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.STRENGTH)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        CreateBeastformRequest request3 = CreateBeastformRequest.builder()
                .name("Owl")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.FAR)
                .attackTrait(Trait.FINESSE)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D4)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        List<CreateBeastformRequest> requests = List.of(request1, request2, request3);

        mockMvc.perform(post("/api/dh/beastforms/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        assertThat(beastformRepository.findAll()).hasSize(3);
    }

    @Test
    void createBeastformsBulk_AsAdmin_WithAgileScoutRawJson_PersistsEvasionTierAndFeatures() throws Exception {
        // Raw JSON, not a builder — proves the full HTTP -> Jackson -> service -> entity path
        // populates evasion and tier, since @Builder.Default on CreateBeastformRequest.evasion
        // would NOT catch a field silently arriving null via real deserialization (HANDOFF.md §4.3).
        String requestJson = """
                [
                  {
                    "name": "Agile Scout",
                    "example": "Fox, Mouse, Weasel, etc.",
                    "advantages": "Gain advantage on: deceive, locate, sneak",
                    "agilityModifier": 1,
                    "evasion": 2,
                    "tier": 1,
                    "attackRange": "MELEE",
                    "attackTrait": "AGILITY",
                    "damage": {
                      "diceCount": 1,
                      "diceType": "D4",
                      "damageType": "PHYSICAL"
                    },
                    "expansionId": %d,
                    "isOfficial": true,
                    "features": [
                      {
                        "name": "Agile",
                        "description": "Your movement is silent, and you can spend a Hope to move up to Far range without rolling.",
                        "featureType": "OTHER",
                        "expansionId": %d
                      },
                      {
                        "name": "Fragile",
                        "description": "When you take Major or greater damage while you're in this beastform, you're immediately knocked out of it.",
                        "featureType": "OTHER",
                        "expansionId": %d
                      }
                    ]
                  }
                ]
                """.formatted(testExpansion.getId(), testExpansion.getId(), testExpansion.getId());

        String responseJson = mockMvc.perform(post("/api/dh/beastforms/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Agile Scout"))
                .andExpect(jsonPath("$[0].evasion").value(2))
                .andExpect(jsonPath("$[0].tier").value(1))
                .andExpect(jsonPath("$[0].agilityModifier").value(1))
                .andExpect(jsonPath("$[0].attackRange").value("MELEE"))
                .andExpect(jsonPath("$[0].attackTrait").value("AGILITY"))
                .andExpect(jsonPath("$[0].featureIds.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseJson).get(0).get("id").asLong();
        Beastform persisted = beastformRepository.findByIdAndDeletedAtIsNull(createdId).orElseThrow();

        assertThat(persisted.getName()).isEqualTo("Agile Scout");
        assertThat(persisted.getEvasion()).isEqualTo(2);
        assertThat(persisted.getTier()).isEqualTo(1);
        assertThat(persisted.getAgilityModifier()).isEqualTo(1);
        assertThat(persisted.getFeatures()).hasSize(2);
        assertThat(persisted.getFeatures())
                .extracting("name")
                .containsExactlyInAnyOrder("Agile", "Fragile");
    }

    @Test
    void createBeastformsBulk_AsAdmin_WithEvolvedMetaCardRawJson_PersistsWithNoStatFields() throws Exception {
        // "Legendary Beast" (PDF p352) is an "Evolved: upgrade an earlier pick" card -- it
        // prints no stat line at all (no evasion, attack range/trait, damage, or trait
        // modifiers). Its bonus applies to whichever base form the player already chose and
        // is described in prose in the feature text, not a per-column value. This raw-JSON
        // payload omits evasion/attackRange/attackTrait/damage and all six trait modifiers
        // entirely -- proving the columns are genuinely optional end-to-end, not just
        // defaulted, since a NOT NULL DEFAULT 0 column would still accept an omitted key.
        String requestJson = """
                [
                  {
                    "name": "Legendary Beast",
                    "example": "Upgrade an earlier pick",
                    "tier": 3,
                    "features": [
                      {
                        "name": "Evolved",
                        "description": "Upgrade the trait bonus, Evasion, and damage of a beastform you've already chosen.",
                        "featureType": "OTHER",
                        "expansionId": %d
                      }
                    ],
                    "expansionId": %d,
                    "isOfficial": true,
                    "isPublic": true
                  }
                ]
                """.formatted(testExpansion.getId(), testExpansion.getId());

        String responseJson = mockMvc.perform(post("/api/dh/beastforms/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Legendary Beast"))
                .andExpect(jsonPath("$[0].tier").value(3))
                .andExpect(jsonPath("$[0].evasion").doesNotExist())
                .andExpect(jsonPath("$[0].attackRange").doesNotExist())
                .andExpect(jsonPath("$[0].attackTrait").doesNotExist())
                .andExpect(jsonPath("$[0].damage").doesNotExist())
                .andExpect(jsonPath("$[0].featureIds.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseJson).get(0).get("id").asLong();
        Beastform persisted = beastformRepository.findByIdAndDeletedAtIsNull(createdId).orElseThrow();

        assertThat(persisted.getName()).isEqualTo("Legendary Beast");
        assertThat(persisted.getTier()).isEqualTo(3);
        assertThat(persisted.getEvasion()).isNull();
        assertThat(persisted.getAttackRange()).isNull();
        assertThat(persisted.getAttackTrait()).isNull();
        assertThat(persisted.getDamage()).isNull();
        assertThat(persisted.getAgilityModifier()).isNull();
        assertThat(persisted.getStrengthModifier()).isNull();
        assertThat(persisted.getFinesseModifier()).isNull();
        assertThat(persisted.getInstinctModifier()).isNull();
        assertThat(persisted.getPresenceModifier()).isNull();
        assertThat(persisted.getKnowledgeModifier()).isNull();
        assertThat(persisted.getFeatures()).hasSize(1);
        assertThat(persisted.getFeatures())
                .extracting("name")
                .containsExactly("Evolved");
    }

    @Test
    void createBeastformsBulk_AsUser_Returns403() throws Exception {
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(testExpansion.getId())
                .isOfficial(true)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        mockMvc.perform(post("/api/dh/beastforms/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isForbidden());

        assertThat(beastformRepository.findAll()).isEmpty();
    }

    // ==================== UPDATE BEASTFORM TESTS ====================

    @Test
    void updateBeastform_AsAdmin_Returns200() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        UpdateBeastformRequest request = UpdateBeastformRequest.builder()
                .name("Dire Wolf")
                .attackTrait(Trait.STRENGTH)
                .build();

        mockMvc.perform(put("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dire Wolf"))
                .andExpect(jsonPath("$.attackTrait").value("STRENGTH"));
    }

    @Test
    void updateBeastform_AsUser_Returns403() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        UpdateBeastformRequest request = UpdateBeastformRequest.builder().name("Dire Wolf").build();

        mockMvc.perform(put("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE / RESTORE BEASTFORM TESTS ====================

    @Test
    void deleteBeastform_AsAdmin_Returns204AndSoftDeletes() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        mockMvc.perform(delete("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isNoContent());

        assertThat(beastformRepository.findByIdAndDeletedAtIsNull(beastform.getId())).isEmpty();

        mockMvc.perform(get("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBeastform_AsUser_Returns403() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);

        mockMvc.perform(delete("/api/dh/beastforms/" + beastform.getId())
                        .cookie(new Cookie("AUTH_TOKEN", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreBeastform_AsAdmin_Returns200AndRestores() throws Exception {
        Beastform beastform = createBeastform("Wolf", testExpansion, Trait.AGILITY, Range.MELEE);
        beastform.softDelete();
        beastformRepository.save(beastform);

        mockMvc.perform(post("/api/dh/beastforms/" + beastform.getId() + "/restore")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        assertThat(beastformRepository.findByIdAndDeletedAtIsNull(beastform.getId())).isPresent();
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

    private Beastform createBeastform(String name, Expansion expansion, Trait attackTrait, Range attackRange) {
        Beastform beastform = Beastform.builder()
                .name(name)
                .expansion(expansion)
                .createdBy(adminUser)
                .isOfficial(true)
                .isPublic(false)
                .evasion(0)
                .tier(1)
                .attackRange(attackRange)
                .attackTrait(attackTrait)
                .damage(DamageRoll.builder()
                        .diceCount(1)
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
        return beastformRepository.save(beastform);
    }
}
