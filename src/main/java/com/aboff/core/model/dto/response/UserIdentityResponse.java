package com.aboff.core.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Admin-facing projection of a linked OAuth identity.
 * <p>
 * Deliberately omits {@code providerSub} and the identity-level {@code email}
 * — both are PII the admin UI does not need.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserIdentityResponse {

    /** Provider identifier (e.g. {@code "google"}, {@code "dev"}). */
    private String provider;

    /** Display name returned by the provider. */
    private String displayName;

    /** When this identity was first linked to the user. */
    private LocalDateTime linkedAt;

    /** Timestamp of the most recent successful sign-in via this identity. */
    private LocalDateTime lastUsedAt;
}
