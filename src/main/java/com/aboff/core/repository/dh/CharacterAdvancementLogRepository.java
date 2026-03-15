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
 * which track level-up choices for undo support.
 * </p>
 */
@Repository
public interface CharacterAdvancementLogRepository extends JpaRepository<CharacterAdvancementLog, Long> {

    /**
     * Finds all advancement logs for a character sheet in a specific tier.
     *
     * @param characterSheetId the character sheet ID
     * @param tier the tier to filter by
     * @return list of advancement logs in the specified tier
     */
    List<CharacterAdvancementLog> findByCharacterSheetIdAndTier(Long characterSheetId, Integer tier);

    /**
     * Finds the most recent advancement log for a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @return optional containing the most recent log entry
     */
    Optional<CharacterAdvancementLog> findTopByCharacterSheetIdOrderByToLevelDesc(Long characterSheetId);

    /**
     * Finds all advancement logs for a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @return list of all advancement logs
     */
    List<CharacterAdvancementLog> findByCharacterSheetId(Long characterSheetId);
}
