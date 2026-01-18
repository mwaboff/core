package com.aboff.core.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for login attempt response.
 * Contains login attempt information for admin viewing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttemptResponse {

    /**
     * The login attempt's unique identifier.
     */
    private Long id;

    /**
     * The user ID associated with this attempt (null if user not found).
     */
    private Long userId;

    /**
     * The username attempted.
     */
    private String usernameAttempted;

    /**
     * Whether the login attempt was successful.
     */
    private Boolean success;

    /**
     * The reason for failure (if applicable).
     */
    private String failureReason;

    /**
     * The IP address from which the attempt was made.
     */
    private String ipAddress;

    /**
     * The User-Agent header from the request.
     */
    private String userAgent;

    /**
     * The timestamp when the attempt was made.
     */
    private LocalDateTime attemptedAt;
}
