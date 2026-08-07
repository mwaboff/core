package com.aboff.core.service;

import com.aboff.core.config.SearchFieldMapping;
import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.SearchIndex;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.service.search.SearchTypeRegistry;
import com.aboff.core.util.PostgresArrayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for managing the full-text search index for game content entities.
 *
 * <p>Provides methods to add, update, soft-delete, and remove entries from the
 * {@code search_index} table. Indexing is driven by the {@link SearchIndexed} annotation
 * on entity classes and the field mappings defined in {@link SearchFieldMapping}.
 *
 * <p>This service is typically invoked by {@link com.aboff.core.event.SearchIndexEventListener}
 * after entity lifecycle events, ensuring the index stays in sync with the primary database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private final SearchIndexRepository searchIndexRepository;
    private final SearchFieldMapping searchFieldMapping;
    private final SearchTypeRegistry searchTypeRegistry;

    /**
     * Indexes (or re-indexes) a single entity by upserting its search index row.
     *
     * <p>The entity must be annotated with {@link SearchIndexed}. If the annotation is absent,
     * the method logs a warning and returns without indexing.
     *
     * <p>On conflict (same {@code entity_type} + {@code entity_id}), all mutable columns are
     * updated and {@code deleted_at} is reset to {@code NULL}, effectively restoring a
     * previously soft-deleted index entry.
     *
     * @param entity the JPA entity instance to index; must be annotated with {@link SearchIndexed}
     */
    @Transactional
    public void indexEntity(Object entity) {
        SearchIndexed annotation = entity.getClass().getAnnotation(SearchIndexed.class);
        if (annotation == null) {
            log.warn("Attempted to index entity {} which is not annotated with @SearchIndexed",
                    entity.getClass().getSimpleName());
            return;
        }

        log.debug("Indexing entity type={}", annotation.type());

        SearchFieldMapping.SearchIndexData data = searchFieldMapping.buildSearchIndexData(entity, annotation.type());

        if (data == null) {
            log.warn("Search index data could not be built for entity type={}; indexing skipped",
                    annotation.type());
            return;
        }

        searchIndexRepository.upsertSearchIndex(
                data.getEntityType(),
                data.getEntityId(),
                data.getName(),
                data.getNameText(),
                data.getDescriptionText(),
                data.getFeatureText(),
                data.getTier(),
                data.getExpansionId(),
                data.getIsOfficial(),
                data.getIsPublic(),
                data.getCreatedByUserId(),
                data.getCardType(),
                data.getFeatureType(),
                data.getAdversaryType(),
                data.getDomainCardType(),
                data.getAssociatedDomainId(),
                data.getTrait(),
                data.getRange(),
                data.getBurden(),
                data.getIsPrimary(),
                data.getDamageType(),
                data.getIsConsumable(),
                data.getIsMixed(),
                data.getSubclassLevel(),
                data.getCostTagCategory(),
                PostgresArrayUtil.toBigintArrayLiteral(data.getSharedCampaignIds())
        );

        log.debug("Upserted search index for entity type={} id={}", data.getEntityType(), data.getEntityId());
    }

    /**
     * Hard-deletes the search index entry for the given entity type and ID.
     *
     * <p>Use this method when the referenced entity is permanently removed from the system.
     * For soft-deletion, use {@link #softDeleteEntity(SearchableEntityType, Long)} instead.
     *
     * @param type     the {@link SearchableEntityType} of the entity being removed
     * @param entityId the primary key of the entity being removed
     */
    @Transactional
    public void removeEntity(SearchableEntityType type, Long entityId) {
        log.debug("Hard-deleting search index entry for type={} id={}", type, entityId);
        searchIndexRepository.deleteByEntityTypeAndEntityId(type.name(), entityId);
    }

    /**
     * Soft-deletes the search index entry for the given entity type and ID by setting its
     * {@code deletedAt} timestamp to the current time.
     *
     * <p>Soft-deleted entries are excluded from search results but remain in the index,
     * allowing them to be restored if the referenced entity is later restored.
     *
     * <p>If no index entry is found for the given type and ID, a warning is logged and no
     * action is taken.
     *
     * @param type     the {@link SearchableEntityType} of the entity being soft-deleted
     * @param entityId the primary key of the entity being soft-deleted
     */
    @Transactional
    public void softDeleteEntity(SearchableEntityType type, Long entityId) {
        log.debug("Soft-deleting search index entry for type={} id={}", type, entityId);

        Optional<SearchIndex> entry = searchIndexRepository.findByEntityTypeAndEntityId(type.name(), entityId);

        if (entry.isEmpty()) {
            log.warn("No search index entry found for type={} id={} to soft-delete", type, entityId);
            return;
        }

        SearchIndex index = entry.get();
        index.softDelete();
        searchIndexRepository.save(index);

        log.debug("Soft-deleted search index entry for type={} id={}", type, entityId);
    }

    /**
     * Re-indexes a previously soft-deleted entity by calling {@link #indexEntity(Object)}.
     *
     * <p>The upsert operation used by {@code indexEntity} automatically clears the
     * {@code deleted_at} field on conflict, effectively restoring the index entry.
     *
     * @param entity the entity instance to restore in the index
     */
    @Transactional
    public void restoreEntity(Object entity) {
        log.debug("Restoring search index entry for entity {}", entity.getClass().getSimpleName());
        indexEntity(entity);
    }

    /**
     * Fully rebuilds the search index for the given entity type by clearing all existing
     * entries and re-indexing every non-soft-deleted row from the source repository.
     *
     * <p>Soft-deleted entities (as determined by {@link BaseEntity#isDeleted()}) are skipped.
     *
     * @param type the {@link SearchableEntityType} whose index should be rebuilt
     * @return the number of entities re-indexed
     */
    @Transactional
    public int reindexAll(SearchableEntityType type) {
        log.info("Rebuilding search index for type={}", type);
        searchIndexRepository.deleteAllByEntityType(type.name());

        JpaRepository<? extends BaseEntity, Long> repository = resolveRepository(type);
        if (repository == null) {
            log.warn("No repository available for type={}; index cleared but not repopulated", type);
            return 0;
        }

        List<? extends BaseEntity> entities = repository.findAll();
        int indexed = 0;
        for (BaseEntity entity : entities) {
            if (isSoftDeleted(entity)) {
                continue;
            }
            indexEntity(entity);
            indexed++;
        }

        log.info("Reindexed {} entities of type={}", indexed, type);
        return indexed;
    }

    /**
     * Checks whether an entity is soft-deleted by invoking its {@code isDeleted()} method
     * reflectively.
     *
     * <p>Soft deletion in this codebase is implemented per-entity (not on {@link BaseEntity}),
     * so reflection is used to avoid coupling this service to every concrete type. Entities
     * without an {@code isDeleted()} method are treated as active.
     *
     * @param entity the entity to check; must not be null
     * @return {@code true} if the entity exposes {@code isDeleted()} and it returns {@code true}
     */
    private boolean isSoftDeleted(BaseEntity entity) {
        try {
            var method = entity.getClass().getMethod("isDeleted");
            Object result = method.invoke(entity);
            return result instanceof Boolean b && b;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (ReflectiveOperationException e) {
            log.warn("Failed to check soft-delete state for {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Fully rebuilds the search index for every supported {@link SearchableEntityType}.
     *
     * <p>Iterates each type and delegates to {@link #reindexAll(SearchableEntityType)}.
     * This is an expensive operation suitable for admin-triggered background tasks or
     * one-off recovery scenarios.
     *
     * @return the total number of entities re-indexed across all types
     */
    @Transactional
    public int reindexAll() {
        log.info("Rebuilding search index for all entity types");
        int total = 0;
        for (SearchableEntityType type : SearchableEntityType.values()) {
            total += reindexAll(type);
        }
        log.info("Completed full search index rebuild: {} entities indexed", total);
        return total;
    }

    /**
     * Resolves the source repository for a given {@link SearchableEntityType}.
     *
     * <p>Delegates to {@link SearchTypeRegistry}, which is validated at application startup to
     * hold exactly one registration per {@link SearchableEntityType} constant — see
     * {@link com.aboff.core.service.search.SearchTypeRegistration} for why this used to be a
     * hand-maintained switch and why that was dangerous.
     *
     * @param type the searchable entity type
     * @return the corresponding {@link JpaRepository}; never {@code null}
     */
    private JpaRepository<? extends BaseEntity, Long> resolveRepository(SearchableEntityType type) {
        return searchTypeRegistry.repositoryFor(type);
    }
}
