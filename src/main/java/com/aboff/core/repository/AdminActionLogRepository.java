package com.aboff.core.repository;

import com.aboff.core.model.entity.AdminActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AdminActionLog} records.
 */
@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    /**
     * Returns the most recent admin actions performed against a user,
     * newest first.
     *
     * @param targetUserId the id of the user the actions targeted
     * @param pageable     pagination information
     * @return a page of admin action log entries
     */
    Page<AdminActionLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
