package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateEncounterRequest;
import com.aboff.core.model.dto.dh.request.UpdateEncounterRequest;
import com.aboff.core.model.dto.dh.response.EncounterResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.EncounterAdversary;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.enums.AdversaryType;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.EncounterAdversaryRepository;
import com.aboff.core.repository.dh.EncounterRepository;
import com.aboff.core.repository.dh.EnvironmentRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EncounterService.
 * <p>
 * EncounterService had no test coverage before this Battle Point / encounter model work; this
 * class covers the logic touched here: Battle Point delegation to {@link BattlePointCalculator}
 * (suggested budget, spent points, Minion grouping), the environment relation, the richer
 * adversary entries with backward compatibility for the deprecated bare ID list, per-instance
 * label/tier-override/display-order, and the existing permission model.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private EncounterAdversaryRepository encounterAdversaryRepository;

    @Mock
    private AdversaryRepository adversaryRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EncounterService encounterService;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private User ownerUser;
    private CustomUserDetails regularUserDetails;
    private CustomUserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        regularUser = User.builder().id(1L).username("regular").email("regular@test.com").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("other").email("other@test.com").role(Role.USER).build();
        adminUser = User.builder().id(3L).username("admin").email("admin@test.com").role(Role.ADMIN).build();
        ownerUser = User.builder().id(4L).username("owner").email("owner@test.com").role(Role.OWNER).build();

        regularUserDetails = new CustomUserDetails(regularUser);
        adminUserDetails = new CustomUserDetails(adminUser);
    }

    // ==================== GET ALL / VIEW PERMISSIONS ====================

    @Test
    void getAllEncounters_AsRegularUser_UsesAccessibleQuery() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        Page<Encounter> page = new PageImpl<>(List.of(encounter));
        when(encounterRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        var response = encounterService.getAllEncounters(
                0, 20, false, null, null, null, null, null, authentication);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        verify(encounterRepository, never()).findAllWithFilters(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void getEncounterById_PrivateNotCreatorNotModerator_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().createdBy(otherUser).isPublic(false).build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> encounterService.getEncounterById(1L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== BATTLE POINTS ====================

    @Test
    void getEncounterById_NoAdversariesOrPartySize_ComputesZeroSpentAndBaseBudget() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        assertThat(response.getSpentBattlePoints()).isZero();
        assertThat(response.getSuggestedBattlePoints()).isEqualTo(2); // (3*0)+2, party size unset
    }

    @Test
    void getEncounterById_PartySizeFourWithEightMinions_SpentIsTwoGroups() {
        // Arrange - the manual QA script's example: 8 Minions, party 4 -> 2 points spent
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().partySize(4).build();
        encounter.setEncounterAdversaries(minionInstances(encounter, 8));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        assertThat(response.getSpentBattlePoints()).isEqualTo(2);
        assertThat(response.getSuggestedBattlePoints()).isEqualTo(14);
    }

    @Test
    void getEncounterById_HarderAdjustment_RaisesSuggestedBudget() {
        // Arrange - toggling "more dangerous" (harder) on a party of 4: 14 -> 16
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().partySize(4).adjustmentHarder(true).build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        assertThat(response.getSuggestedBattlePoints()).isEqualTo(16);
    }

    @Test
    void getEncounterById_WorkedExample_TwoBruisersTwoStandardsFourMinions_SpentIsThirteen() {
        // Arrange - the rulebook's worked example
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().partySize(4).build();
        List<EncounterAdversary> instances = new ArrayList<>();
        instances.add(instanceOf(encounter, AdversaryType.BRUISER, 0));
        instances.add(instanceOf(encounter, AdversaryType.BRUISER, 1));
        instances.add(instanceOf(encounter, AdversaryType.STANDARD, 2));
        instances.add(instanceOf(encounter, AdversaryType.STANDARD, 3));
        instances.addAll(minionInstances(encounter, 4));
        encounter.setEncounterAdversaries(instances);
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        assertThat(response.getSpentBattlePoints()).isEqualTo(13);
    }

    // ==================== RETIER (tierOverride) ====================

    @Test
    void getEncounterById_InstanceWithTierOverride_IncludesRetieredStatistics() {
        // Arrange - a Tier 1 Standard retiered to Tier 3
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        EncounterAdversary instance = instanceOf(encounter, AdversaryType.STANDARD, 0);
        instance.setTierOverride(3);
        encounter.setEncounterAdversaries(List.of(instance));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        EncounterResponse.EncounterAdversaryResponse adversaryResponse = response.getAdversaries().get(0);
        assertThat(adversaryResponse.getTierOverride()).isEqualTo(3);
        assertThat(adversaryResponse.getRetieredStatistics()).isNotNull();
        assertThat(adversaryResponse.getRetieredStatistics().getDifficulty()).isEqualTo(17);
        assertThat(adversaryResponse.getRetieredStatistics().getMajorThreshold()).isEqualTo(20);
        assertThat(adversaryResponse.getRetieredStatistics().getSevereThreshold()).isEqualTo(32);
        assertThat(adversaryResponse.getRetieredStatistics().getAttackModifier()).isEqualTo(3);
    }

    @Test
    void getEncounterById_InstanceWithNoTierOverride_OmitsRetieredStatistics() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        encounter.setEncounterAdversaries(List.of(instanceOf(encounter, AdversaryType.STANDARD, 0)));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        EncounterResponse response = encounterService.getEncounterById(1L, null, authentication);

        // Assert
        assertThat(response.getAdversaries().get(0).getRetieredStatistics()).isNull();
    }

    // ==================== CREATE ====================

    @Test
    void createEncounter_MinimalRequest_UsesCreatorAndDefaults() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        CreateEncounterRequest request = CreateEncounterRequest.builder().name("Goblin Ambush").build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getName()).isEqualTo("Goblin Ambush");
        assertThat(response.getCreatorId()).isEqualTo(1L);
        assertThat(response.getIsOfficial()).isFalse();
        assertThat(response.getIsPublic()).isFalse();
        assertThat(response.getSpentBattlePoints()).isZero();
    }

    @Test
    void createEncounter_WithPartySizeAndAdjustments_PersistsAndComputesBudget() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Boss Fight")
                .partySize(4)
                .adjustmentHarder(true)
                .adjustmentTwoPlusSolos(true)
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert - +2 harder, -2 two-plus-solos nets back to the base budget of 14
        assertThat(response.getPartySize()).isEqualTo(4);
        assertThat(response.getAdjustmentHarder()).isTrue();
        assertThat(response.getAdjustmentTwoPlusSolos()).isTrue();
        assertThat(response.getSuggestedBattlePoints()).isEqualTo(14);
    }

    @Test
    void createEncounter_WithRichAdversaryEntries_PersistsLabelAndTierOverride() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Adversary adversary = testAdversary(5L, AdversaryType.SOLO, 1);
        when(adversaryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(adversary));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Boss Fight")
                .adversaries(List.of(CreateEncounterRequest.AdversaryEntry.builder()
                        .adversaryId(5L)
                        .label("The Big Bad")
                        .tierOverride(3)
                        .build()))
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getAdversaries()).hasSize(1);
        EncounterResponse.EncounterAdversaryResponse adversaryResponse = response.getAdversaries().get(0);
        assertThat(adversaryResponse.getLabel()).isEqualTo("The Big Bad");
        assertThat(adversaryResponse.getTierOverride()).isEqualTo(3);
        assertThat(adversaryResponse.getDisplayOrder()).isZero();
    }

    @Test
    void createEncounter_WithLegacyAdversaryIds_StillCreatesInstances() {
        // Arrange - backward compatibility: the deprecated bare adversaryIds list still works
        // and repeated IDs still produce multiple instances
        setupAuthenticationWith(regularUserDetails);
        Adversary adversary = testAdversary(5L, AdversaryType.MINION, 1);
        when(adversaryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(adversary));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Goblin Ambush")
                .adversaryIds(List.of(5L, 5L))
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getAdversaries()).hasSize(2);
        assertThat(response.getAdversaries()).allSatisfy(a -> assertThat(a.getLabel()).isNull());
    }

    @Test
    void createEncounter_RichEntriesTakePrecedenceOverLegacyIds() {
        // Arrange - if both fields are somehow provided, the richer list wins
        setupAuthenticationWith(regularUserDetails);
        Adversary adversary = testAdversary(5L, AdversaryType.STANDARD, 1);
        when(adversaryRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(adversary));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Ambush")
                .adversaries(List.of(CreateEncounterRequest.AdversaryEntry.builder().adversaryId(5L).build()))
                .adversaryIds(List.of(5L, 5L, 5L))
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getAdversaries()).hasSize(1);
    }

    @Test
    void createEncounter_WithEnvironmentId_SetsEnvironment() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Environment environment = testEnvironment(9L);
        when(environmentRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(environment));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Cliffside Ambush")
                .environmentId(9L)
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getEnvironmentId()).isEqualTo(9L);
    }

    @Test
    void createEncounter_WithInvalidEnvironmentId_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        when(environmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Cliffside Ambush")
                .environmentId(99L)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> encounterService.createEncounter(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Environment not found");
    }

    @Test
    void createEncounter_WithCampaignId_SetsCampaign() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Campaign campaign = Campaign.builder().id(7L).name("Test Campaign").creator(regularUser).build();
        when(campaignRepository.findActiveById(7L)).thenReturn(Optional.of(campaign));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .name("Session One Fight")
                .campaignId(7L)
                .build();
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.createEncounter(request, authentication);

        // Assert
        assertThat(response.getCampaignId()).isEqualTo(7L);
    }

    // ==================== UPDATE ====================

    @Test
    void updateEncounter_PartialUpdate_OnlyChangesProvidedFields() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().description("Original description").build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEncounterRequest request = UpdateEncounterRequest.builder().partySize(5).build();

        // Act
        EncounterResponse response = encounterService.updateEncounter(1L, request, authentication);

        // Assert - description untouched, party size updated
        assertThat(response.getDescription()).isEqualTo("Original description");
        assertThat(response.getPartySize()).isEqualTo(5);
    }

    @Test
    void updateEncounter_ReplacesAdversariesWithRichEntries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        encounter.setEncounterAdversaries(new ArrayList<>(List.of(instanceOf(encounter, AdversaryType.MINION, 0))));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        Adversary replacement = testAdversary(8L, AdversaryType.LEADER, 2);
        when(adversaryRepository.findByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.of(replacement));

        UpdateEncounterRequest request = UpdateEncounterRequest.builder()
                .adversaries(List.of(CreateEncounterRequest.AdversaryEntry.builder()
                        .adversaryId(8L)
                        .label("Warlord")
                        .build()))
                .build();

        // Act
        EncounterResponse response = encounterService.updateEncounter(1L, request, authentication);

        // Assert - the old Minion instance is gone, replaced by the single Leader
        assertThat(response.getAdversaries()).hasSize(1);
        assertThat(response.getAdversaries().get(0).getLabel()).isEqualTo("Warlord");
    }

    @Test
    void updateEncounter_NonCreatorNonModerator_ThrowsInsufficientPermissions() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().createdBy(otherUser).build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        UpdateEncounterRequest request = UpdateEncounterRequest.builder().name("Hacked").build();

        // Act & Assert
        assertThatThrownBy(() -> encounterService.updateEncounter(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateEncounter_OfficialAsNonOwner_ThrowsInsufficientPermissions() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        Encounter encounter = baseEncounterBuilder().createdBy(ownerUser).isOfficial(true).build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        UpdateEncounterRequest request = UpdateEncounterRequest.builder().name("Hacked").build();

        // Act & Assert
        assertThatThrownBy(() -> encounterService.updateEncounter(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateEncounter_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        when(encounterRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> encounterService.updateEncounter(99L,
                UpdateEncounterRequest.builder().build(), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== DELETE / RESTORE ====================

    @Test
    void deleteEncounter_AsCreator_SoftDeletes() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));

        // Act
        encounterService.deleteEncounter(1L, authentication);

        // Assert
        assertThat(encounter.getDeletedAt()).isNotNull();
        verify(encounterRepository).save(encounter);
    }

    @Test
    void restoreEncounter_AsAdmin_Restores() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);
        Encounter encounter = baseEncounterBuilder().build();
        encounter.softDelete();
        when(encounterRepository.findById(1L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse response = encounterService.restoreEncounter(1L, authentication);

        // Assert
        assertThat(response.getDeletedAt()).isNull();
    }

    @Test
    void restoreEncounter_AsNonAdmin_ThrowsInsufficientPermissions() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.ADMIN)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> encounterService.restoreEncounter(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    // ==================== COPY ====================

    @Test
    void copyEncounter_CopiesAdjustmentsPartySizeAndPerInstanceFields() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Environment environment = testEnvironment(9L);
        Encounter original = baseEncounterBuilder()
                .isPublic(true)
                .partySize(4)
                .adjustmentHarder(true)
                .environment(environment)
                .build();
        EncounterAdversary originalInstance = instanceOf(original, AdversaryType.SOLO, 0);
        originalInstance.setLabel("The Big Bad");
        originalInstance.setTierOverride(2);
        original.setEncounterAdversaries(List.of(originalInstance));

        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(original));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse copy = encounterService.copyEncounter(1L, authentication);

        // Assert - partySize, every adjustment, the environment relation, and each instance's
        // label/tierOverride all carry over; this is the copy bug the design exists to fix
        assertThat(copy.getName()).isEqualTo("Test Encounter (Copy)");
        assertThat(copy.getPartySize()).isEqualTo(4);
        assertThat(copy.getAdjustmentHarder()).isTrue();
        assertThat(copy.getEnvironmentId()).isEqualTo(9L);
        assertThat(copy.getOriginalEncounterId()).isEqualTo(1L);
        assertThat(copy.getAdversaries()).hasSize(1);
        assertThat(copy.getAdversaries().get(0).getLabel()).isEqualTo("The Big Bad");
        assertThat(copy.getAdversaries().get(0).getTierOverride()).isEqualTo(2);
    }

    @Test
    void copyEncounter_DoesNotCarryOverCampaignAssociation() {
        // Arrange - copies are deliberately detached from the original's campaign so a copy
        // starts private to the copying user, even when copying a campaign-scoped encounter
        setupAuthenticationWith(regularUserDetails);
        Campaign campaign = Campaign.builder().id(7L).name("Original Campaign").creator(regularUser).build();
        Encounter original = baseEncounterBuilder().isPublic(true).campaign(campaign).build();
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(original));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EncounterResponse copy = encounterService.copyEncounter(1L, authentication);

        // Assert
        assertThat(copy.getCampaignId()).isNull();
        assertThat(copy.getIsPublic()).isFalse();
    }

    // ==================== ADD / REMOVE ADVERSARY ====================

    @Test
    void addAdversaryToEncounter_AppendsAtNextDisplayOrder() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        encounter.setEncounterAdversaries(new ArrayList<>(List.of(instanceOf(encounter, AdversaryType.MINION, 0))));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        Adversary newAdversary = testAdversary(6L, AdversaryType.RANGED, 1);
        when(adversaryRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(newAdversary));

        // Act
        EncounterResponse response = encounterService.addAdversaryToEncounter(1L, 6L, authentication);

        // Assert
        assertThat(response.getAdversaries()).hasSize(2);
        assertThat(response.getAdversaries().get(1).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void removeAdversaryFromEncounter_RemovesInstance() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        Encounter encounter = baseEncounterBuilder().build();
        EncounterAdversary instance = instanceOf(encounter, AdversaryType.MINION, 0);
        instance.setId(42L);
        encounter.setEncounterAdversaries(new ArrayList<>(List.of(instance)));
        when(encounterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(encounter));
        when(encounterAdversaryRepository.findById(42L)).thenReturn(Optional.of(instance));

        // Act
        EncounterResponse response = encounterService.removeAdversaryFromEncounter(1L, 42L, authentication);

        // Assert
        assertThat(response.getAdversaries()).isEmpty();
        verify(encounterAdversaryRepository).delete(instance);
    }

    // ==================== HELPERS ====================

    private void setupAuthenticationWith(CustomUserDetails userDetails) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    private Encounter.EncounterBuilder<?, ?> baseEncounterBuilder() {
        return Encounter.builder()
                .id(1L)
                .name("Test Encounter")
                .createdBy(regularUser)
                .isOfficial(false)
                .isPublic(false)
                .encounterAdversaries(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now());
    }

    private Adversary testAdversary(Long id, AdversaryType type, int tier) {
        return Adversary.builder()
                .id(id)
                .name("Test Adversary " + id)
                .tier(tier)
                .adversaryType(type)
                .difficulty(10)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(10)
                .hitPointMarked(0)
                .stressMax(5)
                .stressMarked(0)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(regularUser)
                .build();
    }

    private Environment testEnvironment(Long id) {
        return Environment.builder()
                .id(id)
                .name("Test Environment")
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .difficulty(10)
                .isOfficial(true)
                .isPublic(true)
                .createdBy(regularUser)
                .expansion(com.aboff.core.model.entity.dh.Expansion.builder().id(1L).name("Core").build())
                .build();
    }

    private EncounterAdversary instanceOf(Encounter encounter, AdversaryType type, int displayOrder) {
        return EncounterAdversary.builder()
                .encounter(encounter)
                .adversary(testAdversary((long) (100 + displayOrder), type, 1))
                .displayOrder(displayOrder)
                .build();
    }

    private List<EncounterAdversary> minionInstances(Encounter encounter, int count) {
        List<EncounterAdversary> minions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            minions.add(instanceOf(encounter, AdversaryType.MINION, i));
        }
        return minions;
    }
}
