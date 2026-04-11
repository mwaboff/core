package com.aboff.core.event;

import com.aboff.core.event.EntityChangeEvent.ChangeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EntityChangeEvent}.
 *
 * <p>Verifies that events are constructed correctly with all supported change types
 * and that getters return the expected values.
 */
class EntityChangeEventTest {

    // ==================== CHANGE TYPE ENUM TESTS ====================

    @Test
    void changeType_Created_IsAvailable() {
        // Act & Assert
        assertThat(ChangeType.CREATED).isNotNull();
    }

    @Test
    void changeType_Updated_IsAvailable() {
        // Act & Assert
        assertThat(ChangeType.UPDATED).isNotNull();
    }

    @Test
    void changeType_Deleted_IsAvailable() {
        // Act & Assert
        assertThat(ChangeType.DELETED).isNotNull();
    }

    @Test
    void changeType_SoftDeleted_IsAvailable() {
        // Act & Assert
        assertThat(ChangeType.SOFT_DELETED).isNotNull();
    }

    @Test
    void changeType_Restored_IsAvailable() {
        // Act & Assert
        assertThat(ChangeType.RESTORED).isNotNull();
    }

    @Test
    void changeType_ValuesCount_IsFive() {
        // Act & Assert
        assertThat(ChangeType.values()).hasSize(5);
    }

    // ==================== CONSTRUCTOR AND GETTERS TESTS ====================

    @Test
    void constructor_WithCreatedType_StoresEntityCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.CREATED);

        // Assert
        assertThat(event.getEntity()).isSameAs(entity);
    }

    @Test
    void constructor_WithCreatedType_StoresChangeTypeCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.CREATED);

        // Assert
        assertThat(event.getChangeType()).isEqualTo(ChangeType.CREATED);
    }

    @Test
    void constructor_WithUpdatedType_StoresChangeTypeCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.UPDATED);

        // Assert
        assertThat(event.getChangeType()).isEqualTo(ChangeType.UPDATED);
    }

    @Test
    void constructor_WithDeletedType_StoresChangeTypeCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.DELETED);

        // Assert
        assertThat(event.getChangeType()).isEqualTo(ChangeType.DELETED);
    }

    @Test
    void constructor_WithSoftDeletedType_StoresChangeTypeCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.SOFT_DELETED);

        // Assert
        assertThat(event.getChangeType()).isEqualTo(ChangeType.SOFT_DELETED);
    }

    @Test
    void constructor_WithRestoredType_StoresChangeTypeCorrectly() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.RESTORED);

        // Assert
        assertThat(event.getChangeType()).isEqualTo(ChangeType.RESTORED);
    }

    @Test
    void getSource_ReturnsSourceObject() {
        // Arrange
        Object source = new Object();
        Object entity = new Object();

        // Act
        EntityChangeEvent event = new EntityChangeEvent(source, entity, ChangeType.CREATED);

        // Assert
        assertThat(event.getSource()).isSameAs(source);
    }
}
