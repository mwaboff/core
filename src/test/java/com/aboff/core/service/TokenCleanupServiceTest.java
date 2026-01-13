package com.aboff.core.service;

import com.aboff.core.repository.ActiveTokenRepository;
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

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private ActiveTokenRepository activeTokenRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    // ==================== CLEANUP EXPIRED TOKENS TESTS ====================

    @Test
    void cleanupExpiredTokens_DeletesExpiredAndOldRevokedTokens() {
        // Arrange
        int deletedCount = 25;
        when(activeTokenRepository.deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class)))
                .thenReturn(deletedCount);

        // Act
        tokenCleanupService.cleanupExpiredTokens();

        // Assert
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(activeTokenRepository).deleteExpiredAndOldRevokedTokens(captor.capture());

        LocalDateTime cutoffDate = captor.getValue();
        // Cutoff should be 30 days ago
        assertThat(cutoffDate).isBefore(LocalDateTime.now());
        assertThat(cutoffDate).isAfter(LocalDateTime.now().minusDays(31));
        assertThat(cutoffDate).isBefore(LocalDateTime.now().minusDays(29));
    }

    @Test
    void cleanupExpiredTokens_NoTokensToDelete_ReturnsZero() {
        // Arrange
        when(activeTokenRepository.deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class)))
                .thenReturn(0);

        // Act
        tokenCleanupService.cleanupExpiredTokens();

        // Assert
        verify(activeTokenRepository).deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredTokens_LargeNumberOfTokens_HandlesSuccessfully() {
        // Arrange
        int deletedCount = 10000;
        when(activeTokenRepository.deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class)))
                .thenReturn(deletedCount);

        // Act
        tokenCleanupService.cleanupExpiredTokens();

        // Assert
        verify(activeTokenRepository).deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class));
    }
}
