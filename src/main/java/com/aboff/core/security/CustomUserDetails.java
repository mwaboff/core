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
 * Custom implementation of Spring Security's UserDetails.
 * Adapts the application's User entity to Spring Security's requirements.
 */
@AllArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    /**
     * The functional user entity.
     */
    private final User user;

    /**
     * Returns the authorities granted to the user.
     * Currently returns a default ROLE_USER for all users.
     *
     * @return collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // For now, all users have the ROLE_USER authority
        // Future enhancement: Add roles table and fetch from database
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Returns the password used to authenticate the user.
     *
     * @return the password hash
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Returns the username used to authenticate the user.
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
     * @return true if the user's account is valid (ie non-expired), false otherwise
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     *
     * @return true if the user is not locked, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked();
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     *
     * @return true if the user's credentials are valid (ie non-expired), false
     *         otherwise
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     *
     * @return true if the user is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return !user.isDeleted();
    }

    /**
     * Gets the user ID for quick access without exposing the entire User entity.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return user.getId();
    }
}
