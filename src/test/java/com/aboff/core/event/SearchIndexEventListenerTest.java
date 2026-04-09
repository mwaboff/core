package com.aboff.core.event;

import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.service.SearchIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link SearchIndexEventListener}.
 *
 * <p>Verifies that the correct {@link SearchIndexService} method is invoked for each
 * {@link EntityChangeEvent.ChangeType}, and that non-annotated entities are silently ignored.
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexEventListenerTest {

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private SearchIndexEventListener listener;

    // ==================== ANNOTATED ENTITY STUB ====================

    /**
     * Minimal {@link BaseEntity} subclass annotated with {@link SearchIndexed} for testing.
     */
    @SearchIndexed(type = SearchableEntityType.WEAPON)
    private static class IndexedEntity extends BaseEntity {
        IndexedEntity(Long id) {
            setId(id);
        }
    }

    /**
     * Minimal entity that does NOT carry the {@link SearchIndexed} annotation.
     */
    private static class NonIndexedEntity extends BaseEntity {
        NonIndexedEntity(Long id) {
            setId(id);
        }
    }

    // ==================== CREATED EVENT TESTS ====================

    @Test
    void onEntityChange_CreatedEvent_TriggersIndexEntity() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(1L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.CREATED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verify(searchIndexService).indexEntity(entity);
    }

    // ==================== UPDATED EVENT TESTS ====================

    @Test
    void onEntityChange_UpdatedEvent_TriggersIndexEntity() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(2L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.UPDATED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verify(searchIndexService).indexEntity(entity);
    }

    // ==================== RESTORED EVENT TESTS ====================

    @Test
    void onEntityChange_RestoredEvent_TriggersIndexEntity() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(3L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.RESTORED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verify(searchIndexService).indexEntity(entity);
    }

    // ==================== SOFT_DELETED EVENT TESTS ====================

    @Test
    void onEntityChange_SoftDeletedEvent_TriggersSoftDeleteEntity() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(4L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.SOFT_DELETED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verify(searchIndexService).softDeleteEntity(SearchableEntityType.WEAPON, 4L);
    }

    // ==================== DELETED EVENT TESTS ====================

    @Test
    void onEntityChange_DeletedEvent_TriggersRemoveEntity() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(5L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.DELETED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verify(searchIndexService).removeEntity(SearchableEntityType.WEAPON, 5L);
    }

    // ==================== NON-ANNOTATED ENTITY TESTS ====================

    @Test
    void onEntityChange_NonIndexedEntity_SkipsIndexUpdate() {
        // Arrange
        NonIndexedEntity entity = new NonIndexedEntity(6L);
        EntityChangeEvent event = new EntityChangeEvent(this, entity, EntityChangeEvent.ChangeType.CREATED);

        // Act
        listener.onEntityChange(event);

        // Assert
        verifyNoInteractions(searchIndexService);
    }
}
