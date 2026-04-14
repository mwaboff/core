package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateCommunityCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateCommunityCardRequest;
import com.aboff.core.model.dto.dh.response.CommunityCardResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.CommunityCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.CommunityCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommunityCardService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class CommunityCardServiceTest {

    @Mock
    private CommunityCardRepository communityCardRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private CardCostTagService cardCostTagService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommunityCardService communityCardService;

    // ==================== GET ALL ANCESTRY CARDS TESTS ====================

    @Test
    void getAllCommunityCards_WithoutFilters_ReturnsPagedCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card1 = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        CommunityCard card2 = CommunityCard.builder()
                .id(2L)
                .name("Trading")
                .description("Mercantile community")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/elf")
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card1, card2));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Farming");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Trading");
    }

    @Test
    void getAllCommunityCards_WithExpansionFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(communityCardRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllCommunityCards_WithIsOfficialFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
        verify(communityCardRepository).findByDeletedAtIsNullAndFilters(isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void getAllCommunityCards_WithIncludeDeleted_ReturnsAllCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, true, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(communityCardRepository).findAllWithFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllCommunityCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<CommunityCard> cardPage = new PageImpl<>(List.of());
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        communityCardService.getAllCommunityCards(0, 500, false, null, null, null);

        // Assert
        verify(communityCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllCommunityCards_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion)
                .costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            Set<String> exp = invocation.getArgument(1);
            FeatureResponse.FeatureResponseBuilder fb = FeatureResponse.builder()
                    .id(f.getId()).name(f.getName()).description(f.getDescription())
                    .featureType(f.getFeatureType()).expansionId(f.getExpansion().getId())
                    .createdAt(f.getCreatedAt()).lastModifiedAt(f.getLastModifiedAt()).deletedAt(f.getDeletedAt());
            if (f.getCostTags() != null) {
                fb.costTagIds(f.getCostTags().stream().map(CardCostTag::getId).collect(Collectors.toList()));
            }
            if (exp.contains("costTags") && f.getCostTags() != null) {
                fb.costTags(f.getCostTags().stream().map(tag -> CardCostTagResponse.builder()
                        .id(tag.getId()).label(tag.getLabel()).category(tag.getCategory())
                        .createdAt(tag.getCreatedAt()).lastModifiedAt(tag.getLastModifiedAt()).deletedAt(tag.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            if (f.getModifiers() != null) {
                fb.modifierIds(f.getModifiers().stream().map(FeatureModifier::getId).collect(Collectors.toList()));
            }
            if (exp.contains("modifiers") && f.getModifiers() != null) {
                fb.modifiers(f.getModifiers().stream().map(mod -> FeatureModifierResponse.builder()
                        .id(mod.getId()).target(mod.getTarget()).operation(mod.getOperation()).value(mod.getValue())
                        .createdAt(mod.getCreatedAt()).lastModifiedAt(mod.getLastModifiedAt()).deletedAt(mod.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            return fb.build();
        });

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, "expansion,features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
    }

    @Test
    void getAllCommunityCards_WithExpandFeaturesAndCostTags_IncludesFullCostTagObjects() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion)
                .costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            Set<String> exp = invocation.getArgument(1);
            FeatureResponse.FeatureResponseBuilder fb = FeatureResponse.builder()
                    .id(f.getId()).name(f.getName()).description(f.getDescription())
                    .featureType(f.getFeatureType()).expansionId(f.getExpansion().getId())
                    .createdAt(f.getCreatedAt()).lastModifiedAt(f.getLastModifiedAt()).deletedAt(f.getDeletedAt());
            if (f.getCostTags() != null) {
                fb.costTagIds(f.getCostTags().stream().map(CardCostTag::getId).collect(Collectors.toList()));
            }
            if (exp.contains("costTags") && f.getCostTags() != null) {
                fb.costTags(f.getCostTags().stream().map(tag -> CardCostTagResponse.builder()
                        .id(tag.getId()).label(tag.getLabel()).category(tag.getCategory())
                        .createdAt(tag.getCreatedAt()).lastModifiedAt(tag.getLastModifiedAt()).deletedAt(tag.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            if (f.getModifiers() != null) {
                fb.modifierIds(f.getModifiers().stream().map(FeatureModifier::getId).collect(Collectors.toList()));
            }
            if (exp.contains("modifiers") && f.getModifiers() != null) {
                fb.modifiers(f.getModifiers().stream().map(mod -> FeatureModifierResponse.builder()
                        .id(mod.getId()).target(mod.getTarget()).operation(mod.getOperation()).value(mod.getValue())
                        .createdAt(mod.getCreatedAt()).lastModifiedAt(mod.getLastModifiedAt()).deletedAt(mod.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            return fb.build();
        });

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, "features,costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatures()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNotNull().hasSize(1);
        CardCostTagResponse tagResponse = result.getContent().get(0).getFeatures().get(0).getCostTags().get(0);
        assertThat(tagResponse.getId()).isEqualTo(10L);
        assertThat(tagResponse.getLabel()).isEqualTo("3 Hope");
        assertThat(tagResponse.getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getAllCommunityCards_WithExpandFeaturesWithoutCostTags_IncludesCostTagIdsOnly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion)
                .costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            Set<String> exp = invocation.getArgument(1);
            FeatureResponse.FeatureResponseBuilder fb = FeatureResponse.builder()
                    .id(f.getId()).name(f.getName()).description(f.getDescription())
                    .featureType(f.getFeatureType()).expansionId(f.getExpansion().getId())
                    .createdAt(f.getCreatedAt()).lastModifiedAt(f.getLastModifiedAt()).deletedAt(f.getDeletedAt());
            if (f.getCostTags() != null) {
                fb.costTagIds(f.getCostTags().stream().map(CardCostTag::getId).collect(Collectors.toList()));
            }
            if (exp.contains("costTags") && f.getCostTags() != null) {
                fb.costTags(f.getCostTags().stream().map(tag -> CardCostTagResponse.builder()
                        .id(tag.getId()).label(tag.getLabel()).category(tag.getCategory())
                        .createdAt(tag.getCreatedAt()).lastModifiedAt(tag.getLastModifiedAt()).deletedAt(tag.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            if (f.getModifiers() != null) {
                fb.modifierIds(f.getModifiers().stream().map(FeatureModifier::getId).collect(Collectors.toList()));
            }
            if (exp.contains("modifiers") && f.getModifiers() != null) {
                fb.modifiers(f.getModifiers().stream().map(mod -> FeatureModifierResponse.builder()
                        .id(mod.getId()).target(mod.getTarget()).operation(mod.getOperation()).value(mod.getValue())
                        .createdAt(mod.getCreatedAt()).lastModifiedAt(mod.getLastModifiedAt()).deletedAt(mod.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            return fb.build();
        });

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, "features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatures()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
    }

    @Test
    void getAllCommunityCards_WithExpandFeaturesNullCostTags_HandlesNullGracefully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion)
                .costTags(null).createdAt(LocalDateTime.now()).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            Set<String> exp = invocation.getArgument(1);
            FeatureResponse.FeatureResponseBuilder fb = FeatureResponse.builder()
                    .id(f.getId()).name(f.getName()).description(f.getDescription())
                    .featureType(f.getFeatureType()).expansionId(f.getExpansion().getId())
                    .createdAt(f.getCreatedAt()).lastModifiedAt(f.getLastModifiedAt()).deletedAt(f.getDeletedAt());
            if (f.getCostTags() != null) {
                fb.costTagIds(f.getCostTags().stream().map(CardCostTag::getId).collect(Collectors.toList()));
            }
            if (exp.contains("costTags") && f.getCostTags() != null) {
                fb.costTags(f.getCostTags().stream().map(tag -> CardCostTagResponse.builder()
                        .id(tag.getId()).label(tag.getLabel()).category(tag.getCategory())
                        .createdAt(tag.getCreatedAt()).lastModifiedAt(tag.getLastModifiedAt()).deletedAt(tag.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            if (f.getModifiers() != null) {
                fb.modifierIds(f.getModifiers().stream().map(FeatureModifier::getId).collect(Collectors.toList()));
            }
            if (exp.contains("modifiers") && f.getModifiers() != null) {
                fb.modifiers(f.getModifiers().stream().map(mod -> FeatureModifierResponse.builder()
                        .id(mod.getId()).target(mod.getTarget()).operation(mod.getOperation()).value(mod.getValue())
                        .createdAt(mod.getCreatedAt()).lastModifiedAt(mod.getLastModifiedAt()).deletedAt(mod.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            return fb.build();
        });

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isNull();
        assertThat(featureResponse.getCostTags()).isNull();
    }

    @Test
    void getAllCommunityCards_WithExpandFeaturesEmptyCostTags_ReturnsEmptyLists() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion)
                .costTags(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<CommunityCard> cardPage = new PageImpl<>(List.of(card));
        when(communityCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            Set<String> exp = invocation.getArgument(1);
            FeatureResponse.FeatureResponseBuilder fb = FeatureResponse.builder()
                    .id(f.getId()).name(f.getName()).description(f.getDescription())
                    .featureType(f.getFeatureType()).expansionId(f.getExpansion().getId())
                    .createdAt(f.getCreatedAt()).lastModifiedAt(f.getLastModifiedAt()).deletedAt(f.getDeletedAt());
            if (f.getCostTags() != null) {
                fb.costTagIds(f.getCostTags().stream().map(CardCostTag::getId).collect(Collectors.toList()));
            }
            if (exp.contains("costTags") && f.getCostTags() != null) {
                fb.costTags(f.getCostTags().stream().map(tag -> CardCostTagResponse.builder()
                        .id(tag.getId()).label(tag.getLabel()).category(tag.getCategory())
                        .createdAt(tag.getCreatedAt()).lastModifiedAt(tag.getLastModifiedAt()).deletedAt(tag.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            if (f.getModifiers() != null) {
                fb.modifierIds(f.getModifiers().stream().map(FeatureModifier::getId).collect(Collectors.toList()));
            }
            if (exp.contains("modifiers") && f.getModifiers() != null) {
                fb.modifiers(f.getModifiers().stream().map(mod -> FeatureModifierResponse.builder()
                        .id(mod.getId()).target(mod.getTarget()).operation(mod.getOperation()).value(mod.getValue())
                        .createdAt(mod.getCreatedAt()).lastModifiedAt(mod.getLastModifiedAt()).deletedAt(mod.getDeletedAt())
                        .build()).collect(Collectors.toList()));
            }
            return fb.build();
        });

        // Act
        PagedResponse<CommunityCardResponse> result = communityCardService.getAllCommunityCards(0, 20, false, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isEmpty();
        assertThat(featureResponse.getCostTags()).isEmpty();
    }

    // ==================== GET COMMUNITY CARD BY ID TESTS ====================

    @Test
    void getCommunityCardById_ValidId_ReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        when(communityCardRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(card));

        // Act
        CommunityCardResponse result = communityCardService.getCommunityCardById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Farming");
        assertThat(result.getDescription()).isEqualTo("Agricultural community");
        assertThat(result.getIsOfficial()).isTrue();
    }

    @Test
    void getCommunityCardById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(communityCardRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.getCommunityCardById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommunityCard not found with id: 999");
    }

    // ==================== CREATE ANCESTRY CARD TESTS ====================

    @Test
    void createCommunityCard_ValidRequest_CreatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.COMMUNITY).expansion(expansion).build();

        CreateCommunityCardRequest request = CreateCommunityCardRequest.builder()
                .name("Farming")
                .description("Agricultural community")
                .expansionId(1L)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .featureIds(List.of(1L))
                .build();

        CommunityCard savedCard = CommunityCard.builder()
                .id(1L)
                .name("Farming")
                .description("Agricultural community")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(communityCardRepository.save(any(CommunityCard.class))).thenReturn(savedCard);

        // Act
        CommunityCardResponse result = communityCardService.createCommunityCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Farming");
        verify(communityCardRepository).save(any(CommunityCard.class));
    }

    @Test
    void createCommunityCard_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateCommunityCardRequest request = CreateCommunityCardRequest.builder()
                .name("Farming")
                .description("Agricultural community")
                .expansionId(999L)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.createCommunityCard(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(communityCardRepository, never()).save(any());
    }

    // ==================== CREATE ANCESTRY CARDS BULK TESTS ====================

    @Test
    void createCommunityCardsBulk_ValidRequests_CreatesAndReturnsCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateCommunityCardRequest request1 = CreateCommunityCardRequest.builder()
                .name("Farming")
                .description("Agricultural community")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        CreateCommunityCardRequest request2 = CreateCommunityCardRequest.builder()
                .name("Trading")
                .description("Mercantile community")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        CommunityCard savedCard1 = CommunityCard.builder().id(1L).name("Farming").description("Agricultural community")
                .expansion(expansion).isOfficial(true).createdAt(LocalDateTime.now()).build();

        CommunityCard savedCard2 = CommunityCard.builder().id(2L).name("Trading").description("Mercantile community")
                .expansion(expansion).isOfficial(true).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(communityCardRepository.saveAll(anyList())).thenReturn(List.of(savedCard1, savedCard2));

        // Act
        List<CommunityCardResponse> results = communityCardService.createCommunityCardsBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Farming");
        assertThat(results.get(1).getName()).isEqualTo("Trading");
        verify(communityCardRepository).saveAll(anyList());
    }

    // ==================== UPDATE ANCESTRY CARD TESTS ====================

    @Test
    void updateCommunityCard_ValidRequest_UpdatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard existingCard = CommunityCard.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .isOfficial(false)
                .features(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateCommunityCardRequest request = UpdateCommunityCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/updated")
                .featureIds(List.of())
                .build();

        when(communityCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(communityCardRepository.save(any(CommunityCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CommunityCardResponse result = communityCardService.updateCommunityCard(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getIsOfficial()).isTrue();
        verify(communityCardRepository).save(any(CommunityCard.class));
    }

    @Test
    void updateCommunityCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateCommunityCardRequest request = UpdateCommunityCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        when(communityCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.updateCommunityCard(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommunityCard not found with id: 999");

        verify(communityCardRepository, never()).save(any());
    }

    // ==================== DELETE ANCESTRY CARD TESTS ====================

    @Test
    void deleteCommunityCard_ValidId_SoftDeletesCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard card = CommunityCard.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(communityCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        // Act
        communityCardService.deleteCommunityCard(1L, authentication);

        // Assert
        verify(communityCardRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteCommunityCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(communityCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.deleteCommunityCard(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommunityCard not found with id: 999");

        verify(communityCardRepository, never()).save(any());
    }

    // ==================== RESTORE ANCESTRY CARD TESTS ====================

    @Test
    void restoreCommunityCard_DeletedCard_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard deletedCard = CommunityCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(communityCardRepository.findById(1L)).thenReturn(Optional.of(deletedCard));
        when(communityCardRepository.save(any(CommunityCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CommunityCardResponse result = communityCardService.restoreCommunityCard(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(communityCardRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreCommunityCard_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CommunityCard activeCard = CommunityCard.builder()
                .id(1L)
                .name("Active Card")
                .description("Active")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(communityCardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.restoreCommunityCard(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CommunityCard with id 1 is not deleted");

        verify(communityCardRepository, never()).save(any());
    }

    @Test
    void restoreCommunityCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(communityCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> communityCardService.restoreCommunityCard(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommunityCard not found with id: 999");
    }
}
