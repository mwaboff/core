package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterSheetLoot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CharacterSheetLoot entity operations.
 * <p>
 * Provides data access methods for managing the association between character sheets
 * and loot items.
 * </p>
 */
@Repository
public interface CharacterSheetLootRepository extends JpaRepository<CharacterSheetLoot, Long> {

    /**
     * Finds all loot associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of loot associations
     */
    List<CharacterSheetLoot> findByCharacterSheetId(Long characterSheetId);
}
