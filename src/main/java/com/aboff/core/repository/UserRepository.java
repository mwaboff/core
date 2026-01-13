package com.aboff.core.repository;

import com.aboff.core.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists by username
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Finds all non-deleted users
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    java.util.List<User> findAllActive();

    /**
     * Finds a non-deleted user by username
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(String username);

    /**
     * Finds a non-deleted user by email
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);
}
