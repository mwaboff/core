package com.aboff.core.service;

import com.aboff.core.model.dto.request.BulkSrdUpdateRequest;
import com.aboff.core.model.dto.response.BulkSrdUpdateResponse;
import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Adversary;
import com.aboff.core.model.entity.dh.AncestryCard;
import com.aboff.core.model.entity.dh.Beastform;
import com.aboff.core.model.entity.dh.CardCostTag;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Domain;
import com.aboff.core.model.entity.dh.Encounter;
import com.aboff.core.model.entity.dh.Environment;
import com.aboff.core.model.entity.dh.Feature;
import com.aboff.core.model.entity.dh.Question;
import com.aboff.core.model.entity.dh.TransformationCard;
import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.AdminActionType;
import com.aboff.core.model.enums.SearchableEntityType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.service.dh.SubclassPathService;
import com.aboff.core.service.search.SearchTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminContentService}, the bulk SRD-flagging tool backing
 * {@code PATCH /api/admin/content/srd}.
 */
@ExtendWith(MockitoExtension.class)
class AdminContentServiceTest {

    @Mock private SearchTypeRegistry searchTypeRegistry;
    @Mock private AdminUserService adminUserService;
    @Mock private SubclassPathService subclassPathService;

    private AdminContentService service;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new AdminContentService(searchTypeRegistry, adminUserService, subclassPathService);
        actor = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity> JpaRepository<T, Long> mockRepositoryReturning(List<T> rows) {
        JpaRepository<T, Long> repository = mock(JpaRepository.class);
        when(repository.findAllById(any())).thenReturn(rows);
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        return repository;
    }

    @Test
    void updateSrd_matchesRequestedIds_marksThemAndReportsNoUnknowns() {
        Domain d1 = Domain.builder().id(1L).name("Blade").build();
        Domain d2 = Domain.builder().id(2L).name("Bone").build();
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of(d1, d2));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("DOMAIN").ids(List.of(1L, 2L)).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getType()).isEqualTo("DOMAIN");
        assertThat(response.getSrd()).isTrue();
        assertThat(response.getUpdatedIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.getUnknownIds()).isEmpty();
        assertThat(d1.getSrd()).isTrue();
        assertThat(d2.getSrd()).isTrue();
    }

    @Test
    void updateSrd_someIdsMissing_reportsThemAsUnknownWithoutFailingTheBatch() {
        Domain d1 = Domain.builder().id(1L).name("Blade").build();
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of(d1));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("DOMAIN").ids(List.of(1L, 999L)).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getUpdatedIds()).containsExactly(1L);
        assertThat(response.getUnknownIds()).containsExactly(999L);
    }

    @Test
    void updateSrd_unmarking_setsSrdFalse() {
        Domain d1 = Domain.builder().id(1L).name("Blade").srd(true).build();
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of(d1));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("DOMAIN").ids(List.of(1L)).srd(false).build();

        service.updateSrd(actor, request, "127.0.0.1");

        assertThat(d1.getSrd()).isFalse();
    }

    @Test
    void updateSrd_emptyIds_updatesNothingAndReportsNoUnknowns() {
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of());
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("DOMAIN").ids(List.of()).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getUpdatedIds()).isEmpty();
        assertThat(response.getUnknownIds()).isEmpty();
    }

    @Test
    void updateSrd_unknownTypeString_throwsWithClearMessage() {
        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("NOT_A_REAL_TYPE").ids(List.of(1L)).srd(true).build();

        assertThatThrownBy(() -> service.updateSrd(actor, request, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown content type")
                .hasMessageContaining("NOT_A_REAL_TYPE");

        verify(adminUserService, never()).recordContentAction(any(), any(), anyString(), anyString());
    }

    @Test
    void updateSrd_blankType_throws() {
        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("  ").ids(List.of(1L)).srd(true).build();

        assertThatThrownBy(() -> service.updateSrd(actor, request, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Type is required");
    }

    @Test
    void updateSrd_expansionType_isRejected() {
        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("EXPANSION").ids(List.of(1L)).srd(true).build();

        assertThatThrownBy(() -> service.updateSrd(actor, request, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPANSION");

        verify(adminUserService, never()).recordContentAction(any(), any(), anyString(), anyString());
    }

    @Test
    void updateSrd_subclassCardType_isRejectedWithRedirectHint() {
        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("SUBCLASS_CARD").ids(List.of(1L)).srd(true).build();

        assertThatThrownBy(() -> service.updateSrd(actor, request, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUBCLASS_PATH");

        verify(adminUserService, never()).recordContentAction(any(), any(), anyString(), anyString());
    }

    @Test
    void updateSrd_isCaseInsensitiveOnTypeKey() {
        Domain d1 = Domain.builder().id(1L).name("Blade").build();
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of(d1));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("domain").ids(List.of(1L)).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getType()).isEqualTo("DOMAIN");
    }

    @Test
    void updateSrd_writesExactlyOneAuditRowPerBatch() {
        Domain d1 = Domain.builder().id(1L).name("Blade").build();
        Domain d2 = Domain.builder().id(2L).name("Bone").build();
        JpaRepository<Domain, Long> repository = mockRepositoryReturning(List.of(d1, d2));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(SearchableEntityType.DOMAIN);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("DOMAIN").ids(List.of(1L, 2L)).srd(true).build();

        service.updateSrd(actor, request, "127.0.0.1");

        verify(adminUserService, times(1)).recordContentAction(
                eq(actor), eq(AdminActionType.CONTENT_SRD_CHANGED), anyString(), eq("127.0.0.1"));
    }

    @Test
    void updateSrd_subclassPathType_cascadesToCardsViaSubclassPathService() {
        // SUBCLASS_PATH must cascade srd to its cards, so it bypasses the generic
        // searchTypeRegistry/applySrd dispatch entirely and routes through
        // SubclassPathService#bulkSetSrd instead -- see AdminContentService's class javadoc.
        // The cascade itself is covered by SubclassPathServiceTest#bulkSetSrd_*; this test only
        // asserts the routing and the unknown-id bookkeeping around it.
        when(subclassPathService.bulkSetSrd(List.of(1L, 2L), true)).thenReturn(List.of(1L));

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type("SUBCLASS_PATH").ids(List.of(1L, 2L)).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getType()).isEqualTo("SUBCLASS_PATH");
        assertThat(response.getUpdatedIds()).containsExactly(1L);
        assertThat(response.getUnknownIds()).containsExactly(2L);
        verify(subclassPathService).bulkSetSrd(List.of(1L, 2L), true);
        verify(searchTypeRegistry, never()).repositoryFor(SearchableEntityType.SUBCLASS_PATH);
        verify(adminUserService, times(1)).recordContentAction(
                eq(actor), eq(AdminActionType.CONTENT_SRD_CHANGED), anyString(), eq("127.0.0.1"));
    }

    /**
     * Every {@link SearchableEntityType} that carries an {@code srd} column and is dispatched
     * generically through {@code applySrd}, paired with a minimal instance of the entity it maps
     * to. Exercises every arm of the private {@code applySrd} dispatch switch (Card, BaseItem,
     * and each of the eleven standalone entities) via the public
     * {@link AdminContentService#updateSrd} entry point.
     * <p>
     * {@link SearchableEntityType#SUBCLASS_PATH} is deliberately absent — it never reaches
     * {@code applySrd} and is covered instead by
     * {@link #updateSrd_subclassPathType_cascadesToCardsViaSubclassPathService}.
     * </p>
     */
    static Stream<Arguments> flaggableTypes() {
        return Stream.of(
                Arguments.of(SearchableEntityType.ANCESTRY_CARD, (Supplier<BaseEntity>) () -> {
                    AncestryCard c = new AncestryCard();
                    c.setId(1L);
                    return c;
                }),
                Arguments.of(SearchableEntityType.WEAPON, (Supplier<BaseEntity>) () -> {
                    Weapon w = new Weapon();
                    w.setId(1L);
                    return w;
                }),
                Arguments.of(SearchableEntityType.ADVERSARY, (Supplier<BaseEntity>) () -> {
                    Adversary a = new Adversary();
                    a.setId(1L);
                    return a;
                }),
                Arguments.of(SearchableEntityType.BEASTFORM, (Supplier<BaseEntity>) () -> {
                    Beastform b = new Beastform();
                    b.setId(1L);
                    return b;
                }),
                Arguments.of(SearchableEntityType.CARD_COST_TAG, (Supplier<BaseEntity>) () -> {
                    CardCostTag t = new CardCostTag();
                    t.setId(1L);
                    return t;
                }),
                Arguments.of(SearchableEntityType.CLASS, (Supplier<BaseEntity>) () -> {
                    com.aboff.core.model.entity.dh.Class c = new com.aboff.core.model.entity.dh.Class();
                    c.setId(1L);
                    return c;
                }),
                Arguments.of(SearchableEntityType.CONDITION, (Supplier<BaseEntity>) () -> {
                    Condition c = new Condition();
                    c.setId(1L);
                    return c;
                }),
                Arguments.of(SearchableEntityType.DOMAIN, (Supplier<BaseEntity>) () -> {
                    Domain d = new Domain();
                    d.setId(1L);
                    return d;
                }),
                Arguments.of(SearchableEntityType.ENCOUNTER, (Supplier<BaseEntity>) () -> {
                    Encounter e = new Encounter();
                    e.setId(1L);
                    return e;
                }),
                Arguments.of(SearchableEntityType.ENVIRONMENT, (Supplier<BaseEntity>) () -> {
                    Environment e = new Environment();
                    e.setId(1L);
                    return e;
                }),
                Arguments.of(SearchableEntityType.FEATURE, (Supplier<BaseEntity>) () -> {
                    Feature f = new Feature();
                    f.setId(1L);
                    return f;
                }),
                Arguments.of(SearchableEntityType.QUESTION, (Supplier<BaseEntity>) () -> {
                    Question q = new Question();
                    q.setId(1L);
                    return q;
                }),
                Arguments.of(SearchableEntityType.TRANSFORMATION_CARD, (Supplier<BaseEntity>) () -> {
                    TransformationCard tc = new TransformationCard();
                    tc.setId(1L);
                    return tc;
                })
        );
    }

    @ParameterizedTest
    @MethodSource("flaggableTypes")
    @SuppressWarnings("unchecked")
    void updateSrd_dispatchesToEveryFlaggableEntityType(SearchableEntityType type, Supplier<BaseEntity> entitySupplier)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        BaseEntity entity = entitySupplier.get();
        JpaRepository<BaseEntity, Long> repository = mock(JpaRepository.class);
        when(repository.findAllById(any())).thenReturn(List.of(entity));
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(repository).when(searchTypeRegistry).repositoryFor(type);

        BulkSrdUpdateRequest request = BulkSrdUpdateRequest.builder()
                .type(type.name()).ids(List.of(1L)).srd(true).build();

        BulkSrdUpdateResponse response = service.updateSrd(actor, request, "127.0.0.1");

        assertThat(response.getUpdatedIds()).containsExactly(1L);
        Object srd = entity.getClass().getMethod("getSrd").invoke(entity);
        assertThat(srd).isEqualTo(Boolean.TRUE);
    }
}
