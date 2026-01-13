package com.aboff.core.service;

import com.aboff.core.model.entity.LoginAttempt;
import com.aboff.core.repository.LoginAttemptRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptService(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    /**
     * Records a login attempt
     */
    @Transactional
    public void recordAttempt(LoginAttempt attempt) {
        loginAttemptRepository.save(attempt);
    }

    /**
     * Gets recent failed login attempts for a username within a time window
     */
    public List<LoginAttempt> getRecentFailedAttempts(String username, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.findRecentFailedAttempts(username, since);
    }

    /**
     * Gets all login attempts for a user, ordered by most recent first
     */
    public List<LoginAttempt> getAttemptsForUser(Long userId) {
        return loginAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
    }

    /**
     * Scheduled cleanup of old login attempts (runs daily at 2 AM)
     * Deletes attempts older than 90 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deletedCount = loginAttemptRepository.deleteOldAttempts(cutoffDate);
        System.out.println("Cleaned up " + deletedCount + " old login attempts");
    }
}
