package com.aboff.core.repository;

import com.aboff.core.model.entity.dh.CharacterSheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CharacterSheet entity operations.
 * <p>
 * Provides data access methods for character sheets including queries for
 * finding sheets by owner, retrieving active (non-deleted) sheets, and
 * managing character data.
 * </p>
 */
@Repository
public interface CharacterSheetRepository extends JpaRepository<CharacterSheet, Long> {

    /**
     * Finds all character sheets owned by a specific user.
     * <p>
     * This includes both active and soft-deleted character sheets.
     * Use {@link #findByOwnerIdAndDeletedAtIsNull(Long)} to retrieve only active sheets.
     * </p>
     *
     * @param ownerId the ID of the user who owns the character sheets
     * @return list of character sheets owned by the user
     */
    List<CharacterSheet> findByOwnerId(Long ownerId);

    /**
     * Finds all active (non-deleted) character sheets owned by a specific user.
     * <p>
     * Only returns character sheets where deletedAt is null, excluding any
     * soft-deleted or retired characters.
     * </p>
     *
     * @param ownerId the ID of the user who owns the character sheets
     * @return list of active character sheets owned by the user
     */
    List<CharacterSheet> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    /**
     * Finds a character sheet by ID only if it is not deleted.
     *
     * @param id the ID of the character sheet
     * @return the character sheet if found and not deleted
     */
    @Query("SELECT cs FROM CharacterSheet cs WHERE cs.id = :id AND cs.deletedAt IS NULL")
    Optional<CharacterSheet> findActiveById(Long id);

    /**
     * Finds all active (non-deleted) character sheets.
     *
     * @return list of all active character sheets
     */
    @Query("SELECT cs FROM CharacterSheet cs WHERE cs.deletedAt IS NULL")
    List<CharacterSheet> findAllActive();

    /**
     * Finds character sheets by name (case-insensitive partial match).
     * <p>
     * Only returns active character sheets. Useful for searching characters
     * by name across the system.
     * </p>
     *
     * @param name the name to search for (partial match supported)
     * @return list of character sheets matching the name
     */
    @Query("SELECT cs FROM CharacterSheet cs WHERE LOWER(cs.name) LIKE LOWER(CONCAT('%', :name, '%')) AND cs.deletedAt IS NULL")
    List<CharacterSheet> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);

    /**
     * Counts the number of active character sheets owned by a specific user.
     *
     * @param ownerId the ID of the user
     * @return the count of active character sheets
     */
    @Query("SELECT COUNT(cs) FROM CharacterSheet cs WHERE cs.owner.id = :ownerId AND cs.deletedAt IS NULL")
    Long countActiveByOwnerId(Long ownerId);

    /**
     * Finds all active character sheets with optional filters.
     * <p>
     * Supports filtering by owner ID, name (case-insensitive partial match), and level range.
     * All filter parameters are optional - null values are ignored.
     * </p>
     *
     * @param ownerId the owner ID to filter by (optional)
     * @param name the name to search for with partial match (optional)
     * @param minLevel the minimum level (optional)
     * @param maxLevel the maximum level (optional)
     * @param pageable pagination information
     * @return paginated list of character sheets matching the filters
     */
    @Query("SELECT cs FROM CharacterSheet cs WHERE cs.deletedAt IS NULL AND " +
           "(:ownerId IS NULL OR cs.owner.id = :ownerId) AND " +
           "(:name IS NULL OR LOWER(cs.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:minLevel IS NULL OR cs.level >= :minLevel) AND " +
           "(:maxLevel IS NULL OR cs.level <= :maxLevel)")
    Page<CharacterSheet> findActiveWithFilters(
            @Param("ownerId") Long ownerId,
            @Param("name") String name,
            @Param("minLevel") Integer minLevel,
            @Param("maxLevel") Integer maxLevel,
            Pageable pageable);
}
