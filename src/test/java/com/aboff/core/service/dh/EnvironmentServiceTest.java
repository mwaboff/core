package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateEnvironmentRequest;
import com.aboff.core.model.dto.dh.request.UpdateEnvironmentRequest;
import com.aboff.core.model.dto.dh.response.EnvironmentResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.EnvironmentType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.EnvironmentRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnvironmentService.
 * Tests CRUD operations, bulk creation, pagination, soft deletion, restore,
 * permission validation, and the difficulty/difficultySpecial mutual-exclusivity rule.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

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

    @Mock
    private ContentAccessService contentAccessService;

    @InjectMocks
    private EnvironmentService environmentService;

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
        regularUser = User.builder().id(1L).username("regularuser").email("regular@test.com").role(Role.USER).build();
        moderatorUser = User.builder().id(2L).username("moderator").email("moderator@test.com").role(Role.MODERATOR).build();
        adminUser = User.builder().id(3L).username("admin").email("admin@test.com").role(Role.ADMIN).build();
        ownerUser = User.builder().id(4L).username("owner").email("owner@test.com").role(Role.OWNER).build();

        regularUserDetails = new CustomUserDetails(regularUser);
        moderatorUserDetails = new CustomUserDetails(moderatorUser);
        adminUserDetails = new CustomUserDetails(adminUser);
        ownerUserDetails = new CustomUserDetails(ownerUser);

        expansion = Expansion.builder()
                .id(1L)
                .name("Core Rulebook")
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Default: content is visible unless a test overrides this to exercise SRD redaction.
        lenient().when(contentAccessService.mayView(any(), any())).thenReturn(true);
    }

    // ==================== GET ALL ENVIRONMENTS TESTS ====================

    @Test
    void getAllEnvironments_WithoutFilters_ReturnsPagedEnvironments() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment1 = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        Environment environment2 = createTestEnvironment(2L, "Bustling Marketplace", expansion, regularUser, 10, null);

        Page<Environment> page = new PageImpl<>(List.of(environment1, environment2));
        when(environmentRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<EnvironmentResponse> result = environmentService.getAllEnvironments(
                0, 20, false, null, null, null, null, null, null, authentication);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Abandoned Grove");
    }

    @Test
    void getAllEnvironments_WithTypeFilter_ReturnsFiltered() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Ambushed", expansion, regularUser, null, "Special");
        environment.setEnvironmentType(EnvironmentType.EVENT);

        Page<Environment> page = new PageImpl<>(List.of(environment));
        when(environmentRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), eq(EnvironmentType.EVENT), isNull(), isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<EnvironmentResponse> result = environmentService.getAllEnvironments(
                0, 20, false, null, null, EnvironmentType.EVENT, null, null, null, authentication);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEnvironmentType()).isEqualTo(EnvironmentType.EVENT);
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void getEnvironmentById_Existing_ReturnsEnvironment() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        environment.setIsPublic(true);
        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));

        EnvironmentResponse result = environmentService.getEnvironmentById(1L, null, authentication);

        assertThat(result.getName()).isEqualTo("Abandoned Grove");
        assertThat(result.getDifficulty()).isEqualTo(11);
        assertThat(result.getDifficultySpecial()).isNull();
    }

    @Test
    void getEnvironmentById_NotFound_ThrowsEntityNotFoundException() {
        setupAuthenticationWith(regularUserDetails);
        when(environmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> environmentService.getEnvironmentById(99L, null, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getEnvironmentById_SpecialDifficulty_ReturnsDifficultySpecialAndNullDifficulty() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Ambushed", expansion, regularUser, null,
                "Special (see \"Relative Strength\")");
        environment.setIsPublic(true);
        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));

        EnvironmentResponse result = environmentService.getEnvironmentById(1L, null, authentication);

        assertThat(result.getDifficulty()).isNull();
        assertThat(result.getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
    }

    // ==================== SRD CONTENT GATING TESTS ====================

    @Test
    void getEnvironmentById_RestrictedNonSrdContent_ReturnsRedactedStub() {
        // Arrange -- an official, non-SRD environment the caller may not view in full
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Paid Expansion Ruin", expansion, regularUser, 11, null);
        environment.setIsOfficial(true);
        environment.setIsPublic(true);
        environment.setSrd(false);

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));
        when(contentAccessService.mayView(true, false)).thenReturn(false);

        EnvironmentResponse result = environmentService.getEnvironmentById(1L, null, authentication);

        assertThat(result.getRestricted()).isTrue();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getExpansionName()).isEqualTo("Core Rulebook");
        assertThat(result.getName()).isNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getIsOfficial()).isNull();
        assertThat(result.getSrd()).isNull();
    }

    @Test
    void getAllEnvironments_ForwardsIncludeNonSrdToRepository() {
        setupAuthenticationWith(regularUserDetails);
        when(contentAccessService.includeNonSrd()).thenReturn(false);

        Page<Environment> page = new PageImpl<>(List.of());
        when(environmentRepository.findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(page);

        environmentService.getAllEnvironments(0, 20, false, null, null, null, null, null, null, authentication);

        verify(environmentRepository).findAccessibleWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), any(Pageable.class));
    }

    // ==================== CREATE ENVIRONMENT TESTS ====================

    @Test
    void createEnvironment_WithNumericDifficulty_CreatesAndReturnsEnvironment() {
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request = createTestEnvironmentRequest();
        request.setDifficulty(11);
        request.setDifficultySpecial(null);

        Environment savedEnvironment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(environmentRepository.save(any(Environment.class))).thenReturn(savedEnvironment);

        EnvironmentResponse result = environmentService.createEnvironment(request, authentication);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDifficulty()).isEqualTo(11);
        verify(environmentRepository).save(any(Environment.class));
    }

    @Test
    void createEnvironment_WithSpecialDifficulty_PersistsDifficultySpecialAndNullDifficulty() {
        // Acceptance-critical: a create with no numeric difficulty at all must succeed.
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request = createTestEnvironmentRequest();
        request.setDifficulty(null);
        request.setDifficultySpecial("Special (see \"Relative Strength\")");

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(environmentRepository.save(any(Environment.class))).thenAnswer(invocation -> {
            Environment arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        EnvironmentResponse result = environmentService.createEnvironment(request, authentication);

        assertThat(result.getDifficulty()).isNull();
        assertThat(result.getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");

        ArgumentCaptor<Environment> captor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDifficulty()).isNull();
        assertThat(captor.getValue().getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
    }

    @Test
    void createEnvironment_BothDifficultyFieldsProvided_ThrowsIllegalArgumentException() {
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request = createTestEnvironmentRequest();
        request.setDifficulty(10);
        request.setDifficultySpecial("Special (see \"Relative Strength\")");

        assertThatThrownBy(() -> environmentService.createEnvironment(request, authentication))
                .isInstanceOf(IllegalArgumentException.class);

        verify(environmentRepository, never()).save(any());
    }

    @Test
    void createEnvironment_NeitherDifficultyFieldProvided_ThrowsIllegalArgumentException() {
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request = createTestEnvironmentRequest();
        request.setDifficulty(null);
        request.setDifficultySpecial(null);

        assertThatThrownBy(() -> environmentService.createEnvironment(request, authentication))
                .isInstanceOf(IllegalArgumentException.class);

        verify(environmentRepository, never()).save(any());
    }

    @Test
    void createEnvironment_ExpansionNotFound_ThrowsEntityNotFoundException() {
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request = createTestEnvironmentRequest();
        request.setExpansionId(999L);

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> environmentService.createEnvironment(request, authentication))
                .isInstanceOf(EntityNotFoundException.class);

        verify(environmentRepository, never()).save(any());
    }

    // ==================== BULK CREATE TESTS ====================

    @Test
    void createEnvironmentsBulk_CreatesAllRequestedEnvironments() {
        setupAuthenticationWith(regularUserDetails);

        CreateEnvironmentRequest request1 = createTestEnvironmentRequest();
        request1.setName("Abandoned Grove");
        request1.setDifficulty(11);

        CreateEnvironmentRequest request2 = createTestEnvironmentRequest();
        request2.setName("Ambushed");
        request2.setDifficulty(null);
        request2.setDifficultySpecial("Special (see \"Relative Strength\")");

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(environmentRepository.save(any(Environment.class))).thenAnswer(invocation -> {
            Environment arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        List<EnvironmentResponse> results = environmentService.createEnvironmentsBulk(
                List.of(request1, request2), authentication);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Abandoned Grove");
        assertThat(results.get(1).getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
        verify(environmentRepository, times(2)).save(any(Environment.class));
    }

    // ==================== UPDATE ENVIRONMENT TESTS ====================

    @Test
    void updateEnvironment_AsCreator_UpdatesEnvironment() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder().name("Reclaimed Grove").build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);

        EnvironmentResponse result = environmentService.updateEnvironment(1L, request, authentication);

        assertThat(result.getName()).isEqualTo("Reclaimed Grove");
    }

    @Test
    void updateEnvironment_SwitchNumericToSpecial_ClearsDifficulty() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Ambushers", expansion, regularUser, 12, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .difficultySpecial("Special (see \"Relative Strength\")")
                .clearDifficulty(true)
                .build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);

        EnvironmentResponse result = environmentService.updateEnvironment(1L, request, authentication);

        assertThat(result.getDifficulty()).isNull();
        assertThat(result.getDifficultySpecial()).isEqualTo("Special (see \"Relative Strength\")");
    }

    @Test
    void updateEnvironment_ResultingInBothDifficultyFields_ThrowsIllegalArgumentException() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        // Setting difficultySpecial without clearing difficulty would leave both set.
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder()
                .difficultySpecial("Special (see \"Relative Strength\")")
                .build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));

        assertThatThrownBy(() -> environmentService.updateEnvironment(1L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class);

        verify(environmentRepository, never()).save(any());
    }

    @Test
    void updateEnvironment_NonCreatorNonModerator_ThrowsInsufficientPermissionsException() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Admin Grove", expansion, adminUser, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder().name("Hacked Grove").build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));

        assertThatThrownBy(() -> environmentService.updateEnvironment(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateEnvironment_OfficialAsNonOwner_ThrowsInsufficientPermissionsException() {
        setupAuthenticationWith(adminUserDetails);

        Environment environment = createTestEnvironment(1L, "Official Grove", expansion, ownerUser, 11, null);
        environment.setIsOfficial(true);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder().name("Unofficial Grove").build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));

        assertThatThrownBy(() -> environmentService.updateEnvironment(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void updateEnvironment_AsModerator_UpdatesEnvironment() {
        setupAuthenticationWith(moderatorUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(moderatorUser, Role.MODERATOR)).thenReturn(true);

        Environment environment = createTestEnvironment(1L, "User Grove", expansion, regularUser, 11, null);
        UpdateEnvironmentRequest request = UpdateEnvironmentRequest.builder().name("Moderated Grove").build();

        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);

        EnvironmentResponse result = environmentService.updateEnvironment(1L, request, authentication);

        assertThat(result.getName()).isEqualTo("Moderated Grove");
    }

    // ==================== DELETE ENVIRONMENT TESTS ====================

    @Test
    void deleteEnvironment_AsCreator_SoftDeletesEnvironment() {
        setupAuthenticationWith(regularUserDetails);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        when(environmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);

        environmentService.deleteEnvironment(1L, authentication);

        assertThat(environment.getDeletedAt()).isNotNull();
        verify(environmentRepository).save(environment);
    }

    @Test
    void deleteEnvironment_NotFound_ThrowsEntityNotFoundException() {
        setupAuthenticationWith(regularUserDetails);
        when(environmentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> environmentService.deleteEnvironment(99L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== RESTORE ENVIRONMENT TESTS ====================

    @Test
    void restoreEnvironment_AsAdmin_RestoresEnvironment() {
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        environment.softDelete();

        when(environmentRepository.findById(1L)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);

        EnvironmentResponse result = environmentService.restoreEnvironment(1L, authentication);

        assertThat(environment.getDeletedAt()).isNull();
        assertThat(result).isNotNull();
    }

    @Test
    void restoreEnvironment_AsRegularUser_ThrowsInsufficientPermissionsException() {
        setupAuthenticationWith(regularUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(regularUser, Role.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> environmentService.restoreEnvironment(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(environmentRepository, never()).findById(any());
    }

    @Test
    void restoreEnvironment_NotDeleted_ThrowsIllegalStateException() {
        setupAuthenticationWith(adminUserDetails);
        when(roleHierarchyService.hasRoleOrHigher(adminUser, Role.ADMIN)).thenReturn(true);

        Environment environment = createTestEnvironment(1L, "Abandoned Grove", expansion, regularUser, 11, null);
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(environment));

        assertThatThrownBy(() -> environmentService.restoreEnvironment(1L, authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== TEST HELPERS ====================

    private void setupAuthenticationWith(CustomUserDetails userDetails) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    private Environment createTestEnvironment(Long id, String name, Expansion expansion, User creator,
                                               Integer difficulty, String difficultySpecial) {
        return Environment.builder()
                .id(id)
                .name(name)
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .description("A test environment")
                .impulses("Test impulses")
                .difficulty(difficulty)
                .difficultySpecial(difficultySpecial)
                .potentialAdversaries("Test adversaries")
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(false)
                .isPublic(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CreateEnvironmentRequest createTestEnvironmentRequest() {
        return CreateEnvironmentRequest.builder()
                .name("Abandoned Grove")
                .tier(1)
                .environmentType(EnvironmentType.EXPLORATION)
                .description("A test environment")
                .impulses("Test impulses")
                .difficulty(11)
                .potentialAdversaries("Test adversaries")
                .expansionId(1L)
                .isOfficial(false)
                .isPublic(false)
                .build();
    }
}
