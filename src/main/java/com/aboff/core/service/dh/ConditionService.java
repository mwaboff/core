package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateConditionRequest;
import com.aboff.core.model.dto.dh.response.ConditionResponse;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.event.EntityChangeEvent;
import com.aboff.core.util.ContentRedaction;
import com.aboff.core.util.ExpandUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for managing Condition entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship
 * expansion for the conditions catalogue (e.g., Restrained, Vulnerable, Drained, Hexed,
 * Chained, Ignited).
 * </p>
 * <p>
 * Follows the same official/custom content pattern as {@code Weapon}/{@code Beastform}: mutation
 * endpoints are restricted to ADMIN/OWNER at the controller level, and creation respects the
 * caller-supplied {@code isOfficial} value rather than hardcoding it, so official conditions can
 * be bulk-imported from the rulebook.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final ExpansionRepository expansionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ContentAccessService contentAccessService;

    /**
     * Retrieves a paginated list of conditions.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted conditions
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing conditions
     */
    @Transactional(readOnly = true)
    public PagedResponse<ConditionResponse> getAllConditions(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Condition> conditionPage;

        if (contentAccessService.resolveIncludeDeleted(includeDeleted)) {
            conditionPage = conditionRepository.findAllWithFilters(expansionId, isOfficial, pageable);
        } else {
            conditionPage = conditionRepository.findByDeletedAtIsNullAndFilters(
                    expansionId, isOfficial, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<ConditionResponse>builder()
                .content(conditionPage.getContent().stream()
                        .map(condition -> toResponse(condition, expandSet))
                        .toList())
                .totalElements(conditionPage.getTotalElements())
                .totalPages(conditionPage.getTotalPages())
                .currentPage(conditionPage.getNumber())
                .pageSize(conditionPage.getSize())
                .build();
    }

    /**
     * Retrieves a single condition by ID.
     *
     * @param id The condition ID
     * @param expand Comma-separated list of relationships to expand
     * @return ConditionResponse containing the condition details
     * @throws EntityNotFoundException if the condition is not found or is deleted
     */
    @Transactional(readOnly = true)
    public ConditionResponse getConditionById(Long id, String expand) {
        Condition condition = conditionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Condition not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(condition, expandSet);
    }

    /**
     * Creates a new condition.
     *
     * @param request The creation request containing condition details
     * @param authentication The authentication of the current user
     * @return ConditionResponse containing the created condition
     * @throws EntityNotFoundException if the referenced expansion is not found
     */
    @Transactional
    public ConditionResponse createCondition(CreateConditionRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Condition condition = Condition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .expansion(expansion)
                .isOfficial(request.getIsOfficial())
                .srd(contentAccessService.resolveSrd(user, request.getSrd()))
                .build();

        Condition savedCondition = conditionRepository.save(condition);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedCondition, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("condition").build(),
                "\"" + savedCondition.getName() + "\" (condition_id: " + savedCondition.getId() + ")");

        return toResponse(savedCondition, Set.of());
    }

    /**
     * Creates multiple conditions in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created condition responses
     */
    @Transactional
    public List<ConditionResponse> createConditionsBulk(List<CreateConditionRequest> requests, Authentication authentication) {
        List<ConditionResponse> responses = requests.stream()
                .map(request -> createCondition(request, authentication))
                .toList();

        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("condition").build(),
                responses.size() + " created, 0 failed");

        return responses;
    }

    /**
     * Updates an existing condition.
     *
     * @param id The condition ID to update
     * @param request The update request containing new condition details
     * @param authentication The authentication of the current user
     * @return ConditionResponse containing the updated condition
     * @throws EntityNotFoundException if the condition or referenced expansion is not found
     */
    @Transactional
    public ConditionResponse updateCondition(Long id, UpdateConditionRequest request, Authentication authentication) {
        Condition condition = conditionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Condition not found with id: " + id));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            condition.setName(request.getName());
        }
        if (request.getDescription() != null) {
            condition.setDescription(request.getDescription());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            condition.setExpansion(expansion);
        }
        if (request.getIsOfficial() != null) {
            condition.setIsOfficial(request.getIsOfficial());
        }
        if (request.getSrd() != null) {
            condition.setSrd(contentAccessService.resolveSrd(user, request.getSrd()));
        }

        Condition updatedCondition = conditionRepository.save(condition);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedCondition, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("condition").build(),
                "condition_id: " + updatedCondition.getId());

        return toResponse(updatedCondition, Set.of());
    }

    /**
     * Soft deletes a condition by setting its deletedAt timestamp.
     *
     * @param id The condition ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the condition is not found or is already deleted
     */
    @Transactional
    public void deleteCondition(Long id, Authentication authentication) {
        Condition condition = conditionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Condition not found with id: " + id));

        condition.softDelete();
        conditionRepository.save(condition);
        eventPublisher.publishEvent(new EntityChangeEvent(this, condition, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("condition").build(),
                "condition_id: " + id);
    }

    /**
     * Restores a soft-deleted condition.
     *
     * @param id The condition ID to restore
     * @param authentication The authentication of the current user
     * @return ConditionResponse containing the restored condition
     * @throws EntityNotFoundException if the condition is not found
     * @throws IllegalStateException if the condition is not deleted
     */
    @Transactional
    public ConditionResponse restoreCondition(Long id, Authentication authentication) {
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Condition not found with id: " + id));

        if (!condition.isDeleted()) {
            throw new IllegalStateException("Condition with id " + id + " is not deleted");
        }

        condition.restore();
        Condition restoredCondition = conditionRepository.save(condition);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredCondition, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("condition").build(),
                "condition_id: " + id);

        return toResponse(restoredCondition, Set.of());
    }

    /**
     * Converts a Condition entity to ConditionResponse DTO.
     *
     * @param condition The condition entity
     * @param expand Set of relationships to expand
     * @return ConditionResponse DTO
     */
    public ConditionResponse toResponse(Condition condition, Set<String> expand) {
        if (!contentAccessService.mayView(condition.getIsOfficial(), condition.getSrd())) {
            return ContentRedaction.stub(ConditionResponse::new, condition.getId(),
                    condition.getExpansion() != null ? condition.getExpansion().getName() : null);
        }

        ConditionResponse.ConditionResponseBuilder builder = ConditionResponse.builder()
                .id(condition.getId())
                .name(condition.getName())
                .description(condition.getDescription())
                .expansionId(condition.getExpansion().getId())
                .expansionName(condition.getExpansion().getName())
                .isOfficial(condition.getIsOfficial())
                .srd(condition.getSrd())
                .createdAt(condition.getCreatedAt())
                .lastModifiedAt(condition.getLastModifiedAt())
                .deletedAt(condition.getDeletedAt());

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = condition.getExpansion();
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
