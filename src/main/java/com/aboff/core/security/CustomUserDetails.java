package com.aboff.core.security;

import com.aboff.core.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of Spring Security's {@link UserDetails}.
 * <p>
 * Adapts the application's {@link User} entity to Spring Security's requirements.
 * This application uses OAuth-only authentication, so no password is stored or
 * checked; {@link #getPassword()} returns an empty string to satisfy the interface
 * contract without exposing sensitive data.
 * </p>
 */
@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    /**
     * The underlying user entity.
     */
    private final User user;

    /**
     * Returns the authorities granted to the user.
     * Maps the user's role to a Spring Security {@link GrantedAuthority}.
     *
     * @return collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    /**
     * Returns an empty string. Passwords are not used in the OAuth-only flow;
     * this method exists solely to satisfy the {@link UserDetails} interface.
     *
     * @return empty string
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * Returns the username used to identify the user within the application.
     *
     * @return the username
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Indicates whether the user's account has expired.
     *
     * @return always {@code true} — account expiry is not modelled in this application
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     * <p>
     * Account locking was part of the password-based auth flow and has been removed.
     * All active accounts are considered unlocked.
     * </p>
     *
     * @return always {@code true}
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials have expired.
     *
     * @return always {@code true} — credential expiry is not applicable in the OAuth flow
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user account is enabled.
     * A user is enabled if they have not been soft-deleted and are not banned.
     *
     * @return true if the user is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return !user.isDeleted() && !user.isBanned();
    }

    /**
     * Gets the user ID for quick access without exposing the entire {@link User} entity.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return user.getId();
    }
}
