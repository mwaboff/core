package com.aboff.core.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    // NEVER expose: passwordHash, accountLockedUntil, failedLoginAttempts, lastFailedLogin, deletedAt
}
