package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateArmorRequest;
import com.aboff.core.model.dto.dh.request.UpdateArmorRequest;
import com.aboff.core.model.dto.dh.response.ArmorResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Armor;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.ArmorRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArmorService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArmorServiceTest {

    @Mock
    private ArmorRepository armorRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private Authentication authentication;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ArmorService armorService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();

        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(adminUser));
        when(roleHierarchyService.hasRoleOrHigher(eq(adminUser), any(Role.class))).thenReturn(true);
        when(armorRepository.countByCreatedByIdAndDeletedAtIsNull(anyLong())).thenReturn(0L);
    }

    // ==================== GET ALL ARMORS TESTS ====================

    @Test
    void getAllArmors_WithoutFilters_ReturnsPagedArmors() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor armor1 = createTestArmor(1L, "Leather Armor", expansion);
        Armor armor2 = createTestArmor(2L, "Plate Mail", expansion);
        armor2.setBaseScore(3);

        Page<Armor> armorPage = new PageImpl<>(List.of(armor1, armor2));
        when(armorRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(armorPage);

        // Act
        PagedResponse<ArmorResponse> result = armorService.getAllArmors(0, 20, false, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Leather Armor");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Plate Mail");
    }

    @Test
    void getAllArmors_WithExpansionFilter_ReturnsFilteredArmors() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor armor = createTestArmor(1L, "Leather Armor", expansion);

        Page<Armor> armorPage = new PageImpl<>(List.of(armor));
        when(armorRepository.findByDeletedAtIsNullAndFilters(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(armorPage);

        // Act
        PagedResponse<ArmorResponse> result = armorService.getAllArmors(0, 20, false, 1L, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(armorRepository).findByDeletedAtIsNullAndFilters(eq(1L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllArmors_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Armor> armorPage = new PageImpl<>(List.of());
        when(armorRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(armorPage);

        // Act
        armorService.getAllArmors(0, 500, false, null, null, null, null);

        // Assert
        verify(armorRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllArmors_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Shield Block").featureType(FeatureType.OTHER).expansion(expansion).createdAt(LocalDateTime.now()).build();

        Armor armor = createTestArmor(1L, "Shield", expansion);
        armor.setFeatures(Set.of(feature));

        Page<Armor> armorPage = new PageImpl<>(List.of(armor));
        when(armorRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(armorPage);
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
        PagedResponse<ArmorResponse> result = armorService.getAllArmors(0, 20, false, null, null, null, "expansion,features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).hasSize(1);
    }

    // ==================== GET ARMOR BY ID TESTS ====================

    @Test
    void getArmorById_ValidId_ReturnsArmor() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor armor = createTestArmor(1L, "Leather Armor", expansion);

        when(armorRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(armor));

        // Act
        ArmorResponse result = armorService.getArmorById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Leather Armor");
        assertThat(result.getBaseMajorThreshold()).isEqualTo(5);
        assertThat(result.getBaseSevereThreshold()).isEqualTo(10);
        assertThat(result.getBaseScore()).isEqualTo(1);
    }

    @Test
    void getArmorById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(armorRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> armorService.getArmorById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Armor not found with id: 999");
    }

    // ==================== CREATE ARMOR TESTS ====================

    @Test
    void createArmor_ValidRequest_CreatesAndReturnsArmor() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        Armor savedArmor = createTestArmor(1L, "Leather Armor", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(armorRepository.save(any(Armor.class))).thenReturn(savedArmor);

        // Act
        ArmorResponse result = armorService.createArmor(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Leather Armor");
        verify(armorRepository).save(any(Armor.class));
    }

    @Test
    void createArmor_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(999L)
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> armorService.createArmor(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(armorRepository, never()).save(any());
    }

    @Test
    void createArmor_WithFeature_AttachesFeature() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Shield Block").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateArmorRequest request = CreateArmorRequest.builder()
                .name("Magic Shield")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .baseMajorThreshold(7)
                .baseSevereThreshold(14)
                .baseScore(2)
                .featureIds(List.of(1L))
                .build();

        Armor savedArmor = createTestArmor(1L, "Magic Shield", expansion);
        savedArmor.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(armorRepository.save(any(Armor.class))).thenReturn(savedArmor);

        // Act
        ArmorResponse result = armorService.createArmor(request, authentication);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
    }

    // ==================== CREATE ARMORS BULK TESTS ====================

    @Test
    void createArmorsBulk_ValidRequests_CreatesAndReturnsArmors() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateArmorRequest request1 = CreateArmorRequest.builder()
                .name("Leather Armor")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        CreateArmorRequest request2 = CreateArmorRequest.builder()
                .name("Plate Mail")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .baseMajorThreshold(8)
                .baseSevereThreshold(16)
                .baseScore(3)
                .build();

        Armor savedArmor1 = createTestArmor(1L, "Leather Armor", expansion);
        Armor savedArmor2 = createTestArmor(2L, "Plate Mail", expansion);
        savedArmor2.setBaseScore(3);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(armorRepository.saveAll(anyList())).thenReturn(List.of(savedArmor1, savedArmor2));

        // Act
        List<ArmorResponse> results = armorService.createArmorsBulk(List.of(request1, request2), null);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Leather Armor");
        assertThat(results.get(1).getName()).isEqualTo("Plate Mail");
        verify(armorRepository).saveAll(anyList());
    }

    // ==================== UPDATE ARMOR TESTS ====================

    @Test
    void updateArmor_ValidRequest_UpdatesAndReturnsArmor() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor existingArmor = createTestArmor(1L, "Old Name", expansion);

        UpdateArmorRequest request = UpdateArmorRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .baseMajorThreshold(7)
                .baseSevereThreshold(14)
                .baseScore(2)
                .build();

        when(armorRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingArmor));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(armorRepository.save(any(Armor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ArmorResponse result = armorService.updateArmor(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getBaseMajorThreshold()).isEqualTo(7);
        assertThat(result.getBaseSevereThreshold()).isEqualTo(14);
        assertThat(result.getBaseScore()).isEqualTo(2);
        verify(armorRepository).save(any(Armor.class));
    }

    @Test
    void updateArmor_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateArmorRequest request = UpdateArmorRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .build();

        when(armorRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> armorService.updateArmor(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Armor not found with id: 999");

        verify(armorRepository, never()).save(any());
    }

    // ==================== DELETE ARMOR TESTS ====================

    @Test
    void deleteArmor_ValidId_SoftDeletesArmor() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor armor = createTestArmor(1L, "To Delete", expansion);

        when(armorRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(armor));

        // Act
        armorService.deleteArmor(1L, null);

        // Assert
        verify(armorRepository).save(argThat(a -> a.getDeletedAt() != null));
    }

    @Test
    void deleteArmor_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(armorRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> armorService.deleteArmor(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Armor not found with id: 999");

        verify(armorRepository, never()).save(any());
    }

    // ==================== RESTORE ARMOR TESTS ====================

    @Test
    void restoreArmor_DeletedArmor_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor deletedArmor = createTestArmor(1L, "Deleted Armor", expansion);
        deletedArmor.setDeletedAt(LocalDateTime.now());

        when(armorRepository.findById(1L)).thenReturn(Optional.of(deletedArmor));
        when(armorRepository.save(any(Armor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ArmorResponse result = armorService.restoreArmor(1L, null);

        // Assert
        assertThat(result).isNotNull();
        verify(armorRepository).save(argThat(a -> a.getDeletedAt() == null));
    }

    @Test
    void restoreArmor_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Armor activeArmor = createTestArmor(1L, "Active Armor", expansion);

        when(armorRepository.findById(1L)).thenReturn(Optional.of(activeArmor));

        // Act & Assert
        assertThatThrownBy(() -> armorService.restoreArmor(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Armor with id 1 is not deleted");

        verify(armorRepository, never()).save(any());
    }

    @Test
    void restoreArmor_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(armorRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> armorService.restoreArmor(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Armor not found with id: 999");
    }

    // ==================== HELPER METHODS ====================

    private Armor createTestArmor(Long id, String name, Expansion expansion) {
        return Armor.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .tier(1)
                .isOfficial(true)
                .baseMajorThreshold(5)
                .baseSevereThreshold(10)
                .baseScore(1)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
