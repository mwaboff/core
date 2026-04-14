package com.aboff.core.service;

import com.aboff.core.model.AuditContext;
import com.aboff.core.model.enums.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized audit logging service for structured, consistent log output.
 * <p>
 * Produces logs in the format:
 * {@code [user_id: 42; username: mwaboff; role: owner] Campaign created: "The Adventures in Ostea" (campaign_id: 3)}
 * </p>
 * <p>
 * Designed as the single integration point for future metrics emission (e.g., New Relic).
 * All services and controllers should use this instead of direct SLF4J calls for auditable actions.
 * </p>
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    /**
     * Logs a service-level audit event at INFO level.
     *
     * @param action  the audit action being performed
     * @param context the metadata context (user, entity IDs, etc.)
     * @param detail  additional detail about the action (e.g., entity name, IDs)
     */
    public void log(AuditAction action, AuditContext context, String detail) {
        log.info("{} {}: {}", context.format(), action.getLabel(), detail);
    }

    /**
     * Logs a service-level audit event at INFO level without additional detail.
     *
     * @param action  the audit action being performed
     * @param context the metadata context
     */
    public void log(AuditAction action, AuditContext context) {
        log.info("{} {}", context.format(), action.getLabel());
    }

    /**
     * Logs a warning-level audit event (e.g., partial batch failures).
     *
     * @param action  the audit action being performed
     * @param context the metadata context
     * @param detail  additional detail about the warning
     */
    public void warn(AuditAction action, AuditContext context, String detail) {
        log.warn("{} {}: {}", context.format(), action.getLabel(), detail);
    }

    /**
     * Logs that an HTTP request was received at the controller layer.
     *
     * @param context the metadata context (user info and/or IP)
     * @param method  the HTTP method (GET, POST, etc.)
     * @param path    the request path
     */
    public void requestReceived(AuditContext context, String method, String path) {
        log.debug("{} {} {} — request received", context.format(), method, path);
    }

    /**
     * Logs that an HTTP request completed at the controller layer, including duration.
     *
     * @param context       the metadata context
     * @param method        the HTTP method
     * @param path          the request path
     * @param startTimeNano the start time from {@link System#nanoTime()}
     * @param detail        optional detail about the result (e.g., created entity ID)
     */
    public void requestCompleted(AuditContext context, String method, String path, long startTimeNano, String detail) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNano);
        if (detail != null && !detail.isEmpty()) {
            log.debug("{} {} {} — completed ({}) in {}ms", context.format(), method, path, detail, durationMs);
        } else {
            log.debug("{} {} {} — completed in {}ms", context.format(), method, path, durationMs);
        }
    }

    /**
     * Logs that an HTTP request completed at the controller layer, including duration.
     *
     * @param context       the metadata context
     * @param method        the HTTP method
     * @param path          the request path
     * @param startTimeNano the start time from {@link System#nanoTime()}
     */
    public void requestCompleted(AuditContext context, String method, String path, long startTimeNano) {
        requestCompleted(context, method, path, startTimeNano, null);
    }
}
