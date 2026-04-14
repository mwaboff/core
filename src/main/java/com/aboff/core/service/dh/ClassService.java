package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateClassRequest;
import com.aboff.core.model.dto.dh.request.UpdateClassRequest;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.QuestionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Class entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassService {

    private final ClassRepository classRepository;
    private final ExpansionRepository expansionRepository;
    private final DomainRepository domainRepository;
    private final FeatureService featureService;
    private final QuestionService questionService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    /**
     * Retrieves a paginated list of classes.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted classes
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing classes
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassResponse> getAllClasses(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Class> classPage;

        if (includeDeleted) {
            classPage = classRepository.findAllWithExpansion(expansionId, pageable);
        } else {
            classPage = classRepository.findByDeletedAtIsNullAndExpansion(expansionId, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<ClassResponse>builder()
                .content(classPage.getContent().stream()
                        .map(clazz -> toResponse(clazz, expandSet))
                        .toList())
                .totalElements(classPage.getTotalElements())
                .totalPages(classPage.getTotalPages())
                .currentPage(classPage.getNumber())
                .pageSize(classPage.getSize())
                .build();
    }

    /**
     * Retrieves a single class by ID.
     *
     * @param id The class ID
     * @param expand Comma-separated list of relationships to expand
     * @return ClassResponse containing the class details
     * @throws EntityNotFoundException if the class is not found or is deleted
     */
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id, String expand) {
        Class clazz = classRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(clazz, expandSet);
    }

    /**
     * Creates a new class.
     *
     * @param request The creation request containing class details
     * @param authentication The authentication of the current user
     * @return ClassResponse containing the created class
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, Authentication authentication) {
        Class clazz = buildClassFromRequest(request);
        Class savedClass = classRepository.save(clazz);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedClass, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED,
                AuditContext.forUser(authentication).withEntityType("class").build(),
                "\"" + savedClass.getName() + "\" (class_id: " + savedClass.getId() + ")");

        return toResponse(savedClass, Set.of());
    }

    /**
     * Creates multiple classes in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created class responses
     */
    @Transactional
    public List<ClassResponse> createClassesBulk(List<CreateClassRequest> requests, Authentication authentication) {
        List<Class> classes = requests.stream()
                .map(this::buildClassFromRequest)
                .toList();

        List<Class> savedClasses = classRepository.saveAll(classes);
        savedClasses.forEach(c -> eventPublisher.publishEvent(new EntityChangeEvent(this, c, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED,
                AuditContext.forUser(authentication).withEntityType("class").build(),
                savedClasses.size() + " created");

        return savedClasses.stream()
                .map(clazz -> toResponse(clazz, Set.of()))
                .toList();
    }

    /**
     * Updates an existing class.
     *
     * @param id The class ID to update
     * @param request The update request containing new class details
     * @param authentication The authentication of the current user
     * @return ClassResponse containing the updated class
     * @throws EntityNotFoundException if the class or referenced entities are not found
     */
    @Transactional
    public ClassResponse updateClass(Long id, UpdateClassRequest request, Authentication authentication) {
        Class clazz = classRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            clazz.setName(request.getName());
        }
        if (request.getDescription() != null) {
            clazz.setDescription(request.getDescription());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            clazz.setExpansion(expansion);
        }
        if (request.getStartingClassItems() != null) {
            clazz.setStartingClassItems(request.getStartingClassItems());
        }
        if (request.getStartingEvasion() != null) {
            clazz.setStartingEvasion(request.getStartingEvasion());
        }
        if (request.getStartingHitPoints() != null) {
            clazz.setStartingHitPoints(request.getStartingHitPoints());
        }

        // Update domains (still ID-only)
        if (request.getAssociatedDomainIds() != null) {
            if (request.getAssociatedDomainIds().isEmpty()) {
                clazz.setAssociatedDomains(new HashSet<>());
            } else {
                Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                clazz.setAssociatedDomains(domains);
            }
        }

        // Resolve hope features (IDs + inline, null = don't modify)
        Set<Feature> hopeFeatures = featureService.resolveFeatures(
                request.getHopeFeatureIds(), request.getHopeFeatures());
        if (hopeFeatures != null) {
            clazz.setHopeFeatures(hopeFeatures);
        }

        // Resolve class features
        Set<Feature> classFeatures = featureService.resolveFeatures(
                request.getClassFeatureIds(), request.getClassFeatures());
        if (classFeatures != null) {
            clazz.setClassFeatures(classFeatures);
        }

        // Resolve background questions
        Set<Question> backgroundQuestions = questionService.resolveQuestions(
                request.getBackgroundQuestionIds(), request.getBackgroundQuestions());
        if (backgroundQuestions != null) {
            clazz.setBackgroundQuestions(backgroundQuestions);
        }

        // Resolve connection questions
        Set<Question> connectionQuestions = questionService.resolveQuestions(
                request.getConnectionQuestionIds(), request.getConnectionQuestions());
        if (connectionQuestions != null) {
            clazz.setConnectionQuestions(connectionQuestions);
        }

        Class updatedClass = classRepository.save(clazz);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedClass, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED,
                AuditContext.forUser(authentication).withEntityType("class").build(),
                "class_id: " + updatedClass.getId());

        return toResponse(updatedClass, Set.of());
    }

    /**
     * Soft deletes a class by setting its deletedAt timestamp.
     *
     * @param id The class ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the class is not found or is already deleted
     */
    @Transactional
    public void deleteClass(Long id, Authentication authentication) {
        Class clazz = classRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        clazz.softDelete();
        classRepository.save(clazz);
        eventPublisher.publishEvent(new EntityChangeEvent(this, clazz, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED,
                AuditContext.forUser(authentication).withEntityType("class").build(),
                "class_id: " + id);
    }

    /**
     * Restores a soft-deleted class.
     *
     * @param id The class ID to restore
     * @param authentication The authentication of the current user
     * @return ClassResponse containing the restored class
     * @throws EntityNotFoundException if the class is not found
     * @throws IllegalStateException if the class is not deleted
     */
    @Transactional
    public ClassResponse restoreClass(Long id, Authentication authentication) {
        Class clazz = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        if (!clazz.isDeleted()) {
            throw new IllegalStateException("Class with id " + id + " is not deleted");
        }

        clazz.restore();
        Class restoredClass = classRepository.save(clazz);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredClass, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED,
                AuditContext.forUser(authentication).withEntityType("class").build(),
                "class_id: " + id);

        return toResponse(restoredClass, Set.of());
    }

    /**
     * Builds a Class entity from a CreateClassRequest, resolving all relationships.
     *
     * @param request The creation request containing class details
     * @return The built Class entity (not yet persisted)
     * @throws EntityNotFoundException if the referenced expansion is not found
     */
    private Class buildClassFromRequest(CreateClassRequest request) {
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Class clazz = Class.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .startingClassItems(request.getStartingClassItems())
                .startingEvasion(request.getStartingEvasion())
                .startingHitPoints(request.getStartingHitPoints())
                .build();

        // Resolve domains (still ID-only)
        if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
            Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
            clazz.setAssociatedDomains(domains);
        }

        // Resolve hope features (IDs + inline)
        Set<Feature> hopeFeatures = featureService.resolveFeatures(
                request.getHopeFeatureIds(), request.getHopeFeatures());
        if (hopeFeatures != null) {
            clazz.setHopeFeatures(hopeFeatures);
        }

        // Resolve class features (IDs + inline)
        Set<Feature> classFeatures = featureService.resolveFeatures(
                request.getClassFeatureIds(), request.getClassFeatures());
        if (classFeatures != null) {
            clazz.setClassFeatures(classFeatures);
        }

        // Resolve background questions (IDs + inline)
        Set<Question> backgroundQuestions = questionService.resolveQuestions(
                request.getBackgroundQuestionIds(), request.getBackgroundQuestions());
        if (backgroundQuestions != null) {
            clazz.setBackgroundQuestions(backgroundQuestions);
        }

        // Resolve connection questions (IDs + inline)
        Set<Question> connectionQuestions = questionService.resolveQuestions(
                request.getConnectionQuestionIds(), request.getConnectionQuestions());
        if (connectionQuestions != null) {
            clazz.setConnectionQuestions(connectionQuestions);
        }

        return clazz;
    }

    /**
     * Converts a Class entity to ClassResponse DTO.
     *
     * @param clazz The class entity
     * @param expand Set of relationships to expand
     * @return ClassResponse DTO
     */
    private ClassResponse toResponse(Class clazz, Set<String> expand) {
        ClassResponse.ClassResponseBuilder builder = ClassResponse.builder()
                .id(clazz.getId())
                .name(clazz.getName())
                .description(clazz.getDescription())
                .expansionId(clazz.getExpansion().getId())
                .startingClassItems(clazz.getStartingClassItems())
                .startingEvasion(clazz.getStartingEvasion())
                .startingHitPoints(clazz.getStartingHitPoints())
                .createdAt(clazz.getCreatedAt())
                .lastModifiedAt(clazz.getLastModifiedAt())
                .deletedAt(clazz.getDeletedAt());

        // Always include IDs
        if (clazz.getAssociatedDomains() != null) {
            builder.associatedDomainIds(clazz.getAssociatedDomains().stream()
                    .map(Domain::getId)
                    .collect(Collectors.toList()));
        }

        if (clazz.getHopeFeatures() != null) {
            builder.hopeFeatureIds(clazz.getHopeFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (clazz.getClassFeatures() != null) {
            builder.classFeatureIds(clazz.getClassFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (clazz.getBackgroundQuestions() != null) {
            builder.backgroundQuestionIds(clazz.getBackgroundQuestions().stream()
                    .map(Question::getId)
                    .collect(Collectors.toList()));
        }

        if (clazz.getConnectionQuestions() != null) {
            builder.connectionQuestionIds(clazz.getConnectionQuestions().stream()
                    .map(Question::getId)
                    .collect(Collectors.toList()));
        }

        // Expand expansion if requested
        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = clazz.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        // Expand associated domains if requested
        if (ExpandUtil.shouldExpand(expand, "associatedDomains") && clazz.getAssociatedDomains() != null) {
            builder.associatedDomains(clazz.getAssociatedDomains().stream()
                    .map(domain -> DomainResponse.builder()
                            .id(domain.getId())
                            .name(domain.getName())
                            .iconUrl(domain.getIconUrl())
                            .description(domain.getDescription())
                            .expansionId(domain.getExpansion().getId())
                            .createdAt(domain.getCreatedAt())
                            .lastModifiedAt(domain.getLastModifiedAt())
                            .deletedAt(domain.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Expand hope features if requested
        if (ExpandUtil.shouldExpand(expand, "hopeFeatures") && clazz.getHopeFeatures() != null) {
            builder.hopeFeatures(clazz.getHopeFeatures().stream()
                    .map(feature -> featureService.toResponse(feature, expand))
                    .collect(Collectors.toList()));
        }

        // Expand class features if requested
        if (ExpandUtil.shouldExpand(expand, "classFeatures") && clazz.getClassFeatures() != null) {
            builder.classFeatures(clazz.getClassFeatures().stream()
                    .map(feature -> featureService.toResponse(feature, expand))
                    .collect(Collectors.toList()));
        }

        // Expand background questions if requested
        if (ExpandUtil.shouldExpand(expand, "backgroundQuestions") && clazz.getBackgroundQuestions() != null) {
            builder.backgroundQuestions(clazz.getBackgroundQuestions().stream()
                    .map(question -> QuestionResponse.builder()
                            .id(question.getId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType())
                            .expansionId(question.getExpansion().getId())
                            .createdAt(question.getCreatedAt())
                            .lastModifiedAt(question.getLastModifiedAt())
                            .deletedAt(question.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Expand connection questions if requested
        if (ExpandUtil.shouldExpand(expand, "connectionQuestions") && clazz.getConnectionQuestions() != null) {
            builder.connectionQuestions(clazz.getConnectionQuestions().stream()
                    .map(question -> QuestionResponse.builder()
                            .id(question.getId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType())
                            .expansionId(question.getExpansion().getId())
                            .createdAt(question.getCreatedAt())
                            .lastModifiedAt(question.getLastModifiedAt())
                            .deletedAt(question.getDeletedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
