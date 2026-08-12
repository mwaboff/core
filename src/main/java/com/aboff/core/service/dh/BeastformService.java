package com.aboff.core.service.dh;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.dto.dh.request.CreateBeastformRequest;
import com.aboff.core.model.dto.dh.request.UpdateBeastformRequest;
import com.aboff.core.model.dto.dh.response.ExpansionResponse;
import com.aboff.core.model.dto.dh.response.BeastformResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.embeddable.DamageRoll;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.enums.AuditAction;
import com.aboff.core.repository.dh.BeastformRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Beastform entities.
 * <p>
 * Handles business logic for CRUD operations, pagination, soft deletion, and relationship
 * expansion for beastform stat blocks — creatures characters can transform into.
 * </p>
 * <p>
 * Beastforms follow the same official/custom content pattern as {@code Weapon}/{@code Armor}/
 * {@code Loot}: mutation endpoints are restricted to ADMIN/OWNER at the controller level, since
 * beastform stat blocks are bulk-imported rulebook content rather than user-authored content
 * (unlike {@code Adversary}/{@code Encounter}, which support per-user creation and therefore need
 * ownership-based permission checks). The {@code isPublic}/{@code originalBeastform} columns exist
 * on the schema for a future user-facing customization feature (CORE-01b, currently deferred) and
 * are exposed on the DTOs so that feature can be layered on later without a schema change.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeastformService {

    private final BeastformRepository beastformRepository;
    private final ExpansionRepository expansionRepository;
    private final FeatureService featureService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;
    private final ContentAccessService contentAccessService;

    /**
     * Retrieves a paginated list of beastforms.
     *
     * @param page Zero-based page number
     * @param size Number of items per page
     * @param includeDeleted Whether to include soft-deleted beastforms
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param isPublic Optional filter for public visibility
     * @param expand Comma-separated list of relationships to expand
     * @return Paginated response containing beastforms
     */
    @Transactional(readOnly = true)
    public PagedResponse<BeastformResponse> getAllBeastforms(
            int page,
            int size,
            boolean includeDeleted,
            Long expansionId,
            Boolean isOfficial,
            Boolean isPublic,
            String expand) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Beastform> beastformPage;

        if (contentAccessService.resolveIncludeDeleted(includeDeleted)) {
            beastformPage = beastformRepository.findAllWithFilters(expansionId, isOfficial, isPublic, pageable);
        } else {
            beastformPage = beastformRepository.findByDeletedAtIsNullAndFilters(
                    expansionId, isOfficial, isPublic, contentAccessService.includeNonSrd(), pageable);
        }

        Set<String> expandSet = ExpandUtil.parseExpand(expand);

        return PagedResponse.<BeastformResponse>builder()
                .content(beastformPage.getContent().stream()
                        .map(beastform -> toResponse(beastform, expandSet))
                        .toList())
                .totalElements(beastformPage.getTotalElements())
                .totalPages(beastformPage.getTotalPages())
                .currentPage(beastformPage.getNumber())
                .pageSize(beastformPage.getSize())
                .build();
    }

    /**
     * Retrieves a single beastform by ID.
     *
     * @param id The beastform ID
     * @param expand Comma-separated list of relationships to expand
     * @return BeastformResponse containing the beastform details
     * @throws EntityNotFoundException if the beastform is not found or is deleted
     */
    @Transactional(readOnly = true)
    public BeastformResponse getBeastformById(Long id, String expand) {
        Beastform beastform = beastformRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Beastform not found with id: " + id));

        Set<String> expandSet = ExpandUtil.parseExpand(expand);
        return toResponse(beastform, expandSet);
    }

    /**
     * Creates a new beastform.
     *
     * @param request The creation request containing beastform details
     * @param authentication The authentication of the current user
     * @return BeastformResponse containing the created beastform
     * @throws EntityNotFoundException if referenced entities are not found
     */
    @Transactional
    public BeastformResponse createBeastform(CreateBeastformRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User creator = userDetails.getUser();

        Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Expansion not found with id: " + request.getExpansionId()));

        Beastform beastform = Beastform.builder()
                .name(request.getName())
                .example(request.getExample())
                .advantages(request.getAdvantages())
                // evasion and the six trait modifiers are passed through as-is (no
                // null-coalescing to 0): an omitted field must persist as NULL, not a
                // manufactured zero -- see Beastform.agilityModifier for the full rationale.
                .evasion(request.getEvasion())
                .tier(request.getTier())
                .agilityModifier(request.getAgilityModifier())
                .strengthModifier(request.getStrengthModifier())
                .finesseModifier(request.getFinesseModifier())
                .instinctModifier(request.getInstinctModifier())
                .presenceModifier(request.getPresenceModifier())
                .knowledgeModifier(request.getKnowledgeModifier())
                .attackRange(request.getAttackRange())
                .attackTrait(request.getAttackTrait())
                .damage(request.getDamage() != null ? toDamageRoll(request.getDamage()) : null)
                .expansion(expansion)
                .createdBy(creator)
                .isOfficial(request.getIsOfficial())
                .srd(contentAccessService.resolveSrd(creator, request.getSrd()))
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(request.getFeatureIds(), request.getFeatures());
        if (resolvedFeatures != null) {
            beastform.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalBeastformId() != null) {
            Beastform originalBeastform = beastformRepository.findByIdAndDeletedAtIsNull(request.getOriginalBeastformId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original beastform not found with id: " + request.getOriginalBeastformId()));
            beastform.setOriginalBeastform(originalBeastform);
        }

        Beastform savedBeastform = beastformRepository.save(beastform);
        eventPublisher.publishEvent(new EntityChangeEvent(this, savedBeastform, EntityChangeEvent.ChangeType.CREATED));
        auditLogger.log(AuditAction.CONTENT_CREATED, AuditContext.forUser(authentication).withEntityType("beastform").build(),
                "\"" + savedBeastform.getName() + "\" (beastform_id: " + savedBeastform.getId() + ")");

        return toResponse(savedBeastform, Set.of());
    }

    /**
     * Creates multiple beastforms in bulk.
     *
     * @param requests List of creation requests
     * @param authentication The authentication of the current user
     * @return List of created beastform responses
     */
    @Transactional
    public List<BeastformResponse> createBeastformsBulk(List<CreateBeastformRequest> requests, Authentication authentication) {
        List<BeastformResponse> responses = requests.stream()
                .map(request -> createBeastform(request, authentication))
                .toList();

        auditLogger.log(AuditAction.CONTENT_BATCH_CREATED, AuditContext.forUser(authentication).withEntityType("beastform").build(),
                responses.size() + " created, 0 failed");

        return responses;
    }

    /**
     * Updates an existing beastform.
     *
     * @param id The beastform ID to update
     * @param request The update request containing new beastform details
     * @param authentication The authentication of the current user
     * @return BeastformResponse containing the updated beastform
     * @throws EntityNotFoundException if the beastform or referenced entities are not found
     */
    @Transactional
    public BeastformResponse updateBeastform(Long id, UpdateBeastformRequest request, Authentication authentication) {
        Beastform beastform = beastformRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Beastform not found with id: " + id));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            beastform.setName(request.getName());
        }
        if (request.getExample() != null) {
            beastform.setExample(request.getExample());
        }
        if (request.getAdvantages() != null) {
            beastform.setAdvantages(request.getAdvantages());
        }
        if (request.getEvasion() != null) {
            beastform.setEvasion(request.getEvasion());
        }
        if (request.getTier() != null) {
            beastform.setTier(request.getTier());
        }
        if (request.getAgilityModifier() != null) {
            beastform.setAgilityModifier(request.getAgilityModifier());
        }
        if (request.getStrengthModifier() != null) {
            beastform.setStrengthModifier(request.getStrengthModifier());
        }
        if (request.getFinesseModifier() != null) {
            beastform.setFinesseModifier(request.getFinesseModifier());
        }
        if (request.getInstinctModifier() != null) {
            beastform.setInstinctModifier(request.getInstinctModifier());
        }
        if (request.getPresenceModifier() != null) {
            beastform.setPresenceModifier(request.getPresenceModifier());
        }
        if (request.getKnowledgeModifier() != null) {
            beastform.setKnowledgeModifier(request.getKnowledgeModifier());
        }
        if (request.getAttackRange() != null) {
            beastform.setAttackRange(request.getAttackRange());
        }
        if (request.getAttackTrait() != null) {
            beastform.setAttackTrait(request.getAttackTrait());
        }
        if (request.getDamage() != null) {
            beastform.setDamage(toDamageRoll(request.getDamage()));
        }
        if (request.getExpansionId() != null) {
            Expansion expansion = expansionRepository.findByIdAndDeletedAtIsNull(request.getExpansionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Expansion not found with id: " + request.getExpansionId()));
            beastform.setExpansion(expansion);
        }
        if (request.getIsOfficial() != null) {
            beastform.setIsOfficial(request.getIsOfficial());
        }
        if (request.getSrd() != null) {
            beastform.setSrd(contentAccessService.resolveSrd(user, request.getSrd()));
        }
        if (request.getIsPublic() != null) {
            beastform.setIsPublic(request.getIsPublic());
        }

        Set<Feature> resolvedFeatures = featureService.resolveFeatures(
                request.getFeatureIds() != null ? new ArrayList<>(request.getFeatureIds()) : null,
                request.getFeatures());
        if (resolvedFeatures != null) {
            beastform.setFeatures(resolvedFeatures);
        }

        if (request.getOriginalBeastformId() != null) {
            Beastform originalBeastform = beastformRepository.findByIdAndDeletedAtIsNull(request.getOriginalBeastformId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Original beastform not found with id: " + request.getOriginalBeastformId()));
            beastform.setOriginalBeastform(originalBeastform);
        }

        Beastform updatedBeastform = beastformRepository.save(beastform);
        eventPublisher.publishEvent(new EntityChangeEvent(this, updatedBeastform, EntityChangeEvent.ChangeType.UPDATED));
        auditLogger.log(AuditAction.CONTENT_UPDATED, AuditContext.forUser(authentication).withEntityType("beastform").build(),
                "beastform_id: " + updatedBeastform.getId());

        return toResponse(updatedBeastform, Set.of());
    }

    /**
     * Soft deletes a beastform by setting its deletedAt timestamp.
     *
     * @param id The beastform ID to delete
     * @param authentication The authentication of the current user
     * @throws EntityNotFoundException if the beastform is not found or is already deleted
     */
    @Transactional
    public void deleteBeastform(Long id, Authentication authentication) {
        Beastform beastform = beastformRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Beastform not found with id: " + id));

        beastform.softDelete();
        beastformRepository.save(beastform);
        eventPublisher.publishEvent(new EntityChangeEvent(this, beastform, EntityChangeEvent.ChangeType.SOFT_DELETED));
        auditLogger.log(AuditAction.CONTENT_DELETED, AuditContext.forUser(authentication).withEntityType("beastform").build(),
                "beastform_id: " + id);
    }

    /**
     * Restores a soft-deleted beastform.
     *
     * @param id The beastform ID to restore
     * @param authentication The authentication of the current user
     * @return BeastformResponse containing the restored beastform
     * @throws EntityNotFoundException if the beastform is not found
     * @throws IllegalStateException if the beastform is not deleted
     */
    @Transactional
    public BeastformResponse restoreBeastform(Long id, Authentication authentication) {
        Beastform beastform = beastformRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Beastform not found with id: " + id));

        if (!beastform.isDeleted()) {
            throw new IllegalStateException("Beastform with id " + id + " is not deleted");
        }

        beastform.restore();
        Beastform restoredBeastform = beastformRepository.save(beastform);
        eventPublisher.publishEvent(new EntityChangeEvent(this, restoredBeastform, EntityChangeEvent.ChangeType.RESTORED));
        auditLogger.log(AuditAction.CONTENT_RESTORED, AuditContext.forUser(authentication).withEntityType("beastform").build(),
                "beastform_id: " + id);

        return toResponse(restoredBeastform, Set.of());
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable.
     *
     * @param request The damage roll request
     * @return DamageRoll embeddable
     */
    private DamageRoll toDamageRoll(CreateBeastformRequest.DamageRollRequest request) {
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts a DamageRollRequest to a DamageRoll embeddable (for update requests).
     *
     * @param request The damage roll request
     * @return DamageRoll embeddable
     */
    private DamageRoll toDamageRoll(UpdateBeastformRequest.DamageRollRequest request) {
        return DamageRoll.builder()
                .diceCount(request.getDiceCount())
                .diceType(request.getDiceType())
                .modifier(request.getModifier())
                .damageType(request.getDamageType())
                .build();
    }

    /**
     * Converts a Beastform entity to BeastformResponse DTO.
     *
     * @param beastform The beastform entity
     * @param expand Set of relationships to expand
     * @return BeastformResponse DTO
     */
    public BeastformResponse toResponse(Beastform beastform, Set<String> expand) {
        if (!contentAccessService.mayView(beastform.getIsOfficial(), beastform.getSrd())) {
            return ContentRedaction.stub(BeastformResponse::new, beastform.getId(),
                    beastform.getExpansion() != null ? beastform.getExpansion().getName() : null);
        }

        BeastformResponse.BeastformResponseBuilder builder = BeastformResponse.builder()
                .id(beastform.getId())
                .name(beastform.getName())
                .example(beastform.getExample())
                .advantages(beastform.getAdvantages())
                .evasion(beastform.getEvasion())
                .tier(beastform.getTier())
                .agilityModifier(beastform.getAgilityModifier())
                .strengthModifier(beastform.getStrengthModifier())
                .finesseModifier(beastform.getFinesseModifier())
                .instinctModifier(beastform.getInstinctModifier())
                .presenceModifier(beastform.getPresenceModifier())
                .knowledgeModifier(beastform.getKnowledgeModifier())
                .attackRange(beastform.getAttackRange())
                .attackTrait(beastform.getAttackTrait())
                .expansionId(beastform.getExpansion().getId())
                .expansionName(beastform.getExpansion().getName())
                .isOfficial(beastform.getIsOfficial())
                .srd(beastform.getSrd())
                .isPublic(beastform.getIsPublic())
                .createdAt(beastform.getCreatedAt())
                .lastModifiedAt(beastform.getLastModifiedAt())
                .deletedAt(beastform.getDeletedAt());

        if (beastform.getDamage() != null) {
            builder.damage(BeastformResponse.DamageRollResponse.builder()
                    .diceCount(beastform.getDamage().getDiceCount())
                    .diceType(beastform.getDamage().getDiceType())
                    .modifier(beastform.getDamage().getModifier())
                    .damageType(beastform.getDamage().getDamageType())
                    .notation(beastform.getDamage().toNotation())
                    .build());
        }

        if (beastform.getFeatures() != null && !beastform.getFeatures().isEmpty()) {
            builder.featureIds(beastform.getFeatures().stream()
                    .map(Feature::getId)
                    .collect(Collectors.toList()));
        }

        if (beastform.getOriginalBeastform() != null) {
            builder.originalBeastformId(beastform.getOriginalBeastform().getId());
        }

        if (ExpandUtil.shouldExpand(expand, "expansion")) {
            Expansion expansion = beastform.getExpansion();
            builder.expansion(ExpansionResponse.builder()
                    .id(expansion.getId())
                    .name(expansion.getName())
                    .isPublished(expansion.getIsPublished())
                    .createdAt(expansion.getCreatedAt())
                    .lastModifiedAt(expansion.getLastModifiedAt())
                    .deletedAt(expansion.getDeletedAt())
                    .build());
        }

        if (ExpandUtil.shouldExpand(expand, "features") && beastform.getFeatures() != null && !beastform.getFeatures().isEmpty()) {
            builder.features(beastform.getFeatures().stream()
                    .map(f -> featureService.toResponse(f, expand))
                    .collect(Collectors.toList()));
        }

        if (ExpandUtil.shouldExpand(expand, "originalBeastform") && beastform.getOriginalBeastform() != null) {
            builder.originalBeastform(toResponse(beastform.getOriginalBeastform(), Set.of()));
        }

        return builder.build();
    }
}
