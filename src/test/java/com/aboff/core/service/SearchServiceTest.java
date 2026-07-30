package com.aboff.core.service;

import com.aboff.core.model.dto.dh.response.BeastformResponse;
import com.aboff.core.model.dto.dh.response.WeaponResponse;
import com.aboff.core.model.dto.response.SearchResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.repository.SearchIndexRepository;
import com.aboff.core.service.dh.AdversaryService;
import com.aboff.core.service.dh.AncestryCardService;
import com.aboff.core.service.dh.ArmorService;
import com.aboff.core.service.dh.BeastformService;
import com.aboff.core.service.dh.CardCostTagService;
import com.aboff.core.service.dh.ClassService;
import com.aboff.core.service.dh.CommunityCardService;
import com.aboff.core.service.dh.DomainCardService;
import com.aboff.core.service.dh.DomainService;
import com.aboff.core.service.dh.EncounterService;
import com.aboff.core.service.dh.ExpansionService;
import com.aboff.core.service.dh.FeatureService;
import com.aboff.core.service.dh.LootService;
import com.aboff.core.service.dh.QuestionService;
import com.aboff.core.service.dh.SubclassCardService;
import com.aboff.core.service.dh.SubclassPathService;
import com.aboff.core.service.dh.WeaponService;
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
 * the database and external services.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;
    @Mock
    private RoleHierarchyService roleHierarchyService;
    @Mock
    private AdversaryService adversaryService;
    @Mock
    private AncestryCardService ancestryCardService;
    @Mock
    private ArmorService armorService;
    @Mock
    private BeastformService beastformService;
    @Mock
    private CardCostTagService cardCostTagService;
    @Mock
    private ClassService classService;
    @Mock
    private CommunityCardService communityCardService;
    @Mock
    private DomainCardService domainCardService;
    @Mock
    private DomainService domainService;
    @Mock
    private EncounterService encounterService;
    @Mock
    private ExpansionService expansionService;
    @Mock
    private FeatureService featureService;
    @Mock
    private LootService lootService;
    @Mock
    private QuestionService questionService;
    @Mock
    private SubclassCardService subclassCardService;
    @Mock
    private SubclassPathService subclassPathService;
    @Mock
    private WeaponService weaponService;

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
        when(weaponService.getWeaponById(eq(42L), anyString())).thenReturn(WeaponResponse.builder().build());

        // Act
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert
        verify(weaponService).getWeaponById(eq(42L), eq("entity"));
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

        // Assert — weaponService should never be called without expand
        org.mockito.Mockito.verifyNoInteractions(weaponService);
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
        org.mockito.Mockito.verifyNoInteractions(weaponService);
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
        when(weaponService.getWeaponById(eq(42L), anyString())).thenReturn(WeaponResponse.builder().build());

        // Act
        searchService.search("longsword", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "all", 0, 20, user);

        // Assert
        verify(weaponService).getWeaponById(eq(42L), eq("all"));
    }

    @Test
    void search_WithExpandEntityKeyword_ResolvesBeastformEntity() {
        // Arrange — proves the BEASTFORM fix: resolveEntity() no longer hardcodes null,
        // it now delegates to BeastformService like every other type.
        User user = userWithRole(Role.USER);
        Object[] row = buildRow("BEASTFORM", 7L, "Wolf", 0.9);
        when(roleHierarchyService.isPrivilegedRole(Role.USER)).thenReturn(false);
        when(searchIndexRepository.search(
                anyString(), anyBoolean(), any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(singleRowPage(row));
        when(beastformService.getBeastformById(eq(7L), anyString()))
                .thenReturn(BeastformResponse.builder().id(7L).name("Wolf").build());

        // Act
        SearchResponse response = searchService.search("wolf", null, null, null, null, null, null, null, null, null,
                null, null, null, null, "entity", 0, 20, user);

        // Assert — a real BeastformResponse comes back, not null
        verify(beastformService).getBeastformById(eq(7L), eq("entity"));
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getExpandedEntity()).isNotNull();
        assertThat(response.getResults().get(0).getExpandedEntity()).isInstanceOf(BeastformResponse.class);
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
