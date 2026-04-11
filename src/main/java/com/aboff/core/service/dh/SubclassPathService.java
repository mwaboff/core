package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateSubclassPathRequest;
import com.aboff.core.model.dto.dh.request.SubclassPathInput;
import com.aboff.core.model.dto.dh.request.UpdateSubclassPathRequest;
import com.aboff.core.model.dto.dh.response.ClassResponse;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
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
import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing SubclassPath entities.
 * Handles business logic for CRUD operations, pagination, soft deletion,
 * relationship expansion, and find-or-create functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubclassPathService {

    private final SubclassPathRepository subclassPathRepository;
    private final ExpansionRepository expansionRepository;
    private final ClassRepository classRepository;
    private final DomainRepository domainRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of subclass paths.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted paths
     * @param classId Optional filter for associated class ID
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing subclass paths
     */
    @Transactional(readOnly = true)
    public PagedResponse<SubclassPathResponse> getAllSubclassPaths(
            int page,
            int size,
            boolean includeDeleted,
            Long classId,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<SubclassPath> pathPage;

        if (includeDeleted) {
            pathPage = subclassPathRepository.findAllWithFilters(classId, pageable);
        } else {
            pathPage = subclassPathRepository.findByDeletedAtIsNullAndFilters(classId, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<SubclassPathResponse>builder()
                .content(pathPage.getContent().stream()
                        .map(path -> toResponse(path, expandSet))
                        .toList())
                .totalElements(pathPage.getTotalElements())
                .totalPages(pathPage.getTotalPages())
                .currentPage(pathPage.getNumber())
                .pageSize(pathPage.getSize())
                .build();
    }

    /**
     * Retrieves a single subclass path by ID.
     *
     * @param id The subclass path ID
     * @param expand Comma-separated list of relationships to expand
     * @return SubclassPathResponse containing the path details
     * @throws EntityNotFoundException if the path is not found or is deleted
     */
    @Transactional(readOnly = true)
    public SubclassPathResponse getSubclassPathById(Long id, String expand) {
        SubclassPath path = subclassPathRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassPath not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(path, expandSet);
    }

    /**
     * Creates a new subclass path.
     *
     * @param request The creation request containing path details
     * @return SubclassPathResponse containing the created path
     * @throws EntityNotFoundException if the referenced class, expansion, or domains are not found
     */
    @Transactional
    public SubclassPathResponse createSubclassPath(CreateSubclassPathRequest request) {
        log.info("Creating new subclass path with name: {}", request.getName());

        Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Class not found with id: " + request.getAssociatedClassId()));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        SubclassPath path = SubclassPath.builder()
                .name(request.getName())
                .associatedClass(associatedClass)
                .spellcastingTrait(request.getSpellcastingTrait())
                .expansion(expansion)
                .build();

        if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
            Set<Domain> domains = new HashSet<>(
                    domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
            path.setAssociatedDomains(domains);
        }

        SubclassPath savedPath = subclassPathRepository.save(path);
        log.info("Created subclass path with id: {}", savedPath.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedPath, EntityChangeEvent.ChangeType.CREATED));

        return toResponse(savedPath, Set.of());
    }

    /**
     * Creates multiple subclass paths in bulk.
     *
     * @param requests List of creation requests
     * @return List of created subclass path responses
     */
    @Transactional
    public List<SubclassPathResponse> createSubclassPathsBulk(List<CreateSubclassPathRequest> requests) {
        log.info("Creating {} subclass paths in bulk", requests.size());

        List<SubclassPath> paths = requests.stream()
                .map(request -> {
                    Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Class not found with id: " + request.getAssociatedClassId()));

                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    SubclassPath path = SubclassPath.builder()
                            .name(request.getName())
                            .associatedClass(associatedClass)
                            .spellcastingTrait(request.getSpellcastingTrait())
                            .expansion(expansion)
                            .build();

                    if (request.getAssociatedDomainIds() != null && !request.getAssociatedDomainIds().isEmpty()) {
                        Set<Domain> domains = new HashSet<>(
                                domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                        path.setAssociatedDomains(domains);
                    }

                    return path;
                })
                .collect(Collectors.toList());

        List<SubclassPath> savedPaths = subclassPathRepository.saveAll(paths);
        log.info("Created {} subclass paths in bulk", savedPaths.size());
        savedPaths.forEach(p -> eventPublisher.publishEvent(new EntityChangeEvent(this, p, EntityChangeEvent.ChangeType.CREATED)));

        return savedPaths.stream()
                .map(path -> toResponse(path, Set.of()))
                .toList();
    }

    /**
     * Updates an existing subclass path.
     *
     * @param id The subclass path ID to update
     * @param request The update request containing new path details
     * @return SubclassPathResponse containing the updated path
     * @throws EntityNotFoundException if the path, class, expansion, or domains are not found
     */
    @Transactional
    public SubclassPathResponse updateSubclassPath(Long id, UpdateSubclassPathRequest request) {
        log.info("Updating subclass path with id: {}", id);

        SubclassPath path = subclassPathRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassPath not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            path.setName(request.getName());
        }
        if (request.getAssociatedClassId() != null) {
            Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(request.getAssociatedClassId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Class not found with id: " + request.getAssociatedClassId()));
            path.setAssociatedClass(associatedClass);
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            path.setExpansion(expansion);
        }
        if (request.getSpellcastingTrait() != null) {
            path.setSpellcastingTrait(request.getSpellcastingTrait());
        }

        if (request.getAssociatedDomainIds() != null) {
            if (request.getAssociatedDomainIds().isEmpty()) {
                path.setAssociatedDomains(new HashSet<>());
            } else {
                Set<Domain> domains = new HashSet<>(
                        domainRepository.findAllByIdInAndDeletedAtIsNull(request.getAssociatedDomainIds()));
                path.setAssociatedDomains(domains);
            }
        }

        SubclassPath updatedPath = subclassPathRepository.save(path);
        log.info("Updated subclass path with id: {}", updatedPath.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedPath, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedPath, Set.of());
    }

    /**
     * Soft deletes a subclass path by setting its deletedAt timestamp.
     *
     * @param id The subclass path ID to delete
     * @throws EntityNotFoundException if the path is not found or is already deleted
     */
    @Transactional
    public void deleteSubclassPath(Long id) {
        log.info("Soft deleting subclass path with id: {}", id);

        SubclassPath path = subclassPathRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassPath not found with id: " + id));

        path.softDelete();
        subclassPathRepository.save(path);
        eventPublisher.publishEvent(new EntityChangeEvent(this, path, EntityChangeEvent.ChangeType.SOFT_DELETED));

        log.info("Soft deleted subclass path with id: {}", id);
    }

    /**
     * Restores a soft-deleted subclass path.
     *
     * @param id The subclass path ID to restore
     * @return SubclassPathResponse containing the restored path
     * @throws EntityNotFoundException if the path is not found
     * @throws IllegalStateException if the path is not deleted
     */
    @Transactional
    public SubclassPathResponse restoreSubclassPath(Long id) {
        log.info("Restoring subclass path with id: {}", id);

        SubclassPath path = subclassPathRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubclassPath not found with id: " + id));

        if (!path.isDeleted()) {
            throw new IllegalStateException("SubclassPath with id " + id + " is not deleted");
        }

        path.restore();
        SubclassPath restoredPath = subclassPathRepository.save(path);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredPath, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored subclass path with id: {}", id);

        return toResponse(restoredPath, Set.of());
    }

    /**
     * Finds an existing subclass path by name and class, or creates a new one if not found.
     * The name lookup is case-insensitive. If an existing path is found with missing
     * attributes (empty domains or null spellcasting trait), those attributes are updated
     * from the provided parameters.
     *
     * @param name The path name to find or create
     * @param classId The associated class ID
     * @param expansionId The expansion ID (used only when creating)
     * @param domainIds The domain IDs to associate (used when creating or backfilling empty domains)
     * @param spellcastingTrait The spellcasting trait (used when creating or backfilling null trait)
     * @return The found or newly created SubclassPath
     * @throws EntityNotFoundException if the class, expansion, or domains are not found
     */
    @Transactional
    public SubclassPath findOrCreate(String name, Long classId, Long expansionId,
                                     List<Long> domainIds, Trait spellcastingTrait) {
        log.info("Finding or creating subclass path with name: {} for class id: {}", name, classId);

        return subclassPathRepository.findByNameIgnoreCaseAndAssociatedClassIdAndDeletedAtIsNull(name, classId)
                .map(existingPath -> {
                    boolean updated = false;

                    // Update domains if provided and currently empty
                    if (domainIds != null && !domainIds.isEmpty()
                            && (existingPath.getAssociatedDomains() == null
                                || existingPath.getAssociatedDomains().isEmpty())) {
                        Set<Domain> domains = new HashSet<>(
                                domainRepository.findAllByIdInAndDeletedAtIsNull(domainIds));
                        existingPath.setAssociatedDomains(domains);
                        updated = true;
                    }

                    // Update spellcasting trait if provided and currently null
                    if (spellcastingTrait != null && existingPath.getSpellcastingTrait() == null) {
                        existingPath.setSpellcastingTrait(spellcastingTrait);
                        updated = true;
                    }

                    if (updated) {
                        log.info("Updated existing subclass path with id: {} with missing attributes",
                                existingPath.getId());
                        return subclassPathRepository.save(existingPath);
                    }

                    return existingPath;
                })
                .orElseGet(() -> {
                    log.info("SubclassPath not found, creating new path with name: {}", name);

                    Class associatedClass = classRepository.findByIdAndDeletedAtIsNull(classId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Class not found with id: " + classId));

                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(expansionId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + expansionId));

                    SubclassPath newPath = SubclassPath.builder()
                            .name(name)
                            .associatedClass(associatedClass)
                            .spellcastingTrait(spellcastingTrait)
                            .expansion(expansion)
                            .build();

                    if (domainIds != null && !domainIds.isEmpty()) {
                        Set<Domain> domains = new HashSet<>(
                                domainRepository.findAllByIdInAndDeletedAtIsNull(domainIds));
                        newPath.setAssociatedDomains(domains);
                    }

                    SubclassPath savedPath = subclassPathRepository.save(newPath);
                    log.info("Created new subclass path with id: {}", savedPath.getId());
                    return savedPath;
                });
    }

    /**
     * Resolves a SubclassPath from either a direct ID reference or an inline path input.
     * Exactly one of subclassPathId or pathInput must be provided.
     *
     * @param subclassPathId Direct ID of an existing SubclassPath (mutually exclusive with pathInput)
     * @param pathInput Inline path details for find-or-create (mutually exclusive with subclassPathId)
     * @param associatedClassId The class ID to use when finding or creating via pathInput
     * @param expansionId The expansion ID to use when creating via pathInput
     * @return The resolved SubclassPath entity
     * @throws IllegalArgumentException if both or neither parameters are provided
     * @throws EntityNotFoundException if the referenced SubclassPath is not found
     */
    @Transactional
    public SubclassPath resolvePath(Long subclassPathId, SubclassPathInput pathInput,
                                    Long associatedClassId, Long expansionId) {
        if (subclassPathId != null && pathInput != null) {
            throw new IllegalArgumentException("Cannot specify both subclassPathId and subclassPath input");
        }

        if (subclassPathId != null) {
            return subclassPathRepository.findByIdAndDeletedAtIsNull(subclassPathId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "SubclassPath not found with id: " + subclassPathId));
        }

        if (pathInput != null) {
            return findOrCreate(
                    pathInput.getName(),
                    associatedClassId,
                    expansionId,
                    pathInput.getAssociatedDomainIds(),
                    pathInput.getSpellcastingTrait());
        }

        throw new IllegalArgumentException("Either subclassPathId or subclassPath input must be provided");
    }

    /**
     * Converts a SubclassPath entity to SubclassPathResponse DTO.
     *
     * @param path The subclass path entity
     * @param expand Set of relationships to expand
     * @return SubclassPathResponse DTO
     */
    public SubclassPathResponse toResponse(SubclassPath path, Set<String> expand) {
        SubclassPathResponse.SubclassPathResponseBuilder builder = SubclassPathResponse.builder()
                .id(path.getId())
                .name(path.getName())
                .associatedClassId(path.getAssociatedClass().getId())
                .expansionId(path.getExpansion().getId())
                .createdAt(path.getCreatedAt())
                .lastModifiedAt(path.getLastModifiedAt())
                .deletedAt(path.getDeletedAt());

        // Add spellcasting trait info if present
        if (path.getSpellcastingTrait() != null) {
            builder.spellcastingTrait(SubclassPathResponse.TraitInfo.builder()
                    .trait(path.getSpellcastingTrait())
                    .description(path.getSpellcastingTrait().getDescription())
                    .examples(path.getSpellcastingTrait().getExamples())
                    .build());
        }

        // Always include associated domain IDs
        if (path.getAssociatedDomains() != null) {
            builder.associatedDomainIds(path.getAssociatedDomains().stream()
                    .map(Domain::getId)
                    .collect(Collectors.toList()));
        }

        // Expand associated class if requested
        if (ExpandUtil.shouldExpand(expand, "associatedClass")) {
            Class clazz = path.getAssociatedClass();
            builder.associatedClass(ClassResponse.builder()
                    .id(clazz.getId())
                    .name(clazz.getName())
                    .description(clazz.getDescription())
                    .expansionId(clazz.getExpansion().getId())
                    .startingClassItems(clazz.getStartingClassItems())
                    .startingEvasion(clazz.getStartingEvasion())
                    .startingHitPoints(clazz.getStartingHitPoints())
                    .createdAt(clazz.getCreatedAt())
                    .lastModifiedAt(clazz.getLastModifiedAt())
                    .deletedAt(clazz.getDeletedAt())
                    .build());
        }

        // Expand associated domains if requested
        if (ExpandUtil.shouldExpand(expand, "associatedDomains") && path.getAssociatedDomains() != null) {
            builder.associatedDomains(path.getAssociatedDomains().stream()
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

        // Expand expansion if requested
        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = path.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        return builder.build();
    }
}
