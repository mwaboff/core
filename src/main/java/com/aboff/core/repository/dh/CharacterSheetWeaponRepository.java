package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterSheetWeapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CharacterSheetWeapon entity operations.
 * <p>
 * Provides data access methods for managing the association between character sheets
 * and weapons, including equipped status and slot tracking.
 * </p>
 */
@Repository
public interface CharacterSheetWeaponRepository extends JpaRepository<CharacterSheetWeapon, Long> {

    /**
     * Finds all weapon associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of weapon associations
     */
    List<CharacterSheetWeapon> findByCharacterSheetId(Long characterSheetId);

    /**
     * Finds all equipped weapon associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of equipped weapon associations
     */
    List<CharacterSheetWeapon> findByCharacterSheetIdAndEquippedTrue(Long characterSheetId);

    /**
     * Finds a weapon association by character sheet ID and slot.
     *
     * @param characterSheetId the character sheet ID
     * @param slot the equipment slot (e.g., "PRIMARY" or "SECONDARY")
     * @return optional containing the weapon association if found
     */
    Optional<CharacterSheetWeapon> findByCharacterSheetIdAndSlot(Long characterSheetId, String slot);
}
