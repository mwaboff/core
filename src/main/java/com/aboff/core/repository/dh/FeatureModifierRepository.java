package com.aboff.core.repository.dh;

import com.aboff.core.model.entity.dh.FeatureModifier;
import com.aboff.core.model.enums.ModifierOperation;
import com.aboff.core.model.enums.ModifierTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing FeatureModifier entities.
 * Provides data access methods with support for soft deletion and lookup by modifier attributes.
 */
@Repository
public interface FeatureModifierRepository extends JpaRepository<FeatureModifier, Long> {

    /**
     * Finds a non-deleted feature modifier by its target, operation, and value.
     *
     * @param target    The modifier target attribute
     * @param operation The modifier operation
     * @param value     The modifier value
     * @return Optional containing the modifier if found and not deleted
     */
    Optional<FeatureModifier> findByTargetAndOperationAndValueAndDeletedAtIsNull(
            ModifierTarget target, ModifierOperation operation, Integer value);

    /**
     * Finds all non-deleted feature modifiers by their IDs.
     *
     * @param ids List of modifier IDs
     * @return List of non-deleted feature modifiers
     */
    @Query("SELECT m FROM FeatureModifier m WHERE m.id IN :ids AND m.deletedAt IS NULL")
    List<FeatureModifier> findAllByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);

    /**
     * Finds a non-deleted feature modifier by ID.
     *
     * @param id The modifier ID
     * @return Optional containing the modifier if found and not deleted
     */
    @Query("SELECT m FROM FeatureModifier m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<FeatureModifier> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * Finds all non-deleted feature modifiers with pagination.
     *
     * @param pageable Pagination information
     * @return Page of non-deleted feature modifiers
     */
    @Query("SELECT m FROM FeatureModifier m WHERE m.deletedAt IS NULL")
    Page<FeatureModifier> findAllByDeletedAtIsNull(Pageable pageable);
}