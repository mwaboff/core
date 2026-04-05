package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateClassRequest;
import com.aboff.core.model.dto.dh.request.FeatureInput;
import com.aboff.core.model.dto.dh.request.QuestionInput;
import com.aboff.core.model.dto.dh.request.UpdateClassRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.FeatureModifierResponse;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.enums.CostTagCategory;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.FeatureType;
import com.aboff.core.model.enums.QuestionType;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClassService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality, expand parameter, bulk operations, and many-to-many relationships.
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ClassService classService;

    // ==================== GET ALL CLASSES TESTS ====================

    @Test
    void getAllClasses_WithoutFilters_ReturnsPagedClasses() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class class1 = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        Class class2 = Class.builder()
                .id(2L)
                .name("Mage")
                .description("Spellcaster")
                .expansion(expansion)
                .startingClassItems("Staff, Spellbook")
                .startingEvasion(15)
                .startingHitPoints(15)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(class1, class2));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Warrior");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Mage");
    }

    @Test
    void getAllClasses_WithExpansionFilter_ReturnsFilteredClasses() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(eq(1L), any(Pageable.class)))
                .thenReturn(classPage);

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, 1L, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansionId()).isEqualTo(1L);
        verify(classRepository).findByDeletedAtIsNullAndExpansion(eq(1L), any(Pageable.class));
    }

    @Test
    void getAllClasses_WithIncludeDeleted_ReturnsAllClasses() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Deleted Class")
                .description("Deleted")
                .expansion(expansion)
                .startingClassItems("Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .deletedAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findAllWithExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, true, null, null);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedAt()).isNotNull();
        verify(classRepository).findAllWithExpansion(isNull(), any(Pageable.class));
    }

    @Test
    void getAllClasses_WithLargePage_LimitsTo100() {
        // Arrange
        Page<Class> classPage = new PageImpl<>(List.of());
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);

        // Act
        classService.getAllClasses(0, 500, false, null, null);

        // Assert
        verify(classRepository).findByDeletedAtIsNullAndExpansion(
                isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void getAllClasses_WithExpandParameters_ExpandsRelationships() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Domain domain = Domain.builder().id(1L).name("Blade").expansion(expansion).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Power Attack").featureType(FeatureType.HOPE).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .associatedDomains(Set.of(domain))
                .hopeFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "expansion,associatedDomains,hopeFeatures");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getAssociatedDomains()).isNotNull();
        assertThat(result.getContent().get(0).getHopeFeatures()).isNotNull();
        FeatureResponse hopeFeature = result.getContent().get(0).getHopeFeatures().get(0);
        assertThat(hopeFeature.getCostTagIds()).containsExactly(1L);
        assertThat(hopeFeature.getCostTags()).isNull();
    }

    // ==================== GET CLASS BY ID TESTS ====================

    @Test
    void getClassById_ValidId_ReturnsClass() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(clazz));

        // Act
        ClassResponse result = classService.getClassById(1L, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Warrior");
        assertThat(result.getStartingEvasion()).isEqualTo(10);
        assertThat(result.getStartingHitPoints()).isEqualTo(20);
    }

    @Test
    void getClassById_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(classRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classService.getClassById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");
    }

    // ==================== CREATE CLASS TESTS ====================

    @Test
    void createClass_ValidRequest_CreatesAndReturnsClass() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Domain domain = Domain.builder().id(1L).name("Blade").expansion(expansion).build();
        Feature feature = Feature.builder().id(1L).name("Power Attack").featureType(FeatureType.HOPE).expansion(expansion).build();
        Question question = Question.builder().id(1L).questionText("What?").questionType(QuestionType.BACKGROUND).expansion(expansion).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior")
                .description("Strong fighter")
                .expansionId(1L)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .associatedDomainIds(List.of(1L))
                .hopeFeatureIds(List.of(1L))
                .classFeatureIds(List.of())
                .backgroundQuestionIds(List.of(1L))
                .connectionQuestionIds(List.of())
                .build();

        Class savedClass = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(domainRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(domain));
        when(featureService.resolveFeatures(eq(List.of(1L)), isNull())).thenReturn(Set.of(feature));
        when(featureService.resolveFeatures(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(questionService.resolveQuestions(eq(List.of(1L)), isNull())).thenReturn(Set.of(question));
        when(questionService.resolveQuestions(eq(List.of()), isNull())).thenReturn(new HashSet<>());
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Warrior");
        verify(classRepository).save(any(Class.class));
    }

    @Test
    void createClass_ExpansionNotFound_ThrowsEntityNotFoundException() {
        // Arrange
        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior")
                .description("Strong fighter")
                .expansionId(999L)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classService.createClass(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(classRepository, never()).save(any());
    }

    // ==================== CREATE CLASSES BULK TESTS ====================

    @Test
    void createClassesBulk_ValidRequests_CreatesAndReturnsClasses() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        CreateClassRequest request1 = CreateClassRequest.builder()
                .name("Warrior")
                .description("Strong fighter")
                .expansionId(1L)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .build();

        CreateClassRequest request2 = CreateClassRequest.builder()
                .name("Mage")
                .description("Spellcaster")
                .expansionId(1L)
                .startingClassItems("Staff, Spellbook")
                .startingEvasion(15)
                .startingHitPoints(15)
                .build();

        Class savedClass1 = Class.builder().id(1L).name("Warrior").description("Strong fighter")
                .expansion(expansion).startingClassItems("Sword, Shield").startingEvasion(10).startingHitPoints(20)
                .createdAt(LocalDateTime.now()).build();

        Class savedClass2 = Class.builder().id(2L).name("Mage").description("Spellcaster")
                .expansion(expansion).startingClassItems("Staff, Spellbook").startingEvasion(15).startingHitPoints(15)
                .createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.saveAll(anyList())).thenReturn(List.of(savedClass1, savedClass2));

        // Act
        List<ClassResponse> results = classService.createClassesBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Warrior");
        assertThat(results.get(1).getName()).isEqualTo("Mage");
        verify(classRepository).saveAll(anyList());
    }

    // ==================== UPDATE CLASS TESTS ====================

    @Test
    void updateClass_ValidRequest_UpdatesAndReturnsClass() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class existingClass = Class.builder()
                .id(1L)
                .name("Old Name")
                .description("Old description")
                .expansion(expansion)
                .startingClassItems("Old Items")
                .startingEvasion(5)
                .startingHitPoints(10)
                .associatedDomains(new HashSet<>())
                .hopeFeatures(new HashSet<>())
                .classFeatures(new HashSet<>())
                .backgroundQuestions(new HashSet<>())
                .connectionQuestions(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .startingClassItems("Updated Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .associatedDomainIds(List.of())
                .hopeFeatureIds(List.of())
                .classFeatureIds(List.of())
                .backgroundQuestionIds(List.of())
                .connectionQuestionIds(List.of())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingClass));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassResponse result = classService.updateClass(1L, request);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getStartingEvasion()).isEqualTo(10);
        assertThat(result.getStartingHitPoints()).isEqualTo(20);
        verify(classRepository).save(any(Class.class));
    }

    @Test
    void updateClass_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .expansionId(1L)
                .startingClassItems("Updated Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classService.updateClass(999L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");

        verify(classRepository, never()).save(any());
    }

    // ==================== DELETE CLASS TESTS ====================

    @Test
    void deleteClass_ValidId_SoftDeletesClass() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("To Delete")
                .description("To be deleted")
                .expansion(expansion)
                .startingClassItems("Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(clazz));

        // Act
        classService.deleteClass(1L);

        // Assert
        verify(classRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteClass_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(classRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classService.deleteClass(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");

        verify(classRepository, never()).save(any());
    }

    // ==================== RESTORE CLASS TESTS ====================

    @Test
    void restoreClass_DeletedClass_RestoresSuccessfully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class deletedClass = Class.builder()
                .id(1L)
                .name("Deleted Class")
                .description("Deleted")
                .expansion(expansion)
                .startingClassItems("Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(deletedClass));
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassResponse result = classService.restoreClass(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(classRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreClass_NotDeleted_ThrowsIllegalStateException() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        Class activeClass = Class.builder()
                .id(1L)
                .name("Active Class")
                .description("Active")
                .expansion(expansion)
                .startingClassItems("Items")
                .startingEvasion(10)
                .startingHitPoints(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(activeClass));

        // Act & Assert
        assertThatThrownBy(() -> classService.restoreClass(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Class with id 1 is not deleted");

        verify(classRepository, never()).save(any());
    }

    @Test
    void restoreClass_NotFound_ThrowsEntityNotFoundException() {
        // Arrange
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classService.restoreClass(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Class not found with id: 999");
    }

    // ==================== FEATURE COST TAG EXPANSION TESTS ====================

    @Test
    void getAllClasses_WithExpandHopeFeaturesAndCostTags_IncludesFullCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(1L).label("3 Hope").category(CostTagCategory.COST).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Power Attack").featureType(FeatureType.HOPE).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .hopeFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "hopeFeatures,costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        FeatureResponse hopeFeature = result.getContent().get(0).getHopeFeatures().get(0);
        assertThat(hopeFeature.getCostTagIds()).containsExactly(1L);
        assertThat(hopeFeature.getCostTags()).isNotNull().hasSize(1);
        assertThat(hopeFeature.getCostTags().get(0).getLabel()).isEqualTo("3 Hope");
        assertThat(hopeFeature.getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.COST);
    }

    @Test
    void getAllClasses_WithExpandClassFeaturesAndCostTags_IncludesFullCostTags() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(2L).label("1/session").category(CostTagCategory.LIMITATION).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Blade Dance").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .classFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "classFeatures,costTags");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        FeatureResponse classFeature = result.getContent().get(0).getClassFeatures().get(0);
        assertThat(classFeature.getCostTagIds()).containsExactly(2L);
        assertThat(classFeature.getCostTags()).isNotNull().hasSize(1);
        assertThat(classFeature.getCostTags().get(0).getLabel()).isEqualTo("1/session");
        assertThat(classFeature.getCostTags().get(0).getCategory()).isEqualTo(CostTagCategory.LIMITATION);
    }

    @Test
    void getAllClasses_WithExpandClassFeaturesWithoutCostTags_IncludesCostTagIdsOnly() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        CardCostTag costTag = CardCostTag.builder().id(2L).label("1/session").category(CostTagCategory.LIMITATION).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Blade Dance").featureType(FeatureType.CLASS).expansion(expansion).costTags(Set.of(costTag)).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .classFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "classFeatures");

        // Assert
        assertThat(result.getContent()).hasSize(1);
        FeatureResponse classFeature = result.getContent().get(0).getClassFeatures().get(0);
        assertThat(classFeature.getCostTagIds()).containsExactly(2L);
        assertThat(classFeature.getCostTags()).isNull();
    }

    @Test
    void getAllClasses_WithExpandFeaturesNullCostTags_HandlesNullGracefully() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Power Attack").featureType(FeatureType.HOPE).expansion(expansion)
                .costTags(null).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .hopeFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "hopeFeatures,costTags");

        // Assert
        FeatureResponse hopeFeature = result.getContent().get(0).getHopeFeatures().get(0);
        assertThat(hopeFeature.getCostTagIds()).isNull();
        assertThat(hopeFeature.getCostTags()).isNull();
    }

    @Test
    void getAllClasses_WithExpandFeaturesEmptyCostTags_ReturnsEmptyLists() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).createdAt(LocalDateTime.now()).build();
        Feature feature = Feature.builder().id(1L).name("Power Attack").featureType(FeatureType.HOPE).expansion(expansion)
                .costTags(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        Class clazz = Class.builder()
                .id(1L)
                .name("Warrior")
                .description("Strong fighter")
                .expansion(expansion)
                .startingClassItems("Sword, Shield")
                .startingEvasion(10)
                .startingHitPoints(20)
                .hopeFeatures(Set.of(feature))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Class> classPage = new PageImpl<>(List.of(clazz));
        when(classRepository.findByDeletedAtIsNullAndExpansion(isNull(), any(Pageable.class)))
                .thenReturn(classPage);
        when(featureService.toResponse(any(Feature.class), anySet())).thenAnswer(invocation -> buildFeatureResponse(invocation.getArgument(0), invocation.getArgument(1)));

        // Act
        PagedResponse<ClassResponse> result = classService.getAllClasses(0, 20, false, null, "hopeFeatures,costTags");

        // Assert
        FeatureResponse hopeFeature = result.getContent().get(0).getHopeFeatures().get(0);
        assertThat(hopeFeature.getCostTagIds()).isEmpty();
        assertThat(hopeFeature.getCostTags()).isEmpty();
    }

    // ==================== INLINE CREATION TESTS ====================

    @Test
    void createClass_withInlineHopeFeatures_resolvesAndSetsFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature inlineFeature = Feature.builder().id(10L).name("Inline Hope").featureType(FeatureType.HOPE).expansion(expansion).build();

        FeatureInput hopeInput = FeatureInput.builder()
                .name("Inline Hope").featureType(FeatureType.HOPE).expansionId(1L).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(List.of(hopeInput))
                .build();

        Class savedClass = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(Set.of(inlineFeature)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), eq(List.of(hopeInput)))).thenReturn(Set.of(inlineFeature));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHopeFeatureIds()).containsExactly(10L);
        verify(featureService).resolveFeatures(isNull(), eq(List.of(hopeInput)));
    }

    @Test
    void createClass_withInlineClassFeatures_resolvesAndSetsFeatures() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature classFeature = Feature.builder().id(11L).name("Blade Dance").featureType(FeatureType.CLASS).expansion(expansion).build();

        FeatureInput classInput = FeatureInput.builder()
                .name("Blade Dance").featureType(FeatureType.CLASS).expansionId(1L).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .classFeatures(List.of(classInput))
                .build();

        Class savedClass = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .classFeatures(Set.of(classFeature)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), eq(List.of(classInput)))).thenReturn(Set.of(classFeature));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClassFeatureIds()).containsExactly(11L);
        verify(featureService).resolveFeatures(isNull(), eq(List.of(classInput)));
    }

    @Test
    void createClass_withInlineBackgroundQuestions_resolvesAndSetsQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Question bgQuestion = Question.builder().id(20L).questionText("What is your background?")
                .questionType(QuestionType.BACKGROUND).expansion(expansion).build();

        QuestionInput bgInput = QuestionInput.builder()
                .questionText("What is your background?").questionType(QuestionType.BACKGROUND).expansionId(1L).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .backgroundQuestions(List.of(bgInput))
                .build();

        Class savedClass = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .backgroundQuestions(Set.of(bgQuestion)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), eq(List.of(bgInput)))).thenReturn(Set.of(bgQuestion));
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBackgroundQuestionIds()).containsExactly(20L);
        verify(questionService).resolveQuestions(isNull(), eq(List.of(bgInput)));
    }

    @Test
    void createClass_withInlineConnectionQuestions_resolvesAndSetsQuestions() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Question connQuestion = Question.builder().id(21L).questionText("Who do you know?")
                .questionType(QuestionType.CONNECTION).expansion(expansion).build();

        QuestionInput connInput = QuestionInput.builder()
                .questionText("Who do you know?").questionType(QuestionType.CONNECTION).expansionId(1L).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .connectionQuestions(List.of(connInput))
                .build();

        Class savedClass = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .connectionQuestions(Set.of(connQuestion)).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), eq(List.of(connInput)))).thenReturn(Set.of(connQuestion));
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getConnectionQuestionIds()).containsExactly(21L);
        verify(questionService).resolveQuestions(isNull(), eq(List.of(connInput)));
    }

    @Test
    void createClass_withMixedIdsAndInlineFeatures_mergesBoth() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature existingFeature = Feature.builder().id(1L).name("Existing Hope").featureType(FeatureType.HOPE).expansion(expansion).build();
        Feature inlineFeature = Feature.builder().id(10L).name("Inline Hope").featureType(FeatureType.HOPE).expansion(expansion).build();

        FeatureInput hopeInput = FeatureInput.builder()
                .name("Inline Hope").featureType(FeatureType.HOPE).expansionId(1L).build();

        CreateClassRequest request = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .hopeFeatureIds(List.of(1L))
                .hopeFeatures(List.of(hopeInput))
                .build();

        Set<Feature> mergedFeatures = Set.of(existingFeature, inlineFeature);

        Class savedClass = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(mergedFeatures).createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(eq(List.of(1L)), eq(List.of(hopeInput)))).thenReturn(mergedFeatures);
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenReturn(savedClass);

        // Act
        ClassResponse result = classService.createClass(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getHopeFeatureIds()).containsExactlyInAnyOrder(1L, 10L);
        verify(featureService).resolveFeatures(eq(List.of(1L)), eq(List.of(hopeInput)));
    }

    @Test
    void createClassesBulk_withInlineInputs_resolvesAll() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature hopeFeature = Feature.builder().id(10L).name("Hope Feat").featureType(FeatureType.HOPE).expansion(expansion).build();

        FeatureInput hopeInput = FeatureInput.builder()
                .name("Hope Feat").featureType(FeatureType.HOPE).expansionId(1L).build();

        CreateClassRequest request1 = CreateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(List.of(hopeInput))
                .build();

        CreateClassRequest request2 = CreateClassRequest.builder()
                .name("Mage").description("Caster").expansionId(1L)
                .startingEvasion(15).startingHitPoints(15)
                .build();

        Class savedClass1 = Class.builder().id(1L).name("Warrior").description("Fighter")
                .expansion(expansion).startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(Set.of(hopeFeature)).createdAt(LocalDateTime.now()).build();
        Class savedClass2 = Class.builder().id(2L).name("Mage").description("Caster")
                .expansion(expansion).startingEvasion(15).startingHitPoints(15)
                .createdAt(LocalDateTime.now()).build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), eq(List.of(hopeInput)))).thenReturn(Set.of(hopeFeature));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.saveAll(anyList())).thenReturn(List.of(savedClass1, savedClass2));

        // Act
        List<ClassResponse> results = classService.createClassesBulk(List.of(request1, request2));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getHopeFeatureIds()).containsExactly(10L);
        verify(featureService).resolveFeatures(isNull(), eq(List.of(hopeInput)));
    }

    @Test
    void updateClass_withInlineFeatures_resolvesAndUpdates() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature newFeature = Feature.builder().id(10L).name("New Hope").featureType(FeatureType.HOPE).expansion(expansion).build();

        FeatureInput hopeInput = FeatureInput.builder()
                .name("New Hope").featureType(FeatureType.HOPE).expansionId(1L).build();

        Class existingClass = Class.builder()
                .id(1L).name("Warrior").description("Fighter").expansion(expansion)
                .startingEvasion(10).startingHitPoints(20)
                .associatedDomains(new HashSet<>()).hopeFeatures(new HashSet<>())
                .classFeatures(new HashSet<>()).backgroundQuestions(new HashSet<>())
                .connectionQuestions(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .hopeFeatures(List.of(hopeInput))
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingClass));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), eq(List.of(hopeInput)))).thenReturn(Set.of(newFeature));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassResponse result = classService.updateClass(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(featureService).resolveFeatures(isNull(), eq(List.of(hopeInput)));
    }

    @Test
    void updateClass_withNullInlineInputs_doesNotModify() {
        // Arrange
        Expansion expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        Feature existingFeature = Feature.builder().id(1L).name("Existing Hope").featureType(FeatureType.HOPE).expansion(expansion).build();

        Class existingClass = Class.builder()
                .id(1L).name("Warrior").description("Fighter").expansion(expansion)
                .startingEvasion(10).startingHitPoints(20)
                .associatedDomains(new HashSet<>()).hopeFeatures(new HashSet<>(Set.of(existingFeature)))
                .classFeatures(new HashSet<>()).backgroundQuestions(new HashSet<>())
                .connectionQuestions(new HashSet<>()).createdAt(LocalDateTime.now()).build();

        UpdateClassRequest request = UpdateClassRequest.builder()
                .name("Warrior").description("Fighter").expansionId(1L)
                .startingEvasion(10).startingHitPoints(20)
                .build();

        when(classRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingClass));
        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(featureService.resolveFeatures(isNull(), isNull())).thenReturn(null);
        when(questionService.resolveQuestions(isNull(), isNull())).thenReturn(null);
        when(classRepository.save(any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassResponse result = classService.updateClass(1L, request);

        // Assert - existing features should be preserved since resolveFeatures returned null
        assertThat(result).isNotNull();
        assertThat(result.getHopeFeatureIds()).containsExactly(1L);
        verify(featureService, times(2)).resolveFeatures(isNull(), isNull());
    }

    /**
     * Helper to build a FeatureResponse from a Feature entity and expand set,
     * mirroring the behavior of FeatureService.toResponse().
     */
    private FeatureResponse buildFeatureResponse(Feature f, Set<String> exp) {
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
    }
}
