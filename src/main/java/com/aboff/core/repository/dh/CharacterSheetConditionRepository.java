package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterSheetCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for CharacterSheetCondition entity operations.
 * <p>
 * Provides data access methods for per-character condition instances, including queries for
 * finding condition instances by character sheet.
 * </p>
 */
@Repository
public interface CharacterSheetConditionRepository extends JpaRepository<CharacterSheetCondition, Long> {

    /**
     * Finds all condition instances associated with a specific character sheet, paginated.
     *
     * @param characterSheetId the ID of the character sheet
     * @param pageable          the pagination information
     * @return page of condition instances for the character sheet
     */
    @Query("SELECT csc FROM CharacterSheetCondition csc WHERE csc.characterSheet.id = :characterSheetId")
    Page<CharacterSheetCondition> findByCharacterSheetId(Long characterSheetId, Pageable pageable);
}
