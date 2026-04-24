package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full admin detail view of a user.
 * <p>
 * Composes {@link UserResponse} (reused, always populated with privileged
 * fields) with the linked OAuth {@link UserIdentityResponse} collection
 * (always populated) and optional expanded histories (populated only when
 * requested via {@code ?expand=}).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDetailResponse {

    /** Core profile + privileged fields. */
    private UserResponse user;

    /** Linked OAuth identities. Always populated (1-3 rows). */
    private List<UserIdentityResponse> identities;

    /** Login history (only when {@code ?expand=loginEvents} or {@code all}). */
    private List<LoginEventResponse> loginEvents;

    /** Username history (only when {@code ?expand=usernameHistory} or {@code all}). */
    private List<UsernameHistoryResponse> usernameHistory;

    /** Admin actions targeting this user ({@code ?expand=adminActions} or {@code all}). */
    private List<AdminActionResponse> adminActions;
}
