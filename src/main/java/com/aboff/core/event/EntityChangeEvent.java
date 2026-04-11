package com.aboff.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published whenever a game content entity is created, updated, deleted,
 * soft-deleted, or restored.
 *
 * <p>Listeners (such as {@link SearchIndexEventListener}) subscribe to this event via
 * {@link org.springframework.transaction.event.TransactionalEventListener} to keep the
 * search index in sync after the originating transaction commits.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * eventPublisher.publishEvent(new EntityChangeEvent(this, savedWeapon, EntityChangeEvent.ChangeType.CREATED));
 * }
 * </pre>
 */
@Getter
public class EntityChangeEvent extends ApplicationEvent {

    /**
     * The entity instance that was changed. Must extend
     * {@link com.aboff.core.model.entity.BaseEntity} for ID extraction.
     */
    private final Object entity;

    /**
     * The type of change that occurred on the entity.
     */
    private final ChangeType changeType;

    /**
     * Enumerates the types of lifecycle changes that can be published for an entity.
     */
    public enum ChangeType {
        /** The entity was newly created and persisted. */
        CREATED,
        /** An existing entity was modified and re-persisted. */
        UPDATED,
        /** The entity was permanently (hard) deleted. */
        DELETED,
        /** The entity was soft-deleted (deletedAt timestamp set). */
        SOFT_DELETED,
        /** A previously soft-deleted entity was restored. */
        RESTORED
    }

    /**
     * Constructs a new {@code EntityChangeEvent}.
     *
     * @param source     the object that published the event (typically the service that made the change)
     * @param entity     the entity instance that was changed
     * @param changeType the nature of the change
     */
    public EntityChangeEvent(Object source, Object entity, ChangeType changeType) {
        super(source);
        this.entity = entity;
        this.changeType = changeType;
    }
}
