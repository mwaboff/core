package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.request.UpdateCharacterSheetConditionRequest;
import com.aboff.core.model.dto.dh.response.CharacterSheetConditionResponse;
import com.aboff.core.model.dto.response.PagedResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.CharacterSheet;
import com.aboff.core.model.entity.dh.CharacterSheetCondition;
import com.aboff.core.model.entity.dh.Condition;
import com.aboff.core.model.entity.dh.Expansion;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CharacterSheetConditionRepository;
import com.aboff.core.repository.dh.CharacterSheetRepository;
import com.aboff.core.repository.dh.ConditionRepository;
import com.aboff.core.security.CustomUserDetails;
import com.aboff.core.service.AuditLogger;
import com.aboff.core.service.RoleHierarchyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CharacterSheetConditionService.
 * Tests CRUD operations for a character's per-instance conditions, including the magnitude
 * round-trip and owner/moderator access control.
 */
@ExtendWith(MockitoExtension.class)
class CharacterSheetConditionServiceTest {

    @Mock
    private CharacterSheetConditionRepository characterSheetConditionRepository;

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CharacterSheetConditionService characterSheetConditionService;

    private Expansion expansion;
    private User owner;
    private CharacterSheet sheet;
    private Condition condition;

    private void setUpFixtures() {
        expansion = Expansion.builder().id(1L).name("Core Rulebook").isPublished(true).build();
        owner = User.builder().id(1L).username("player1").role(Role.USER).build();
        sheet = CharacterSheet.builder().id(1L).name("Aragorn").owner(owner).build();
        condition = Condition.builder().id(1L).name("Restrained").expansion(expansion).isOfficial(true).build();
    }

    // ==================== GET TESTS ====================

    @Test
    void getConditionsForCharacterSheet_ReturnsPagedInstances() {
        setUpFixtures();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(2).build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(characterSheetConditionRepository.findByCharacterSheetId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(instance)));

        PagedResponse<CharacterSheetConditionResponse> result =
                characterSheetConditionService.getConditionsForCharacterSheet(0, 20, 1L, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMagnitude()).isEqualTo(2);
        assertThat(result.getContent().get(0).getCharacterSheetId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getConditionId()).isEqualTo(1L);
    }

    @Test
    void getConditionsForCharacterSheet_CharacterSheetNotFound_ThrowsEntityNotFoundException() {
        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetConditionService.getConditionsForCharacterSheet(0, 20, 999L, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CharacterSheet not found with id: 999");
    }

    @Test
    void getConditionInstanceById_ValidId_ReturnsInstance() {
        setUpFixtures();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(3).build();

        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));

        CharacterSheetConditionResponse result = characterSheetConditionService.getConditionInstanceById(1L, null);

        assertThat(result.getMagnitude()).isEqualTo(3);
    }

    @Test
    void getConditionInstanceById_WithExpandCondition_ExpandsCondition() {
        setUpFixtures();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));

        CharacterSheetConditionResponse result = characterSheetConditionService.getConditionInstanceById(1L, "condition");

        assertThat(result.getCondition()).isNotNull();
        assertThat(result.getCondition().getName()).isEqualTo("Restrained");
    }

    // ==================== CREATE TESTS — the magnitude round-trip ====================

    @Test
    void createCharacterSheetCondition_ValidRequest_RoundTripsMagnitude() {
        setUpFixtures();
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(1L)
                .conditionId(1L)
                .magnitude(4)
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(conditionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(condition));
        when(characterSheetConditionRepository.save(any(CharacterSheetCondition.class)))
                .thenAnswer(inv -> {
                    CharacterSheetCondition instance = inv.getArgument(0);
                    instance.setId(10L);
                    return instance;
                });

        CharacterSheetConditionResponse result =
                characterSheetConditionService.createCharacterSheetCondition(request, authentication);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCharacterSheetId()).isEqualTo(1L);
        assertThat(result.getConditionId()).isEqualTo(1L);
        assertThat(result.getMagnitude()).isEqualTo(4);
    }

    @Test
    void createCharacterSheetCondition_WithNullMagnitude_AllowsNonStackingCondition() {
        setUpFixtures();
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(1L)
                .conditionId(1L)
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(conditionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(condition));
        when(characterSheetConditionRepository.save(any(CharacterSheetCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CharacterSheetConditionResponse result =
                characterSheetConditionService.createCharacterSheetCondition(request, authentication);

        assertThat(result.getMagnitude()).isNull();
    }

    @Test
    void createCharacterSheetCondition_CharacterSheetNotFound_ThrowsEntityNotFoundException() {
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(999L)
                .conditionId(1L)
                .build();

        when(characterSheetRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetConditionService.createCharacterSheetCondition(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CharacterSheet not found with id: 999");
    }

    @Test
    void createCharacterSheetCondition_ConditionNotFound_ThrowsEntityNotFoundException() {
        setUpFixtures();
        CreateCharacterSheetConditionRequest request = CreateCharacterSheetConditionRequest.builder()
                .characterSheetId(1L)
                .conditionId(999L)
                .build();

        when(characterSheetRepository.findActiveById(1L)).thenReturn(Optional.of(sheet));
        when(conditionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetConditionService.createCharacterSheetCondition(request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Condition not found with id: 999");
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void updateCharacterSheetCondition_AsOwner_UpdatesMagnitude() {
        setUpFixtures();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(5)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(characterSheetConditionRepository.save(any(CharacterSheetCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CharacterSheetConditionResponse result =
                characterSheetConditionService.updateCharacterSheetCondition(1L, request, authentication);

        assertThat(result.getMagnitude()).isEqualTo(5);
    }

    @Test
    void updateCharacterSheetCondition_AsModerator_UpdatesMagnitude() {
        setUpFixtures();
        User moderator = User.builder().id(3L).username("moderator1").role(Role.MODERATOR).build();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(6)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(moderator);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));
        when(characterSheetConditionRepository.save(any(CharacterSheetCondition.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(roleHierarchyService.hasModeratorOrHigher(any(CustomUserDetails.class))).thenReturn(true);

        CharacterSheetConditionResponse result =
                characterSheetConditionService.updateCharacterSheetCondition(1L, request, authentication);

        assertThat(result.getMagnitude()).isEqualTo(6);
    }

    @Test
    void updateCharacterSheetCondition_AsUnauthorizedUser_ThrowsInsufficientPermissionsException() {
        setUpFixtures();
        User otherUser = User.builder().id(4L).username("player2").role(Role.USER).build();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder()
                .magnitude(6)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> characterSheetConditionService.updateCharacterSheetCondition(1L, request, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to update this condition instance");

        verify(characterSheetConditionRepository, never()).save(any());
    }

    @Test
    void updateCharacterSheetCondition_NotFound_ThrowsEntityNotFoundException() {
        UpdateCharacterSheetConditionRequest request = UpdateCharacterSheetConditionRequest.builder().magnitude(1).build();

        when(characterSheetConditionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetConditionService.updateCharacterSheetCondition(999L, request, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CharacterSheetCondition not found with id: 999");
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteCharacterSheetCondition_AsOwner_RemovesInstance() {
        setUpFixtures();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));

        characterSheetConditionService.deleteCharacterSheetCondition(1L, authentication);

        verify(characterSheetConditionRepository).delete(instance);
    }

    @Test
    void deleteCharacterSheetCondition_AsUnauthorizedUser_ThrowsInsufficientPermissionsException() {
        setUpFixtures();
        User otherUser = User.builder().id(4L).username("player2").role(Role.USER).build();
        CharacterSheetCondition instance = CharacterSheetCondition.builder()
                .id(1L).characterSheet(sheet).condition(condition).magnitude(1).build();

        CustomUserDetails userDetails = new CustomUserDetails(otherUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(characterSheetConditionRepository.findById(1L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> characterSheetConditionService.deleteCharacterSheetCondition(1L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("You do not have permission to remove this condition instance");

        verify(characterSheetConditionRepository, never()).delete(any());
    }

    @Test
    void deleteCharacterSheetCondition_NotFound_ThrowsEntityNotFoundException() {
        when(characterSheetConditionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterSheetConditionService.deleteCharacterSheetCondition(999L, authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CharacterSheetCondition not found with id: 999");
    }
}
