package com.aboff.core.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing an OAuth identity linked to a {@link User}.
 * <p>
 * Each row corresponds to one OAuth provider connection for a user. A single
 * user may have multiple identities (e.g. Google and GitHub). The combination
 * of {@code provider} and {@code providerSub} is globally unique and serves as
 * the stable lookup key when processing an incoming OAuth callback.
 * </p>
 */
@Entity
@Table(name = "user_identities")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity extends BaseEntity {

    /**
     * The {@link User} this identity belongs to.
     * Loaded lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth provider identifier (e.g. {@code "google"}, {@code "github"}).
     * Together with {@link #providerSub}, forms the unique identity key.
     */
    @Column(nullable = false, length = 32)
    private String provider;

    /**
     * The subject identifier returned by the OAuth provider ({@code sub} claim).
     * This is the provider's stable, unique ID for the authenticated end-user.
     */
    @Column(name = "provider_sub", nullable = false, length = 255)
    private String providerSub;

    /**
     * Email address returned by the OAuth provider for this identity.
     * May be {@code null} if the provider does not expose an email or the user
     * did not grant the email scope.
     */
    @Column(length = 255)
    private String email;

    /**
     * Display name returned by the OAuth provider (e.g. full name or username).
     * Used to pre-populate the user's profile on first sign-in.
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * URL of the avatar/profile picture returned by the OAuth provider.
     * Used to pre-populate the user's avatar on first sign-in.
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Timestamp at which this identity was first linked to the user account.
     * Defaults to the current time when the entity is created.
     */
    @Column(name = "linked_at", nullable = false)
    @Builder.Default
    private LocalDateTime linkedAt = LocalDateTime.now();

    /**
     * Timestamp of the most recent successful authentication via this identity.
     * Updated on every successful OAuth callback. May be {@code null} if the
     * identity was linked but has not yet been used to sign in.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
