package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCompanionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCompanionRequest;
import com.aboff.core.model.dto.dh.response.CompanionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.Companion;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.CompanionRepository;
import com.aboff.core.security.CustomUserDetails;
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
 * Tests all CRUD operations, access control, pagination, filtering, and expansion.
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
    private Authentication authentication;

    @InjectMocks
    private CompanionService companionService;

    // ==================== GET ALL COMPANIONS TESTS ====================

    @Test
    void getAllCompanions_WithoutFilters_ReturnsPagedCompanions() {
        // Arrange
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion1 = Companion.builder()
                .id(1L)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        Companion companion2 = Companion.builder()
                .id(2L)
                .characterSheet(sheet)
                .name("Hawk")
                .attackName("Talons")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D4)
                .evasion(15)
                .stressMax(2)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Page<Companion> companionPage = new PageImpl<>(List.of(companion1, companion2));
        when(companionRepository.findAll(any(Pageable.class))).thenReturn(companionPage);

        // Act
        PagedResponse<CompanionResponse> result = companionService.getAllCompanions(0, 20, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wolf");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Hawk");

        verify(companionRepository).findAll(any(Pageable.class));
        verifyNoInteractions(characterSheetRepository);
    }

    @Test
    void getAllCompanions_WithCharacterSheetFilter_ReturnsFilteredCompanions() {
        // Arrange
        Long characterSheetId = 1L;
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(characterSheetId)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(1L)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .experiences(new HashSet<>())
                .build();

        Page<Companion> companionPage = new PageImpl<>(List.of(companion));
        when(characterSheetRepository.findActiveById(characterSheetId)).thenReturn(Optional.of(sheet));
        when(companionRepository.findByCharacterSheetId(eq(characterSheetId), any(Pageable.class)))
                .thenReturn(companionPage);

        // Act
        PagedResponse<CompanionResponse> result = companionService.getAllCompanions(0, 20, characterSheetId, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCharacterSheetId()).isEqualTo(characterSheetId);

        verify(characterSheetRepository).findActiveById(characterSheetId);
        verify(companionRepository).findByCharacterSheetId(eq(characterSheetId), any(Pageable.class));
    }

    @Test
    void getAllCompanions_WithInvalidCharacterSheetId_ThrowsException() {
        // Arrange
        Long invalidSheetId = 999L;
        when(characterSheetRepository.findActiveById(invalidSheetId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companionService.getAllCompanions(0, 20, invalidSheetId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found");

        verify(characterSheetRepository).findActiveById(invalidSheetId);
        verifyNoInteractions(companionRepository);
    }

    // ==================== GET COMPANION BY ID TESTS ====================

    @Test
    void getCompanionById_WithValidId_ReturnsCompanion() {
        // Arrange
        Long companionId = 1L;
        User owner = User.builder().id(1L).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .experiences(new HashSet<>())
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));

        // Act
        CompanionResponse result = companionService.getCompanionById(companionId, null);

        // Assert
        assertThat(result.getId()).isEqualTo(companionId);
        assertThat(result.getName()).isEqualTo("Wolf");
        assertThat(result.getAttackName()).isEqualTo("Bite");

        verify(companionRepository).findById(companionId);
    }

    @Test
    void getCompanionById_WithInvalidId_ThrowsException() {
        // Arrange
        Long invalidId = 999L;
        when(companionRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companionService.getCompanionById(invalidId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Companion not found");

        verify(companionRepository).findById(invalidId);
    }

    // ==================== CREATE COMPANION TESTS ====================

    @Test
    void createCompanion_AsOwner_CreatesSuccessfully() {
        // Arrange
        Long userId = 1L;
        Long characterSheetId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        User owner = User.builder().id(userId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(characterSheetId)
                .name("Aragorn")
                .owner(owner)
                .build();

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(characterSheetId)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .build();

        Companion savedCompanion = Companion.builder()
                .id(1L)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(characterSheetId)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenReturn(savedCompanion);

        // Act
        CompanionResponse result = companionService.createCompanion(request, authentication);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wolf");
        assertThat(result.getAttackName()).isEqualTo("Bite");

        verify(characterSheetRepository).findActiveById(characterSheetId);
        verify(companionRepository).save(any(Companion.class));
    }

    @Test
    void createCompanion_AsModerator_CreatesSuccessfully() {
        // Arrange
        Long userId = 2L;
        Long ownerId = 1L;
        Long characterSheetId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(true);

        User owner = User.builder().id(ownerId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(characterSheetId)
                .name("Aragorn")
                .owner(owner)
                .build();

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(characterSheetId)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        Companion savedCompanion = Companion.builder()
                .id(1L)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .experiences(new HashSet<>())
                .build();

        when(characterSheetRepository.findActiveById(characterSheetId)).thenReturn(Optional.of(sheet));
        when(companionRepository.save(any(Companion.class))).thenReturn(savedCompanion);

        // Act
        CompanionResponse result = companionService.createCompanion(request, authentication);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wolf");

        verify(characterSheetRepository).findActiveById(characterSheetId);
        verify(roleHierarchyService).hasModeratorOrHigher(userDetails);
        verify(companionRepository).save(any(Companion.class));
    }

    @Test
    void createCompanion_WithoutPermission_ThrowsException() {
        // Arrange
        Long userId = 2L;
        Long ownerId = 1L;
        Long characterSheetId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(false);

        User owner = User.builder().id(ownerId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(characterSheetId)
                .name("Aragorn")
                .owner(owner)
                .build();

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(characterSheetId)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(characterSheetRepository.findActiveById(characterSheetId)).thenReturn(Optional.of(sheet));

        // Act & Assert
        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("permission to create");

        verify(characterSheetRepository).findActiveById(characterSheetId);
        verify(roleHierarchyService).hasModeratorOrHigher(userDetails);
        verifyNoInteractions(companionRepository);
    }

    @Test
    void createCompanion_WithInvalidCharacterSheet_ThrowsException() {
        // Arrange
        Long userId = 1L;
        Long invalidSheetId = 999L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        CreateCompanionRequest request = CreateCompanionRequest.builder()
                .characterSheetId(invalidSheetId)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(characterSheetRepository.findActiveById(invalidSheetId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companionService.createCompanion(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CharacterSheet not found");

        verify(characterSheetRepository).findActiveById(invalidSheetId);
        verifyNoInteractions(companionRepository);
    }

    // ==================== UPDATE COMPANION TESTS ====================

    @Test
    void updateCompanion_AsOwner_UpdatesSuccessfully() {
        // Arrange
        Long userId = 1L;
        Long companionId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        User owner = User.builder().id(userId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .build();

        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenReturn(companion);

        // Act
        CompanionResponse result = companionService.updateCompanion(companionId, request, authentication);

        // Assert
        assertThat(result.getId()).isEqualTo(companionId);
        verify(companionRepository).findById(companionId);
        verify(companionRepository).save(companion);
    }

    @Test
    void updateCompanion_PartialUpdate_OnlyUpdatesProvidedFields() {
        // Arrange
        Long userId = 1L;
        Long companionId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        User owner = User.builder().id(userId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .description("A loyal wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .evasion(12)
                .stressMax(3)
                .stressMarked(0)
                .experiences(new HashSet<>())
                .build();

        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .name("Shadow Wolf")
                .stressMarked(1)
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));
        when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CompanionResponse result = companionService.updateCompanion(companionId, request, authentication);

        // Assert
        assertThat(companion.getName()).isEqualTo("Shadow Wolf");
        assertThat(companion.getStressMarked()).isEqualTo(1);
        assertThat(companion.getDescription()).isEqualTo("A loyal wolf"); // Unchanged
        assertThat(companion.getAttackName()).isEqualTo("Bite"); // Unchanged

        verify(companionRepository).save(companion);
    }

    @Test
    void updateCompanion_WithoutPermission_ThrowsException() {
        // Arrange
        Long userId = 2L;
        Long ownerId = 1L;
        Long companionId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(false);

        User owner = User.builder().id(ownerId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        UpdateCompanionRequest request = UpdateCompanionRequest.builder()
                .stressMarked(2)
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));

        // Act & Assert
        assertThatThrownBy(() -> companionService.updateCompanion(companionId, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("permission to update");

        verify(companionRepository).findById(companionId);
        verify(companionRepository, never()).save(any(Companion.class));
    }

    // ==================== DELETE COMPANION TESTS ====================

    @Test
    void deleteCompanion_AsOwner_DeletesSuccessfully() {
        // Arrange
        Long userId = 1L;
        Long companionId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        User owner = User.builder().id(userId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));

        // Act
        companionService.deleteCompanion(companionId, authentication);

        // Assert
        verify(companionRepository).findById(companionId);
        verify(companionRepository).delete(companion);
    }

    @Test
    void deleteCompanion_WithoutPermission_ThrowsException() {
        // Arrange
        Long userId = 2L;
        Long ownerId = 1L;
        Long companionId = 1L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(roleHierarchyService.hasModeratorOrHigher(userDetails)).thenReturn(false);

        User owner = User.builder().id(ownerId).username("player1").build();
        CharacterSheet sheet = CharacterSheet.builder()
                .id(1L)
                .name("Aragorn")
                .owner(owner)
                .build();

        Companion companion = Companion.builder()
                .id(companionId)
                .characterSheet(sheet)
                .name("Wolf")
                .attackName("Bite")
                .attackRange(Range.CLOSE)
                .damageDice(DiceType.D6)
                .build();

        when(companionRepository.findById(companionId)).thenReturn(Optional.of(companion));

        // Act & Assert
        assertThatThrownBy(() -> companionService.deleteCompanion(companionId, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("permission to delete");

        verify(companionRepository).findById(companionId);
        verify(companionRepository, never()).delete(any(Companion.class));
    }

    @Test
    void deleteCompanion_WithInvalidId_ThrowsException() {
        // Arrange
        Long invalidId = 999L;

        when(companionRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companionService.deleteCompanion(invalidId, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Companion not found");

        verify(companionRepository).findById(invalidId);
        verify(companionRepository, never()).delete(any(Companion.class));
    }
}
