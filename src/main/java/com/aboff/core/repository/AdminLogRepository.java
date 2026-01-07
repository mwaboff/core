package com.aboff.core.repository;

import com.aboff.core.model.entity.AdminLog;
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
public interface AdminLogRepository extends JpaRepository<AdminLog, UUID> {
    
    Page<AdminLog> findByAdminUserOrderByPerformedAtDesc(User adminUser, Pageable pageable);
    
    Page<AdminLog> findByTargetUserOrderByPerformedAtDesc(User targetUser, Pageable pageable);
    
    Page<AdminLog> findByActionOrderByPerformedAtDesc(String action, Pageable pageable);
    
    @Query("SELECT al FROM AdminLog al WHERE al.performedAt >= :since ORDER BY al.performedAt DESC")
    List<AdminLog> findRecentActions(@Param("since") LocalDateTime since);
    
    @Query("SELECT al FROM AdminLog al WHERE al.adminUser = :adminUser AND al.performedAt >= :since ORDER BY al.performedAt DESC")
    List<AdminLog> findRecentActionsByAdmin(@Param("adminUser") User adminUser, @Param("since") LocalDateTime since);
    
    @Query("SELECT al FROM AdminLog al WHERE al.targetUser = :targetUser AND al.performedAt >= :since ORDER BY al.performedAt DESC")
    List<AdminLog> findRecentActionsOnTarget(@Param("targetUser") User targetUser, @Param("since") LocalDateTime since);
}
