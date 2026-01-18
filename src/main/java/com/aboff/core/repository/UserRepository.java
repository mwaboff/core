package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email.
     *
     * @param email the email to search for
     * @return the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists by username.
     *
     * @param username the username to check
     * @return true if the user exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists by email.
     *
     * @param email the email to check
     * @return true if the user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all non-deleted users.
     *
     * @return list of active users
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    java.util.List<User> findAllActive();

    /**
     * Finds a non-deleted user by username.
     *
     * @param username the username to search for
     * @return the user if found and not deleted
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(String username);

    /**
     * Finds a non-deleted user by email.
     *
     * @param email the email to search for
     * @return the user if found and not deleted
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);
}
