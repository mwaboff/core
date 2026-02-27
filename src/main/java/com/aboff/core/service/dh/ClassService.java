package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateClassRequest;
import com.aboff.core.model.dto.dh.request.UpdateClassRequest;
import com.aboff.core.model.dto.dh.response.CardCostTagResponse;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.dto.dh.response.FeatureResponse;
import com.aboff.core.model.dto.dh.response.QuestionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Class;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.repository.dh.ClassRepository;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.FeatureRepository;
import com.aboff.core.repository.dh.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aboff.core.util.ExpandUtil;

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
    private final FeatureRepository featureRepository;
    private final QuestionRepository questionRepository;

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
     * @return ClassResponse containing the created class
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        log.info("Creating new class with name: {}", request.getName());

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

        // Set many-to-many relationships
        if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
            Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
            clazz.setAssociatedDomains(domains);
        }

        if (request.getHopeFeatureIds() != null && !request.getHopeFeatureIds().isEmpty()) {
            Set<Feature> hopeFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getHopeFeatureIds()));
            clazz.setHopeFeatures(hopeFeatures);
        }

        if (request.getClassFeatureIds() != null && !request.getClassFeatureIds().isEmpty()) {
            Set<Feature> classFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getClassFeatureIds()));
            clazz.setClassFeatures(classFeatures);
        }

        if (request.getBackgroundQuestionIds() != null && !request.getBackgroundQuestionIds().isEmpty()) {
            Set<Question> backgroundQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getBackgroundQuestionIds()));
            clazz.setBackgroundQuestions(backgroundQuestions);
        }

        if (request.getConnectionQuestionIds() != null && !request.getConnectionQuestionIds().isEmpty()) {
            Set<Question> connectionQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getConnectionQuestionIds()));
            clazz.setConnectionQuestions(connectionQuestions);
        }

        Class savedClass = classRepository.save(clazz);
        log.info("Created class with id: {}", savedClass.getId());

        return toResponse(savedClass, Set.of());
    }

    /**
     * Creates multiple classes in bulk.
     *
     * @param requests List of creation requests
     * @return List of created class responses
     */
    @Transactional
    public List<ClassResponse> createClassesBulk(List<CreateClassRequest> requests) {
        log.info("Creating {} classes in bulk", requests.size());

        List<Class> classes = requests.stream()
                .map(request -> {
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

                    // Set many-to-many relationships
                    if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
                        Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                        clazz.setAssociatedDomains(domains);
                    }

                    if (request.getHopeFeatureIds() != null && !request.getHopeFeatureIds().isEmpty()) {
                        Set<Feature> hopeFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getHopeFeatureIds()));
                        clazz.setHopeFeatures(hopeFeatures);
                    }

                    if (request.getClassFeatureIds() != null && !request.getClassFeatureIds().isEmpty()) {
                        Set<Feature> classFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getClassFeatureIds()));
                        clazz.setClassFeatures(classFeatures);
                    }

                    if (request.getBackgroundQuestionIds() != null && !request.getBackgroundQuestionIds().isEmpty()) {
                        Set<Question> backgroundQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getBackgroundQuestionIds()));
                        clazz.setBackgroundQuestions(backgroundQuestions);
                    }

                    if (request.getConnectionQuestionIds() != null && !request.getConnectionQuestionIds().isEmpty()) {
                        Set<Question> connectionQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getConnectionQuestionIds()));
                        clazz.setConnectionQuestions(connectionQuestions);
                    }

                    return clazz;
                })
                .toList();

        List<Class> savedClasses = classRepository.saveAll(classes);
        log.info("Created {} classes in bulk", savedClasses.size());

        return savedClasses.stream()
                .map(clazz -> toResponse(clazz, Set.of()))
                .toList();
    }

    /**
     * Updates an existing class.
     *
     * @param id The class ID to update
     * @param request The update request containing new class details
     * @return ClassResponse containing the updated class
     * @throws EntityNotFoundException if the class or referenced entities are not found
     */
    @Transactional
    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        log.info("Updating class with id: {}", id);

        Class clazz = classRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        clazz.setName(request.getName());
        clazz.setDescription(request.getDescription());
        clazz.setExpansion(expansion);
        clazz.setStartingClassItems(request.getStartingClassItems());
        clazz.setStartingEvasion(request.getStartingEvasion());
        clazz.setStartingHitPoints(request.getStartingHitPoints());

        // Update many-to-many relationships
        if (request.getAssociatedDomainIds() != null) {
            if (request.getAssociatedDomainIds().isEmpty()) {
                clazz.setAssociatedDomains(new HashSet<>());
            } else {
                Set<Domain> domains = new HashSet<>(domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                clazz.setAssociatedDomains(domains);
            }
        }

        if (request.getHopeFeatureIds() != null) {
            if (request.getHopeFeatureIds().isEmpty()) {
                clazz.setHopeFeatures(new HashSet<>());
            } else {
                Set<Feature> hopeFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getHopeFeatureIds()));
                clazz.setHopeFeatures(hopeFeatures);
            }
        }

        if (request.getClassFeatureIds() != null) {
            if (request.getClassFeatureIds().isEmpty()) {
                clazz.setClassFeatures(new HashSet<>());
            } else {
                Set<Feature> classFeatures = new HashSet<>(featureRepository.findAllByIdInAndDeletedAtIsNull(request.getClassFeatureIds()));
                clazz.setClassFeatures(classFeatures);
            }
        }

        if (request.getBackgroundQuestionIds() != null) {
            if (request.getBackgroundQuestionIds().isEmpty()) {
                clazz.setBackgroundQuestions(new HashSet<>());
            } else {
                Set<Question> backgroundQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getBackgroundQuestionIds()));
                clazz.setBackgroundQuestions(backgroundQuestions);
            }
        }

        if (request.getConnectionQuestionIds() != null) {
            if (request.getConnectionQuestionIds().isEmpty()) {
                clazz.setConnectionQuestions(new HashSet<>());
            } else {
                Set<Question> connectionQuestions = new HashSet<>(questionRepository.findAllByIdInAndDeletedAtIsNull(request.getConnectionQuestionIds()));
                clazz.setConnectionQuestions(connectionQuestions);
            }
        }

        Class updatedClass = classRepository.save(clazz);
        log.info("Updated class with id: {}", updatedClass.getId());

        return toResponse(updatedClass, Set.of());
    }

    /**
     * Soft deletes a class by setting its deletedAt timestamp.
     *
     * @param id The class ID to delete
     * @throws EntityNotFoundException if the class is not found or is already deleted
     */
    @Transactional
    public void deleteClass(Long id) {
        log.info("Soft deleting class with id: {}", id);

        Class clazz = classRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        clazz.softDelete();
        classRepository.save(clazz);

        log.info("Soft deleted class with id: {}", id);
    }

    /**
     * Restores a soft-deleted class.
     *
     * @param id The class ID to restore
     * @return ClassResponse containing the restored class
     * @throws EntityNotFoundException if the class is not found
     * @throws IllegalStateException if the class is not deleted
     */
    @Transactional
    public ClassResponse restoreClass(Long id) {
        log.info("Restoring class with id: {}", id);

        Class clazz = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + id));

        if (!clazz.isDeleted()) {
            throw new IllegalStateException("Class with id " + id + " is not deleted");
        }

        clazz.restore();
        Class restoredClass = classRepository.save(clazz);

        log.info("Restored class with id: {}", id);

        return toResponse(restoredClass, Set.of());
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
        if (expand.contains("expansion")) {
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
        if (expand.contains("associatedDomains") && clazz.getAssociatedDomains() != null) {
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
        if (expand.contains("hopeFeatures") && clazz.getHopeFeatures() != null) {
            builder.hopeFeatures(clazz.getHopeFeatures().stream()
                    .map(feature -> {
                        FeatureResponse.FeatureResponseBuilder featureBuilder = FeatureResponse.builder()
                                .id(feature.getId())
                                .name(feature.getName())
                                .description(feature.getDescription())
                                .featureType(feature.getFeatureType())
                                .expansionId(feature.getExpansion().getId())
                                .createdAt(feature.getCreatedAt())
                                .lastModifiedAt(feature.getLastModifiedAt())
                                .deletedAt(feature.getDeletedAt());

                        // Always include cost tag IDs
                        if (feature.getCostTags() != null) {
                            featureBuilder.costTagIds(feature.getCostTags().stream()
                                    .map(CardCostTag::getId)
                                    .collect(Collectors.toList()));
                        }

                        // Expand cost tags if requested
                        if (expand.contains("costTags") && feature.getCostTags() != null) {
                            featureBuilder.costTags(feature.getCostTags().stream()
                                    .map(tag -> CardCostTagResponse.builder()
                                            .id(tag.getId())
                                            .label(tag.getLabel())
                                            .category(tag.getCategory())
                                            .createdAt(tag.getCreatedAt())
                                            .lastModifiedAt(tag.getLastModifiedAt())
                                            .deletedAt(tag.getDeletedAt())
                                            .build())
                                    .collect(Collectors.toList()));
                        }

                        return featureBuilder.build();
                    })
                    .collect(Collectors.toList()));
        }

        // Expand class features if requested
        if (expand.contains("classFeatures") && clazz.getClassFeatures() != null) {
            builder.classFeatures(clazz.getClassFeatures().stream()
                    .map(feature -> {
                        FeatureResponse.FeatureResponseBuilder featureBuilder = FeatureResponse.builder()
                                .id(feature.getId())
                                .name(feature.getName())
                                .description(feature.getDescription())
                                .featureType(feature.getFeatureType())
                                .expansionId(feature.getExpansion().getId())
                                .createdAt(feature.getCreatedAt())
                                .lastModifiedAt(feature.getLastModifiedAt())
                                .deletedAt(feature.getDeletedAt());

                        // Always include cost tag IDs
                        if (feature.getCostTags() != null) {
                            featureBuilder.costTagIds(feature.getCostTags().stream()
                                    .map(CardCostTag::getId)
                                    .collect(Collectors.toList()));
                        }

                        // Expand cost tags if requested
                        if (expand.contains("costTags") && feature.getCostTags() != null) {
                            featureBuilder.costTags(feature.getCostTags().stream()
                                    .map(tag -> CardCostTagResponse.builder()
                                            .id(tag.getId())
                                            .label(tag.getLabel())
                                            .category(tag.getCategory())
                                            .createdAt(tag.getCreatedAt())
                                            .lastModifiedAt(tag.getLastModifiedAt())
                                            .deletedAt(tag.getDeletedAt())
                                            .build())
                                    .collect(Collectors.toList()));
                        }

                        return featureBuilder.build();
                    })
                    .collect(Collectors.toList()));
        }

        // Expand background questions if requested
        if (expand.contains("backgroundQuestions") && clazz.getBackgroundQuestions() != null) {
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
        if (expand.contains("connectionQuestions") && clazz.getConnectionQuestions() != null) {
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
