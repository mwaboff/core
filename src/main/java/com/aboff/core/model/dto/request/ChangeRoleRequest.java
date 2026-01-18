package com.aboff.core.model.dto.request;

import com.aboff.core.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for changing a user's role.
 * Used by admins to update user permissions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    /**
     * The new role to assign to the user.
     */
    @NotNull(message = "Role is required")
    private Role newRole;
}
