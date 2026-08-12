package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateMartialStanceRequest;
import com.aboff.core.model.dto.dh.request.UpdateMartialStanceRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.MartialStanceResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.MartialStance;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.repository.dh.MartialStanceRepository;
import com.aboff.core.security.CustomUserDetails;
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
import com.aboff.core.util.ContentRedaction;
import com.aboff.core.util.ExpandUtil;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing MartialStance entities.
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship expansion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MartialStanceService {

    private final MartialStanceRepository martialStanceRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ContentAccessService contentAccessService;

    /**
     * Retrieves a paginated list of martial stances.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted martial stances
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param tier Optional filter for martial stance tier (1–4)
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing martial stances
     */
    @Transactional(readOnly = true)
    public PagedResponse<MartialStanceResponse> getAllMartialStances(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Integer tier,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        // Soft-deleted rows are a moderation surface, not a browse surface; this call was
        // previously ungated with no role check at all despite this parameter's name implying one.
        boolean effectiveIncludeDeleted = contentAccessService.resolveIncludeDeleted(includeDeleted);
        Page<MartialStance> martialStancePage;

        if (effectiveIncludeDeleted) {
            martialStancePage = martialStanceRepository.findAllWithFilters(expansionId, isOfficial, tier, pageable);
        } else {
            martialStancePage = martialStanceRepository.findByDeletedAtIsNullAndFilters(
                    expansionId, isOfficial, tier, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<MartialStanceResponse>builder()
                .content(martialStancePage.getContent().stream()
                        .map(martialStance -> toResponse(martialStance, expandSet))
                        .toList())
                .totalElements(martialStancePage.getTotalElements())
                .totalPages(martialStancePage.getTotalPages())
                .currentPage(martialStancePage.getNumber())
                .pageSize(martialStancePage.getSize())
                .build();
    }

    /**
     * Retrieves a single martial stance by ID.
     *
     * @param id The martial stance ID
     * @param expand Comma-separated list of relationships to expand
     * @return MartialStanceResponse containing the martial stance details
     * @throws EntityNotFoundException if the martial stance is not found or is deleted
     */
    @Transactional(readOnly = true)
    public MartialStanceResponse getMartialStanceById(Long id, String expand) {
        MartialStance martialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Martial stance not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(martialStance, expandSet);
    }

    /**
     * Creates a new martial stance.
     *
     * @param request The creation request containing martial stance details
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the created martial stance
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public MartialStanceResponse createMartialStance(CreateMartialStanceRequest request, Authentication authentication) {
        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));
        User user = currentUser(authentication);

        MartialStance martialStance = MartialStance.builder()
                .name(request.getName())
                .expansion(expansion)
                .tier(request.getTier())
                .isOfficial(request.getIsOfficial())
                .srd(contentAccessService.resolveSrd(user, request.getSrd()))
                .description(request.getDescription())
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            martialStance.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalMartialStanceId() != null) {
            MartialStance originalMartialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(request.getOriginalMartialStanceId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original martial stance not found with id: " + request.getOriginalMartialStanceId()));
            martialStance.setOriginalMartialStance(originalMartialStance);
        }

        MartialStance savedMartialStance = martialStanceRepository.save(martialStance);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedMartialStance, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("martial_stance").build(),
                "\"" + savedMartialStance.getName() + "\" (martial_stance_id: " + savedMartialStance.getId() + ")");

        return toResponse(savedMartialStance, Set.of());
    }

    /**
     * Creates multiple martial stances in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created martial stance responses
     */
    @Transactional
    public List<MartialStanceResponse> createMartialStanceBulk(List<CreateMartialStanceRequest> requests, Authentication authentication) {
        User user = currentUser(authentication);
        List<MartialStance> martialStances = requests.stream()
                .map(request -> {
                    Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Expansion not found with id: " + request.getExpansionId()));

                    MartialStance martialStance = MartialStance.builder()
                            .name(request.getName())
                            .expansion(expansion)
                            .tier(request.getTier())
                            .isOfficial(request.getIsOfficial())
                            .srd(contentAccessService.resolveSrd(user, request.getSrd()))
                            .description(request.getDescription())
                            .build();

                    Set<Feature> bulkResolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
                    if (bulkResolvedFeatures != null) {
                        martialStance.setFeatures(bulkResolvedFeatures);
                    }

                    if (request.getOriginalMartialStanceId() != null) {
                        MartialStance originalMartialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(request.getOriginalMartialStanceId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Original martial stance not found with id: " + request.getOriginalMartialStanceId()));
                        martialStance.setOriginalMartialStance(originalMartialStance);
                    }

                    return martialStance;
                })
                .toList();

        List<MartialStance> savedMartialStances = martialStanceRepository.saveAll(martialStances);
        savedMartialStances.forEach(m -> eventPublisher.publishEvent(new EntityChangeEvent(this, m, EntityChangeEvent.ChangeType.CREATED)));
        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("martial_stance").build(),
                savedMartialStances.size() + " created, 0 failed");

        return savedMartialStances.stream()
                .map(martialStance -> toResponse(martialStance, Set.of()))
                .toList();
    }

    /**
     * Updates an existing martial stance.
     *
     * @param id The martial stance ID to update
     * @param request The update request containing new martial stance details
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the updated martial stance
     * @throws EntityNotFoundException if the martial stance or referenced entities are not found
     */
    @Transactional
    public MartialStanceResponse updateMartialStance(Long id, UpdateMartialStanceRequest request, Authentication authentication) {
        MartialStance martialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Martial stance not found with id: " + id));
        User user = currentUser(authentication);

        if (request.getName() != null && !request.getName().isBlank()) {
            martialStance.setName(request.getName());
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            martialStance.setExpansion(expansion);
        }
        if (request.getTier() != null) {
            martialStance.setTier(request.getTier());
        }
        if (request.getIsOfficial() != null) {
            martialStance.setIsOfficial(request.getIsOfficial());
        }
        if (request.getSrd() != null) {
            martialStance.setSrd(contentAccessService.resolveSrd(user, request.getSrd()));
        }
        if (request.getDescription() != null) {
            martialStance.setDescription(request.getDescription());
        }

        if (request.getFeatureIds() != null || request.getFeatures() != null) {
            Set<Feature> resolvedUpdateFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
            if (resolvedUpdateFeatures != null) {
                martialStance.setFeatures(resolvedUpdateFeatures);
            }
        }

        if (request.getOriginalMartialStanceId() != null) {
            MartialStance originalMartialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(request.getOriginalMartialStanceId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original martial stance not found with id: " + request.getOriginalMartialStanceId()));
            martialStance.setOriginalMartialStance(originalMartialStance);
        }

        MartialStance updatedMartialStance = martialStanceRepository.save(martialStance);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedMartialStance, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("martial_stance").build(),
                "martial_stance_id: " + updatedMartialStance.getId());

        return toResponse(updatedMartialStance, Set.of());
    }

    /**
     * Soft deletes a martial stance by setting its deletedAt timestamp.
     *
     * @param id The martial stance ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the martial stance is not found or is already deleted
     */
    @Transactional
    public void deleteMartialStance(Long id, Authentication authentication) {
        MartialStance martialStance = martialStanceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Martial stance not found with id: " + id));

        martialStance.softDelete();
        martialStanceRepository.save(martialStance);
        eventPublisher.publishEvent(new EntityChangeEvent(this, martialStance, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("martial_stance").build(),
                "martial_stance_id: " + id);
    }

    /**
     * Restores a soft-deleted martial stance.
     *
     * @param id The martial stance ID to restore
     * @param authentication The authentication of the current user
     * @return MartialStanceResponse containing the restored martial stance
     * @throws EntityNotFoundException if the martial stance is not found
     * @throws IllegalStateException if the martial stance is not deleted
     */
    @Transactional
    public MartialStanceResponse restoreMartialStance(Long id, Authentication authentication) {
        MartialStance martialStance = martialStanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Martial stance not found with id: " + id));

        if (!martialStance.isDeleted()) {
            throw new IllegalStateException("Martial stance with id " + id + " is not deleted");
        }

        martialStance.restore();
        MartialStance restoredMartialStance = martialStanceRepository.save(martialStance);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredMartialStance, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("martial_stance").build(),
                "martial_stance_id: " + id);

        return toResponse(restoredMartialStance, Set.of());
    }

    /**
     * Converts a MartialStance entity to MartialStanceResponse DTO.
     *
     * @param martialStance The martial stance entity
     * @param expand Set of relationships to expand
     * @return MartialStanceResponse DTO
     */
    public MartialStanceResponse toResponse(MartialStance martialStance, Set<String> expand) {
        if (!contentAccessService.mayView(martialStance)) {
            return ContentRedaction.stub(MartialStanceResponse::new, martialStance.getId(),
                    martialStance.getExpansion() != null ? martialStance.getExpansion().getName() : null);
        }

        MartialStanceResponse.MartialStanceResponseBuilder builder = MartialStanceResponse.builder()
                .id(martialStance.getId())
                .name(martialStance.getName())
                .expansionId(martialStance.getExpansion().getId())
                .expansionName(martialStance.getExpansion() != null ? martialStance.getExpansion().getName() : null)
                .tier(martialStance.getTier())
                .isOfficial(martialStance.getIsOfficial())
                .srd(martialStance.getSrd())
                .description(martialStance.getDescription())
                .createdAt(martialStance.getCreatedAt())
                .lastModifiedAt(martialStance.getLastModifiedAt())
                .deletedAt(martialStance.getDeletedAt());

        if (martialStance.getFeatures() != null && !martialStance.getFeatures().isEmpty()) {
            builder.featureIds(martialStance.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (martialStance.getOriginalMartialStance() != null) {
            builder.originalMartialStanceId(martialStance.getOriginalMartialStance().getId());
        }

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = martialStance.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "features") && martialStance.getFeatures() != null && !martialStance.getFeatures().isEmpty()) {
            builder.features(martialStance.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalMartialStance") && martialStance.getOriginalMartialStance() != null) {
            builder.originalMartialStance(toResponse(martialStance.getOriginalMartialStance(), Set.of()));
        }

        return builder.build();
    }

    /**
     * Extracts the authenticated user from the security context principal.
     * <p>
     * Inlined here rather than delegating to {@link ItemAccessService#currentUser}: this service
     * does not otherwise depend on {@code ItemAccessService} (martial stances have no
     * user-authoring path — only ADMIN/OWNER can create or update one), and pulling in that
     * dependency for this one line would be a heavier coupling than the cast it replaces.
     * </p>
     *
     * @param authentication the current authentication
     * @return the authenticated user
     */
    private User currentUser(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }
}
