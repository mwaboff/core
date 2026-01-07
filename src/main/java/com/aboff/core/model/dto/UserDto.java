package com.aboff.core.model.dto;

import com.aboff.core.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDto {
    
    public record Profile(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        String timezone,
        Role role,
        Boolean active,
        LocalDateTime lastLoginAt
    ) {}
    
    public record PublicProfile(
        UUID id,
        String displayName,
        Role role
    ) {}
    
    public record UpdateProfileRequest(
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,
        
        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,
        
        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone
    ) {}
    
    public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
        
        @NotBlank(message = "Display name is required")
        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName
    ) {}
}
