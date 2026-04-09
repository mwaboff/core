package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateDomainRequest;
import com.aboff.core.model.dto.dh.request.UpdateDomainRequest;
import com.aboff.core.model.dto.dh.response.DomainResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.repository.dh.DomainRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Service for managing Domain entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainService {

    private final DomainRepository domainRepository;
    private final ExpansionRepository expansionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of domains.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted domains
     * @param expansionId Optional filter for expansion ID
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing domains
     */
    @Transactional(readOnly = true)
    public PagedResponse<DomainResponse> getAllDomains(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            String expand) {

        // Limit page size to 100
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Domain> domainPage;

        if (includeDeleted) {
            // Include deleted items (admin only)
            domainPage = domainRepository.findAllWithExpansion(expansionId, pageable);
        } else {
            // Exclude deleted items (default)
            domainPage = domainRepository.findByDeletedAtIsNullAndExpansion(expansionId, pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<DomainResponse>builder()
                .content(domainPage.getContent().stream()
                        .map(domain -> toResponse(domain, expandSet))
                        .toList())
                .totalElements(domainPage.getTotalElements())
                .totalPages(domainPage.getTotalPages())
                .currentPage(domainPage.getNumber())
                .pageSize(domainPage.getSize())
                .build();
    }

    /**
     * Retrieves a single domain by ID.
     *
     * @param id The domain ID
     * @param expand Comma-separated list of relationships to expand
     * @return DomainResponse containing the domain details
     * @throws EntityNotFoundException if the domain is not found or is deleted
     */
    @Transactional(readOnly = true)
    public DomainResponse getDomainById(Long id, String expand) {
        Domain domain = domainRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(domain, expandSet);
    }

    /**
     * Creates a new domain.
     *
     * @param request The creation request containing domain details
     * @return DomainResponse containing the created domain
     * @throws EntityNotFoundException if the expansion is not found
     */
    @Transactional
    public DomainResponse createDomain(CreateDomainRequest request) {
        log.info("Creating new domain with name: {}", request.getName());

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Domain domain = Domain.builder()
                .name(request.getName())
                .iconUrl(request.getIconUrl())
                .description(request.getDescription())
                .expansion(expansion)
                .build();

        Domain savedDomain = domainRepository.save(domain);
        log.info("Created domain with id: {}", savedDomain.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedDomain, EntityChangeEvent.ChangeType.CREATED));

        return toResponse(savedDomain, Set.of());
    }

    /**
     * Creates multiple domains in bulk.
     *
     * @param requests List of creation requests
     * @return List of created domain responses
     */
    @Transactional
    public List<DomainResponse> createDomainsBulk(List<CreateDomainRequest> requests) {
        log.info("Creating {} domains in bulk", requests.size());

        List<Domain> domains = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    return Domain.builder()
                            .name(request.getName())
                            .iconUrl(request.getIconUrl())
                            .description(request.getDescription())
                            .expansion(expansion)
                            .build();
                })
                .collect(Collectors.toList());

        List<Domain> savedDomains = domainRepository.saveAll(domains);
        log.info("Created {} domains in bulk", savedDomains.size());
        savedDomains.forEach(d -> eventPublisher.publishEvent(new EntityChangeEvent(this, d, EntityChangeEvent.ChangeType.CREATED)));

        return savedDomains.stream()
                .map(domain -> toResponse(domain, Set.of()))
                .toList();
    }

    /**
     * Updates an existing domain.
     *
     * @param id The domain ID to update
     * @param request The update request containing new domain details
     * @return DomainResponse containing the updated domain
     * @throws EntityNotFoundException if the domain or expansion is not found
     */
    @Transactional
    public DomainResponse updateDomain(Long id, UpdateDomainRequest request) {
        log.info("Updating domain with id: {}", id);

        Domain domain = domainRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found with id: " + id));

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        domain.setName(request.getName());
        domain.setIconUrl(request.getIconUrl());
        domain.setDescription(request.getDescription());
        domain.setExpansion(expansion);

        Domain updatedDomain = domainRepository.save(domain);
        log.info("Updated domain with id: {}", updatedDomain.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedDomain, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedDomain, Set.of());
    }

    /**
     * Soft deletes a domain by setting its deletedAt timestamp.
     *
     * @param id The domain ID to delete
     * @throws EntityNotFoundException if the domain is not found or is already deleted
     */
    @Transactional
    public void deleteDomain(Long id) {
        log.info("Soft deleting domain with id: {}", id);

        Domain domain = domainRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found with id: " + id));

        domain.softDelete();
        domainRepository.save(domain);
        eventPublisher.publishEvent(new EntityChangeEvent(this, domain, EntityChangeEvent.ChangeType.SOFT_DELETED));

        log.info("Soft deleted domain with id: {}", id);
    }

    /**
     * Restores a soft-deleted domain.
     *
     * @param id The domain ID to restore
     * @return DomainResponse containing the restored domain
     * @throws EntityNotFoundException if the domain is not found
     * @throws IllegalStateException if the domain is not deleted
     */
    @Transactional
    public DomainResponse restoreDomain(Long id) {
        log.info("Restoring domain with id: {}", id);

        Domain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found with id: " + id));

        if (!domain.isDeleted()) {
            throw new IllegalStateException("Domain with id " + id + " is not deleted");
        }

        domain.restore();
        Domain restoredDomain = domainRepository.save(domain);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredDomain, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored domain with id: {}", id);

        return toResponse(restoredDomain, Set.of());
    }

    /**
     * Converts a Domain entity to DomainResponse DTO.
     *
     * @param domain The domain entity
     * @param expand Set of relationships to expand
     * @return DomainResponse DTO
     */
    private DomainResponse toResponse(Domain domain, Set<String> expand) {
        DomainResponse.DomainResponseBuilder builder = DomainResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .iconUrl(domain.getIconUrl())
                .description(domain.getDescription())
                .expansionId(domain.getExpansion().getId())
                .createdAt(domain.getCreatedAt())
                .lastModifiedAt(domain.getLastModifiedAt())
                .deletedAt(domain.getDeletedAt());

        // Expand expansion if requested
        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = domain.getExpansion();
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
