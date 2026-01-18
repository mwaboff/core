package com.aboff.core.exception;

/**
 * Exception thrown when a user lacks sufficient permissions for an action.
 */
public class InsufficientPermissionsException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public InsufficientPermissionsException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InsufficientPermissionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
