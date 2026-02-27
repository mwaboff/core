package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassPathRequest;
import com.aboff.core.model.dto.dh.request.SubclassPathInput;
import com.aboff.core.model.dto.dh.request.UpdateSubclassPathRequest;
import com.aboff.core.model.dto.dh.response.SubclassPathResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.SubclassPath;
import com.aboff.core.model.enums.Trait;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.SubclassPathRepository;
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

    @InjectMocks
    private SubclassPathService subclassPathService;

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
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), any(Pageable.class)))
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
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(eq(1L), any(Pageable.class)))
                .thenReturn(pathPage);

        // Act
        PagedResponse<SubclassPathResponse> result = subclassPathService.getAllSubclassPaths(0, 20, false, 1L, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAssociatedClassId()).isEqualTo(1L);
        verify(subclassPathRepository).findByDeletedAtIsNullAndFilters(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllSubclassPaths_WithLargePage_LimitsTo100() {
        // Arrange
        Page<SubclassPath> pathPage = new PageImpl<>(List.of());
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), any(Pageable.class)))
                .thenReturn(pathPage);

        // Act
        subclassPathService.getAllSubclassPaths(0, 500, false, null, null);

        // Assert
        verify(subclassPathRepository).findByDeletedAtIsNullAndFilters(
                isNull(),
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
        when(subclassPathRepository.findByDeletedAtIsNullAndFilters(isNull(), any(Pageable.class)))
                .thenReturn(pathPage);

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
        SubclassPathResponse result = subclassPathService.createSubclassPath(request);

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
        SubclassPathResponse result = subclassPathService.createSubclassPath(request);

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
        SubclassPathResponse result = subclassPathService.createSubclassPath(request);

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
        assertThatThrownBy(() -> subclassPathService.createSubclassPath(request))
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
        assertThatThrownBy(() -> subclassPathService.createSubclassPath(request))
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
        List<SubclassPathResponse> results = subclassPathService.createSubclassPathsBulk(List.of(request1, request2));

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
        SubclassPathResponse result = subclassPathService.updateSubclassPath(1L, request);

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
        assertThatThrownBy(() -> subclassPathService.updateSubclassPath(999L, request))
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
        subclassPathService.deleteSubclassPath(1L);

        // Assert
        verify(subclassPathRepository).save(argThat(p -> p.getDeletedAt() != null));
    }

    @Test
    void deleteSubclassPath_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassPathRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.deleteSubclassPath(999L))
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
        SubclassPathResponse result = subclassPathService.restoreSubclassPath(1L);

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
        assertThatThrownBy(() -> subclassPathService.restoreSubclassPath(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SubclassPath with id 1 is not deleted");

        verify(subclassPathRepository, never()).save(any());
    }

    @Test
    void restoreSubclassPath_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(subclassPathRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subclassPathService.restoreSubclassPath(999L))
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
    void findOrCreate_CaseInsensitiveMatch_ReturnsExisting() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Class clazz = Class.builder().id(1L).name("Druid").expansion(expansion).startingEvasion(9).startingHitPoints(16).build();

        SubclassPath existingPath = SubclassPath.builder()
                .id(1L)
                .name("Warden of Renewal")
                .associatedClass(clazz)
                .expansion(expansion)
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
}
