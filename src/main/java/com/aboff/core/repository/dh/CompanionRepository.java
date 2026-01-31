package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Companion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Companion entity data access.
 * Provides methods to query companions by character sheet.
 */
@Repository
public interface CompanionRepository extends JpaRepository<Companion, Long> {

    /**
     * Find all companions associated with a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of companions for the character sheet
     */
    List<Companion> findByCharacterSheetId(Long characterSheetId);

    /**
     * Find all companions associated with a specific character sheet with pagination.
     *
     * @param characterSheetId the ID of the character sheet
     * @param pageable pagination information
     * @return page of companions for the character sheet
     */
    Page<Companion> findByCharacterSheetId(Long characterSheetId, Pageable pageable);

    /**
     * Count companions associated with a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return count of companions for the character sheet
     */
    long countByCharacterSheetId(Long characterSheetId);
}
