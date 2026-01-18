package com.aboff.core.repository;

import com.aboff.core.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for LoginAttempt entity operations.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

        /**
         * Finds recent failed login attempts for a username within a time window.
         *
         * @param username the username to check
         * @param since    the start time of the window
         * @return list of failed login attempts
         */
        @Query("SELECT la FROM LoginAttempt la WHERE la.usernameAttempted = :username " +
                        "AND la.attemptedAt >= :since AND la.success = false")
        List<LoginAttempt> findRecentFailedAttempts(
                        @Param("username") String username,
                        @Param("since") LocalDateTime since);

        /**
         * Finds all login attempts for a user, ordered by most recent first.
         *
         * @param userId the user ID
         * @return list of login attempts
         */
        List<LoginAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);

        /**
         * Finds all login attempts for an IP address, ordered by most recent first.
         *
         * @param ipAddress the IP address
         * @return list of login attempts
         */
        List<LoginAttempt> findByIpAddressOrderByAttemptedAtDesc(String ipAddress);

        /**
         * Deletes login attempts older than the specified date.
         *
         * @param before the timestamp before which attempts should be deleted
         * @return the number of attempts deleted
         */
        @Modifying
        @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
        int deleteOldAttempts(@Param("before") LocalDateTime before);
}
