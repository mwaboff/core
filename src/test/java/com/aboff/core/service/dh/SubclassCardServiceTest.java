package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateSubclassCardRequest;
import com.aboff.core.model.dto.dh.response.SubclassCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
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
    private FeatureRepository featureRepository;

    @Mock
    private CardCostTagService cardCostTagService;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private SubclassCardService subclassCardService;

    // ==================== GET ALL SUBCLASS CARDS TESTS ====================

    @Test
    void getAllSubclassCards_WithoutFilters_ReturnsPagedCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        SubclassCard card1 = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
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
                .associatedClass(clazz)
                .level(SubclassLevel.SPECIALIZATION)
                .backgroundImageUrl("https://img.url/guardian")
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card1, card2));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, null);

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

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), eq(SubclassLevel.FOUNDATION), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, SubclassLevel.FOUNDATION, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLevel()).isEqualTo(SubclassLevel.FOUNDATION);
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), eq(SubclassLevel.FOUNDATION), any(Pageable.class));
    }

    @Test
    void getAllSubclassCards_WithAssociatedClassFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssociatedClassId()).isEqualTo(1L);
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllSubclassCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<SubclassCard> cardPage = new PageImpl<>(List.of());
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        subclassCardService.getAllSubclassCards(0, 500, false, null, null, null, null, null);

        // Assert
        verify(subclassCardRepository).findByDeletedAtIsNullAndFilters(
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
        Domain domain = Domain.builder().id(1L).name("Blade").expansion(expansion).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .associatedDomains(Set.of(domain))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, "expansion,features,associatedClass,associatedDomains");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNull();
        assertThat(result.getContent().get(0).getAssociatedClass()).isNotNull();
        assertThat(result.getContent().get(0).getAssociatedDomains()).isNotNull();
    }

    @Test
    void getAllSubclassCards_WithExpandFeaturesWithoutCostTags_IncludesCostTagIdsOnly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, "features");

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
        CardCostTag costTag = CardCostTag.builder().id(10L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassCard> cardPage = new PageImpl<>(List.of(card));
        when(subclassCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<SubclassCardResponse> result = subclassCardService.getAllSubclassCards(0, 20, false, null, null, null, null, "features,costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFeatures()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTagIds()).containsExactly(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags()).isNotNull().hasSize(1);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(result.getContent().get(0).getFeatures().get(0).getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
    }

    // ==================== GET SUBCLASS CARD BY ID TESTS ====================

    @Test
    void getSubclassCardById_ValidId_ReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
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
        Feature feature = Feature.builder().id(1L).name("Rage").featureType(FeatureType.CLASS).expansion(expansion).build();
        Domain domain = Domain.builder().id(1L).name("Blade").expansion(expansion).build();

        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .featureIds(List.of(1L))
                .associatedDomainIds(List.of(1L))
                .build();

        SubclassCard savedCard = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .backgroundImageUrl("https://img.url/berserker")
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(feature));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(domain));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenReturn(savedCard);

        // Act
        SubclassCardResponse result = subclassCardService.createSubclassCard(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Berserker");
        verify(subclassCardRepository).save(any(SubclassCard.class));
    }

    @Test
    void createSubclassCard_ClassNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(999L)
                .level(SubclassLevel.FOUNDATION)
                .build();

        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.createSubclassCard(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");

        verify(subclassCardRepository, never()).save(any());
    }

    @Test
    void createSubclassCard_WithSpellcastingTrait_CreatesCardWithTraitInfo() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Mage").expansion(expansion).startingEvasion(8).startingHitPoints(15).build();

        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(Trait.KNOWLEDGE)
                .build();

        SubclassCard savedCard = SubclassCard.builder()
                .id(1L)
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(Trait.KNOWLEDGE)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenReturn(savedCard);

        // Act
        SubclassCardResponse result = subclassCardService.createSubclassCard(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSpellcastingTrait()).isNotNull();
        assertThat(result.getSpellcastingTrait().getTrait()).isEqualTo(Trait.KNOWLEDGE);
        assertThat(result.getSpellcastingTrait().getDescription()).isEqualTo(Trait.KNOWLEDGE.getDescription());
        assertThat(result.getSpellcastingTrait().getExamples()).isEqualTo(Trait.KNOWLEDGE.getExamples());
        verify(subclassCardRepository).save(argThat(card -> card.getSpellcastingTrait() == Trait.KNOWLEDGE));
    }

    @Test
    void createSubclassCard_WithoutSpellcastingTrait_CreatesCardWithNullTrait() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        CreateSubclassCardRequest request = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .build();

        SubclassCard savedCard = SubclassCard.builder()
                .id(1L)
                .name("Berserker")
                .description("Rage fighter")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenReturn(savedCard);

        // Act
        SubclassCardResponse result = subclassCardService.createSubclassCard(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSpellcastingTrait()).isNull();
        verify(subclassCardRepository).save(argThat(card -> card.getSpellcastingTrait() == null));
    }

    // ==================== CREATE SUBCLASS CARDS BULK TESTS ====================

    @Test
    void createSubclassCardsBulk_ValidRequests_CreatesAndReturnsCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        CreateSubclassCardRequest request1 = CreateSubclassCardRequest.builder()
                .name("Berserker")
                .description("Rage fighter")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .build();

        CreateSubclassCardRequest request2 = CreateSubclassCardRequest.builder()
                .name("Guardian")
                .description("Defender")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .build();

        SubclassCard savedCard1 = SubclassCard.builder().id(1L).name("Berserker").description("Rage fighter")
                .expansion(expansion).isOfficial(true).associatedClass(clazz).level(SubclassLevel.FOUNDATION)
                .createdAt(LocalDateTime.now()).build();

        SubclassCard savedCard2 = SubclassCard.builder().id(2L).name("Guardian").description("Defender")
                .expansion(expansion).isOfficial(true).associatedClass(clazz).level(SubclassLevel.SPECIALIZATION)
                .createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
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

        SubclassCard existingCard = SubclassCard.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .isOfficial(false)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .features(new HashSet<>())
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .backgroundImageUrl("https://img.url/updated")
                .featureIds(List.of())
                .associatedDomainIds(List.of())
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
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
                .associatedClassId(1L)
                .level(SubclassLevel.SPECIALIZATION)
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassCardService.updateSubclassCard(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassCard not found with id: 999");

        verify(subclassCardRepository, never()).save(any());
    }

    @Test
    void updateSubclassCard_UpdateSpellcastingTrait_UpdatesTraitSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Mage").expansion(expansion).startingEvasion(8).startingHitPoints(15).build();

        SubclassCard existingCard = SubclassCard.builder()
                .id(1L)
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(Trait.KNOWLEDGE)
                .features(new HashSet<>())
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(Trait.INSTINCT)
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassCardResponse result = subclassCardService.updateSubclassCard(1L, request);

        // Assert
        assertThat(result.getSpellcastingTrait()).isNotNull();
        assertThat(result.getSpellcastingTrait().getTrait()).isEqualTo(Trait.INSTINCT);
        assertThat(result.getSpellcastingTrait().getDescription()).isEqualTo(Trait.INSTINCT.getDescription());
        assertThat(result.getSpellcastingTrait().getExamples()).isEqualTo(Trait.INSTINCT.getExamples());
        verify(subclassCardRepository).save(argThat(card -> card.getSpellcastingTrait() == Trait.INSTINCT));
    }

    @Test
    void updateSubclassCard_RemoveSpellcastingTrait_SetsTraitToNull() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Mage").expansion(expansion).startingEvasion(8).startingHitPoints(15).build();

        SubclassCard existingCard = SubclassCard.builder()
                .id(1L)
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(Trait.KNOWLEDGE)
                .features(new HashSet<>())
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateSubclassCardRequest request = UpdateSubclassCardRequest.builder()
                .name("Elementalist")
                .description("Master of elemental magic")
                .expansionId(1L)
                .isOfficial(true)
                .associatedClassId(1L)
                .level(SubclassLevel.FOUNDATION)
                .spellcastingTrait(null)
                .build();

        when(subclassCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(subclassCardRepository.save(any(SubclassCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassCardResponse result = subclassCardService.updateSubclassCard(1L, request);

        // Assert
        assertThat(result.getSpellcastingTrait()).isNull();
        verify(subclassCardRepository).save(argThat(card -> card.getSpellcastingTrait() == null));
    }

    // ==================== DELETE SUBCLASS CARD TESTS ====================

    @Test
    void deleteSubclassCard_ValidId_SoftDeletesCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Warrior").expansion(expansion).startingEvasion(10).startingHitPoints(20).build();

        SubclassCard card = SubclassCard.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
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

        SubclassCard deletedCard = SubclassCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
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

        SubclassCard activeCard = SubclassCard.builder()
                .id(1L)
                .name("Active Card")
                .description("Active")
                .expansion(expansion)
                .isOfficial(true)
                .associatedClass(clazz)
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
