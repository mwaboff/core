package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainCardRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainCardRequest;
import com.aboff.core.model.dto.dh.response.DomainCardResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.DomainCard;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.DomainCardType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.DomainCardRepository;
import com.aboff.core.repository.dh.DomainRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DomainCardService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class DomainCardServiceTest {

    @Mock
    private DomainCardRepository domainCardRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainCardService domainCardService;

    // ==================== GET ALL DOMAIN CARDS TESTS ====================

    @Test
    void getAllDomainCards_WithoutFilters_ReturnsPagedCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard card1 = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .backgroundImageUrl("https://img.url/fireball")
                .createdAt(LocalDateTime.now())
                .build();

        DomainCard card2 = DomainCard.builder()
                .id(2L)
                .name("Magic Shield")
                .description("Create shield")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(2)
                .recallCost(3)
                .type(DomainCardType.GRIMOIRE)
                .backgroundImageUrl("https://img.url/shield")
                .createdAt(LocalDateTime.now())
                .build();

        Page<DomainCard> cardPage = new PageImpl<>(List.of(card1, card2));
        when(domainCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<DomainCardResponse> result = domainCardService.getAllDomainCards(0, 20, false, null, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Fireball");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Magic Shield");
    }

    @Test
    void getAllDomainCards_WithTypeFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard card = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now())
                .build();

        Page<DomainCard> cardPage = new PageImpl<>(List.of(card));
        when(domainCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), eq(DomainCardType.SPELL), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<DomainCardResponse> result = domainCardService.getAllDomainCards(0, 20, false, null, null, null, DomainCardType.SPELL, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(DomainCardType.SPELL);
        verify(domainCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), eq(DomainCardType.SPELL), any(Pageable.class));
    }

    @Test
    void getAllDomainCards_WithAssociatedDomainFilter_ReturnsFilteredCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard card = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now())
                .build();

        Page<DomainCard> cardPage = new PageImpl<>(List.of(card));
        when(domainCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<DomainCardResponse> result = domainCardService.getAllDomainCards(0, 20, false, null, null, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssociatedDomainId()).isEqualTo(1L);
        verify(domainCardRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllDomainCards_WithLargePage_LimitsTo100() {
        // Arrange
        Page<DomainCard> cardPage = new PageImpl<>(List.of());
        when(domainCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        domainCardService.getAllDomainCards(0, 500, false, null, null, null, null, null);

        // Assert
        verify(domainCardRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllDomainCards_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Blast").featureType(FeatureType.DOMAIN).expansion(expansion).createdAt(LocalDateTime.now()).build();

        DomainCard card = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .features(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<DomainCard> cardPage = new PageImpl<>(List.of(card));
        when(domainCardRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(cardPage);

        // Act
        PagedResponse<DomainCardResponse> result = domainCardService.getAllDomainCards(0, 20, false, null, null, null, null, "expansion,features,associatedDomain");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getAssociatedDomain()).isNotNull();
    }

    // ==================== GET DOMAIN CARD BY ID TESTS ====================

    @Test
    void getDomainCardById_ValidId_ReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard card = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .backgroundImageUrl("https://img.url/fireball")
                .createdAt(LocalDateTime.now())
                .build();

        when(domainCardRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(card));

        // Act
        DomainCardResponse result = domainCardService.getDomainCardById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Fireball");
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getRecallCost()).isEqualTo(2);
        assertThat(result.getType()).isEqualTo(DomainCardType.SPELL);
    }

    @Test
    void getDomainCardById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainCardRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.getDomainCardById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("DomainCard not found with id: 999");
    }

    // ==================== CREATE DOMAIN CARD TESTS ====================

    @Test
    void createDomainCard_ValidRequest_CreatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();
        Feature feature = Feature.builder().id(1L).name("Blast").featureType(FeatureType.DOMAIN).expansion(expansion).build();

        CreateDomainCardRequest request = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Cast fire spell")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(1L)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .backgroundImageUrl("https://img.url/fireball")
                .featureIds(List.of(1L))
                .build();

        DomainCard savedCard = DomainCard.builder()
                .id(1L)
                .name("Fireball")
                .description("Cast fire spell")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .backgroundImageUrl("https://img.url/fireball")
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(domain));
        when(featureRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(feature));
        when(domainCardRepository.save(any(DomainCard.class))).thenReturn(savedCard);

        // Act
        DomainCardResponse result = domainCardService.createDomainCard(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Fireball");
        verify(domainCardRepository).save(any(DomainCard.class));
    }

    @Test
    void createDomainCard_DomainNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateDomainCardRequest request = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Cast fire spell")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(999L)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .build();

        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.createDomainCard(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Domain not found with id: 999");

        verify(domainCardRepository, never()).save(any());
    }

    // ==================== CREATE DOMAIN CARDS BULK TESTS ====================

    @Test
    void createDomainCardsBulk_ValidRequests_CreatesAndReturnsCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        CreateDomainCardRequest request1 = CreateDomainCardRequest.builder()
                .name("Fireball")
                .description("Cast fire spell")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(1L)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .build();

        CreateDomainCardRequest request2 = CreateDomainCardRequest.builder()
                .name("Magic Shield")
                .description("Create shield")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(1L)
                .level(2)
                .recallCost(3)
                .type(DomainCardType.GRIMOIRE)
                .build();

        DomainCard savedCard1 = DomainCard.builder().id(1L).name("Fireball").description("Cast fire spell")
                .expansion(expansion).isOfficial(true).associatedDomain(domain).level(1).recallCost(2).type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now()).build();

        DomainCard savedCard2 = DomainCard.builder().id(2L).name("Magic Shield").description("Create shield")
                .expansion(expansion).isOfficial(true).associatedDomain(domain).level(2).recallCost(3).type(DomainCardType.GRIMOIRE)
                .createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(domain));
        when(domainCardRepository.saveAll(anyList())).thenReturn(List.of(savedCard1, savedCard2));

        // Act
        List<DomainCardResponse> results = domainCardService.createDomainCardsBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Fireball");
        assertThat(results.get(1).getName()).isEqualTo("Magic Shield");
        verify(domainCardRepository).saveAll(anyList());
    }

    // ==================== UPDATE DOMAIN CARD TESTS ====================

    @Test
    void updateDomainCard_ValidRequest_UpdatesAndReturnsCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard existingCard = DomainCard.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .isOfficial(false)
                .associatedDomain(domain)
                .level(1)
                .recallCost(1)
                .type(DomainCardType.SPELL)
                .features(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateDomainCardRequest request = UpdateDomainCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(1L)
                .level(2)
                .recallCost(3)
                .type(DomainCardType.GRIMOIRE)
                .backgroundImageUrl("https://img.url/updated")
                .featureIds(List.of())
                .build();

        when(domainCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCard));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(domain));
        when(domainCardRepository.save(any(DomainCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DomainCardResponse result = domainCardService.updateDomainCard(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getLevel()).isEqualTo(2);
        assertThat(result.getRecallCost()).isEqualTo(3);
        assertThat(result.getType()).isEqualTo(DomainCardType.GRIMOIRE);
        verify(domainCardRepository).save(any(DomainCard.class));
    }

    @Test
    void updateDomainCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateDomainCardRequest request = UpdateDomainCardRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .isOfficial(true)
                .associatedDomainId(1L)
                .level(2)
                .recallCost(3)
                .type(DomainCardType.GRIMOIRE)
                .build();

        when(domainCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.updateDomainCard(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("DomainCard not found with id: 999");

        verify(domainCardRepository, never()).save(any());
    }

    // ==================== DELETE DOMAIN CARD TESTS ====================

    @Test
    void deleteDomainCard_ValidId_SoftDeletesCard() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard card = DomainCard.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainCardRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(card));

        // Act
        domainCardService.deleteDomainCard(1L);

        // Assert
        verify(domainCardRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteDomainCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainCardRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.deleteDomainCard(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("DomainCard not found with id: 999");

        verify(domainCardRepository, never()).save(any());
    }

    // ==================== RESTORE DOMAIN CARD TESTS ====================

    @Test
    void restoreDomainCard_DeletedCard_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard deletedCard = DomainCard.builder()
                .id(1L)
                .name("Deleted Card")
                .description("Deleted")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(domainCardRepository.findById(1L)).thenReturn(Optional.of(deletedCard));
        when(domainCardRepository.save(any(DomainCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DomainCardResponse result = domainCardService.restoreDomainCard(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(domainCardRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreDomainCard_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Arcana").expansion(expansion).build();

        DomainCard activeCard = DomainCard.builder()
                .id(1L)
                .name("Active Card")
                .description("Active")
                .expansion(expansion)
                .isOfficial(true)
                .associatedDomain(domain)
                .level(1)
                .recallCost(2)
                .type(DomainCardType.SPELL)
                .createdAt(LocalDateTime.now())
                .build();

        when(domainCardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.restoreDomainCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DomainCard with id 1 is not deleted");

        verify(domainCardRepository, never()).save(any());
    }

    @Test
    void restoreDomainCard_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(domainCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> domainCardService.restoreDomainCard(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("DomainCard not found with id: 999");
    }
}
