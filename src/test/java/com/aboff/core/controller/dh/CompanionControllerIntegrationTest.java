package com.aboff.core.controller.dh;

import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.CreateCompanionTrainingRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.entity.ActiveToken;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.ViciousAxis;
import com.aboff.core.repository.ActiveTokenRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.CompanionRepository;
import com.aboff.core.repository.dh.CompanionTrainingRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CompanionController.
 * <p>
 * The priority scenarios here prove the previously-live data-exposure leak is closed:
 * {@code GET /api/dh/companions} without a {@code characterSheetId}, and cross-user access to
 * another owner's companions, must both be rejected. See {@code companionSecurity*} tests.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CompanionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveTokenRepository activeTokenRepository;

    @Autowired
    private CharacterSheetRepository characterSheetRepository;

    @Autowired
    private CompanionRepository companionRepository;

    @Autowired
    private CompanionTrainingRepository companionTrainingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User player1;
    private User player2;
    private User moderator;
    private String player1Token;
    private String player2Token;
    private String moderatorToken;
    private CharacterSheet testSheet;

    @BeforeEach
    void setUp() {
        player1 = createUserWithRole("player1", "player1@example.com", Role.USER);
        player2 = createUserWithRole("player2", "player2@example.com", Role.USER);
        moderator = createUserWithRole("moderator", "moderator@example.com", Role.MODERATOR);

        player1Token = jwtTokenProvider.generateToken(player1);
        player2Token = jwtTokenProvider.generateToken(player2);
        moderatorToken = jwtTokenProvider.generateToken(moderator);

        storeTokenInDatabase(player1.getId(), player1Token);
        storeTokenInDatabase(player2.getId(), player2Token);
        storeTokenInDatabase(moderator.getId(), moderatorToken);

        testSheet = createCharacterSheet("Aragorn", player1, 5);
    }

    // ==================== SECURITY FIX: GET /api/dh/companions ====================

    @Test
    void companionSecurity_ListWithoutCharacterSheetId_Returns400() throws Exception {
        mockMvc.perform(get("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companionSecurity_OwnerCanListOwnCompanions() throws Exception {
        createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void companionSecurity_ModeratorCanListAnyonesCompanions() throws Exception {
        createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void companionSecurity_OtherUserCannotListSomeoneElsesCompanions_Returns403() throws Exception {
        createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void companionSecurity_ExpandExperiencesCannotBeUsedToLeakAnotherUsersData() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        addExperienceToCompanion(companion, "Tracking");

        // The previous vulnerability: expand=experiences on an unfiltered/unauthenticated list
        // returned every user's companions and their Experience text. Confirm player2 is
        // rejected outright, before any experience data could be serialized.
        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .param("expand", "experiences")
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCompanions_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllCompanions_ExcludesSoftDeletedCompanions() throws Exception {
        Companion active = createCompanion("Wolf", testSheet);
        Companion deleted = createCompanion("Hawk", testSheet);
        deleted.softDelete();
        companionRepository.save(deleted);

        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", testSheet.getId().toString())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(active.getId()));
    }

    @Test
    void getAllCompanions_WithInvalidCharacterSheetId_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/companions")
                        .param("characterSheetId", "999999")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    // ==================== SECURITY FIX: GET /api/dh/companions/{id} ====================

    @Test
    void companionSecurity_OwnerCanGetOwnCompanion() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wolf"));
    }

    @Test
    void companionSecurity_ModeratorCanGetAnyonesCompanion() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", moderatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void companionSecurity_OtherUserCannotGetSomeoneElsesCompanion_Returns403() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCompanionById_Unauthenticated_Returns401() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCompanionById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/dh/companions/{id}", 99999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCompanionById_SoftDeleted_Returns404() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        companion.softDelete();
        companionRepository.save(companion);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCompanionById_WithExpansion_IncludesExpandedEntities() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .param("expand", "characterSheet")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterSheet.name").value("Aragorn"));
    }

    @Test
    void getCompanionById_ReturnsDerivedAndBaseValues() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        addTrainingToCompanion(companion, CompanionTrainingOption.AWARE, null);

        mockMvc.perform(get("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseEvasion").value(12))
                .andExpect(jsonPath("$.evasion").value(14)) // base 12 + 2 for Aware
                .andExpect(jsonPath("$.attackDiceCount").value(testSheet.getProficiency()))
                .andExpect(jsonPath("$.trainings.length()").value(1))
                .andExpect(jsonPath("$.trainings[0].option").value("AWARE"));
    }

    // ==================== CREATE COMPANION ====================

    @Test
    void createCompanion_AsOwner_Returns201() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .description("A loyal wolf companion")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wolf"))
                .andExpect(jsonPath("$.characterSheetId").value(testSheet.getId()));

        assertThat(companionRepository.countByCharacterSheetId(testSheet.getId())).isEqualTo(1);
    }

    @Test
    void createCompanion_AsOtherUser_Returns403() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(companionRepository.countByCharacterSheetId(testSheet.getId())).isEqualTo(0);
    }

    @Test
    void createCompanion_WithStressMarkedExceedingMax_Returns400() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .stressMax(3)
                .stressMarked(5)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCompanion_WithEvasionAboveBound_Returns400() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(999)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCompanion_WithoutDamageType_DefaultsToPhysical() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.damageType").value("PHYSICAL"));
    }

    @Test
    void createCompanion_WithExplicitMagicDamageType_Returns201() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .damageType(DamageType.MAGIC)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.damageType").value("MAGIC"));
    }

    @Test
    void createCompanion_WithPhysicalAndMagicDamageType_Returns400() throws Exception {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(testSheet.getId())
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .damageType(DamageType.PHYSICAL_AND_MAGIC)
                .build();

        mockMvc.perform(post("/api/dh/companions")
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(companionRepository.countByCharacterSheetId(testSheet.getId())).isEqualTo(0);
    }

    // ==================== UPDATE COMPANION ====================

    @Test
    void updateCompanion_AsOwner_Returns200() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(2).build();

        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stressMarked").value(2));
    }

    @Test
    void updateCompanion_AsOtherUser_Returns403() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(2).build();

        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Companion unchanged = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(unchanged.getStressMarked()).isEqualTo(0);
    }

    @Test
    void updateCompanion_SoftDeleted_Returns404() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        companion.softDelete();
        companionRepository.save(companion);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(1).build();

        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCompanion_WithDamageType_Returns200AndUpdatesDamageType() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder().damageType(DamageType.MAGIC).build();

        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damageType").value("MAGIC"));

        Companion updated = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(updated.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void updateCompanion_WithPhysicalAndMagicDamageType_Returns400() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .damageType(DamageType.PHYSICAL_AND_MAGIC)
                .build();

        mockMvc.perform(put("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Companion unchanged = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(unchanged.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    // ==================== DELETE COMPANION (SOFT DELETE) ====================

    @Test
    void deleteCompanion_AsOwner_SoftDeletesAndReturns204() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNoContent());

        Companion stillPresent = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(stillPresent.isDeleted()).isTrue();
    }

    @Test
    void deleteCompanion_AsOtherUser_Returns403() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        Companion unchanged = companionRepository.findById(companion.getId()).orElseThrow();
        assertThat(unchanged.isDeleted()).isFalse();
    }

    @Test
    void deleteCompanion_AlreadyDeleted_Returns404() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        companion.softDelete();
        companionRepository.save(companion);

        mockMvc.perform(delete("/api/dh/companions/{id}", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    // ==================== TRAINING ENDPOINTS ====================

    @Test
    void addTraining_AsOwner_Returns201AndUpdatesDerivedStats() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        mockMvc.perform(post("/api/dh/companions/{id}/trainings", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evasion").value(14)) // base 12 + 2
                .andExpect(jsonPath("$.trainings.length()").value(1));

        assertThat(companionTrainingRepository.findByCompanionId(companion.getId())).hasSize(1);
    }

    @Test
    void addTraining_ExceedingCap_Returns400() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        addTrainingToCompanion(companion, CompanionTrainingOption.LIGHT_IN_THE_DARK, null);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.LIGHT_IN_THE_DARK)
                .build();

        mockMvc.perform(post("/api/dh/companions/{id}/trainings", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addTraining_AsOtherUser_Returns403() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        mockMvc.perform(post("/api/dh/companions/{id}/trainings", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(companionTrainingRepository.findByCompanionId(companion.getId())).isEmpty();
    }

    @Test
    void addTraining_Unauthenticated_Returns401() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        mockMvc.perform(post("/api/dh/companions/{id}/trainings", companion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removeTraining_AsOwner_Returns200AndUpdatesDerivedStats() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CompanionTraining training = addTrainingToCompanion(companion, CompanionTrainingOption.AWARE, null);

        // The orphanRemoval DELETE is only flushed at transaction commit, not necessarily
        // before a same-transaction repository re-query, so the response body -- built
        // straight from the in-memory entity graph -- is the reliable assertion here.
        mockMvc.perform(delete("/api/dh/companions/{id}/trainings/{trainingId}", companion.getId(), training.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evasion").value(12)) // back to base
                .andExpect(jsonPath("$.trainings.length()").value(0));

        assertThat(companion.getTrainings()).isEmpty();
    }

    @Test
    void removeTraining_NotFound_Returns404() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);

        mockMvc.perform(delete("/api/dh/companions/{id}/trainings/{trainingId}", companion.getId(), 999999L)
                        .cookie(new Cookie("AUTH_TOKEN", player1Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeTraining_AsOtherUser_Returns403() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CompanionTraining training = addTrainingToCompanion(companion, CompanionTrainingOption.AWARE, null);

        mockMvc.perform(delete("/api/dh/companions/{id}/trainings/{trainingId}", companion.getId(), training.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player2Token)))
                .andExpect(status().isForbidden());

        assertThat(companionTrainingRepository.findByCompanionId(companion.getId())).hasSize(1);
    }

    @Test
    void addTraining_ViciousWithAxis_Returns201() throws Exception {
        Companion companion = createCompanion("Wolf", testSheet);
        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.VICIOUS)
                .viciousAxis(ViciousAxis.DAMAGE_DIE)
                .build();

        mockMvc.perform(post("/api/dh/companions/{id}/trainings", companion.getId())
                        .cookie(new Cookie("AUTH_TOKEN", player1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.damageDice").value("D8"));
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

    private CharacterSheet createCharacterSheet(String name, User owner, Integer level) {
        CharacterSheet sheet = CharacterSheet.builder()
                .name(name)
                .owner(owner)
                .level(level)
                .proficiency(2)
                .evasion(0)
                .armorMax(0)
                .armorMarked(0)
                .majorDamageThreshold(5)
                .severeDamageThreshold(10)
                .agilityModifier(0)
                .agilityMarked(false)
                .strengthModifier(0)
                .strengthMarked(false)
                .finesseModifier(0)
                .finesseMarked(false)
                .instinctModifier(0)
                .instinctMarked(false)
                .presenceModifier(0)
                .presenceMarked(false)
                .knowledgeModifier(0)
                .knowledgeMarked(false)
                .hitPointMax(6)
                .hitPointMarked(0)
                .stressMax(5)
                .stressMarked(0)
                .hopeMax(5)
                .hopeMarked(0)
                .build();
        return characterSheetRepository.save(sheet);
    }

    private Companion createCompanion(String name, CharacterSheet characterSheet) {
        Companion companion = Companion.builder()
                .characterSheet(characterSheet)
                .name(name)
                .description("A " + name + " companion")
                .attackName("Bite")
                .baseAttackRange(Range.CLOSE)
                .baseDamageDice(DiceType.D6)
                .baseEvasion(12)
                .baseStressMax(3)
                .stressMarked(0)
                .build();
        return companionRepository.save(companion);
    }

    /**
     * Adds a Training selection through the companion's own collection (never via
     * {@code companionTrainingRepository.save(...)} directly), so the already-loaded
     * {@code companion} instance's in-memory {@code trainings} set stays consistent with the
     * database within this test's single transaction/persistence context -- mirroring the
     * mutate-through-the-parent rule the production code follows for the same reason.
     */
    private CompanionTraining addTrainingToCompanion(Companion companion, CompanionTrainingOption option, ViciousAxis axis) {
        CompanionTraining training = CompanionTraining.builder()
                .companion(companion)
                .option(option)
                .viciousAxis(axis)
                .acquiredAtLevel(1)
                .build();
        companion.getTrainings().add(training);
        // companionRepository.save() merges an already-managed companion, which cascades a
        // MERGE (copying state onto a new managed instance) rather than a PERSIST for the new
        // child -- so the generated id lands on the *returned* companion's collection, not on
        // this local `training` reference.
        Companion saved = companionRepository.save(companion);
        return saved.getTrainings().stream()
                .filter(t -> t.getOption() == option)
                .reduce((first, second) -> second) // most recently added, in case of repeat calls with the same option
                .orElseThrow();
    }

    private void addExperienceToCompanion(Companion companion, String description) {
        com.aboff.core.model.entity.dh.Experience experience = com.aboff.core.model.entity.dh.Experience.builder()
                .companion(companion)
                .createdBy(player1)
                .description(description)
                .modifier(2)
                .build();
        companion.getExperiences().add(experience);
        companionRepository.save(companion);
    }
}
