package com.aboff.core.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a login attempt.
 * Used for security monitoring and rate limiting.
 */
@Entity
@Table(name = "login_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username_attempted", nullable = false, length = 100)
    private String usernameAttempted;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;
}
