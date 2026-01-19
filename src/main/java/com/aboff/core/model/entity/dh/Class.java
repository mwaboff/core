package com.aboff.core.model.entity.dh;

import com.aboff.core.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entity representing a character class in the Daggerheart TTRPG system.
 * <p>
 * Classes define a character's role and abilities. Each class has associated
 * domains, features (hope and class features), and character creation questions
 * (background and connection).
 * </p>
 */
@Entity
@Table(name = "classes")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Class extends BaseEntity {

    /**
     * The name of the class.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Detailed description of the class and its characteristics.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The expansion this class belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expansion_id", nullable = false)
    private Expansion expansion;

    /**
     * Text describing the starting items or equipment for this class.
     */
    @Column(name = "starting_class_items", columnDefinition = "TEXT")
    private String startingClassItems;

    /**
     * The starting evasion value for characters of this class.
     * Must be a positive integer.
     */
    @Column(name = "starting_evasion", nullable = false)
    private Integer startingEvasion;

    /**
     * The starting hit points for characters of this class.
     * Must be a positive integer.
     */
    @Column(name = "starting_hit_points", nullable = false)
    private Integer startingHitPoints;

    /**
     * The domains associated with this class.
     * Characters of this class typically have access to cards from these domains.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_domains",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "domain_id")
    )
    private Set<Domain> associatedDomains;

    /**
     * The hope features available to this class.
     * These are special features that can be gained through hope mechanics.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_hope_features",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> hopeFeatures;

    /**
     * The class features inherent to this class.
     * These are core abilities that define the class.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_class_features",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<Feature> classFeatures;

    /**
     * Background questions for character creation.
     * These help players develop their character's backstory.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_background_questions",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> backgroundQuestions;

    /**
     * Connection questions for character creation.
     * These help players establish relationships with other characters.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "class_connection_questions",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> connectionQuestions;

    /**
     * Timestamp indicating when this class was soft-deleted.
     * If null, the class is active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Returns whether this class has been soft-deleted.
     *
     * @return true if the class is deleted, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes the class by setting the deleted_at timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted class.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
