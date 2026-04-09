package com.aboff.core.event;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring event listener that keeps the search index in sync with entity lifecycle changes.
 *
 * <p>Listens for {@link EntityChangeEvent} instances and delegates to {@link SearchIndexService}
 * to add, update, soft-delete, or remove the corresponding search index entry.
 *
 * <p>The listener is bound to {@link TransactionPhase#BEFORE_COMMIT} so that the entity
 * remains attached to the persistence context when the event fires, allowing lazy-loaded
 * collections and associations to be accessed without {@code LazyInitializationException}.
 * Indexing only takes place within the same transaction as the originating write, so a
 * rollback will undo both the entity change and the index update atomically.
 *
 * <p>Entities that are not annotated with {@link SearchIndexed} are silently ignored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchIndexEventListener {

    private final SearchIndexService searchIndexService;

    /**
     * Handles an {@link EntityChangeEvent} before the enclosing transaction commits.
     *
     * <p>The entity must be annotated with {@link SearchIndexed}; otherwise, the event is skipped.
     * Based on the {@link EntityChangeEvent.ChangeType}, the appropriate indexing operation is invoked:
     * <ul>
     *   <li>{@code CREATED}, {@code UPDATED}, {@code RESTORED} — upsert the search index entry</li>
     *   <li>{@code SOFT_DELETED} — mark the index entry as deleted</li>
     *   <li>{@code DELETED} — hard-delete the index entry</li>
     * </ul>
     *
     * <p>Running {@code BEFORE_COMMIT} ensures the entity is still attached to the persistence
     * context, so lazy-loaded associations and collections can be safely accessed during indexing.
     *
     * @param event the entity change event to handle
     */
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onEntityChange(EntityChangeEvent event) {
        Object entity = event.getEntity();
        SearchIndexed annotation = entity.getClass().getAnnotation(SearchIndexed.class);

        if (annotation == null) {
            log.debug("Entity {} is not @SearchIndexed, skipping index update", entity.getClass().getSimpleName());
            return;
        }

        log.debug("Handling {} event for entity type={}", event.getChangeType(), annotation.type());

        switch (event.getChangeType()) {
            case CREATED, UPDATED, RESTORED -> searchIndexService.indexEntity(entity);
            case SOFT_DELETED -> searchIndexService.softDeleteEntity(annotation.type(), getEntityId(entity));
            case DELETED -> searchIndexService.removeEntity(annotation.type(), getEntityId(entity));
        }
    }

    /**
     * Extracts the primary key ID from the entity by casting it to {@link BaseEntity}.
     *
     * @param entity the entity to extract the ID from
     * @return the entity's ID
     * @throws IllegalArgumentException if the entity does not extend {@link BaseEntity}
     */
    private Long getEntityId(Object entity) {
        if (entity instanceof BaseEntity base) {
            return base.getId();
        }
        throw new IllegalArgumentException(
                "Entity must extend BaseEntity to extract ID, but was: " + entity.getClass().getName());
    }
}
