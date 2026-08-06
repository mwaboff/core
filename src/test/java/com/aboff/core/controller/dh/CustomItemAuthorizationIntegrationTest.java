package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCustomArmorRequest;
import com.aboff.core.model.dto.dh.request.CreateCustomLootRequest;
import com.aboff.core.model.dto.dh.request.CreateCustomWeaponRequest;
import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.FeatureInput;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.ArmorRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.LootRepository;
import com.aboff.core.repository.dh.WeaponRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end authorization checks for user-authored equipment.
 * <p>
 * Covers the privilege boundaries that opening item creation introduces: that a regular user
 * cannot publish content or claim it as canon, that they cannot edit anyone else's work, that
 * the admin import path stays closed to them, and — the one that runs the other way — that
 * fetching any item by id stays open to everyone.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CustomItemAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private WeaponRepository weaponRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ArmorRepository armorRepository;

    @Autowired
    private LootRepository lootRepository;

    @Autowired
    private FeatureRepository featureRepository;

    private User author;
    private User otherUser;
    private User moderator;
    private User admin;
    private String authorToken;
    private String otherToken;
    private String moderatorToken;
    private String adminToken;
    private Expansion expansion;

    @BeforeEach
    void setUp() {
        author = createUser("author", Role.USER);
        otherUser = createUser("other", Role.USER);
        moderator = createUser("moderator", Role.MODERATOR);
        admin = createUser("admin", Role.ADMIN);

        authorToken = issueToken(author);
        otherToken = issueToken(otherUser);
        moderatorToken = issueToken(moderator);
        adminToken = issueToken(admin);

        expansion = expansionRepository.save(
                Expansion.builder().name("Auth Test Book").isPublished(true).build());
    }

    private User createUser(String name, Role role) {
        return userRepository.save(User.builder()
                .username(name + "-" + System.nanoTime())
                .email(name + System.nanoTime() + "@test.com")
                .role(role)
                .build());
    }

    private String issueToken(User user) {
        String token = jwtTokenProvider.generateToken(user);
        activeTokenRepository.save(ActiveToken.builder()
                .userId(user.getId())
                .tokenHash(jwtTokenProvider.hashToken(token))
                .deviceInfo("Test Device")
                .ipAddress("127.0.0.1")
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
        return token;
    }

    private CreateCustomWeaponRequest customWeapon(String name) {
        return CreateCustomWeaponRequest.builder()
                .name(name)
                .tier(1)
                .isPrimary(true)
                .trait(Trait.AGILITY)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D8)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();
    }

    private Weapon persistWeapon(String name, boolean official, boolean isPublic, User creator) {
        return weaponRepository.save(Weapon.builder()
                .name(name).tier(1)
                .expansion(official ? expansion : null)
                .isOfficial(official).isPublic(isPublic).createdBy(creator)
                .isPrimary(true).trait(Trait.AGILITY).range(Range.MELEE).burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder().diceType(DiceType.D8).damageType(DamageType.PHYSICAL).build())
                .build());
    }

    // ==================== CREATION ====================

    @Test
    void regularUserCanCreateACustomWeapon() throws Exception {
        mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customWeapon("My Blade"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Blade"))
                .andExpect(jsonPath("$.isOfficial").value(false))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.expansionId").doesNotExist())
                .andExpect(jsonPath("$.createdByUserId").value(author.getId()));
    }

    @Test
    void regularUserRequestingPublicIsCoercedToPrivate() throws Exception {
        CreateCustomWeaponRequest request = customWeapon("Sneaky Publish");
        request.setIsPublic(true);

        mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    void moderatorRequestingPublicIsHonoured() throws Exception {
        CreateCustomWeaponRequest request = customWeapon("Published Blade");
        request.setIsPublic(true);

        mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void taggingACampaignTheUserIsNotPartOfIsRejected() throws Exception {
        Campaign strangers = campaignRepository.save(
                Campaign.builder().name("Not Mine").creator(otherUser).build());

        CreateCustomWeaponRequest request = customWeapon("Gatecrasher");
        request.setCampaignIds(List.of(strangers.getId()));

        mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void taggingAnOwnCampaignSucceeds() throws Exception {
        Campaign mine = campaignRepository.save(
                Campaign.builder().name("My Table").creator(author).build());

        CreateCustomWeaponRequest request = customWeapon("Table Blade");
        request.setCampaignIds(List.of(mine.getId()));

        mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignIds[0]").value(mine.getId()));
    }

    // ==================== THE ADMIN IMPORT PATH STAYS CLOSED ====================

    @Test
    void regularUserCannotUseTheAdminCreateEndpoint() throws Exception {
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Canon Blade").expansionId(expansion.getId()).tier(1)
                .isOfficial(true).isPrimary(true)
                .trait(Trait.AGILITY).range(Range.MELEE).burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D8).damageType(DamageType.PHYSICAL).build())
                .build();

        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotUseTheBulkImportEndpoint() throws Exception {
        mockMvc.perform(post("/api/dh/weapons/bulk")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateStillRejectsAPayloadMissingIsOfficial() throws Exception {
        // Guards the DTO split: the import contract must stay strict so a malformed payload
        // fails loudly instead of landing as un-attributed homebrew.
        String payload = """
                {"name":"Sloppy","expansionId":%d,"tier":1,"isPrimary":true,
                 "trait":"AGILITY","range":"MELEE","burden":"ONE_HANDED",
                 "damage":{"diceType":"D8","damageType":"PHYSICAL"}}
                """.formatted(expansion.getId());

        mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // ==================== EDITING ====================

    @Test
    void authorCanUpdateTheirOwnWeapon() throws Exception {
        Weapon weapon = persistWeapon("Mine", false, false, author);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setName("Mine Renamed");

        mockMvc.perform(put("/api/dh/weapons/" + weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mine Renamed"));
    }

    @Test
    void aUserCannotUpdateSomeoneElsesWeapon() throws Exception {
        Weapon weapon = persistWeapon("Not Yours", false, false, author);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setName("Hijacked");

        mockMvc.perform(put("/api/dh/weapons/" + weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aUserCannotEditOfficialContent() throws Exception {
        Weapon official = persistWeapon("Canon Blade", true, false, null);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setName("Defaced");

        mockMvc.perform(put("/api/dh/weapons/" + official.getId())
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanStillEditOfficialContent() throws Exception {
        // The import pipeline runs as ADMIN; restricting official edits to OWNER would lock
        // out the people who maintain the catalogue.
        Weapon official = persistWeapon("Canon Blade", true, false, null);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setName("Canon Blade Revised");

        mockMvc.perform(put("/api/dh/weapons/" + official.getId())
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void aUserCannotPromoteTheirOwnWeaponToOfficial() throws Exception {
        Weapon weapon = persistWeapon("Aspiring Canon", false, false, author);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setIsOfficial(true);

        mockMvc.perform(put("/api/dh/weapons/" + weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(false));
    }

    // ==================== VISIBILITY AND READS ====================

    @Test
    void anyUserCanFetchSomeoneElsesPrivateWeaponById() throws Exception {
        // Required so a custom weapon renders on another player's character sheet or profile.
        // If this ever starts 403ing, those pages silently break.
        Weapon privateWeapon = persistWeapon("Private Blade", false, false, author);

        mockMvc.perform(get("/api/dh/weapons/" + privateWeapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Private Blade"));
    }

    @Test
    void browseExcludesSomeoneElsesPrivateWeapon() throws Exception {
        persistWeapon("Hidden From You", false, false, author);

        mockMvc.perform(get("/api/dh/weapons")
                        .param("size", "100")
                        .cookie(new Cookie("AUTH_TOKEN", otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'Hidden From You')]").isEmpty());
    }

    @Test
    void regularUserCannotListSoftDeletedWeapons() throws Exception {
        mockMvc.perform(get("/api/dh/weapons")
                        .param("includeDeleted", "true")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderatorCanListSoftDeletedWeapons() throws Exception {
        mockMvc.perform(get("/api/dh/weapons")
                        .param("includeDeleted", "true")
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk());
    }

    // ==================== COPYING ====================

    @Test
    void anyUserCanCopyAnOfficialWeaponAndOwnsTheResult() throws Exception {
        Weapon official = persistWeapon("Longsword", true, false, null);

        mockMvc.perform(post("/api/dh/weapons/" + official.getId() + "/copy")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Longsword (Copy)"))
                .andExpect(jsonPath("$.isOfficial").value(false))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.expansionId").doesNotExist())
                .andExpect(jsonPath("$.createdByUserId").value(author.getId()))
                .andExpect(jsonPath("$.originalWeaponId").value(official.getId()));
    }

    @Test
    void copyingSomeoneElsesPrivateWeaponIsAllowed() throws Exception {
        Weapon privateWeapon = persistWeapon("Their Blade", false, false, author);

        mockMvc.perform(post("/api/dh/weapons/" + privateWeapon.getId() + "/copy")
                        .cookie(new Cookie("AUTH_TOKEN", otherToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByUserId").value(otherUser.getId()));
    }

    @Test
    void copyDoesNotInheritCampaignTags() throws Exception {
        Campaign campaign = campaignRepository.save(
                Campaign.builder().name("Source Table").creator(author).build());
        Weapon shared = weaponRepository.save(Weapon.builder()
                .name("Shared Blade").tier(1).isOfficial(false).isPublic(true).createdBy(author)
                .campaigns(Set.of(campaign))
                .isPrimary(true).trait(Trait.AGILITY).range(Range.MELEE).burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder().diceType(DiceType.D8).damageType(DamageType.PHYSICAL).build())
                .build());

        mockMvc.perform(post("/api/dh/weapons/" + shared.getId() + "/copy")
                        .cookie(new Cookie("AUTH_TOKEN", otherToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignIds").doesNotExist());
    }

    // ==================== SOURCEBOOK PROVENANCE: INLINE FEATURES ====================
    //
    // An expansion identifies the book a piece of content was printed in, so only official
    // content may hold one. These cover the three places a caller could get one onto a row
    // they do not own: an inline feature at create, an inline feature at update, and the
    // item's own expansionId at update.

    private FeatureInput inlineFeature(String name, Long expansionId) {
        return FeatureInput.builder()
                .name(name)
                .description("Inline rules text for " + name)
                .featureType(FeatureType.ITEM)
                .expansionId(expansionId)
                .build();
    }

    private Feature onlyFeatureOf(Long weaponId) {
        Weapon saved = weaponRepository.findByIdAndDeletedAtIsNull(weaponId).orElseThrow();
        assertThat(saved.getFeatures()).hasSize(1);
        return saved.getFeatures().iterator().next();
    }

    private Long createdIdFrom(String responseBody) throws Exception {
        return objectMapper.readTree(responseBody).get("id").asLong();
    }

    @Test
    void anInlineFeatureOnACustomWeaponCannotClaimASourcebook() throws Exception {
        // The inline feature was written straight to the global features table with whatever
        // expansionId the caller sent, so a plain user could put a row into the Daggerheart Core
        // Set's feature list. It also poisons the find-or-create key a later import searches.
        CreateCustomWeaponRequest request = customWeapon("Smuggler's Blade");
        request.setFeatures(List.of(inlineFeature("Smuggled Canon", expansion.getId())));

        String body = mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Feature created = onlyFeatureOf(createdIdFrom(body));
        assertThat(created.getExpansion()).isNull();
    }

    @Test
    void anInlineFeatureOnACustomWeaponRecordsItsAuthor() throws Exception {
        // features.created_by_user_id was added for exactly this and nothing wrote it, which is
        // what would make a flood of user-minted rows impossible to attribute after the fact.
        CreateCustomWeaponRequest request = customWeapon("Attributed Blade");
        request.setFeatures(List.of(inlineFeature("Authored Feature", null)));

        String body = mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Feature created = onlyFeatureOf(createdIdFrom(body));
        assertThat(created.getCreatedBy()).isNotNull();
        assertThat(created.getCreatedBy().getId()).isEqualTo(author.getId());
    }

    @Test
    void aCustomWeaponWithAnInlineFeatureCanBeReadBackWithExpandFeatures() throws Exception {
        // A feature with no expansion was dereferenced unconditionally when building its
        // response, so expanding the features of any custom item threw.
        CreateCustomWeaponRequest request = customWeapon("Readable Blade");
        request.setFeatures(List.of(inlineFeature("Readable Feature", null)));

        String body = mockMvc.perform(post("/api/dh/weapons/custom")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/dh/weapons/" + createdIdFrom(body))
                        .param("expand", "features")
                        .cookie(new Cookie("AUTH_TOKEN", authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[0].name").value("Readable Feature"))
                .andExpect(jsonPath("$.features[0].expansionId").doesNotExist());
    }

    @Test
    void theAdminImportPathCanStillGiveAFeatureASourcebook() throws Exception {
        // The coercion must be conditional on the owning item, not a blanket rule, or every
        // content import stops being able to attribute a feature to its book.
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Imported Blade").expansionId(expansion.getId()).tier(1)
                .isOfficial(true).isPrimary(true)
                .trait(Trait.AGILITY).range(Range.MELEE).burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D8).damageType(DamageType.PHYSICAL).build())
                .features(List.of(inlineFeature("Canon Feature", expansion.getId())))
                .build();

        String body = mockMvc.perform(post("/api/dh/weapons")
                        .cookie(new Cookie("AUTH_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Feature created = onlyFeatureOf(createdIdFrom(body));
        assertThat(created.getExpansion()).isNotNull();
        assertThat(created.getExpansion().getId()).isEqualTo(expansion.getId());
        assertThat(created.getCreatedBy()).isNull();
    }

    @Test
    void anInlineFeatureAddedByUpdatingACustomWeaponCannotClaimASourcebook() throws Exception {
        Weapon weapon = persistWeapon("Retrofitted", false, false, author);
        UpdateWeaponRequest request = new UpdateWeaponRequest();
        request.setFeatures(List.of(inlineFeature("Retrofitted Canon", expansion.getId())));

        mockMvc.perform(put("/api/dh/weapons/" + weapon.getId())
                        .cookie(new Cookie("AUTH_TOKEN", authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(onlyFeatureOf(weapon.getId()).getExpansion()).isNull();
    }

}
