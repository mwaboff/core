package com.aboff.core.model.dto;

import com.aboff.core.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdminDto {
    
    public record DetailedUserInfo(
        UUID id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String timezone,
        Role role,
        Boolean active,
        LocalDateTime lastLoginAt,
        LocalDateTime createdDate,
        LocalDateTime lastUpdatedDate
    ) {}
    
    public record BanUserRequest(
        @NotNull(message = "Banned status is required")
        Boolean banned,
        
        String reason
    ) {}
    
    public record ChangeRoleRequest(
        @NotNull(message = "Role is required")
        Role role
    ) {}
    
    public record AdminLogEntry(
        UUID id,
        String adminUsername,
        String targetUsername,
        String action,
        String details,
        String ipAddress,
        LocalDateTime performedAt
    ) {}
    
    public record LoginHistoryEntry(
        UUID id,
        String username,
        String email,
        Boolean success,
        String ipAddress,
        String userAgent,
        LocalDateTime attemptedAt
    ) {}
}
