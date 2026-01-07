package com.aboff.core.repository;

import com.aboff.core.model.entity.LoginHistory;
import com.aboff.core.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
    
    Page<LoginHistory> findByUserOrderByAttemptedAtDesc(User user, Pageable pageable);
    
    Page<LoginHistory> findBySuccessOrderByAttemptedAtDesc(Boolean success, Pageable pageable);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.ipAddress = :ipAddress ORDER BY lh.attemptedAt DESC")
    Page<LoginHistory> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.attemptedAt >= :since ORDER BY lh.attemptedAt DESC")
    List<LoginHistory> findRecentAttempts(@Param("since") LocalDateTime since);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user = :user AND lh.attemptedAt >= :since ORDER BY lh.attemptedAt DESC")
    List<LoginHistory> findRecentAttemptsByUser(@Param("user") User user, @Param("since") LocalDateTime since);
    
    void deleteLoginHistoryByAttemptedAtBefore(LocalDateTime cutoffDate);
}
