package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterAdvancementLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CharacterAdvancementLog entity operations.
 * <p>
 * Provides data access methods for managing character advancement log entries,
 * including queries for finding logs by character sheet and tier.
 * </p>
 */
@Repository
public interface CharacterAdvancementLogRepository extends JpaRepository<CharacterAdvancementLog, Long> {

    /**
     * Finds all advancement logs for a character sheet ordered by level ascending.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of advancement logs ordered by toLevel ascending
     */
    List<CharacterAdvancementLog> findByCharacterSheetIdOrderByToLevelAsc(Long characterSheetId);

    /**
     * Finds advancement logs for a character sheet filtered by tier.
     *
     * @param characterSheetId the ID of the character sheet
     * @param tier the tier to filter by
     * @return list of advancement logs matching the criteria
     */
    List<CharacterAdvancementLog> findByCharacterSheetIdAndTier(Long characterSheetId, Integer tier);

    /**
     * Finds the most recent advancement log for a character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return the most recent advancement log if any exist
     */
    Optional<CharacterAdvancementLog> findTopByCharacterSheetIdOrderByToLevelDesc(Long characterSheetId);
}
