package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.AdvancementChoice;
import com.aboff.core.model.dto.dh.request.CompanionExperienceGrant;
import com.aboff.core.model.dto.dh.request.CompanionTrainingChoice;
import com.aboff.core.model.dto.dh.request.LevelUpRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetResponse;
import com.aboff.core.model.dto.dh.response.CompanionLevelUpOptionsResponse;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.dh.response.LevelUpOptionsResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterAdvancementLog;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.AdvancementType;
import com.aboff.core.model.enums.CompanionOrigin;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.ViciousAxis;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.*;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LevelUpService}'s companion Training/Experience/creation-and-restore
 * handling and its full reversal via {@code undoLevelUp}.
 * <p>
 * Kept in its own file rather than appended to the already-large {@code LevelUpServiceTest} --
 * see {@code core/docs/agent-plans/2026-08-04-companion-wp5-levelup-design.md}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LevelUpServiceCompanionTest {

    @Mock
    private CharacterSheetRepository characterSheetRepository;
    @Mock
    private CharacterSheetDomainCardRepository characterSheetDomainCardRepository;
    @Mock
    private CharacterAdvancementLogRepository characterAdvancementLogRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private DomainCardRepository domainCardRepository;
    @Mock
    private SubclassCardRepository subclassCardRepository;
    @Mock
    private SubclassPathRepository subclassPathRepository;
    @Mock
    private CompanionRepository companionRepository;
    @Mock
    private CompanionService companionService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleHierarchyService roleHierarchyService;
    @Mock
    private CharacterSheetService characterSheetService;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private Authentication authentication;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LevelUpService levelUpService;

    private User testOwner;
    private CharacterSheet sheet;

    /** Simulates IDENTITY-strategy id generation for newly-added CompanionTraining rows. */
    private final AtomicLong trainingIdSequence = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        levelUpService = new LevelUpService(
                characterSheetRepository, characterSheetDomainCardRepository,
                characterAdvancementLogRepository, experienceRepository,
                domainCardRepository, subclassCardRepository, subclassPathRepository,
                companionRepository, companionService,
                userRepository, roleHierarchyService, characterSheetService, auditLogger, objectMapper
        );

        testOwner = User.builder().id(1L).username("player1").email("player1@test.com").role(Role.USER).build();
        CustomUserDetails userDetails = new CustomUserDetails(testOwner);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);

        sheet = buildSheet(2);
        lenient().when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        lenient().when(characterSheetRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(characterAdvancementLogRepository.save(any())).thenAnswer(i -> {
            CharacterAdvancementLog l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        lenient().when(characterSheetDomainCardRepository.countEquippedByCharacterSheetId(1L)).thenReturn(0L);
        lenient().when(characterSheetService.toResponse(any(), any())).thenReturn(CharacterSheetResponse.builder().build());
        lenient().when(companionService.toResponse(any(Companion.class), any())).thenAnswer(inv -> {
            Companion c = inv.getArgument(0);
            return CompanionResponse.builder().id(c.getId()).name(c.getName()).build();
        });
        // Mockito doesn't simulate JPA identity generation, but applyCompanionTrainings relies on
        // a newly-added CompanionTraining getting a generated id back from save() -- assign one
        // to any not-yet-id'd training here, mirroring what the real IDENTITY strategy does.
        lenient().when(companionRepository.save(any(Companion.class))).thenAnswer(i -> {
            Companion c = i.getArgument(0);
            for (CompanionTraining t : c.getTrainings()) {
                if (t.getId() == null) {
                    t.setId(trainingIdSequence.incrementAndGet());
                }
            }
            return c;
        });
        lenient().when(experienceRepository.save(any(Experience.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ==================== getLevelUpOptions ====================

    @Test
    void getLevelUpOptions_IncludesEligibleCompanionWithBaselinePicksAvailable() {
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(companionRepository.findByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpOptionsResponse response = levelUpService.getLevelUpOptions(1L, authentication);

        assertThat(response.getCompanionTraining()).hasSize(1);
        CompanionLevelUpOptionsResponse entry = response.getCompanionTraining().get(0);
        assertThat(entry.getCompanionId()).isEqualTo(5L);
        assertThat(entry.getPicksAvailable()).isEqualTo(1);
        assertThat(entry.getAvailableOptions()).hasSize(CompanionTrainingOption.values().length);
    }

    @Test
    void getLevelUpOptions_ExcludesCompanionThatDoesNotAdvanceOnLevelUp() {
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());
        when(companionRepository.findByCharacterSheetId(1L)).thenReturn(List.of());

        LevelUpOptionsResponse response = levelUpService.getLevelUpOptions(1L, authentication);

        assertThat(response.getCompanionTraining()).isEmpty();
    }

    @Test
    void getLevelUpOptions_IncludesSoftDeletedSubclassFeatureCompanionAsRestorable() {
        Companion softDeleted = buildCompanion(6L, sheet);
        softDeleted.softDelete();
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());
        when(companionRepository.findByCharacterSheetId(1L)).thenReturn(List.of(softDeleted));

        LevelUpOptionsResponse response = levelUpService.getLevelUpOptions(1L, authentication);

        assertThat(response.getRestorableCompanions()).hasSize(1);
        assertThat(response.getRestorableCompanions().get(0).getId()).isEqualTo(6L);
    }

    @Test
    void getLevelUpOptions_ExcludesManualOriginSoftDeletedCompanionFromRestorable() {
        Companion softDeletedManual = buildCompanion(7L, sheet);
        softDeletedManual.setOrigin(CompanionOrigin.MANUAL);
        softDeletedManual.softDelete();
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());
        when(companionRepository.findByCharacterSheetId(1L)).thenReturn(List.of(softDeletedManual));

        LevelUpOptionsResponse response = levelUpService.getLevelUpOptions(1L, authentication);

        assertThat(response.getRestorableCompanions()).isEmpty();
    }

    // ==================== Training pick validation ====================

    @Test
    void levelUp_WithoutRequiredCompanionTrainingPick_Throws() {
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 1 Training pick");
    }

    @Test
    void levelUp_CompanionTrainingExceedingCap_Throws() {
        Companion companion = buildCompanion(5L, sheet);
        companion.getTrainings().add(CompanionTraining.builder().id(90L).companion(companion)
                .option(CompanionTrainingOption.LIGHT_IN_THE_DARK).acquiredAtLevel(1).build());
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.LIGHT_IN_THE_DARK).build()))
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void levelUp_TwoPicksOfSameCappedOptionInOneRequest_SecondRejected() {
        Companion companion = buildCompanion(5L, sheet);
        SubclassPath path = buildSubclassPath(19L);
        sheet.getSubclassCards().add(buildPathCard(21L, SubclassLevel.FOUNDATION, path, null));
        SubclassCard expertTrainingCard = buildPathCard(41L, SubclassLevel.SPECIALIZATION, path, "Expert Training");
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(subclassCardRepository.findById(41L)).thenReturn(Optional.of(expertTrainingCard));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.UPGRADE_SUBCLASS).subclassCardId(41L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .companionTrainings(List.of(
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.LIGHT_IN_THE_DARK).build(),
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.LIGHT_IN_THE_DARK).build()
                ))
                .build();

        // picksAvailable is 2 here (baseline 1 + Expert Training), so the count check passes,
        // but LIGHT_IN_THE_DARK's cap of 1 means the second identical pick must be rejected.
        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remaining");
    }

    @Test
    void levelUp_ViciousWithoutAxis_Throws() {
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.VICIOUS).build()))
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("viciousAxis");
    }

    @Test
    void levelUp_IntelligentWithoutTargetExperience_Throws() {
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.INTELLIGENT).build()))
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targetExperienceId");
    }

    @Test
    void levelUp_TrainingForIneligibleCompanion_Throws() {
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(999L).option(CompanionTrainingOption.AWARE).build()))
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not eligible");
    }

    // ==================== picksAvailable bonuses ====================

    @Test
    void levelUp_ExpertTrainingCard_GrantsOneExtraPick() {
        Companion companion = buildCompanion(5L, sheet);
        SubclassPath path = buildSubclassPath(15L);
        sheet.getSubclassCards().add(buildPathCard(14L, SubclassLevel.FOUNDATION, path, null));
        SubclassCard expertTrainingCard = buildPathCard(41L, SubclassLevel.SPECIALIZATION, path, "Expert Training");
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(subclassCardRepository.findById(41L)).thenReturn(Optional.of(expertTrainingCard));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.UPGRADE_SUBCLASS).subclassCardId(41L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .companionTrainings(List.of(
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.AWARE).build(),
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.RESILIENT).build()
                ))
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(companion.getTrainings()).hasSize(2);
    }

    @Test
    void levelUp_AdvancedTrainingCard_GrantsTwoExtraPicks() {
        Companion companion = buildCompanion(5L, sheet);
        SubclassPath path = buildSubclassPath(16L);
        sheet.getSubclassCards().add(buildPathCard(17L, SubclassLevel.FOUNDATION, path, null));
        sheet.getSubclassCards().add(buildPathCard(18L, SubclassLevel.SPECIALIZATION, path, null));
        SubclassCard advancedTrainingCard = buildPathCard(42L, SubclassLevel.MASTERY, path, "Advanced Training");
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(subclassCardRepository.findById(42L)).thenReturn(Optional.of(advancedTrainingCard));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.UPGRADE_SUBCLASS).subclassCardId(42L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .companionTrainings(List.of(
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.AWARE).build(),
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.RESILIENT).build(),
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.ARMORED).build()
                ))
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(companion.getTrainings()).hasSize(3);
    }

    // ==================== Companion created this level-up gets no picks ====================

    @Test
    void levelUp_CompanionCreatedThisLevelUp_IsNotEligibleForTrainingThisLevelUp() {
        // Only companions returned by findActiveByCharacterSheetId (called once, up front) are
        // eligible -- a companion granted via newCompanionId this same request must not appear
        // there, since it's a *different* companion (the multiclass case), not one already active.
        sheet.setLevel(4); // MULTICLASS requires tier 3+ (nextLevel 5 -> nextTier 3)
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 3)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        SubclassCard companionGrantingCard = buildCompanionGrantingFoundationCard(20L);
        Companion freshCompanion = buildCompanion(9L, sheet);
        freshCompanion.setOrigin(CompanionOrigin.MANUAL);
        when(subclassCardRepository.findById(20L)).thenReturn(Optional.of(companionGrantingCard));
        when(companionRepository.findById(9L)).thenReturn(Optional.of(freshCompanion));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.MULTICLASS).subclassCardId(20L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .newExperienceDescription("Tier 3 experience")
                .newCompanionId(9L)
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(freshCompanion.getOrigin()).isEqualTo(CompanionOrigin.SUBCLASS_FEATURE);
        assertThat(freshCompanion.getOriginSubclassCard()).isEqualTo(companionGrantingCard);
        assertThat(freshCompanion.getTrainings()).isEmpty();
    }

    // ==================== newCompanionId ====================

    @Test
    void levelUp_NewCompanionIdWithoutGrantingAdvancement_Throws() {
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .newCompanionId(9L)
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("grants the Companion feature");
    }

    @Test
    void levelUp_RestoreCase_RestoresSoftDeletedCompanion() {
        sheet.setLevel(4); // MULTICLASS requires tier 3+ (nextLevel 5 -> nextTier 3)
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 3)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        SubclassCard companionGrantingCard = buildCompanionGrantingFoundationCard(20L);
        Companion archived = buildCompanion(9L, sheet);
        archived.setOrigin(CompanionOrigin.SUBCLASS_FEATURE);
        archived.setOriginSubclassCard(companionGrantingCard);
        archived.softDelete();
        when(subclassCardRepository.findById(20L)).thenReturn(Optional.of(companionGrantingCard));
        when(companionRepository.findById(9L)).thenReturn(Optional.of(archived));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.MULTICLASS).subclassCardId(20L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .newExperienceDescription("Tier 3 experience")
                .newCompanionId(9L)
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(archived.isDeleted()).isFalse();
    }

    @Test
    void levelUp_NewCompanionIdBelongingToDifferentSheet_Throws() {
        sheet.setLevel(4); // MULTICLASS requires tier 3+ (nextLevel 5 -> nextTier 3)
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 3)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of());

        SubclassCard companionGrantingCard = buildCompanionGrantingFoundationCard(20L);
        CharacterSheet otherSheet = buildSheet(2);
        otherSheet.setId(2L);
        Companion foreignCompanion = buildCompanion(9L, otherSheet);
        when(subclassCardRepository.findById(20L)).thenReturn(Optional.of(companionGrantingCard));
        when(companionRepository.findById(9L)).thenReturn(Optional.of(foreignCompanion));

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.MULTICLASS).subclassCardId(20L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .newExperienceDescription("Tier 3 experience")
                .newCompanionId(9L)
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong");
    }

    // ==================== Companion Experience grant (tier transitions) ====================

    @Test
    void levelUp_TierTransition_GrantsCompanionExperience() {
        sheet.setLevel(1); // level 1 -> 2 is a tier transition
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(i -> {
            Experience e = i.getArgument(0);
            e.setId(77L);
            return e;
        });

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .newExperienceDescription("Battle hardened")
                .companionExperiences(List.of(CompanionExperienceGrant.builder()
                        .companionId(5L).description("Loyal tracker").build()))
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.AWARE).build()))
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(companion.getExperiences()).hasSize(1);
        Experience granted = companion.getExperiences().iterator().next();
        assertThat(granted.getDescription()).isEqualTo("Loyal tracker");
        assertThat(granted.getCharacterSheet()).isNull();
        assertThat(granted.getCompanion()).isEqualTo(companion);
    }

    @Test
    void levelUp_TierTransition_CompanionExperienceCapExceeded_Throws() {
        sheet.setLevel(1);
        Companion companion = buildCompanion(5L, sheet);
        for (int i = 0; i < 5; i++) {
            companion.getExperiences().add(Experience.builder().id((long) (200 + i)).companion(companion)
                    .createdBy(testOwner).description("Exp " + i).modifier(2).build());
        }
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .newExperienceDescription("Battle hardened")
                .companionExperiences(List.of(CompanionExperienceGrant.builder()
                        .companionId(5L).description("Sixth experience").build()))
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.AWARE).build()))
                .build();

        assertThatThrownBy(() -> levelUpService.levelUp(1L, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void levelUp_NonTierTransition_CompanionExperiencesSilentlyIgnored() {
        // sheet is already level 2 -> 3, both tier 2, not a transition
        Companion companion = buildCompanion(5L, sheet);
        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));

        LevelUpRequest request = twoBasicAdvancementsRequest().toBuilder()
                .companionExperiences(List.of(CompanionExperienceGrant.builder()
                        .companionId(5L).description("Should be ignored").build()))
                .companionTrainings(List.of(CompanionTrainingChoice.builder()
                        .companionId(5L).option(CompanionTrainingOption.AWARE).build()))
                .build();

        levelUpService.levelUp(1L, request, authentication);

        assertThat(companion.getExperiences()).isEmpty();
        verifyNoInteractions(experienceRepository);
    }

    // ==================== The priority deliverable: full round-trip ====================

    @Test
    void levelUpThenUndo_RestoresCompanionExactly() {
        sheet.setLevel(1); // tier transition
        Companion companion = buildCompanion(5L, sheet);
        Experience preExistingExp = Experience.builder().id(55L).companion(companion)
                .createdBy(testOwner).description("Tracking").modifier(2).build();
        companion.getExperiences().add(preExistingExp);

        // Expert Training grants the second pick this test needs (AWARE + INTELLIGENT), so the
        // round trip also exercises the picks-available bonus, not just the baseline.
        SubclassPath path = buildSubclassPath(50L);
        sheet.getSubclassCards().add(buildPathCard(51L, SubclassLevel.FOUNDATION, path, null));
        SubclassCard expertTrainingCard = buildPathCard(52L, SubclassLevel.SPECIALIZATION, path, "Expert Training");

        when(characterAdvancementLogRepository.findByCharacterSheetIdAndTier(1L, 2)).thenReturn(List.of());
        when(companionRepository.findActiveByCharacterSheetId(1L)).thenReturn(List.of(companion));
        when(subclassCardRepository.findById(52L)).thenReturn(Optional.of(expertTrainingCard));
        when(experienceRepository.findById(55L)).thenReturn(Optional.of(preExistingExp));
        when(companionRepository.findById(5L)).thenReturn(Optional.of(companion));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(i -> {
            Experience e = i.getArgument(0);
            if (e.getId() == null) {
                e.setId(77L);
            }
            return e;
        });

        LevelUpRequest request = LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.UPGRADE_SUBCLASS).subclassCardId(52L).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build()
                ))
                .newExperienceDescription("Battle hardened")
                .companionExperiences(List.of(CompanionExperienceGrant.builder()
                        .companionId(5L).description("Loyal tracker").build()))
                .companionTrainings(List.of(
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.AWARE).build(),
                        CompanionTrainingChoice.builder().companionId(5L).option(CompanionTrainingOption.INTELLIGENT)
                                .targetExperienceId(55L).build()
                ))
                .build();

        levelUpService.levelUp(1L, request, authentication);

        // Sanity check the level-up actually changed things before undoing.
        assertThat(companion.getTrainings()).hasSize(2);
        assertThat(companion.getExperiences()).hasSize(2);
        assertThat(preExistingExp.getModifier()).isEqualTo(3);
        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(12); // base 10 + 2 Aware

        // Capture what was actually logged so undo can read it back exactly as levelUp() would
        // have produced through the real save; the mocked characterAdvancementLogRepository.save
        // stub already returns the log entry, so no ArgumentCaptor is needed here.

        String advancementDataJson = capturedLogJson();
        CharacterAdvancementLog logEntry = CharacterAdvancementLog.builder()
                .id(1L).characterSheet(sheet).fromLevel(1).toLevel(2).tier(2)
                .advancementData(advancementDataJson).build();
        when(characterAdvancementLogRepository.findTopByCharacterSheetIdOrderByToLevelDesc(1L))
                .thenReturn(Optional.of(logEntry));

        levelUpService.undoLevelUp(1L, authentication);

        assertThat(companion.getTrainings()).isEmpty();
        assertThat(companion.getExperiences()).containsExactly(preExistingExp);
        assertThat(preExistingExp.getModifier()).isEqualTo(2);
        assertThat(CompanionDerivationService.evasion(companion)).isEqualTo(10); // back to base
        assertThat(sheet.getLevel()).isEqualTo(1);
    }

    @Test
    void undoLevelUp_HardDeletedCompanion_ReversalIsNoOpNotThrow() throws Exception {
        String advancementDataJson = objectMapper.writeValueAsString(Map.of(
                "previousValues", Map.of("companionExperienceModifiers", Map.of()),
                "advancements", List.of(Map.of("type", "GAIN_HP")),
                "companionTrainings", List.of(Map.of("companionId", 999L, "trainingId", 1L, "option", "AWARE"))
        ));
        CharacterAdvancementLog logEntry = CharacterAdvancementLog.builder()
                .id(1L).characterSheet(sheet).fromLevel(1).toLevel(2).tier(1).advancementData(advancementDataJson).build();
        when(characterAdvancementLogRepository.findTopByCharacterSheetIdOrderByToLevelDesc(1L))
                .thenReturn(Optional.of(logEntry));
        when(companionRepository.findById(999L)).thenReturn(Optional.empty());
        sheet.setLevel(2);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> levelUpService.undoLevelUp(1L, authentication));
    }

    @Test
    void undoLevelUp_ResilientRemoved_ClampsStressMarked() throws Exception {
        Companion companion = buildCompanion(5L, sheet);
        companion.setBaseStressMax(3);
        companion.setStressMarked(4); // legal only with the Resilient bonus (base 3 + 1 = 4)
        CompanionTraining resilient = CompanionTraining.builder().id(300L).companion(companion)
                .option(CompanionTrainingOption.RESILIENT).acquiredAtLevel(2).build();
        companion.getTrainings().add(resilient);

        String advancementDataJson = objectMapper.writeValueAsString(Map.of(
                "previousValues", Map.of("companionExperienceModifiers", Map.of()),
                "advancements", List.of(Map.of("type", "GAIN_HP")),
                "companionTrainings", List.of(Map.of("companionId", 5L, "trainingId", 300L, "option", "RESILIENT"))
        ));
        CharacterAdvancementLog logEntry = CharacterAdvancementLog.builder()
                .id(1L).characterSheet(sheet).fromLevel(1).toLevel(2).tier(1).advancementData(advancementDataJson).build();
        when(characterAdvancementLogRepository.findTopByCharacterSheetIdOrderByToLevelDesc(1L))
                .thenReturn(Optional.of(logEntry));
        when(companionRepository.findById(5L)).thenReturn(Optional.of(companion));
        sheet.setLevel(2);

        levelUpService.undoLevelUp(1L, authentication);

        assertThat(companion.getTrainings()).isEmpty();
        assertThat(companion.getStressMarked()).isEqualTo(3); // clamped to the new derived max
    }

    // ==================== Helpers ====================

    /**
     * Captures the {@code advancementData} JSON the preceding {@code levelUp()} call saved, so
     * a test can feed it straight back into {@code undoLevelUp} without hand-authoring the JSON
     * shape -- the round-trip test's whole point is exercising the real serialize/apply path.
     */
    private String capturedLogJson() {
        org.mockito.ArgumentCaptor<CharacterAdvancementLog> captor =
                org.mockito.ArgumentCaptor.forClass(CharacterAdvancementLog.class);
        verify(characterAdvancementLogRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue().getAdvancementData();
    }

    private LevelUpRequest twoBasicAdvancementsRequest() {
        return LevelUpRequest.builder()
                .advancements(List.of(
                        AdvancementChoice.builder().type(AdvancementType.GAIN_HP).build(),
                        AdvancementChoice.builder().type(AdvancementType.GAIN_STRESS).build()
                ))
                .build();
    }

    private CharacterSheet buildSheet(int level) {
        return CharacterSheet.builder()
                .id(1L)
                .name("Test Character")
                .level(level)
                .proficiency(0)
                .evasion(10)
                .armorMax(5)
                .armorMarked(0)
                .majorDamageThreshold(3)
                .severeDamageThreshold(6)
                .agilityModifier(0).agilityMarked(false)
                .strengthModifier(0).strengthMarked(false)
                .finesseModifier(0).finesseMarked(false)
                .instinctModifier(0).instinctMarked(false)
                .presenceModifier(0).presenceMarked(false)
                .knowledgeModifier(0).knowledgeMarked(false)
                .hitPointMax(6).hitPointMarked(0)
                .stressMax(6).stressMarked(0)
                .hopeMax(3).hopeMarked(0)
                .gold(50)
                .owner(testOwner)
                .subclassCards(new HashSet<>())
                .experiences(new HashSet<>())
                .domainCards(new HashSet<>())
                .communityCards(new HashSet<>())
                .ancestryCards(new HashSet<>())
                .characterSheetWeapons(new HashSet<>())
                .characterSheetArmors(new HashSet<>())
                .characterSheetLoot(new HashSet<>())
                .companions(new HashSet<>())
                .characterSheetDomainCards(new HashSet<>())
                .advancementLogs(new HashSet<>())
                .build();
    }

    private Companion buildCompanion(Long id, CharacterSheet owningSheet) {
        return Companion.builder()
                .id(id)
                .characterSheet(owningSheet)
                .name("Rufus")
                .attackName("Bite")
                .baseAttackRange(Range.MELEE)
                .baseDamageDice(DiceType.D6)
                .baseEvasion(10)
                .baseStressMax(3)
                .stressMarked(0)
                .origin(CompanionOrigin.SUBCLASS_FEATURE)
                .advancesOnLevelUp(true)
                .trainings(new HashSet<>())
                .experiences(new HashSet<>())
                .build();
    }

    /**
     * Builds a foundation subclass card carrying the "Companion" subclass feature, mirroring the
     * Beastbound Ranger's foundation feature (prod ids 19/20/21) but detected by name+type, not id.
     */
    private SubclassCard buildCompanionGrantingFoundationCard(Long id) {
        return buildPathCard(id, SubclassLevel.FOUNDATION, buildSubclassPath(id + 7000), "Companion");
    }

    private SubclassPath buildSubclassPath(Long id) {
        Class beastbound = Class.builder().id(id + 6000).name("Ranger").build();
        return SubclassPath.builder().id(id).name("Beastbound")
                .associatedClass(beastbound).associatedDomains(Set.of()).build();
    }

    /**
     * Builds a subclass card on a given path, optionally carrying a named subclass feature
     * (e.g. "Companion", "Expert Training", "Advanced Training"). Pass {@code null} for
     * {@code featureName} to build a plain card with no notable feature (e.g. a foundation card
     * only needed to satisfy {@code validateUpgradeSubclass}'s "next level in path" check).
     */
    private SubclassCard buildPathCard(Long id, SubclassLevel level, SubclassPath path, String featureName) {
        Set<Feature> features = featureName == null ? new HashSet<>() : new HashSet<>(Set.of(
                Feature.builder().id(id + 5000).name(featureName).featureType(FeatureType.SUBCLASS).build()));
        return SubclassCard.builder().id(id)
                .name((featureName != null ? featureName : "Card") + " " + id)
                .level(level).subclassPath(path)
                .features(features)
                .build();
    }
}
