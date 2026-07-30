package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateMartialStanceRequest;
import com.aboff.core.model.dto.dh.request.UpdateMartialStanceRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.MartialStanceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.MartialStance;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.MartialStanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import com.aboff.core.service.AuditLogger;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MartialStanceService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class MartialStanceServiceTest {

    @Mock
    private MartialStanceRepository martialStanceRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private MartialStanceService martialStanceService;

    // ==================== GET ALL MARTIAL STANCES TESTS ====================

    @Test
    void getAllMartialStances_WithoutFilters_ReturnsPagedMartialStances() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance stance1 = createTestMartialStance(1L, "Favored", expansion, 1);
        MartialStance stance2 = createTestMartialStance(2L, "Aggressive", expansion, 2);

        Page<MartialStance> stancePage = new PageImpl<>(List.of(stance1, stance2));
        when(martialStanceRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stancePage);

        // Act
        PagedResponse<MartialStanceResponse> result = martialStanceService.getAllMartialStances(0, 20, false, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Favored");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Aggressive");
    }

    @Test
    void getAllMartialStances_WithExpansionFilter_ReturnsFilteredMartialStances() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance stance = createTestMartialStance(1L, "Favored", expansion, 1);

        Page<MartialStance> stancePage = new PageImpl<>(List.of(stance));
        when(martialStanceRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stancePage);

        // Act
        PagedResponse<MartialStanceResponse> result = martialStanceService.getAllMartialStances(0, 20, false, 1L, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(martialStanceRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllMartialStances_WithTierFilter_ReturnsFilteredMartialStances() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance stance = createTestMartialStance(1L, "Aggressive", expansion, 2);

        Page<MartialStance> stancePage = new PageImpl<>(List.of(stance));
        when(martialStanceRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(2), any(Pageable.class)))
                .thenReturn(stancePage);

        // Act
        PagedResponse<MartialStanceResponse> result = martialStanceService.getAllMartialStances(0, 20, false, null, null, 2, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTier()).isEqualTo(2);
    }

    @Test
    void getAllMartialStances_WithLargePage_LimitsTo100() {
        // Arrange
        Page<MartialStance> stancePage = new PageImpl<>(List.of());
        when(martialStanceRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stancePage);

        // Act
        martialStanceService.getAllMartialStances(0, 500, false, null, null, null, null);

        // Assert
        verify(martialStanceRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllMartialStances_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).createdAt(LocalDateTime.now()).build();

        MartialStance stance = createTestMartialStance(1L, "Favored", expansion, 1);

        Page<MartialStance> stancePage = new PageImpl<>(List.of(stance));
        when(martialStanceRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stancePage);

        // Act
        PagedResponse<MartialStanceResponse> result = martialStanceService.getAllMartialStances(0, 20, false, null, null, null, "expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Hope and Fear");
    }

    // ==================== GET MARTIAL STANCE BY ID TESTS ====================

    @Test
    void getMartialStanceById_ValidId_ReturnsMartialStance() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance stance = createTestMartialStance(1L, "Favored", expansion, 1);

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(stance));

        // Act
        MartialStanceResponse result = martialStanceService.getMartialStanceById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Favored");
        assertThat(result.getDescription()).isEqualTo("Gain a bonus to damage rolls equal to a trait of your choice.");
    }

    @Test
    void getMartialStanceById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(martialStanceRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.getMartialStanceById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Martial stance not found with id: 999");
    }

    // ==================== CREATE MARTIAL STANCE TESTS ====================

    @Test
    void createMartialStance_ValidRequest_CreatesAndReturnsMartialStance() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .description("Gain a bonus to damage rolls equal to a trait of your choice.")
                .build();

        MartialStance savedMartialStance = createTestMartialStance(1L, "Favored", expansion, 1);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(martialStanceRepository.save(any(MartialStance.class))).thenReturn(savedMartialStance);

        // Act
        MartialStanceResponse result = martialStanceService.createMartialStance(request, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Favored");
        verify(martialStanceRepository).save(any(MartialStance.class));
    }

    @Test
    void createMartialStance_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(999L)
                .tier(1)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.createMartialStance(request, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(martialStanceRepository, never()).save(any());
    }

    @Test
    void createMartialStance_WithOriginalMartialStance_AttachesOriginal() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();
        MartialStance originalMartialStance = createTestMartialStance(1L, "Favored", expansion, 1);

        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Custom Favored")
                .expansionId(1L)
                .tier(1)
                .isOfficial(false)
                .description("A modified version")
                .originalMartialStanceId(1L)
                .build();

        MartialStance savedMartialStance = createTestMartialStance(2L, "Custom Favored", expansion, 1);
        savedMartialStance.setOriginalMartialStance(originalMartialStance);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(originalMartialStance));
        when(martialStanceRepository.save(any(MartialStance.class))).thenReturn(savedMartialStance);

        // Act
        MartialStanceResponse result = martialStanceService.createMartialStance(request, null);

        // Assert
        assertThat(result.getOriginalMartialStanceId()).isEqualTo(1L);
    }

    // ==================== CREATE MARTIAL STANCE BULK TESTS ====================

    @Test
    void createMartialStanceBulk_ValidRequests_CreatesAndReturnsMartialStances() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        CreateMartialStanceRequest request1 = CreateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .build();

        CreateMartialStanceRequest request2 = CreateMartialStanceRequest.builder()
                .name("Reliable")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .description("Gain a +1 bonus to your attack rolls.")
                .build();

        MartialStance savedStance1 = createTestMartialStance(1L, "Favored", expansion, 1);
        MartialStance savedStance2 = createTestMartialStance(2L, "Reliable", expansion, 1);
        savedStance2.setDescription("Gain a +1 bonus to your attack rolls.");

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(martialStanceRepository.saveAll(anyList())).thenReturn(List.of(savedStance1, savedStance2));

        // Act
        List<MartialStanceResponse> results = martialStanceService.createMartialStanceBulk(List.of(request1, request2), null);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Favored");
        assertThat(results.get(1).getName()).isEqualTo("Reliable");
        verify(martialStanceRepository).saveAll(anyList());
    }

    // ==================== UPDATE MARTIAL STANCE TESTS ====================

    @Test
    void updateMartialStance_ValidRequest_UpdatesAndReturnsMartialStance() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance existingMartialStance = createTestMartialStance(1L, "Old Name", expansion, 1);

        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .description("Updated description")
                .build();

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingMartialStance));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(martialStanceRepository.save(any(MartialStance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MartialStanceResponse result = martialStanceService.updateMartialStance(1L, request, null);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(martialStanceRepository).save(any(MartialStance.class));
    }

    @Test
    void updateMartialStance_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .build();

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.updateMartialStance(999L, request, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Martial stance not found with id: 999");

        verify(martialStanceRepository, never()).save(any());
    }

    // ==================== DELETE MARTIAL STANCE TESTS ====================

    @Test
    void deleteMartialStance_ValidId_SoftDeletesMartialStance() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance stance = createTestMartialStance(1L, "To Delete", expansion, 1);

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(stance));

        // Act
        martialStanceService.deleteMartialStance(1L, null);

        // Assert
        verify(martialStanceRepository).save(argThat(m -> m.getDeletedAt() != null));
    }

    @Test
    void deleteMartialStance_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(martialStanceRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.deleteMartialStance(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Martial stance not found with id: 999");

        verify(martialStanceRepository, never()).save(any());
    }

    // ==================== RESTORE MARTIAL STANCE TESTS ====================

    @Test
    void restoreMartialStance_DeletedMartialStance_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance deletedStance = createTestMartialStance(1L, "Deleted Stance", expansion, 1);
        deletedStance.setDeletedAt(LocalDateTime.now());

        when(martialStanceRepository.findById(1L)).thenReturn(Optional.of(deletedStance));
        when(martialStanceRepository.save(any(MartialStance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MartialStanceResponse result = martialStanceService.restoreMartialStance(1L, null);

        // Assert
        assertThat(result).isNotNull();
        verify(martialStanceRepository).save(argThat(m -> m.getDeletedAt() == null));
    }

    @Test
    void restoreMartialStance_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance activeStance = createTestMartialStance(1L, "Active Stance", expansion, 1);

        when(martialStanceRepository.findById(1L)).thenReturn(Optional.of(activeStance));

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.restoreMartialStance(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Martial stance with id 1 is not deleted");

        verify(martialStanceRepository, never()).save(any());
    }

    @Test
    void restoreMartialStance_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(martialStanceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> martialStanceService.restoreMartialStance(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Martial stance not found with id: 999");
    }

    // ==================== EXPAND ORIGINAL MARTIAL STANCE TESTS ====================

    @Test
    void getMartialStanceById_WithExpandOriginalMartialStance_ExpandsOriginal() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).createdAt(LocalDateTime.now()).build();

        MartialStance originalMartialStance = createTestMartialStance(1L, "Favored", expansion, 1);
        MartialStance customMartialStance = createTestMartialStance(2L, "Custom Favored", expansion, 1);
        customMartialStance.setOriginalMartialStance(originalMartialStance);

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(customMartialStance));

        // Act
        MartialStanceResponse result = martialStanceService.getMartialStanceById(2L, "originalMartialStance");

        // Assert
        assertThat(result.getOriginalMartialStance()).isNotNull();
        assertThat(result.getOriginalMartialStance().getName()).isEqualTo("Favored");
    }

    // ==================== FEATURE TESTS ====================

    @Test
    void createMartialStance_WithFeatureIds_AttachesFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Aggressive Stance").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateMartialStanceRequest request = CreateMartialStanceRequest.builder()
                .name("Aggressive")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .description("A penalty-for-power stance")
                .featureIds(List.of(1L))
                .build();

        MartialStance savedMartialStance = createTestMartialStance(1L, "Aggressive", expansion, 2);
        savedMartialStance.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(martialStanceRepository.save(any(MartialStance.class))).thenReturn(savedMartialStance);

        // Act
        MartialStanceResponse result = martialStanceService.createMartialStance(request, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        verify(featureService).resolveFeatures(eq(List.of(1L)), isNull());
    }

    @Test
    void updateMartialStance_WithNullFeatures_DoesNotModifyFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();

        MartialStance existingMartialStance = createTestMartialStance(1L, "Favored", expansion, 1);

        UpdateMartialStanceRequest request = UpdateMartialStanceRequest.builder()
                .name("Favored")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .description("Same stance")
                .build();

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingMartialStance));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(martialStanceRepository.save(any(MartialStance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        martialStanceService.updateMartialStance(1L, request, null);

        // Assert
        verify(featureService, never()).resolveFeatures(any(), any());
    }

    @Test
    void getMartialStanceById_WithExpandFeatures_ExpandsFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Aggressive Stance").featureType(FeatureType.OTHER).expansion(expansion).createdAt(LocalDateTime.now()).build();

        MartialStance stance = createTestMartialStance(1L, "Aggressive", expansion, 2);
        stance.setFeatures(Set.of(feature));

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(stance));
        when(featureService.toResponse(any(Feature.class), anySet())).thenReturn(
                FeatureResponse.builder()
                        .id(1L)
                        .name("Aggressive Stance")
                        .featureType(FeatureType.OTHER)
                        .expansionId(1L)
                        .build()
        );

        // Act
        MartialStanceResponse result = martialStanceService.getMartialStanceById(1L, "features");

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        assertThat(result.getFeatures()).hasSize(1);
        assertThat(result.getFeatures().get(0).getName()).isEqualTo("Aggressive Stance");
        verify(featureService).toResponse(any(Feature.class), anySet());
    }

    @Test
    void getMartialStanceById_WithoutExpandFeatures_IncludesOnlyIds() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope and Fear").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Aggressive Stance").featureType(FeatureType.OTHER).expansion(expansion).build();

        MartialStance stance = createTestMartialStance(1L, "Aggressive", expansion, 2);
        stance.setFeatures(Set.of(feature));

        when(martialStanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(stance));

        // Act
        MartialStanceResponse result = martialStanceService.getMartialStanceById(1L, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        assertThat(result.getFeatures()).isNull();
        verify(featureService, never()).toResponse(any(Feature.class), anySet());
    }

    // ==================== HELPER METHODS ====================

    private MartialStance createTestMartialStance(Long id, String name, Expansion expansion, int tier) {
        return MartialStance.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .tier(tier)
                .isOfficial(true)
                .description("Gain a bonus to damage rolls equal to a trait of your choice.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
