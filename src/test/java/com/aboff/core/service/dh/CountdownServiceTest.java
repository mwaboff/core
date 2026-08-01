package com.aboff.core.service.dh;

import com.aboff.core.exception.InsufficientPermissionsException;
import com.aboff.core.model.dto.dh.request.CreateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownRequest;
import com.aboff.core.model.dto.dh.request.UpdateCountdownValueRequest;
import com.aboff.core.model.dto.dh.response.CountdownResponse;
import com.aboff.core.model.entity.User;
import com.aboff.core.model.entity.dh.Campaign;
import com.aboff.core.model.entity.dh.Countdown;
import com.aboff.core.model.enums.CountdownLoop;
import com.aboff.core.model.enums.CountdownType;
import com.aboff.core.model.enums.Role;
import com.aboff.core.repository.dh.CampaignRepository;
import com.aboff.core.repository.dh.CountdownRepository;
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
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CountdownService.
 * <p>
 * Concentrates on the logic this service actually owns: loop behaviour when a countdown
 * reaches 0, clamping, display ordering, and delegation of the GM access check to
 * CampaignService.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CountdownServiceTest {

    @Mock
    private CountdownRepository countdownRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CountdownService countdownService;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        User creator = User.builder().id(1L).username("gm").role(Role.USER).build();
        campaign = Campaign.builder()
                .id(10L)
                .name("The Hollow Road")
                .creator(creator)
                .gameMasters(new HashSet<>())
                .build();
        campaign.getGameMasters().add(creator);

        when(campaignRepository.findActiveById(10L)).thenReturn(Optional.of(campaign));
        when(countdownRepository.save(any(Countdown.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Countdown countdown(int starting, int current, CountdownLoop loop) {
        return Countdown.builder()
                .id(100L)
                .campaign(campaign)
                .name("The ritual completes")
                .type(CountdownType.CONSEQUENCE)
                .loopBehavior(loop)
                .startingValue(starting)
                .currentValue(current)
                .displayOrder(0)
                .build();
    }

    private UpdateCountdownValueRequest valueRequest(int value) {
        return UpdateCountdownValueRequest.builder().currentValue(value).build();
    }

    // ==================== CREATE ====================

    @Test
    void createCountdown_SetsCurrentValueToStartingValue() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).build(),
                authentication);

        assertThat(response.getCurrentValue()).isEqualTo(6);
    }

    @Test
    void createCountdown_DefaultsLoopBehaviorToNone() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).build(),
                authentication);

        assertThat(response.getLoopBehavior()).isEqualTo(CountdownLoop.NONE);
    }

    @Test
    void createCountdown_AppendsAfterTheHighestDisplayOrder() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(4);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).build(),
                authentication);

        assertThat(response.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void createCountdown_FirstInCampaign_GetsDisplayOrderZero() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).build(),
                authentication);

        assertThat(response.getDisplayOrder()).isZero();
    }

    @Test
    void createCountdown_UnknownCampaign_Throws() {
        when(campaignRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(99L).name("X").type(CountdownType.STANDARD).startingValue(4).build(),
                authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createCountdown_WithoutGameMasterAccess_Throws() {
        doThrow(new InsufficientPermissionsException("nope"))
                .when(campaignService).validateGameMasterAccess(any(), any(), anyString());

        assertThatThrownBy(() -> countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("X").type(CountdownType.STANDARD).startingValue(4).build(),
                authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(countdownRepository, never()).save(any());
    }

    @Test
    void createCountdown_NullNote_StaysNull() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).note(null).build(),
                authentication);

        assertThat(response.getNote()).isNull();
    }

    @Test
    void createCountdown_SanitizesNote() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        CountdownResponse response = countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).note("<script>alert('x')</script>The gate opens").build(),
                authentication);

        assertThat(response.getNote()).doesNotContain("<script>");
    }

    // ==================== TICK / LOOP ====================

    @Test
    void updateCountdownValue_AboveZero_LeavesValueAsSet() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 8, CountdownLoop.NONE)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(5), authentication);

        assertThat(response.getCurrentValue()).isEqualTo(5);
    }

    @Test
    void updateCountdownValue_ClampsAboveStartingValue() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 8, CountdownLoop.NONE)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(12), authentication);

        assertThat(response.getCurrentValue()).isEqualTo(8);
    }

    @Test
    void updateCountdownValue_ReachesZeroWithoutLoop_RestsAtZero() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 1, CountdownLoop.NONE)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getCurrentValue()).isZero();
    }

    @Test
    void updateCountdownValue_ReachesZeroWithLoop_ResetsToStartingValue() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 1, CountdownLoop.LOOP)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getCurrentValue()).isEqualTo(8);
    }

    @Test
    void updateCountdownValue_ReachesZeroWithLoop_LeavesStartingValueUnchanged() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 1, CountdownLoop.LOOP)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getStartingValue()).isEqualTo(8);
    }

    @Test
    void updateCountdownValue_IncreasingLoop_RaisesStartingValueByOne() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(4, 1, CountdownLoop.LOOP_INCREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getStartingValue()).isEqualTo(5);
    }

    @Test
    void updateCountdownValue_IncreasingLoop_ResetsToTheNewStartingValue() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(4, 1, CountdownLoop.LOOP_INCREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getCurrentValue()).isEqualTo(5);
    }

    @Test
    void updateCountdownValue_DecreasingLoop_LowersStartingValueByOne() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(4, 1, CountdownLoop.LOOP_DECREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getStartingValue()).isEqualTo(3);
    }

    @Test
    void updateCountdownValue_DecreasingLoopOnItsLastLoop_DecaysToZero() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(1, 1, CountdownLoop.LOOP_DECREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getStartingValue()).isZero();
    }

    @Test
    void updateCountdownValue_DecreasingLoopOnItsLastLoop_DoesNotResetAgain() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(1, 1, CountdownLoop.LOOP_DECREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getCurrentValue()).isZero();
    }

    @Test
    void updateCountdownValue_SpentDecreasingLoop_DoesNotDecayBelowZero() {
        when(countdownRepository.findById(100L))
                .thenReturn(Optional.of(countdown(0, 0, CountdownLoop.LOOP_DECREASING)));

        CountdownResponse response =
                countdownService.updateCountdownValue(100L, valueRequest(0), authentication);

        assertThat(response.getStartingValue()).isZero();
    }

    @Test
    void updateCountdownValue_UnknownCountdown_Throws() {
        when(countdownRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                countdownService.updateCountdownValue(404L, valueRequest(3), authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateCountdownValue_AuthorizesAgainstTheCountdownsOwnCampaign() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 8, CountdownLoop.NONE)));

        countdownService.updateCountdownValue(100L, valueRequest(4), authentication);

        verify(campaignService).validateGameMasterAccess(
                org.mockito.ArgumentMatchers.eq(campaign), any(), anyString());
    }

    @Test
    void updateCountdownValue_EndedCampaign_Throws() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(8, 8, CountdownLoop.NONE)));
        doThrow(new IllegalStateException("Cannot update countdowns for an ended campaign"))
                .when(campaignService).validateNotEnded(any(), anyString());

        assertThatThrownBy(() ->
                countdownService.updateCountdownValue(100L, valueRequest(4), authentication))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==================== UPDATE DEFINITION ====================

    @Test
    void updateCountdown_LoweringStartingValueClampsCurrentValue() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(10, 9, CountdownLoop.NONE)));

        CountdownResponse response = countdownService.updateCountdown(100L,
                UpdateCountdownRequest.builder()
                        .name("Renamed").type(CountdownType.PROGRESS)
                        .loopBehavior(CountdownLoop.NONE).startingValue(4).build(),
                authentication);

        assertThat(response.getCurrentValue()).isEqualTo(4);
    }

    @Test
    void updateCountdown_RaisingStartingValueLeavesCurrentValueAlone() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(4, 2, CountdownLoop.NONE)));

        CountdownResponse response = countdownService.updateCountdown(100L,
                UpdateCountdownRequest.builder()
                        .name("Renamed").type(CountdownType.PROGRESS)
                        .loopBehavior(CountdownLoop.NONE).startingValue(10).build(),
                authentication);

        assertThat(response.getCurrentValue()).isEqualTo(2);
    }

    @Test
    void updateCountdown_ChangesType() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(4, 2, CountdownLoop.NONE)));

        CountdownResponse response = countdownService.updateCountdown(100L,
                UpdateCountdownRequest.builder()
                        .name("Renamed").type(CountdownType.LONG_TERM)
                        .loopBehavior(CountdownLoop.LOOP).startingValue(4).build(),
                authentication);

        assertThat(response.getType()).isEqualTo(CountdownType.LONG_TERM);
    }

    // ==================== READ / DELETE ====================

    @Test
    void getCountdownsForCampaign_ReturnsRepositoryOrder() {
        when(countdownRepository.findByCampaignId(10L))
                .thenReturn(List.of(countdown(4, 4, CountdownLoop.NONE), countdown(6, 6, CountdownLoop.NONE)));

        List<CountdownResponse> responses =
                countdownService.getCountdownsForCampaign(10L, authentication);

        assertThat(responses).hasSize(2);
    }

    @Test
    void getCountdownsForCampaign_RequiresGameMasterAccess() {
        doThrow(new InsufficientPermissionsException("nope"))
                .when(campaignService).validateGameMasterAccess(any(), any(), anyString());

        assertThatThrownBy(() -> countdownService.getCountdownsForCampaign(10L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);
    }

    @Test
    void deleteCountdown_DeletesTheEntity() {
        Countdown existing = countdown(4, 4, CountdownLoop.NONE);
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(existing));

        countdownService.deleteCountdown(100L, authentication);

        verify(countdownRepository).delete(existing);
    }

    @Test
    void deleteCountdown_WithoutGameMasterAccess_DoesNotDelete() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(4, 4, CountdownLoop.NONE)));
        doThrow(new InsufficientPermissionsException("nope"))
                .when(campaignService).validateGameMasterAccess(any(), any(), anyString());

        assertThatThrownBy(() -> countdownService.deleteCountdown(100L, authentication))
                .isInstanceOf(InsufficientPermissionsException.class);

        verify(countdownRepository, never()).delete(any());
    }

    @Test
    void deleteCountdown_UnknownCountdown_Throws() {
        when(countdownRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countdownService.deleteCountdown(404L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCountdownById_ReturnsTheCountdown() {
        when(countdownRepository.findById(100L)).thenReturn(Optional.of(countdown(4, 3, CountdownLoop.NONE)));

        CountdownResponse response = countdownService.getCountdownById(100L, authentication);

        assertThat(response.getCurrentValue()).isEqualTo(3);
    }

    @Test
    void getCountdownById_UnknownCountdown_Throws() {
        when(countdownRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countdownService.getCountdownById(404L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createCountdown_AuditsTheCreation() {
        when(countdownRepository.findMaxDisplayOrderByCampaignId(10L)).thenReturn(-1);

        countdownService.createCountdown(
                CreateCountdownRequest.builder()
                        .campaignId(10L).name("Reinforcements").type(CountdownType.STANDARD)
                        .startingValue(6).build(),
                authentication);

        verify(auditLogger).log(any(), any(), anyString());
    }

    @Test
    void getCountdownsForCampaign_UnknownCampaign_Throws() {
        when(campaignRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countdownService.getCountdownsForCampaign(99L, authentication))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
