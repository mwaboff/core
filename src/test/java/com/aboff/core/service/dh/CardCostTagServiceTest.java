package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateCardCostTagRequest;
import com.aboff.core.model.dto.dh.request.UpdateCardCostTagRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.repository.dh.CardCostTagRepository;
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
 * Unit tests for CardCostTagService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, and category filtering.
 */
@ExtendWith(MockitoExtension.class)
class CardCostTagServiceTest {

    @Mock
    private CardCostTagRepository cardCostTagRepository;

    @InjectMocks
    private CardCostTagService cardCostTagService;

    // ==================== GET ALL COST TAGS TESTS ====================

    @Test
    void getAllCostTags_WithoutFilters_ReturnsPagedTags() {
        // Arrange
        CardCostTag tag1 = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        CardCostTag tag2 = CardCostTag.builder()
                .id(2L)
                .label("1/session")
                .category(CostTagCategory.TIMING)
                .createdAt(LocalDateTime.now())
                .build();

        Page<CardCostTag> tagPage = new PageImpl<>(List.of(tag1, tag2));
        when(cardCostTagRepository.findByDeletedAtIsNullAndFilters(isNull(), any(Pageable.class)))
                .thenReturn(tagPage);

        // Act
        PagedResponse<CardCostTagResponse> result = cardCostTagService.getAllCostTags(0, 20, false, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isZero();
        assertThat(result.getContent().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(result.getContent().get(1).getLabel()).isEqualTo("1/session");
    }

    @Test
    void getAllCostTags_WithCategoryFilter_ReturnsFilteredTags() {
        // Arrange
        CardCostTag tag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        Page<CardCostTag> tagPage = new PageImpl<>(List.of(tag));
        when(cardCostTagRepository.findByDeletedAtIsNullAndFilters(eq(CostTagCategory.COST), any(Pageable.class)))
                .thenReturn(tagPage);

        // Act
        PagedResponse<CardCostTagResponse> result = cardCostTagService.getAllCostTags(0, 20, false, CostTagCategory.COST);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
        verify(cardCostTagRepository).findByDeletedAtIsNullAndFilters(eq(CostTagCategory.COST), any(Pageable.class));
    }

    @Test
    void getAllCostTags_WithIncludeDeleted_ReturnsAllTags() {
        // Arrange
        CardCostTag tag = CardCostTag.builder()
                .id(1L)
                .label("Deleted Tag")
                .category(CostTagCategory.COST)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<CardCostTag> tagPage = new PageImpl<>(List.of(tag));
        when(cardCostTagRepository.findAllWithFilters(isNull(), any(Pageable.class)))
                .thenReturn(tagPage);

        // Act
        PagedResponse<CardCostTagResponse> result = cardCostTagService.getAllCostTags(0, 20, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(cardCostTagRepository).findAllWithFilters(isNull(), any(Pageable.class));
    }

    @Test
    void getAllCostTags_WithLargePage_LimitsTo100() {
        // Arrange
        Page<CardCostTag> tagPage = new PageImpl<>(List.of());
        when(cardCostTagRepository.findByDeletedAtIsNullAndFilters(isNull(), any(Pageable.class)))
                .thenReturn(tagPage);

        // Act
        cardCostTagService.getAllCostTags(0, 500, false, null);

        // Assert
        verify(cardCostTagRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    // ==================== GET COST TAG BY ID TESTS ====================

    @Test
    void getCostTagById_ValidId_ReturnsTag() {
        // Arrange
        CardCostTag tag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(tag));

        // Act
        CardCostTagResponse result = cardCostTagService.getCostTagById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLabel()).isEqualTo("3 Hope");
        assertThat(result.getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getCostTagById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardCostTagService.getCostTagById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CardCostTag not found with id: 999");
    }

    // ==================== CREATE COST TAG TESTS ====================

    @Test
    void createCostTag_ValidRequest_CreatesAndReturnsTag() {
        // Arrange
        CreateCardCostTagRequest request = CreateCardCostTagRequest.builder()
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .build();

        CardCostTag savedTag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardCostTagRepository.save(any(CardCostTag.class)))
                .thenReturn(savedTag);

        // Act
        CardCostTagResponse result = cardCostTagService.createCostTag(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLabel()).isEqualTo("3 Hope");
        assertThat(result.getCategory()).isEqualTo(CostTagCategory.COST);

        verify(cardCostTagRepository).save(argThat(tag ->
                tag.getLabel().equals("3 Hope") &&
                        tag.getCategory().equals(CostTagCategory.COST)
        ));
    }

    // ==================== UPDATE COST TAG TESTS ====================

    @Test
    void updateCostTag_ValidRequest_UpdatesAndReturnsTag() {
        // Arrange
        CardCostTag existingTag = CardCostTag.builder()
                .id(1L)
                .label("Old Label")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateCardCostTagRequest request = UpdateCardCostTagRequest.builder()
                .label("Updated Label")
                .category(CostTagCategory.TIMING)
                .build();

        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingTag));
        when(cardCostTagRepository.save(any(CardCostTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardCostTagResponse result = cardCostTagService.updateCostTag(1L, request);

        // Assert
        assertThat(result.getLabel()).isEqualTo("Updated Label");
        assertThat(result.getCategory()).isEqualTo(CostTagCategory.TIMING);

        verify(cardCostTagRepository).save(argThat(tag ->
                tag.getLabel().equals("Updated Label") &&
                        tag.getCategory().equals(CostTagCategory.TIMING)
        ));
    }

    @Test
    void updateCostTag_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateCardCostTagRequest request = UpdateCardCostTagRequest.builder()
                .label("Updated Label")
                .category(CostTagCategory.TIMING)
                .build();

        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardCostTagService.updateCostTag(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CardCostTag not found with id: 999");

        verify(cardCostTagRepository, never()).save(any());
    }

    // ==================== DELETE COST TAG TESTS ====================

    @Test
    void deleteCostTag_ValidId_SoftDeletesTag() {
        // Arrange
        CardCostTag tag = CardCostTag.builder()
                .id(1L)
                .label("To Delete")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(tag));

        // Act
        cardCostTagService.deleteCostTag(1L);

        // Assert
        verify(cardCostTagRepository).save(argThat(t -> t.getDeletedAt() != null));
    }

    @Test
    void deleteCostTag_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(cardCostTagRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardCostTagService.deleteCostTag(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CardCostTag not found with id: 999");

        verify(cardCostTagRepository, never()).save(any());
    }

    // ==================== RESTORE COST TAG TESTS ====================

    @Test
    void restoreCostTag_DeletedTag_RestoresSuccessfully() {
        // Arrange
        CardCostTag deletedTag = CardCostTag.builder()
                .id(1L)
                .label("Deleted Tag")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(cardCostTagRepository.findById(1L))
                .thenReturn(Optional.of(deletedTag));
        when(cardCostTagRepository.save(any(CardCostTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardCostTagResponse result = cardCostTagService.restoreCostTag(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(cardCostTagRepository).save(argThat(t -> t.getDeletedAt() == null));
    }

    @Test
    void restoreCostTag_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        CardCostTag activeTag = CardCostTag.builder()
                .id(1L)
                .label("Active Tag")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        when(cardCostTagRepository.findById(1L))
                .thenReturn(Optional.of(activeTag));

        // Act & Assert
        assertThatThrownBy(() -> cardCostTagService.restoreCostTag(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CardCostTag with id 1 is not deleted");

        verify(cardCostTagRepository, never()).save(any());
    }

    @Test
    void restoreCostTag_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(cardCostTagRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cardCostTagService.restoreCostTag(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CardCostTag not found with id: 999");
    }
}
