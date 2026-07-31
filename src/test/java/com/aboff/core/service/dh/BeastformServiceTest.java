package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateBeastformRequest;
import com.aboff.core.model.dto.dh.request.UpdateBeastformRequest;
import com.aboff.core.model.dto.dh.response.BeastformResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.DiceType;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.BeastformRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BeastformService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality,
 * expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BeastformServiceTest {

    @Mock
    private BeastformRepository beastformRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BeastformService beastformService;

    private Expansion expansion;
    private User creator;
    private CustomUserDetails creatorDetails;

    @BeforeEach
    void setUp() {
        expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        creator = User.builder().id(1L).username("admin").email("admin@test.com").role(Role.ADMIN).build();
        creatorDetails = new CustomUserDetails(creator);
        when(authentication.getPrincipal()).thenReturn(creatorDetails);
    }

    // ==================== GET ALL BEASTFORMS TESTS ====================

    @Test
    void getAllBeastforms_WithoutFilters_ReturnsPagedBeastforms() {
        // Arrange
        Beastform beastform1 = createTestBeastform(1L, "Wolf", expansion);
        Beastform beastform2 = createTestBeastform(2L, "Bear", expansion);

        Page<Beastform> beastformPage = new PageImpl<>(List.of(beastform1, beastform2));
        when(beastformRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(beastformPage);

        // Act
        PagedResponse<BeastformResponse> result = beastformService.getAllBeastforms(0, 20, false, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Wolf");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Bear");
    }

    @Test
    void getAllBeastforms_WithIncludeDeleted_UsesFindAllWithFilters() {
        // Arrange
        Beastform beastform = createTestBeastform(1L, "Wolf", expansion);
        Page<Beastform> beastformPage = new PageImpl<>(List.of(beastform));
        when(beastformRepository.findAllWithFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(beastformPage);

        // Act
        PagedResponse<BeastformResponse> result = beastformService.getAllBeastforms(0, 20, true, null, null, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(beastformRepository).findAllWithFilters(isNull(), isNull(), isNull(), any(Pageable.class));
        verify(beastformRepository, never()).findByDeletedAtIsNullAndFilters(any(), any(), any(), any());
    }

    @Test
    void getAllBeastforms_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Beastform> beastformPage = new PageImpl<>(List.of());
        when(beastformRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(beastformPage);

        // Act
        beastformService.getAllBeastforms(0, 500, false, null, null, null, null);

        // Assert
        verify(beastformRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(), isNull(), argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    void getAllBeastforms_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Feature feature = Feature.builder().id(1L).name("Keen Senses").featureType(FeatureType.OTHER)
                .expansion(expansion).createdAt(LocalDateTime.now()).build();

        Beastform beastform = createTestBeastform(1L, "Wolf", expansion);
        beastform.setFeatures(Set.of(feature));

        Page<Beastform> beastformPage = new PageImpl<>(List.of(beastform));
        when(beastformRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(beastformPage);

        // Act
        PagedResponse<BeastformResponse> result = beastformService.getAllBeastforms(0, 20, false, null, null, null, "expansion,features");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatureIds()).containsExactly(1L);
    }

    // ==================== GET BEASTFORM BY ID TESTS ====================

    @Test
    void getBeastformById_ValidId_ReturnsBeastform() {
        // Arrange
        Beastform beastform = createTestBeastform(1L, "Wolf", expansion);
        when(beastformRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(beastform));

        // Act
        BeastformResponse result = beastformService.getBeastformById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wolf");
        assertThat(result.getAttackTrait()).isEqualTo(Trait.AGILITY);
        assertThat(result.getAttackRange()).isEqualTo(Range.MELEE);
    }

    @Test
    void getBeastformById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(beastformRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> beastformService.getBeastformById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Beastform not found with id: 999");
    }

    // ==================== CREATE BEASTFORM TESTS ====================

    @Test
    void createBeastform_ValidRequest_CreatesAndReturnsBeastform() {
        // Arrange
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(1L)
                .isOfficial(true)
                .evasion(2)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceCount(1)
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        Beastform savedBeastform = createTestBeastform(1L, "Wolf", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(beastformRepository.save(any(Beastform.class))).thenReturn(savedBeastform);

        // Act
        BeastformResponse result = beastformService.createBeastform(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Wolf");
        assertThat(result.getEvasion()).isEqualTo(2);
        assertThat(result.getTier()).isEqualTo(1);
        verify(beastformRepository).save(argThat(b -> b.getEvasion() == 2 && b.getTier() == 1));
    }

    @Test
    void createBeastform_EvasionNull_StaysNull() {
        // Arrange
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(1L)
                .isOfficial(true)
                .evasion(null)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(beastformRepository.save(any(Beastform.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        beastformService.createBeastform(request, authentication);

        // Assert: no null-coalescing -- a request with evasion omitted/null persists as
        // null, not a manufactured 0 (see Beastform.evasion for the rationale).
        verify(beastformRepository).save(argThat(b -> b.getEvasion() == null));
    }

    @Test
    void createBeastform_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(999L)
                .isOfficial(true)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> beastformService.createBeastform(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(beastformRepository, never()).save(any());
    }

    @Test
    void createBeastform_WithFeature_AttachesFeature() {
        // Arrange
        Feature feature = Feature.builder().id(1L).name("Keen Senses").featureType(FeatureType.OTHER).expansion(expansion).build();

        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(1L)
                .isOfficial(true)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .featureIds(List.of(1L))
                .build();

        Beastform savedBeastform = createTestBeastform(1L, "Wolf", expansion);
        savedBeastform.setFeatures(Set.of(feature));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(beastformRepository.save(any(Beastform.class))).thenReturn(savedBeastform);

        // Act
        BeastformResponse result = beastformService.createBeastform(request, authentication);

        // Assert
        assertThat(result.getFeatureIds()).containsExactly(1L);
    }

    @Test
    void createBeastform_WithOriginalBeastform_LinksOriginal() {
        // Arrange
        Beastform original = createTestBeastform(1L, "Wolf", expansion);

        CreateBeastformRequest request = CreateBeastformRequest.builder()
                .name("Custom Wolf")
                .expansionId(1L)
                .isOfficial(false)
                .isPublic(true)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .originalBeastformId(1L)
                .build();

        Beastform savedBeastform = createTestBeastform(2L, "Custom Wolf", expansion);
        savedBeastform.setOriginalBeastform(original);
        savedBeastform.setIsOfficial(false);
        savedBeastform.setIsPublic(true);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(beastformRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(original));
        when(beastformRepository.save(any(Beastform.class))).thenReturn(savedBeastform);

        // Act
        BeastformResponse result = beastformService.createBeastform(request, authentication);

        // Assert
        assertThat(result.getOriginalBeastformId()).isEqualTo(1L);
        assertThat(result.getIsPublic()).isTrue();
    }

    // ==================== CREATE BEASTFORMS BULK TESTS ====================

    @Test
    void createBeastformsBulk_ValidRequests_CreatesAndReturnsBeastforms() {
        // Arrange
        CreateBeastformRequest request1 = CreateBeastformRequest.builder()
                .name("Wolf")
                .expansionId(1L)
                .isOfficial(true)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        CreateBeastformRequest request2 = CreateBeastformRequest.builder()
                .name("Bear")
                .expansionId(1L)
                .isOfficial(true)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.STRENGTH)
                .damage(CreateBeastformRequest.DamageRollRequest.builder()
                        .diceType(DiceType.D10)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .build();

        Beastform savedBeastform1 = createTestBeastform(1L, "Wolf", expansion);
        Beastform savedBeastform2 = createTestBeastform(2L, "Bear", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(beastformRepository.save(any(Beastform.class))).thenReturn(savedBeastform1, savedBeastform2);

        // Act
        List<BeastformResponse> results = beastformService.createBeastformsBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Wolf");
        assertThat(results.get(1).getName()).isEqualTo("Bear");
        verify(beastformRepository, times(2)).save(any(Beastform.class));
    }

    // ==================== UPDATE BEASTFORM TESTS ====================

    @Test
    void updateBeastform_ValidRequest_UpdatesAndReturnsBeastform() {
        // Arrange
        Beastform existingBeastform = createTestBeastform(1L, "Old Name", expansion);

        UpdateBeastformRequest request = UpdateBeastformRequest.builder()
                .name("Updated Name")
                .evasion(3)
                .tier(2)
                .attackRange(Range.CLOSE)
                .attackTrait(Trait.STRENGTH)
                .damage(UpdateBeastformRequest.DamageRollRequest.builder()
                        .diceCount(2)
                        .diceType(DiceType.D8)
                        .modifier(1)
                        .damageType(DamageType.MAGIC)
                        .build())
                .build();

        when(beastformRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingBeastform));
        when(beastformRepository.save(any(Beastform.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BeastformResponse result = beastformService.updateBeastform(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getEvasion()).isEqualTo(3);
        assertThat(result.getTier()).isEqualTo(2);
        assertThat(result.getAttackRange()).isEqualTo(Range.CLOSE);
        assertThat(result.getAttackTrait()).isEqualTo(Trait.STRENGTH);
        verify(beastformRepository).save(any(Beastform.class));
    }

    @Test
    void updateBeastform_NullEvasionAndTier_LeavesExistingValuesUnchanged() {
        // Arrange
        Beastform existingBeastform = createTestBeastform(1L, "Wolf", expansion);

        UpdateBeastformRequest request = UpdateBeastformRequest.builder()
                .name("Renamed Wolf")
                .build();

        when(beastformRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingBeastform));
        when(beastformRepository.save(any(Beastform.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BeastformResponse result = beastformService.updateBeastform(1L, request, authentication);

        // Assert
        assertThat(result.getEvasion()).isEqualTo(2);
        assertThat(result.getTier()).isEqualTo(1);
    }

    @Test
    void updateBeastform_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateBeastformRequest request = UpdateBeastformRequest.builder().name("Updated Name").build();

        when(beastformRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> beastformService.updateBeastform(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Beastform not found with id: 999");

        verify(beastformRepository, never()).save(any());
    }

    // ==================== DELETE BEASTFORM TESTS ====================

    @Test
    void deleteBeastform_ValidId_SoftDeletesBeastform() {
        // Arrange
        Beastform beastform = createTestBeastform(1L, "To Delete", expansion);
        when(beastformRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(beastform));

        // Act
        beastformService.deleteBeastform(1L, authentication);

        // Assert
        verify(beastformRepository).save(argThat(b -> b.getDeletedAt() != null));
    }

    @Test
    void deleteBeastform_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(beastformRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> beastformService.deleteBeastform(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Beastform not found with id: 999");

        verify(beastformRepository, never()).save(any());
    }

    // ==================== RESTORE BEASTFORM TESTS ====================

    @Test
    void restoreBeastform_DeletedBeastform_RestoresSuccessfully() {
        // Arrange
        Beastform deletedBeastform = createTestBeastform(1L, "Deleted Beastform", expansion);
        deletedBeastform.setDeletedAt(LocalDateTime.now());

        when(beastformRepository.findById(1L)).thenReturn(Optional.of(deletedBeastform));
        when(beastformRepository.save(any(Beastform.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BeastformResponse result = beastformService.restoreBeastform(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(beastformRepository).save(argThat(b -> b.getDeletedAt() == null));
    }

    @Test
    void restoreBeastform_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Beastform activeBeastform = createTestBeastform(1L, "Active Beastform", expansion);
        when(beastformRepository.findById(1L)).thenReturn(Optional.of(activeBeastform));

        // Act & Assert
        assertThatThrownBy(() -> beastformService.restoreBeastform(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Beastform with id 1 is not deleted");

        verify(beastformRepository, never()).save(any());
    }

    @Test
    void restoreBeastform_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(beastformRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> beastformService.restoreBeastform(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Beastform not found with id: 999");
    }

    // ==================== HELPER METHODS ====================

    private Beastform createTestBeastform(Long id, String name, Expansion expansion) {
        return Beastform.builder()
                .id(id)
                .name(name)
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(true)
                .isPublic(false)
                .evasion(2)
                .tier(1)
                .attackRange(Range.MELEE)
                .attackTrait(Trait.AGILITY)
                .damage(DamageRoll.builder()
                        .diceCount(1)
                        .diceType(DiceType.D6)
                        .damageType(DamageType.PHYSICAL)
                        .build())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
