package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import com.aboff.core.model.enums.Trait;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entity representing a subclass path in the Daggerheart TTRPG system.
 * <p>
 * A subclass path groups three subclass cards (Foundation, Specialization, Mastery)
 * that share a common theme within a class. Each path has associated domains and
 * an optional spellcasting trait that apply to all cards in the path.
 * </p>
 */
@Entity
@Table(name = "subclass_paths")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubclassPath extends BaseEntity {

    /**
     * The name of the subclass path.
     * Identifies the thematic grouping of subclass cards within a class.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * The class this subclass path belongs to.
     * Each path is associated with exactly one class.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associated_class_id", nullable = false)
    private Class associatedClass;

    /**
     * The trait used for spellcasting within this subclass path.
     * Optional; null if the path does not involve spellcasting.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "spellcasting_trait", length = 20)
    private Trait spellcastingTrait;

    /**
     * The domains associated with this subclass path.
     * Cards within this path typically draw from these domains.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subclass_path_domains",
        joinColumns = @JoinColumn(name = "subclass_path_id"),
        inverseJoinColumns = @JoinColumn(name = "domain_id")
    )
    private Set<Domain> associatedDomains;

    /**
     * The expansion this subclass path belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Timestamp indicating when this subclass path was soft-deleted.
     * If null, the subclass path is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this subclass path has been soft-deleted.
     *
     * @return true if the subclass path is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the subclass path by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted subclass path.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
