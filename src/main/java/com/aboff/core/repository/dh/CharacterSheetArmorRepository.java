package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterSheetArmor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CharacterSheetArmor entity operations.
 * <p>
 * Provides data access methods for managing the association between character sheets
 * and armor pieces, including equipped status tracking.
 * </p>
 */
@Repository
public interface CharacterSheetArmorRepository extends JpaRepository<CharacterSheetArmor, Long> {

    /**
     * Finds all armor associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of armor associations
     */
    List<CharacterSheetArmor> findByCharacterSheetId(Long characterSheetId);

    /**
     * Finds all equipped armor associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of equipped armor associations
     */
    List<CharacterSheetArmor> findByCharacterSheetIdAndEquippedTrue(Long characterSheetId);
}
