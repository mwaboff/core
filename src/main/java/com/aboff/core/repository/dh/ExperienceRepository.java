package com.aboff.core.repository.dh;

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
 * Provides data access methods for character and companion experiences including queries for
 * finding experiences by character sheet, companion, created by user, and managing experience data.
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
     * Finds all experiences associated with a specific companion.
     * <p>
     * Returns experiences ordered by creation date (newest first) to show
     * the companion's most recent experiences at the top.
     * </p>
     *
     * @param companionId the ID of the companion
     * @return list of experiences for the companion, ordered by creation date descending
     */
    @Query("SELECT e FROM Experience e WHERE e.companion.id = :companionId ORDER BY e.createdAt DESC")
    List<Experience> findByCompanionId(Long companionId);

    /**
     * Finds all experiences associated with a specific companion with pagination.
     *
     * @param companionId the ID of the companion
     * @param pageable the pagination information
     * @return page of experiences for the companion
     */
    @Query("SELECT e FROM Experience e WHERE e.companion.id = :companionId")
    Page<Experience> findByCompanionId(Long companionId, Pageable pageable);

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
     * Finds a page of experiences owned (directly or via a companion) by a specific user.
     * <p>
     * An Experience is owned by exactly one of {@code characterSheet} or {@code companion}
     * (enforced by the {@code chk_experience_single_owner} CHECK constraint), so "owned by
     * {@code ownerId}" means either the experience's own character sheet is owned by that user,
     * or -- for a companion experience -- the companion's character sheet is. Used to scope an
     * unfiltered list request for a non-privileged caller, so it never falls through to
     * {@link #findAll(Pageable)} and enumerates every user's experiences in one call -- any
     * single experience is still readable by ID or by the {@code characterSheetId}/
     * {@code companionId} filters, since character sheets are public; this only bounds the
     * unfiltered case.
     * </p>
     * <p>
     * Uses explicit {@code LEFT JOIN}s rather than navigating {@code e.characterSheet.owner.id}
     * and {@code e.companion.characterSheet.owner.id} directly in the WHERE clause: JPQL turns
     * each such path expression into its own join, and since no {@code Experience} row ever has
     * both {@code characterSheet} and {@code companion} set, combining two <em>inner</em> joins
     * in one query would always return zero rows (a row would need to satisfy both joins
     * simultaneously to appear at all). {@code LEFT JOIN} avoids that.
     * </p>
     *
     * @param ownerId the ID of the user whose experiences to return
     * @param pageable the pagination information
     * @return page of experiences owned by the user
     */
    @Query("SELECT e FROM Experience e "
            + "LEFT JOIN e.characterSheet cs "
            + "LEFT JOIN e.companion co "
            + "LEFT JOIN co.characterSheet ccs "
            + "WHERE cs.owner.id = :ownerId OR ccs.owner.id = :ownerId")
    Page<Experience> findByOwnerId(Long ownerId, Pageable pageable);

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
