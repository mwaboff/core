package com.aboff.core.service;

import com.aboff.core.model.entity.LoginAttempt;
import com.aboff.core.repository.LoginAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    // ==================== RECORD ATTEMPT TESTS ====================

    @Test
    void recordAttempt_ValidAttempt_SavesAttempt() {
        // Arrange
        LoginAttempt attempt = LoginAttempt.builder()
                .userId(1L)
                .usernameAttempted("testuser")
                .success(true)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        // Act
        loginAttemptService.recordAttempt(attempt);

        // Assert
        verify(loginAttemptRepository).save(attempt);
    }

    @Test
    void recordAttempt_FailedAttempt_SavesWithFailureReason() {
        // Arrange
        LoginAttempt attempt = LoginAttempt.builder()
                .userId(1L)
                .usernameAttempted("testuser")
                .success(false)
                .failureReason("INVALID_CREDENTIALS")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        // Act
        loginAttemptService.recordAttempt(attempt);

        // Assert
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        LoginAttempt captured = captor.getValue();
        assertThat(captured.getSuccess()).isFalse();
        assertThat(captured.getFailureReason()).isEqualTo("INVALID_CREDENTIALS");
    }

    // ==================== GET RECENT FAILED ATTEMPTS TESTS ====================

    @Test
    void getRecentFailedAttempts_ReturnsFilteredAttempts() {
        // Arrange
        String username = "testuser";
        int minutes = 15;

        List<LoginAttempt> expectedAttempts = List.of(
                LoginAttempt.builder()
                        .usernameAttempted(username)
                        .success(false)
                        .attemptedAt(LocalDateTime.now().minusMinutes(5))
                        .build(),
                LoginAttempt.builder()
                        .usernameAttempted(username)
                        .success(false)
                        .attemptedAt(LocalDateTime.now().minusMinutes(10))
                        .build()
        );

        when(loginAttemptRepository.findRecentFailedAttempts(eq(username), any(LocalDateTime.class)))
                .thenReturn(expectedAttempts);

        // Act
        List<LoginAttempt> result = loginAttemptService.getRecentFailedAttempts(username, minutes);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedAttempts);
        verify(loginAttemptRepository).findRecentFailedAttempts(eq(username), any(LocalDateTime.class));
    }

    @Test
    void getRecentFailedAttempts_NoAttempts_ReturnsEmptyList() {
        // Arrange
        String username = "testuser";
        int minutes = 15;

        when(loginAttemptRepository.findRecentFailedAttempts(eq(username), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<LoginAttempt> result = loginAttemptService.getRecentFailedAttempts(username, minutes);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== GET ATTEMPTS FOR USER TESTS ====================

    @Test
    void getAttemptsForUser_ReturnsUserAttempts() {
        // Arrange
        Long userId = 1L;

        List<LoginAttempt> expectedAttempts = List.of(
                LoginAttempt.builder()
                        .userId(userId)
                        .usernameAttempted("testuser")
                        .success(true)
                        .attemptedAt(LocalDateTime.now())
                        .build(),
                LoginAttempt.builder()
                        .userId(userId)
                        .usernameAttempted("testuser")
                        .success(false)
                        .attemptedAt(LocalDateTime.now().minusHours(1))
                        .build()
        );

        when(loginAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId))
                .thenReturn(expectedAttempts);

        // Act
        List<LoginAttempt> result = loginAttemptService.getAttemptsForUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedAttempts);
    }

    // ==================== CLEANUP OLD ATTEMPTS TESTS ====================

    @Test
    void cleanupOldAttempts_DeletesOldAttempts() {
        // Arrange
        int deletedCount = 42;
        when(loginAttemptRepository.deleteOldAttempts(any(LocalDateTime.class)))
                .thenReturn(deletedCount);

        // Act
        loginAttemptService.cleanupOldAttempts();

        // Assert
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(loginAttemptRepository).deleteOldAttempts(captor.capture());

        LocalDateTime cutoffDate = captor.getValue();
        assertThat(cutoffDate).isBefore(LocalDateTime.now());
        assertThat(cutoffDate).isAfter(LocalDateTime.now().minusDays(91));
    }

    @Test
    void cleanupOldAttempts_NoOldAttempts_ReturnsZero() {
        // Arrange
        when(loginAttemptRepository.deleteOldAttempts(any(LocalDateTime.class)))
                .thenReturn(0);

        // Act
        loginAttemptService.cleanupOldAttempts();

        // Assert
        verify(loginAttemptRepository).deleteOldAttempts(any(LocalDateTime.class));
    }
}
