package com.aboff.core.repository;

import com.aboff.core.model.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link UserIdentity} entity operations.
 * <p>
 * Provides lookups by OAuth provider coordinates and by owning user,
 * which are the two primary access patterns for the OAuth authentication flow.
 * </p>
 */
@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    /**
     * Finds an identity by its OAuth provider and the provider's subject identifier.
     * <p>
     * This is the primary lookup used during an OAuth callback to determine whether
     * an incoming authentication maps to an existing user account.
     * </p>
     *
     * @param provider    the OAuth provider identifier (e.g. {@code "google"})
     * @param providerSub the subject identifier issued by the provider
     * @return the matching identity, or empty if none exists
     */
    Optional<UserIdentity> findByProviderAndProviderSub(String provider, String providerSub);

    /**
     * Finds an identity by provider and subject, eagerly fetching the associated {@link com.aboff.core.model.entity.User}.
     * <p>
     * Used during OAuth login flows where the caller needs to access user properties
     * outside of the original transaction boundary.
     * </p>
     *
     * @param provider    the OAuth provider identifier (e.g. {@code "google"})
     * @param providerSub the subject identifier issued by the provider
     * @return the matching identity with its user eagerly loaded, or empty if none exists
     */
    @Query("SELECT ui FROM UserIdentity ui JOIN FETCH ui.user WHERE ui.provider = :provider AND ui.providerSub = :providerSub")
    Optional<UserIdentity> findByProviderAndProviderSubWithUser(@Param("provider") String provider, @Param("providerSub") String providerSub);

    /**
     * Returns all OAuth identities linked to the given user.
     *
     * @param userId the ID of the owning user
     * @return list of identities; empty if the user has none
     */
    List<UserIdentity> findByUserId(Long userId);
}
