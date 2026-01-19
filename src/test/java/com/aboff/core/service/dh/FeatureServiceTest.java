package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateFeatureRequest;
import com.aboff.core.model.dto.dh.request.UpdateFeatureRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
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
        FeatureResponse result = featureService.createFeature(request);

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
        assertThatThrownBy(() -> featureService.createFeature(request))
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
        FeatureResponse result = featureService.updateFeature(1L, request);

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
        assertThatThrownBy(() -> featureService.updateFeature(999L, request))
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
        assertThatThrownBy(() -> featureService.updateFeature(1L, request))
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
        featureService.deleteFeature(1L);

        // Assert
        verify(featureRepository).save(argThat(f -> f.getDeletedAt() != null));
    }

    @Test
    void deleteFeature_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(featureRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> featureService.deleteFeature(999L))
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
        FeatureResponse result = featureService.restoreFeature(1L);

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
        assertThatThrownBy(() -> featureService.restoreFeature(1L))
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
        assertThatThrownBy(() -> featureService.restoreFeature(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Feature not found with id: 999");
    }
}
