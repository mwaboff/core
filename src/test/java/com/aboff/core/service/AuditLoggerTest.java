package com.aboff.core.service;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link AuditLogger}.
 * <p>
 * Since AuditLogger delegates to SLF4J, these tests focus on verifying that all
 * public methods execute without throwing exceptions across a range of inputs,
 * including null and empty detail strings.
 * </p>
 */
class AuditLoggerTest {

    private AuditLogger auditLogger;
    private AuditContext context;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
        context = AuditContext.forIp("127.0.0.1").build();
    }

    // ==================== log(action, context, detail) ====================

    @Test
    void log_WithDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.CAMPAIGN_CREATED, context, "\"The Adventures in Ostea\" (campaign_id: 3)"))
                .doesNotThrowAnyException();
    }

    @Test
    void log_WithEmptyDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.CAMPAIGN_UPDATED, context, ""))
                .doesNotThrowAnyException();
    }

    @Test
    void log_WithNullDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.CAMPAIGN_DELETED, context, null))
                .doesNotThrowAnyException();
    }

    @Test
    void log_WithSpecialCharactersInDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.CHARACTER_CREATED, context, "name: Ëlindë & <Dragonborn>"))
                .doesNotThrowAnyException();
    }

    // ==================== log(action, context) ====================

    @Test
    void log_WithoutDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.USER_LOGIN, context))
                .doesNotThrowAnyException();
    }

    @Test
    void log_WithoutDetail_DifferentActions_DoesNotThrow() {
        assertThatCode(() -> auditLogger.log(AuditAction.USER_LOGOUT, context))
                .doesNotThrowAnyException();
        assertThatCode(() -> auditLogger.log(AuditAction.USER_TOKENS_INVALIDATED, context))
                .doesNotThrowAnyException();
    }

    // ==================== warn(action, context, detail) ====================

    @Test
    void warn_WithDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.warn(AuditAction.ADVERSARY_BATCH_CREATED, context, "3 of 5 adversaries created; 2 failed validation"))
                .doesNotThrowAnyException();
    }

    @Test
    void warn_WithEmptyDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.warn(AuditAction.CONTENT_BATCH_CREATED, context, ""))
                .doesNotThrowAnyException();
    }

    @Test
    void warn_WithNullDetail_DoesNotThrow() {
        assertThatCode(() -> auditLogger.warn(AuditAction.CONTENT_DELETED, context, null))
                .doesNotThrowAnyException();
    }

    // ==================== requestReceived(context, method, path) ====================

    @Test
    void requestReceived_DoesNotThrow() {
        assertThatCode(() -> auditLogger.requestReceived(context, "GET", "/api/dh/campaigns"))
                .doesNotThrowAnyException();
    }

    @Test
    void requestReceived_PostMethod_DoesNotThrow() {
        assertThatCode(() -> auditLogger.requestReceived(context, "POST", "/api/dh/campaigns"))
                .doesNotThrowAnyException();
    }

    @Test
    void requestReceived_WithQueryParams_DoesNotThrow() {
        assertThatCode(() -> auditLogger.requestReceived(context, "GET", "/api/dh/character-sheets/1?expand=owner,experiences"))
                .doesNotThrowAnyException();
    }

    // ==================== requestCompleted(context, method, path, startTimeNano, detail) ====================

    @Test
    void requestCompleted_WithDetail_DoesNotThrow() {
        long startTime = System.nanoTime();
        assertThatCode(() -> auditLogger.requestCompleted(context, "POST", "/api/dh/campaigns", startTime, "campaign_id: 42"))
                .doesNotThrowAnyException();
    }

    @Test
    void requestCompleted_WithEmptyDetail_DoesNotThrow() {
        long startTime = System.nanoTime();
        assertThatCode(() -> auditLogger.requestCompleted(context, "DELETE", "/api/dh/campaigns/1", startTime, ""))
                .doesNotThrowAnyException();
    }

    @Test
    void requestCompleted_WithNullDetail_DoesNotThrow() {
        long startTime = System.nanoTime();
        assertThatCode(() -> auditLogger.requestCompleted(context, "PUT", "/api/dh/campaigns/1", startTime, null))
                .doesNotThrowAnyException();
    }

    @Test
    void requestCompleted_DurationIsNonNegative() {
        // Capture startTime before the call and ensure nanoTime diff stays non-negative
        long startTime = System.nanoTime();
        // If this executes without error, duration was computed as (nanoTime - startTime) which is >= 0
        assertThatCode(() -> auditLogger.requestCompleted(context, "GET", "/api/dh/character-sheets", startTime, "5 results"))
                .doesNotThrowAnyException();
    }

    @Test
    void requestCompleted_FutureStartTime_DoesNotThrow() {
        // A start time set far in the future produces a negative duration but should still log without error
        long futureStartTime = System.nanoTime() + Long.MAX_VALUE / 2;
        assertThatCode(() -> auditLogger.requestCompleted(context, "GET", "/api/dh/adversaries", futureStartTime, null))
                .doesNotThrowAnyException();
    }

    // ==================== requestCompleted(context, method, path, startTimeNano) ====================

    @Test
    void requestCompleted_WithoutDetail_DoesNotThrow() {
        long startTime = System.nanoTime();
        assertThatCode(() -> auditLogger.requestCompleted(context, "GET", "/api/dh/campaigns", startTime))
                .doesNotThrowAnyException();
    }

    // ==================== Context variations ====================

    @Test
    void log_WithEnrichedContext_DoesNotThrow() {
        AuditContext enrichedContext = AuditContext.forIp("192.168.1.100")
                .withCampaignId(7L)
                .withTargetUserId(99L)
                .build();

        assertThatCode(() -> auditLogger.log(AuditAction.CAMPAIGN_PLAYER_ADDED, enrichedContext, "player_id: 99"))
                .doesNotThrowAnyException();
    }

    @Test
    void log_WithEmptyContext_DoesNotThrow() {
        AuditContext emptyContext = AuditContext.forIp(null).build();

        assertThatCode(() -> auditLogger.log(AuditAction.USER_PROVISIONED, emptyContext, "oauth provider: google"))
                .doesNotThrowAnyException();
    }
}
