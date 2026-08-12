package com.aboff.core.service.dh;

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.model.dto.dh.request.CreateSubclassPathRequest;
import com.aboff.core.model.dto.dh.request.SubclassPathInput;
import com.aboff.core.model.dto.dh.request.UpdateSubclassPathRequest;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.SubclassPathResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.SubclassCard;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SubclassLevel;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.SubclassCardRepository;
import com.aboff.core.repository.dh.SubclassPathRepository;
import com.aboff.core.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import com.aboff.core.service.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

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
 * Unit tests for SubclassPathService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality,
 * expand parameter, bulk operations, filtering, find-or-create, and path resolution.
 */
@ExtendWith(MockitoExtension.class)
class SubclassPathServiceTest {

    @Mock
    private SubclassPathRepository subclassPathRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private SubclassCardRepository subclassCardRepository;

    @Mock
    private ClassService classService;

    @Mock
    private DomainService domainService;

    @Mock
    private ContentAccessService contentAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SubclassPathService subclassPathService;

    private final User defaultUser = User.builder().id(1L).username("tester").role(Role.USER).build();

    @BeforeEach
    void stubDefaultVisibility() {
        // Every list call resolves includeDeleted/includeNonSrd through the SRD gate first;
        // default to the ordinary (non-deleted, full-visibility) browse path unless a test
        // overrides it.
        lenient().when(contentAccessService.resolveIncludeDeleted(anyBoolean())).thenReturn(false);
        lenient().when(contentAccessService.includeNonSrd()).thenReturn(true);
        // toResponse redacts anything mayView() rejects; default to visible so existing
        // assertions on full response fields keep working. Redaction itself is covered by a
        // dedicated test below.
        lenient().when(contentAccessService.mayView(any(), any())).thenReturn(true);
        // createSubclassPath/createSubclassPathsBulk/updateSubclassPath always resolve
        // currentUser(authentication) now (for the srd field), so every test needs a principal.
        lenient().when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(defaultUser));
        // Default a non-admin caller's requested srd to false; tests exercising the srd cascade
        // override this per-test.
        lenient().when(contentAccessService.resolveSrd(any(), any())).thenReturn(false);
        // create/updateSubclassPath always cascade srd to the path's cards after save; default
        // to no cards so that cascade is a no-op unless a test stubs otherwise.
        lenient().when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(anyLong()))
                .thenReturn(List.of());
    }

    // ==================== GET ALL TESTS ====================

    @Test
    void getAllSubclassPaths_WithoutFilters_ReturnsPagedPaths() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path1 = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        SubclassPath path2 = SubclassPath.builder()
                .id(2L)
                .name("Warden of the Elements")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassPath> pathPage = new PageImpl<>(List.of(path1, path2));
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(pathPage);

        // Act
        PagedResponse<SubclassPathResponse> result = subclassPathService.getAllSubclassPaths(0, 20, false, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Warden of Renewal");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Warden of the Elements");
    }

    @Test
    void getAllSubclassPaths_WithClassIdFilter_ReturnsFilteredPaths() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassPath> pathPage = new PageImpl<>(List.of(path));
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(eq(1L), anyBoolean(), any(Pageable.class)))
                .thenReturn(pathPage);

        // Act
        PagedResponse<SubclassPathResponse> result = subclassPathService.getAllSubclassPaths(0, 20, false, 1L, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssociatedClassId()).isEqualTo(1L);
        verify(subclassPathRepository).findByDeletedAtIsNullAndFilters(eq(1L), anyBoolean(), any(Pageable.class));
    }

    @Test
    void getAllSubclassPaths_WithLargePage_LimitsTo100() {
        // Arrange
        Page<SubclassPath> pathPage = new PageImpl<>(List.of());
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(pathPage);

        // Act
        subclassPathService.getAllSubclassPaths(0, 500, false, null, null);

        // Assert
        verify(subclassPathRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
                anyBoolean(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllSubclassPaths_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).createdAt(LocalDateTime.now()).build();
        Domain domain = Domain.builder().id(1L).name("Sage").expansion(expansion).createdAt(LocalDateTime.now()).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(domain))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SubclassPath> pathPage = new PageImpl<>(List.of(path));
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), anyBoolean(), any(Pageable.class)))
                .thenReturn(pathPage);
        when(classService.toResponse(any(Class.class), any())).thenAnswer(invocation -> {
            Class c = invocation.getArgument(0);
            return ClassResponse.builder().id(c.getId()).name(c.getName()).build();
        });
        when(domainService.toResponse(any(Domain.class), any())).thenAnswer(invocation -> {
            Domain d = invocation.getArgument(0);
            return DomainResponse.builder().id(d.getId()).name(d.getName()).build();
        });

        // Act
        PagedResponse<SubclassPathResponse> result = subclassPathService.getAllSubclassPaths(0, 20, false, null, "associatedClass,associatedDomains,expansion");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssociatedClass()).isNotNull();
        assertThat(result.getContent().get(0).getAssociatedDomains()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void getSubclassPathById_ValidId_ReturnsPath() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(path));

        // Act
        SubclassPathResponse result = subclassPathService.getSubclassPathById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
    }

    @Test
    void getSubclassPathById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassPathRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.getSubclassPathById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassPath not found with id: 999");
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createSubclassPath_ValidRequest_CreatesAndReturnsPath() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        SubclassPathResponse result = subclassPathService.createSubclassPath(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void createSubclassPath_WithSpellcastingTrait_CreatesPathWithTraitInfo() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .spellcastingTrait(Trait.INSTINCT)
                .build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .spellcastingTrait(Trait.INSTINCT)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        SubclassPathResponse result = subclassPathService.createSubclassPath(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSpellcastingTrait()).isNotNull();
        assertThat(result.getSpellcastingTrait().getTrait()).isEqualTo(Trait.INSTINCT);
        assertThat(result.getSpellcastingTrait().getDescription()).isEqualTo(Trait.INSTINCT.getDescription());
        assertThat(result.getSpellcastingTrait().getExamples()).isEqualTo(Trait.INSTINCT.getExamples());
        verify(subclassPathRepository).save(argThat(path -> path.getSpellcastingTrait() == Trait.INSTINCT));
    }

    @Test
    void createSubclassPath_WithDomains_CreatesPathWithDomains() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        Domain domain1 = Domain.builder().id(1L).name("Sage").expansion(expansion).build();
        Domain domain2 = Domain.builder().id(2L).name("Arcana").expansion(expansion).build();

        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .associatedDomainIds(List.of(1L, 2L))
                .build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(domain1, domain2))
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L))).thenReturn(List.of(domain1, domain2));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        SubclassPathResponse result = subclassPathService.createSubclassPath(request, authentication);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAssociatedDomainIds()).hasSize(2);
        verify(domainRepository).findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L));
    }

    @Test
    void createSubclassPath_ClassNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(999L)
                .expansionId(1L)
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.createSubclassPath(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");

        verify(subclassPathRepository, never()).save(any());
    }

    @Test
    void createSubclassPath_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        Class clazz = Class.builder().id(1L).name("Druid").expansion(
                Expansion.builder().id(1L).name("Core Rulebook").build()
        ).startingEvasion(9).startingHitPoints(16).build();

        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(999L)
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.createSubclassPath(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(subclassPathRepository, never()).save(any());
    }

    // ==================== BULK CREATE TESTS ====================

    @Test
    void createSubclassPathsBulk_ValidRequests_CreatesAndReturnsPaths() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        CreateSubclassPathRequest request1 = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .build();

        CreateSubclassPathRequest request2 = CreateSubclassPathRequest.builder()
                .name("Warden of the Elements")
                .associatedClassId(1L)
                .expansionId(1L)
                .build();

        SubclassPath savedPath1 = SubclassPath.builder().id(1L).name("Warden of Renewal")
                .associatedClass(clazz).expansion(expansion).associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now()).build();

        SubclassPath savedPath2 = SubclassPath.builder().id(2L).name("Warden of the Elements")
                .associatedClass(clazz).expansion(expansion).associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now()).build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.saveAll(anyList())).thenReturn(List.of(savedPath1, savedPath2));

        // Act
        List<SubclassPathResponse> results = subclassPathService.createSubclassPathsBulk(List.of(request1, request2), authentication);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Warden of Renewal");
        assertThat(results.get(1).getName()).isEqualTo("Warden of the Elements");
        verify(subclassPathRepository).saveAll(anyList());
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateSubclassPath_ValidRequest_UpdatesAndReturnsPath() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Old Name")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .associatedDomainIds(List.of())
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingPath));
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPathResponse result = subclassPathService.updateSubclassPath(1L, request, authentication);

        // Assert
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void updateSubclassPath_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .name("Updated Name")
                .associatedClassId(1L)
                .expansionId(1L)
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.updateSubclassPath(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassPath not found with id: 999");

        verify(subclassPathRepository, never()).save(any());
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteSubclassPath_ValidId_SoftDeletesPath() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(path));

        // Act
        subclassPathService.deleteSubclassPath(1L, authentication);

        // Assert
        verify(subclassPathRepository).save(argThat(p -> p.getDeletedAt() != null));
    }

    @Test
    void deleteSubclassPath_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassPathRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.deleteSubclassPath(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassPath not found with id: 999");

        verify(subclassPathRepository, never()).save(any());
    }

    // ==================== RESTORE TESTS ====================

    @Test
    void restoreSubclassPath_DeletedPath_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath deletedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findById(1L)).thenReturn(Optional.of(deletedPath));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPathResponse result = subclassPathService.restoreSubclassPath(1L, authentication);

        // Assert
        assertThat(result).isNotNull();
        verify(subclassPathRepository).save(argThat(p -> p.getDeletedAt() == null));
    }

    @Test
    void restoreSubclassPath_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath activePath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findById(1L)).thenReturn(Optional.of(activePath));

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.restoreSubclassPath(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SubclassPath with id 1 is not deleted");

        verify(subclassPathRepository, never()).save(any());
    }

    @Test
    void restoreSubclassPath_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassPathRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.restoreSubclassPath(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("SubclassPath not found with id: 999");
    }

    // ==================== FIND OR CREATE TESTS ====================

    @Test
    void findOrCreate_ExistingPath_ReturnsExisting() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(Domain.builder().id(1L).name("Sage").expansion(expansion).build()))
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
        verify(subclassPathRepository, never()).save(any());
    }

    @Test
    void findOrCreate_NewPath_CreatesAndReturns() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.empty());
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void findOrCreate_NewPath_PublishesCreatedEvent() {
        // Arrange — regression test: paths created implicitly from an inlined subclassPath on a
        // subclass-card upload published no event, so they never reached search_index
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.empty());
        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, null, null);

        // Assert
        ArgumentCaptor<EntityChangeEvent> captor = ArgumentCaptor.forClass(EntityChangeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(EntityChangeEvent.ChangeType.CREATED);
        assertThat(captor.getValue().getEntity()).isSameAs(savedPath);
    }

    @Test
    void findOrCreate_ExistingPathBackfilled_PublishesUpdatedEvent() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        Domain domain = Domain.builder().id(1L).name("Sage").expansion(expansion).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(domain));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, List.of(1L), null);

        // Assert
        ArgumentCaptor<EntityChangeEvent> captor = ArgumentCaptor.forClass(EntityChangeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getChangeType()).isEqualTo(EntityChangeEvent.ChangeType.UPDATED);
        assertThat(captor.getValue().getEntity()).isSameAs(existingPath);
    }

    @Test
    void findOrCreate_ExistingPathUnchanged_PublishesNoEvent() {
        // Arrange — returning an untouched existing path is not a write, so it must not reindex
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(Domain.builder().id(1L).name("Sage").expansion(expansion).build()))
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));

        // Act
        subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, null, null);

        // Assert
        verify(eventPublisher, never()).publishEvent(any(EntityChangeEvent.class));
    }

    @Test
    void findOrCreate_CaseInsensitiveMatch_ReturnsExisting() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(Domain.builder().id(1L).name("Sage").expansion(expansion).build()))
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("warden of renewal", 1L))
                .thenReturn(Optional.of(existingPath));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("warden of renewal", 1L, 1L, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(subclassPathRepository, never()).save(any());
    }

    @Test
    void findOrCreate_ExistingPathWithEmptyDomains_BackfillsDomains() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        Domain domain1 = Domain.builder().id(1L).name("Sage").expansion(expansion).build();
        Domain domain2 = Domain.builder().id(2L).name("Arcana").expansion(expansion).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>())
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
                .thenReturn(List.of(domain1, domain2));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, List.of(1L, 2L), null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAssociatedDomains()).hasSize(2);
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void findOrCreate_ExistingPathWithNullDomains_BackfillsDomains() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        Domain domain = Domain.builder().id(1L).name("Sage").expansion(expansion).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(null)
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
                .thenReturn(List.of(domain));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, List.of(1L), null);

        // Assert
        assertThat(result.getAssociatedDomains()).hasSize(1);
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void findOrCreate_ExistingPathWithNullTrait_BackfillsTrait() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(Set.of(Domain.builder().id(1L).name("Sage").expansion(expansion).build()))
                .spellcastingTrait(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, null, Trait.INSTINCT);

        // Assert
        assertThat(result.getSpellcastingTrait()).isEqualTo(Trait.INSTINCT);
        verify(subclassPathRepository).save(any(SubclassPath.class));
    }

    @Test
    void findOrCreate_ExistingPathWithPopulatedDomains_DoesNotOverwrite() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        Domain existingDomain = Domain.builder().id(1L).name("Sage").expansion(expansion).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .associatedDomains(new HashSet<>(Set.of(existingDomain)))
                .spellcastingTrait(Trait.INSTINCT)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));

        // Act
        SubclassPath result = subclassPathService.findOrCreate("Warden of Renewal", 1L, 1L, List.of(2L, 3L), null);

        // Assert
        assertThat(result.getAssociatedDomains()).hasSize(1);
        assertThat(result.getAssociatedDomains()).contains(existingDomain);
        verify(subclassPathRepository, never()).save(any());
    }

    // ==================== RESOLVE PATH TESTS ====================

    @Test
    void resolvePath_WithId_ReturnsPath() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(path));

        // Act
        SubclassPath result = subclassPathService.resolvePath(1L, null, 1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void resolvePath_WithInput_FindsOrCreates() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPathInput input = SubclassPathInput.builder()
                .name("Warden of Renewal")
                .build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .createdAt(LocalDateTime.now())
                .build();

        when(subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull("Warden of Renewal", 1L))
                .thenReturn(Optional.of(existingPath));

        // Act
        SubclassPath result = subclassPathService.resolvePath(null, input, 1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Warden of Renewal");
    }

    @Test
    void resolvePath_BothProvided_ThrowsIllegalArgumentException() {
        // Arrange
        SubclassPathInput input = SubclassPathInput.builder()
                .name("Warden of Renewal")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.resolvePath(1L, input, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot specify both subclassPathId and subclassPath input");
    }

    @Test
    void resolvePath_NeitherProvided_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.resolvePath(null, null, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either subclassPathId or subclassPath input must be provided");
    }

    // ==================== SRD GATING TESTS ====================

    @Test
    void toResponse_MayViewFalse_ReturnsRedactedStub() {
        // Arrange — SubclassPath has no isOfficial of its own, so toResponse forces the mayView
        // check with isOfficial=true; a caller who may not view non-SRD content sees a stub.
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path = SubclassPath.builder()
                .id(1L)
                .name("Restricted Path")
                .associatedClass(clazz)
                .expansion(expansion)
                .srd(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(contentAccessService.mayView(true, false)).thenReturn(false);

        // Act
        SubclassPathResponse result = subclassPathService.toResponse(path, Set.of());

        // Assert — only id, restricted, and expansionName are set; everything else stays unset
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRestricted()).isTrue();
        assertThat(result.getExpansionName()).isEqualTo("Hope & Fear");
        assertThat(result.getName()).isNull();
        assertThat(result.getSrd()).isNull();
        assertThat(result.getAssociatedClassId()).isNull();
    }

    @Test
    void updateSubclassPath_SrdChanged_CascadesToNonDeletedCards() {
        // Arrange — the only writable srd flag for the (path, Foundation, Specialization,
        // Mastery) group lives on the path; updateSubclassPath must re-derive every non-deleted
        // card's srd from the path in the same transaction.
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .srd(false)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        SubclassCard foundation = SubclassCard.builder().id(10L).name("Foundation Card")
                .subclassPath(existingPath).level(SubclassLevel.FOUNDATION).srd(false).build();
        SubclassCard specialization = SubclassCard.builder().id(11L).name("Specialization Card")
                .subclassPath(existingPath).level(SubclassLevel.SPECIALIZATION).srd(false).build();
        SubclassCard mastery = SubclassCard.builder().id(12L).name("Mastery Card")
                .subclassPath(existingPath).level(SubclassLevel.MASTERY).srd(false).build();
        List<SubclassCard> cards = List.of(foundation, specialization, mastery);

        UpdateSubclassPathRequest request = UpdateSubclassPathRequest.builder()
                .srd(true)
                .build();

        when(subclassPathRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingPath));
        when(contentAccessService.resolveSrd(defaultUser, true)).thenReturn(true);
        when(subclassPathRepository.save(any(SubclassPath.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(1L)).thenReturn(cards);
        when(subclassCardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubclassPathResponse result = subclassPathService.updateSubclassPath(1L, request, authentication);

        // Assert
        assertThat(result.getSrd()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubclassCard>> captor = ArgumentCaptor.forClass(List.class);
        verify(subclassCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue()).allMatch(card -> Boolean.TRUE.equals(card.getSrd()));
    }

    @Test
    void createSubclassPath_NoCardsYet_CascadeIsNoOp() {
        // Arrange — a freshly created path has no cards, so the post-save cascade call is
        // exercised but resolves to an empty list and never calls saveAll.
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        CreateSubclassPathRequest request = CreateSubclassPathRequest.builder()
                .name("Warden of Renewal")
                .associatedClassId(1L)
                .expansionId(1L)
                .srd(true)
                .build();

        SubclassPath savedPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
                .srd(true)
                .associatedDomains(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(contentAccessService.resolveSrd(defaultUser, true)).thenReturn(true);
        when(subclassPathRepository.save(any(SubclassPath.class))).thenReturn(savedPath);

        // Act
        SubclassPathResponse result = subclassPathService.createSubclassPath(request, authentication);

        // Assert
        assertThat(result.getSrd()).isTrue();
        verify(subclassCardRepository).findBySubclassPathIdAndDeletedAtIsNull(1L);
        verify(subclassCardRepository, never()).saveAll(anyList());
    }

    // ==================== BULK SET SRD TESTS ====================

    @Test
    void bulkSetSrd_MatchingPaths_CascadesToEveryPathsCards() {
        // Arrange — backs AdminContentService's bulk SRD-flagging tool for type=SUBCLASS_PATH;
        // unlike the generic repository dispatch every other type uses, this must cascade to
        // each path's Foundation/Specialization/Mastery cards in the same transaction.
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath path1 = SubclassPath.builder()
                .id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion)
                .srd(false).associatedDomains(new HashSet<>()).createdAt(LocalDateTime.now()).build();
        SubclassPath path2 = SubclassPath.builder()
                .id(2L).name("Warden of the Elements").associatedClass(clazz).expansion(expansion)
                .srd(false).associatedDomains(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        SubclassCard path1Card = SubclassCard.builder().id(10L).name("Foundation Card")
                .subclassPath(path1).level(SubclassLevel.FOUNDATION).srd(false).build();
        SubclassCard path2Card = SubclassCard.builder().id(20L).name("Foundation Card")
                .subclassPath(path2).level(SubclassLevel.FOUNDATION).srd(false).build();

        when(subclassPathRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(path1, path2));
        when(subclassPathRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(1L)).thenReturn(List.of(path1Card));
        when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(2L)).thenReturn(List.of(path2Card));
        when(subclassCardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Long> updatedIds = subclassPathService.bulkSetSrd(List.of(1L, 2L), true);

        // Assert
        assertThat(updatedIds).containsExactlyInAnyOrder(1L, 2L);
        assertThat(path1.getSrd()).isTrue();
        assertThat(path2.getSrd()).isTrue();
        assertThat(path1Card.getSrd()).isTrue();
        assertThat(path2Card.getSrd()).isTrue();
        verify(subclassCardRepository).saveAll(List.of(path1Card));
        verify(subclassCardRepository).saveAll(List.of(path2Card));
    }

    @Test
    void bulkSetSrd_UnknownId_SkippedAndOmittedFromResult() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        SubclassPath path = SubclassPath.builder()
                .id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion)
                .srd(false).createdAt(LocalDateTime.now()).build();

        when(subclassPathRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(path));
        when(subclassPathRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        // Act
        List<Long> updatedIds = subclassPathService.bulkSetSrd(List.of(1L, 999L), true);

        // Assert
        assertThat(updatedIds).containsExactly(1L);
    }

    @Test
    void bulkSetSrd_UnmarkingToFalse_SetsSrdFalseOnPathAndCards() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Hope & Fear").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();
        SubclassPath path = SubclassPath.builder()
                .id(1L).name("Warden of Renewal").associatedClass(clazz).expansion(expansion)
                .srd(true).createdAt(LocalDateTime.now()).build();
        SubclassCard card = SubclassCard.builder().id(10L).name("Foundation Card")
                .subclassPath(path).level(SubclassLevel.FOUNDATION).srd(true).build();

        when(subclassPathRepository.findAllById(List.of(1L))).thenReturn(List.of(path));
        when(subclassPathRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subclassCardRepository.findBySubclassPathIdAndDeletedAtIsNull(1L)).thenReturn(List.of(card));
        when(subclassCardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        subclassPathService.bulkSetSrd(List.of(1L), false);

        // Assert
        assertThat(path.getSrd()).isFalse();
        assertThat(card.getSrd()).isFalse();
    }
}
