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
     * Optional human-readable reason provided by an admin at ban time.
     * Cleared when the user is unbanned.
     */
    @Column(name = "ban_reason", length = 500)
    private String banReason;

    /**
     * Timestamp of the most recent authenticated request made by this user.
     * Updated by the JWT filter on each authenticated request (throttled).
     * Nullable for users who have never signed in since this column was added.
     */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

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
     * Manually-granted per-user override for paid-expansion content.
     * <p>
     * Lets an admin give a specific USER-role account visibility into non-SRD content
     * (paid expansion books) without changing their role. Independent of {@link #role} --
     * ADMIN and OWNER always see non-SRD content regardless of this flag; it exists solely
     * to extend that visibility to individual users below MODERATOR. See
     * {@code ContentAccessService#mayViewNonSrd()}.
     * </p>
     */
    @Column(name = "access_all_expansions", nullable = false)
    @Builder.Default
    private Boolean accessAllExpansions = false;

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
     * Bans the user with no reason recorded.
     */
    public void ban() {
        ban(null);
    }

    /**
     * Bans the user, recording the provided reason.
     *
     * @param reason human-readable reason; may be {@code null}
     */
    public void ban(String reason) {
        this.bannedAt = LocalDateTime.now();
        this.banReason = reason;
    }

    /**
     * Unbans the user by clearing the banned_at timestamp and reason.
     */
    public void unban() {
        this.bannedAt = null;
        this.banReason = null;
    }
}
