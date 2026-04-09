package com.aboff.core.service;

import com.aboff.core.config.SearchFieldMapping;
import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.SearchIndex;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                data.getCostTagCategory()
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
     * Clears all search index entries for the given entity type.
     *
     * <p>Full re-indexing after clearing requires loading all entities of the given type
     * and calling {@link #indexEntity(Object)} for each. This method only performs the
     * deletion step. Manual population is required to repopulate the index.
     *
     * @param type the {@link SearchableEntityType} whose index entries should be cleared
     */
    @Transactional
    public void reindexAll(SearchableEntityType type) {
        log.debug("Clearing all search index entries for type={}", type);
        searchIndexRepository.deleteAllByEntityType(type.name());
        log.warn("Cleared search index for type={}. Manual re-population of index entries is required " +
                "as full reindex requires loading all entities from their respective repositories.", type);
    }

    /**
     * Clears all search index entries for every known {@link SearchableEntityType}.
     *
     * <p>This is a destructive operation. Manual re-population is required after calling
     * this method. Each entity type's index is cleared by delegating to
     * {@link #reindexAll(SearchableEntityType)}.
     */
    @Transactional
    public void reindexAll() {
        log.warn("Clearing all search index entries for all entity types. Manual re-population is required.");
        for (SearchableEntityType type : SearchableEntityType.values()) {
            reindexAll(type);
        }
    }
}
