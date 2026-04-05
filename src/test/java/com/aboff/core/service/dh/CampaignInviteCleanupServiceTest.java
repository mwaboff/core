package com.aboff.core.service.dh;

import com.aboff.core.repository.dh.CampaignInviteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CampaignInviteCleanupService}.
 * Tests scheduled cleanup of expired and old used campaign invites.
 */
@ExtendWith(MockitoExtension.class)
class CampaignInviteCleanupServiceTest {

    @Mock
    private CampaignInviteRepository campaignInviteRepository;

    @InjectMocks
    private CampaignInviteCleanupService campaignInviteCleanupService;

    /**
     * Verifies that cleanupInvites calls deleteExpiredInvites with the current time
     * and deleteOldUsedInvites with a cutoff 7 days in the past.
     */
    @Test
    void cleanupInvites_DeletesExpiredAndOldUsedInvites() {
        LocalDateTime beforeTest = LocalDateTime.now();

        when(campaignInviteRepository.deleteExpiredInvites(any(LocalDateTime.class))).thenReturn(5);
        when(campaignInviteRepository.deleteOldUsedInvites(any(LocalDateTime.class))).thenReturn(3);

        campaignInviteCleanupService.cleanupInvites();

        LocalDateTime afterTest = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> expiredCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(campaignInviteRepository).deleteExpiredInvites(expiredCaptor.capture());
        LocalDateTime expiredArg = expiredCaptor.getValue();
        assertThat(expiredArg).isAfterOrEqualTo(beforeTest);
        assertThat(expiredArg).isBeforeOrEqualTo(afterTest);

        ArgumentCaptor<LocalDateTime> usedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(campaignInviteRepository).deleteOldUsedInvites(usedCaptor.capture());
        LocalDateTime usedArg = usedCaptor.getValue();
        assertThat(usedArg).isAfterOrEqualTo(beforeTest.minusDays(7));
        assertThat(usedArg).isBeforeOrEqualTo(afterTest.minusDays(7));
    }

    /**
     * Verifies that cleanupInvites completes successfully when there are no invites to clean.
     */
    @Test
    void cleanupInvites_WithNoInvitesToClean_CompletesSuccessfully() {
        when(campaignInviteRepository.deleteExpiredInvites(any(LocalDateTime.class))).thenReturn(0);
        when(campaignInviteRepository.deleteOldUsedInvites(any(LocalDateTime.class))).thenReturn(0);

        campaignInviteCleanupService.cleanupInvites();

        verify(campaignInviteRepository).deleteExpiredInvites(any(LocalDateTime.class));
        verify(campaignInviteRepository).deleteOldUsedInvites(any(LocalDateTime.class));
    }
}
