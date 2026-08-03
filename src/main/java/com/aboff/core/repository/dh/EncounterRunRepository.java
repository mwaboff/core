package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.EncounterRun;
import com.aboff.core.model.enums.EncounterRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link EncounterRun} entities.
 * <p>
 * All three finders below eagerly fetch {@code encounter} and {@code encounter.environment} --
 * both are single-valued (to-one) associations, so joining them adds no row multiplication risk
 * -- so that {@code EncounterRunService} can read {@code environmentId} off every run without an
 * extra query per run.
 * </p>
 */
@Repository
public interface EncounterRunRepository extends JpaRepository<EncounterRun, Long> {

    /**
     * Finds a run by ID, eagerly fetching its encounter and that encounter's environment.
     * <p>
     * Overrides the inherited {@link JpaRepository#findById} rather than introducing a
     * differently-named method, so every existing caller (and every existing test stub) keeps
     * working unchanged while gaining the eager fetch.
     * </p>
     *
     * @param id The run ID
     * @return The run, if found
     */
    @Override
    @Query("SELECT r FROM EncounterRun r "
            + "LEFT JOIN FETCH r.encounter e "
            + "LEFT JOIN FETCH e.environment "
            + "WHERE r.id = :id")
    Optional<EncounterRun> findById(@Param("id") Long id);

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
    @Query("SELECT r FROM EncounterRun r "
            + "LEFT JOIN FETCH r.encounter e "
            + "LEFT JOIN FETCH e.environment "
            + "WHERE r.startedBy.id = :userId "
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
    @Query("SELECT r FROM EncounterRun r "
            + "LEFT JOIN FETCH r.encounter e "
            + "LEFT JOIN FETCH e.environment "
            + "WHERE r.campaign.id = :campaignId "
            + "AND (:status IS NULL OR r.status = :status) "
            + "ORDER BY r.createdAt DESC")
    List<EncounterRun> findByCampaignIdAndOptionalStatus(
            @Param("campaignId") Long campaignId, @Param("status") EncounterRunStatus status);

    /**
     * Finds a source encounter's runs in a given status.
     * <p>
     * Backs {@code EncounterService#deleteEncounter}'s cascade: a soft-deleted encounter must not
     * leave an {@link EncounterRunStatus#ACTIVE} run playable/resumable against it, so that run is
     * discarded in the same transaction. No fetch-join here (unlike the finders above) -- the
     * caller only needs the ids to hard-delete, never the encounter/environment relations.
     * </p>
     *
     * @param encounterId The source encounter's ID
     * @param status The status to match
     * @return The matching runs
     */
    List<EncounterRun> findByEncounter_IdAndStatus(Long encounterId, EncounterRunStatus status);
}
