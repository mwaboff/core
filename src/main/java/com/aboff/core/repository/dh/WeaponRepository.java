package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.Weapon;
import com.aboff.core.model.enums.Burden;
import com.aboff.core.model.enums.DamageType;
import com.aboff.core.model.enums.Range;
import com.aboff.core.model.enums.Trait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Weapon entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface WeaponRepository extends JpaRepository<Weapon, Long> {

    /**
     * Finds non-deleted weapons the given user is allowed to see, with optional filters.
     * <p>
     * A weapon is visible when any of the following holds: the caller is a moderator or above;
     * the weapon is official; the weapon is public; the weapon has no author (every imported
     * row, plus any official row later demoted to custom — these are treated as system
     * content); the caller authored it; or it is explicitly tagged to a campaign the caller is
     * involved in.
     * </p>
     * <p>
     * {@code memberCampaignIds} <em>grants</em> access rather than narrowing it, unlike the
     * other parameters which are filters. It must never be empty — PostgreSQL rejects an empty
     * {@code IN ()} list — so callers pass a sentinel that matches nothing. See
     * {@code ItemAccessService.VisibilityScope}.
     * </p>
     * <p>
     * The {@code LEFT JOIN} onto campaign tags multiplies rows for a weapon shared with several
     * campaigns, so the query is {@code DISTINCT} and supplies an explicit {@code countQuery}.
     * Without that count override the derived one would report inflated totals and paginate
     * incorrectly.
     * </p>
     *
     * @param userId The calling user's ID, matched against a weapon's author
     * @param memberCampaignIds Campaign IDs the caller is involved in; never empty
     * @param isPrivileged True for MODERATOR+, who bypass visibility filtering
     * @param expansionId Optional filter for expansion ID
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param tier Optional filter for tier
     * @param damageType Optional filter for damage type
     * @param pageable Pagination information
     * @return Page of visible weapons matching the criteria
     */
    @Query(value = "SELECT DISTINCT w FROM Weapon w LEFT JOIN w.campaigns wc " +
           "WHERE w.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR w.isOfficial = true " +
           "     OR w.isPublic = true " +
           "     OR w.createdBy IS NULL " +
           "     OR w.createdBy.id = :userId " +
           "     OR wc.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR w.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR w.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR w.isOfficial = :isOfficial) " +
           "AND (:trait IS NULL OR w.trait = :trait) " +
           "AND (:range IS NULL OR w.range = :range) " +
           "AND (:burden IS NULL OR w.burden = :burden) " +
           "AND (:isPrimary IS NULL OR w.isPrimary = :isPrimary) " +
           "AND (:tier IS NULL OR w.tier = :tier) " +
           "AND (:damageType IS NULL OR w.damage.damageType = :damageType)",
           countQuery = "SELECT COUNT(DISTINCT w) FROM Weapon w LEFT JOIN w.campaigns wc " +
           "WHERE w.deletedAt IS NULL " +
           "AND (:isPrivileged = true " +
           "     OR w.isOfficial = true " +
           "     OR w.isPublic = true " +
           "     OR w.createdBy IS NULL " +
           "     OR w.createdBy.id = :userId " +
           "     OR wc.id IN :memberCampaignIds) " +
           "AND (:expansionId IS NULL OR w.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR w.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR w.isOfficial = :isOfficial) " +
           "AND (:trait IS NULL OR w.trait = :trait) " +
           "AND (:range IS NULL OR w.range = :range) " +
           "AND (:burden IS NULL OR w.burden = :burden) " +
           "AND (:isPrimary IS NULL OR w.isPrimary = :isPrimary) " +
           "AND (:tier IS NULL OR w.tier = :tier) " +
           "AND (:damageType IS NULL OR w.damage.damageType = :damageType)")
    Page<Weapon> findAccessibleWithFilters(
            @Param("userId") Long userId,
            @Param("memberCampaignIds") Collection<Long> memberCampaignIds,
            @Param("isPrivileged") boolean isPrivileged,
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("trait") Trait trait,
            @Param("range") Range range,
            @Param("burden") Burden burden,
            @Param("isPrimary") Boolean isPrimary,
            @Param("tier") Integer tier,
            @Param("damageType") DamageType damageType,
            Pageable pageable);

    /**
     * Finds all non-deleted weapons with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC, PHYSICAL_AND_MAGIC)
     * @param pageable Pagination information
     * @return Page of non-deleted weapons matching the criteria
     */
    @Query("SELECT w FROM Weapon w WHERE w.deletedAt IS NULL " +
           "AND (:expansionId IS NULL OR w.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR w.isOfficial = :isOfficial) " +
           "AND (:trait IS NULL OR w.trait = :trait) " +
           "AND (:range IS NULL OR w.range = :range) " +
           "AND (:burden IS NULL OR w.burden = :burden) " +
           "AND (:isPrimary IS NULL OR w.isPrimary = :isPrimary) " +
           "AND (:tier IS NULL OR w.tier = :tier) " +
           "AND (:damageType IS NULL OR w.damage.damageType = :damageType)")
    Page<Weapon> findByDeletedAtIsNullAndFilters(
            @Param("expansionId") Long expansionId,
            @Param("isOfficial") Boolean isOfficial,
            @Param("trait") Trait trait,
            @Param("range") Range range,
            @Param("burden") Burden burden,
            @Param("isPrimary") Boolean isPrimary,
            @Param("tier") Integer tier,
            @Param("damageType") DamageType damageType,
            Pageable pageable);

    /**
     * Finds all weapons with optional filters, including soft-deleted ones.
     * <p>
     * Supports the same filter set as {@link #findAccessibleWithFilters} so that switching a
     * request to {@code includeDeleted=true} narrows the result set rather than silently
     * widening it: this query previously took no {@code name} or {@code createdByUserId} and
     * returned the whole catalogue with a 200 when either was supplied.
     * </p>
     *
     * @param expansionId Optional filter for expansion ID
     * @param createdByUserId Optional filter narrowing results to one author
     * @param name Optional case-insensitive substring match on the name
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param tier Optional filter for tier
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC, PHYSICAL_AND_MAGIC)
     * @param pageable Pagination information
     * @return Page of all weapons matching the criteria
     */
    @Query("SELECT w FROM Weapon w WHERE " +
           "(:expansionId IS NULL OR w.expansion.id = :expansionId) " +
           "AND (:createdByUserId IS NULL OR w.createdBy.id = :createdByUserId) " +
           "AND (:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:isOfficial IS NULL OR w.isOfficial = :isOfficial) " +
           "AND (:trait IS NULL OR w.trait = :trait) " +
           "AND (:range IS NULL OR w.range = :range) " +
           "AND (:burden IS NULL OR w.burden = :burden) " +
           "AND (:isPrimary IS NULL OR w.isPrimary = :isPrimary) " +
           "AND (:tier IS NULL OR w.tier = :tier) " +
           "AND (:damageType IS NULL OR w.damage.damageType = :damageType)")
    Page<Weapon> findAllWithFilters(
            @Param("expansionId") Long expansionId,
            @Param("createdByUserId") Long createdByUserId,
            @Param("name") String name,
            @Param("isOfficial") Boolean isOfficial,
            @Param("trait") Trait trait,
            @Param("range") Range range,
            @Param("burden") Burden burden,
            @Param("isPrimary") Boolean isPrimary,
            @Param("tier") Integer tier,
            @Param("damageType") DamageType damageType,
            Pageable pageable);

    /**
     * Finds a non-deleted weapon by ID.
     *
     * @param id The weapon ID
     * @return Optional containing the weapon if found and not deleted
     */
    @Query("SELECT w FROM Weapon w WHERE w.id = :id AND w.deletedAt IS NULL")
    Optional<Weapon> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted weapons by their IDs.
     *
     * @param ids List of weapon IDs
     * @return List of non-deleted weapons
     */
    @Query("SELECT w FROM Weapon w WHERE w.id IN :ids AND w.deletedAt IS NULL")
    List<Weapon> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);
}
