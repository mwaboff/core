package com.aboff.core.service.dh;

import com.aboff.core.repository.dh.CampaignInviteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up expired and old used campaign invites.
 * Runs scheduled jobs to maintain database size.
 */
@Service
@Slf4j
public class CampaignInviteCleanupService {

    private final CampaignInviteRepository campaignInviteRepository;

    public CampaignInviteCleanupService(CampaignInviteRepository campaignInviteRepository) {
        this.campaignInviteRepository = campaignInviteRepository;
    }

    /**
     * Scheduled cleanup of expired and old used campaign invites (runs daily at 3:15 AM).
     * <p>
     * Offset from token cleanup at 3:00 AM to avoid concurrent load.
     * </p>
     * <ul>
     *   <li>Deletes expired unused invites</li>
     *   <li>Deletes used invites older than 7 days</li>
     * </ul>
     */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void cleanupInvites() {
        LocalDateTime now = LocalDateTime.now();

        int expiredCount = campaignInviteRepository.deleteExpiredInvites(now);
        log.info("Cleaned up {} expired campaign invites", expiredCount);

        LocalDateTime usedCutoff = now.minusDays(7);
        int usedCount = campaignInviteRepository.deleteOldUsedInvites(usedCutoff);
        log.info("Cleaned up {} old used campaign invites", usedCount);
    }
}
