package com.aboff.core.model.entity;

import com.aboff.core.model.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a user account.
 * <p>
 * Stores profile information and role assignment. Authentication credentials
 * are held in {@link UserIdentity} records linked to this entity — one per
 * OAuth provider the user has connected.
 * </p>
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /**
     * Unique display name for the user. Used for identification within the application.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Email address associated with the user's account.
     * May be null if the OAuth provider does not expose an email address.
     */
    @Column(nullable = true, length = 255)
    private String email;

    /**
     * URL pointing to the user's avatar image.
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * IANA timezone identifier for the user (e.g. {@code America/New_York}).
     */
    @Column(length = 50)
    private String timezone;

    /**
     * Timestamp at which the user was soft-deleted, or {@code null} if active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Timestamp at which the user was banned, or {@code null} if not banned.
     */
    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    /**
     * Application-level role governing the user's permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Whether the user has explicitly chosen their own username.
     * <p>
     * Set to {@code false} on first OAuth sign-in, when a temporary random username
     * is auto-generated. Set to {@code true} once the user completes the username
     * selection flow. Used to gate the post-OAuth redirect: first-time users are
     * sent to the choose-username page until this flag is {@code true}.
     * </p>
     */
    @Column(name = "username_chosen", nullable = false)
    @Builder.Default
    private Boolean usernameChosen = false;

    /**
     * Returns whether this user has been soft-deleted.
     *
     * @return true if the user is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the user by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted user.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Returns whether this user is banned.
     *
     * @return true if the user is banned, false otherwise
     */
    public boolean isBanned() {
        return bannedAt != null;
    }

    /**
     * Bans the user by setting the banned_at timestamp.
     */
    public void ban() {
        this.bannedAt = LocalDateTime.now();
    }

    /**
     * Unbans the user by clearing the banned_at timestamp.
     */
    public void unban() {
        this.bannedAt = null;
    }
}
