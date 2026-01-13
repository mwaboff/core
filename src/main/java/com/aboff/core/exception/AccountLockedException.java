package com.aboff.core.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;

    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public AccountLockedException(String message, LocalDateTime lockedUntil, Throwable cause) {
        super(message, cause);
        this.lockedUntil = lockedUntil;
    }
}
