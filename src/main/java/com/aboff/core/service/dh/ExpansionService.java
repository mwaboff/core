package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateExpansionRequest;
import com.aboff.core.model.dto.dh.request.UpdateExpansionRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.event.EntityChangeEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing Expansion entities.
 * Handles business logic for CRUD operations, pagination, and soft deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpansionService {

    private final ExpansionRepository expansionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of expansions.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted expansions
     * @param published Optional filter for published status
     * @return Paginated response containing expansions
     */
    @Transactional(readOnly = true)
    public PagedResponse<ExpansionResponse> getAllExpansions(
            int page,
            int size,
            boolean includeDeleted,
            Boolean published) {

        // Limit page size to 100
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Expansion> expansionPage;

        if (includeDeleted) {
            // Include deleted items (admin only)
            expansionPage = expansionRepository.findAllWithPublished(published, pageable);
        } else {
            // Exclude deleted items (default)
            expansionPage = expansionRepository.findByDeletedAtIsNullAndPublished(published, pageable);
        }

        return PagedResponse.<ExpansionResponse>builder()
                .content(expansionPage.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .totalElements(expansionPage.getTotalElements())
                .totalPages(expansionPage.getTotalPages())
                .currentPage(expansionPage.getNumber())
                .pageSize(expansionPage.getSize())
                .build();
    }

    /**
     * Retrieves a single expansion by ID.
     *
     * @param id The expansion ID
     * @return ExpansionResponse containing the expansion details
     * @throws EntityNotFoundException if the expansion is not found or is deleted
     */
    @Transactional(readOnly = true)
    public ExpansionResponse getExpansionById(Long id) {
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Expansion not found with id: " + id));

        return toResponse(expansion);
    }

    /**
     * Creates a new expansion.
     *
     * @param request The creation request containing expansion details
     * @return ExpansionResponse containing the created expansion
     */
    @Transactional
    public ExpansionResponse createExpansion(CreateExpansionRequest request) {
        log.info("Creating new expansion with name: {}", request.getName());

        Expansion expansion = Expansion.builder()
                .name(request.getName())
                .isPublished(request.getIsPublished())
                .build();

        Expansion savedExpansion = expansionRepository.save(expansion);
        log.info("Created expansion with id: {}", savedExpansion.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedExpansion, EntityChangeEvent.ChangeType.CREATED));

        return toResponse(savedExpansion);
    }

    /**
     * Updates an existing expansion.
     *
     * @param id The expansion ID to update
     * @param request The update request containing new expansion details
     * @return ExpansionResponse containing the updated expansion
     * @throws EntityNotFoundException if the expansion is not found or is deleted
     */
    @Transactional
    public ExpansionResponse updateExpansion(Long id, UpdateExpansionRequest request) {
        log.info("Updating expansion with id: {}", id);

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Expansion not found with id: " + id));

        expansion.setName(request.getName());
        expansion.setIsPublished(request.getIsPublished());

        Expansion updatedExpansion = expansionRepository.save(expansion);
        log.info("Updated expansion with id: {}", updatedExpansion.getId());
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedExpansion, EntityChangeEvent.ChangeType.UPDATED));

        return toResponse(updatedExpansion);
    }

    /**
     * Soft deletes an expansion by setting its deletedAt timestamp.
     *
     * @param id The expansion ID to delete
     * @throws EntityNotFoundException if the expansion is not found or is already deleted
     */
    @Transactional
    public void deleteExpansion(Long id) {
        log.info("Soft deleting expansion with id: {}", id);

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Expansion not found with id: " + id));

        expansion.softDelete();
        expansionRepository.save(expansion);
        eventPublisher.publishEvent(new EntityChangeEvent(this, expansion, EntityChangeEvent.ChangeType.SOFT_DELETED));

        log.info("Soft deleted expansion with id: {}", id);
    }

    /**
     * Restores a soft-deleted expansion.
     *
     * @param id The expansion ID to restore
     * @return ExpansionResponse containing the restored expansion
     * @throws EntityNotFoundException if the expansion is not found
     * @throws IllegalStateException if the expansion is not deleted
     */
    @Transactional
    public ExpansionResponse restoreExpansion(Long id) {
        log.info("Restoring expansion with id: {}", id);

        Expansion expansion = expansionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expansion not found with id: " + id));

        if (!expansion.isDeleted()) {
            throw new IllegalStateException("Expansion with id " + id + " is not deleted");
        }

        expansion.restore();
        Expansion restoredExpansion = expansionRepository.save(expansion);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredExpansion, EntityChangeEvent.ChangeType.RESTORED));

        log.info("Restored expansion with id: {}", id);

        return toResponse(restoredExpansion);
    }

    /**
     * Converts an Expansion entity to ExpansionResponse DTO.
     *
     * @param expansion The expansion entity
     * @return ExpansionResponse DTO
     */
    private ExpansionResponse toResponse(Expansion expansion) {
        return ExpansionResponse.builder()
                .id(expansion.getId())
                .name(expansion.getName())
                .isPublished(expansion.getIsPublished())
                .createdAt(expansion.getCreatedAt())
                .lastModifiedAt(expansion.getLastModifiedAt())
                .deletedAt(expansion.getDeletedAt())
                .build();
    }
}
