package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateAncestryCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateAncestryCardRequest;
import com.aboff.core.model.dto.dh.response.AncestryCardResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.dto.dh.request.CostTagInput;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.AncestryCardRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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
    private FeatureService featureService;

    @Mock
    private CardCostTagService cardCostTagService;

    @InjectMocks
    private AncestryCardService ancestryCardService;

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, null);

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class));
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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(isNull(), eq(true), any(Pageable.class));
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
        when(ancestryCardRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, true, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(ancestryCardRepository).findAllWithFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllAncestryCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<AncestryCard> cardPage = new PageImpl<>(List.of());
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        ancestryCardService.getAllAncestryCards(0, 500, false, null, null, null);

        // Assert
        verify(ancestryCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "expansion,features");

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "features");

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "features,costTags");

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "features,costTags");

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "features,costTags");

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
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request);

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
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request);

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
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request);

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
        AncestryCardResponse result = ancestryCardService.createAncestryCard(request);

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
        when(ancestryCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<AncestryCardResponse> result = ancestryCardService.getAllAncestryCards(0, 20, false, null, null, "costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCostTags()).isNotNull();
        assertThat(result.getContent().get(0).getCostTags()).hasSize(1);
        assertThat(result.getContent().get(0).getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
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
        assertThatThrownBy(() -> ancestryCardService.createAncestryCard(request))
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
        List<AncestryCardResponse> results = ancestryCardService.createAncestryCardsBulk(List.of(request1, request2));

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
        AncestryCardResponse result = ancestryCardService.updateAncestryCard(1L, request);

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
        assertThatThrownBy(() -> ancestryCardService.updateAncestryCard(999L, request))
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
        ancestryCardService.deleteAncestryCard(1L);

        // Assert
        verify(ancestryCardRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteAncestryCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(ancestryCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.deleteAncestryCard(999L))
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
        AncestryCardResponse result = ancestryCardService.restoreAncestryCard(1L);

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
        assertThatThrownBy(() -> ancestryCardService.restoreAncestryCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AncestryCard with id 1 is not deleted");

        verify(ancestryCardRepository, never()).save(any());
    }

    @Test
    void restoreAncestryCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(ancestryCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ancestryCardService.restoreAncestryCard(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("AncestryCard not found with id: 999");
    }
}
