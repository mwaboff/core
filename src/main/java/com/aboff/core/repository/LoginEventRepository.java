package com.aboff.core.repository;

import com.aboff.core.model.entity.LoginEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link LoginEvent} records.
 */
@Repository
public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

    /**
     * Returns the most recent login events for a user, newest first.
     *
     * @param userId   the id of the user
     * @param pageable pagination information
     * @return a page of login events
     */
    Page<LoginEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
