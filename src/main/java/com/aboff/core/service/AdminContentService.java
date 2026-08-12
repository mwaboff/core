package com.aboff.core.service;

import com.aboff.core.model.dto.request.BulkSrdUpdateRequest;
import com.aboff.core.model.dto.response.BulkSrdUpdateResponse;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.BaseItem;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Card;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.enums.AdminActionType;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.service.dh.SubclassPathService;
import com.aboff.core.service.search.SearchTypeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Backs the bulk SRD-flagging tool: {@code PATCH /api/admin/content/srd}.
 * <p>
 * Content only in a paid Daggerheart book is gated behind ADMIN/OWNER or a per-user grant (see
 * {@link ContentAccessService}), and every existing row was backfilled to {@code srd = false}
 * when that column was added. This service is the only way to turn {@code srd} on afterward,
 * so it needs to reach every gated type without a hand-written switch per controller.
 * </p>
 * <p>
 * <strong>Type dispatch:</strong> there is no pre-existing admin CRUD registry to reuse — each
 * content type has its own hand-written controller/service pair with no shared abstraction.
 * {@link SearchTypeRegistry} does exist and already maps every {@link SearchableEntityType} to
 * its backing {@link JpaRepository}, validated complete at startup, so this service reuses it
 * for the fetch-by-id half of the job instead of injecting twenty repositories. There is no
 * equivalent registry for <em>setting</em> {@code srd}, because the gated entities share no
 * common interface carrying that field (see {@link ContentAccessService}'s javadoc on
 * {@code mayView(Boolean, Boolean)} for why) — introducing one would mean touching entity
 * classes owned by several other in-flight workstreams for a single setter. {@link #applySrd}
 * is the one place that gap is bridged, with a pattern-matching switch instead.
 * </p>
 * <p>
 * <strong>{@link SearchableEntityType#SUBCLASS_PATH} is the one exception</strong> to that
 * generic dispatch: a subclass path's {@code srd} must cascade to its three Foundation/
 * Specialization/Mastery cards (see {@code SubclassCardService}'s javadoc on that invariant), and
 * only {@link SubclassPathService#bulkSetSrd} does that cascade. Saving the path straight through
 * {@link SearchTypeRegistry#repositoryFor} the way every other type is handled would flip the
 * path's flag while leaving its cards silently out of sync. {@link #updateSrd} special-cases this
 * one type to route through {@link SubclassPathService} instead of {@link #flagAndSave}.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminContentService {

    /**
     * Types the admin card search UI never exposes for direct SRD flagging.
     * <p>
     * {@link SearchableEntityType#EXPANSION} carries no {@code srd} column at all — a
     * sourcebook is not content. {@link SearchableEntityType#SUBCLASS_CARD} does carry one,
     * but a subclass path and its three cards must never disagree: the cards' {@code srd} is
     * always re-derived from {@code subclassPath.getSrd()} (see {@code SubclassCardService}),
     * so flagging the cards directly here would either be silently overwritten on their next
     * save or drift out of sync with the path. Both are rejected with a clear 400 instead of
     * silently doing the wrong thing; {@link SearchableEntityType#SUBCLASS_PATH} is the correct
     * type to flag, and its cards follow automatically.
     * </p>
     */
    private static final Set<SearchableEntityType> UNSUPPORTED_TYPES =
            Set.of(SearchableEntityType.EXPANSION, SearchableEntityType.SUBCLASS_CARD);

    private final SearchTypeRegistry searchTypeRegistry;
    private final AdminUserService adminUserService;
    private final SubclassPathService subclassPathService;

    /**
     * Applies {@code request.getSrd()} to every id in {@code request.getIds()} that resolves to
     * a row of {@code request.getType()}, and writes one {@link AdminActionType#CONTENT_SRD_CHANGED}
     * audit row for the whole batch.
     *
     * @param actor     the admin performing the change
     * @param request   the validated request body
     * @param ipAddress originating ip, captured on the audit row
     * @return the updated and unknown ids
     * @throws IllegalStateException if {@code type} is not a recognized, srd-flaggable content type
     */
    @Transactional
    public BulkSrdUpdateResponse updateSrd(User actor, BulkSrdUpdateRequest request, String ipAddress) {
        SearchableEntityType type = resolveType(request.getType());

        // SUBCLASS_PATH must cascade srd to its cards, which only SubclassPathService#bulkSetSrd
        // does -- see the class javadoc. Every other type goes through the generic repository
        // dispatch.
        List<Long> updatedIds = type == SearchableEntityType.SUBCLASS_PATH
                ? subclassPathService.bulkSetSrd(request.getIds(), request.getSrd())
                : flagAndSave(searchTypeRegistry.repositoryFor(type), request.getIds(), request.getSrd());
        Set<Long> updatedIdSet = Set.copyOf(updatedIds);
        List<Long> unknownIds = request.getIds().stream()
                .filter(id -> !updatedIdSet.contains(id))
                .distinct()
                .toList();

        adminUserService.recordContentAction(actor, AdminActionType.CONTENT_SRD_CHANGED,
                String.format("type=%s; srd=%s; requested=%d; updated=%d; unknown=%s",
                        type, request.getSrd(), request.getIds().size(), updatedIds.size(), unknownIds),
                ipAddress);

        log.info("Admin id={} set srd={} on {} {} row(s), {} unknown id(s)",
                actor != null ? actor.getId() : null, request.getSrd(), updatedIds.size(), type, unknownIds.size());

        return BulkSrdUpdateResponse.builder()
                .type(type.name())
                .srd(request.getSrd())
                .updatedIds(updatedIds)
                .unknownIds(unknownIds)
                .build();
    }

    /**
     * Parses and validates the request's raw type string.
     *
     * @param rawType the requested type key
     * @return the resolved, srd-flaggable type
     * @throws IllegalStateException if {@code rawType} is blank, not a recognized
     *                                {@link SearchableEntityType}, or is in {@link #UNSUPPORTED_TYPES}
     */
    private SearchableEntityType resolveType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalStateException("Type is required");
        }

        SearchableEntityType type;
        try {
            type = SearchableEntityType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown content type: " + rawType + ". Valid types: "
                    + Arrays.toString(SearchableEntityType.values()));
        }

        if (UNSUPPORTED_TYPES.contains(type)) {
            String reason = type == SearchableEntityType.SUBCLASS_CARD
                    ? "subclass card srd is derived from its subclass path; flag type=SUBCLASS_PATH instead"
                    : "expansions are not gated content and carry no srd flag";
            throw new IllegalStateException("Cannot flag srd on type=" + type + ": " + reason);
        }

        return type;
    }

    /**
     * Fetches, flags, and saves every id in {@code ids} that exists for the given repository.
     * <p>
     * A single generic method so the compiler captures {@code T} once and keeps
     * {@code findAllById} and {@code saveAll} talking about the same type — splitting this
     * across two statements against the caller's {@code JpaRepository<? extends BaseEntity, Long>}
     * produces two independent wildcard captures that don't unify.
     * </p>
     *
     * @param repository the repository backing {@code type}
     * @param ids        the requested ids; entries that don't resolve are silently skipped
     * @param srd        the srd value to apply
     * @param <T>        the entity type
     * @return the ids that were found and updated
     */
    private <T extends BaseEntity> List<Long> flagAndSave(JpaRepository<T, Long> repository, List<Long> ids, boolean srd) {
        List<T> found = repository.findAllById(ids);
        found.forEach(entity -> applySrd(entity, srd));
        repository.saveAll(found);
        return found.stream().map(BaseEntity::getId).toList();
    }

    /**
     * Sets {@code srd} on a single entity, dispatching by runtime type.
     * <p>
     * {@link Card} and {@link BaseItem} cover eight of the twenty flaggable types in two
     * branches; the remaining eleven standalone entities (which share no common supertype
     * carrying {@code srd}) each get one branch. {@link SearchableEntityType#SUBCLASS_PATH} is
     * the twentieth flaggable type and deliberately has no branch here — {@link #updateSrd}
     * never routes it to this method, since {@link SubclassPathService#bulkSetSrd} is the only
     * place that type's {@code srd} may be set (see the class javadoc). Every other
     * {@link SearchableEntityType} outside {@link #UNSUPPORTED_TYPES} is covered — a type
     * reaching the {@code default} arm is a bug in this dispatch table, not a bad request, so it
     * throws rather than silently no-op-ing.
     * </p>
     *
     * @param entity the entity to update, as resolved by {@link SearchTypeRegistry#repositoryFor}
     * @param srd    the srd value to apply
     */
    private void applySrd(BaseEntity entity, boolean srd) {
        switch (entity) {
            case Card card -> card.setSrd(srd);
            case BaseItem item -> item.setSrd(srd);
            case Adversary a -> a.setSrd(srd);
            case Beastform b -> b.setSrd(srd);
            case CardCostTag t -> t.setSrd(srd);
            case com.aboff.core.model.entity.dh.Class c -> c.setSrd(srd);
            case Condition c -> c.setSrd(srd);
            case Domain d -> d.setSrd(srd);
            case Encounter e -> e.setSrd(srd);
            case Environment e -> e.setSrd(srd);
            case Feature f -> f.setSrd(srd);
            case Question q -> q.setSrd(srd);
            case TransformationCard tc -> tc.setSrd(srd);
            default -> throw new IllegalStateException(
                    "No srd setter registered for entity type " + entity.getClass().getSimpleName());
        }
    }
}
