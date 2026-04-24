package com.aboff.core.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body for the admin ban endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanUserRequest {

    /**
     * Human-readable reason for the ban. Stored verbatim on the user record
     * and shown to admins in the user detail view. Optional.
     */
    @Size(max = 500, message = "Ban reason must not exceed 500 characters")
    private String reason;
}
