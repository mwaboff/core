package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateEncounterRunRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRunAdversaryRequest;
import com.aboff.core.model.dto.dh.response.EncounterRunResponse;
import com.aboff.core.model.dto.dh.response.ExperienceResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.entity.dh.EncounterRunAdversary;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.EncounterRunStatus;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EncounterRunAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRunRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EncounterRunService.
 * <p>
 * Covers snapshot-on-start isolation from the source encounter, HP/Stress clamping, and the
 * full authorization matrix (owner / campaign GM / unrelated user / MODERATOR+, for both a
 * standalone and a campaign-tagged run) -- including an explicit regression test that a user
 * belonging to no campaign at all can start and mutate a standalone run, since that is the
 * entire reason this feature is designed campaign-free.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncounterRunServiceTest {

    @Mock
    private EncounterRunRepository encounterRunRepository;

    @Mock
    private EncounterRunAdversaryRepository encounterRunAdversaryRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdversaryRepository adversaryRepository;

    @Mock
    private EncounterService encounterService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private AdversaryService adversaryService;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private ContentAccessService contentAccessService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EncounterRunService encounterRunService;

    private User owner;
    private User outsider;
    private User moderator;
    private CustomUserDetails ownerDetails;
    private CustomUserDetails outsiderDetails;
    private CustomUserDetails moderatorDetails;
    private Encounter encounter;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").role(Role.USER).build();
        outsider = User.builder().id(2L).username("outsider").role(Role.USER).build();
        moderator = User.builder().id(3L).username("moderator").role(Role.MODERATOR).build();

        ownerDetails = new CustomUserDetails(owner);
        outsiderDetails = new CustomUserDetails(outsider);
        moderatorDetails = new CustomUserDetails(moderator);

        encounter = Encounter.builder()
                .id(10L)
                .name("Goblin Ambush")
                .createdBy(owner)
                .isOfficial(false)
                .isPublic(false)
                .encounterAdversaries(new ArrayList<>())
                .build();

        campaign = Campaign.builder()
                .id(20L)
                .name("The Hollow Road")
                .creator(owner)
                .gameMasters(new HashSet<>())
                .build();

        when(encounterRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(encounter));
        when(campaignRepository.findActiveById(20L)).thenReturn(Optional.of(campaign));
        when(encounterRunRepository.save(any(EncounterRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encounterRunAdversaryRepository.save(any(EncounterRunAdversary.class))).thenAnswer(inv -> inv.getArgument(0));
        // Default every adversary to visible; the redaction gate itself is covered by a
        // dedicated test below.
        when(contentAccessService.mayView(any(), any())).thenReturn(true);
    }

    private void authAs(CustomUserDetails userDetails) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    private Adversary testAdversary(Long id) {
        return Adversary.builder()
                .id(id)
                .name("Goblin Scout " + id)
                .tier(1)
                .adversaryType(AdversaryType.STANDARD)
                .difficulty(11)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(6)
                .stressMax(3)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(owner)
                .build();
    }

    private EncounterAdversary addInstance(Long adversaryId, String label, Integer tierOverride, int displayOrder) {
        EncounterAdversary instance = EncounterAdversary.builder()
                .encounter(encounter)
                .adversary(testAdversary(adversaryId))
                .label(label)
                .tierOverride(tierOverride)
                .displayOrder(displayOrder)
                .build();
        encounter.getEncounterAdversaries().add(instance);
        return instance;
    }

    private EncounterRun activeRun(User startedBy, Campaign runCampaign, EncounterRunAdversary... instances) {
        EncounterRun run = EncounterRun.builder()
                .id(100L)
                .encounter(encounter)
                .campaign(runCampaign)
                .startedBy(startedBy)
                .status(EncounterRunStatus.ACTIVE)
                .encounterRunAdversaries(new ArrayList<>(List.of(instances)))
                .build();
        for (EncounterRunAdversary instance : instances) {
            instance.setEncounterRun(run);
        }
        return run;
    }

    private EncounterRunAdversary runInstance(Long id, Adversary adversary, int hp, int stress, int displayOrder) {
        return EncounterRunAdversary.builder()
                .id(id)
                .adversary(adversary)
                .hitPointsMarked(hp)
                .stressMarked(stress)
                .isDefeated(false)
                .displayOrder(displayOrder)
                .build();
    }

    // ==================== START RUN / SNAPSHOTTING ====================

    @Test
    void startRun_SnapshotsEachSourceInstance() {
        authAs(ownerDetails);
        addInstance(50L, "Archer A", null, 0);
        addInstance(51L, "Archer B", 3, 1);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        assertThat(response.getAdversaries()).hasSize(2);
    }

    @Test
    void startRun_CopiesLabelAndTierOverrideFromTemplate() {
        authAs(ownerDetails);
        addInstance(50L, "Archer A", 3, 0);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        assertThat(response.getAdversaries().get(0).getLabel()).isEqualTo("Archer A");
        assertThat(response.getAdversaries().get(0).getTierOverride()).isEqualTo(3);
    }

    @Test
    void startRun_NewInstancesStartAtZeroHitPointsAndStressNotDefeated() {
        authAs(ownerDetails);
        addInstance(50L, "Archer A", null, 0);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        EncounterRunResponse.EncounterRunAdversaryResponse instance = response.getAdversaries().get(0);
        assertThat(instance.getHitPointsMarked()).isZero();
        assertThat(instance.getStressMarked()).isZero();
        assertThat(instance.getIsDefeated()).isFalse();
        // A Pool (Hope & Fear) or any other token-driven feature is empty when the scene begins.
        assertThat(instance.getTokens()).isZero();
    }

    @Test
    void startRun_SnapshotsFieldsIndependentlyOfTheSourceEncounter() {
        // This is the whole reason snapshots exist: editing the source encounter mid-run must
        // not corrupt a run already in progress.
        authAs(ownerDetails);
        EncounterAdversary template = addInstance(50L, "Archer A", null, 0);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        // Simulate the GM editing the saved encounter after the run has started.
        template.setLabel("Renamed After Run Started");
        template.setTierOverride(4);

        assertThat(response.getAdversaries().get(0).getLabel()).isEqualTo("Archer A");
        assertThat(response.getAdversaries().get(0).getTierOverride()).isNull();
    }

    @Test
    void startRun_PreservesDisplayOrderFromSource() {
        authAs(ownerDetails);
        addInstance(50L, "Archer A", null, 0);
        addInstance(51L, "Archer B", null, 1);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        assertThat(response.getAdversaries().get(0).getDisplayOrder()).isZero();
        assertThat(response.getAdversaries().get(1).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void startRun_NoRequestBody_StartsStandaloneRun() {
        authAs(ownerDetails);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        assertThat(response.getCampaignId()).isNull();
    }

    @Test
    void startRun_NullCampaignIdInRequest_StartsStandaloneRun() {
        authAs(ownerDetails);

        EncounterRunResponse response = encounterRunService.startRun(
                10L, CreateEncounterRunRequest.builder().build(), authentication);

        assertThat(response.getCampaignId()).isNull();
    }

    @Test
    void startRun_WithCampaignId_TagsTheRun() {
        authAs(ownerDetails);

        EncounterRunResponse response = encounterRunService.startRun(
                10L, CreateEncounterRunRequest.builder().campaignId(20L).build(), authentication);

        assertThat(response.getCampaignId()).isEqualTo(20L);
    }

    @Test
    void startRun_SetsStatusActive() {
        authAs(ownerDetails);

        EncounterRunResponse response = encounterRunService.startRun(10L, null, authentication);

        assertThat(response.getStatus()).isEqualTo(EncounterRunStatus.ACTIVE);
    }

    @Test
    void startRun_UnknownEncounter_Throws() {
        when(encounterRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.startRun(999L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void startRun_UnknownCampaign_Throws() {
        authAs(ownerDetails);
        when(campaignRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.startRun(
                10L, CreateEncounterRunRequest.builder().campaignId(999L).build(), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void startRun_WithoutViewPermissionOnEncounter_Throws() {
        authAs(outsiderDetails);
        doThrow(new InsufficientPermissionsException("nope"))
                .when(encounterService).validateViewPermission(any(), any());

        assertThatThrownBy(() -> encounterRunService.startRun(10L, null, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(encounterRunRepository, never()).save(any());
    }

    @Test
    void startRun_TaggedToEndedCampaign_Throws() {
        authAs(ownerDetails);
        doThrow(new IllegalStateException("Cannot start an encounter run for an ended campaign"))
                .when(campaignService).validateNotEnded(any(), anyString());

        assertThatThrownBy(() -> encounterRunService.startRun(
                10L, CreateEncounterRunRequest.builder().campaignId(20L).build(), authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startRun_AuditsTheStart() {
        authAs(ownerDetails);

        encounterRunService.startRun(10L, null, authentication);

        verify(auditLogger).log(eq(com.aboff.core.model.enums.AuditAction.ENCOUNTER_RUN_STARTED), any(), anyString());
    }

    // ==================== AUTHORIZATION MATRIX ====================

    @Test
    void getRun_AsOwner_Standalone_Returns() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getRun_AsOwner_Standalone_NeverConsultsCampaignService() {
        // Regression guard: a user belonging to no campaign at all must be able to access a
        // standalone run without any campaign lookup happening.
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        encounterRunService.getRun(100L, authentication);

        verifyNoInteractions(campaignService);
    }

    @Test
    void getRun_AsUnrelatedUser_Standalone_Throws() {
        authAs(outsiderDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.getRun(100L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void getRun_AsModerator_Standalone_Returns() {
        authAs(moderatorDetails);
        when(roleHierarchyService.hasModeratorOrHigher(moderatorDetails)).thenReturn(true);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getRun_AsOwner_CampaignTagged_Returns() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getRun_AsCampaignGameMaster_CampaignTagged_Returns() {
        User gm = User.builder().id(4L).username("gm").role(Role.USER).build();
        CustomUserDetails gmDetails = new CustomUserDetails(gm);
        authAs(gmDetails);
        when(campaignService.hasGameMasterAccess(campaign, authentication)).thenReturn(true);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getRun_AsUnrelatedUser_CampaignTagged_Throws() {
        authAs(outsiderDetails);
        when(campaignService.hasGameMasterAccess(campaign, authentication)).thenReturn(false);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.getRun(100L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void getRun_AsModerator_CampaignTagged_Returns() {
        authAs(moderatorDetails);
        when(roleHierarchyService.hasModeratorOrHigher(moderatorDetails)).thenReturn(true);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getRun_UnknownRun_Throws() {
        when(encounterRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.getRun(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getRun_ExpandsAdversaryStatBlocks() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getAdversaries().get(0).getAdversary()).isNotNull();
        assertThat(response.getAdversaries().get(0).getAdversary().getName()).isEqualTo(adversary.getName());
    }

    @Test
    void getRun_AdversaryGatedNonSrd_ReturnsRedactedStatBlockStub() {
        // A run is visible to any GM tagged to its campaign or any MODERATOR (see the class
        // javadoc), neither of which implies ADMIN/OWNER or an SRD grant, so a licensing-gated
        // adversary must still redact here even though the caller can see the run itself.
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        adversary.setIsOfficial(true);
        adversary.setSrd(false);
        adversary.setExpansion(Expansion.builder().id(9L).name("Hope & Fear").build());
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(contentAccessService.mayView(true, false)).thenReturn(false);

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        var statBlock = response.getAdversaries().get(0).getAdversary();
        assertThat(statBlock.getRestricted()).isTrue();
        assertThat(statBlock.getId()).isEqualTo(50L);
        assertThat(statBlock.getExpansionName()).isEqualTo("Hope & Fear");
        assertThat(statBlock.getName()).isNull();
        assertThat(statBlock.getDescription()).isNull();
        assertThat(statBlock.getFeatures()).isNull();
        assertThat(statBlock.getExperiences()).isNull();
        verify(adversaryService, never()).toFeatureResponses(any(), any());
        verify(adversaryService, never()).toExperienceResponses(any());
    }

    @Test
    void getRun_IncludesAdversaryFeaturesAndExperiences() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        Feature feature = Feature.builder().id(1L).name("Relentless (3) - Passive").featureType(FeatureType.ADVERSARY).build();
        Experience experience = Experience.builder().id(2L).description("Combat Expert").modifier(2).build();
        Adversary withFeature = testAdversary(50L);
        withFeature.setFeatures(new HashSet<>(Set.of(feature)));
        Adversary withExperience = testAdversary(50L);
        withExperience.setExperiences(new HashSet<>(Set.of(experience)));
        when(adversaryRepository.findAllByIdInWithFeatures(List.of(50L))).thenReturn(List.of(withFeature));
        when(adversaryRepository.findAllByIdInWithExperiences(List.of(50L))).thenReturn(List.of(withExperience));

        FeatureResponse featureResponse = FeatureResponse.builder().id(1L).name("Relentless (3) - Passive").build();
        ExperienceResponse experienceResponse = ExperienceResponse.builder().id(2L).description("Combat Expert").modifier(2).build();
        when(adversaryService.toFeatureResponses(Set.of(feature), Set.of())).thenReturn(Set.of(featureResponse));
        when(adversaryService.toExperienceResponses(Set.of(experience))).thenReturn(Set.of(experienceResponse));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        var statBlock = response.getAdversaries().get(0).getAdversary();
        assertThat(statBlock.getFeatures()).containsExactly(featureResponse);
        assertThat(statBlock.getFeatureIds()).containsExactly(1L);
        assertThat(statBlock.getExperiences()).containsExactly(experienceResponse);
        assertThat(statBlock.getExperienceIds()).containsExactly(2L);
    }

    @Test
    void getRun_AdversaryWithNoFeaturesOrExperiences_OmitsThemFromStatBlock() {
        // Batch-load queries default (via Mockito's unstubbed-collection behavior) to empty
        // lists, matching an adversary with no features/experiences at all.
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        var statBlock = response.getAdversaries().get(0).getAdversary();
        assertThat(statBlock.getFeatures()).isNull();
        assertThat(statBlock.getFeatureIds()).isNull();
        assertThat(statBlock.getExperiences()).isNull();
        assertThat(statBlock.getExperienceIds()).isNull();
        verify(adversaryService, never()).toFeatureResponses(any(), any());
        verify(adversaryService, never()).toExperienceResponses(any());
    }

    @Test
    void getRun_BatchLoadsFeaturesOncePerDistinctAdversaryNotPerInstance() {
        // A run holding multiple instances of the same adversary must load its features once,
        // not once per instance -- the whole point of the batching.
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null,
                runInstance(200L, adversary, 0, 0, 0),
                runInstance(201L, adversary, 0, 0, 1),
                runInstance(202L, adversary, 0, 0, 2));
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        encounterRunService.getRun(100L, authentication);

        verify(adversaryRepository, times(1)).findAllByIdInWithFeatures(List.of(50L));
        verify(adversaryRepository, times(1)).findAllByIdInWithExperiences(List.of(50L));
    }

    @Test
    void getRun_EncounterWithoutEnvironment_EnvironmentIdIsNull() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getEnvironmentId()).isNull();
    }

    @Test
    void getRun_EncounterWithEnvironment_ReturnsEnvironmentId() {
        authAs(ownerDetails);
        encounter.setEnvironment(Environment.builder().id(30L).name("Sunken Ruins")
                .tier(1).environmentType(EnvironmentType.EXPLORATION).build());
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.getRun(100L, authentication);

        assertThat(response.getEnvironmentId()).isEqualTo(30L);
    }

    // ==================== UPDATE RUN ADVERSARY ====================

    @Test
    void updateRunAdversary_SetsHitPointsMarked() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().hitPointsMarked(4).build(), authentication);

        assertThat(response.getAdversaries().get(0).getHitPointsMarked()).isEqualTo(4);
    }

    @Test
    void updateRunAdversary_ClampsHitPointsMarkedToAdversaryMax() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L); // hitPointMax = 6
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().hitPointsMarked(99).build(), authentication);

        assertThat(response.getAdversaries().get(0).getHitPointsMarked()).isEqualTo(6);
    }

    @Test
    void updateRunAdversary_ClampsStressMarkedToAdversaryMax() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L); // stressMax = 3
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().stressMarked(99).build(), authentication);

        assertThat(response.getAdversaries().get(0).getStressMarked()).isEqualTo(3);
    }

    @Test
    void updateRunAdversary_SetsTokens() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().tokens(5).build(), authentication);

        assertThat(response.getAdversaries().get(0).getTokens()).isEqualTo(5);
    }

    @Test
    void updateRunAdversary_TokensBackToZero() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        instance.setTokens(5);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().tokens(0).build(), authentication);

        assertThat(response.getAdversaries().get(0).getTokens()).isZero();
    }

    @Test
    void updateRunAdversary_TokensNotClampedToAnyMax() {
        // Unlike hitPointsMarked/stressMarked, tokens has no adversary-derived ceiling -- a Pool
        // (Hope & Fear) can hold any number.
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().tokens(99).build(), authentication);

        assertThat(response.getAdversaries().get(0).getTokens()).isEqualTo(99);
    }

    @Test
    void updateRunAdversary_NegativeTokens_ClampsToZero() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        instance.setTokens(3);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().tokens(-5).build(), authentication);

        assertThat(response.getAdversaries().get(0).getTokens()).isZero();
    }

    @Test
    void updateRunAdversary_NegativeValue_ClampsToZero() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 4, 2, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().hitPointsMarked(-5).build(), authentication);

        assertThat(response.getAdversaries().get(0).getHitPointsMarked()).isZero();
    }

    @Test
    void updateRunAdversary_SetsIsDefeated() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().isDefeated(true).build(), authentication);

        assertThat(response.getAdversaries().get(0).getIsDefeated()).isTrue();
    }

    @Test
    void updateRunAdversary_SanitizesNote() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L,
                UpdateEncounterRunAdversaryRequest.builder().note("<script>alert('x')</script>Flanking").build(),
                authentication);

        assertThat(response.getAdversaries().get(0).getNote()).doesNotContain("<script>");
    }

    @Test
    void updateRunAdversary_NullFields_LeavesExistingStateUnchanged() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 3, 1, 0);
        instance.setNote("Existing note");
        instance.setTokens(2);
        EncounterRun run = activeRun(owner, null, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        EncounterRunResponse response = encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().build(), authentication);

        EncounterRunResponse.EncounterRunAdversaryResponse updated = response.getAdversaries().get(0);
        assertThat(updated.getHitPointsMarked()).isEqualTo(3);
        assertThat(updated.getStressMarked()).isEqualTo(1);
        assertThat(updated.getNote()).isEqualTo("Existing note");
        assertThat(updated.getTokens()).isEqualTo(2);
    }

    @Test
    void updateRunAdversary_UnknownRun_Throws() {
        when(encounterRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                999L, 200L, UpdateEncounterRunAdversaryRequest.builder().build(), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateRunAdversary_UnknownInstance_Throws() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(encounterRunAdversaryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                100L, 999L, UpdateEncounterRunAdversaryRequest.builder().build(), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateRunAdversary_InstanceBelongsToDifferentRun_Throws() {
        authAs(ownerDetails);

        EncounterRun requestedRun = EncounterRun.builder()
                .id(100L).encounter(encounter).startedBy(owner).status(EncounterRunStatus.ACTIVE)
                .encounterRunAdversaries(new ArrayList<>())
                .build();
        EncounterRun differentRun = EncounterRun.builder()
                .id(999L).encounter(encounter).startedBy(owner).status(EncounterRunStatus.ACTIVE)
                .encounterRunAdversaries(new ArrayList<>())
                .build();
        EncounterRunAdversary instance = runInstance(200L, testAdversary(50L), 0, 0, 0);
        instance.setEncounterRun(differentRun);

        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(requestedRun));
        when(encounterRunAdversaryRepository.findById(200L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().build(), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateRunAdversary_OnCompletedRun_Throws() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, null, instance);
        run.setStatus(EncounterRunStatus.COMPLETED);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().hitPointsMarked(1).build(), authentication))
                .isInstanceOf(IllegalStateException.class);

        verify(encounterRunAdversaryRepository, never()).save(any());
    }

    @Test
    void updateRunAdversary_TaggedToEndedCampaign_Throws() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRunAdversary instance = runInstance(200L, adversary, 0, 0, 0);
        EncounterRun run = activeRun(owner, campaign, instance);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        doThrow(new IllegalStateException("Cannot update encounter run adversaries for an ended campaign"))
                .when(campaignService).validateNotEnded(any(), anyString());

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().hitPointsMarked(1).build(), authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateRunAdversary_WithoutAccess_Throws() {
        authAs(outsiderDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.updateRunAdversary(
                100L, 200L, UpdateEncounterRunAdversaryRequest.builder().build(), authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(encounterRunAdversaryRepository, never()).findById(any());
    }

    // ==================== COMPLETE ====================

    @Test
    void completeRun_SetsStatusCompletedAndEndedAt() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        EncounterRunResponse response = encounterRunService.completeRun(100L, authentication);

        assertThat(response.getStatus()).isEqualTo(EncounterRunStatus.COMPLETED);
        assertThat(response.getEndedAt()).isNotNull();
    }

    @Test
    void completeRun_AlreadyCompleted_Throws() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        run.setStatus(EncounterRunStatus.COMPLETED);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.completeRun(100L, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeRun_WithoutAccess_Throws() {
        authAs(outsiderDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.completeRun(100L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void completeRun_TaggedToEndedCampaign_Throws() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));
        doThrow(new IllegalStateException("Cannot complete encounter runs for an ended campaign"))
                .when(campaignService).validateNotEnded(any(), anyString());

        assertThatThrownBy(() -> encounterRunService.completeRun(100L, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== DELETE ====================

    @Test
    void deleteRun_DeletesTheEntity() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        encounterRunService.deleteRun(100L, authentication);

        verify(encounterRunRepository).delete(run);
    }

    @Test
    void deleteRun_AllowedEvenWhenTaggedCampaignHasEnded() {
        // Discarding is cleanup, not play -- matches CountdownService#deleteCountdown and
        // CampaignService#removeCharacterSheet, neither of which calls validateNotEnded.
        authAs(ownerDetails);
        campaign.endCampaign();
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        encounterRunService.deleteRun(100L, authentication);

        verify(campaignService, never()).validateNotEnded(any(), anyString());
        verify(encounterRunRepository).delete(run);
    }

    @Test
    void deleteRun_WithoutAccess_DoesNotDelete() {
        authAs(outsiderDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> encounterRunService.deleteRun(100L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(encounterRunRepository, never()).delete(any());
    }

    @Test
    void deleteRun_UnknownRun_Throws() {
        when(encounterRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.deleteRun(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== LIST ====================

    @Test
    void listRuns_NoCampaignId_ReturnsCallersOwnRuns() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, null);
        when(encounterRunRepository.findByStartedByIdAndOptionalStatus(1L, null)).thenReturn(List.of(run));

        List<EncounterRunResponse> responses = encounterRunService.listRuns(null, null, authentication);

        assertThat(responses).hasSize(1);
        verify(encounterRunRepository, never()).findByCampaignIdAndOptionalStatus(any(), any());
    }

    @Test
    void listRuns_WithStatusFilter_PassesThroughToRepository() {
        authAs(ownerDetails);
        when(encounterRunRepository.findByStartedByIdAndOptionalStatus(1L, EncounterRunStatus.ACTIVE))
                .thenReturn(List.of());

        encounterRunService.listRuns(EncounterRunStatus.ACTIVE, null, authentication);

        verify(encounterRunRepository).findByStartedByIdAndOptionalStatus(1L, EncounterRunStatus.ACTIVE);
    }

    @Test
    void listRuns_WithCampaignId_RequiresGameMasterAccess() {
        authAs(outsiderDetails);
        doThrow(new InsufficientPermissionsException("nope"))
                .when(campaignService).validateGameMasterAccess(any(), any(), anyString());

        assertThatThrownBy(() -> encounterRunService.listRuns(null, 20L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(encounterRunRepository, never()).findByCampaignIdAndOptionalStatus(any(), any());
    }

    @Test
    void listRuns_WithCampaignId_ReturnsThatCampaignsRuns() {
        authAs(ownerDetails);
        EncounterRun run = activeRun(owner, campaign);
        when(encounterRunRepository.findByCampaignIdAndOptionalStatus(20L, null)).thenReturn(List.of(run));

        List<EncounterRunResponse> responses = encounterRunService.listRuns(null, 20L, authentication);

        assertThat(responses).hasSize(1);
        verify(encounterRunRepository, never()).findByStartedByIdAndOptionalStatus(any(), any());
    }

    @Test
    void listRuns_UnknownCampaign_Throws() {
        when(campaignRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterRunService.listRuns(null, 999L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listRuns_DoesNotExpandAdversaryStatBlocks() {
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findByStartedByIdAndOptionalStatus(1L, null)).thenReturn(List.of(run));

        List<EncounterRunResponse> responses = encounterRunService.listRuns(null, null, authentication);

        assertThat(responses.get(0).getAdversaries().get(0).getAdversary()).isNull();
    }

    @Test
    void listRuns_DoesNotBatchLoadFeaturesOrExperiences() {
        // The list endpoint must stay lightweight -- no per-adversary batch query at all, not
        // even an empty-result one.
        authAs(ownerDetails);
        Adversary adversary = testAdversary(50L);
        EncounterRun run = activeRun(owner, null, runInstance(200L, adversary, 0, 0, 0));
        when(encounterRunRepository.findByStartedByIdAndOptionalStatus(1L, null)).thenReturn(List.of(run));

        encounterRunService.listRuns(null, null, authentication);

        verify(adversaryRepository, never()).findAllByIdInWithFeatures(any());
        verify(adversaryRepository, never()).findAllByIdInWithExperiences(any());
    }
}
