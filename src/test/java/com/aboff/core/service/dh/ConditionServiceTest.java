package com.aboff.core.service.dh;

import com.aboff.core.model.dto.dh.request.CreateConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateConditionRequest;
import com.aboff.core.model.dto.dh.response.ConditionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.repository.dh.ExpansionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConditionService.
 * Tests all CRUD operations, pagination, soft deletion, restore functionality,
 * expand parameter, and bulk operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConditionServiceTest {

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private ExpansionRepository expansionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ConditionService conditionService;

    private Expansion expansion;

    @BeforeEach
    void setUp() {
        expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();

        User admin = User.builder().id(1L).username("admin").email("admin@test.com").role(Role.ADMIN).build();
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(admin));
    }

    // ==================== GET ALL CONDITIONS TESTS ====================

    @Test
    void getAllConditions_WithoutFilters_ReturnsPagedConditions() {
        Condition c1 = createTestCondition(1L, "Restrained", expansion);
        Condition c2 = createTestCondition(2L, "Vulnerable", expansion);

        Page<Condition> conditionPage = new PageImpl<>(List.of(c1, c2));
        when(conditionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(conditionPage);

        PagedResponse<ConditionResponse> result = conditionService.getAllConditions(0, 20, false, null, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Restrained");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Vulnerable");
    }

    @Test
    void getAllConditions_WithIncludeDeleted_UsesFindAllWithFilters() {
        Condition c = createTestCondition(1L, "Restrained", expansion);
        Page<Condition> conditionPage = new PageImpl<>(List.of(c));
        when(conditionRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(conditionPage);

        PagedResponse<ConditionResponse> result = conditionService.getAllConditions(0, 20, true, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(conditionRepository).findAllWithFilters(isNull(), isNull(), any(Pageable.class));
        verify(conditionRepository, never()).findByDeletedAtIsNullAndFilters(any(), any(), any());
    }

    @Test
    void getAllConditions_WithLargePage_LimitsTo100() {
        Page<Condition> conditionPage = new PageImpl<>(List.of());
        when(conditionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(conditionPage);

        conditionService.getAllConditions(0, 500, false, null, null, null);

        verify(conditionRepository).findByDeletedAtIsNullAndFilters(
                isNull(), isNull(), argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    void getAllConditions_WithExpandExpansion_ExpandsExpansion() {
        Condition c = createTestCondition(1L, "Restrained", expansion);
        Page<Condition> conditionPage = new PageImpl<>(List.of(c));
        when(conditionRepository.findByDeletedAtIsNullAndFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(conditionPage);

        PagedResponse<ConditionResponse> result = conditionService.getAllConditions(0, 20, false, null, null, "expansion");

        assertThat(result.getContent().get(0).getExpansion()).isNotNull();
        assertThat(result.getContent().get(0).getExpansion().getName()).isEqualTo("Core Rulebook");
    }

    // ==================== GET CONDITION BY ID TESTS ====================

    @Test
    void getConditionById_ValidId_ReturnsCondition() {
        Condition condition = createTestCondition(1L, "Restrained", expansion);
        when(conditionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(condition));

        ConditionResponse result = conditionService.getConditionById(1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Restrained");
    }

    @Test
    void getConditionById_NotFound_ThrowsEntityNotFoundException() {
        when(conditionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conditionService.getConditionById(999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Condition not found with id: 999");
    }

    // ==================== CREATE CONDITION TESTS ====================

    @Test
    void createCondition_ValidRequest_CreatesAndReturnsCondition() {
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Restrained")
                .description("You cannot move or evade.")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        Condition savedCondition = createTestCondition(1L, "Restrained", expansion);

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(conditionRepository.save(any(Condition.class))).thenReturn(savedCondition);

        ConditionResponse result = conditionService.createCondition(request, authentication);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Restrained");
        assertThat(result.getIsOfficial()).isTrue();
        verify(conditionRepository).save(any(Condition.class));
    }

    @Test
    void createCondition_RespectsIsOfficialFromRequest_NotHardcodedFalse() {
        // Regression guard: bulk-importing official rulebook content requires the create path
        // to honor request.getIsOfficial(), not hardcode it to false (as Adversary's does).
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Vulnerable")
                .expansionId(1L)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(conditionRepository.save(any(Condition.class))).thenAnswer(inv -> inv.getArgument(0));

        conditionService.createCondition(request, authentication);

        verify(conditionRepository).save(argThat(c -> Boolean.TRUE.equals(c.getIsOfficial())));
    }

    @Test
    void createCondition_ExpansionNotFound_ThrowsEntityNotFoundException() {
        CreateConditionRequest request = CreateConditionRequest.builder()
                .name("Restrained")
                .expansionId(999L)
                .isOfficial(true)
                .build();

        when(expansionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conditionService.createCondition(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Expansion not found with id: 999");

        verify(conditionRepository, never()).save(any());
    }

    // ==================== CREATE CONDITIONS BULK TESTS ====================

    @Test
    void createConditionsBulk_ValidRequests_CreatesAllSixConditions() {
        List<CreateConditionRequest> requests = List.of(
                CreateConditionRequest.builder().name("Restrained").expansionId(1L).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Vulnerable").expansionId(1L).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Drained").expansionId(1L).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Hexed").expansionId(1L).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Chained").expansionId(1L).isOfficial(true).build(),
                CreateConditionRequest.builder().name("Ignited").expansionId(1L).isOfficial(true).build());

        when(expansionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(expansion));
        when(conditionRepository.save(any(Condition.class))).thenAnswer(inv -> {
            Condition c = inv.getArgument(0);
            c.setId((long) (Math.random() * 1000));
            return c;
        });

        List<ConditionResponse> results = conditionService.createConditionsBulk(requests, authentication);

        assertThat(results).hasSize(6);
        assertThat(results.stream().map(ConditionResponse::getName))
                .containsExactly("Restrained", "Vulnerable", "Drained", "Hexed", "Chained", "Ignited");
        verify(conditionRepository, times(6)).save(any(Condition.class));
    }

    // ==================== UPDATE CONDITION TESTS ====================

    @Test
    void updateCondition_ValidRequest_UpdatesAndReturnsCondition() {
        Condition existingCondition = createTestCondition(1L, "Old Name", expansion);

        UpdateConditionRequest request = UpdateConditionRequest.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        when(conditionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existingCondition));
        when(conditionRepository.save(any(Condition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConditionResponse result = conditionService.updateCondition(1L, request, authentication);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(conditionRepository).save(any(Condition.class));
    }

    @Test
    void updateCondition_NotFound_ThrowsEntityNotFoundException() {
        UpdateConditionRequest request = UpdateConditionRequest.builder().name("Updated Name").build();

        when(conditionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conditionService.updateCondition(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Condition not found with id: 999");

        verify(conditionRepository, never()).save(any());
    }

    // ==================== DELETE CONDITION TESTS ====================

    @Test
    void deleteCondition_ValidId_SoftDeletesCondition() {
        Condition condition = createTestCondition(1L, "To Delete", expansion);
        when(conditionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(condition));

        conditionService.deleteCondition(1L, authentication);

        verify(conditionRepository).save(argThat(c -> c.getDeletedAt() != null));
    }

    @Test
    void deleteCondition_NotFound_ThrowsEntityNotFoundException() {
        when(conditionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conditionService.deleteCondition(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Condition not found with id: 999");

        verify(conditionRepository, never()).save(any());
    }

    // ==================== RESTORE CONDITION TESTS ====================

    @Test
    void restoreCondition_DeletedCondition_RestoresSuccessfully() {
        Condition deletedCondition = createTestCondition(1L, "Deleted Condition", expansion);
        deletedCondition.setDeletedAt(LocalDateTime.now());

        when(conditionRepository.findById(1L)).thenReturn(Optional.of(deletedCondition));
        when(conditionRepository.save(any(Condition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConditionResponse result = conditionService.restoreCondition(1L, authentication);

        assertThat(result).isNotNull();
        verify(conditionRepository).save(argThat(c -> c.getDeletedAt() == null));
    }

    @Test
    void restoreCondition_NotDeleted_ThrowsIllegalStateException() {
        Condition activeCondition = createTestCondition(1L, "Active Condition", expansion);
        when(conditionRepository.findById(1L)).thenReturn(Optional.of(activeCondition));

        assertThatThrownBy(() -> conditionService.restoreCondition(1L, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Condition with id 1 is not deleted");

        verify(conditionRepository, never()).save(any());
    }

    // ==================== HELPER METHODS ====================

    private Condition createTestCondition(Long id, String name, Expansion expansion) {
        return Condition.builder()
                .id(id)
                .name(name)
                .description("Test description")
                .expansion(expansion)
                .isOfficial(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
