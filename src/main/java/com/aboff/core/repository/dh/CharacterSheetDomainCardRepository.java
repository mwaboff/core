package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.CharacterSheetDomainCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CharacterSheetDomainCard entity operations.
 * <p>
 * Provides data access methods for managing the association between character sheets
 * and domain cards, including queries for finding cards by equipped status.
 * </p>
 */
@Repository
public interface CharacterSheetDomainCardRepository extends JpaRepository<CharacterSheetDomainCard, Long> {

    /**
     * Finds all domain card associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of domain card associations for the character sheet
     */
    List<CharacterSheetDomainCard> findByCharacterSheetId(Long characterSheetId);

    /**
     * Finds domain card associations for a character sheet filtered by equipped status.
     *
     * @param characterSheetId the ID of the character sheet
     * @param equipped whether to find equipped or vault cards
     * @return list of domain card associations matching the criteria
     */
    List<CharacterSheetDomainCard> findByCharacterSheetIdAndEquipped(Long characterSheetId, Boolean equipped);

    /**
     * Finds a specific domain card association by character sheet and domain card IDs.
     *
     * @param characterSheetId the ID of the character sheet
     * @param domainCardId the ID of the domain card
     * @return the domain card association if found
     */
    Optional<CharacterSheetDomainCard> findByCharacterSheetIdAndDomainCardId(Long characterSheetId, Long domainCardId);

    /**
     * Counts the number of equipped domain cards for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return the count of equipped domain cards
     */
    @Query("SELECT COUNT(c) FROM CharacterSheetDomainCard c WHERE c.characterSheet.id = :characterSheetId AND c.equipped = true")
    long countEquippedByCharacterSheetId(@Param("characterSheetId") Long characterSheetId);
}
