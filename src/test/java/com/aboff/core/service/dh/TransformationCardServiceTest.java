package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateTransformationCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateTransformationCardRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.TransformationCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.TransformationCardRepository;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
 * Unit tests for TransformationCardService.
 * Tests CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class TransformationCardServiceTest {

    @Mock
    private TransformationCardRepository transformationCardRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransformationCardService transformationCardService;

    // ==================== GET ALL TESTS ====================

    @Test
    void getAllTransformationCards_WithoutFilters_ReturnsPagedCards() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();

        TransformationCard card1 = TransformationCard.builder()
                .id(1L).name("Feral Transformation").description("Becomes a beast")
                .expansion(expansion).createdAt(LocalDateTime.now()).build();
        TransformationCard card2 = TransformationCard.builder()
                .id(2L).name("Elemental Transformation").description("Becomes an element")
                .expansion(expansion).createdAt(LocalDateTime.now()).build();

        Page<TransformationCard> page = new PageImpl<>(List.of(card1, card2));
        when(transformationCardRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<TransformationCardResponse> result =
                transformationCardService.getAllTransformationCards(0, 20, false, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Feral Transformation");
    }

    @Test
    void getAllTransformationCards_IncludeDeleted_UsesFindAllWithExpansion() {
        Page<TransformationCard> page = new PageImpl<>(List.of());
        when(transformationCardRepository.findAllWithExpansion(isNull(), any(Pageable.class)))
                .thenReturn(page);

        transformationCardService.getAllTransformationCards(0, 20, true, null, null);

        verify(transformationCardRepository).findAllWithExpansion(isNull(), any(Pageable.class));
        verify(transformationCardRepository, never()).findByDeletedAtIsNullAndExpansion(any(), any());
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void getTransformationCardById_Existing_ReturnsCard() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        TransformationCard card = TransformationCard.builder()
                .id(1L).name("Feral Transformation").expansion(expansion).build();
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        TransformationCardResponse response = transformationCardService.getTransformationCardById(1L, null);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Feral Transformation");
    }

    @Test
    void getTransformationCardById_NotFound_ThrowsException() {
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transformationCardService.getTransformationCardById(999L, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createTransformationCard_WithValidData_PersistsAndReturnsResponse() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));

        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .description("Becomes a beast")
                .expansionId(1L)
                .build();

        TransformationCard saved = TransformationCard.builder()
                .id(10L).name("Feral Transformation").description("Becomes a beast")
                .expansion(expansion).build();
        when(transformationCardRepository.save(any(TransformationCard.class))).thenReturn(saved);

        TransformationCardResponse response =
                transformationCardService.createTransformationCard(request, authentication);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Feral Transformation");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createTransformationCard_ExpansionNotFound_ThrowsException() {
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        CreateTransformationCardRequest request = CreateTransformationCardRequest.builder()
                .name("Feral Transformation")
                .expansionId(999L)
                .build();

        assertThatThrownBy(() -> transformationCardService.createTransformationCard(request, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== CREATE BULK TESTS ====================

    @Test
    void createTransformationCardsBulk_WithValidData_PersistsAll() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));

        CreateTransformationCardRequest request1 = CreateTransformationCardRequest.builder()
                .name("Feral Transformation").expansionId(1L).build();
        CreateTransformationCardRequest request2 = CreateTransformationCardRequest.builder()
                .name("Elemental Transformation").expansionId(1L).build();

        TransformationCard saved1 = TransformationCard.builder().id(1L).name("Feral Transformation").expansion(expansion).build();
        TransformationCard saved2 = TransformationCard.builder().id(2L).name("Elemental Transformation").expansion(expansion).build();
        when(transformationCardRepository.saveAll(anyList())).thenReturn(List.of(saved1, saved2));

        List<TransformationCardResponse> responses = transformationCardService.createTransformationCardsBulk(
                List.of(request1, request2), authentication);

        assertThat(responses).hasSize(2);
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateTransformationCard_WithPartialFields_UpdatesOnlyProvidedFields() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        TransformationCard existing = TransformationCard.builder()
                .id(1L).name("Feral Transformation").description("Original").expansion(expansion).build();
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(transformationCardRepository.save(any(TransformationCard.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTransformationCardRequest request = UpdateTransformationCardRequest.builder()
                .description("Updated description")
                .build();

        TransformationCardResponse response =
                transformationCardService.updateTransformationCard(1L, request, authentication);

        assertThat(response.getName()).isEqualTo("Feral Transformation");
        assertThat(response.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void updateTransformationCard_NotFound_ThrowsException() {
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        UpdateTransformationCardRequest request = UpdateTransformationCardRequest.builder().name("X").build();

        assertThatThrownBy(() -> transformationCardService.updateTransformationCard(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== DELETE / RESTORE TESTS ====================

    @Test
    void deleteTransformationCard_Existing_SoftDeletes() {
        TransformationCard card = TransformationCard.builder().id(1L).name("Feral Transformation").build();
        when(transformationCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        transformationCardService.deleteTransformationCard(1L, authentication);

        assertThat(card.getDeletedAt()).isNotNull();
        verify(transformationCardRepository).save(card);
    }

    @Test
    void restoreTransformationCard_Deleted_ClearsDeletedAt() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        TransformationCard card = TransformationCard.builder().id(1L).name("Feral Transformation").expansion(expansion).build();
        card.softDelete();
        when(transformationCardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(transformationCardRepository.save(any(TransformationCard.class))).thenReturn(card);

        transformationCardService.restoreTransformationCard(1L, authentication);

        assertThat(card.getDeletedAt()).isNull();
    }

    @Test
    void restoreTransformationCard_NotDeleted_ThrowsIllegalStateException() {
        TransformationCard card = TransformationCard.builder().id(1L).name("Feral Transformation").build();
        when(transformationCardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> transformationCardService.restoreTransformationCard(1L, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== EXPAND TESTS ====================

    @Test
    void toResponse_ExpandFeatures_IncludesFullFeatureObjects() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Feature feature = Feature.builder().id(5L).name("Bestial Fury").build();
        TransformationCard card = TransformationCard.builder()
                .id(1L).name("Feral Transformation").expansion(expansion)
                .features(Set.of(feature)).build();

        FeatureResponse featureResponse = FeatureResponse.builder().id(5L).name("Bestial Fury").build();
        when(featureService.toResponse(eq(feature), eq(Set.of()))).thenReturn(featureResponse);

        TransformationCardResponse response = transformationCardService.toResponse(card, Set.of("features"));

        assertThat(response.getFeatureIds()).containsExactly(5L);
        assertThat(response.getFeatures()).hasSize(1);
        assertThat(response.getFeatures().get(0).getName()).isEqualTo("Bestial Fury");
    }

    @Test
    void toResponse_WithoutExpand_OnlyIncludesFeatureIds() {
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Feature feature = Feature.builder().id(5L).name("Bestial Fury").build();
        TransformationCard card = TransformationCard.builder()
                .id(1L).name("Feral Transformation").expansion(expansion)
                .features(Set.of(feature)).build();

        TransformationCardResponse response = transformationCardService.toResponse(card, Set.of());

        assertThat(response.getFeatureIds()).containsExactly(5L);
        assertThat(response.getFeatures()).isNull();
    }
}
