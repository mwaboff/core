package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateWeaponRequest;
import com.aboff.core.model.dto.dh.request.UpdateWeaponRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.*;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.WeaponRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeaponService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
class WeaponServiceTest {

    @Mock
    private WeaponRepository weaponRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private WeaponService weaponService;

    // ==================== GET ALL WEAPONS TESTS ====================

    @Test
    void getAllWeapons_WithoutFilters_ReturnsPagedWeapons() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon weapon1 = createTestWeapon(1L, "Longsword", expansion);
        Weapon weapon2 = createTestWeapon(2L, "Shortbow", expansion);
        weapon2.setRange(Range.FAR);
        weapon2.setTrait(Trait.FINESSE);

        Page<Weapon> weaponPage = new PageImpl<>(List.of(weapon1, weapon2));
        when(weaponRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(weaponPage);

        // Act
        PagedResponse<WeaponResponse> result = weaponService.getAllWeapons(0, 20, false, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Longsword");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Shortbow");
    }

    @Test
    void getAllWeapons_WithTraitFilter_ReturnsFilteredWeapons() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon weapon = createTestWeapon(1L, "Longsword", expansion);

        Page<Weapon> weaponPage = new PageImpl<>(List.of(weapon));
        when(weaponRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(Trait.STRENGTH), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(weaponPage);

        // Act
        PagedResponse<WeaponResponse> result = weaponService.getAllWeapons(0, 20, false, null, null, Trait.STRENGTH, null, null, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTrait()).isEqualTo(Trait.STRENGTH);
        verify(weaponRepository).findByDeletedAtIsNullAndFilters(isNull(), isNull(), eq(Trait.STRENGTH), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllWeapons_WithRangeFilter_ReturnsFilteredWeapons() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon weapon = createTestWeapon(1L, "Longbow", expansion);
        weapon.setRange(Range.FAR);

        Page<Weapon> weaponPage = new PageImpl<>(List.of(weapon));
        when(weaponRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), eq(Range.FAR), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(weaponPage);

        // Act
        PagedResponse<WeaponResponse> result = weaponService.getAllWeapons(0, 20, false, null, null, null, Range.FAR, null, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRange()).isEqualTo(Range.FAR);
    }

    @Test
    void getAllWeapons_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Weapon> weaponPage = new PageImpl<>(List.of());
        when(weaponRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(weaponPage);

        // Act
        weaponService.getAllWeapons(0, 500, false, null, null, null, null, null, null, null, null);

        // Assert
        verify(weaponRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllWeapons_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Keen Edge").featureType(FeatureType.OTHER).expansion(expansion).createdAt(LocalDateTime.now()).build();

        Weapon weapon = createTestWeapon(1L, "Longsword", expansion);
        weapon.setFeatures(Set.of(feature));

        Page<Weapon> weaponPage = new PageImpl<>(List.of(weapon));
        when(weaponRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(weaponPage);
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
        PagedResponse<WeaponResponse> result = weaponService.getAllWeapons(0, 20, false, null, null, null, null, null, null, null, "expansion,features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).hasSize(1);
    }

    // ==================== GET WEAPON BY ID TESTS ====================

    @Test
    void getWeaponById_ValidId_ReturnsWeapon() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon weapon = createTestWeapon(1L, "Longsword", expansion);

        when(weaponRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(weapon));

        // Act
        WeaponResponse result = weaponService.getWeaponById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Longsword");
        assertThat(result.getTrait()).isEqualTo(Trait.STRENGTH);
        assertThat(result.getRange()).isEqualTo(Range.MELEE);
    }

    @Test
    void getWeaponById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(weaponRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> weaponService.getWeaponById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Weapon not found with id: 999");
    }

    // ==================== CREATE WEAPON TESTS ====================

    @Test
    void createWeapon_ValidRequest_CreatesAndReturnsWeapon() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(3)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        Weapon savedWeapon = createTestWeapon(1L, "Longsword", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(weaponRepository.save(any(Weapon.class))).thenReturn(savedWeapon);

        // Act
        WeaponResponse result = weaponService.createWeapon(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Longsword");
        verify(weaponRepository).save(any(Weapon.class));
    }

    @Test
    void createWeapon_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(999L)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> weaponService.createWeapon(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(weaponRepository, never()).save(any());
    }

    @Test
    void createWeapon_WithFeature_AttachesFeature() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature feature = Feature.builder().id(1L).name("Keen Edge").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Magic Sword")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.MAGIC)
                        .build())
                .featureIds(List.of(1L))
                .build();

        Weapon savedWeapon = createTestWeapon(1L, "Magic Sword", expansion);
        savedWeapon.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(weaponRepository.save(any(Weapon.class))).thenReturn(savedWeapon);

        // Act
        WeaponResponse result = weaponService.createWeapon(request);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
    }

    // ==================== CREATE WEAPONS BULK TESTS ====================

    @Test
    void createWeaponsBulk_ValidRequests_CreatesAndReturnsWeapons() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateWeaponRequest request1 = CreateWeaponRequest.builder()
                .name("Longsword")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        CreateWeaponRequest request2 = CreateWeaponRequest.builder()
                .name("Shortbow")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.FINESSE)
                .range(Range.FAR)
                .burden(Burden.TWO_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        Weapon savedWeapon1 = createTestWeapon(1L, "Longsword", expansion);
        Weapon savedWeapon2 = createTestWeapon(2L, "Shortbow", expansion);
        savedWeapon2.setTrait(Trait.FINESSE);
        savedWeapon2.setRange(Range.FAR);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(weaponRepository.saveAll(anyList())).thenReturn(List.of(savedWeapon1, savedWeapon2));

        // Act
        List<WeaponResponse> results = weaponService.createWeaponsBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Longsword");
        assertThat(results.get(1).getName()).isEqualTo("Shortbow");
        verify(weaponRepository).saveAll(anyList());
    }

    // ==================== UPDATE WEAPON TESTS ====================

    @Test
    void updateWeapon_ValidRequest_UpdatesAndReturnsWeapon() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon existingWeapon = createTestWeapon(1L, "Old Name", expansion);

        UpdateWeaponRequest request = UpdateWeaponRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .isPrimary(false)
                .trait(Trait.AGILITY)
                .range(Range.CLOSE)
                .burden(Burden.TWO_HANDED)
                .damage(UpdateWeaponRequest.DamageRollRequest.builder()
                        .diceCount(3)
                        .diceType(DiceType.D8)
                        .modifier(2)
                        .damageType(DamageType.MAGIC)
                        .build())
                .build();

        when(weaponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingWeapon));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(weaponRepository.save(any(Weapon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WeaponResponse result = weaponService.updateWeapon(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getIsPrimary()).isFalse();
        assertThat(result.getTrait()).isEqualTo(Trait.AGILITY);
        assertThat(result.getRange()).isEqualTo(Range.CLOSE);
        assertThat(result.getBurden()).isEqualTo(Burden.TWO_HANDED);
        verify(weaponRepository).save(any(Weapon.class));
    }

    @Test
    void updateWeapon_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateWeaponRequest request = UpdateWeaponRequest.builder()
                .name("Updated Name")
                .expansionId(1L)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(UpdateWeaponRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        when(weaponRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> weaponService.updateWeapon(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Weapon not found with id: 999");

        verify(weaponRepository, never()).save(any());
    }

    // ==================== DELETE WEAPON TESTS ====================

    @Test
    void deleteWeapon_ValidId_SoftDeletesWeapon() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon weapon = createTestWeapon(1L, "To Delete", expansion);

        when(weaponRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(weapon));

        // Act
        weaponService.deleteWeapon(1L);

        // Assert
        verify(weaponRepository).save(argThat(w -> w.getDeletedAt() != null));
    }

    @Test
    void deleteWeapon_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(weaponRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> weaponService.deleteWeapon(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Weapon not found with id: 999");

        verify(weaponRepository, never()).save(any());
    }

    // ==================== RESTORE WEAPON TESTS ====================

    @Test
    void restoreWeapon_DeletedWeapon_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon deletedWeapon = createTestWeapon(1L, "Deleted Weapon", expansion);
        deletedWeapon.setDeletedAt(LocalDateTime.now());

        when(weaponRepository.findById(1L)).thenReturn(Optional.of(deletedWeapon));
        when(weaponRepository.save(any(Weapon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WeaponResponse result = weaponService.restoreWeapon(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(weaponRepository).save(argThat(w -> w.getDeletedAt() == null));
    }

    @Test
    void restoreWeapon_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Weapon activeWeapon = createTestWeapon(1L, "Active Weapon", expansion);

        when(weaponRepository.findById(1L)).thenReturn(Optional.of(activeWeapon));

        // Act & Assert
        assertThatThrownBy(() -> weaponService.restoreWeapon(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Weapon with id 1 is not deleted");

        verify(weaponRepository, never()).save(any());
    }

    @Test
    void restoreWeapon_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(weaponRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> weaponService.restoreWeapon(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Weapon not found with id: 999");
    }

    // ==================== DAMAGE ROLL TESTS ====================

    @Test
    void createWeapon_WithProficiencyBasedDamage_CreatesCorrectly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateWeaponRequest request = CreateWeaponRequest.builder()
                .name("Magic Staff")
                .expansionId(1L)
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.KNOWLEDGE)
                .range(Range.FAR)
                .burden(Burden.TWO_HANDED)
                .damage(CreateWeaponRequest.DamageRollRequest.builder()
                        .diceCount(null) // Uses proficiency
                        .diceType(DiceType.D6)
                        .modifier(2)
                        .damageType(DamageType.MAGIC)
                        .build())
                .build();

        Weapon savedWeapon = Weapon.builder()
                .id(1L)
                .name("Magic Staff")
                .expansion(expansion)
                .tier(2)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.KNOWLEDGE)
                .range(Range.FAR)
                .burden(Burden.TWO_HANDED)
                .damage(DamageRoll.builder()
                        .diceCount(null)
                        .diceType(DiceType.D6)
                        .modifier(2)
                        .damageType(DamageType.MAGIC)
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(weaponRepository.save(any(Weapon.class))).thenReturn(savedWeapon);

        // Act
        WeaponResponse result = weaponService.createWeapon(request);

        // Assert
        assertThat(result.getDamage()).isNotNull();
        assertThat(result.getDamage().getDiceCount()).isNull();
        assertThat(result.getDamage().getDiceType()).isEqualTo(DiceType.D6);
        assertThat(result.getDamage().getNotation()).isEqualTo("d6+2 mag");
    }

    // ==================== HELPER METHODS ====================

    private Weapon createTestWeapon(Long id, String name, Expansion expansion) {
        return Weapon.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .tier(1)
                .isOfficial(true)
                .isPrimary(true)
                .trait(Trait.STRENGTH)
                .range(Range.MELEE)
                .burden(Burden.ONE_HANDED)
                .damage(DamageRoll.builder()
                        .diceCount(2)
                        .diceType(DiceType.D10)
                        .modifier(3)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
