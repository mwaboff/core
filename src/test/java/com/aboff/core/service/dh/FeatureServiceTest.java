package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CostTagInput;
import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.FeatureInput;
import com.aboff.core.model.dto.dh.request.FeatureModifierInput;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeatureService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private CardCostTagService cardCostTagService;

    @Mock
    private FeatureModifierService featureModifierService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FeatureService featureService;

    // ==================== GET ALL FEATURES TESTS ====================

    @Test
    void getAllFeatures_WithoutFilters_ReturnsPagedFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature1 = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature2 = Feature.builder()
                .id(2L)
                .name("Arcane Blast")
                .description("Deal magic damage")
                .featureType(FeatureType.CLASS)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Feature> featurePage = new PageImpl<>(List.of(feature1, feature2));
        when(featureRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        PagedResponse<FeatureResponse> result = featureService.getAllFeatures(0, 20, false, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Healing Touch");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Arcane Blast");
    }

    @Test
    void getAllFeatures_WithExpansionFilter_ReturnsFilteredFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Feature> featurePage = new PageImpl<>(List.of(feature));
        when(featureRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        PagedResponse<FeatureResponse> result = featureService.getAllFeatures(0, 20, false, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(featureRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllFeatures_WithFeatureTypeFilter_ReturnsFilteredFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Feature> featurePage = new PageImpl<>(List.of(feature));
        when(featureRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(FeatureType.HOPE), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        PagedResponse<FeatureResponse> result = featureService.getAllFeatures(0, 20, false, null, FeatureType.HOPE, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatureType()).isEqualTo(FeatureType.HOPE);
        verify(featureRepository).findByDeletedAtIsNullAndFilters(isNull(), eq(FeatureType.HOPE), any(Pageable.class));
    }

    @Test
    void getAllFeatures_WithIncludeDeleted_ReturnsAllFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Deleted Feature")
                .description("Deleted")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<Feature> featurePage = new PageImpl<>(List.of(feature));
        when(featureRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        PagedResponse<FeatureResponse> result = featureService.getAllFeatures(0, 20, true, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(featureRepository).findAllWithFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllFeatures_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Feature> featurePage = new PageImpl<>(List.of());
        when(featureRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        featureService.getAllFeatures(0, 500, false, null, null, null);

        // Assert
        verify(featureRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllFeatures_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Feature> featurePage = new PageImpl<>(List.of(feature));
        when(featureRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(featurePage);

        // Act
        PagedResponse<FeatureResponse> result = featureService.getAllFeatures(0, 20, false, null, null, "expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    // ==================== GET FEATURE BY ID TESTS ====================

    @Test
    void getFeatureById_ValidId_ReturnsFeature() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(feature));

        // Act
        FeatureResponse result = featureService.getFeatureById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Healing Touch");
        assertThat(result.getDescription()).isEqualTo("Restore HP to allies");
        assertThat(result.getFeatureType()).isEqualTo(FeatureType.HOPE);
    }

    @Test
    void getFeatureById_WithExpandParameter_ExpandsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(feature));

        // Act
        FeatureResponse result = featureService.getFeatureById(1L, "expansion");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExpansion()).isNotNull();
        assertThat(result.getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    @Test
    void getFeatureById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.getFeatureById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");
    }

    // ==================== CREATE FEATURE TESTS ====================

    @Test
    void createFeature_ValidRequest_CreatesAndReturnsFeature() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(featureRepository.save(any(Feature.class)))
                .thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Healing Touch");
        assertThat(result.getDescription()).isEqualTo("Restore HP to allies");
        assertThat(result.getFeatureType()).isEqualTo(FeatureType.HOPE);

        verify(featureRepository).save(argThat(feature ->
                feature.getName().equals("Healing Touch") &&
                        feature.getDescription().equals("Restore HP to allies") &&
                        feature.getFeatureType().equals(FeatureType.HOPE)
        ));
    }

    @Test
    void createFeature_NullName_CreatesFeatureSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L).name("Core Rulebook").isPublished(true).build();
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name(null)
                .description("A nameless feature")
                .featureType(FeatureType.DOMAIN)
                .expansionId(1L)
                .build();
        Feature savedFeature = Feature.builder()
                .id(1L).name(null).description("A nameless feature")
                .featureType(FeatureType.DOMAIN).expansion(expansion).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result.getName()).isNull();
        assertThat(result.getDescription()).isEqualTo("A nameless feature");
        verify(featureRepository).save(argThat(feature ->
                feature.getName() == null &&
                        feature.getDescription().equals("A nameless feature")
        ));
    }

    @Test
    void createFeature_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(999L)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.createFeature(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(featureRepository, never()).save(any());
    }

    // ==================== UPDATE FEATURE TESTS ====================

    @Test
    void updateFeature_ValidRequest_UpdatesAndReturnsFeature() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(1L)
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));
        when(featureRepository.save(any(Feature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getFeatureType()).isEqualTo(FeatureType.CLASS);

        verify(featureRepository).save(argThat(feature ->
                feature.getName().equals("Updated Name") &&
                        feature.getDescription().equals("Updated description") &&
                        feature.getFeatureType().equals(FeatureType.CLASS)
        ));
    }

    @Test
    void updateFeature_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(1L)
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.updateFeature(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");

        verify(featureRepository, never()).save(any());
    }

    @Test
    void updateFeature_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .featureType(FeatureType.CLASS)
                .expansionId(999L)
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.updateFeature(1L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(featureRepository, never()).save(any());
    }

    // ==================== DELETE FEATURE TESTS ====================

    @Test
    void deleteFeature_ValidId_SoftDeletesFeature() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(feature));

        // Act
        featureService.deleteFeature(1L, authentication);

        // Assert
        verify(featureRepository).save(argThat(f -> f.getDeletedAt() != null));
    }

    @Test
    void deleteFeature_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.deleteFeature(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");

        verify(featureRepository, never()).save(any());
    }

    // ==================== RESTORE FEATURE TESTS ====================

    @Test
    void restoreFeature_DeletedFeature_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature deletedFeature = Feature.builder()
                .id(1L)
                .name("Deleted Feature")
                .description("Deleted")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(featureRepository.findById(1L))
                .thenReturn(Optional.of(deletedFeature));
        when(featureRepository.save(any(Feature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.restoreFeature(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(featureRepository).save(argThat(f -> f.getDeletedAt() == null));
    }

    @Test
    void restoreFeature_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .build();

        Feature activeFeature = Feature.builder()
                .id(1L)
                .name("Active Feature")
                .description("Active")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findById(1L))
                .thenReturn(Optional.of(activeFeature));

        // Act & Assert
        assertThatThrownBy(() -> featureService.restoreFeature(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature with id 1 is not deleted");

        verify(featureRepository, never()).save(any());
    }

    @Test
    void restoreFeature_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.restoreFeature(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");
    }

    // ==================== COST TAG TESTS ====================

    @Test
    void createFeature_WithCostTagIds_SetsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTagIds(List.of(1L))
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), isNull())).thenReturn(Set.of(costTag));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), isNull());
    }

    @Test
    void createFeature_WithCostTagInputs_ResolvesAndSetsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();
        List<CostTagInput> costTagInputs = List.of(
                CostTagInput.builder().label("3 Hope").category(CostTagCategory.COST).build()
        );

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTags(costTagInputs)
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), eq(costTagInputs))).thenReturn(Set.of(costTag));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(isNull(), eq(costTagInputs));
    }

    @Test
    void createFeature_WithBothCostTagIdsAndInputs_MergesBoth() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag1 = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();
        CardCostTag costTag2 = CardCostTag.builder().id(2L).label("1/session").category(CostTagCategory.TIMING).build();
        List<CostTagInput> costTagInputs = List.of(
                CostTagInput.builder().label("1/session").category(CostTagCategory.TIMING).build()
        );

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTagIds(List.of(1L))
                .costTags(costTagInputs)
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(Set.of(costTag1, costTag2))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), eq(costTagInputs))).thenReturn(Set.of(costTag1, costTag2));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), eq(costTagInputs));
    }

    @Test
    void updateFeature_WithCostTags_UpdatesCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTagIds(List.of(1L))
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), isNull())).thenReturn(Set.of(costTag));
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), isNull());
    }

    @Test
    void updateFeature_WithEmptyCostTags_ClearsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(new HashSet<>(Set.of(costTag)))
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTagIds(List.of())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(cardCostTagService).resolveCostTags(eq(List.of()), isNull());
    }

    @Test
    void updateFeature_WithNullCostTags_DoesNotChangeCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(new HashSet<>(Set.of(costTag)))
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
    }

    @Test
    void getFeatureById_WithExpandCostTags_ExpandsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder()
                .id(1L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(feature));

        // Act
        FeatureResponse result = featureService.getFeatureById(1L, "costTags");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        assertThat(result.getCostTags()).isNotNull();
        assertThat(result.getCostTags()).hasSize(1);
        assertThat(result.getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(result.getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
    }

    // ==================== MODIFIER TESTS ====================

    @Test
    void createFeature_WithModifierIds_SetsModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansionId(1L)
                .modifierIds(List.of(1L))
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansion(expansion)
                .modifiers(Set.of(modifier))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(eq(List.of(1L)), isNull())).thenReturn(Set.of(modifier));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(1L);
        verify(featureModifierService).resolveModifiers(eq(List.of(1L)), isNull());
    }

    @Test
    void createFeature_WithModifierInputs_ResolvesAndSetsModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.EVASION).operation(ModifierOperation.ADD).value(-1).build();
        List<FeatureModifierInput> modifierInputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.EVASION)
                        .operation(ModifierOperation.ADD)
                        .value(-1)
                        .build()
        );

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Heavy Armor Training")
                .description("Reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansionId(1L)
                .modifiers(modifierInputs)
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Heavy Armor Training")
                .description("Reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansion(expansion)
                .modifiers(Set.of(modifier))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(isNull(), eq(modifierInputs))).thenReturn(Set.of(modifier));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(1L);
        verify(featureModifierService).resolveModifiers(isNull(), eq(modifierInputs));
    }

    @Test
    void createFeature_WithBothModifierIdsAndInputs_MergesBoth() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier1 = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        FeatureModifier modifier2 = FeatureModifier.builder()
                .id(2L).target(ModifierTarget.EVASION).operation(ModifierOperation.ADD).value(-1).build();
        List<FeatureModifierInput> modifierInputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.EVASION)
                        .operation(ModifierOperation.ADD)
                        .value(-1)
                        .build()
        );

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Balanced Stance")
                .description("Boosts strength, reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansionId(1L)
                .modifierIds(List.of(1L))
                .modifiers(modifierInputs)
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L)
                .name("Balanced Stance")
                .description("Boosts strength, reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansion(expansion)
                .modifiers(Set.of(modifier1, modifier2))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(eq(List.of(1L)), eq(modifierInputs)))
                .thenReturn(Set.of(modifier1, modifier2));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        FeatureResponse result = featureService.createFeature(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(featureModifierService).resolveModifiers(eq(List.of(1L)), eq(modifierInputs));
    }

    @Test
    void updateFeature_WithModifiers_UpdatesModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansion(expansion)
                .modifiers(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansionId(1L)
                .modifierIds(List.of(1L))
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(eq(List.of(1L)), isNull())).thenReturn(Set.of(modifier));
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(1L);
        verify(featureModifierService).resolveModifiers(eq(List.of(1L)), isNull());
    }

    @Test
    void updateFeature_WithEmptyModifiers_ClearsModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansion(expansion)
                .modifiers(new HashSet<>(Set.of(modifier)))
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansionId(1L)
                .modifierIds(List.of())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(featureModifierService).resolveModifiers(eq(List.of()), isNull());
    }

    @Test
    void updateFeature_WithNullModifiers_DoesNotChangeModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();

        Feature existingFeature = Feature.builder()
                .id(1L)
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansion(expansion)
                .modifiers(new HashSet<>(Set.of(modifier)))
                .createdAt(LocalDateTime.now())
                .build();

        UpdateFeatureRequest request = UpdateFeatureRequest.builder()
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansionId(1L)
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingFeature));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FeatureResponse result = featureService.updateFeature(1L, request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(1L);
    }

    @Test
    void toResponse_IncludesModifierIdsAlways() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(5L).target(ModifierTarget.EVASION).operation(ModifierOperation.ADD).value(-1).build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Heavy Armor Training")
                .description("Reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansion(expansion)
                .modifiers(Set.of(modifier))
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(feature));

        // Act
        FeatureResponse result = featureService.getFeatureById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(5L);
        assertThat(result.getModifiers()).isNull();
    }

    @Test
    void toResponse_ExpandsModifiersWhenRequested() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(5L)
                .target(ModifierTarget.EVASION)
                .operation(ModifierOperation.ADD)
                .value(-1)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Heavy Armor Training")
                .description("Reduces evasion")
                .featureType(FeatureType.CLASS)
                .expansion(expansion)
                .modifiers(Set.of(modifier))
                .createdAt(LocalDateTime.now())
                .build();

        when(featureRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(feature));

        // Act
        FeatureResponse result = featureService.getFeatureById(1L, "modifiers");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModifierIds()).containsExactly(5L);
        assertThat(result.getModifiers()).isNotNull();
        assertThat(result.getModifiers()).hasSize(1);
        assertThat(result.getModifiers().get(0).getTarget()).isEqualTo(ModifierTarget.EVASION);
        assertThat(result.getModifiers().get(0).getOperation()).isEqualTo(ModifierOperation.ADD);
        assertThat(result.getModifiers().get(0).getValue()).isEqualTo(-1);
    }

    @Test
    void findOrCreate_NoMatch_WithModifiers_CreatesWithModifiers() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();
        List<FeatureModifierInput> modifierInputs = List.of(
                FeatureModifierInput.builder()
                        .target(ModifierTarget.STRENGTH)
                        .operation(ModifierOperation.ADD)
                        .value(1)
                        .build()
        );
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(1L)
                .modifiers(modifierInputs).build();
        Feature savedFeature = Feature.builder()
                .id(10L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
                .expansion(expansion).modifiers(Set.of(modifier)).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 1L, FeatureType.ANCESTRY, null))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), eq(modifierInputs))).thenReturn(Set.of(modifier));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getModifiers()).hasSize(1);
        verify(featureModifierService).resolveModifiers(isNull(), eq(modifierInputs));
    }

    @Test
    void findOrCreate_NullName_SkipsLookupAndCreatesDirectly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name(null).description("A nameless feature")
                .featureType(FeatureType.DOMAIN).expansionId(1L).build();
        Feature savedFeature = Feature.builder()
                .id(11L).name(null).description("A nameless feature")
                .featureType(FeatureType.DOMAIN).expansion(expansion).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getName()).isNull();
        verify(featureRepository, never()).findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                any(), anyLong(), any(), any());
        verify(featureRepository).save(any(Feature.class));
    }

    @Test
    void findOrCreate_BlankName_SkipsLookupAndCreatesDirectly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("   ").description("A blank-named feature")
                .featureType(FeatureType.DOMAIN).expansionId(1L).build();
        Feature savedFeature = Feature.builder()
                .id(12L).name("   ").description("A blank-named feature")
                .featureType(FeatureType.DOMAIN).expansion(expansion).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getId()).isEqualTo(12L);
        verify(featureRepository, never()).findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                any(), anyLong(), any(), any());
        verify(featureRepository).save(any(Feature.class));
    }

    // ==================== BULK CREATE TESTS ====================

    @Test
    void createFeaturesBulk_ValidRequests_CreatesAllFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateFeatureRequest request1 = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .build();

        CreateFeatureRequest request2 = CreateFeatureRequest.builder()
                .name("Arcane Blast")
                .description("Deal magic damage")
                .featureType(FeatureType.CLASS)
                .expansionId(1L)
                .build();

        Feature savedFeature1 = Feature.builder()
                .id(1L).name("Healing Touch").description("Restore HP to allies")
                .featureType(FeatureType.HOPE).expansion(expansion).createdAt(LocalDateTime.now()).build();
        Feature savedFeature2 = Feature.builder()
                .id(2L).name("Arcane Blast").description("Deal magic damage")
                .featureType(FeatureType.CLASS).expansion(expansion).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureRepository.saveAll(anyList())).thenReturn(List.of(savedFeature1, savedFeature2));

        // Act
        List<FeatureResponse> results = featureService.createFeaturesBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Healing Touch");
        assertThat(results.get(1).getName()).isEqualTo("Arcane Blast");
        verify(featureRepository).saveAll(anyList());
    }

    @Test
    void createFeaturesBulk_WithCostTags_ResolvesTagsForEachFeature() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(1L)
                .costTagIds(List.of(1L))
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L).name("Healing Touch").description("Restore HP to allies")
                .featureType(FeatureType.HOPE).expansion(expansion)
                .costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), isNull())).thenReturn(Set.of(costTag));
        when(featureRepository.saveAll(anyList())).thenReturn(List.of(savedFeature));

        // Act
        List<FeatureResponse> results = featureService.createFeaturesBulk(List.of(request), authentication);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), isNull());
    }

    @Test
    void createFeaturesBulk_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Healing Touch")
                .description("Restore HP to allies")
                .featureType(FeatureType.HOPE)
                .expansionId(999L)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.createFeaturesBulk(List.of(request), authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(featureRepository, never()).saveAll(anyList());
    }

    @Test
    void createFeaturesBulk_WithModifiers_ResolvesModifiersForEachFeature() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureModifier modifier = FeatureModifier.builder()
                .id(1L).target(ModifierTarget.STRENGTH).operation(ModifierOperation.ADD).value(1).build();

        CreateFeatureRequest request = CreateFeatureRequest.builder()
                .name("Mighty Presence")
                .description("Boosts strength")
                .featureType(FeatureType.ANCESTRY)
                .expansionId(1L)
                .modifierIds(List.of(1L))
                .build();

        Feature savedFeature = Feature.builder()
                .id(1L).name("Mighty Presence").description("Boosts strength")
                .featureType(FeatureType.ANCESTRY).expansion(expansion)
                .modifiers(Set.of(modifier)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(eq(List.of(1L)), isNull())).thenReturn(Set.of(modifier));
        when(featureRepository.saveAll(anyList())).thenReturn(List.of(savedFeature));

        // Act
        List<FeatureResponse> results = featureService.createFeaturesBulk(List.of(request), authentication);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getModifierIds()).containsExactly(1L);
        verify(featureModifierService).resolveModifiers(eq(List.of(1L)), isNull());
    }

    // ==================== FIND OR CREATE TESTS ====================

    @Test
    void findOrCreate_ExistingFeature_ReturnsExisting() {
        // Arrange: name AND description both match the existing row (idempotent re-import).
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature existingFeature = Feature.builder()
                .id(5L).name("Mighty Leap").description("Jump far")
                .featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(1L)
                .description("Jump far").build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 1L, FeatureType.ANCESTRY, "Jump far"))
            .thenReturn(Optional.of(existingFeature));

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result).isEqualTo(existingFeature);
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getDescription()).isEqualTo("Jump far");
        verify(featureRepository, never()).save(any());
        verify(expansionRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void findOrCreate_ExistingFeatureCaseInsensitive_ReturnsExisting() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature existingFeature = Feature.builder()
                .id(5L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
                .expansion(expansion).build();
        FeatureInput input = FeatureInput.builder()
                .name("mighty leap").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "mighty leap", 1L, FeatureType.ANCESTRY, null))
            .thenReturn(Optional.of(existingFeature));

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result).isEqualTo(existingFeature);
        verify(featureRepository, never()).save(any());
    }

    @Test
    void findOrCreate_NameMatchDifferentDescription_CreatesDifferentiatedFeature() {
        // Arrange: same name+type+expansion as an existing row, but different description —
        // e.g. the core rulebook's Tier 1 "Barrier: +2 to Armor Score" vs. Tier 2's
        // "Barrier: +3 to Armor Score". The repository key is description-aware, so this must
        // NOT match the existing row and must create a new, differentiated Feature instead of
        // silently reusing (and thereby corrupting) the Tier 1 row.
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Barrier").featureType(FeatureType.ITEM).expansionId(1L)
                .description("+3 to Armor Score").build();
        Feature newFeature = Feature.builder()
                .id(20L).name("Barrier").description("+3 to Armor Score")
                .featureType(FeatureType.ITEM).expansion(expansion).build();

        // The existing (Tier 1) row has a different description, so the description-aware
        // lookup correctly reports no match.
        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Barrier", 1L, FeatureType.ITEM, "+3 to Armor Score"))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(newFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert: a new, distinct row was created preserving the Tier 2 description; the
        // (hypothetical) Tier 1 row and its description are untouched by this call.
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("Barrier");
        assertThat(result.getDescription()).isEqualTo("+3 to Armor Score");
        verify(featureRepository).save(any(Feature.class));
    }

    @Test
    void findOrCreate_NullDescriptionMatchesExistingNullDescription_ReturnsExisting() {
        // Arrange: both the incoming input and the existing row have a null description — this
        // must still match (null-safe comparison), not be treated as a mismatch.
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature existingFeature = Feature.builder()
                .id(6L).name("Nameless Boon").description(null)
                .featureType(FeatureType.OTHER).expansion(expansion).build();
        FeatureInput input = FeatureInput.builder()
                .name("Nameless Boon").featureType(FeatureType.OTHER).expansionId(1L)
                .description(null).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Nameless Boon", 1L, FeatureType.OTHER, null))
            .thenReturn(Optional.of(existingFeature));

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result).isEqualTo(existingFeature);
        assertThat(result.getDescription()).isNull();
        verify(featureRepository, never()).save(any());
    }

    @Test
    void findOrCreate_NullDescriptionDoesNotMatchNonNullDescription_CreatesNewFeature() {
        // Arrange: incoming description is null but the existing row (same name/type/expansion)
        // has a non-null description. Null must not be treated as a wildcard that matches
        // anything — this must miss and create a new row.
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Paired").featureType(FeatureType.ITEM).expansionId(1L)
                .description(null).build();
        Feature newFeature = Feature.builder()
                .id(21L).name("Paired").description(null)
                .featureType(FeatureType.ITEM).expansion(expansion).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Paired", 1L, FeatureType.ITEM, null))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureModifierService.resolveModifiers(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(newFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getId()).isEqualTo(21L);
        assertThat(result.getDescription()).isNull();
        verify(featureRepository).save(any(Feature.class));
    }

    @Test
    void findOrCreate_NoMatch_CreatesNewFeature() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").description("Jump far")
                .featureType(FeatureType.ANCESTRY).expansionId(1L).build();
        Feature savedFeature = Feature.builder()
                .id(10L).name("Mighty Leap").description("Jump far")
                .featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 1L, FeatureType.ANCESTRY, "Jump far"))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Mighty Leap");
        verify(expansionRepository).findByIdAndDeletedAtIsNull(1L);
        verify(featureRepository).save(any(Feature.class));
    }

    @Test
    void findOrCreate_NoMatch_WithCostTags_CreatesWithTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("1/session").category(CostTagCategory.LIMITATION).build();
        List<CostTagInput> costTagInputs = List.of(
                CostTagInput.builder().label("1/session").category(CostTagCategory.LIMITATION).build());
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(1L)
                .costTags(costTagInputs).build();
        Feature savedFeature = Feature.builder()
                .id(10L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
                .expansion(expansion).costTags(Set.of(costTag)).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 1L, FeatureType.ANCESTRY, null))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), eq(costTagInputs))).thenReturn(Set.of(costTag));
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getCostTags()).hasSize(1);
        verify(cardCostTagService).resolveCostTags(isNull(), eq(costTagInputs));
    }

    @Test
    void findOrCreate_NoMatch_WithoutCostTags_CreatesWithoutTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(1L).build();
        Feature savedFeature = Feature.builder()
                .id(10L).name("Mighty Leap").featureType(FeatureType.ANCESTRY)
                .expansion(expansion).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 1L, FeatureType.ANCESTRY, null))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), isNull())).thenReturn(null);
        when(featureRepository.save(any(Feature.class))).thenReturn(savedFeature);

        // Act
        Feature result = featureService.findOrCreate(input);

        // Assert
        assertThat(result.getCostTags()).isNull();
        verify(cardCostTagService).resolveCostTags(isNull(), isNull());
    }

    @Test
    void findOrCreate_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        FeatureInput input = FeatureInput.builder()
                .name("Mighty Leap").featureType(FeatureType.ANCESTRY).expansionId(999L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Mighty Leap", 999L, FeatureType.ANCESTRY, null))
            .thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.findOrCreate(input))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(featureRepository, never()).save(any());
    }

    // ==================== RESOLVE FEATURES TESTS ====================

    @Test
    void resolveFeatures_BothNull_ReturnsNull() {
        // Act
        Set<Feature> result = featureService.resolveFeatures(null, null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void resolveFeatures_BothEmpty_ReturnsEmptySet() {
        // Act
        Set<Feature> result = featureService.resolveFeatures(List.of(), List.of());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void resolveFeatures_IdsOnly_ResolvesById() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature1 = Feature.builder().id(1L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        Feature feature2 = Feature.builder().id(2L).name("F2").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
                .thenReturn(List.of(feature1, feature2));

        // Act
        Set<Feature> result = featureService.resolveFeatures(List.of(1L, 2L), null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(feature1, feature2);
    }

    @Test
    void resolveFeatures_InputsOnly_FindsOrCreatesEach() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature1 = Feature.builder().id(1L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        Feature feature2 = Feature.builder().id(2L).name("F2").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        FeatureInput input1 = FeatureInput.builder().name("F1").featureType(FeatureType.ANCESTRY).expansionId(1L).build();
        FeatureInput input2 = FeatureInput.builder().name("F2").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "F1", 1L, FeatureType.ANCESTRY, null)).thenReturn(Optional.of(feature1));
        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "F2", 1L, FeatureType.ANCESTRY, null)).thenReturn(Optional.of(feature2));

        // Act
        Set<Feature> result = featureService.resolveFeatures(null, List.of(input1, input2));

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(feature1, feature2);
    }

    @Test
    void resolveFeatures_BothProvided_MergesResults() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature1 = Feature.builder().id(1L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        Feature feature2 = Feature.builder().id(2L).name("F2").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        FeatureInput input2 = FeatureInput.builder().name("F2").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(feature1));
        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "F2", 1L, FeatureType.ANCESTRY, null)).thenReturn(Optional.of(feature2));

        // Act
        Set<Feature> result = featureService.resolveFeatures(List.of(1L), List.of(input2));

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(feature1, feature2);
    }

    @Test
    void resolveFeatures_DuplicatesBetweenIdsAndInputs_Deduplicates() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        FeatureInput input = FeatureInput.builder().name("F1").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(feature));
        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "F1", 1L, FeatureType.ANCESTRY, null)).thenReturn(Optional.of(feature));

        // Act
        Set<Feature> result = featureService.resolveFeatures(List.of(1L), List.of(input));

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void resolveFeatures_IdsNotFound_ReturnsOnlyFound() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 999L))).thenReturn(List.of(feature));

        // Act
        Set<Feature> result = featureService.resolveFeatures(List.of(1L, 999L), null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(feature);
    }

    // ==================== RESOLVE FEATURE (SINGLE) TESTS ====================

    @Test
    void resolveFeature_BothNull_ReturnsNull() {
        // Act
        Feature result = featureService.resolveFeature(null, null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void resolveFeature_IdProvided_ResolvesById() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature = Feature.builder().id(5L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        when(featureRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(feature));

        // Act
        Feature result = featureService.resolveFeature(5L, null);

        // Assert
        assertThat(result).isEqualTo(feature);
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void resolveFeature_IdNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.resolveFeature(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");
    }

    @Test
    void resolveFeature_InputProvided_FindsOrCreates() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature = Feature.builder().id(5L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        FeatureInput input = FeatureInput.builder().name("F1").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "F1", 1L, FeatureType.ANCESTRY, null)).thenReturn(Optional.of(feature));

        // Act
        Feature result = featureService.resolveFeature(null, input);

        // Assert
        assertThat(result).isEqualTo(feature);
    }

    @Test
    void resolveFeature_BothProvided_IdTakesPrecedence() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core").isPublished(true).build();
        Feature feature = Feature.builder().id(5L).name("F1").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        FeatureInput input = FeatureInput.builder().name("Other").featureType(FeatureType.ANCESTRY).expansionId(1L).build();

        when(featureRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(feature));

        // Act
        Feature result = featureService.resolveFeature(5L, input);

        // Assert
        assertThat(result).isEqualTo(feature);
        verify(featureRepository, never()).findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                any(), any(), any(), any());
    }

    // ==================== FEATURE ORIGIN TESTS ====================

    @Test
    void findOrCreate_CustomItemOrigin_DropsTheRequestedSourcebook() {
        // A logged-in user POSTing to /api/dh/weapons/custom controls every field of an inline
        // feature, including expansionId. Left alone it landed on the row, making a user's
        // feature indistinguishable from one printed in the Daggerheart Core Set.
        User author = User.builder().id(7L).username("author").build();
        FeatureInput input = FeatureInput.builder()
                .name("Serrated")
                .description("Deals extra damage.")
                .featureType(FeatureType.ITEM)
                .expansionId(1L)
                .build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Serrated", null, FeatureType.ITEM, "Deals extra damage.")).thenReturn(Optional.empty());
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        Feature result = featureService.findOrCreate(
                input, FeatureService.FeatureOrigin.forItem(author, false));

        assertThat(result.getExpansion()).isNull();
        verify(expansionRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void findOrCreate_CustomItemOrigin_LooksUpWithoutTheRequestedSourcebook() {
        // The expansion is part of the find-or-create key, so it has to be dropped before the
        // lookup. Otherwise a custom item's feature matches — and then silently reuses — an
        // official row belonging to a sourcebook it has no claim to.
        User author = User.builder().id(7L).username("author").build();
        FeatureInput input = FeatureInput.builder()
                .name("Brutal")
                .featureType(FeatureType.ITEM)
                .expansionId(1L)
                .build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        featureService.findOrCreate(input, FeatureService.FeatureOrigin.forItem(author, false));

        verify(featureRepository).findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Brutal", null, FeatureType.ITEM, null);
    }

    @Test
    void findOrCreate_CustomItemOrigin_RecordsTheAuthor() {
        // features.created_by_user_id exists precisely so a user-minted row can be traced back
        // and cleaned up. Nothing wrote it, so every row was NULL.
        User author = User.builder().id(7L).username("author").build();
        FeatureInput input = FeatureInput.builder()
                .name("Weightless").featureType(FeatureType.ITEM).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        Feature result = featureService.findOrCreate(
                input, FeatureService.FeatureOrigin.forItem(author, false));

        assertThat(result.getCreatedBy()).isEqualTo(author);
    }

    @Test
    void findOrCreate_OfficialItemOrigin_KeepsTheSourcebookAndRecordsNoAuthor() {
        // The other half of the rule: a moderator editing official content is entering catalogue
        // data, so the feature belongs to the book and to no individual.
        User moderator = User.builder().id(9L).username("mod").build();
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Barrier").featureType(FeatureType.ITEM).expansionId(1L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Barrier", 1L, FeatureType.ITEM, null)).thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        Feature result = featureService.findOrCreate(
                input, FeatureService.FeatureOrigin.forItem(moderator, true));

        assertThat(result.getExpansion()).isEqualTo(expansion);
        assertThat(result.getCreatedBy()).isNull();
    }

    @Test
    void findOrCreate_ImportedOrigin_IsTheDefaultForTheBulkImportPath() {
        // The single-argument overload is what the ADMIN/OWNER card and bulk-import services
        // call. It must keep assigning features to their sourcebook or every import breaks.
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        FeatureInput input = FeatureInput.builder()
                .name("Paired").featureType(FeatureType.ITEM).expansionId(1L).build();

        when(featureRepository.findByNameIgnoreCaseAndExpansionIdAndFeatureTypeAndDescriptionAndDeletedAtIsNull(
                "Paired", 1L, FeatureType.ITEM, null)).thenReturn(Optional.empty());
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));

        Feature result = featureService.findOrCreate(input);

        assertThat(result.getExpansion()).isEqualTo(expansion);
        assertThat(result.getCreatedBy()).isNull();
    }

    @Test
    void toResponse_FeatureWithNoSourcebook_ReturnsANullExpansionId() {
        // Reading back a feature authored on a custom item dereferenced a null expansion, so
        // GET /api/dh/weapons/{id}?expand=features on any custom item threw.
        Feature feature = Feature.builder()
                .id(3L).name("Homebrew").featureType(FeatureType.ITEM).expansion(null).build();

        FeatureResponse result = featureService.toResponse(feature, Set.of("expansion"));

        assertThat(result.getExpansionId()).isNull();
        assertThat(result.getExpansion()).isNull();
    }
}
