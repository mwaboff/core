package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import com.aboff.core.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by exact username match.
     * <p>
     * Intended for username collision checking during OAuth user provisioning,
     * where the username is known precisely.
     * </p>
     *
     * @param username the exact username to search for
     * @return the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email (case-insensitive).
     *
     * @param email the email to search for
     * @return the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists by email (case-insensitive).
     *
     * @param email the email to check
     * @return true if the user exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists by username (case-insensitive).
     * Used during username selection to prevent duplicate usernames.
     *
     * @param username the username to check
     * @return true if the username is already taken, false otherwise
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Finds all non-deleted users.
     *
     * @return list of active users
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    /**
     * Finds a non-deleted user by email.
     *
     * @param email the email to search for
     * @return the user if found and not deleted
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);

    /**
     * Finds non-deleted users with optional admin filters applied.
     * <p>
     * Intended for the admin user-list endpoint. Filters are null-tolerant and
     * composable: passing {@code null} for any filter skips that predicate.
     * </p>
     *
     * @param isBanned    if non-null, matches only users whose {@code banned_at}
     *                    is (when {@code true}) or is not (when {@code false})
     *                    populated
     * @param role        if non-null, restricts to this role
     * @param usernameLike case-insensitive substring match on username; null-skipped
     * @param emailLike   case-insensitive substring match on email; null-skipped
     * @param pageable    pagination and sort
     * @return a page of matching, non-deleted users
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL " +
            "AND (:isBanned IS NULL " +
            "     OR (:isBanned = true AND u.bannedAt IS NOT NULL) " +
            "     OR (:isBanned = false AND u.bannedAt IS NULL)) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:usernameLike IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:usernameLike AS string), '%'))) " +
            "AND (:emailLike IS NULL OR (u.email IS NOT NULL AND LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:emailLike AS string), '%'))))")
    Page<User> findAllWithAdminFilters(
            @Param("isBanned") Boolean isBanned,
            @Param("role") Role role,
            @Param("usernameLike") String usernameLike,
            @Param("emailLike") String emailLike,
            Pageable pageable);

    /**
     * Updates the {@code last_seen_at} timestamp for a user without loading the
     * full entity. Used by the authentication filter to keep activity tracking
     * cheap.
     *
     * @param userId the user id
     * @param ts     the new timestamp
     * @return the number of rows updated (0 if the user does not exist)
     */
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :ts WHERE u.id = :userId")
    int updateLastSeenAt(@Param("userId") Long userId, @Param("ts") LocalDateTime ts);
}
