package com.aboff.core.model.dto.response;

import com.aboff.core.model.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Compact user projection for the admin user-list endpoint.
 * <p>
 * Flat by design: the list endpoint does not support {@code ?expand=} so it
 * can serve 50 users per page without N+1 queries.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserSummaryResponse {

    private Long id;
    private String username;
    private String avatarUrl;
    private Role role;
    private boolean banned;
    private LocalDateTime bannedAt;
    private String banReason;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;
}
