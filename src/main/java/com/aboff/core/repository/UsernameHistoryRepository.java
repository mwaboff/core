package com.aboff.core.repository;

import com.aboff.core.model.entity.UsernameHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link UsernameHistory} records.
 */
@Repository
public interface UsernameHistoryRepository extends JpaRepository<UsernameHistory, Long> {

    /**
     * Returns the most recent username changes for a user, newest first.
     *
     * @param userId   the id of the user
     * @param pageable pagination information
     * @return a page of username history entries
     */
    Page<UsernameHistory> findByUserIdOrderByChangedAtDesc(Long userId, Pageable pageable);
}
