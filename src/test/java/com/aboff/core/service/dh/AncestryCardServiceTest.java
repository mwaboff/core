package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.CreateMixedAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.dto.dh.response.AncestryCardResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Card;
import com.aboff.core.model.dto.dh.request.CostTagInput;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.AncestryCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
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
 * Unit tests for AncestryCardService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class AncestryCardServiceTest {

    @Mock
    private AncestryCardRepository ancestryCardRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureRepository featureRepository;

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

    @Mock
    private ContentAccessService contentAccessService;

    @InjectMocks
    private AncestryCardService ancestryCardService;

    /**
     * Defaults every test to "caller may see everything" so pre-existing assertions on full
     * card content keep passing without each test having to stub SRD gating explicitly. Tests
     * exercising gating override these with their own stubbing.
     */
    @BeforeEach
    void setUpContentAccess() {
        lenient().when(contentAccessService.mayView(any(Card.class))).thenReturn(true);
        lenient().when(contentAccessService.includeNonSrd()).thenReturn(true);
        lenient().when(contentAccessService.resolveIncludeDeleted(anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(contentAccessService.resolveSrd(any(), any())).thenAnswer(invocation -> Boolean.TRUE.equals(invocation.getArgument(1)));
        lenient().when(authentication.getPrincipal())
                .thenReturn(new CustomUserDetails(User.builder().id(1L).username("tester").role(Role.ADMIN).build()));
    }

    // ==================== GET ALL ANCESTRY CARDS TESTS ====================

    @Test
    void getAllAncestryCards_WithoutFilters_ReturnsPagedCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card1 = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        AncestryCard card2 = AncestryCard.builder()
                .id(2L)
                .name("Elf")
                .description("Ancient ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/elf")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card1, card2));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Human");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Elf");
    }

    @Test
    void getAllAncestryCards_WithExpansionFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, 1L, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), eq(false), eq(true), any(Pageable.class));
    }

    @Test
    void getAllAncestryCards_WithIsOfficialFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(true), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, true, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(isNull(), eq(true), eq(false), eq(true), any(Pageable.class));
    }

    @Test
    void getAllAncestryCards_WithIncludeDeleted_ReturnsAllCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findAllWithFilters(isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, true, null, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(ancestryCardRepository).findAllWithFilters(isNull(), isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void getAllAncestryCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<AncestryCard> cardPage = new PageImpl<>(List.of());
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        ancestryCardService.getAllAncestryCards(0, 500, false, null, null, null, null);

        // Assert
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                eq(false), eq(true),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllAncestryCards_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag featureCostTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion)
                .costTags(Set.of(featureCostTag)).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
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
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "expansion,features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
    }

    @Test
    void getAllAncestryCards_WithExpandFeaturesOnly_IncludesCostTagIdsButNotCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag featureCostTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion)
                .costTags(Set.of(featureCostTag)).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
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
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "features");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).containsExactly(10L);
        assertThat(featureResponse.getCostTags()).isNull();
    }

    @Test
    void getAllAncestryCards_WithExpandFeaturesAndCostTags_IncludesFullCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag featureCostTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion)
                .costTags(Set.of(featureCostTag)).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
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
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).containsExactly(10L);
        assertThat(featureResponse.getCostTags()).isNotNull();
        assertThat(featureResponse.getCostTags()).hasSize(1);
        assertThat(featureResponse.getCostTags().get(0).getId()).isEqualTo(10L);
        assertThat(featureResponse.getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(featureResponse.getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getAllAncestryCards_WithExpandFeaturesNullCostTags_HandlesNullGracefully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion)
                .costTags(null).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
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
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isNull();
        assertThat(featureResponse.getCostTags()).isNull();
    }

    @Test
    void getAllAncestryCards_WithExpandFeaturesEmptyCostTags_ReturnsEmptyLists() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion)
                .costTags(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
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
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "features,costTags");

        // Assert
        FeatureResponse featureResponse = result.getContent().get(0).getFeatures().get(0);
        assertThat(featureResponse.getCostTagIds()).isEmpty();
        assertThat(featureResponse.getCostTags()).isEmpty();
    }

    // ==================== GET ANCESTRY CARD BY ID TESTS ====================

    @Test
    void getAncestryCardById_ValidId_ReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(card));

        // Act
        AncestryCardResponse result = ancestryCardService.getAncestryCardById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Human");
        assertThat(result.getDescription()).isEqualTo("Versatile ancestry");
        assertThat(result.getIsOfficial()).isTrue();
    }

    @Test
    void getAncestryCardById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.getAncestryCardById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("AncestryCard not found with id: 999");
    }

    // ==================== CREATE ANCESTRY CARD TESTS ====================

    @Test
    void createAncestryCard_ValidRequest_CreatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .featureIds(List.of(1L))
                .build();

        AncestryCard savedCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/human")
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenReturn(savedCard);

        // Act
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Human");
        verify(ancestryCardRepository).save(any(AncestryCard.class));
    }

    @Test
    void createAncestryCard_WithCostTagIds_SetsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();

        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .costTagIds(List.of(1L))
                .build();

        AncestryCard savedCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), isNull())).thenReturn(Set.of(costTag));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenReturn(savedCard);

        // Act
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), isNull());
    }

    @Test
    void createAncestryCard_WithCostTagInputs_ResolvesAndSetsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();
        List<CostTagInput> costTagInputs = List.of(
                CostTagInput.builder().label("3 Hope").category(CostTagCategory.COST).build()
        );

        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .costTags(costTagInputs)
                .build();

        AncestryCard savedCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(isNull(), eq(costTagInputs))).thenReturn(Set.of(costTag));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenReturn(savedCard);

        // Act
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactly(1L);
        verify(cardCostTagService).resolveCostTags(isNull(), eq(costTagInputs));
    }

    @Test
    void createAncestryCard_WithBothCostTagIdsAndInputs_MergesBoth() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CardCostTag costTag1 = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).build();
        CardCostTag costTag2 = CardCostTag.builder().id(2L).label("1/session").category(CostTagCategory.TIMING).build();
        List<CostTagInput> costTagInputs = List.of(
                CostTagInput.builder().label("1/session").category(CostTagCategory.TIMING).build()
        );

        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .costTagIds(List.of(1L))
                .costTags(costTagInputs)
                .build();

        AncestryCard savedCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .costTags(Set.of(costTag1, costTag2))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(cardCostTagService.resolveCostTags(eq(List.of(1L)), eq(costTagInputs))).thenReturn(Set.of(costTag1, costTag2));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenReturn(savedCard);

        // Act
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCostTagIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(cardCostTagService).resolveCostTags(eq(List.of(1L)), eq(costTagInputs));
    }

    @Test
    void getAllAncestryCards_WithExpandCostTags_ExpandsCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .description("Versatile ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(card));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);
        when(cardCostTagService.toResponse(costTag)).thenReturn(CardCostTagResponse.builder()
                .id(1L).label("3 Hope").category(CostTagCategory.COST).build());

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, "costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCostTags()).isNotNull();
        assertThat(result.getContent().get(0).getCostTags()).hasSize(1);
        assertThat(result.getContent().get(0).getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        // Routed through CardCostTagService#toResponse (not built inline) so a gated non-SRD
        // cost tag redacts to a stub here too.
        verify(cardCostTagService).toResponse(costTag);
    }

    @Test
    void createAncestryCard_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(999L)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.createAncestryCard(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(ancestryCardRepository, never()).save(any());
    }

    // ==================== CREATE ANCESTRY CARDS BULK TESTS ====================

    @Test
    void createAncestryCardsBulk_ValidRequests_CreatesAndReturnsCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateAncestryCardRequest request1 = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        CreateAncestryCardRequest request2 = CreateAncestryCardRequest.builder()
                .name("Elf")
                .description("Ancient ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        AncestryCard savedCard1 = AncestryCard.builder().id(1L).name("Human").description("Versatile ancestry")
                .expansion(expansion).isOfficial(true).createdAt(LocalDateTime.now()).build();

        AncestryCard savedCard2 = AncestryCard.builder().id(2L).name("Elf").description("Ancient ancestry")
                .expansion(expansion).isOfficial(true).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(ancestryCardRepository.saveAll(anyList())).thenReturn(List.of(savedCard1, savedCard2));

        // Act
        List<AncestryCardResponse> results = ancestryCardService.createAncestryCardsBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Human");
        assertThat(results.get(1).getName()).isEqualTo("Elf");
        verify(ancestryCardRepository).saveAll(anyList());
    }

    // ==================== UPDATE ANCESTRY CARD TESTS ====================

    @Test
    void updateAncestryCard_ValidRequest_UpdatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard existingCard = AncestryCard.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .isOfficial(false)
                .features(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .backgroundImageUrl("https://img.url/updated")
                .featureIds(List.of())
                .build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AncestryCardResponse result = ancestryCardService.updateAncestryCard(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getIsOfficial()).isTrue();
        verify(ancestryCardRepository).save(any(AncestryCard.class));
    }

    @Test
    void updateAncestryCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.updateAncestryCard(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("AncestryCard not found with id: 999");

        verify(ancestryCardRepository, never()).save(any());
    }

    // ==================== DELETE ANCESTRY CARD TESTS ====================

    @Test
    void deleteAncestryCard_ValidId_SoftDeletesCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard card = AncestryCard.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        // Act
        ancestryCardService.deleteAncestryCard(1L, authentication);

        // Assert
        verify(ancestryCardRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteAncestryCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.deleteAncestryCard(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("AncestryCard not found with id: 999");

        verify(ancestryCardRepository, never()).save(any());
    }

    // ==================== RESTORE ANCESTRY CARD TESTS ====================

    @Test
    void restoreAncestryCard_DeletedCard_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard deletedCard = AncestryCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(ancestryCardRepository.findById(1L)).thenReturn(Optional.of(deletedCard));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AncestryCardResponse result = ancestryCardService.restoreAncestryCard(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(ancestryCardRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreAncestryCard_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard activeCard = AncestryCard.builder()
                .id(1L)
                .name("Active Card")
                .description("Active")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(ancestryCardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.restoreAncestryCard(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AncestryCard with id 1 is not deleted");

        verify(ancestryCardRepository, never()).save(any());
    }

    @Test
    void restoreAncestryCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(ancestryCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.restoreAncestryCard(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("AncestryCard not found with id: 999");
    }

    // ==================== MIXED ANCESTRY CARD TESTS ====================

    @Test
    void createMixedAncestryCard_WithValid2Features_ReturnsResponse() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature1 = Feature.builder().id(1L).name("Tough").featureType(FeatureType.ANCESTRY).expansion(expansion).build();
        Feature feature2 = Feature.builder().id(2L).name("Nimble").featureType(FeatureType.ANCESTRY).expansion(expansion).build();

        CreateMixedAncestryCardRequest request = CreateMixedAncestryCardRequest.builder()
                .name("Human-Elf Mix")
                .description("A mixed heritage")
                .expansionId(1L)
                .featureIds(List.of(1L, 2L))
                .build();

        AncestryCard savedCard = AncestryCard.builder()
                .id(10L)
                .name("Human-Elf Mix")
                .description("A mixed heritage")
                .expansion(expansion)
                .isOfficial(false)
                .isMixed(true)
                .features(new HashSet<>(List.of(feature1, feature2)))
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L))).thenReturn(List.of(feature1, feature2));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenReturn(savedCard);

        // Act
        AncestryCardResponse result = ancestryCardService.createMixedAncestryCard(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Human-Elf Mix");
        assertThat(result.getIsMixed()).isTrue();
        assertThat(result.getIsOfficial()).isFalse();
        assertThat(result.getFeatureIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(ancestryCardRepository).save(any(AncestryCard.class));
    }

    @Test
    void createMixedAncestryCard_WithLessThan2Features_ThrowsException() {
        // Arrange
        CreateMixedAncestryCardRequest request = CreateMixedAncestryCardRequest.builder()
                .name("Mixed")
                .description("Invalid")
                .expansionId(1L)
                .featureIds(List.of(1L))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.createMixedAncestryCard(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly two feature IDs must be provided for a mixed ancestry card");

        verify(ancestryCardRepository, never()).save(any());
    }

    @Test
    void createMixedAncestryCard_WithMoreThan2Features_ThrowsException() {
        // Arrange
        CreateMixedAncestryCardRequest request = CreateMixedAncestryCardRequest.builder()
                .name("Mixed")
                .description("Invalid")
                .expansionId(1L)
                .featureIds(List.of(1L, 2L, 3L))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.createMixedAncestryCard(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exactly two feature IDs must be provided for a mixed ancestry card");

        verify(ancestryCardRepository, never()).save(any());
    }

    @Test
    void getAllAncestryCards_DefaultHidesMixed() {
        // Arrange
        Page<AncestryCard> cardPage = new PageImpl<>(List.of());
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act — isMixed is null, should default to false
        ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, null);

        // Assert — verify repository was called with isMixed=false (the default)
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                eq(false), eq(true),
                any(Pageable.class)
        );
    }

    @Test
    void getAllAncestryCards_WithIsMixedTrue_ReturnsMixed() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        AncestryCard mixedCard = AncestryCard.builder()
                .id(10L)
                .name("Human-Elf Mix")
                .description("Mixed heritage")
                .expansion(expansion)
                .isOfficial(false)
                .isMixed(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AncestryCard> cardPage = new PageImpl<>(List.of(mixedCard));
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(true), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsMixed()).isTrue();
        assertThat(result.getContent().get(0).getName()).isEqualTo("Human-Elf Mix");
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                eq(true), eq(true),
                any(Pageable.class)
        );
    }

    // ==================== SRD GATING TESTS ====================

    @Test
    void toResponse_WhenNotViewable_ReturnsRedactedStub() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        AncestryCard card = AncestryCard.builder()
                .id(7L)
                .name("Faerie")
                .description("A secretive ancestry")
                .expansion(expansion)
                .isOfficial(true)
                .srd(false)
                .build();

        when(contentAccessService.mayView(card)).thenReturn(false);

        // Act
        AncestryCardResponse response = ancestryCardService.toResponse(card, Set.of());

        // Assert — only id, cardType, expansionName, restricted are carried
        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getExpansionName()).isEqualTo("Hope & Fear");
        assertThat(response.getRestricted()).isTrue();
        assertThat(response.getName()).isNull();
        assertThat(response.getDescription()).isNull();
        assertThat(response.getIsOfficial()).isNull();
        assertThat(response.getSrd()).isNull();
        assertThat(response.getFeatureIds()).isNull();
    }

    @Test
    void getAllAncestryCards_PassesIncludeNonSrdFromContentAccessService() {
        // Arrange
        when(contentAccessService.includeNonSrd()).thenReturn(false);
        Page<AncestryCard> cardPage = new PageImpl<>(List.of());
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(false), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null, null);

        // Assert
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(false), any(Pageable.class));
    }

    @Test
    void getAllAncestryCards_IncludeDeletedRequestedButNotResolved_UsesActiveOnlyQuery() {
        // Arrange — caller requests includeDeleted=true but ContentAccessService coerces it to false
        when(contentAccessService.resolveIncludeDeleted(true)).thenReturn(false);
        Page<AncestryCard> cardPage = new PageImpl<>(List.of());
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        ancestryCardService.getAllAncestryCards(0, 20, true, null, null, null, null);

        // Assert — the includeDeleted=true (unfiltered) query is never reached
        verify(ancestryCardRepository, never()).findAllWithFilters(any(), any(), any(), any());
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(false), eq(true), any(Pageable.class));
    }

    @Test
    void createAncestryCard_UsesResolveSrdResult() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        CreateAncestryCardRequest request = CreateAncestryCardRequest.builder()
                .name("Human")
                .description("Versatile ancestry")
                .expansionId(1L)
                .isOfficial(true)
                .srd(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(contentAccessService.resolveSrd(any(), eq(true))).thenReturn(false);
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request, authentication);

        // Assert — the coerced (not the requested) value is what gets persisted
        assertThat(result.getSrd()).isFalse();
        verify(ancestryCardRepository).save(argThat(c -> Boolean.FALSE.equals(c.getSrd())));
    }

    @Test
    void updateAncestryCard_WithSrdProvided_UsesResolveSrdResult() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        AncestryCard existingCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .expansion(expansion)
                .isOfficial(true)
                .srd(false)
                .build();

        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder()
                .srd(true)
                .build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(contentAccessService.resolveSrd(any(), eq(true))).thenReturn(true);
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AncestryCardResponse result = ancestryCardService.updateAncestryCard(1L, request, authentication);

        // Assert
        assertThat(result.getSrd()).isTrue();
        verify(contentAccessService).resolveSrd(any(), eq(true));
    }

    @Test
    void updateAncestryCard_WithoutSrdProvided_LeavesSrdUnchanged() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        AncestryCard existingCard = AncestryCard.builder()
                .id(1L)
                .name("Human")
                .expansion(expansion)
                .isOfficial(true)
                .srd(true)
                .build();

        UpdateAncestryCardRequest request = UpdateAncestryCardRequest.builder().build();

        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(ancestryCardRepository.save(any(AncestryCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AncestryCardResponse result = ancestryCardService.updateAncestryCard(1L, request, authentication);

        // Assert
        assertThat(result.getSrd()).isTrue();
        verify(contentAccessService, never()).resolveSrd(any(), any());
    }
}
