package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateExpansionRequest;
import com.aboff.core.model.dto.dh.request.UpdateExpansionRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.repository.dh.ExpansionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpansionService.
 * Tests all CRUD operations, pagination, soft deletion, and restore functionality.
 */
@ExtendWith(MockitoExtension.class)
class ExpansionServiceTest {

    @Mock
    private ExpansionRepository expansionRepository;

    @InjectMocks
    private ExpansionService expansionService;

    // ==================== GET ALL EXPANSIONS TESTS ====================

    @Test
    void getAllExpansions_WithoutFilters_ReturnsPagedExpansions() {
        // Arrange
        Expansion expansion1 = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Expansion expansion2 = Expansion.builder()
                .id(2L)
                .name("Expansion Pack")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Expansion> expansionPage = new PageImpl<>(List.of(expansion1, expansion2));
        when(expansionRepository.findByDeletedAtIsNullAndPublished(isNull(), any(Pageable.class)))
                .thenReturn(expansionPage);

        // Act
        PagedResponse<ExpansionResponse> result = expansionService.getAllExpansions(0, 20, false, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Core Rulebook");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Expansion Pack");
    }

    @Test
    void getAllExpansions_WithPublishedFilter_ReturnsOnlyPublished() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Expansion> expansionPage = new PageImpl<>(List.of(expansion));
        when(expansionRepository.findByDeletedAtIsNullAndPublished(eq(true), any(Pageable.class)))
                .thenReturn(expansionPage);

        // Act
        PagedResponse<ExpansionResponse> result = expansionService.getAllExpansions(0, 20, false, true);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsPublished()).isTrue();
        verify(expansionRepository).findByDeletedAtIsNullAndPublished(eq(true), any(Pageable.class));
    }

    @Test
    void getAllExpansions_WithIncludeDeleted_ReturnsAllExpansions() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Deleted Expansion")
                .isPublished(false)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<Expansion> expansionPage = new PageImpl<>(List.of(expansion));
        when(expansionRepository.findAllWithPublished(isNull(), any(Pageable.class)))
                .thenReturn(expansionPage);

        // Act
        PagedResponse<ExpansionResponse> result = expansionService.getAllExpansions(0, 20, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(expansionRepository).findAllWithPublished(isNull(), any(Pageable.class));
    }

    @Test
    void getAllExpansions_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Expansion> expansionPage = new PageImpl<>(List.of());
        when(expansionRepository.findByDeletedAtIsNullAndPublished(isNull(), any(Pageable.class)))
                .thenReturn(expansionPage);

        // Act
        expansionService.getAllExpansions(0, 500, false, null);

        // Assert
        verify(expansionRepository).findByDeletedAtIsNullAndPublished(
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    // ==================== GET EXPANSION BY ID TESTS ====================

    @Test
    void getExpansionById_ValidId_ReturnsExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));

        // Act
        ExpansionResponse result = expansionService.getExpansionById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Core Rulebook");
        assertThat(result.getIsPublished()).isTrue();
    }

    @Test
    void getExpansionById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expansionService.getExpansionById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");
    }

    // ==================== CREATE EXPANSION TESTS ====================

    @Test
    void createExpansion_ValidRequest_CreatesAndReturnsExpansion() {
        // Arrange
        CreateExpansionRequest request = CreateExpansionRequest.builder()
                .name("New Expansion")
                .isPublished(false)
                .build();

        Expansion savedExpansion = Expansion.builder()
                .id(1L)
                .name("New Expansion")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.save(any(Expansion.class)))
                .thenReturn(savedExpansion);

        // Act
        ExpansionResponse result = expansionService.createExpansion(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Expansion");
        assertThat(result.getIsPublished()).isFalse();

        verify(expansionRepository).save(argThat(exp ->
                exp.getName().equals("New Expansion") &&
                        exp.getIsPublished().equals(false)
        ));
    }

    // ==================== UPDATE EXPANSION TESTS ====================

    @Test
    void updateExpansion_ValidRequest_UpdatesAndReturnsExpansion() {
        // Arrange
        Expansion existingExpansion = Expansion.builder()
                .id(1L)
                .name("Old Name")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateExpansionRequest request = UpdateExpansionRequest.builder()
                .name("Updated Name")
                .isPublished(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingExpansion));
        when(expansionRepository.save(any(Expansion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExpansionResponse result = expansionService.updateExpansion(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getIsPublished()).isTrue();

        verify(expansionRepository).save(argThat(exp ->
                exp.getName().equals("Updated Name") &&
                        exp.getIsPublished().equals(true)
        ));
    }

    @Test
    void updateExpansion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateExpansionRequest request = UpdateExpansionRequest.builder()
                .name("Updated Name")
                .isPublished(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expansionService.updateExpansion(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(expansionRepository, never()).save(any());
    }

    // ==================== DELETE EXPANSION TESTS ====================

    @Test
    void deleteExpansion_ValidId_SoftDeletesExpansion() {
        // Arrange
        Expansion expansion = Expansion.builder()
                .id(1L)
                .name("To Delete")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(expansion));

        // Act
        expansionService.deleteExpansion(1L);

        // Assert
        verify(expansionRepository).save(argThat(exp -> exp.getDeletedAt() != null));
    }

    @Test
    void deleteExpansion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expansionService.deleteExpansion(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(expansionRepository, never()).save(any());
    }

    // ==================== RESTORE EXPANSION TESTS ====================

    @Test
    void restoreExpansion_DeletedExpansion_RestoresSuccessfully() {
        // Arrange
        Expansion deletedExpansion = Expansion.builder()
                .id(1L)
                .name("Deleted Expansion")
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findById(1L))
                .thenReturn(Optional.of(deletedExpansion));
        when(expansionRepository.save(any(Expansion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExpansionResponse result = expansionService.restoreExpansion(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(expansionRepository).save(argThat(exp -> exp.getDeletedAt() == null));
    }

    @Test
    void restoreExpansion_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion activeExpansion = Expansion.builder()
                .id(1L)
                .name("Active Expansion")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findById(1L))
                .thenReturn(Optional.of(activeExpansion));

        // Act & Assert
        assertThatThrownBy(() -> expansionService.restoreExpansion(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expansion with id 1 is not deleted");

        verify(expansionRepository, never()).save(any());
    }

    @Test
    void restoreExpansion_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(expansionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expansionService.restoreExpansion(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");
    }
}
