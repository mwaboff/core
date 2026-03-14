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
 * Stores authentication and profile information.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 50)
    private String timezone;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_failed_login")
    private LocalDateTime lastFailedLogin;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

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

    /**
     * Returns whether this user's account is locked.
     *
     * @return true if the account is locked, false otherwise
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Locks the account for the specified number of minutes.
     *
     * @param minutes the duration in minutes to lock the account
     */
    public void lockAccount(int minutes) {
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    /**
     * Unlocks the account and resets failed login attempts.
     */
    public void unlockAccount() {
        this.accountLockedUntil = null;
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
    }

    /**
     * Increments the failed login attempts counter.
     */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        this.lastFailedLogin = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Resets the failed login attempts counter.
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
    }
}
