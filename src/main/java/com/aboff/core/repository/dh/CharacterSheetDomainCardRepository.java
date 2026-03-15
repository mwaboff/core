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
 * and domain cards, including equipped status tracking.
 * </p>
 */
@Repository
public interface CharacterSheetDomainCardRepository extends JpaRepository<CharacterSheetDomainCard, Long> {

    /**
     * Finds all domain card associations for a specific character sheet.
     *
     * @param characterSheetId the ID of the character sheet
     * @return list of domain card associations
     */
    List<CharacterSheetDomainCard> findByCharacterSheetId(Long characterSheetId);

    /**
     * Finds a domain card association by character sheet ID and domain card ID.
     *
     * @param characterSheetId the character sheet ID
     * @param domainCardId the domain card ID
     * @return optional containing the association if found
     */
    Optional<CharacterSheetDomainCard> findByCharacterSheetIdAndDomainCardId(
            Long characterSheetId, Long domainCardId);

    /**
     * Counts the number of equipped domain cards for a character sheet.
     *
     * @param characterSheetId the character sheet ID
     * @return the count of equipped domain cards
     */
    @Query("SELECT COUNT(csdc) FROM CharacterSheetDomainCard csdc WHERE csdc.characterSheet.id = :characterSheetId AND csdc.equipped = true")
    long countEquippedByCharacterSheetId(@Param("characterSheetId") Long characterSheetId);

    /**
     * Deletes all domain card associations for a specific character sheet and domain card.
     *
     * @param characterSheetId the character sheet ID
     * @param domainCardId the domain card ID
     */
    void deleteByCharacterSheetIdAndDomainCardId(Long characterSheetId, Long domainCardId);
}
