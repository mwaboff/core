package com.aboff.core.exception;

/**
 * Exception thrown when a password does not meet security requirements.
 */
public class InvalidPasswordException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public InvalidPasswordException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
