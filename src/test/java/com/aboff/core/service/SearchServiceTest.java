package com.aboff.core.service;

import com.aboff.core.model.dto.dh.response.BeastformResponse;
import com.aboff.core.model.dto.dh.response.ConditionResponse;
import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.SearchResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.service.search.SearchTypeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SearchService}.
 *
 * <p>All dependencies are mocked via Mockito so that tests remain fast and isolated from
 * the database and external services. Per-type entity resolution now delegates entirely to
 * {@link SearchTypeRegistry#resolveEntity}, so tests that used to stub/verify individual
 * game-content services (e.g. {@code WeaponService}) instead stub/verify the registry mock.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;
    @Mock
    private RoleHierarchyService roleHierarchyService;
    @Mock
    private SearchTypeRegistry searchTypeRegistry;

    @InjectMocks
    private SearchService searchService;

    // ==================== HELPER METHODS ====================

    /**
     * Creates a test user with the given role.
     */
    private User userWithRole(Role role) {
        return User.builder().id(1L).username("tester").email("t@t.com").role(role).build();
    }

    /**
     * Builds a minimal Object[] row matching the column layout expected by SearchService.
     * Columns: [id, entityType, entityId, name, ...23 filter/vector cols..., relevanceScore]
     */
    private Object[] buildRow(String entityType, Long entityId, String name, double score) {
        Object[] row = new Object[28];
        row[0] = 100L;              // COL_ID
        row[1] = entityType;        // COL_ENTITY_TYPE
        row[2] = entityId;          // COL_ENTITY_ID
        row[3] = name;              // COL_NAME
        // columns 4–26 are filter/vector fields — left null
        row[27] = score;            // COL_RELEVANCE_SCORE
        return row;
    }

    /**
     * Returns a typed empty page of Object[] rows.
     */
    private Page<Object[]> emptyPage() {
        return new PageImpl<>(Collections.<Object[]>emptyList());
    }

    /**
     * Returns a typed page containing a single row.
     */
    private Page<Object[]> singleRowPage(Object[] row) {
        return new PageImpl<>(List.<Object[]>of(row), PageRequest.of(0, 20), 1L);
    }

    private void stubEmptyPageForUser(User user) {
        when(roleHierarchyService.isPrivilegedRole(user.getRole())).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(emptyPage());
    }

    // ==================== QUERY VALIDATION TESTS ====================

    @Test
    void search_NullQuery_ThrowsIllegalArgumentException() {
        // Arrange
        User user = userWithRole(Role.USER);

        // Act & Assert
        assertThatThrownBy(() -> searchService.search(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query must not be empty");
    }

    @Test
    void search_EmptyQuery_ThrowsIllegalArgumentException() {
        // Arrange
        User user = userWithRole(Role.USER);

        // Act & Assert
        assertThatThrownBy(() -> searchService.search(
                "", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query must not be empty");
    }

    @Test
    void search_WhitespaceOnlyQuery_ThrowsIllegalArgumentException() {
        // Arrange
        User user = userWithRole(Role.USER);

        // Act & Assert
        assertThatThrownBy(() -> searchService.search(
                "   ", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query must not be empty");
    }

    // ==================== SIZE CAP TESTS ====================

    @Test
    void search_SizeLargerThan100_IsCappedAt100() {
        // Arrange
        User user = userWithRole(Role.USER);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(emptyPage());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // Act
        searchService.search("test", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 200, user);

        // Assert
        verify(searchIndexRepository).search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    // ==================== PRIVILEGE TESTS ====================

    @Test
    void search_PrivilegedUser_PassesIsPrivilegedTrue() {
        // Arrange
        User user = userWithRole(Role.ADMIN);
        when(roleHierarchyService.isPrivilegedRole(Role.ADMIN)).thenReturn(true);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), eq(true), any(Pageable.class)))
                .thenReturn(emptyPage());

        // Act
        searchService.search("test", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        verify(searchIndexRepository).search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(1L), eq(true), any(Pageable.class));
    }

    @Test
    void search_NonPrivilegedUser_PassesIsPrivilegedFalse() {
        // Arrange
        User user = userWithRole(Role.USER);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), eq(false), any(Pageable.class)))
                .thenReturn(emptyPage());

        // Act
        searchService.search("test", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        verify(searchIndexRepository).search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(1L), eq(false), any(Pageable.class));
    }

    // ==================== ENTITY TYPE FILTER TESTS ====================

    @Test
    void search_WithEntityTypeFilter_PassesTypeNamesToRepository() {
        // Arrange
        User user = userWithRole(Role.USER);
        List<SearchableEntityType> types = List.of(SearchableEntityType.WEAPON, SearchableEntityType.ARMOR);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), eq(true), eq(List.of("WEAPON", "ARMOR")), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(emptyPage());

        // Act
        searchService.search("test", types, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        verify(searchIndexRepository).search(
                anyString(), eq(true), eq(List.of("WEAPON", "ARMOR")), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), anyLong(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void search_NullEntityTypes_PassesFilterFalseAndSentinelList() {
        // Arrange — when no types are provided, the service must pass filterByEntityTypes=false
        // plus a non-null sentinel list for the IN clause. PostgreSQL cannot determine the
        // parameter type for a NULL list parameter referenced inside an IN expression.
        User user = userWithRole(Role.USER);
        stubEmptyPageForUser(user);

        // Act
        searchService.search("test", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        verify(searchIndexRepository).search(
                anyString(), eq(false), eq(List.of("")), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class));
    }

    // ==================== PAGINATION METADATA TESTS ====================

    @Test
    void search_WithResults_ReturnsCorrectTotalElements() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 1L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act
        SearchResponse response = searchService.search(
                "longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void search_WithResults_ReturnsCorrectCurrentPage() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 1L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act
        SearchResponse response = searchService.search(
                "longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getCurrentPage()).isEqualTo(0);
    }

    @Test
    void search_WithResults_ReturnsCorrectPageSize() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 1L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act
        SearchResponse response = searchService.search(
                "longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getPageSize()).isEqualTo(20);
    }

    @Test
    void search_WithResults_ReturnsQueryInResponse() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 1L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act
        SearchResponse response = searchService.search(
                "longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getQuery()).isEqualTo("longsword");
    }

    // ==================== EMPTY RESULTS TESTS ====================

    @Test
    void search_NoResults_ReturnsEmptyResultsList() {
        // Arrange
        User user = userWithRole(Role.USER);
        stubEmptyPageForUser(user);

        // Act
        SearchResponse response = searchService.search(
                "unknownterm", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getResults()).isEmpty();
    }

    @Test
    void search_NoResults_ReturnsTotalElementsZero() {
        // Arrange
        User user = userWithRole(Role.USER);
        stubEmptyPageForUser(user);

        // Act
        SearchResponse response = searchService.search(
                "unknownterm", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response.getTotalElements()).isEqualTo(0L);
    }

    // ==================== EXPAND TESTS ====================

    @Test
    void search_WithExpandEntityKeyword_ResolvesWeaponEntity() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 42L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(searchTypeRegistry.resolveEntity(eq(SearchableEntityType.WEAPON), eq(42L), anyString(), any()))
                .thenReturn(WeaponResponse.builder().build());

        // Act
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert
        verify(searchTypeRegistry).resolveEntity(eq(SearchableEntityType.WEAPON), eq(42L), eq("entity"), any());
    }

    @Test
    void search_WithNullExpand_DoesNotResolveWeaponEntity() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 42L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert — resolution should never be attempted without expand
        org.mockito.Mockito.verifyNoInteractions(searchTypeRegistry);
    }

    @Test
    void search_WithUnrelatedExpandKeyword_DoesNotResolveEntity() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 42L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));

        // Act — 'other' doesn't contain 'entity' or 'all', so entity should not be resolved
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "other", 0, 20, user);

        // Assert
        org.mockito.Mockito.verifyNoInteractions(searchTypeRegistry);
    }

    @Test
    void search_WithAllExpandKeyword_ResolvesWeaponEntity() {
        // Arrange
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 42L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(searchTypeRegistry.resolveEntity(eq(SearchableEntityType.WEAPON), eq(42L), anyString(), any()))
                .thenReturn(WeaponResponse.builder().build());

        // Act
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "all", 0, 20, user);

        // Assert
        verify(searchTypeRegistry).resolveEntity(eq(SearchableEntityType.WEAPON), eq(42L), eq("all"), any());
    }

    @Test
    void search_WithExpandEntityKeyword_ResolvesBeastformEntity() {
        // Arrange — proves the BEASTFORM fix continues to hold: resolveEntity() no longer
        // hardcodes null, it now delegates to SearchTypeRegistry (backed in production by
        // BeastformSearchRegistration) like every other type.
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("BEASTFORM", 7L, "Wolf", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(searchTypeRegistry.resolveEntity(eq(SearchableEntityType.BEASTFORM), eq(7L), anyString(), any()))
                .thenReturn(BeastformResponse.builder().id(7L).name("Wolf").build());

        // Act
        SearchResponse response = searchService.search("wolf", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert — a real BeastformResponse comes back, not null
        verify(searchTypeRegistry).resolveEntity(eq(SearchableEntityType.BEASTFORM), eq(7L), eq("entity"), any());
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getExpandedEntity()).isNotNull();
        assertThat(response.getResults().get(0).getExpandedEntity()).isInstanceOf(BeastformResponse.class);
    }

    @Test
    void search_WithExpandEntityKeyword_ResolvesConditionEntity() {
        // Arrange — proves a created Condition is findable and expandable via search,
        // exactly like every other registered type.
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("CONDITION", 9L, "Restrained", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(searchTypeRegistry.resolveEntity(eq(SearchableEntityType.CONDITION), eq(9L), anyString(), any()))
                .thenReturn(ConditionResponse.builder().id(9L).name("Restrained").build());

        // Act
        SearchResponse response = searchService.search("restrained", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert — a real ConditionResponse comes back, not null
        verify(searchTypeRegistry).resolveEntity(eq(SearchableEntityType.CONDITION), eq(9L), eq("entity"), any());
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getExpandedEntity()).isInstanceOf(ConditionResponse.class);
    }

    @Test
    void search_WhenRegistryThrowsEntityNotFound_ResolvesToNullExpandedEntity() {
        // Arrange — resolveEntity() must catch EntityNotFoundException from the registry so one
        // missing entity does not abort the whole search response.
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("WEAPON", 42L, "Longsword", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(searchTypeRegistry.resolveEntity(eq(SearchableEntityType.WEAPON), eq(42L), anyString(), any(Authentication.class)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("gone"));

        // Act
        SearchResponse response = searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getExpandedEntity()).isNull();
    }

    // ==================== VALID SEARCH TEST ====================

    @Test
    void search_WithValidQuery_ReturnsNonNullResponse() {
        // Arrange
        User user = userWithRole(Role.USER);
        stubEmptyPageForUser(user);

        // Act
        SearchResponse response = searchService.search(
                "dragon", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 20, user);

        // Assert
        assertThat(response).isNotNull();
    }
}
