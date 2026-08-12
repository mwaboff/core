package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateLootRequest;
import com.aboff.core.model.dto.dh.request.UpdateLootRequest;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.LootResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.BaseItem;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Loot;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.LootRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.aboff.core.service.AuditLogger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
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
 * Unit tests for LootService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class LootServiceTest {

    @Mock
    private LootRepository lootRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private ItemAccessService itemAccessService;

    @Mock
    private ContentAccessService contentAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LootService lootService;

    // ==================== GET ALL LOOT TESTS ====================

    @BeforeEach
    void stubDefaultVisibility() {
        // Every list call resolves the caller's visibility scope first. Default to a
        // non-privileged user who belongs to no campaigns; tests that care override it.
        lenient().when(itemAccessService.visibilityScope(any()))
                .thenReturn(new ItemAccessService.VisibilityScope(1L, List.of(-1L), false));
        // Every list call also resolves includeDeleted through the SRD gate; default to the
        // ordinary (non-deleted) browse path unless a test overrides it.
        lenient().when(contentAccessService.resolveIncludeDeleted(anyBoolean())).thenReturn(false);
        // toResponse redacts anything mayView() rejects; default to visible so existing
        // assertions on full response fields keep working. Redaction itself is covered by a
        // dedicated test below.
        lenient().when(contentAccessService.mayView(any(BaseItem.class))).thenReturn(true);
    }

    @Test
    void getAllLoot_WithoutFilters_ReturnsPagedLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot1 = createTestLoot(1L, "Health Potion", expansion);
        Loot loot2 = createTestLoot(2L, "Rope", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot1, loot2));
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, null, null, null, null, null, null, null, authentication);

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
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, 1L, null, null, null, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(lootRepository).findAccessibleWithFilters(any(), any(), anyBoolean(), eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void getAllLoot_WithOfficialFilter_ReturnsFilteredLoot() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot));
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), eq(true), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, true, null, null, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
    }

    @Test
    void getAllLoot_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Loot> lootPage = new PageImpl<>(List.of());
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        lootService.getAllLoot(0, 500, false, null, null, null, null, null, null, null, null, authentication);

        // Assert
        verify(lootRepository).findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    void getAllLoot_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();

        Loot loot = createTestLoot(1L, "Health Potion", expansion);

        Page<Loot> lootPage = new PageImpl<>(List.of(loot));
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        PagedResponse<LootResponse> result = lootService.getAllLoot(0, 20, false, null, null, null, null, null, null, null, "expansion", authentication);

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
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("Restores health when consumed")
                .build();

        Loot savedLoot = createTestLoot(1L, "Health Potion", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request, null);

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
        assertThatThrownBy(() -> lootService.createLoot(request, null))
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
                .tier(1)
                .isOfficial(false)
                .isConsumable(true)
                .description("A modified version")
                .originalLootId(1L)
                .build();

        Loot savedLoot = createTestLoot(2L, "Custom Health Potion", expansion);
        savedLoot.setOriginalLoot(originalLoot);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(originalLoot));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request, null);

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
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .build();

        CreateLootRequest request2 = CreateLootRequest.builder()
                .name("Rope")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isConsumable(false)
                .description("50 feet of rope")
                .build();

        Loot savedLoot1 = createTestLoot(1L, "Health Potion", expansion);
        Loot savedLoot2 = createTestLoot(2L, "Rope", expansion);
        savedLoot2.setDescription("50 feet of rope");

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(lootRepository.saveAll(anyList())).thenReturn(List.of(savedLoot1, savedLoot2));

        // Act
        List<LootResponse> results = lootService.createLootBulk(List.of(request1, request2), null);

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
                .tier(2)
                .isOfficial(true)
                .isConsumable(false)
                .description("Updated description")
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingLoot));
        // The update path resolves the official flag first, then asks ItemAccessService which
        // sourcebook that flag permits, rather than trusting the request's expansionId.
        when(itemAccessService.resolveIsOfficial(any(), eq(true))).thenReturn(true);
        when(itemAccessService.resolveExpansion(any(), eq(1L), eq(true))).thenReturn(expansion);
        when(lootRepository.save(any(Loot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LootResponse result = lootService.updateLoot(1L, request, null);

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
                .tier(2)
                .isOfficial(true)
                .isConsumable(false)
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.updateLoot(999L, request, null))
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
        lootService.deleteLoot(1L, null);

        // Assert
        verify(lootRepository).save(argThat(l -> l.getDeletedAt() != null));
    }

    @Test
    void deleteLoot_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(lootRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.deleteLoot(999L, null))
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
        LootResponse result = lootService.restoreLoot(1L, null);

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
        assertThatThrownBy(() -> lootService.restoreLoot(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Loot with id 1 is not deleted");

        verify(lootRepository, never()).save(any());
    }

    @Test
    void restoreLoot_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(lootRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> lootService.restoreLoot(999L, null))
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

    // ==================== FEATURE TESTS ====================

    @Test
    void createLoot_WithFeatureIds_AttachesFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Healing").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateLootRequest request = CreateLootRequest.builder()
                .name("Magic Potion")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("A magical potion")
                .featureIds(List.of(1L))
                .build();

        Loot savedLoot = createTestLoot(1L, "Magic Potion", expansion);
        savedLoot.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        verify(featureService).resolveFeatures(eq(List.of(1L)), isNull());
    }

    @Test
    void createLoot_WithInlineFeatures_AttachesFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(2L).name("Restore").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateLootRequest request = CreateLootRequest.builder()
                .name("Healing Herb")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("A healing herb")
                .features(List.of())
                .build();

        Loot savedLoot = createTestLoot(1L, "Healing Herb", expansion);
        savedLoot.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), eq(List.of()))).thenReturn(Set.of(feature));
        when(lootRepository.save(any(Loot.class))).thenReturn(savedLoot);

        // Act
        LootResponse result = lootService.createLoot(request, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(2L);
    }

    @Test
    void updateLoot_WithFeatures_UpdatesFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Enhanced Healing").featureType(FeatureType.OTHER).expansion(expansion).build();

        Loot existingLoot = createTestLoot(1L, "Potion", expansion);

        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Enhanced Potion")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .isConsumable(true)
                .description("Enhanced version")
                .featureIds(List.of(1L))
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingLoot));
        // The update path resolves the official flag first, then asks ItemAccessService which
        // sourcebook that flag permits, rather than trusting the request's expansionId.
        when(itemAccessService.resolveIsOfficial(any(), eq(true))).thenReturn(true);
        when(itemAccessService.resolveExpansion(any(), eq(1L), eq(true))).thenReturn(expansion);
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull(), any())).thenReturn(Set.of(feature));
        when(lootRepository.save(any(Loot.class))).thenAnswer(invocation -> {
            Loot saved = invocation.getArgument(0);
            saved.setFeatures(Set.of(feature));
            return saved;
        });

        // Act
        LootResponse result = lootService.updateLoot(1L, request, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        verify(featureService).resolveFeatures(eq(List.of(1L)), isNull(), any());
    }

    @Test
    void updateLoot_WithNullFeatures_DoesNotModifyFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Loot existingLoot = createTestLoot(1L, "Potion", expansion);

        UpdateLootRequest request = UpdateLootRequest.builder()
                .name("Same Potion")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isConsumable(true)
                .description("Same potion")
                .build();

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingLoot));
        // The update path resolves the official flag first, then asks ItemAccessService which
        // sourcebook that flag permits, rather than trusting the request's expansionId.
        when(itemAccessService.resolveIsOfficial(any(), eq(true))).thenReturn(true);
        when(itemAccessService.resolveExpansion(any(), eq(1L), eq(true))).thenReturn(expansion);
        when(lootRepository.save(any(Loot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        lootService.updateLoot(1L, request, null);

        // Assert
        verify(featureService, never()).resolveFeatures(any(), any(), any());
    }

    @Test
    void getLootById_WithExpandFeatures_ExpandsFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Healing").featureType(FeatureType.OTHER).expansion(expansion).createdAt(LocalDateTime.now()).build();

        Loot loot = createTestLoot(1L, "Healing Potion", expansion);
        loot.setFeatures(Set.of(feature));

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(loot));
        when(featureService.toResponse(any(Feature.class), anySet())).thenReturn(
                FeatureResponse.builder()
                        .id(1L)
                        .name("Healing")
                        .featureType(FeatureType.OTHER)
                        .expansionId(1L)
                        .build()
        );

        // Act
        LootResponse result = lootService.getLootById(1L, "features");

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        assertThat(result.getFeatures()).hasSize(1);
        assertThat(result.getFeatures().get(0).getName()).isEqualTo("Healing");
        verify(featureService).toResponse(any(Feature.class), anySet());
    }

    @Test
    void getLootById_WithoutExpandFeatures_IncludesOnlyIds() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Healing").featureType(FeatureType.OTHER).expansion(expansion).build();

        Loot loot = createTestLoot(1L, "Healing Potion", expansion);
        loot.setFeatures(Set.of(feature));

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(loot));

        // Act
        LootResponse result = lootService.getLootById(1L, null);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
        assertThat(result.getFeatures()).isNull();
        verify(featureService, never()).toResponse(any(Feature.class), anySet());
    }

    // ==================== SRD CONTENT GATING TESTS ====================

    @Test
    void toResponse_RestrictedNonSrdContent_ReturnsRedactedStub() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Loot loot = createTestLoot(1L, "Restricted Elixir", expansion);

        when(lootRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(loot));
        when(contentAccessService.mayView(loot)).thenReturn(false);

        // Act
        LootResponse result = lootService.getLootById(1L, null);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRestricted()).isTrue();
        assertThat(result.getExpansionName()).isEqualTo("Hope & Fear");
        assertThat(result.getName()).isNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getSrd()).isNull();
    }

    @Test
    void getAllLoot_IncludeDeletedRequestedByNonModerator_CoercesToFalse() {
        // Arrange: resolveIncludeDeleted is what enforces the role check now, so a coercion to
        // false must route through the ordinary (non-deleted) query rather than findAllWithFilters.
        when(contentAccessService.resolveIncludeDeleted(true)).thenReturn(false);
        Page<Loot> lootPage = new PageImpl<>(List.of());
        when(lootRepository.findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(lootPage);

        // Act
        lootService.getAllLoot(0, 20, true, null, null, null, null, null, null, null, null, authentication);

        // Assert
        verify(lootRepository, never()).findAllWithFilters(any(), any(), any(), any(), any(), any(), any());
        verify(lootRepository).findAccessibleWithFilters(any(), any(), anyBoolean(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class));
    }

    // ==================== HELPER METHODS ====================

    private Loot createTestLoot(Long id, String name, Expansion expansion) {
        return Loot.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .tier(1)
                .isOfficial(true)
                .isConsumable(false)
                .description("Restores health when consumed")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
