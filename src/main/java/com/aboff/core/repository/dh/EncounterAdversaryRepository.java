package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.EncounterAdversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing EncounterAdversary entities.
 * Provides data access methods for the join entity between encounters and adversaries.
 */
@Repository
public interface EncounterAdversaryRepository extends JpaRepository<EncounterAdversary, Long> {

    /**
     * Finds all adversary entries for a specific encounter.
     *
     * @param encounterId The ID of the encounter
     * @return List of encounter adversary entries
     */
    @Query("SELECT ea FROM EncounterAdversary ea WHERE ea.encounter.id = :encounterId")
    List<EncounterAdversary> findByEncounterId(@Param("encounterId") Long encounterId);

    /**
     * Finds a specific encounter adversary entry by encounter and adversary IDs.
     *
     * @param encounterId The ID of the encounter
     * @param adversaryId The ID of the adversary
     * @return Optional containing the encounter adversary if found
     */
    @Query("SELECT ea FROM EncounterAdversary ea WHERE ea.encounter.id = :encounterId " +
           "AND ea.adversary.id = :adversaryId")
    Optional<EncounterAdversary> findByEncounterIdAndAdversaryId(
            @Param("encounterId") Long encounterId,
            @Param("adversaryId") Long adversaryId);
}
