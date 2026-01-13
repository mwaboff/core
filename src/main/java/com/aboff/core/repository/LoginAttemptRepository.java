package com.aboff.core.repository;

import com.aboff.core.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Finds recent failed login attempts for a username within a time window
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.usernameAttempted = :username " +
           "AND la.attemptedAt >= :since AND la.success = false")
    List<LoginAttempt> findRecentFailedAttempts(
        @Param("username") String username,
        @Param("since") LocalDateTime since);

    /**
     * Finds all login attempts for a user, ordered by most recent first
     */
    List<LoginAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);

    /**
     * Deletes login attempts older than the specified date
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    int deleteOldAttempts(@Param("before") LocalDateTime before);
}
