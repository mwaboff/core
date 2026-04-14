package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureModifierRequest;
import com.aboff.core.model.dto.dh.request.FeatureModifierInput;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import com.aboff.core.repository.dh.FeatureModifierRepository;
import com.aboff.core.service.AuditLogger;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeatureModifierService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality,
 * find-or-create semantics, and modifier resolution.
 */
@ExtendWith(MockitoExtension.class)
class FeatureModifierServiceTest {

    @Mock
    private FeatureModifierRepository featureModifierRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FeatureModifierService featureModifierService;

    // ==================== GET ALL MODIFIERS TESTS ====================

    @Test
    void getAllModifiers_WithoutIncludeDeleted_ReturnsActiveModifiers() {
        // Arrange
        FeatureModifier modifier1 = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .createdAt(LocalDateTime.now())
                .build();

        FeatureModifier modifier2 = FeatureModifier.builder()
                .id(2L)
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .createdAt(LocalDateTime.now())
                .build();

        Page<FeatureModifier> modifierPage = new PageImpl<>(List.of(modifier1, modifier2));
        when(featureModifierRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(modifierPage);

        // Act
        PagedResponse<FeatureModifierResponse> result = featureModifierService.getAllModifiers(0, 20, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getContent().get(0).getTarget()).isEqualTo(ModifierTarget.STRENGTH);
        assertThat(result.getContent().get(1).getTarget()).isEqualTo(ModifierTarget.EVASION);
    }

    @Test
    void getAllModifiers_WithIncludeDeleted_ReturnsAllModifiers() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<FeatureModifier> modifierPage = new PageImpl<>(List.of(modifier));
        when(featureModifierRepository.findAll(any(Pageable.class)))
                .thenReturn(modifierPage);

        // Act
        PagedResponse<FeatureModifierResponse> result = featureModifierService.getAllModifiers(0, 20, true);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(featureModifierRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllModifiers_WithLargePage_LimitsTo100() {
        // Arrange
        Page<FeatureModifier> modifierPage = new PageImpl<>(List.of());
        when(featureModifierRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(modifierPage);

        // Act
        featureModifierService.getAllModifiers(0, 500, false);

        // Assert
        verify(featureModifierRepository).findAllByDeletedAtIsNull(
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    // ==================== GET MODIFIER BY ID TESTS ====================

    @Test
    void getModifier_ValidId_ReturnsModifier() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(2)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureModifierRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(modifier));

        // Act
        FeatureModifierResponse result = featureModifierService.getModifier(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.STRENGTH);
        assertThat(result.getOperation()).isEqualTo(ModifierOperation.ADD);
        assertThat(result.getValue()).isEqualTo(2);
    }

    @Test
    void getModifier_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureModifierRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureModifierService.getModifier(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FeatureModifier not found with id: 999");
    }

    // ==================== CREATE MODIFIER TESTS ====================

    @Test
    void createModifier_ValidRequest_CreatesAndReturnsModifier() {
        // Arrange
        CreateFeatureModifierRequest request = CreateFeatureModifierRequest.builder()
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .build();

        FeatureModifier savedModifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureModifierRepository.save(any(FeatureModifier.class)))
                .thenReturn(savedModifier);

        // Act
        FeatureModifierResponse result = featureModifierService.createModifier(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.EVASION);
        assertThat(result.getOperation()).isEqualTo(ModifierOperation.ADD);
        assertThat(result.getValue()).isEqualTo(-1);

        verify(featureModifierRepository).save(argThat(modifier ->
                modifier.getTarget().equals(ModifierTarget.EVASION) &&
                        modifier.getOperation().equals(ModifierOperation.ADD) &&
                        modifier.getValue().equals(-1)
        ));
    }

    // ==================== DELETE MODIFIER TESTS ====================

    @Test
    void deleteModifier_ValidId_SoftDeletesModifier() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureModifierRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(modifier));

        // Act
        featureModifierService.deleteModifier(1L, authentication);

        // Assert
        verify(featureModifierRepository).save(argThat(m -> m.getDeletedAt() != null));
    }

    @Test
    void deleteModifier_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureModifierRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureModifierService.deleteModifier(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FeatureModifier not found with id: 999");

        verify(featureModifierRepository, never()).save(any());
    }

    // ==================== RESTORE MODIFIER TESTS ====================

    @Test
    void restoreModifier_DeletedModifier_RestoresSuccessfully() {
        // Arrange
        FeatureModifier deletedModifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(featureModifierRepository.findById(1L))
                .thenReturn(Optional.of(deletedModifier));
        when(featureModifierRepository.save(any(FeatureModifier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureModifierResponse result = featureModifierService.restoreModifier(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(featureModifierRepository).save(argThat(m -> m.getDeletedAt() == null));
    }

    @Test
    void restoreModifier_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        FeatureModifier activeModifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureModifierRepository.findById(1L))
                .thenReturn(Optional.of(activeModifier));

        // Act & Assert
        assertThatThrownBy(() -> featureModifierService.restoreModifier(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FeatureModifier with id 1 is not deleted");

        verify(featureModifierRepository, never()).save(any());
    }

    @Test
    void restoreModifier_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureModifierRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureModifierService.restoreModifier(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FeatureModifier not found with id: 999");
    }

    // ==================== FIND OR CREATE TESTS ====================

    @Test
    void findOrCreate_ExistingModifier_ReturnsExisting() {
        // Arrange
        FeatureModifier existingModifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .build();

        FeatureModifierInput input = FeatureModifierInput.builder()
                .target(ModifierTarget.STRENGTH)
                .operation(ModifierOperation.ADD)
                .value(1)
                .build();

        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.STRENGTH, ModifierOperation.ADD, 1))
                .thenReturn(Optional.of(existingModifier));

        // Act
        FeatureModifier result = featureModifierService.findOrCreate(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.STRENGTH);
        verify(featureModifierRepository, never()).save(any());
    }

    @Test
    void findOrCreate_NoMatch_CreatesNewModifier() {
        // Arrange
        FeatureModifierInput input = FeatureModifierInput.builder()
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .build();

        FeatureModifier savedModifier = FeatureModifier.builder()
                .id(2L)
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .build();

        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.EVASION, ModifierOperation.ADD, -1))
                .thenReturn(Optional.empty());
        when(featureModifierRepository.save(any(FeatureModifier.class)))
                .thenReturn(savedModifier);

        // Act
        FeatureModifier result = featureModifierService.findOrCreate(input);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.EVASION);
        assertThat(result.getOperation()).isEqualTo(ModifierOperation.ADD);
        assertThat(result.getValue()).isEqualTo(-1);
        verify(featureModifierRepository).save(argThat(m ->
                m.getTarget().equals(ModifierTarget.EVASION) &&
                        m.getOperation().equals(ModifierOperation.ADD) &&
                        m.getValue().equals(-1)
        ));
    }

    // ==================== RESOLVE MODIFIERS TESTS ====================

    @Test
    void resolveModifiers_OnlyModifierIds_ReturnsModifiersById() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        when(featureModifierRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
                .thenReturn(List.of(modifier));

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(List.of(1L), null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(modifier);
    }

    @Test
    void resolveModifiers_OnlyModifierInputs_FindsOrCreatesEach() {
        // Arrange
        FeatureModifier existingModifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.STRENGTH, ModifierOperation.ADD, 1))
                .thenReturn(Optional.of(existingModifier));

        List<FeatureModifierInput> inputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.STRENGTH)
                        .operation(ModifierOperation.ADD)
                        .value(1)
                        .build()
        );

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(null, inputs);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(existingModifier);
    }

    @Test
    void resolveModifiers_BothProvided_MergesResults() {
        // Arrange
        FeatureModifier modifierById = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        FeatureModifier modifierByInput = FeatureModifier.builder()
                .id(2L).target(ModifierTarget.EVASION).operation(ModifierOperation.ADD).value(-1).build();

        when(featureModifierRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
                .thenReturn(List.of(modifierById));
        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.EVASION, ModifierOperation.ADD, -1))
                .thenReturn(Optional.of(modifierByInput));

        List<FeatureModifierInput> inputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.EVASION)
                        .operation(ModifierOperation.ADD)
                        .value(-1)
                        .build()
        );

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(List.of(1L), inputs);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(modifierById, modifierByInput);
    }

    @Test
    void resolveModifiers_BothNull_ReturnsNull() {
        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(null, null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void resolveModifiers_BothEmpty_ReturnsEmptySet() {
        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(List.of(), List.of());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void resolveModifiers_DuplicateBetweenIdsAndInputs_DeduplicatedInSet() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        when(featureModifierRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
                .thenReturn(List.of(modifier));
        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.STRENGTH, ModifierOperation.ADD, 1))
                .thenReturn(Optional.of(modifier));

        List<FeatureModifierInput> inputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.STRENGTH)
                        .operation(ModifierOperation.ADD)
                        .value(1)
                        .build()
        );

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(List.of(1L), inputs);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(modifier);
    }

    @Test
    void resolveModifiers_IdsNullInputsNonNull_ReturnsResolvedModifiers() {
        // Arrange
        FeatureModifier newModifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.HIT_POINT_MAX).operation(ModifierOperation.ADD).value(5).build();
        when(featureModifierRepository.findByTargetAndOperationAndValueAndDeletedAtIsNull(
                ModifierTarget.HIT_POINT_MAX, ModifierOperation.ADD, 5))
                .thenReturn(Optional.empty());
        when(featureModifierRepository.save(any(FeatureModifier.class))).thenReturn(newModifier);

        List<FeatureModifierInput> inputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.HIT_POINT_MAX)
                        .operation(ModifierOperation.ADD)
                        .value(5)
                        .build()
        );

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(null, inputs);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(newModifier);
    }

    @Test
    void resolveModifiers_IdsNonNullInputsNull_ReturnsModifiersById() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        when(featureModifierRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
                .thenReturn(List.of(modifier));

        // Act
        Set<FeatureModifier> result = featureModifierService.resolveModifiers(List.of(1L), null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(modifier);
    }

    // ==================== TO RESPONSE TESTS ====================

    @Test
    void toResponse_MapsAllFields() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 28, 10, 0);
        LocalDateTime lastModifiedAt = LocalDateTime.of(2026, 2, 28, 12, 0);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 2, 28, 14, 0);

        FeatureModifier modifier = FeatureModifier.builder()
                .id(42L)
                .target(ModifierTarget.MAJOR_DAMAGE_THRESHOLD)
                .operation(ModifierOperation.SET)
                .value(15)
                .createdAt(createdAt)
                .lastModifiedAt(lastModifiedAt)
                .deletedAt(deletedAt)
                .build();

        // Act
        FeatureModifierResponse result = featureModifierService.toResponse(modifier);

        // Assert
        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.MAJOR_DAMAGE_THRESHOLD);
        assertThat(result.getOperation()).isEqualTo(ModifierOperation.SET);
        assertThat(result.getValue()).isEqualTo(15);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getLastModifiedAt()).isEqualTo(lastModifiedAt);
        assertThat(result.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void toResponse_NullTimestamps_MapsNullFields() {
        // Arrange
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L)
                .target(ModifierTarget.AGILITY)
                .operation(ModifierOperation.MULTIPLY)
                .value(2)
                .build();

        // Act
        FeatureModifierResponse result = featureModifierService.toResponse(modifier);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTarget()).isEqualTo(ModifierTarget.AGILITY);
        assertThat(result.getOperation()).isEqualTo(ModifierOperation.MULTIPLY);
        assertThat(result.getValue()).isEqualTo(2);
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getLastModifiedAt()).isNull();
        assertThat(result.getDeletedAt()).isNull();
    }
}
