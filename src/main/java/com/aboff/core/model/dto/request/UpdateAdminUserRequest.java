package com.aboff.core.model.dto.request;

import com.aboff.core.model.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin PATCH body for editing another user's profile.
 * <p>
 * All fields are optional — a {@code null} value means "leave unchanged".
 * No {@code @NotBlank} validation on purpose: partial-update semantics.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminUserRequest {

    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    private Role role;
}
