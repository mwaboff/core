package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CompanionTraining;
import com.aboff.core.model.enums.CompanionTrainingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CompanionTraining entity data access.
 * Provides methods to query a companion's Training selections.
 * <p>
 * Callers that also save the owning {@code Companion} in the same transaction should mutate
 * trainings through {@code companion.getTrainings()} rather than this repository's
 * {@code delete}/{@code save} methods directly -- see the javadoc on
 * {@link CompanionTraining} for why a direct repository delete can be resurrected by a
 * subsequent cascade save of an already-loaded parent.
 * </p>
 */
@Repository
public interface CompanionTrainingRepository extends JpaRepository<CompanionTraining, Long> {

    /**
     * Find all Training selections for a specific companion.
     *
     * @param companionId the ID of the companion
     * @return list of Training selections for the companion
     */
    List<CompanionTraining> findByCompanionId(Long companionId);

    /**
     * Count how many times a specific Training option has been selected by a companion.
     *
     * @param companionId the ID of the companion
     * @param option the {@code CompanionTrainingOption} name to count
     * @return count of matching Training selections
     */
    long countByCompanionIdAndOption(Long companionId, CompanionTrainingOption option);
}
