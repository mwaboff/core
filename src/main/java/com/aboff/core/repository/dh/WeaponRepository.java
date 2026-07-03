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

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Weapon entities.
 * Provides data access methods with support for soft deletion and filtering.
 */
@Repository
public interface WeaponRepository extends JpaRepository<Weapon, Long> {

    /**
     * Finds all non-deleted weapons with optional filters.
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC)
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
     *
     * @param expansionId Optional filter for expansion ID
     * @param isOfficial Optional filter for official status
     * @param trait Optional filter for weapon trait
     * @param range Optional filter for weapon range
     * @param burden Optional filter for weapon burden
     * @param isPrimary Optional filter for primary/secondary weapon
     * @param damageType Optional filter for damage type (PHYSICAL, MAGIC)
     * @param pageable Pagination information
     * @return Page of all weapons matching the criteria
     */
    @Query("SELECT w FROM Weapon w WHERE " +
           "(:expansionId IS NULL OR w.expansion.id = :expansionId) " +
           "AND (:isOfficial IS NULL OR w.isOfficial = :isOfficial) " +
           "AND (:trait IS NULL OR w.trait = :trait) " +
           "AND (:range IS NULL OR w.range = :range) " +
           "AND (:burden IS NULL OR w.burden = :burden) " +
           "AND (:isPrimary IS NULL OR w.isPrimary = :isPrimary) " +
           "AND (:tier IS NULL OR w.tier = :tier) " +
           "AND (:damageType IS NULL OR w.damage.damageType = :damageType)")
    Page<Weapon> findAllWithFilters(
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

    /**
     * Counts non-deleted weapons created by the given user.
     * Used to enforce the per-user custom item creation cap.
     *
     * @param userId The creator's user ID
     * @return Count of non-deleted weapons created by the user
     */
    long countByCreatedByIdAndDeletedAtIsNull(Long userId);
}
