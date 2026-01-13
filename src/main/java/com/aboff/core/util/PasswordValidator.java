package com.aboff.core.util;

import com.aboff.core.exception.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordValidator(
            @Value("${application.security.password.min-length}") int minLength,
            @Value("${application.security.password.require-uppercase}") boolean requireUppercase,
            @Value("${application.security.password.require-lowercase}") boolean requireLowercase,
            @Value("${application.security.password.require-digit}") boolean requireDigit,
            @Value("${application.security.password.require-special}") boolean requireSpecial) {
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecial = requireSpecial;
    }

    /**
     * Validates password strength according to configured rules
     * @throws InvalidPasswordException if password doesn't meet requirements
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < minLength) {
            throw new InvalidPasswordException(
                    String.format("Password must be at least %d characters long", minLength));
        }

        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }

        if (requireLowercase && !password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }

        if (requireDigit && !password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one digit");
        }

        if (requireSpecial && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new InvalidPasswordException("Password must contain at least one special character");
        }
    }
}
