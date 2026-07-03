package com.aboff.core.exception;

/**
 * Exception thrown when a user attempts to create more custom items than the allowed cap.
 */
public class TooManyCustomItemsException extends RuntimeException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public TooManyCustomItemsException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public TooManyCustomItemsException(String message, Throwable cause) {
        super(message, cause);
    }
}
