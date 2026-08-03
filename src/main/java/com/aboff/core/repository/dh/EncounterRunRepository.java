package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.enums.EncounterRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link EncounterRun} entities.
 */
@Repository
public interface EncounterRunRepository extends JpaRepository<EncounterRun, Long> {

    /**
     * Finds the runs started by a user, optionally filtered by status.
     * <p>
     * Backs the "no {@code campaignId}" branch of {@code GET /api/dh/encounter-runs}: a user's
     * own runs, most-recently-started first.
     * </p>
     *
     * @param userId The ID of the user who started the runs
     * @param status The status to filter by, or null to include every status
     * @return The user's runs, newest first
     */
    @Query("SELECT r FROM EncounterRun r WHERE r.startedBy.id = :userId "
            + "AND (:status IS NULL OR r.status = :status) "
            + "ORDER BY r.createdAt DESC")
    List<EncounterRun> findByStartedByIdAndOptionalStatus(
            @Param("userId") Long userId, @Param("status") EncounterRunStatus status);

    /**
     * Finds the runs tagged to a campaign, optionally filtered by status.
     * <p>
     * Backs the "with {@code campaignId}" branch of {@code GET /api/dh/encounter-runs}: the GM
     * screen panel's campaign-scoped list.
     * </p>
     *
     * @param campaignId The campaign ID the runs are tagged to
     * @param status The status to filter by, or null to include every status
     * @return The campaign's runs, newest first
     */
    @Query("SELECT r FROM EncounterRun r WHERE r.campaign.id = :campaignId "
            + "AND (:status IS NULL OR r.status = :status) "
            + "ORDER BY r.createdAt DESC")
    List<EncounterRun> findByCampaignIdAndOptionalStatus(
            @Param("campaignId") Long campaignId, @Param("status") EncounterRunStatus status);
}
