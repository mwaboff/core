package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.LootRepository;
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
 * Unit tests for LootService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class LootServiceTest {

    @Mock
    private LootRepository lootRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @InjectMocks
    private LootService lootService;

    // ==================== GET ALL LOOT TESTS ====================

    @Test
    void getAllLoot_WithoutFilters_ReturnsPagedLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot1 = createTestLoot(1L, "Health Potion", expansion);
        Loot loot2 = createTestLoot(2L, "Rope", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot1, loot2));
        when(lootRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Health Potion");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Rope");
    }

    @Test
    void getAllLoot_WithExpansionFilter_ReturnsFilteredLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot));
        when(lootRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, 1L, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(lootRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void getAllLoot_WithOfficialFilter_ReturnsFilteredLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot));
        when(lootRepository.findByDeletedAtIsNullAndFilters(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, true, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
    }

    @Test
    void getAllLoot_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Loot> lootPage = new PageImpl<>(List.of());
        when(lootRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        lootService.getAllLoot(0, 500, false, null, null, null);

        // Assert
        verify(lootRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllLoot_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot));
        when(lootRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, null, "expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    // ==================== GET LOOT BY ID TESTS ====================

    @Test
    void getLootById_ValidId_ReturnsLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        when(lootRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(loot));

        // Act
        LootResponse result = lootService.getLootById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Health Potion");
        assertThat(result.getDescription()).isEqualTo("Restores health when consumed");
    }

    @Test
    void getLootById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(lootRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.getLootById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Loot not found with id: 999");
    }

    // ==================== CREATE LOOT TESTS ====================

    @Test
    void createLoot_ValidRequest_CreatesAndReturnsLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateLootRequest request = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(1L)
                .isOfficial(true)
                .description("Restores health when consumed")
                .build();

        Loot savedLoot = createTestLoot(1L, "Health Potion", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Health Potion");
        verify(lootRepository).save(any(Loot.class));
    }

    @Test
    void createLoot_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateLootRequest request = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(999L)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.createLoot(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(lootRepository, never()).save(any());
    }

    @Test
    void createLoot_WithOriginalLoot_AttachesOriginal() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Loot originalLoot = createTestLoot(1L, "Health Potion", expansion);

        CreateLootRequest request = CreateLootRequest.builder()
                .name("Custom Health Potion")
                .expansionId(1L)
                .isOfficial(false)
                .description("A modified version")
                .originalLootId(1L)
                .build();

        Loot savedLoot = createTestLoot(2L, "Custom Health Potion", expansion);
        savedLoot.setOriginalLoot(originalLoot);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(originalLoot));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request);

        // Assert
        assertThat(result.getOriginalLootId()).isEqualTo(1L);
    }

    // ==================== CREATE LOOT BULK TESTS ====================

    @Test
    void createLootBulk_ValidRequests_CreatesAndReturnsLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateLootRequest request1 = CreateLootRequest.builder()
                .name("Health Potion")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        CreateLootRequest request2 = CreateLootRequest.builder()
                .name("Rope")
                .expansionId(1L)
                .isOfficial(true)
                .description("50 feet of rope")
                .build();

        Loot savedLoot1 = createTestLoot(1L, "Health Potion", expansion);
        Loot savedLoot2 = createTestLoot(2L, "Rope", expansion);
        savedLoot2.setDescription("50 feet of rope");

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.saveAll(anyList())).thenReturn(List.of(savedLoot1, savedLoot2));

        // Act
        List<LootResponse> results = lootService.createLootBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Health Potion");
        assertThat(results.get(1).getName()).isEqualTo("Rope");
        verify(lootRepository).saveAll(anyList());
    }

    // ==================== UPDATE LOOT TESTS ====================

    @Test
    void updateLoot_ValidRequest_UpdatesAndReturnsLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot existingLoot = createTestLoot(1L, "Old Name", expansion);

        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .isOfficial(true)
                .description("Updated description")
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingLoot));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.save(any(Loot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LootResponse result = lootService.updateLoot(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(lootRepository).save(any(Loot.class));
    }

    @Test
    void updateLoot_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.updateLoot(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Loot not found with id: 999");

        verify(lootRepository, never()).save(any());
    }

    // ==================== DELETE LOOT TESTS ====================

    @Test
    void deleteLoot_ValidId_SoftDeletesLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot = createTestLoot(1L, "To Delete", expansion);

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(loot));

        // Act
        lootService.deleteLoot(1L);

        // Assert
        verify(lootRepository).save(argThat(l -> l.getDeletedAt() != null));
    }

    @Test
    void deleteLoot_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(lootRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.deleteLoot(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Loot not found with id: 999");

        verify(lootRepository, never()).save(any());
    }

    // ==================== RESTORE LOOT TESTS ====================

    @Test
    void restoreLoot_DeletedLoot_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot deletedLoot = createTestLoot(1L, "Deleted Loot", expansion);
        deletedLoot.setDeletedAt(LocalDateTime.now());

        when(lootRepository.findById(1L)).thenReturn(Optional.of(deletedLoot));
        when(lootRepository.save(any(Loot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LootResponse result = lootService.restoreLoot(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(lootRepository).save(argThat(l -> l.getDeletedAt() == null));
    }

    @Test
    void restoreLoot_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot activeLoot = createTestLoot(1L, "Active Loot", expansion);

        when(lootRepository.findById(1L)).thenReturn(Optional.of(activeLoot));

        // Act & Assert
        assertThatThrownBy(() -> lootService.restoreLoot(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Loot with id 1 is not deleted");

        verify(lootRepository, never()).save(any());
    }

    @Test
    void restoreLoot_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(lootRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.restoreLoot(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Loot not found with id: 999");
    }

    // ==================== EXPAND ORIGINAL LOOT TESTS ====================

    @Test
    void getLootById_WithExpandOriginalLoot_ExpandsOriginal() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();

        Loot originalLoot = createTestLoot(1L, "Health Potion", expansion);
        Loot customLoot = createTestLoot(2L, "Custom Potion", expansion);
        customLoot.setOriginalLoot(originalLoot);

        when(lootRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(customLoot));

        // Act
        LootResponse result = lootService.getLootById(2L, "originalLoot");

        // Assert
        assertThat(result.getOriginalLoot()).isNotNull();
        assertThat(result.getOriginalLoot().getName()).isEqualTo("Health Potion");
    }

    // ==================== HELPER METHODS ====================

    private Loot createTestLoot(Long id, String name, Expansion expansion) {
        return Loot.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .isOfficial(true)
                .description("Restores health when consumed")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
