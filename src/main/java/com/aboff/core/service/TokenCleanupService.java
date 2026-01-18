package com.aboff.core.service;

import com.aboff.core.repository.ActiveTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up expired and revoked tokens.
 * Runs scheduled jobs to maintain database size.
 */
@Service
public class TokenCleanupService {

    private final ActiveTokenRepository activeTokenRepository;

    public TokenCleanupService(ActiveTokenRepository activeTokenRepository) {
        this.activeTokenRepository = activeTokenRepository;
    }

    /**
     * Scheduled cleanup of expired and old revoked tokens (runs daily at 3 AM).
     * - Deletes expired tokens
     * - Deletes revoked tokens older than 30 days
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = activeTokenRepository.deleteExpiredAndOldRevokedTokens(cutoffDate);
        System.out.println("Cleaned up " + deletedCount + " expired/old revoked tokens");
    }
}
