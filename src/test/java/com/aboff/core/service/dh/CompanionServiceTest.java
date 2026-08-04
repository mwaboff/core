package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.CreateCompanionTrainingRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.enums.CompanionTrainingOption;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.ViciousAxis;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.CompanionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompanionService.
 * <p>
 * Covers CRUD, Training add/remove, soft delete, cross-field validation, and -- as the
 * priority deliverable -- that every operation is scoped to a specific character sheet and
 * access-checked owner-or-MODERATOR+, closing the previous unauthenticated/unfiltered leak.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CompanionServiceTest {

    @Mock
    private CompanionRepository companionRepository;

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CompanionService companionService;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long SHEET_ID = 10L;
    private static final Long COMPANION_ID = 100L;

    private User owner() {
        return User.builder().id(OWNER_ID).username("player1").build();
    }

    private CharacterSheet sheet(User owner) {
        return CharacterSheet.builder()
                .id(SHEET_ID)
                .name("Aragorn")
                .owner(owner)
                .level(3)
                .proficiency(2)
                .build();
    }

    private Companion companion(CharacterSheet sheet) {
        return Companion.builder()
                .id(COMPANION_ID)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .baseAttackRange(Range.CLOSE)
                .baseDamageDice(DiceType.D6)
                .baseEvasion(10)
                .baseStressMax(3)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .trainings(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CustomUserDetails asUser(Long userId, boolean moderatorOrHigher) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        lenientModerator(userDetails, moderatorOrHigher);
        return userDetails;
    }

    private void lenientModerator(CustomUserDetails userDetails, boolean moderatorOrHigher) {
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(moderatorOrHigher);
    }

    // ==================== GET ALL COMPANIONS (SECURITY FIX) ====================

    @Test
    void getAllCompanions_WithoutCharacterSheetId_ThrowsException() {
        assertThatThrownBy(() -> companionService.getAllCompanions(0, 20, null, null, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("characterSheetId is required");

        verifyNoInteractions(characterSheetRepository, companionRepository);
    }

    @Test
    void getAllCompanions_AsOwner_ReturnsCompanions() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(eq(SHEET_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(companion)));

        PagedResponse<CompanionResponse> result =
                companionService.getAllCompanions(0, 20, SHEET_ID, null, authentication);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wolf");
        verify(companionRepository).findActiveByCharacterSheetId(eq(SHEET_ID), any(Pageable.class));
    }

    @Test
    void getAllCompanions_AsModerator_ReturnsCompanions() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, true);

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.findActiveByCharacterSheetId(eq(SHEET_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<CompanionResponse> result =
                companionService.getAllCompanions(0, 20, SHEET_ID, null, authentication);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getAllCompanions_AsOtherUser_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> companionService.getAllCompanions(0, 20, SHEET_ID, null, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verifyNoInteractions(companionRepository);
    }

    @Test
    void getAllCompanions_WithInvalidCharacterSheetId_ThrowsException() {
        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companionService.getAllCompanions(0, 20, SHEET_ID, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found");

        verifyNoInteractions(companionRepository);
    }

    // ==================== GET COMPANION BY ID (SECURITY FIX) ====================

    @Test
    void getCompanionById_AsOwner_ReturnsCompanion() {
        CharacterSheet sheet = sheet(owner());
        asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        CompanionResponse result = companionService.getCompanionById(COMPANION_ID, null, authentication);

        assertThat(result.getId()).isEqualTo(COMPANION_ID);
        assertThat(result.getName()).isEqualTo("Wolf");
    }

    @Test
    void getCompanionById_AsModerator_ReturnsCompanion() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, true);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        CompanionResponse result = companionService.getCompanionById(COMPANION_ID, null, authentication);

        assertThat(result.getId()).isEqualTo(COMPANION_ID);
    }

    @Test
    void getCompanionById_AsOtherUser_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.getCompanionById(COMPANION_ID, null, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("permission to view");
    }

    @Test
    void getCompanionById_WithInvalidId_ThrowsException() {
        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companionService.getCompanionById(COMPANION_ID, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Companion not found");
    }

    @Test
    void getCompanionById_SoftDeleted_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        Companion companion = companion(sheet);
        companion.softDelete();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.getCompanionById(COMPANION_ID, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Companion not found");
    }

    // ==================== CREATE COMPANION ====================

    @Test
    void createCompanion_AsOwner_CreatesSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.createCompanion(request, authentication);

        assertThat(result.getName()).isEqualTo("Wolf");
        assertThat(result.getBaseEvasion()).isEqualTo(12);
        verify(companionRepository).save(any(Companion.class));
    }

    @Test
    void createCompanion_AsModerator_CreatesSuccessfully() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OTHER_USER_ID, true);
        when(userDetails.getUser()).thenReturn(User.builder().id(OTHER_USER_ID).username("mod").build());

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.createCompanion(request, authentication);

        assertThat(result.getName()).isEqualTo("Wolf");
    }

    @Test
    void createCompanion_WithoutPermission_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verifyNoInteractions(companionRepository);
    }

    @Test
    void createCompanion_WithInvalidCharacterSheet_ThrowsException() {
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(companionRepository);
    }

    @Test
    void createCompanion_WithStressMarkedExceedingMax_ThrowsException() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .stressMax(3)
                .stressMarked(5)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stressMarked");

        verifyNoInteractions(companionRepository);
    }

    @Test
    void createCompanion_WithExplicitPhysicalDamageType_PersistsPhysical() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .damageType(DamageType.PHYSICAL)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.createCompanion(request, authentication);

        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void createCompanion_WithExplicitMagicDamageType_PersistsMagic() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .damageType(DamageType.MAGIC)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.createCompanion(request, authentication);

        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void createCompanion_WithoutDamageType_DefaultsToPhysical() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);

        // Simulate JSON deserialization without a damageType field, which bypasses the
        // builder default: set it explicitly to null rather than relying on the builder.
        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();
        request.setDamageType(null);

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.createCompanion(request, authentication);

        assertThat(result.getDamageType()).isEqualTo(DamageType.PHYSICAL);
    }

    @Test
    void createCompanion_WithPhysicalAndMagicDamageType_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OWNER_ID, false);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(SHEET_ID)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .damageType(DamageType.PHYSICAL_AND_MAGIC)
                .build();

        when(characterSheetRepository.findActiveById(SHEET_ID)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PHYSICAL_AND_MAGIC");

        verifyNoInteractions(companionRepository);
    }

    // ==================== UPDATE COMPANION ====================

    @Test
    void updateCompanion_AsOwner_UpdatesSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(2).build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenReturn(companion);

        CompanionResponse result = companionService.updateCompanion(COMPANION_ID, request, authentication);

        assertThat(result.getStressMarked()).isEqualTo(2);
        verify(companionRepository).save(companion);
    }

    @Test
    void updateCompanion_PartialUpdate_OnlyUpdatesProvidedFields() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner());
        Companion companion = companion(sheet);
        companion.setDescription("A loyal wolf");

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().name("Shadow Wolf").build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        companionService.updateCompanion(COMPANION_ID, request, authentication);

        assertThat(companion.getName()).isEqualTo("Shadow Wolf");
        assertThat(companion.getDescription()).isEqualTo("A loyal wolf");
        assertThat(companion.getAttackName()).isEqualTo("Bite");
    }

    @Test
    void updateCompanion_WithoutPermission_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);
        Companion companion = companion(sheet);

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(2).build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.updateCompanion(COMPANION_ID, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("permission to update");

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void updateCompanion_SoftDeleted_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        Companion companion = companion(sheet);
        companion.softDelete();

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(2).build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.updateCompanion(COMPANION_ID, request, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateCompanion_WithStressMarkedExceedingDerivedMax_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().stressMarked(10).build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.updateCompanion(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stressMarked");

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void updateCompanion_WithDamageType_UpdatesDamageType() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        companion.setDamageType(DamageType.PHYSICAL);

        UpdateCompanionRequest request = UpdateCompanionRequest.builder().damageType(DamageType.MAGIC).build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.updateCompanion(COMPANION_ID, request, authentication);

        assertThat(result.getDamageType()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void updateCompanion_WithPhysicalAndMagicDamageType_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .damageType(DamageType.PHYSICAL_AND_MAGIC)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.updateCompanion(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PHYSICAL_AND_MAGIC");

        verify(companionRepository, never()).save(any(Companion.class));
    }

    // ==================== DELETE COMPANION (SOFT DELETE) ====================

    @Test
    void deleteCompanion_AsOwner_SoftDeletesSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenReturn(companion);

        companionService.deleteCompanion(COMPANION_ID, authentication);

        assertThat(companion.isDeleted()).isTrue();
        verify(companionRepository).save(companion);
        verify(companionRepository, never()).delete(any(Companion.class));
    }

    @Test
    void deleteCompanion_WithoutPermission_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.deleteCompanion(COMPANION_ID, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        assertThat(companion.isDeleted()).isFalse();
        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void deleteCompanion_WithInvalidId_ThrowsException() {
        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companionService.deleteCompanion(COMPANION_ID, authentication))
                .isInstanceOf(EntityNotFoundException.class);

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void deleteCompanion_AlreadySoftDeleted_ThrowsException() {
        Companion companion = companion(sheet(owner()));
        companion.softDelete();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.deleteCompanion(COMPANION_ID, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteCompanion_ClampsSheetHopeMarkedWhenGrantedSlotLost() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        sheet.setHopeMax(3);
        sheet.setHopeMarked(4); // only legal because this companion's LIGHT_IN_THE_DARK grants +1
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        companion.getTrainings().add(CompanionTraining.builder()
                .id(400L).companion(companion).option(CompanionTrainingOption.LIGHT_IN_THE_DARK).acquiredAtLevel(1).build());

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenReturn(companion);
        // The deleted companion is gone, so no active companion still grants the bonus slot.
        when(companionRepository.findActiveByCharacterSheetId(SHEET_ID)).thenReturn(List.of());

        companionService.deleteCompanion(COMPANION_ID, authentication);

        assertThat(sheet.getHopeMarked()).isEqualTo(3);
        verify(characterSheetRepository).save(sheet);
    }

    // ==================== ADD TRAINING ====================

    @Test
    void addTraining_Aware_AddsSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.addTraining(COMPANION_ID, request, authentication);

        assertThat(companion.getTrainings()).hasSize(1);
        assertThat(result.getEvasion()).isEqualTo(12); // base 10 + 2 for Aware
        assertThat(result.getTrainings()).hasSize(1);
        CompanionTraining added = companion.getTrainings().iterator().next();
        assertThat(added.getAcquiredAtLevel()).isEqualTo(sheet.getLevel());
    }

    @Test
    void addTraining_ExceedingCap_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);
        for (int i = 0; i < CompanionTrainingOption.LIGHT_IN_THE_DARK.getMaxSelections(); i++) {
            companion.getTrainings().add(CompanionTraining.builder()
                    .id((long) (200 + i))
                    .companion(companion)
                    .option(CompanionTrainingOption.LIGHT_IN_THE_DARK)
                    .acquiredAtLevel(1)
                    .build());
        }

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.LIGHT_IN_THE_DARK)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remaining");

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void addTraining_ViciousWithoutAxis_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.VICIOUS)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("viciousAxis");
    }

    @Test
    void addTraining_ViciousAxisAtCap_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);
        companion.setBaseDamageDice(DiceType.D12);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.VICIOUS)
                .viciousAxis(ViciousAxis.DAMAGE_DIE)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void addTraining_IntelligentWithoutTargetExperience_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.INTELLIGENT)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targetExperienceId");
    }

    @Test
    void addTraining_IntelligentWithForeignExperience_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.INTELLIGENT)
                .targetExperienceId(999L)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void addTraining_IntelligentWithValidExperience_AddsSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        Experience experience = Experience.builder().id(55L).companion(companion).createdBy(owner).description("Tracking").modifier(2).build();
        companion.getExperiences().add(experience);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.INTELLIGENT)
                .targetExperienceId(55L)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        companionService.addTraining(COMPANION_ID, request, authentication);

        CompanionTraining added = companion.getTrainings().iterator().next();
        assertThat(added.getTargetExperience().getId()).isEqualTo(55L);
    }

    @Test
    void addTraining_WithoutPermission_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);
        Companion companion = companion(sheet);

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void addTraining_SoftDeletedCompanion_ThrowsException() {
        Companion companion = companion(sheet(owner()));
        companion.softDelete();

        CreateCompanionTrainingRequest request = CreateCompanionTrainingRequest.builder()
                .option(CompanionTrainingOption.AWARE)
                .build();

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.addTraining(COMPANION_ID, request, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== REMOVE TRAINING ====================

    @Test
    void removeTraining_AsOwner_RemovesSuccessfully() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        CompanionTraining training = CompanionTraining.builder()
                .id(300L).companion(companion).option(CompanionTrainingOption.AWARE).acquiredAtLevel(1).build();
        companion.getTrainings().add(training);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanionResponse result = companionService.removeTraining(COMPANION_ID, 300L, authentication);

        assertThat(companion.getTrainings()).isEmpty();
        assertThat(result.getTrainings()).isEmpty();
        verify(companionRepository, never()).delete(any(Companion.class));
    }

    @Test
    void removeTraining_ClampsStressMarkedAfterResilientRemoved() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        companion.setStressMarked(4); // legal only with the Resilient bonus below (base 3 + 1 = 4)
        CompanionTraining training = CompanionTraining.builder()
                .id(300L).companion(companion).option(CompanionTrainingOption.RESILIENT).acquiredAtLevel(1).build();
        companion.getTrainings().add(training);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));

        companionService.removeTraining(COMPANION_ID, 300L, authentication);

        assertThat(companion.getStressMarked()).isEqualTo(3); // clamped to the new derived max
    }

    @Test
    void removeTraining_ClampsSheetHopeMarkedWhenGrantedSlotLost() {
        User owner = owner();
        CharacterSheet sheet = sheet(owner);
        sheet.setHopeMax(3);
        sheet.setHopeMarked(4); // only legal because this companion's LIGHT_IN_THE_DARK grants +1
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        when(userDetails.getUser()).thenReturn(owner);
        Companion companion = companion(sheet);
        CompanionTraining training = CompanionTraining.builder()
                .id(300L).companion(companion).option(CompanionTrainingOption.LIGHT_IN_THE_DARK).acquiredAtLevel(1).build();
        companion.getTrainings().add(training);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companionRepository.findActiveByCharacterSheetId(SHEET_ID)).thenReturn(List.of(companion));

        companionService.removeTraining(COMPANION_ID, 300L, authentication);

        assertThat(sheet.getHopeMarked()).isEqualTo(3);
        verify(characterSheetRepository).save(sheet);
    }

    @Test
    void removeTraining_NotFound_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        CustomUserDetails userDetails = asUser(OWNER_ID, false);
        Companion companion = companion(sheet);

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.removeTraining(COMPANION_ID, 999L, authentication))
                .isInstanceOf(EntityNotFoundException.class);

        verify(companionRepository, never()).save(any(Companion.class));
    }

    @Test
    void removeTraining_WithoutPermission_ThrowsException() {
        CharacterSheet sheet = sheet(owner());
        asUser(OTHER_USER_ID, false);
        Companion companion = companion(sheet);
        companion.getTrainings().add(CompanionTraining.builder()
                .id(300L).companion(companion).option(CompanionTrainingOption.AWARE).acquiredAtLevel(1).build());

        when(companionRepository.findById(COMPANION_ID)).thenReturn(Optional.of(companion));

        assertThatThrownBy(() -> companionService.removeTraining(COMPANION_ID, 300L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        assertThat(companion.getTrainings()).hasSize(1);
    }
}
