package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.dh.response.SubclassPathResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.SubclassCardRepository;
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
 * Unit tests for SubclassCardService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class SubclassCardServiceTest {

    @Mock
    private SubclassCardRepository subclassCardRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private CardCostTagService cardCostTagService;

    @Mock
    private SubclassPathService subclassPathService;

    @InjectMocks
    private SubclassCardService subclassCardService;

    // ==================== GET ALL SUBCLASS CARDS TESTS ====================

    @Test
    void getAllSubclassCards_WithoutFilters_ReturnsPagedCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard card1 = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .createdAt(LocalDateTime.now())
                .build();

        SubclassCard card2 = SubclassCard.builder()
                .id(2L)
                .name("Guardian")
                .description("Defender")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.SPECIALIZATION)
                .backgroundImageUrl("https://img.url/guardian")
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card1, card2));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Berserker");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Guardian");
    }

    @Test
    void getAllSubclassCards_WithLevelFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), eq(SubclassLevel.FOUNDATION), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, SubclassLevel.FOUNDATION, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLevel()).isEqualTo(SubclassLevel.FOUNDATION);
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), eq(SubclassLevel.FOUNDATION), any(Pageable.class));
    }

    @Test
    void getAllSubclassCards_WithAssociatedClassFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, 1L, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSubclassPathId()).isEqualTo(1L);
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllSubclassCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<SubclassCard> cardPage = new PageImpl<>(List.of());
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        subclassCardService.getAllSubclassCards(0, 500, false, null, null, null, null, null, null);

        // Assert
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllSubclassCards_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        SubclassPathResponse pathResponse = SubclassPathResponse.builder()
                .id(1L).name("Warden of Renewal").associatedClassId(1L).expansionId(1L).build();
        when(subclassPathService.toResponse(eq(path), anySet())).thenReturn(pathResponse);
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
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, "expansion,features,subclassPath");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
        assertThat(result.getContent().get(0).getSubclassPath()).isNotNull();
        assertThat(result.getContent().get(0).getSubclassPath().getName()).isEqualTo("Warden of Renewal");
    }

    @Test
    void getAllSubclassCards_WithExpandFeaturesWithoutCostTags_IncludesCostTagIdsOnly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
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
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, "features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatures()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
    }

    @Test
    void getAllSubclassCards_WithExpandFeaturesAndCostTags_IncludesFullCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
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
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, "features,costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatures()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getAllSubclassCards_WithExpandFeaturesNullCostTags_HandlesNullGracefully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion)
                .costTags(null).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
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
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isNull();
        assertThat(featureResponse.getCostTags()).isNull();
    }

    @Test
    void getAllSubclassCards_WithExpandFeaturesEmptyCostTags_ReturnsEmptyLists() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion)
                .costTags(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
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
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isEmpty();
        assertThat(featureResponse.getCostTags()).isEmpty();
    }

    @Test
    void getAllSubclassCards_AlwaysIncludesExpansionNameAndDomainNames() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        Domain domain1 = Domain.builder().id(1L).name("Blade").build();
        Domain domain2 = Domain.builder().id(2L).name("Bone").build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz)
                .expansion(expansion).associatedDomains(Set.of(domain1, domain2)).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, null);

        // Assert
        SubclassCardResponse response = result.getContent().get(0);
        assertThat(response.getExpansionName()).isEqualTo("Core Rulebook");
        assertThat(response.getAssociatedClassId()).isEqualTo(1L);
        assertThat(response.getAssociatedClassName()).isEqualTo("Warrior");
        assertThat(response.getSubclassPathName()).isEqualTo("Warden of Renewal");
        assertThat(response.getDomainNames()).containsExactly("Blade", "Bone");
        assertThat(response.getSpellcastingTrait()).isNull();
    }

    @Test
    void getAllSubclassCards_WithSpellcastingTrait_IncludesTraitInfo() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Wizard").expansion(expansion).startingEvasion(8).startingHitPoints(14).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("School of Arcana").associatedClass(clazz)
                .expansion(expansion).spellcastingTrait(Trait.KNOWLEDGE).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Arcane Scholar")
                .description("A scholar of the arcane")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, null);

        // Assert
        SubclassCardResponse response = result.getContent().get(0);
        assertThat(response.getSpellcastingTrait()).isNotNull();
        assertThat(response.getSpellcastingTrait().getTrait()).isEqualTo(Trait.KNOWLEDGE);
        assertThat(response.getSpellcastingTrait().getDescription()).isEqualTo(Trait.KNOWLEDGE.getDescription());
        assertThat(response.getSpellcastingTrait().getExamples()).isEqualTo(Trait.KNOWLEDGE.getExamples());
    }

    @Test
    void getAllSubclassCards_NullAssociatedDomains_ReturnEmptyDomainNames() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz)
                .expansion(expansion).associatedDomains(null).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null, null);

        // Assert
        assertThat(result.getContent().get(0).getDomainNames()).isEmpty();
    }

    // ==================== GET SUBCLASS CARD BY ID TESTS ====================

    @Test
    void getSubclassCardById_ValidId_ReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(card));

        // Act
        SubclassCardResponse result = subclassCardService.getSubclassCardById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Berserker");
        assertThat(result.getLevel()).isEqualTo(SubclassLevel.FOUNDATION);
    }

    @Test
    void getSubclassCardById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassCardRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.getSubclassCardById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassCard not found with id: 999");
    }

    // ==================== CREATE SUBCLASS CARD TESTS ====================

    @Test
    void createSubclassCard_ValidRequest_CreatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).build();

        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .subclassPathId(1L)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .featureIds(List.of(1L))
                .build();

        SubclassCard savedCard = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathService.resolvePath(eq(1L), isNull(), isNull(), eq(1L))).thenReturn(path);
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenReturn(savedCard);

        // Act
        SubclassCardResponse result = subclassCardService.createSubclassCard(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Berserker");
        assertThat(result.getSubclassPathId()).isEqualTo(1L);
        verify(subclassCardRepository).save(any(SubclassCard.class));
    }

    // ==================== CREATE SUBCLASS CARDS BULK TESTS ====================

    @Test
    void createSubclassCardsBulk_ValidRequests_CreatesAndReturnsCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        CreateSubclassCardRequest request1 = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .subclassPathId(1L)
                .level(SubclassLevel.FOUNDATION)
                .build();

        CreateSubclassCardRequest request2 = CreateSubclassCardRequest.builder()
                .name("Guardian")
                .description("Defender")
                .expansionId(1L)
                .isOfficial(true)
                .subclassPathId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .build();

        SubclassCard savedCard1 = SubclassCard.builder().id(1L).name("Berserker").description("Rage fighter")
                .expansion(expansion).isOfficial(true).subclassPath(path).level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now()).build();

        SubclassCard savedCard2 = SubclassCard.builder().id(2L).name("Guardian").description("Defender")
                .expansion(expansion).isOfficial(true).subclassPath(path).level(SubclassLevel.SPECIALIZATION)
                .createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathService.resolvePath(eq(1L), isNull(), isNull(), eq(1L))).thenReturn(path);
        when(subclassCardRepository.saveAll(anyList())).thenReturn(List.of(savedCard1, savedCard2));

        // Act
        List<SubclassCardResponse> results = subclassCardService.createSubclassCardsBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Berserker");
        assertThat(results.get(1).getName()).isEqualTo("Guardian");
        verify(subclassCardRepository).saveAll(anyList());
    }

    // ==================== UPDATE SUBCLASS CARD TESTS ====================

    @Test
    void updateSubclassCard_ValidRequest_UpdatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard existingCard = SubclassCard.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .isOfficial(false)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .features(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .subclassPathId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .backgroundImageUrl("https://img.url/updated")
                .featureIds(List.of())
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathService.resolvePath(eq(1L), isNull(), isNull(), eq(1L))).thenReturn(path);
        when(featureService.resolveFeatures(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(subclassCardRepository.save(any(SubclassCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassCardResponse result = subclassCardService.updateSubclassCard(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getLevel()).isEqualTo(SubclassLevel.SPECIALIZATION);
        verify(subclassCardRepository).save(any(SubclassCard.class));
    }

    @Test
    void updateSubclassCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .subclassPathId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.updateSubclassCard(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassCard not found with id: 999");

        verify(subclassCardRepository, never()).save(any());
    }

    // ==================== DELETE SUBCLASS CARD TESTS ====================

    @Test
    void deleteSubclassCard_ValidId_SoftDeletesCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        // Act
        subclassCardService.deleteSubclassCard(1L);

        // Assert
        verify(subclassCardRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteSubclassCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.deleteSubclassCard(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassCard not found with id: 999");

        verify(subclassCardRepository, never()).save(any());
    }

    // ==================== RESTORE SUBCLASS CARD TESTS ====================

    @Test
    void restoreSubclassCard_DeletedCard_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard deletedCard = SubclassCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(subclassCardRepository.findById(1L)).thenReturn(Optional.of(deletedCard));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassCardResponse result = subclassCardService.restoreSubclassCard(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(subclassCardRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreSubclassCard_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();
        SubclassPath path = SubclassPath.builder().id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion).build();

        SubclassCard activeCard = SubclassCard.builder()
                .id(1L)
                .name("Active Card")
                .description("Active")
                .expansion(expansion)
                .isOfficial(true)
                .subclassPath(path)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassCardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.restoreSubclassCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SubclassCard with id 1 is not deleted");

        verify(subclassCardRepository, never()).save(any());
    }

    @Test
    void restoreSubclassCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.restoreSubclassCard(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassCard not found with id: 999");
    }
}
