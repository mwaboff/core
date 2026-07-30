package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateAdversaryRequest;
import com.aboff.core.model.dto.dh.request.UpdateAdversaryRequest;
import com.aboff.core.model.dto.dh.response.AdversaryResponse;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Experience;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.*;
import com.aboff.core.repository.dh.ExperienceRepository;
import com.aboff.core.repository.UserRepository;
import com.aboff.core.repository.dh.AdversaryRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for AdversaryService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality,
 * expand parameter, batch operations, copy functionality, and permission validation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdversaryServiceTest {

    @Mock
    private AdversaryRepository adversaryRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private Authentication authentication;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdversaryService adversaryService;

    private User regularUser;
    private User moderatorUser;
    private User adminUser;
    private User ownerUser;
    private CustomUserDetails regularUserDetails;
    private CustomUserDetails moderatorUserDetails;
    private CustomUserDetails adminUserDetails;
    private CustomUserDetails ownerUserDetails;
    private Expansion expansion;

    @BeforeEach
    void setUp() {
        // Set up test users
        regularUser = User.builder()
                .id(1L)
                .username("regularuser")
                .email("regular@test.com")
                .role(Role.USER)
                .build();

        moderatorUser = User.builder()
                .id(2L)
                .username("moderator")
                .email("moderator@test.com")
                .role(Role.MODERATOR)
                .build();

        adminUser = User.builder()
                .id(3L)
                .username("admin")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();

        ownerUser = User.builder()
                .id(4L)
                .username("owner")
                .email("owner@test.com")
                .role(Role.OWNER)
                .build();

        regularUserDetails = new CustomUserDetails(regularUser);
        moderatorUserDetails = new CustomUserDetails(moderatorUser);
        adminUserDetails = new CustomUserDetails(adminUser);
        ownerUserDetails = new CustomUserDetails(ownerUser);

        // Set up test expansion
        expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET ALL ADVERSARIES TESTS ====================

    @Test
    void getAllAdversaries_WithoutFilters_ReturnsPagedAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary1 = createTestAdversary(1L, "Goblin", expansion, regularUser);
        Adversary adversary2 = createTestAdversary(2L, "Orc", expansion, regularUser);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary1, adversary2));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, null, null, null, null, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Goblin");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Orc");
    }

    @Test
    void getAllAdversaries_WithTierFilter_ReturnsFilteredAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setTier(4);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), eq(4), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, 4, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTier()).isEqualTo(4);
    }

    @Test
    void getAllAdversaries_WithAdversaryTypeFilter_ReturnsFilteredAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Solo Dragon", expansion, regularUser);
        adversary.setAdversaryType(AdversaryType.SOLO);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), eq(AdversaryType.SOLO), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, null, AdversaryType.SOLO, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAdversaryType()).isEqualTo(AdversaryType.SOLO);
    }

    @Test
    void getAllAdversaries_WithNameFilter_ReturnsFilteredAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Shadow Goblin", expansion, regularUser);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), eq("goblin"), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, null, null, null, "goblin", null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Goblin");
    }

    @Test
    void getAllAdversaries_WithIsOfficialFilter_ReturnsFilteredAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Official Goblin", expansion, regularUser);
        adversary.setIsOfficial(true);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), eq(true), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, null, null, true, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsOfficial()).isTrue();
    }

    @Test
    void getAllAdversaries_WithExpansionIdFilter_ReturnsFilteredAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Expansion Goblin", expansion, regularUser);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, 1L, null, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
    }

    @Test
    void getAllAdversaries_WithLargePage_LimitsTo100() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of());
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        adversaryService.getAllAdversaries(0, 500, false, null, null, null, null, null, null, authentication);

        // Assert
        verify(adversaryRepository).findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllAdversaries_IncludeDeletedAsAdmin_ReturnsAllIncludingDeleted() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        Adversary adversary1 = createTestAdversary(1L, "Active Goblin", expansion, adminUser);
        Adversary adversary2 = createTestAdversary(2L, "Deleted Orc", expansion, adminUser);
        adversary2.setDeletedAt(LocalDateTime.now());

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary1, adversary2));
        when(adversaryRepository.findAllWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, true, null, null, null, null, null, null, authentication);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        verify(adversaryRepository).findAllWithFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void getAllAdversaries_IncludeDeletedAsNonAdmin_UsesAccessibleQuery() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.ADMIN)).thenReturn(false);

        Page<Adversary> adversaryPage = new PageImpl<>(List.of());
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        adversaryService.getAllAdversaries(0, 20, true, null, null, null, null, null, null, authentication);

        // Assert - Should use accessible query, not all query
        verify(adversaryRepository).findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        verify(adversaryRepository, never()).findAllWithFilters(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void getAllAdversaries_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CardCostTag costTag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        Page<Adversary> adversaryPage = new PageImpl<>(List.of(adversary));
        when(adversaryRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(adversaryPage);

        // Act
        PagedResponse<AdversaryResponse> result = adversaryService.getAllAdversaries(
                0, 20, false, null, null, null, null, null, "expansion,features", authentication);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getFeatures()).isNotNull();
    }

    // ==================== GET ADVERSARY BY ID TESTS ====================

    @Test
    void getAdversaryById_ValidId_ReturnsAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setIsPublic(true);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Goblin");
        assertThat(result.getTier()).isEqualTo(1);
        assertThat(result.getAdversaryType()).isEqualTo(AdversaryType.STANDARD);
    }

    @Test
    void getAdversaryById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.getAdversaryById(999L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 999");
    }

    @Test
    void getAdversaryById_PrivateAdversaryNotCreator_ThrowsEntityNotFoundException() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Private Goblin", expansion, otherUser);
        adversary.setIsOfficial(false);
        adversary.setIsPublic(false);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.getAdversaryById(1L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 1");
    }

    @Test
    void getAdversaryById_PrivateAdversaryAsCreator_ReturnsAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Private Goblin", expansion, regularUser);
        adversary.setIsOfficial(false);
        adversary.setIsPublic(false);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Private Goblin");
    }

    @Test
    void getAdversaryById_PrivateAdversaryAsModerator_ReturnsAdversary() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(moderatorUserDetails);

        Adversary adversary = createTestAdversary(1L, "Private Goblin", expansion, otherUser);
        adversary.setIsOfficial(false);
        adversary.setIsPublic(false);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));
        when(roleHierarchyService.hasRoleOrHigher(moderatorUser, Role.MODERATOR)).thenReturn(true);

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Private Goblin");
    }

    @Test
    void getAdversaryById_OfficialAdversary_AnyoneCanView() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Official Goblin", expansion, ownerUser);
        adversary.setIsOfficial(true);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, null, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsOfficial()).isTrue();
    }

    @Test
    void getAdversaryById_WithExpandCreator_ExpandsCreator() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setIsPublic(true);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "creator", authentication);

        // Assert
        assertThat(result.getCreator()).isNotNull();
        assertThat(result.getCreator().getUsername()).isEqualTo("regularuser");
    }

    // ==================== CREATE ADVERSARY TESTS ====================

    @Test
    void createAdversary_ValidRequest_CreatesAndReturnsAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = createTestAdversaryRequest();

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Adversary");
        assertThat(result.getIsOfficial()).isFalse();
        verify(adversaryRepository).save(any(Adversary.class));
    }

    @Test
    void createAdversary_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setExpansionId(999L);

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.createAdversary(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void createAdversary_InvalidThresholds_ThrowsIllegalArgumentException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setMajorThreshold(10);
        request.setSevereThreshold(5); // Severe less than major - invalid

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.createAdversary(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Severe threshold must be greater than or equal to major threshold");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void createAdversary_OmittedThresholds_PersistsNullNotZero() {
        // Arrange -- a "framework" stat block (e.g. Forlorne Lykona, Hope & Fear
        // p.143) has no Difficulty or Thresholds at all. createAdversary must not
        // silently coerce the omitted majorThreshold/severeThreshold to 0.
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = CreateAdversaryRequest.builder()
                .name("Forlorne Lykona (Framework)")
                .tier(2)
                .adversaryType(AdversaryType.STANDARD)
                .description("Framework description")
                .motivesAndTactics("Framework motives and tactics")
                .expansionId(1L)
                .isPublic(false)
                .build();
        // difficulty, majorThreshold, severeThreshold intentionally omitted

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        adversaryService.createAdversary(request, authentication);

        // Assert
        ArgumentCaptor<Adversary> captor = ArgumentCaptor.forClass(Adversary.class);
        verify(adversaryRepository).save(captor.capture());
        Adversary persisted = captor.getValue();

        assertThat(persisted.getDifficulty()).isNull();
        assertThat(persisted.getMajorThreshold()).isNull();
        assertThat(persisted.getSevereThreshold()).isNull();
    }

    @Test
    void createAdversariesBulk_OmittedThresholds_PersistsNullForEachRequest() {
        // Arrange -- bulk import is the mechanism the rulebook's adversaries
        // (including framework blocks) actually land through, so it must not
        // reintroduce the 0-defaulting behavior removed from createAdversary.
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = CreateAdversaryRequest.builder()
                .name("Forlorne Lykona (Framework)")
                .tier(2)
                .adversaryType(AdversaryType.STANDARD)
                .expansionId(1L)
                .isPublic(false)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        adversaryService.createAdversariesBulk(List.of(request), authentication);

        // Assert
        ArgumentCaptor<Adversary> captor = ArgumentCaptor.forClass(Adversary.class);
        verify(adversaryRepository).save(captor.capture());
        Adversary persisted = captor.getValue();

        assertThat(persisted.getMajorThreshold()).isNull();
        assertThat(persisted.getSevereThreshold()).isNull();
    }

    @Test
    void createAdversary_WithExperiences_AssociatesExperiences() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Experience experience = Experience.builder()
                .id(1L)
                .description("Combat Expert")
                .modifier(2)
                .build();

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setExperienceIds(Set.of(1L));

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);
        savedAdversary.setExperiences(new HashSet<>(Set.of(experience)));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(experienceRepository.findAllById(Set.of(1L))).thenReturn(List.of(experience));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result.getExperienceIds()).contains(1L);
    }

    @Test
    void createAdversary_WithFeatures_AssociatesFeatures() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .build();

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setFeatureIds(Set.of(1L));

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);
        savedAdversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(anyList(), isNull())).thenReturn(Set.of(feature));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result.getFeatureIds()).contains(1L);
    }

    @Test
    void createAdversary_WithOriginalAdversaryId_SetsOriginalAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary originalAdversary = createTestAdversary(10L, "Original Goblin", expansion, ownerUser);
        originalAdversary.setIsOfficial(true);

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setOriginalAdversaryId(10L);

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);
        savedAdversary.setOriginalAdversary(originalAdversary);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(originalAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result.getOriginalAdversaryId()).isEqualTo(10L);
    }

    @Test
    void createAdversary_WithDamage_SetsDamage() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setDamage(CreateAdversaryRequest.DamageRollRequest.builder()
                .diceCount(2)
                .diceType(DiceType.D8)
                .modifier(3)
                .damageType(DamageType.PHYSICAL)
                .build());

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);
        savedAdversary.setDamage(DamageRoll.builder()
                .diceCount(2)
                .diceType(DiceType.D8)
                .modifier(3)
                .damageType(DamageType.PHYSICAL)
                .build());

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result.getDamage()).isNotNull();
        assertThat(result.getDamage().getDiceCount()).isEqualTo(2);
        assertThat(result.getDamage().getDiceType()).isEqualTo(DiceType.D8);
    }

    // ==================== BULK CREATE ADVERSARIES TESTS ====================

    @Test
    void createAdversariesBulk_ValidRequests_CreatesAllAdversaries() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request1 = createTestAdversaryRequest();
        request1.setName("Goblin 1");

        CreateAdversaryRequest request2 = createTestAdversaryRequest();
        request2.setName("Goblin 2");

        Adversary savedAdversary1 = createTestAdversary(1L, "Goblin 1", expansion, regularUser);
        Adversary savedAdversary2 = createTestAdversary(2L, "Goblin 2", expansion, regularUser);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class)))
                .thenReturn(savedAdversary1)
                .thenReturn(savedAdversary2);

        // Act
        List<AdversaryResponse> result = adversaryService.createAdversariesBulk(
                List.of(request1, request2), authentication);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Goblin 1");
        assertThat(result.get(1).getName()).isEqualTo("Goblin 2");
    }

    @Test
    void createAdversariesBulk_InvalidRequest_ThrowsException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest invalidRequest = createTestAdversaryRequest();
        invalidRequest.setExpansionId(999L);

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.createAdversariesBulk(
                List.of(invalidRequest), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== UPDATE ADVERSARY TESTS ====================

    @Test
    void updateAdversary_ValidRequest_UpdatesAndReturnsAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary existingAdversary = createTestAdversary(1L, "Old Name", expansion, regularUser);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Name")
                .tier(2)
                .adversaryType(AdversaryType.BRUISER)
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getTier()).isEqualTo(2);
        assertThat(result.getAdversaryType()).isEqualTo(AdversaryType.BRUISER);
        verify(adversaryRepository).save(any(Adversary.class));
    }

    @Test
    void updateAdversary_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Name")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.updateAdversary(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 999");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void updateAdversary_OfficialAsNonOwner_ThrowsInsufficientPermissionsException() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);

        Adversary officialAdversary = createTestAdversary(1L, "Official Goblin", expansion, ownerUser);
        officialAdversary.setIsOfficial(true);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Name")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(officialAdversary));

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.updateAdversary(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("Only owners can modify official adversaries");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void updateAdversary_OfficialAsOwner_UpdatesSuccessfully() {
        // Arrange
        setupAuthenticationWith(ownerUserDetails);

        Adversary officialAdversary = createTestAdversary(1L, "Official Goblin", expansion, ownerUser);
        officialAdversary.setIsOfficial(true);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Official")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(officialAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Official");
    }

    @Test
    void updateAdversary_NonOfficialAsCreator_UpdatesSuccessfully() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "My Goblin", expansion, regularUser);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Goblin")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Goblin");
    }

    @Test
    void updateAdversary_NonOfficialAsModerator_UpdatesSuccessfully() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(moderatorUserDetails);

        Adversary adversary = createTestAdversary(1L, "Other's Goblin", expansion, otherUser);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Moderated Goblin")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
        when(roleHierarchyService.hasRoleOrHigher(moderatorUser, Role.MODERATOR)).thenReturn(true);
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Moderated Goblin");
    }

    @Test
    void updateAdversary_NonOfficialNotCreatorNotModerator_ThrowsInsufficientPermissionsException() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Other's Goblin", expansion, otherUser);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Attempted Update")
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.updateAdversary(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("You do not have permission to modify this adversary");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void updateAdversary_InvalidMarkedExceedsMax_ThrowsIllegalArgumentException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setHitPointMax(10);
        adversary.setHitPointMarked(0);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .hitPointMarked(20) // More than max
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.updateAdversary(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hit points marked cannot exceed hit points max");
    }

    @Test
    void updateAdversary_InvalidStressMarkedExceedsMax_ThrowsIllegalArgumentException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setStressMax(5);
        adversary.setStressMarked(0);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .stressMarked(10) // More than max
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.updateAdversary(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stress marked cannot exceed stress max");
    }

    @Test
    void updateAdversary_PartialUpdate_OnlyUpdatesProvidedFields() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary existingAdversary = createTestAdversary(1L, "Original Name", expansion, regularUser);
        existingAdversary.setDescription("Original Description");
        existingAdversary.setDifficulty(10);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .name("Updated Name")
                // Not updating description or difficulty
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Original Description");
        assertThat(result.getDifficulty()).isEqualTo(10);
    }

    @Test
    void updateAdversary_WithExperiences_UpdatesExperiences() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Experience newExperience = Experience.builder()
                .id(2L)
                .description("New Skill")
                .modifier(3)
                .build();

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);

        UpdateAdversaryRequest request = UpdateAdversaryRequest.builder()
                .experienceIds(Set.of(2L))
                .build();

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
        when(experienceRepository.findAllById(Set.of(2L))).thenReturn(List.of(newExperience));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> {
            Adversary saved = invocation.getArgument(0);
            saved.setExperiences(new HashSet<>(Set.of(newExperience)));
            return saved;
        });

        // Act
        AdversaryResponse result = adversaryService.updateAdversary(1L, request, authentication);

        // Assert
        assertThat(result.getExperienceIds()).contains(2L);
    }

    // ==================== DELETE ADVERSARY TESTS ====================

    @Test
    void deleteAdversary_ValidId_SoftDeletesAdversary() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "To Delete", expansion, regularUser);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act
        adversaryService.deleteAdversary(1L, authentication);

        // Assert
        verify(adversaryRepository).save(argThat(a -> a.getDeletedAt() != null));
    }

    @Test
    void deleteAdversary_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.deleteAdversary(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 999");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void deleteAdversary_OfficialAsNonOwner_ThrowsInsufficientPermissionsException() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);

        Adversary officialAdversary = createTestAdversary(1L, "Official Goblin", expansion, ownerUser);
        officialAdversary.setIsOfficial(true);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(officialAdversary));

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.deleteAdversary(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("Only owners can modify official adversaries");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void deleteAdversary_NonOfficialNotCreatorNotModerator_ThrowsInsufficientPermissionsException() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Other's Goblin", expansion, otherUser);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.deleteAdversary(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("You do not have permission to modify this adversary");

        verify(adversaryRepository, never()).save(any());
    }

    // ==================== RESTORE ADVERSARY TESTS ====================

    @Test
    void restoreAdversary_DeletedAdversaryAsAdmin_RestoresSuccessfully() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        Adversary deletedAdversary = createTestAdversary(1L, "Deleted Goblin", expansion, regularUser);
        deletedAdversary.setDeletedAt(LocalDateTime.now());

        when(adversaryRepository.findById(1L)).thenReturn(Optional.of(deletedAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdversaryResponse result = adversaryService.restoreAdversary(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(adversaryRepository).save(argThat(a -> a.getDeletedAt() == null));
    }

    @Test
    void restoreAdversary_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        Adversary activeAdversary = createTestAdversary(1L, "Active Goblin", expansion, regularUser);

        when(adversaryRepository.findById(1L)).thenReturn(Optional.of(activeAdversary));

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.restoreAdversary(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Adversary with id 1 is not deleted");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void restoreAdversary_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        when(adversaryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.restoreAdversary(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 999");
    }

    @Test
    void restoreAdversary_AsNonAdmin_ThrowsInsufficientPermissionsException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.ADMIN)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.restoreAdversary(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("Only admins can restore adversaries");

        verify(adversaryRepository, never()).findById(any());
    }

    // ==================== COPY ADVERSARY TESTS ====================

    @Test
    void copyAdversary_ValidId_CreatesCopy() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .build();

        Experience experience = Experience.builder()
                .id(1L)
                .description("Combat")
                .modifier(2)
                .build();

        Adversary original = createTestAdversary(1L, "Original Goblin", expansion, ownerUser);
        original.setIsOfficial(true);
        original.setFeatures(new HashSet<>(Set.of(feature)));
        original.setExperiences(new HashSet<>(Set.of(experience)));

        Adversary copy = createTestAdversary(2L, "Original Goblin (Copy)", expansion, regularUser);
        copy.setOriginalAdversary(original);
        copy.setIsOfficial(false);
        copy.setIsPublic(false);
        copy.setFeatures(new HashSet<>(Set.of(feature)));
        copy.setExperiences(new HashSet<>(Set.of(experience)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(original));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(copy);

        // Act
        AdversaryResponse result = adversaryService.copyAdversary(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("Original Goblin (Copy)");
        assertThat(result.getOriginalAdversaryId()).isEqualTo(1L);
        assertThat(result.getIsOfficial()).isFalse();
        assertThat(result.getIsPublic()).isFalse();
        assertThat(result.getCreatorId()).isEqualTo(regularUser.getId());
    }

    @Test
    void copyAdversary_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.copyAdversary(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 999");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void copyAdversary_PrivateNotAccessible_ThrowsEntityNotFoundException() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(regularUserDetails);

        Adversary privateAdversary = createTestAdversary(1L, "Private Goblin", expansion, otherUser);
        privateAdversary.setIsOfficial(false);
        privateAdversary.setIsPublic(false);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(privateAdversary));
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.MODERATOR)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> adversaryService.copyAdversary(1L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Adversary not found with id: 1");

        verify(adversaryRepository, never()).save(any());
    }

    @Test
    void copyAdversary_PublicAdversary_CreatesCopy() {
        // Arrange
        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        setupAuthenticationWith(regularUserDetails);

        Adversary publicAdversary = createTestAdversary(1L, "Public Goblin", expansion, otherUser);
        publicAdversary.setIsOfficial(false);
        publicAdversary.setIsPublic(true);

        Adversary copy = createTestAdversary(2L, "Public Goblin (Copy)", expansion, regularUser);
        copy.setOriginalAdversary(publicAdversary);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(publicAdversary));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(copy);

        // Act
        AdversaryResponse result = adversaryService.copyAdversary(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOriginalAdversaryId()).isEqualTo(1L);
    }

    // ==================== EXPAND PARAMETER TESTS ====================

    @Test
    void getAdversaryById_WithExpandExpansion_ExpandsExpansion() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setIsPublic(true);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "expansion", authentication);

        // Assert
        assertThat(result.getExpansion()).isNotNull();
        assertThat(result.getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    @Test
    void getAdversaryById_WithExpandOriginalAdversary_ExpandsOriginal() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Adversary original = createTestAdversary(10L, "Original", expansion, ownerUser);
        original.setIsOfficial(true);

        Adversary adversary = createTestAdversary(1L, "Copy", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setOriginalAdversary(original);

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "originalAdversary", authentication);

        // Assert
        assertThat(result.getOriginalAdversary()).isNotNull();
        assertThat(result.getOriginalAdversary().getName()).isEqualTo("Original");
    }

    @Test
    void getAdversaryById_WithExpandExperiences_ExpandsExperiences() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Experience experience = Experience.builder()
                .id(1L)
                .description("Combat Expert")
                .modifier(2)
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Goblin", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setExperiences(new HashSet<>(Set.of(experience)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));

        // Act
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "experiences", authentication);

        // Assert
        assertThat(result.getExperiences()).isNotNull();
        assertThat(result.getExperiences()).hasSize(1);
    }

    @Test
    void getAdversaryById_WithExpandFeatures_ExpandsFeatures() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CardCostTag costTag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "features", authentication);

        // Assert
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).hasSize(1);
        // costTagIds should always be present when features are expanded
        assertThat(result.getFeatures().iterator().next().getCostTagIds()).containsExactly(1L);
        // costTags should be null when costTags is not in expand
        assertThat(result.getFeatures().iterator().next().getCostTags()).isNull();
    }

    @Test
    void getAdversaryById_WithMultipleExpands_ExpandsAll() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CardCostTag costTag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "expansion,creator,features", authentication);

        // Assert
        assertThat(result.getExpansion()).isNotNull();
        assertThat(result.getCreator()).isNotNull();
        assertThat(result.getFeatures()).isNotNull();
    }

    @Test
    void getAdversaryById_WithExpandFeaturesAndCostTags_IncludesFullCostTags() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CardCostTag costTag = CardCostTag.builder()
                .id(1L)
                .label("3 Hope")
                .category(CostTagCategory.COST)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "features,costTags", authentication);

        // Assert
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).hasSize(1);
        var expandedFeature = result.getFeatures().iterator().next();
        assertThat(expandedFeature.getCostTagIds()).containsExactly(1L);
        assertThat(expandedFeature.getCostTags()).isNotNull();
        assertThat(expandedFeature.getCostTags()).hasSize(1);
        CardCostTagResponse tagResponse = expandedFeature.getCostTags().get(0);
        assertThat(tagResponse.getId()).isEqualTo(1L);
        assertThat(tagResponse.getLabel()).isEqualTo("3 Hope");
        assertThat(tagResponse.getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getAdversaryById_WithExpandFeaturesWithoutCostTags_IncludesCostTagIdsOnly() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CardCostTag costTag = CardCostTag.builder()
                .id(5L)
                .label("1/session")
                .category(CostTagCategory.LIMITATION)
                .createdAt(LocalDateTime.now())
                .build();

        Feature feature = Feature.builder()
                .id(2L)
                .name("Shield Wall")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(Set.of(costTag))
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Guardian", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "features", authentication);

        // Assert
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).hasSize(1);
        var expandedFeature = result.getFeatures().iterator().next();
        assertThat(expandedFeature.getCostTagIds()).containsExactly(5L);
        assertThat(expandedFeature.getCostTags()).isNull();
    }

    @Test
    void getAdversaryById_WithExpandFeaturesNullCostTags_HandlesNullGracefully() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(null)
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "features,costTags", authentication);

        // Assert
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).hasSize(1);
        var expandedFeature = result.getFeatures().iterator().next();
        assertThat(expandedFeature.getCostTagIds()).isNull();
        assertThat(expandedFeature.getCostTags()).isNull();
    }

    @Test
    void getAdversaryById_WithExpandFeaturesEmptyCostTags_ReturnsEmptyLists() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        Feature feature = Feature.builder()
                .id(1L)
                .name("Fire Breath")
                .featureType(FeatureType.OTHER)
                .expansion(expansion)
                .costTags(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        Adversary adversary = createTestAdversary(1L, "Dragon", expansion, regularUser);
        adversary.setIsPublic(true);
        adversary.setFeatures(new HashSet<>(Set.of(feature)));

        when(adversaryRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(adversary));
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
        AdversaryResponse result = adversaryService.getAdversaryById(1L, "features,costTags", authentication);

        // Assert
        assertThat(result.getFeatures()).isNotNull();
        assertThat(result.getFeatures()).hasSize(1);
        var expandedFeature = result.getFeatures().iterator().next();
        assertThat(expandedFeature.getCostTagIds()).isEmpty();
        assertThat(expandedFeature.getCostTags()).isEmpty();
    }

    // ==================== DAMAGE ROLL TESTS ====================

    @Test
    void createAdversary_WithoutDamage_CreatesAdversaryWithNoDamage() {
        // Arrange
        setupAuthenticationWith(regularUserDetails);

        CreateAdversaryRequest request = createTestAdversaryRequest();
        request.setDamage(null);

        Adversary savedAdversary = createTestAdversary(1L, "Test Adversary", expansion, regularUser);
        savedAdversary.setDamage(null);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(adversaryRepository.save(any(Adversary.class))).thenReturn(savedAdversary);

        // Act
        AdversaryResponse result = adversaryService.createAdversary(request, authentication);

        // Assert
        assertThat(result.getDamage()).isNull();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Sets up the authentication mock to return the specified user details.
     */
    private void setupAuthenticationWith(CustomUserDetails userDetails) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    /**
     * Creates a test adversary with default values.
     */
    private Adversary createTestAdversary(Long id, String name, Expansion expansion, User creator) {
        return Adversary.builder()
                .id(id)
                .name(name)
                .tier(1)
                .adversaryType(AdversaryType.STANDARD)
                .description("A test adversary")
                .motivesAndTactics("Attack on sight")
                .difficulty(10)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(20)
                .hitPointMarked(0)
                .stressMax(5)
                .stressMarked(0)
                .attackModifier(2)
                .weaponName("Claws")
                .attackRange(Range.MELEE)
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(false)
                .experiences(new HashSet<>())
                .features(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a test create adversary request with default values.
     */
    private CreateAdversaryRequest createTestAdversaryRequest() {
        return CreateAdversaryRequest.builder()
                .name("Test Adversary")
                .tier(1)
                .adversaryType(AdversaryType.STANDARD)
                .description("A test adversary")
                .motivesAndTactics("Attack on sight")
                .difficulty(10)
                .majorThreshold(5)
                .severeThreshold(10)
                .hitPointMax(20)
                .stressMax(5)
                .expansionId(1L)
                .isPublic(false)
                .build();
    }
}
