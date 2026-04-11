package com.aboff.core.service;

import com.aboff.core.config.SearchFieldMapping;
import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.SearchIndex;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.repository.dh.WeaponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchIndexService}.
 *
 * <p>Mocks {@link SearchIndexRepository} and {@link SearchFieldMapping} to verify
 * indexing, removal, and soft-deletion behavior in isolation.
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;

    @Mock
    private SearchFieldMapping searchFieldMapping;

    @Mock
    private WeaponRepository weaponRepository;

    @InjectMocks
    private SearchIndexService searchIndexService;

    // ==================== STUB ENTITIES ====================

    @SearchIndexed(type = SearchableEntityType.WEAPON)
    private static class IndexedEntity extends BaseEntity {
        IndexedEntity(Long id) {
            setId(id);
        }
    }

    private static class NonIndexedEntity extends BaseEntity {
        NonIndexedEntity(Long id) {
            setId(id);
        }
    }

    private SearchFieldMapping.SearchIndexData buildMinimalData(SearchableEntityType type, Long entityId) {
        return SearchFieldMapping.SearchIndexData.builder()
                .entityType(type.name())
                .entityId(entityId)
                .name("Test Entity")
                .nameText("Test Entity")
                .build();
    }

    // ==================== indexEntity TESTS ====================

    @Test
    void indexEntity_WithAnnotatedEntity_CallsUpsert() {
        // Arrange
        IndexedEntity entity = new IndexedEntity(1L);
        SearchFieldMapping.SearchIndexData data = buildMinimalData(SearchableEntityType.WEAPON, 1L);
        when(searchFieldMapping.buildSearchIndexData(entity, SearchableEntityType.WEAPON)).thenReturn(data);

        // Act
        searchIndexService.indexEntity(entity);

        // Assert
        verify(searchIndexRepository).upsertSearchIndex(
                eq("WEAPON"), eq(1L), eq("Test Entity"), eq("Test Entity"),
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null)
        );
    }

    @Test
    void indexEntity_WithNonAnnotatedEntity_DoesNotCallUpsert() {
        // Arrange
        NonIndexedEntity entity = new NonIndexedEntity(2L);

        // Act
        searchIndexService.indexEntity(entity);

        // Assert
        verify(searchIndexRepository, never()).upsertSearchIndex(
                anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void indexEntity_WhenBuildReturnsNull_SkipsUpsert() {
        // Arrange — simulate a Feature with a null name (name is nullable in DB)
        IndexedEntity entity = new IndexedEntity(3L);
        when(searchFieldMapping.buildSearchIndexData(entity, SearchableEntityType.WEAPON)).thenReturn(null);

        // Act
        searchIndexService.indexEntity(entity);

        // Assert
        verify(searchIndexRepository, never()).upsertSearchIndex(
                anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    // ==================== removeEntity TESTS ====================

    @Test
    void removeEntity_CallsDeleteByEntityTypeAndEntityId() {
        // Arrange
        SearchableEntityType type = SearchableEntityType.WEAPON;
        Long entityId = 10L;

        // Act
        searchIndexService.removeEntity(type, entityId);

        // Assert
        verify(searchIndexRepository).deleteByEntityTypeAndEntityId("WEAPON", 10L);
    }

    @Test
    void removeEntity_ForDomainType_PassesCorrectTypeName() {
        // Arrange
        SearchableEntityType type = SearchableEntityType.DOMAIN;
        Long entityId = 20L;

        // Act
        searchIndexService.removeEntity(type, entityId);

        // Assert
        verify(searchIndexRepository).deleteByEntityTypeAndEntityId("DOMAIN", 20L);
    }

    // ==================== softDeleteEntity TESTS ====================

    @Test
    void softDeleteEntity_WhenEntryExists_SetsDeletedAt() {
        // Arrange
        SearchIndex indexEntry = SearchIndex.builder()
                .entityType("WEAPON")
                .entityId(5L)
                .name("Test Weapon")
                .build();
        when(searchIndexRepository.findByEntityTypeAndEntityId("WEAPON", 5L))
                .thenReturn(Optional.of(indexEntry));

        // Act
        searchIndexService.softDeleteEntity(SearchableEntityType.WEAPON, 5L);

        // Assert
        verify(searchIndexRepository).save(indexEntry);
    }

    @Test
    void softDeleteEntity_WhenEntryExists_MarksEntryAsDeleted() {
        // Arrange
        SearchIndex indexEntry = SearchIndex.builder()
                .entityType("ARMOR")
                .entityId(6L)
                .name("Test Armor")
                .build();
        when(searchIndexRepository.findByEntityTypeAndEntityId("ARMOR", 6L))
                .thenReturn(Optional.of(indexEntry));

        // Act
        searchIndexService.softDeleteEntity(SearchableEntityType.ARMOR, 6L);

        // Assert - verify softDelete was called by checking deletedAt is set
        org.assertj.core.api.Assertions.assertThat(indexEntry.isDeleted()).isTrue();
    }

    @Test
    void softDeleteEntity_WhenEntryDoesNotExist_DoesNotSave() {
        // Arrange
        when(searchIndexRepository.findByEntityTypeAndEntityId("WEAPON", 99L))
                .thenReturn(Optional.empty());

        // Act
        searchIndexService.softDeleteEntity(SearchableEntityType.WEAPON, 99L);

        // Assert
        verify(searchIndexRepository, never()).save(any());
    }

    // ==================== reindexAll TESTS ====================

    @Test
    void reindexAll_ForWeaponType_ClearsThenIndexesActiveEntities() {
        // Arrange — two active weapons and one soft-deleted
        Weapon active1 = new Weapon();
        active1.setId(1L);
        active1.setName("Sword");

        Weapon active2 = new Weapon();
        active2.setId(2L);
        active2.setName("Bow");

        Weapon deleted = new Weapon();
        deleted.setId(3L);
        deleted.setName("Broken Dagger");
        deleted.setDeletedAt(LocalDateTime.now());

        when(weaponRepository.findAll()).thenReturn(List.of(active1, active2, deleted));
        when(searchFieldMapping.buildSearchIndexData(any(Weapon.class), eq(SearchableEntityType.WEAPON)))
                .thenAnswer(inv -> {
                    Weapon w = inv.getArgument(0);
                    return buildMinimalData(SearchableEntityType.WEAPON, w.getId());
                });

        // Act
        int indexed = searchIndexService.reindexAll(SearchableEntityType.WEAPON);

        // Assert
        assertThat(indexed).isEqualTo(2);
        verify(searchIndexRepository).deleteAllByEntityType("WEAPON");
        verify(searchIndexRepository, times(2)).upsertSearchIndex(
                eq("WEAPON"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void reindexAll_ForBeastformType_ClearsButDoesNotRepopulate() {
        // Arrange — Beastform has no repository, so reindex can only clear

        // Act
        int indexed = searchIndexService.reindexAll(SearchableEntityType.BEASTFORM);

        // Assert
        assertThat(indexed).isZero();
        verify(searchIndexRepository).deleteAllByEntityType("BEASTFORM");
        verify(searchIndexRepository, never()).upsertSearchIndex(
                anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}
