package com.aboff.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Immutable record of a single successful authentication event.
 * <p>
 * Written when a JWT is issued (either via OAuth2 or the dev-login endpoint).
 * Persists beyond the lifetime of the associated {@link ActiveToken}, so that
 * admins can view a user's historical login activity even after token cleanup.
 * Uses the inherited {@code createdAt} column as the authoritative event time.
 * </p>
 */
@Entity
@Table(name = "login_events")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent extends BaseEntity {

    /**
     * The id of the user this login event belongs to.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * OAuth provider identifier ({@code "google"}, {@code "dev"}, etc.), or
     * {@code null} if the authentication path did not expose one.
     */
    @Column(length = 32)
    private String provider;

    /**
     * IP address the login originated from; IPv4 or IPv6. Nullable because
     * the remote address is not always recoverable.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Truncated User-Agent string for the device used to authenticate.
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
}
