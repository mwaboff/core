package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.EncounterRunAdversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link EncounterRunAdversary} entities.
 * <p>
 * Listing an encounter run's instances goes through {@code EncounterRun.getEncounterRunAdversaries()}
 * rather than a query here (mirroring {@code Encounter}/{@code EncounterAdversary}); this
 * repository exists for direct by-id lookups when applying a PATCH to a single instance.
 * </p>
 */
@Repository
public interface EncounterRunAdversaryRepository extends JpaRepository<EncounterRunAdversary, Long> {
}
