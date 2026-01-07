package com.aboff.core.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_login_history_user_id", columnList = "user_id"),
    @Index(name = "idx_login_history_ip_address", columnList = "ip_address"),
    @Index(name = "idx_login_history_attempted_at", columnList = "attempted_at")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LoginHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Size(max = 1000, message = "User agent must not exceed 1000 characters")
    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
