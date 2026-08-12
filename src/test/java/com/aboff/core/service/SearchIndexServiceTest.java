package com.aboff.core.service;

import com.aboff.core.config.SearchFieldMapping;
import com.aboff.core.model.annotation.SearchIndexed;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.SearchIndex;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.repository.dh.BeastformRepository;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.repository.dh.WeaponRepository;
import com.aboff.core.service.search.SearchTypeRegistry;
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
 * <p>Mocks {@link SearchIndexRepository}, {@link SearchFieldMapping}, and {@link SearchTypeRegistry}
 * to verify indexing, removal, soft-deletion, and reindex behavior in isolation. {@code resolveRepository}
 * now delegates entirely to {@link SearchTypeRegistry#repositoryFor}, so per-type repository lookups are
 * stubbed on the registry mock rather than on individually mocked repository beans.
 */
@ExtendWith(MockitoExtension.class)
class SearchIndexServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;

    @Mock
    private SearchFieldMapping searchFieldMapping;

    @Mock
    private SearchTypeRegistry searchTypeRegistry;

    @Mock
    private WeaponRepository weaponRepository;

    @Mock
    private BeastformRepository beastformRepository;

    @Mock
    private ConditionRepository conditionRepository;

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
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                eq("{}"), eq(null)
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
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
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
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
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

        org.mockito.Mockito.doReturn(weaponRepository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.WEAPON);
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
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any()
        );
    }

    @Test
    void reindexAll_ForBeastformType_ClearsThenIndexesActiveEntities() {
        // Arrange — two active beastforms and one soft-deleted
        Beastform active1 = new Beastform();
        active1.setId(1L);
        active1.setName("Wolf");

        Beastform active2 = new Beastform();
        active2.setId(2L);
        active2.setName("Bear");

        Beastform deleted = new Beastform();
        deleted.setId(3L);
        deleted.setName("Retired Form");
        deleted.setDeletedAt(LocalDateTime.now());

        org.mockito.Mockito.doReturn(beastformRepository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.BEASTFORM);
        when(beastformRepository.findAll()).thenReturn(List.of(active1, active2, deleted));
        when(searchFieldMapping.buildSearchIndexData(any(Beastform.class), eq(SearchableEntityType.BEASTFORM)))
                .thenAnswer(inv -> {
                    Beastform b = inv.getArgument(0);
                    return buildMinimalData(SearchableEntityType.BEASTFORM, b.getId());
                });

        // Act
        int indexed = searchIndexService.reindexAll(SearchableEntityType.BEASTFORM);

        // Assert — BeastformRepository is wired into the registry, so reindex clears then
        // fully repopulates the index instead of leaving it empty. See BeastformSearchRegistration.
        assertThat(indexed).isEqualTo(2);
        verify(searchIndexRepository).deleteAllByEntityType("BEASTFORM");
        verify(searchIndexRepository, times(2)).upsertSearchIndex(
                eq("BEASTFORM"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any()
        );
    }

    @Test
    void reindexAll_ForConditionType_ClearsThenIndexesActiveEntities() {
        // Arrange — two active conditions and one soft-deleted
        Condition active1 = new Condition();
        active1.setId(1L);
        active1.setName("Restrained");

        Condition active2 = new Condition();
        active2.setId(2L);
        active2.setName("Vulnerable");

        Condition deleted = new Condition();
        deleted.setId(3L);
        deleted.setName("Retired Condition");
        deleted.setDeletedAt(LocalDateTime.now());

        org.mockito.Mockito.doReturn(conditionRepository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.CONDITION);
        when(conditionRepository.findAll()).thenReturn(List.of(active1, active2, deleted));
        when(searchFieldMapping.buildSearchIndexData(any(Condition.class), eq(SearchableEntityType.CONDITION)))
                .thenAnswer(inv -> {
                    Condition c = inv.getArgument(0);
                    return buildMinimalData(SearchableEntityType.CONDITION, c.getId());
                });

        // Act
        int indexed = searchIndexService.reindexAll(SearchableEntityType.CONDITION);

        // Assert
        assertThat(indexed).isEqualTo(2);
        verify(searchIndexRepository).deleteAllByEntityType("CONDITION");
        verify(searchIndexRepository, times(2)).upsertSearchIndex(
                eq("CONDITION"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any()
        );
    }

    @Test
    void reindexAll_WhenRegistryReturnsNoRepository_ClearsButDoesNotRepopulate() {
        // Arrange — defensive test: resolveRepository() delegates to
        // SearchTypeRegistry#repositoryFor, which is guaranteed non-null in production by the
        // registry's own startup validation. This test exercises the defensive null-check that
        // remains in reindexAll() in case that guarantee is ever bypassed (e.g. a test double).
        when(searchTypeRegistry.repositoryFor(SearchableEntityType.WEAPON)).thenReturn(null);

        // Act
        int indexed = searchIndexService.reindexAll(SearchableEntityType.WEAPON);

        // Assert
        assertThat(indexed).isEqualTo(0);
        verify(searchIndexRepository).deleteAllByEntityType("WEAPON");
        verify(searchIndexRepository, never()).upsertSearchIndex(
                anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}
