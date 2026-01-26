package com.aboff.core.repository;

import com.aboff.core.model.entity.dh.Experience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Experience entity operations.
 * <p>
 * Provides data access methods for character experiences including queries for
 * finding experiences by character sheet, created by user, and managing experience data.
 * </p>
 */
@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Finds all experiences associated with a specific character sheet.
     * <p>
     * Returns experiences ordered by creation date (newest first) to show
     * the character's most recent experiences at the top.
     * </p>
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of experiences for the character sheet, ordered by creation date descending
     */
    @Query("SELECT e FROM Experience e WHERE e.characterSheet.id = :characterSheetId ORDER BY e.createdAt DESC")
    List<Experience> findByCharacterSheetId(Long characterSheetId);

    /**
     * Finds all experiences associated with a specific character sheet with pagination.
     *
     * @param characterSheetId the ID of the character sheet
     * @param pageable the pagination information
     * @return page of experiences for the character sheet
     */
    @Query("SELECT e FROM Experience e WHERE e.characterSheet.id = :characterSheetId")
    Page<Experience> findByCharacterSheetId(Long characterSheetId, Pageable pageable);

    /**
     * Finds all experiences created by a specific user.
     * <p>
     * Useful for tracking which experiences a GM or player has added across
     * all character sheets.
     * </p>
     *
     * @param userId the ID of the user who created the experiences
     * @return list of experiences created by the user
     */
    List<Experience> findByCreatedById(Long userId);

    /**
     * Counts the number of experiences for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return the count of experiences
     */
    @Query("SELECT COUNT(e) FROM Experience e WHERE e.characterSheet.id = :characterSheetId")
    Long countByCharacterSheetId(Long characterSheetId);

    /**
     * Finds experiences by character sheet and created by user.
     * <p>
     * Useful for tracking which experiences a specific user has added to
     * a specific character.
     * </p>
     *
     * @param characterSheetId the ID of the character sheet
     * @param userId the ID of the user who created the experiences
     * @return list of experiences matching both criteria
     */
    @Query("SELECT e FROM Experience e WHERE e.characterSheet.id = :characterSheetId AND e.createdBy.id = :userId")
    List<Experience> findByCharacterSheetIdAndCreatedById(Long characterSheetId, Long userId);
}
