package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainCardRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.DomainCardRepository;
import com.aboff.core.repository.dh.DomainRepository;
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
    private FeatureRepository featureRepository;

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
    void createDomainCardsBulk_TransformationType_RealisticBeastformPayload_Returns201() throws Exception {
        // Arrange - the core rulebook's 24 Druid Beastform cards (pages 33-36) import as
        // ordinary domainCard payloads with type: "TRANSFORMATION" -- this is the ONE case
        // where DomainCardType.TRANSFORMATION is correct (see
        // .claude/skills/daggerheart-pdf-import/references/core-rulebook.md's "Beastform" note
        // and HANDOFF.md's correction that this content is 24 named entries, not 12). This
        // exact shape is what's on disk in
        // core-import/intermediate/04b-beastform-cards.json's "Agile Scout"/"Nimble Grazer"
        // records and is about to be uploaded for real -- the transformation_card/beastform
        // table has 0 rows today and this path has never seen production data.
        //
        // Raw JSON, not builders, matching the actual resolved payload shape:
        //   - no card-level "description" (Beastform cards carry all rules text inside a single
        //     feature, not the card description field) -- CreateDomainCardRequest.description
        //     has no @NotNull, so this must round-trip as null, not fail validation.
        //   - the nested feature genuinely omits "name" (the parse deliberately treats the whole
        //     printed card as one unnamed rules-text block, confirmed in the source JSON's
        //     "features[0].name": null derivation) -- FeatureInput.name has no @NotNull, so
        //     find-or-create must fall through to always-create rather than colliding two
        //     different creatures' null-named features into one row.
        //   - recallCost: 0 and level: 1, both inferred defaults per the source JSON's _flags,
        //     sent explicitly (not omitted) since they're genuinely part of the resolved payload.
        Domain arcana = createDomain("Arcana", "Arcana domain", testExpansion);
        String bulkRequest = """
            [
                {
                    "name": "Agile Scout",
                    "expansionId": %d,
                    "isOfficial": true,
                    "associatedDomainId": %d,
                    "level": 1,
                    "recallCost": 0,
                    "type": "TRANSFORMATION",
                    "features": [
                        { "description": "(Fox, Mouse, Weasel, etc.) Agility +1 | Evasion +2. Melee Agility d4 phy. Gain advantage on: deceive, locate, sneak. Agile: Your movement is silent, and you can spend a Hope to move up to Far range without rolling. Fragile: When you take Major or greater damage, you drop out of Beastform.", "featureType": "DOMAIN", "expansionId": %d }
                    ]
                },
                {
                    "name": "Nimble Grazer",
                    "expansionId": %d,
                    "isOfficial": true,
                    "associatedDomainId": %d,
                    "level": 1,
                    "recallCost": 0,
                    "type": "TRANSFORMATION",
                    "features": [
                        { "description": "(Deer, Gazelle, Goat, etc.) Agility +1 | Evasion +3. Melee Agility d6 phy. Gain advantage on: leap, sneak, sprint. Elusive Prey: When an attack roll against you would succeed, you can mark a Stress and roll a d4. Add the result to your Evasion against this attack. Fragile: When you take Major or greater damage, you drop out of Beastform.", "featureType": "DOMAIN", "expansionId": %d }
                    ]
                }
            ]
            """.formatted(testExpansion.getId(), arcana.getId(), testExpansion.getId(),
                    testExpansion.getId(), arcana.getId(), testExpansion.getId());

        // Act & Assert
        mockMvc.perform(post("/api/dh/cards/domain/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Agile Scout"))
                .andExpect(jsonPath("$[0].type").value("TRANSFORMATION"))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].featureIds").isArray())
                .andExpect(jsonPath("$[0].featureIds.length()").value(1))
                // HANDOFF.md section 4.4's hazard: Card.cardType is insertable=false/updatable=false
                // and reads null on a freshly-built in-memory entity before the DB round-trip
                // populates the discriminator. DomainCardService.toResponse() is called immediately
                // after save() in the same transaction, with no re-fetch -- assert the bulk-create
                // response's cardType is not silently null on the exact path that hazard describes.
                .andExpect(jsonPath("$[0].cardType").value("DOMAIN_CARD"))
                .andExpect(jsonPath("$[1].name").value("Nimble Grazer"))
                .andExpect(jsonPath("$[1].type").value("TRANSFORMATION"))
                .andExpect(jsonPath("$[1].featureIds.length()").value(1))
                .andExpect(jsonPath("$[1].cardType").value("DOMAIN_CARD"));

        List<DomainCard> saved = domainCardRepository.findAll();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(DomainCard::getType)
                .containsExactly(DomainCardType.TRANSFORMATION, DomainCardType.TRANSFORMATION);
        assertThat(saved).extracting(DomainCard::getLevel).containsExactly(1, 1);
        assertThat(saved).extracting(DomainCard::getRecallCost).containsExactly(0, 0);
        assertThat(saved).extracting(c -> c.getAssociatedDomain().getId())
                .containsExactly(arcana.getId(), arcana.getId());

        // The two null-named features must NOT collide into one shared row via find-or-create --
        // FeatureService.findOrCreate only attempts a lookup when a name is provided, so each
        // unnamed Beastform feature must always create its own distinct row even though both
        // share (name=null, expansionId, featureType=DOMAIN).
        assertThat(featureRepository.findAll()).hasSize(2);
        assertThat(featureRepository.findAll()).extracting(Feature::getName)
                .containsExactly(null, null);
        assertThat(featureRepository.findAll()).extracting(Feature::getDescription)
                .allMatch(d -> d != null && !d.isBlank());
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
