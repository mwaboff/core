package com.aboff.core.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Exception thrown when a user account is locked.
 */
@Getter
public class AccountLockedException extends RuntimeException {

    /**
     * The timestamp when the account lock expires.
     */
    private final LocalDateTime lockedUntil;

    /**
     * Constructs a new exception with the specified message and lock expiration
     * time.
     *
     * @param message     the detail message
     * @param lockedUntil the time until which the account is locked
     */
    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    /**
     * Constructs a new exception with the specified message, lock expiration time,
     * and cause.
     *
     * @param message     the detail message
     * @param lockedUntil the time until which the account is locked
     * @param cause       the cause
     */
    public AccountLockedException(String message, LocalDateTime lockedUntil, Throwable cause) {
        super(message, cause);
        this.lockedUntil = lockedUntil;
    }
}
